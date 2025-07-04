package com.example.falldetectorapp.activities
/**
 * Aktywność `ContactActivity` wyświetla listę kontaktów (opiekunów) przypisanych do seniora.
 * Dane pobierane są z Firestore na podstawie tokena seniora (`seniorToken`).
 *
 * Użytkownik może zobaczyć swój unikalny token oraz listę opiekunów, którzy go nadzorują.
 */
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.falldetectorapp.R
import com.example.falldetectorapp.adapters.ContactAdapter
import com.example.falldetectorapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ContactActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView // widok listy
    private val contacts = mutableListOf<User>() // lista kontaktow
    private lateinit var adapter: ContactAdapter // adapter do wyswietlania listy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val backArrowButton = findViewById<ImageButton>(R.id.backButton)
//        val tokenTextView = findViewById<TextView>(R.id.TokenTextView)f
        recyclerView = findViewById(R.id.contactRecyclerView)

        adapter = ContactAdapter(contacts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        backArrowButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        loadContacts()
    }
    /**
     * Ładuje dane kontaktowe z Firestore:
     * - pobiera token seniora aktualnie zalogowanego użytkownika,
     * - wyszukuje opiekunów, którzy mają przypisany ten token w swoim polu `supervising`,
     * - wyświetla ich na liście w RecyclerView.
     */
    private fun loadContacts() {
        val uid = auth.currentUser?.uid ?: return
        val tokenTextView = findViewById<TextView>(R.id.TokenTextView)

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val currentUser = document.toObject(User::class.java) ?: return@addOnSuccessListener
                val token = currentUser.seniorToken ?: return@addOnSuccessListener

                runOnUiThread {
                    tokenTextView.text = "Twój token: $token"
                }

                db.collection("users")
                    .whereArrayContains("supervising", token)
                    .get()
                    .addOnSuccessListener { result ->
                        contacts.clear()
                        for (doc in result) {
                            val supervisor = doc.toObject(User::class.java)
                            contacts.add(supervisor)
                        }
                        adapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Błąd ładowania kontaktów", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd ładowania danych użytkownika", Toast.LENGTH_SHORT).show()
            }
    }
}