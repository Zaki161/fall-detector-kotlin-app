/*
Logowanie analogicznie do roli
 */
package com.example.falldetectorapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.falldetectorapp.activities.MainActivity
import com.example.falldetectorapp.activities.RegisterActivity
import com.example.falldetectorapp.activities.SupervisorActivity
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.auth.FirebaseAuth
import com.example.falldetectorapp.R


class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val isSenior = document.getBoolean("senior") ?: true
                        val targetActivity = if (isSenior) MainActivity::class.java else SupervisorActivity::class.java
                        startActivity(Intent(this, targetActivity))
                        finish()
                    }
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailField = findViewById<EditText>(R.id.emailEditText)
        val passwordField = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerRedirect = findViewById<Button>(R.id.goToRegisterButton)

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                        val db = FirebaseFirestore.getInstance()

                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val isSenior = document.getBoolean("senior") ?: true // domyślnie senior

                                    val targetActivity = if (isSenior) {
                                        MainActivity::class.java
                                    } else {
                                        SupervisorActivity::class.java
                                    }

                                    startActivity(Intent(this, targetActivity))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Nie znaleziono danych użytkownika", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Błąd pobierania danych", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Logowanie nie powiodło się", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        registerRedirect.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}