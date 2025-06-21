/*
Dodawanie/ Aktualizowanie danych uzytkownika
 */

package com.example.falldetectorapp.activities
import android.os.Bundle
import android.widget.Button
import android.content.Intent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.falldetectorapp.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.example.falldetectorapp.models.User


class YourDataActivity : AppCompatActivity() {

    private lateinit var heightEditText: EditText
    private lateinit var ageEditText: EditText
    private lateinit var weightEditText: EditText
    private lateinit var saveButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Przykład: można przechowywać dane lokalnie lub w Firestore. Na początek lokalnie:
    private var userData = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_your_data)

        heightEditText = findViewById(R.id.heightEditText)
        ageEditText = findViewById(R.id.ageEditText)
        weightEditText = findViewById(R.id.weightEditText)
        saveButton = findViewById(R.id.saveButton)

        loadUserData()
        val backArrowButton = findViewById<ImageButton>(R.id.backButton)
        backArrowButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        saveButton.setOnClickListener {
            saveUserData()
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData = document.toObject(User::class.java) ?: User()
                        heightEditText.setText(userData.height.toString())
                        ageEditText.setText(userData.age.toString())
                        weightEditText.setText(userData.weight.toString())
                    } else {
                        Toast.makeText(this, "Brak danych użytkownika", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Błąd pobierania danych", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveUserData() {
        val heightStr = heightEditText.text.toString()
        val ageStr = ageEditText.text.toString()
        val weightStr = weightEditText.text.toString()

        if (heightStr.isBlank() || ageStr.isBlank() || weightStr.isBlank()) {
            Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
            return
        }

        userData.height = heightStr.toInt()
        userData.age = ageStr.toInt()
        userData.weight = weightStr.toInt()

        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "height" to userData.height,
                        "age" to userData.age,
                        "weight" to userData.weight
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(this, "Dane zapisane w Firebase!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Błąd zapisu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Użytkownik niezalogowany", Toast.LENGTH_SHORT).show()
        }
    }
}