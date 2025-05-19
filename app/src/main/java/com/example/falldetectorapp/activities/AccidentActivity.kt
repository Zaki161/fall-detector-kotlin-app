package com.example.falldetectorapp.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.falldetectorapp.R

class AccidentActivity : AppCompatActivity() {

    private lateinit var rejectButton: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable {
        finish() // automatyczne zamknięcie po 5 sek
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accident)

        rejectButton = findViewById(R.id.rejectButton)

        rejectButton.setOnClickListener {
            handler.removeCallbacks(dismissRunnable)
            finish() // wróć do poprzedniego (np. MainActivity)
        }

        // odpal timer 5 sekundowy
        handler.postDelayed(dismissRunnable, 5000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(dismissRunnable)
    }
}