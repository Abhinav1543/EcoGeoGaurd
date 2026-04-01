// data/model/SystemHealth.kt
package com.example.ecogeoguard.data.model

data class SystemHealth(
    val totalSensors: Int,
    val activeSensors: Int,
    val uptimePercentage: Float,
    val dataAccuracy: Float,
    val lastUpdated: Long,
    val alertsToday: Int,
    val resolvedAlerts: Int,
    val avgResponseTime: Long
) {

    val systemStatus: String
        get() = when {
            uptimePercentage < 80f -> "CRITICAL"
            uptimePercentage < 90f -> "WARNING"
            else -> "HEALTHY"
        }

    val systemStatusColor: Int
        get() = when(systemStatus) {
            "HEALTHY" -> android.R.color.holo_green_dark
            "WARNING" -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_red_dark
        }

    val sensorHealthPercentage: Int
        get() = if (totalSensors > 0) {
            ((activeSensors.toFloat() / totalSensors) * 100).toInt()
        } else {
            100
        }

    val resolutionRate: Float
        get() = if (alertsToday > 0) {
            (resolvedAlerts.toFloat() / alertsToday) * 100
        } else {
            100f
        }
}