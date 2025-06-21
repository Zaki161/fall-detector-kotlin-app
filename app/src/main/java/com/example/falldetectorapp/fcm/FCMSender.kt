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

object FCMSender {

    private const val TAG = "FCMSender"
    private const val FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
    private const val FCM_SEND_URL = "https://fcm.googleapis.com/v1/projects/falldetectorapp-c926f/messages:send"

    fun sendNotification(
        context: Context,
        targetToken: String,
        title: String,
        body: String
    ) {
        val serviceAccount = context.assets.open("falldetectorapp-c926f-a33259b239e3.json")
            .bufferedReader().use { it.readText() }

        val jsonObject = JSONObject(serviceAccount)
        val clientEmail = jsonObject.getString("client_email")
        val privateKey = jsonObject.getString("private_key")
        val projectId = jsonObject.getString("project_id")

        val jwt = generateJwt(clientEmail, privateKey)
        getAccessToken(jwt) { accessToken ->
            if (accessToken != null) {
                postToFcm(accessToken, targetToken, title, body, projectId)
            } else {
                Log.e(TAG, "Access token is null.")
            }
        }
    }

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
                response.use {
                    if (it.isSuccessful) {
                        val res = JSONObject(it.body!!.string())
                        callback(res.getString("access_token"))
                    } else {
                        Log.e(TAG, "Access token error: ${it.code}")
                        callback(null)
                    }
                }
            }
        })
    }

    private fun postToFcm(
        accessToken: String,
        targetToken: String,
        title: String,
        body: String,
        projectId: String
    ) {
        val client = OkHttpClient()
        val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

        val json = JSONObject()
        json.put("message", JSONObject().apply {
            put("token", targetToken)
            put("notification", JSONObject().apply {
                put("title", title)
                put("body", body)
            })
        })

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create("application/json".toMediaType(), json.toString()))
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Send failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.i(TAG, "FCM response: ${response.code}, ${response.body?.string()}")
            }
        })
    }
}