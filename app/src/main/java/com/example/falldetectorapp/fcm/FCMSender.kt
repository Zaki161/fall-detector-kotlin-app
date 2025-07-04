package com.example.falldetectorapp.fcm

import android.content.Context
import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import okhttp3.MediaType.Companion.toMediaType
/**
 * Obiekt odpowiedzialny za wysyłanie powiadomień push do Firebase Cloud Messaging (FCM)
 * z wykorzystaniem tokena serwisowego oraz biblioteki JWT do podpisywania żądań.
 *
 * Implementuje pełny przepływ autoryzacji OAuth 2.0 z JWT i komunikację REST z FCM HTTP v1 API.
 */
object FCMSender {

    private const val TAG = "FCMSender"
    private const val FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
    /**
     * Wysyła powiadomienie push do określonego urządzenia na podstawie jego tokena FCM.
     *
     * @param context Kontekst aplikacji wymagany do odczytania pliku klucza serwisowego.
     * @param targetToken Token FCM odbiorcy (urządzenia).
     * @param title Tytuł powiadomienia.
     * @param body Treść powiadomienia.
     * @param onResult Callback z wynikiem: `true` gdy sukces, `false` w przypadku błędu.
     */
    fun sendNotification(
        context: Context,
        targetToken: String,
        title: String,
        body: String,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            val serviceAccount = context.assets.open("falldetectorapp-c926f-a33259b239e3.json")
                .bufferedReader().use { it.readText() }

            val jsonObject = JSONObject(serviceAccount)
            val clientEmail = jsonObject.getString("client_email")
            val privateKey = jsonObject.getString("private_key")
            val projectId = jsonObject.getString("project_id")

            val jwt = generateJwt(clientEmail, privateKey)

            getAccessToken(jwt) { accessToken ->
                if (accessToken != null) {
                    postToFcm(accessToken, targetToken, title, body, projectId, onResult)
                } else {
                    onResult(false, "Nie udało się uzyskać tokena dostępu.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas przygotowania danych", e)
            onResult(false, "Błąd podczas przygotowania danych: ${e.message}")
        }
    }
    /**
     * Tworzy i podpisuje token JWT niezbędny do uzyskania tokena dostępu OAuth 2.0.
     *
     * @param clientEmail Adres e-mail konta serwisowego.
     * @param privateKeyPem Prywatny klucz RSA (PEM).
     * @return Podpisany token JWT.
     */
    private fun generateJwt(clientEmail: String, privateKeyPem: String): String {
        val now = System.currentTimeMillis()
        val algorithm = Algorithm.RSA256(null, getPrivateKeyFromPem(privateKeyPem))

        return JWT.create()
            .withIssuer(clientEmail)
            .withAudience(TOKEN_URL)
            .withClaim("scope", FCM_SCOPE)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + 3600 * 1000))
            .sign(algorithm)
    }
    /**
     * Parsuje i dekoduje klucz prywatny RSA z formatu PEM.
     *
     * @param pem Tekst w formacie PEM zawierający klucz prywatny.
     * @return Obiekt `RSAPrivateKey` gotowy do podpisywania JWT.
     */
    private fun getPrivateKeyFromPem(pem: String): RSAPrivateKey {
        val cleaned = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val decoded = android.util.Base64.decode(cleaned, android.util.Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(decoded)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec) as RSAPrivateKey
    }
    /**
     * Wysyła żądanie do Google OAuth 2.0 API w celu uzyskania tokena dostępu na podstawie JWT.
     *
     * @param jwt Podpisany token JWT.
     * @param callback Callback zwracający token dostępu lub `null` w przypadku błędu.
     */
    private fun getAccessToken(jwt: String, callback: (String?) -> Unit) {
        val client = OkHttpClient()

        val body = FormBody.Builder()
            .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            .add("assertion", jwt)
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Access token request failed", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBodyString = response.body?.string()
                Log.d(TAG, "FCM HTTP ${response.code} BODY: $responseBodyString")
                response.close() // Zamknij ręcznie, nie używaj `use {}` jeśli już czytasz body!

                if (response.isSuccessful && responseBodyString != null) {
                    try {
                        val res = JSONObject(responseBodyString)
                        val token = res.getString("access_token")
                        Log.d(TAG, "Access token retrieved successfully.")
                        callback(token)
                    } catch (e: Exception) {
                        Log.e(TAG, "JSON parsing error: ${e.message}")
                        callback(null)
                    }
                } else {
                    Log.e(TAG, "Access token error: ${response.code}, $responseBodyString")
                    callback(null)
                }
            }
        })
    }
    /**
     * Wysyła wiadomość push do FCM HTTP v1 API.
     *
     * @param accessToken Token dostępu OAuth 2.0.
     * @param targetToken Token FCM odbiorcy.
     * @param title Tytuł powiadomienia.
     * @param body Treść powiadomienia.
     * @param projectId Identyfikator projektu Firebase.
     * @param onResult Callback z informacją o sukcesie lub błędzie.
     */
    private fun postToFcm(
        accessToken: String,
        targetToken: String,
        title: String,
        body: String,
        projectId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val client = OkHttpClient()
        val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

        val json = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", targetToken)
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", body)
                })
            })
        }

        Log.d(TAG, "Sending notification to token: $targetToken")
        Log.d(TAG, "Payload: $json")

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create("application/json".toMediaType(), json.toString()))
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "FCM send failed", e)
                onResult(false, "Nie udało się wysłać powiadomienia: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Log.i(TAG, "FCM response: ${response.code}, $responseBody")
                    onResult(true, "Powiadomienie wysłane pomyślnie.")
                } else {
                    Log.e(TAG, "FCM error: ${response.code}, $responseBody")
                    onResult(false, "Błąd FCM: ${response.code} ${responseBody}")
                }
            }
        })
    }
}