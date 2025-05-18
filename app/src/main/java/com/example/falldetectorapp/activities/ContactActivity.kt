package com.example.falldetectorapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.falldetectorapp.R
import com.example.falldetectorapp.adapters.ContactAdapter
import com.example.testapplication.models.ContactPerson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class ContactActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private val contacts = mutableListOf<ContactPerson>()
    private lateinit var adapter: ContactAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            val uid = user.uid
            // używaj uid wszędzie gdzie potrzebujesz
        } else {
            // użytkownik NIE jest zalogowany → przekieruj do LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val backButton = findViewById<Button>(R.id.backButton)
        val addContactButton = findViewById<Button>(R.id.addContactButton)
        recyclerView = findViewById(R.id.contactRecyclerView)

        adapter = ContactAdapter(contacts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        backButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        addContactButton.setOnClickListener {
            showAddContactDialog()
        }

        loadContacts()
    }

    private fun loadContacts() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("contacts")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->
                contacts.clear()
                for (document in result) {
                    val contact = document.toObject(ContactPerson::class.java)
                    contacts.add(contact)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd pobierania kontaktów", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddContactDialog() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val nameInput = EditText(this).apply { hint = "Imię i nazwisko" }
        val emailInput = EditText(this).apply { hint = "Email" }
        val phoneInput = EditText(this).apply { hint = "Numer telefonu" }

        dialogLayout.addView(nameInput)
        dialogLayout.addView(emailInput)
        dialogLayout.addView(phoneInput)

        AlertDialog.Builder(this)
            .setTitle("Dodaj osobę kontaktową")
            .setView(dialogLayout)
            .setPositiveButton("Dodaj") { _, _ ->
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Toast.makeText(this, "Użytkownik niezalogowany", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val uid = currentUser.uid
                val name = nameInput.text.toString()
                val email = emailInput.text.toString()
                val phone = phoneInput.text.toString()

                if (name.isBlank() || email.isBlank() || phone.isBlank()) {
                    Toast.makeText(this, "Wszystkie pola są wymagane", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val contactId = db.collection("contacts").document().id
                val contact = ContactPerson(
                    id = contactId,
                    userId = uid,
                    name = name,
                    email = email,
                    phoneNumber = phone
                )

                db.collection("contacts").document(contactId).set(contact)
                    .addOnSuccessListener {
                        Log.d("FIREBASE", "Kontakt dodany!")
                        loadContacts()
                        Toast.makeText(this, "Kontakt dodany!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("FIREBASE", "Błąd zapisu: ${e.message}")
                        Toast.makeText(this, "Błąd zapisu", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
}