package com.example.falldetectorapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.falldetectorapp.activities.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.falldetectorapp.R
import android.util.Log
import com.example.testapplication.models.User
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val emailField = findViewById<EditText>(R.id.emailEditText)
        val passwordField = findViewById<EditText>(R.id.passwordEditText)
        val nickField= findViewById<EditText>(R.id.nickEditText)
        val phoneField= findViewById<EditText>(R.id.phoneEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginRedirect = findViewById<Button>(R.id.goToLoginButton)



        registerButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            val nick =nickField.text.toString()
            val phone =phoneField.text.toString()

            if (email.isBlank() || password.isBlank() || nick.isBlank() || phone.isBlank()) {
                Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
//                        val nick= "Jan Kowalski" // TODO: zamień na input z UI
//                        val phone = "123456789" // TODO: zamień na input z UI

                        val user = User(uid = uid, mail = email, nick = nick, phone = phone)

                        val db = FirebaseFirestore.getInstance()
                        db.collection("users").document(uid).set(user)
                            .addOnSuccessListener {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Błąd zapisu danych", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Log.e("REGISTRATION", "Registration failed", it.exception)
                        Toast.makeText(this, "Registration failed: ${it.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}