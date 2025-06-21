package com.example.falldetectorapp.models

data class AccidentHistory(
    val uid: String = "",
    val userId: String = "", // id seniora
    val wasRejected: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)