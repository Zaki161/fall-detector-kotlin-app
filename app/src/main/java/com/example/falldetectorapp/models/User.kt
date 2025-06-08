package com.example.falldetectorapp.models

data class User(
    val uid: String = "",
    val nick: String = "",
    val mail: String = "",
    val phone: String = "",
    val password: String = "",
    val senior: Boolean = true // domyślnie senior
//    val isSenior: Boolean = true // domyślnie senior

)