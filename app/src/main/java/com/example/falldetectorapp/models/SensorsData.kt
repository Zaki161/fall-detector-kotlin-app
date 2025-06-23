package com.example.falldetectorapp.models
/**
 * Reprezentuje dane z czujników (akcelerometru i żyroskopu) zebrane w danym momencie.
 *
 * Używane do wykrywania upadków.
 */
data class SensorsData (
    val id: String = "",
    val userId: String ="",
    val timestamp: String="",
    val acc_x: String= "",
    val acc_y: String= "",
    val acc_z: String= "",
    val gyro_x: String= "",
    val gyro_z: String= "",
    val gyro_y: String= ""
)

