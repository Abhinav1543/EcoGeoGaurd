package com.example.ecogeoguard.data.model

data class SystemState(
    val mode: String = "NORMAL",   // NORMAL | WARNING | EMERGENCY
    val triggeredBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
