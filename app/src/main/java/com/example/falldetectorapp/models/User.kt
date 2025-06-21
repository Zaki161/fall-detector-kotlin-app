package com.example.falldetectorapp.models

data class User(
    val uid: String = "",
    val nick: String = "",
    val mail: String = "",
    val phone: String = "",
    val password: String = "",
    val senior: Boolean = true,// domyślnie senior
//    val isSenior: Boolean = true // domyślnie senior
    val seniorToken: String? = null,
    val fcmToken: String = "",
    val supervising: List<String> = listOf(),
    val supervisedBy: List<String> = listOf(), // dla seniora: lista uid opiekunów

    var height: Int = 0,  // wzrost w cm
    var age: Int = 0,
    var weight: Int = 0   // waga w kg

)