package com.example.ecogeoguard.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

// ==================== ALERT RELATED ====================

@Parcelize
data class DisasterAlert(
    val id: String,
    val type: DisasterType,
    val severity: AlertSeverityLevel,
    val title: String,
    val message: String,
    val timestamp: Long,
    val location: LocationData,
    val affectedVillages: List<String>,
    val affectedPopulation: Int,
    val affectedLivestock: Int,
    val recommendedAction: String,
    val isActive: Boolean = true,
    val acknowledgedBy: String? = null,
    val evacuationRouteId: String? = null,
    val sensorDataIds: List<String> = emptyList()
) : Parcelable

enum class DisasterType {
    LANDSLIDE, FLASH_FLOOD, HEAVY_RAINFALL, EARTHQUAKE, FOREST_FIRE, DROUGHT, STORM
}

enum class AlertSeverityLevel {
    INFO, LOW, MEDIUM, HIGH, CRITICAL
}

// ==================== SENSOR RELATED ====================

@Parcelize
data class DisasterSensorNode(
    val id: String,
    val type: DisasterSensorType,
    val location: LocationData,
    val villageId: String,
    val status: DisasterSensorStatus,
    val batteryLevel: Int,
    val lastReading: Long,
    val readings: List<DisasterSensorReading> = emptyList(),
    val signalStrength: Int,
    val firmwareVersion: String,
    val installedDate: Long
) : Parcelable

enum class DisasterSensorType {
    VIBRATION, TILT, SOIL_MOISTURE, RAINFALL, TEMPERATURE, HUMIDITY, PRESSURE, GPS, CAMERA
}

enum class DisasterSensorStatus {
    ONLINE, OFFLINE, LOW_BATTERY, MAINTENANCE, ERROR
}

@Parcelize
data class DisasterSensorReading(
    val sensorId: String,
    val timestamp: Long,
    val values: Map<String, Double>,
    val quality: ReadingQualityLevel
) : Parcelable

enum class ReadingQualityLevel {
    EXCELLENT, GOOD, FAIR, POOR, INVALID
}

// ==================== EVACUATION RELATED ====================

@Parcelize
data class EvacuationRoute(
    val id: String,
    val name: String,
    val fromVillage: String,
    val toShelter: String,
    val waypoints: List<LocationPoint>,
    val distanceKm: Float,
    val estimatedTimeMin: Int,
    val capacity: Int,
    val currentOccupancy: Int = 0,
    val status: RouteStatusLevel,
    val riskLevel: RiskLevelType,
    val lastMaintained: Long,
    val alternativeRoutes: List<String> = emptyList()
) : Parcelable

enum class RouteStatusLevel {
    OPEN, CLOSED, UNDER_MAINTENANCE, CONGESTED, ALTERNATIVE
}

enum class RiskLevelType {
    LOW, MODERATE, HIGH, CRITICAL
}



// ==================== SHELTER RELATED ====================

@Parcelize
data class Shelter(
    val id: String,
    val name: String,
    val location: LocationData,
    val capacity: Int,
    val currentOccupancy: Int,
    val resources: ShelterResources,
    val status: ShelterStatusLevel,
    val contactPerson: String,
    val contactNumber: String
) : Parcelable

@Parcelize
data class ShelterResources(
    val foodSupply: Int, // meals
    val waterSupply: Int, // liters
    val medicalKits: Int,
    val blankets: Int,
    val generators: Int,
    val lastUpdated: Long
) : Parcelable

enum class ShelterStatusLevel {
    OPEN, FULL, CLOSED, UNDER_PREPARATION
}

// ==================== RISK ASSESSMENT ====================

@Parcelize
data class RiskAssessment(
    val villageId: String,
    val timestamp: Long,
    val overallRisk: RiskLevelType,
    val landslideRisk: RiskLevelType,
    val floodRisk: RiskLevelType,
    val earthquakeRisk: RiskLevelType,
    val factors: Map<String, Double>,
    val recommendations: List<String>,
    val affectedArea: Float, // square km
    val populationAtRisk: Int,
    val livestockAtRisk: Int
) : Parcelable

// ==================== RESOURCE MANAGEMENT ====================

@Parcelize
data class ResourceRequest(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val requesterRole: String,
    val resourceType: ResourceTypeCategory,
    val quantity: Int,
    val urgency: AlertSeverityLevel,
    val location: LocationData,
    val timestamp: Long,
    val status: RequestStatusType,
    val assignedTo: String? = null
) : Parcelable

enum class ResourceTypeCategory {
    FOOD, WATER, MEDICAL, BLANKETS, TENTS, GENERATORS, RESCUE_TEAMS, VEHICLES
}

enum class RequestStatusType {
    PENDING, APPROVED, IN_PROGRESS, COMPLETED, REJECTED
}

// ==================== WEATHER FORECAST ====================

@Parcelize
data class WeatherForecast(
    val villageId: String,
    val timestamp: Long,
    val temperature: Float,
    val humidity: Int,
    val rainfall: Float,
    val windSpeed: Float,
    val windDirection: String,
    val pressure: Float,
    val visibility: Float,
    val uvIndex: Int,
    val forecast: List<HourlyForecast>
) : Parcelable

@Parcelize
data class HourlyForecast(
    val hour: Int,
    val temperature: Float,
    val rainfall: Float,
    val windSpeed: Float,
    val condition: String
) : Parcelable

// ==================== SUMMARY STATS ====================

@Parcelize
data class EvacuationSummary(
    val totalEvacuated: Int,
    val activeEvacuations: Int,
    val sheltersOpen: Int,
    val availableCapacity: Int,
    val routesOpen: Int,
    val lastUpdated: Long
) : Parcelable

@Parcelize
data class DisasterStats(
    val activeAlerts: Int,
    val criticalAlerts: Int,
    val affectedVillages: Int,
    val sensorsOnline: Int,
    val totalSensors: Int,
    val avgResponseTime: Int, // minutes
    val last24hEvents: Int
) : Parcelable

