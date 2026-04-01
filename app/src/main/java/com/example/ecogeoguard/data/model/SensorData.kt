package com.example.ecogeoguard.data.model

data class SensorData(
    val rainfall: Int = 0,
    val soilMoisture: Int = 0,
    val vibration: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
