package com.example.falldetectorapp.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.falldetectorapp.R
import com.google.firebase.firestore.FirebaseFirestore

class AccidentActivity : AppCompatActivity() {

    private lateinit var rejectButton: TextView
    private val db = FirebaseFirestore.getInstance()
    private var accidentId: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable {
        accidentId?.let {
            db.collection("accident_history").document(it)
                .update("wasAutoClosed", true)
        }
        finish()
    }

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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(dismissRunnable)
    }
}