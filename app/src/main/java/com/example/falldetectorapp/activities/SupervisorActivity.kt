/* Obsluga strony opiekuna */
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

class SupervisorActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor)

        val displayText = findViewById<TextView>(R.id.dataTextView)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        val currentUser = auth.currentUser

        if (currentUser == null) {
            // Użytkownik niezalogowany — przekieruj do logowania
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val uid = currentUser.uid

        // Pobranie danych użytkownika
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    displayText.text = "Witaj, ${user?.nick ?: "Opiekunie"}!"
                } else {
                    displayText.text = "Nie znaleziono danych użytkownika."
                }
            }
            .addOnFailureListener {
                displayText.text = "Błąd pobierania danych."
            }

        // Wylogowanie
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}

//        contactsButton.setOnClickListener {
//            startActivity(Intent(this, ContactActivity::class.java))
//        }