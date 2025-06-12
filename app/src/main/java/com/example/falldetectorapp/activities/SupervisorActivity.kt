package com.example.falldetectorapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.falldetectorapp.R
import com.example.falldetectorapp.adapters.SeniorAdapter
import com.example.falldetectorapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class SupervisorActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var seniorList: RecyclerView
    private lateinit var adapter: SeniorAdapter
    private val seniors = mutableListOf<User>()
    private lateinit var addSeniorButton: Button
    private lateinit var logoutButton: Button
    private lateinit var welcomeText: TextView

    private lateinit var currentUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor)

        seniorList = findViewById(R.id.seniorRecyclerView)
        addSeniorButton = findViewById(R.id.addSeniorButton)
        logoutButton = findViewById(R.id.logoutButton)
        welcomeText = findViewById(R.id.dataTextView)

        adapter = SeniorAdapter(seniors)
        seniorList.layoutManager = LinearLayoutManager(this)
        seniorList.adapter = adapter

        loadSupervisorData()

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("fcmToken", token)
        }
        addSeniorButton.setOnClickListener {
            showAddSeniorDialog()
        }
    }

    private fun loadSupervisorData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(User::class.java)
                if (user != null) {
                    currentUser = user
                    welcomeText.text = "Witaj, ${user.nick}!"

                    loadSupervisedSeniors(user.supervising)
                }
            }
            .addOnFailureListener {
                welcomeText.text = "Błąd ładowania danych."
            }
    }

    private fun loadSupervisedSeniors(tokens: List<String>) {
        if (tokens.isEmpty()) {
            seniors.clear()
            adapter.notifyDataSetChanged()
            return
        }

        db.collection("users")
            .whereIn("seniorToken", tokens)
            .get()
            .addOnSuccessListener { result ->
                seniors.clear()
                for (doc in result) {
                    val senior = doc.toObject(User::class.java)
                    seniors.add(senior)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd ładowania seniorów", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddSeniorDialog() {
        val input = EditText(this).apply { hint = "Wprowadź token seniora" }

        AlertDialog.Builder(this)
            .setTitle("Dodaj seniora")
            .setView(input)
            .setPositiveButton("Dodaj") { _, _ ->
                val token = input.text.toString().uppercase().trim()

                if (token.isBlank()) {
                    Toast.makeText(this, "Token nie może być pusty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Sprawdź czy taki senior istnieje
                db.collection("users")
                    .whereEqualTo("seniorToken", token)
                    .get()
                    .addOnSuccessListener { query ->
                        if (query.isEmpty) {
                            Toast.makeText(this, "Nie znaleziono seniora", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val seniorDoc = query.documents[0]
                        val seniorToken = seniorDoc.getString("seniorToken")
                        if (seniorToken == null) {
                            Toast.makeText(this, "Senior nie ma przypisanego tokena", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Dodaj token do listy supervising, jeśli nie istnieje
                        if (!currentUser.supervising.contains(seniorToken)) {
                            val updatedList = currentUser.supervising + seniorToken

                            db.collection("users").document(currentUser.uid)
                                .update("supervising", updatedList)
                                .addOnSuccessListener {
                                    currentUser = currentUser.copy(supervising = updatedList)
                                    loadSupervisedSeniors(updatedList)
                                    Toast.makeText(this, "Senior dodany!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Błąd aktualizacji opiekuna", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Senior już dodany", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Błąd wyszukiwania seniora", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
}