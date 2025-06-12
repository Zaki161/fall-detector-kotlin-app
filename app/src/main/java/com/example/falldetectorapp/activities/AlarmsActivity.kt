

package com.example.falldetectorapp.activities

import android.app.VoiceInteractor
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.falldetectorapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Timer
import kotlin.concurrent.timer

import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

class AlarmsActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var timer: Timer? = null
    private var saveTimer: Timer? = null

    private lateinit var accText: TextView
    private lateinit var gyroText: TextView
    private lateinit var statusText: TextView
    private lateinit var backButton: ImageButton

    private var accX = ""
    private var accY = ""
    private var accZ = ""
    private var gyroX = ""
    private var gyroY = ""
    private var gyroZ = ""

//    TESTY
    private var simulateSensorData = false // <-- Zmienna sterująca symulacją
    private var fakeSensorTimer: Timer? = null

    private var isAccidentActivityRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarms)

        accText = findViewById(R.id.accText)
        gyroText = findViewById(R.id.gyroText)
        statusText = findViewById(R.id.statusText)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    override fun onResume() {
        super.onResume()
        isAccidentActivityRunning = false

        // TESTY
        if (!simulateSensorData) {
            accelerometer?.also {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }

        }
        gyroscope?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Co 10 sek – usuwa stare dane
        // Co 5 sek – usuwa stare dane
        timer = timer(period = 10_000) {
            val tenSecondsAgo = System.currentTimeMillis() - 5_000
            val currentUserId = auth.currentUser?.uid ?: return@timer

            db.collection("sensors")
                .whereLessThan("timestamp", tenSecondsAgo)
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        runOnUiThread {
                            statusText.text = "Brak starych danych do usunięcia"
                        }
                    } else {
                        for (doc in documents) {
                            val docRef = db.collection("sensors").document(doc.id)
                            docRef.get().addOnSuccessListener { snapshot ->
                                val userId = snapshot.getString("userId")
//                                val currentUserId = auth.currentUser?.uid

                                if (userId == currentUserId) {
                                    docRef.delete()
                                        .addOnSuccessListener {
                                            runOnUiThread {
                                                statusText.text = "Usunięto: ${doc.id}"
                                            }
                                            Log.d("AlarmsActivity", "Usunięto dokument ${doc.id}")
                                        }
                                        .addOnFailureListener { e ->
                                            runOnUiThread {
                                                statusText.text = "Błąd usuwania: ${e.message}"
                                            }
                                            Log.e("AlarmsActivity", "Błąd usuwania: ${e.message}", e)
                                        }
                                } else {
                                    runOnUiThread {
                                        statusText.text = "Brak uprawnień do usunięcia: ${doc.id}"
                                    }
                                    Log.w("AlarmsActivity", "Próba usunięcia cudzych danych: ${doc.id}")
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    runOnUiThread {
                        statusText.text = "Błąd pobierania JILjil danych: ${e.message}"

                    }
                    Log.e("AlarmsActivity", "Błąd pobierania dokumentów: ${e.localizedMessage}", e)
                }
        }

        // Co 1 sek – zapisuje nowe dane
        saveTimer = timer(period = 1000) {
            val userId = auth.currentUser?.uid ?: return@timer
            val sensorDataId = db.collection("sensors").document().id

            val sensorData = hashMapOf(
                "id" to sensorDataId,
                "userId" to userId,
                "acc_x" to accX,
                "acc_y" to accY,
                "acc_z" to accZ,
                "gyro_x" to gyroX,
                "gyro_y" to gyroY,
                "gyro_z" to gyroZ,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("sensors")
                .add(sensorData)
                .addOnSuccessListener {
                    runOnUiThread {
                        statusText.text = "Zapisano: $sensorDataId"
                    }
                    Log.d("AlarmsActivity", "Dodano dokument: $sensorDataId")
                }
                .addOnFailureListener { e ->
                    runOnUiThread {
                        statusText.text = "Błąd zapisu: ${e.message}"
                    }
                    Log.e("AlarmsActivity", "Błąd zapisu: ${e.message}", e)
                }
        }
        // TESTY
        if (simulateSensorData) {
            startFakeSensorData()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        timer?.cancel()
        saveTimer?.cancel()
        fakeSensorTimer?.cancel() // TESTY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accX = it.values[0].toString()
                    accY = it.values[1].toString()
                    accZ = it.values[2].toString()
                    accText.text = "Akcelerometr:\nX: $accX\nY: $accY\nZ: $accZ"

                    val magnitude = Math.sqrt(
                        (it.values[0] * it.values[0] +
                                it.values[1] * it.values[1] +
                                it.values[2] * it.values[2]).toDouble()
                    )
                    if (magnitude > 25) {
                        sendPushNotificationToCaretaker()
                        launchAccidentActivity()
//                        startActivity(Intent(this, AccidentActivity::class.java))
                    }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroX = it.values[0].toString()
                    gyroY = it.values[1].toString()
                    gyroZ = it.values[2].toString()
                    gyroText.text = "Żyroskop:\nX: $gyroX\nY: $gyroY\nZ: $gyroZ"
                }
            }
        }
    }
    private fun launchAccidentActivity() {
        if (!isAccidentActivityRunning) {
            isAccidentActivityRunning = true
            startActivity(Intent(this, AccidentActivity::class.java))
        }
    }
    private fun startFakeSensorData() {
        fakeSensorTimer = timer(period = 1000) {
            if (!simulateSensorData) return@timer

            val fakeAccX = (-2..2).random() + Math.random()
            val fakeAccY = (-2..2).random() + Math.random()
            val fakeAccZ = (8..11).random() + Math.random() // grawitacja + ruch

            accX = String.format("%.2f", fakeAccX)
            accY = String.format("%.2f", fakeAccY)
            accZ = String.format("%.2f", fakeAccZ)

            val magnitude = Math.sqrt(
                fakeAccX * fakeAccX + fakeAccY * fakeAccY + fakeAccZ * fakeAccZ
            )

            runOnUiThread {
                accText.text = "Akcelerometr (symulacja):\nX: $accX\nY: $accY\nZ: $accZ"
            }

            if (magnitude > 12 && !isAccidentActivityRunning) {
                runOnUiThread {
                    launchAccidentActivity()
                }
            }
        }
    }
    private fun sendPushNotificationToCaretaker() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // Pobierz seniora (bieżącego użytkownika)
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { seniorDoc ->

                val supervisors = seniorDoc.get("supervisors") as? List<String> ?: emptyList()

                if (supervisors.isEmpty()) {
                    Log.w("FCM", "Senior nie ma przypisanych opiekunów")
                    return@addOnSuccessListener
                }

                // Pobierz tokeny FCM opiekunów i wyślij powiadomienie
                for (supervisorUid in supervisors) {
                    db.collection("users").document(supervisorUid).get()
                        .addOnSuccessListener { supervisorDoc ->
                            val fcmToken = supervisorDoc.getString("fcmToken")
                            if (!fcmToken.isNullOrEmpty()) {
                                val notificationData = mapOf(
                                    "to" to fcmToken,
                                    "notification" to mapOf(
                                        "title" to "Wykryto upadek!",
                                        "body" to "Twój podopieczny może potrzebować pomocy."
                                    )
                                )
                                sendFCMRequest(notificationData)
                            }
                        }
                }
            }
    }
    private fun sendFCMRequest(notificationData: Map<String, Any>) {
        val fcmServerKey = "AAAAXXX:XXXXXXXXXXXXXXXXX" // zamień na prawdziwy klucz serwera FCM

        val client = OkHttpClient()
        val json = Gson().toJson(notificationData)

        val body = RequestBody.create("application/json".toMediaType(), json)

        val request =Request.Builder()
            .url("https://fcm.googleapis.com/fcm/send")
            .post(body)
            .addHeader("Authorization", "key=$fcmServerKey")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FCM", "Błąd wysyłania: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("FCM", "Wysłano powiadomienie: ${response.body?.string()}")
            }
        })
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}


