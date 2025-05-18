package com.example.testapplication.models

data class Settings(
    val id: String = "",
    val userId: String ="",
    val sendEmail: String ="",
    val sendSMS: String ="",
    val cancelTimeSeconds: String=""
)

