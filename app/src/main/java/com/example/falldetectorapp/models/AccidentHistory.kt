package com.example.falldetectorapp.models

/**
 * Reprezentuje zapis historii potencjalnego upadku (incydentu) użytkownika.
 *
 * Może być wykorzystana do przeglądania statystyk upadków, potwierdzonych lub odrzuconych.
 *
 */
data class AccidentHistory(
    val uid: String = "",
    val userId: String = "", // id seniora
    val wasRejected: Boolean = false, //czy upadek został odrzucony
    val timestamp: Long = System.currentTimeMillis()
)