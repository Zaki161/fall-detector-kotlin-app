package com.example.falldetectorapp.models
/**
 * Klasa reprezentująca użytkownika aplikacji.
 *
 * Zawiera dane zarówno wprowadzone podczas rejestracji,
 * jak i edytowalne po zalogowaniu oraz informacje techniczne używane w backendzie.
 */
data class User(
    // Dane ustawiane przy rejestracji
    val uid: String = "",
    val nick: String = "",
    val mail: String = "",
    val phone: String = "",
    val password: String = "",
    val senior: Boolean = true,// domyślnie senior

    // utorzone oraz uzywane w backend
    val seniorToken: String? = null,
    val fcmToken: String = "",
    val supervising: List<String> = listOf(), // Lista uid seniorów, ktorych opiekun pilnuje
    val supervisedBy: List<String> = listOf(), // dla seniora: lista uid opiekunów

    // Dane ustawiana po zalogowaniu - edytowalne
    var height: Int = 0,  // wzrost w cm
    var age: Int = 0,
    var weight: Int = 0   // waga w kg

)