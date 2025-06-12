/* zdazenia po wykryciu wypadku */

package com.example.falldetectorapp.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.falldetectorapp.R
import com.google.firebase.firestore.FirebaseFirestore

//class AccidentActivity : AppCompatActivity() {
//
//    private lateinit var rejectButton: TextView
//    private val handler = Handler(Looper.getMainLooper())
//    private val dismissRunnable = Runnable {
//        finish() // automatyczne zamknięcie po 5 sek
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_accident)
//
//        rejectButton = findViewById(R.id.rejectButton)
//
//        rejectButton.setOnClickListener {
//            handler.removeCallbacks(dismissRunnable)
//            finish() // wróć do poprzedniego (np. MainActivity)
//        }
//
//        // odpal timer 5 sekundowy
//        handler.postDelayed(dismissRunnable, 5000)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        handler.removeCallbacks(dismissRunnable)
//    }
//}


class AccidentActivity : AppCompatActivity() {

    private lateinit var rejectButton: TextView
    private val db = FirebaseFirestore.getInstance()
    private var accidentId: String? = null


    private val handler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable {
        finish() // automatyczne zamknięcie po 5 sekundach
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accident)

        rejectButton = findViewById(R.id.rejectButton)
        accidentId = intent.getStringExtra("accidentId")

        rejectButton.setOnClickListener {
            handler.removeCallbacks(dismissRunnable) // zatrzymaj auto-zamykanie
            accidentId?.let {
                db.collection("accident_history").document(it)
                    .update("wasRejected", true)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Zgłoszenie odrzucone", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Błąd odrzucenia: ${e.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            } ?: run {
                Toast.makeText(this, "Brak accidentId", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Ustaw timer na automatyczne zamknięcie
        handler.postDelayed(dismissRunnable, 5000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(dismissRunnable)
    }
}