package com.example.falldetectorapp.models

data class AccidentHistory(
    val uid: String = "",
    val userId: String = "",
    val wasRejected: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)