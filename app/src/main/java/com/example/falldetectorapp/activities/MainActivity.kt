/*
Strona glowna po zalogowaniu !!! */

package com.example.falldetectorapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.falldetectorapp.R
import com.example.falldetectorapp.activities.LoginActivity
import com.example.falldetectorapp.activities.ContactActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.testapplication.models.User

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            val uid = user.uid
            // używaj uid wszędzie gdzie potrzebujesz
        } else {
            // użytkownik NIE jest zalogowany → przekieruj do LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

//        val inputField = findViewById<EditText>(R.id.dataEditText)
        val contactsButton = findViewById<Button>(R.id.contactsButton)
        val yourDataButton = findViewById<Button>(R.id.dataButton)
        val alarmsButton: Button = findViewById(R.id.alarmsButton)

        val displayText = findViewById<TextView>(R.id.dataTextView)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        val uid = auth.currentUser?.uid

//        saveButton.setOnClickListener {
//            val text = inputField.text.toString()
//            val data = hashMapOf("note" to text)
//
//            uid?.let {
//                db.collection("users").document(uid).set(data)
//                    .addOnSuccessListener {
//                        displayText.text = "Zapisano: $text"
//                    }
//            }
//        }

        uid?.let {
            db.collection("users").document(it).get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(User::class.java)
                    displayText.text = "Hi ${user?.nick}!"
                }
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