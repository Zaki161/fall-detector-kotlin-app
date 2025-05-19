package com.example.falldetectorapp.activities

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.falldetectorapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Timer
import com.example.testapplication.models.SensorsData
//import com.example.testapplication.models.User
import kotlin.concurrent.timer


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
    private lateinit var backButton: ImageButton

    private var accX = ""
    private var accY = ""
    private var accZ = ""
    private var gyroX = ""
    private var gyroY = ""
    private var gyroZ = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarms)

        accText = findViewById(R.id.accText)
        gyroText = findViewById(R.id.gyroText)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish() // lub: startActivity(Intent(this, MainActivity::class.java))
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        timer = timer(period = 10_000) { // co 10 sekund
            val tenSecondsAgo = System.currentTimeMillis() - 10_000

            db.collection("sensors")
                .whereLessThan("timestamp", tenSecondsAgo)
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        db.collection("sensors").document(doc.id).delete()
                    }
                }
        }
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
                .addOnSuccessListener { /* optional */ }
                .addOnFailureListener { e -> e.printStackTrace() }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        timer?.cancel()
        saveTimer?.cancel()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accX = it.values[0].toString()
                    accY = it.values[1].toString()
                    accZ = it.values[2].toString()
                    accText.text = "Akcelerometr:\nX: $accX\nY: $accY\nZ: $accZ"

                    // Wektorowe przyspieszenie
                    val magnitude = Math.sqrt(
                        (it.values[0] * it.values[0] +
                                it.values[1] * it.values[1] +
                                it.values[2] * it.values[2]).toDouble()
                    )

                    if (magnitude > 25) { // wartość progowa – dopasuj według testów
                        startActivity(Intent(this, AccidentActivity::class.java))
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}