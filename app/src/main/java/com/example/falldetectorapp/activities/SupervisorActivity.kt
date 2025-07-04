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
import com.example.falldetectorapp.fcm.FCMSender
import com.example.falldetectorapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Activity dla opiekuna (supervisora), umożliwiająca:
 * - przeglądanie przypisanych seniorów,
 * - dodawanie nowych seniorów za pomocą tokenu,
 * - testowe wysyłanie powiadomień FCM,
 * - aktualizację tokena FCM użytkownika,
 * - wylogowanie.
 */
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
    // UI, pobranie danych zalogowanego opiekuna oraz przypisanych seniorów.

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

        // Aktualizacja tokena FCM bieżącego urządzenia
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("FCM", "Token urządzenia: $token")
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
            db.collection("users").document(uid)
                .update("fcmToken", token)
        }

        addSeniorButton.setOnClickListener {
            showAddSeniorDialog()
        }


        //    testowe powiadomienie FCM do przypisanego seniora i jego opiekunów.
        val testNotificationButton = findViewById<Button>(R.id.testNotificationButton)
        Log.d("SupervisorActivity", "testNotificationButton: $testNotificationButton")
        testNotificationButton.setOnClickListener {
            Log.d("SupervisorActivity", "Clicked testNotificationButton, currentUser=$currentUser")

            // Zakładamy, że currentUser.supervising zawiera token seniora
            if (currentUser.supervising.isNotEmpty()) {
                val token = currentUser.supervising.first()

                db.collection("users")
                    .whereEqualTo("seniorToken", token)
                    .get()
                    .addOnSuccessListener { docs ->
                        val senior = docs.firstOrNull()?.toObject(User::class.java)
                        if (senior != null) {
                            val fcmTokens = senior.supervisedBy
                            for (supervisorUid in fcmTokens) {
                                db.collection("users").document(supervisorUid).get()
                                    .addOnSuccessListener { supervisorDoc ->
                                        val supervisor = supervisorDoc.toObject(User::class.java)
                                        val token = supervisor?.fcmToken
                                        if (!token.isNullOrEmpty()) {
                                            Log.d("SupervisorActivity", "Wysyłam powiadomienie do tokena: $token")
                                            FCMSender.sendNotification(
                                                this,
                                                token,
                                                "TEST",
                                                "To jest testowa wiadomość"
                                            ) { success, message ->
                                                Log.d("SupervisorActivity", "Wynik wysyłki: success=$success, message=$message")
                                                runOnUiThread {
                                                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                            Toast.makeText(this, "Wysłano testowe powiadomienie", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(this, "Brak tokena FCM", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        }
                    }
            } else {
                Toast.makeText(this, "Brak przypisanego seniora", Toast.LENGTH_SHORT).show()
            }
        }
    }
    //* Wczytuje dane aktualnie zalogowanego opiekuna z Firestore.

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
//Ładuje listę seniorów przypisanych do opiekuna na podstawie ich tokenów.
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
    //Pokazuje okno umożliwiający opiekunowi dodanie seniora po tokenie.
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

                db.collection("users")
                    .whereEqualTo("seniorToken", token)
                    .get()
                    .addOnSuccessListener { query ->
                        if (query.isEmpty) {
                            Toast.makeText(this, "Nie znaleziono seniora", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val seniorDoc = query.documents[0]
                        val senior = seniorDoc.toObject(User::class.java) ?: return@addOnSuccessListener
                        val seniorId = senior.uid

                        if (!currentUser.supervising.contains(token)) {
                            val updatedSupervising = currentUser.supervising + token

                            // 1. Update supervisor
                            db.collection("users").document(currentUser.uid)
                                .update("supervising", updatedSupervising)
                                .addOnSuccessListener {
                                    currentUser = currentUser.copy(supervising = updatedSupervising)

                                    // 2. Update senior
                                    val updatedSupervisedBy = senior.supervisedBy + currentUser.uid
                                    db.collection("users").document(seniorId)
                                        .update("supervisedBy", updatedSupervisedBy)
                                        .addOnSuccessListener {
                                            loadSupervisedSeniors(updatedSupervising)
                                            Toast.makeText(this, "Senior dodany!", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Błąd aktualizacji seniora", Toast.LENGTH_SHORT).show()
                                        }
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