package com.example.falldetectorapp.activities
/**
 * Aktywność odpowiedzialna za monitorowanie danych z sensorów (akcelerometr i żyroskop)
 * oraz wykrywanie potencjalnych upadków przy użyciu modelu uczenia maszynowego (RFClassifier).
 *
 * Dane z sensorów są zapisywane do Firestore, a po wykryciu upadku uruchamiana jest
 * aktywność potwierdzająca (`AccidentActivity`).
 */
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

    private val accMagnitudes = mutableListOf<Float>()
    private val gyroMagnitudes = mutableListOf<Float>()
    private val accDiffs = mutableListOf<Float>()
    private val gyroDiffs = mutableListOf<Float>()

    private val windowSize = 50

    private var userAge = 25f
    private var userHeight = 170f
    private var userWeight = 70f
    private var userGender = 0f  // 0 = Male, 1 = Female

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarms)

        accText = findViewById(R.id.accText)
        gyroText = findViewById(R.id.gyroText)
        statusText = findViewById(R.id.statusText)
        backButton = findViewById(R.id.backButton)

        rfClassifier = RFClassifier(this)

        userGender = 0f  // Zakładamy, że użytkownik to "Male"
        backButton.setOnClickListener { finish() }

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
        }
        gyroscope?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        val currentUser = auth.currentUser
        currentUser?.let {
            db.collection("users").document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userAge = (document.getLong("age") ?: 25).toFloat()
                        userHeight = (document.getLong("height") ?: 170).toFloat()
                        userWeight = (document.getLong("weight") ?: 70).toFloat()
                        // potem gender jeśli w bazie
                    }
                }
        }

        // Usuwanie dokumentów starszych niż x minuta co y sekund
        timer = timer(period = 30_000) {
            val oneMinuteAgo = System.currentTimeMillis() - 60_000
            val currentUserId = auth.currentUser?.uid ?: return@timer

            db.collection("sensors")
                .whereLessThan("timestamp", oneMinuteAgo)
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
                                if (userId == currentUserId) {
                                    docRef.delete()
                                        .addOnSuccessListener {
                                            runOnUiThread {
                                                statusText.text = "Usunięto dane: ${doc.id}"
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            runOnUiThread {
                                                statusText.text = "Błąd usuwania: ${e.message}"
                                            }
                                        }
                                } else {
                                    runOnUiThread {
                                        statusText.text = "Brak uprawnień do usunięcia: ${doc.id}"
                                    }
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    runOnUiThread {
                        statusText.text = "Błąd pobierania danych: ${e.message}"
                    }
                }
        }

        // Zapis danych co 1 sekundę z pokazaniem statusu
        saveTimer = timer(period = 500) {
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
                }
                .addOnFailureListener { e ->
                    runOnUiThread {
                        statusText.text = "Błąd zapisu: ${e.message}"
                    }
                }
        }

        // symulowane dane ( do testow)
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

                    accMagnitudes.add(accMagnitude.toFloat())
                    accDiffs.add(accDiff.toFloat())
                    if (accMagnitudes.size > windowSize) accMagnitudes.removeAt(0)
                    if (accDiffs.size > windowSize) accDiffs.removeAt(0)

                    tryEvaluateModel()
                }

                Sensor.TYPE_GYROSCOPE -> {
                    gyroX = it.values[0].toString()
                    gyroY = it.values[1].toString()
                    gyroZ = it.values[2].toString()
                    gyroText.text = "Żyroskop:\nX: $gyroX\nY: $gyroY\nZ: $gyroZ"

                    val gyroMagnitude = sqrt(
                        it.values[0].toDouble().pow(2) +
                                it.values[1].toDouble().pow(2) +
                                it.values[2].toDouble().pow(2)
                    )

                    val gyroDiff = gyroMagnitude - lastGyroMagnitude
                    lastGyroMagnitude = gyroMagnitude

                    gyroMagnitudes.add(gyroMagnitude.toFloat())
                    gyroDiffs.add(gyroDiff.toFloat())
                    if (gyroMagnitudes.size > windowSize) gyroMagnitudes.removeAt(0)
                    if (gyroDiffs.size > windowSize) gyroDiffs.removeAt(0)

                    tryEvaluateModel()
                }
            }
        }
    }

    private fun tryEvaluateModel() {
        if (accMagnitudes.size >= windowSize && gyroMagnitudes.size >= windowSize) {
            val features = floatArrayOf(
                accMagnitudes.average().toFloat(),
                gyroMagnitudes.average().toFloat(),
                accDiffs.average().toFloat(),
                gyroDiffs.average().toFloat(),
                accMagnitudes.standardDeviation(),
                gyroMagnitudes.standardDeviation(),
                userAge,
                userHeight,
                userWeight,
                userGender
            )

            val prediction = rfClassifier.predict(features)
            Log.d("ML", "Wynik predykcji: $prediction")

            if (prediction == 1.0f && !isAccidentActivityRunning) {
                runOnUiThread {
                    launchAccidentActivity()
                }
            }
        }
    }

    private fun List<Float>.standardDeviation(): Float {
        val mean = this.average()
        return sqrt(this.map { (it - mean).pow(2) }.average()).toFloat()
    }

    private fun launchAccidentActivity() {
        if (!isAccidentActivityRunning) {
            isAccidentActivityRunning = true

            val userId = auth.currentUser?.uid ?: return
            val accidentId = db.collection("accident_history").document().id

            val accidentHistory = hashMapOf(
                "uid" to accidentId,
                "userId" to userId,
                "wasRejected" to false,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("accident_history").document(accidentId)
                .set(accidentHistory)
                .addOnSuccessListener {
                    Log.d("AccidentHistory", "Zapisano wypadek")
                }
                .addOnFailureListener { e ->
                    Log.e("AccidentHistory", "Błąd zapisu: ${e.message}")
                }

            // Dodajemy accidentId do Intenta
            val intent = Intent(this, AccidentActivity::class.java)
            intent.putExtra("accidentId", accidentId)
            startActivity(intent)
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