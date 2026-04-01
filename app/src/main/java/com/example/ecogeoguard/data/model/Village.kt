// data/model/Village.kt
package com.example.ecogeoguard.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Village(
    val id: String,
    val name: String,
    val district: String,
    val state: String,
    val population: Int,
    val totalSensors: Int,
    val activeSensors: Int,
    val riskLevel: RiskLevel,
    val lastUpdated: Long,
    val latitude: Double,
    val longitude: Double
) : Parcelable {

    enum class RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    val riskColor: Int
        get() = when(riskLevel) {
            RiskLevel.LOW -> android.R.color.holo_green_light
            RiskLevel.MEDIUM -> android.R.color.holo_orange_light
            RiskLevel.HIGH -> android.R.color.holo_red_light
            RiskLevel.CRITICAL -> android.R.color.holo_red_dark
        }

    val sensorHealth: Int
        get() = if (totalSensors > 0) {
            (activeSensors * 100) / totalSensors
        } else {
            0
        }

    val formattedLastUpdated: String
        get() {
            val diff = System.currentTimeMillis() - lastUpdated
            val minutes = diff / (60 * 1000)
            val hours = diff / (60 * 60 * 1000)
            val days = diff / (24 * 60 * 60 * 1000)

            return when {
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours hours ago"
                else -> "$days days ago"
            }
        }

    fun getRiskPercentage(): Int {
        return when(riskLevel) {
            RiskLevel.LOW -> 20
            RiskLevel.MEDIUM -> 50
            RiskLevel.HIGH -> 75
            RiskLevel.CRITICAL -> 95
        }
    }
}