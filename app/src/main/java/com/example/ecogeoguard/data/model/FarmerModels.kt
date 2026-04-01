// data/model/FarmerModels.kt
package com.example.ecogeoguard.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*


@Parcelize
data class Field(
    val id: String,
    val name: String,
    val villageId: String,
    val areaInAcres: Float,
    val cropType: CropType,
    val soilType: SoilType,
    val sensorNodeId: String? = null,
    val location: LocationData,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class FieldSensorData(
    val fieldId: String,
    val timestamp: Long,
    val soilMoisture: Float,           // 0-100%
    val soilTemperature: Float,         // Celsius
    val ambientTemperature: Float,      // Celsius
    val humidity: Float,                // 0-100%
    val rainfall: Float,                // mm per hour
    val windSpeed: Float,               // km/h
    val solarRadiation: Float,          // W/m²
    val batteryLevel: Float,            // 0-100%
    val signalStrength: Int,            // RSSI
    val vibrationLevel: Float? = null   // For landslide detection
) : Parcelable

@Parcelize
data class IrrigationRecommendation(
    val fieldId: String,
    val timestamp: Long,
    val shouldIrrigate: Boolean,
    val confidence: Float,              // 0-1
    val durationMinutes: Int,
    val waterAmountLiters: Float,
    val reason: String,
    val riskFactors: List<String> = emptyList()
) : Parcelable

@Parcelize
data class CropHealthIndex(
    val fieldId: String,
    val timestamp: Long,
    val overallHealth: Float,           // 0-100
    val ndvi: Float,                    // Normalized Difference Vegetation Index
    val moistureStress: Float,          // 0-100
    val temperatureStress: Float,       // 0-100
    val pestRiskLevel: RiskLevel,
    val diseaseProbability: Float,      // 0-100
    val recommendation: String
) : Parcelable

enum class CropType {
    RICE, WHEAT, MAIZE, SUGARCANE, COTTON, VEGETABLES, FRUITS, OTHER
}

enum class SoilType {
    CLAY, SANDY, LOAMY, SILTY, PEATY, CHALKY, ALLUVIAL, BLACK, RED, LATERITE
}

enum class RiskLevel {
    LOW, MODERATE, HIGH, CRITICAL
}

@Parcelize
data class WeatherAlert(
    val id: String,
    val type: WeatherAlertType,
    val severity: RiskLevel,
    val message: String,
    val timestamp: Long,
    val expiryTimestamp: Long,
    val affectedFields: List<String>,
    val recommendedAction: String
) : Parcelable

enum class WeatherAlertType {
    HEAVY_RAINFALL, HAILSTORM, FROST, DROUGHT, STRONG_WINDS, LANDSLIDE, FLOOD
}