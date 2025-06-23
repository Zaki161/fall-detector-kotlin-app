package com.example.falldetectorapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.falldetectorapp.R
import com.example.falldetectorapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Ekran główny aplikacji dla użytkownika typu senior.
 *
 * Umożliwia dostęp do kontaktów, danych osobowych, podgladu sensorow oraz obsługuje wylogowanie.
 * Automatycznie aktualizuje token FCM i wyświetla nick zalogowanego użytkownika.
 */
class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentUser = auth.currentUser
        // Jesli nie zalogownay to wroc do logowania
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val contactsButton = findViewById<Button>(R.id.contactsButton)
        val yourDataButton = findViewById<Button>(R.id.dataButton)
        val alarmsButton = findViewById<Button>(R.id.alarmsButton)
        val displayText = findViewById<TextView>(R.id.dataTextView)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        // !!!! Aktualizacja tokena FCM, aby mieć zawsze aktualny identyfikator urządzenia do powiadomień
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            currentUser.uid.let { uid ->
                db.collection("users").document(uid)
                    .update("fcmToken", token)
            }
        }

        // Wyświetlenie nicku
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                displayText.text = "Hi ${user?.nick}!"
            }

        contactsButton.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
        }

        yourDataButton.setOnClickListener {
            startActivity(Intent(this, YourDataActivity::class.java))
        }

        alarmsButton.setOnClickListener {
            startActivity(Intent(this, AlarmsActivity::class.java))
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}