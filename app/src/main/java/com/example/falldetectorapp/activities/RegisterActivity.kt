package com.example.falldetectorapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.falldetectorapp.R
import com.example.falldetectorapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val emailField = findViewById<EditText>(R.id.emailEditText)
        val passwordField = findViewById<EditText>(R.id.passwordEditText)
        val nickField = findViewById<EditText>(R.id.nickEditText)
        val phoneField = findViewById<EditText>(R.id.phoneEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginRedirect = findViewById<Button>(R.id.goToLoginButton)
        val roleGroup = findViewById<RadioGroup>(R.id.roleRadioGroup)

        registerButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString()
            val nick = nickField.text.toString().trim()
            val phone = phoneField.text.toString().trim()
            val selectedRoleId = roleGroup.checkedRadioButtonId
            val senior = selectedRoleId == R.id.seniorRadioButton

            if (selectedRoleId == -1) {
                Toast.makeText(this, "Wybierz rolę użytkownika", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isBlank() || password.isBlank() || nick.isBlank() || phone.isBlank()) {
                Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                        val seniorToken = if (senior) generateToken() else null

                        val user = User(
                            uid = uid,
                            mail = email,
                            nick = nick,
                            phone = phone,
                            password = password,
                            senior = senior,
                            seniorToken = seniorToken,
                            supervising = listOf()
                        )

                        val db = FirebaseFirestore.getInstance()
                        db.collection("users").document(uid).set(user)
                            .addOnSuccessListener {
                                val targetActivity = if (senior) {
                                    MainActivity::class.java
                                } else {
                                    SupervisorActivity::class.java
                                }
                                startActivity(Intent(this, targetActivity))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Błąd zapisu danych: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Log.e("REGISTER", "Registration failed", task.exception)
                        Toast.makeText(this, "Rejestracja nieudana: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun generateToken(length: Int = 6): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
}