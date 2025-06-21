package com.example.falldetectorapp.activities

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
import com.google.gson.Gson
import com.example.falldetectorapp.fcm.FCMSender
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.sqrt
import com.example.falldetectorapp.models.AccidentHistory

class AlarmsActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var timer: Timer? = null
    private var saveTimer: Timer? = null
    private var fakeSensorTimer: Timer? = null

    private lateinit var accText: TextView
    private lateinit var gyroText: TextView
    private lateinit var statusText: TextView
    private lateinit var backButton: ImageButton

    private var accX = 0f
    private var accY = 0f
    private var accZ = 0f
    private var gyroX = 0f
    private var gyroY = 0f
    private var gyroZ = 0f

    private var isAccidentActivityRunning = false
    private var simulateSensorData = false // do testów

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarms)

        accText = findViewById(R.id.accText)
        gyroText = findViewById(R.id.gyroText)
        statusText = findViewById(R.id.statusText)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener { finish() }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    override fun onResume() {
        super.onResume()
        isAccidentActivityRunning = false

//        if (!simulateSensorData) {
//            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
//            gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
//        }

        if (!simulateSensorData) {
            accelerometer?.also {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
        gyroscope?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }



        // dodane z commit " komunikat o upadku"
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
                                val currentUserId = auth.currentUser?.uid

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

        saveTimer = timer(period = 1000) {
            val userId = auth.currentUser?.uid ?: return@timer
            val sensorData = hashMapOf(
                "userId" to userId,
                "acc_x" to accX.toString(),
                "acc_y" to accY.toString(),
                "acc_z" to accZ.toString(),
                "gyro_x" to gyroX.toString(),
                "gyro_y" to gyroY.toString(),
                "gyro_z" to gyroZ.toString(),
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("sensors")
                .add(sensorData)
                .addOnSuccessListener {
                    Log.d("Firestore", "Zapisano dane czujników.")
                }
                .addOnFailureListener {
                    Log.e("Firestore", "Błąd zapisu: ${it.message}")
                }
        }

        if (simulateSensorData) startFakeSensorData()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        timer?.cancel()
        saveTimer?.cancel()
        fakeSensorTimer?.cancel()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accX = it.values[0]
                    accY = it.values[1]
                    accZ = it.values[2]
                    accText.text = "Akcelerometr:\nX: $accX\nY: $accY\nZ: $accZ"

                    val magnitude = sqrt(accX * accX + accY * accY + accZ * accZ)
                    if (magnitude > 12) {
                        sendPushNotificationToCaretaker()
                        launchAccidentActivity()
                    }
                }

                Sensor.TYPE_GYROSCOPE -> {
                    gyroX = it.values[0]
                    gyroY = it.values[1]
                    gyroZ = it.values[2]
                    gyroText.text = "Żyroskop:\nX: $gyroX\nY: $gyroY\nZ: $gyroZ"
                }
            }
        }
    }

    private fun launchAccidentActivity() {
        if (!isAccidentActivityRunning) {
            isAccidentActivityRunning = true

            val accidentId = UUID.randomUUID().toString()
            val userId = auth.currentUser?.uid ?: return

            val history = AccidentHistory(
                uid = accidentId,
                userId = userId,
                wasRejected = false,
                timestamp = System.currentTimeMillis()
            )

            db.collection("accident_history")
                .document(accidentId)
                .set(history)
                .addOnSuccessListener {
                    Log.d("Firestore", "Dodano wpis o upadku: $accidentId")

                    val intent = Intent(this, AccidentActivity::class.java)
                    intent.putExtra("accidentId", accidentId)
                    startActivity(intent)
                }
                .addOnFailureListener {
                    Log.e("Firestore", "Nie udało się zapisać upadku: ${it.message}")
                }
        }
    }

    private fun startFakeSensorData() {
        fakeSensorTimer = timer(period = 1000) {
            accX = (-2..2).random() + Math.random().toFloat()
            accY = (-2..2).random() + Math.random().toFloat()
            accZ = (8..11).random() + Math.random().toFloat()

            val magnitude = sqrt(accX * accX + accY * accY + accZ * accZ)

            runOnUiThread {
                accText.text = "Akcelerometr (symulacja):\nX: $accX\nY: $accY\nZ: $accZ"
            }

            if (magnitude > 11 && !isAccidentActivityRunning) {
                runOnUiThread { launchAccidentActivity() }
            }
        }
    }

    private fun sendPushNotificationToCaretaker() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { seniorDoc ->
                val supervisors = seniorDoc.get("supervisors") as? List<String> ?: return@addOnSuccessListener
                for (supervisorUid in supervisors) {
                    db.collection("users").document(supervisorUid).get()
                        .addOnSuccessListener { supervisorDoc ->
                            val fcmToken = supervisorDoc.getString("fcmToken") ?: return@addOnSuccessListener
                            // Wywołujemy FCMSender zamiast własnej metody HTTP
                            FCMSender.sendNotification(
                                this@AlarmsActivity,
                                fcmToken,
                                "Wykryto upadek!",
                                "Twój podopieczny może potrzebować pomocy."
                            )
                        }
                }
            }
    }

//    private fun sendFCMRequest(data: Map<String, Any>) {
//        val fcmServerKey = "AAAAXXX:XXXXXXXXXXXXXXXXX" // Uwaga: KLUCZ KLUCZ KLUCZ.
//
//        val client = OkHttpClient()
//        val json = Gson().toJson(data)
//
//        val request = Request.Builder()
//            .url("https://fcm.googleapis.com/fcm/send")
//            .post(RequestBody.create("application/json".toMediaType(), json))
//            .addHeader("Authorization", "key=$fcmServerKey")
//            .addHeader("Content-Type", "application/json")
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e("FCM", "Błąd: ${e.message}")
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                Log.d("FCM", "Sukces: ${response.body?.string()}")
//            }
//        })
//    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}