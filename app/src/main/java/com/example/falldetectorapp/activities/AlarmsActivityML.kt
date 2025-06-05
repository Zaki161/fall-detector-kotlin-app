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
import com.example.falldetectorapp.ml.RFClassifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.pow
import kotlin.math.sqrt

class AlarmsActivityML : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private lateinit var rfClassifier: RFClassifier

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

    private var lastAccMagnitude = 0.0
    private var lastGyroMagnitude = 0.0
    private var isAccidentActivityRunning = false

    private var simulateSensorData = false
    private var fakeSensorTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarms)

        accText = findViewById(R.id.accText)
        gyroText = findViewById(R.id.gyroText)
        statusText = findViewById(R.id.statusText)
        backButton = findViewById(R.id.backButton)

        rfClassifier = RFClassifier(this)

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

        if (!simulateSensorData) {
            accelerometer?.also {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            gyroscope?.also {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }

        timer = timer(period = 10_000) {
            val currentUserId = auth.currentUser?.uid ?: return@timer
            db.collection("sensors")
                .whereEqualTo("userId", currentUserId)
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

            db.collection("sensors").add(sensorData)
        }

        if (simulateSensorData) {
            startFakeSensorData()
        }
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
                    accX = it.values[0].toString()
                    accY = it.values[1].toString()
                    accZ = it.values[2].toString()
                    accText.text = "Akcelerometr:\nX: $accX\nY: $accY\nZ: $accZ"

                    val accMagnitude = sqrt(
                        it.values[0].toDouble().pow(2) +
                                it.values[1].toDouble().pow(2) +
                                it.values[2].toDouble().pow(2)
                    )

                    val accDiff = accMagnitude - lastAccMagnitude
                    lastAccMagnitude = accMagnitude

                    val gyroMagnitude = sqrt(
                        gyroX.toDouble().pow(2) +
                                gyroY.toDouble().pow(2) +
                                gyroZ.toDouble().pow(2)
                    )
                    val gyroDiff = gyroMagnitude - lastGyroMagnitude
                    lastGyroMagnitude = gyroMagnitude

                    val features = floatArrayOf(
                        accMagnitude.toFloat(),
                        gyroMagnitude.toFloat(),
                        accDiff.toFloat(),
                        gyroDiff.toFloat(),
                        accMagnitude.toFloat(),  // TEMP std
                        gyroMagnitude.toFloat(), // TEMP std
                        25f,   // age
                        170f,  // height
                        70f,   // weight
                        0f     // gender
                    )

                    val prediction = rfClassifier.predict(features)

                    if (prediction >= 0.5 && !isAccidentActivityRunning) {
                        runOnUiThread {
                            launchAccidentActivity()
                        }
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
            val fakeAccX = (-2..2).random() + Math.random()
            val fakeAccY = (-2..2).random() + Math.random()
            val fakeAccZ = (8..11).random() + Math.random()

            accX = String.format("%.2f", fakeAccX)
            accY = String.format("%.2f", fakeAccY)
            accZ = String.format("%.2f", fakeAccZ)

            val magnitude = sqrt(fakeAccX * fakeAccX + fakeAccY * fakeAccY + fakeAccZ * fakeAccZ)

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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}