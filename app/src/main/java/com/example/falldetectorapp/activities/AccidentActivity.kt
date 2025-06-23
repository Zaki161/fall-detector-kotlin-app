package com.example.falldetectorapp.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.falldetectorapp.R
import com.example.falldetectorapp.fcm.FCMSender
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Ekran potwierdzający upadek wykryty przez system.
 *
 * Użytkownik ma 5 sekund na odrzucenie zgłoszenia, zanim zostanie wysłane powiadomienie
 * do opiekunów informujące o potencjalnym upadku.
 */

class AccidentActivity : AppCompatActivity() {

    private lateinit var rejectButton: TextView
    private val db = FirebaseFirestore.getInstance()
    private var accidentId: String? = null

    /** Handler odpowiedzialny za opóźnione zamknięcie aktywności. */
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Runnable, który po 5 sekundach:
     * - aktualizuje rekord upadku jako autozamknięty,
     * - pobiera opiekunów użytkownika,
     * - wysyła im powiadomienie push.
     */
    private val dismissRunnable = Runnable {
        accidentId?.let { accidentId ->
            db.collection("accident_history").document(accidentId)
                .update("wasAutoClosed", true)

            // Pobierz UID seniora
            db.collection("accident_history").document(accidentId).get()
                .addOnSuccessListener { doc ->
                    val seniorUid = doc.getString("userId") ?: return@addOnSuccessListener

                    // Pobierz dokument seniora
                    db.collection("users").document(seniorUid).get()
                        .addOnSuccessListener { userDoc ->
                            val supervisedBy = userDoc.get("supervisedBy") as? List<String> ?: return@addOnSuccessListener

                            // Dla każdego opiekuna
                            for (supervisorUid in supervisedBy) {
                                db.collection("users").document(supervisorUid).get()
                                    .addOnSuccessListener { supDoc ->
                                        val token = supDoc.getString("fcmToken")
                                        val nick = userDoc.getString("nick") ?: "senior"

                                        if (!token.isNullOrBlank()) {
                                            FCMSender.sendNotification(
                                                this,
                                                token,
                                                "ALERT",
                                                "Upadek! Użytkownik $nick upadł, sprawdź to!"
                                            ) { success, message ->
                                                runOnUiThread {
                                                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                }
        }

        finish()
    }
    /**
     * Inicjalizuje aktywność, pobiera accidentId z intentu i ustawia przycisk odrzucenia.
     * Jeśli użytkownik nie zareaguje w ciągu 5 sekund, uruchamiany jest `dismissRunnable`.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accident)

        rejectButton = findViewById(R.id.rejectButton)
        accidentId = intent.getStringExtra("accidentId")

        val accidentId = intent.getStringExtra("accidentId")
        if (accidentId == null) {
            Log.e("AccidentActivity", "Błąd: brak accidentId")
            finish() // lub wróć do poprzedniej aktywności
            return
        }

        Log.d("AccidentActivity", "Odebrano accidentId: $accidentId")

        if (accidentId == null) {
            Toast.makeText(this, "Błąd: brak accidentId", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        rejectButton.setOnClickListener {
            handler.removeCallbacks(dismissRunnable)
            db.collection("accident_history").document(accidentId!!)
                .update("wasRejected", true)
                .addOnSuccessListener {
                    Toast.makeText(this, "Zgłoszenie odrzucone", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Błąd: ${it.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }

        handler.postDelayed(dismissRunnable, 5000)
    }
    /**
     * Usuwa zaplanowane akcje, gdy aktywność jest niszczona.
     */
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(dismissRunnable)
    }
}