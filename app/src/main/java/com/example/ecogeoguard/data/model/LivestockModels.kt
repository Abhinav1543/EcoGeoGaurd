package com.example.ecogeoguard.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Animal(
    val id: String,
    val name: String,
    val type: AnimalType,
    val breed: String,
    val age: Int, // in months
    val weight: Float, // in kg
    val ownerId: String,
    val tagNumber: String,
    val collarId: String? = null,
    val profileImageUrl: String? = null,
    val healthStatus: HealthStatus,
    val lastVaccinationDate: Long? = null,
    val nextVaccinationDate: Long? = null,
    val pregnancyStatus: PregnancyStatus = PregnancyStatus.NOT_APPLICABLE,
    val expectedDeliveryDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class AnimalLocation(
    val animalId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val speed: Float? = null, // km/h
    val heading: Float? = null,
    val accuracy: Float? = null,
    val batteryLevel: Int, // 0-100%
    val signalStrength: Int // RSSI
) : Parcelable {
    fun isWithinGeofence(zone: SafeZone): Boolean {
        return zone.isPointInside(latitude, longitude)
    }
}

@Parcelize
data class SafeZone(
    val id: String,
    val name: String,
    val type: ZoneType,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusMeters: Float,
    val isActive: Boolean = true,
    val alertOnExit: Boolean = true,
    val alertOnEntry: Boolean = false,
    val restrictedHours: List<TimeRange>? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {

    fun isPointInside(lat: Double, lng: Double): Boolean {
        val distance = calculateDistance(centerLatitude, centerLongitude, lat, lng)
        return distance <= radiusMeters
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371e3 // Earth's radius in meters
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δφ = Math.toRadians(lat2 - lat1)
        val Δλ = Math.toRadians(lon2 - lon1)

        val a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
                Math.cos(φ1) * Math.cos(φ2) *
                Math.sin(Δλ / 2) * Math.sin(Δλ / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return (R * c).toFloat()
    }
}

@Parcelize
data class TimeRange(
    val startHour: Int, // 0-23
    val startMinute: Int, // 0-59
    val endHour: Int,
    val endMinute: Int
) : Parcelable {
    fun isWithin(currentHour: Int, currentMinute: Int): Boolean {
        val currentMinutes = currentHour * 60 + currentMinute
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            // Overnight range
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}

@Parcelize
data class HealthAlert(
    val id: String,
    val animalId: String,
    val animalName: String,
    val type: HealthAlertType,
    val severity: AlertSeverity,
    val message: String,
    val timestamp: Long,
    val isAcknowledged: Boolean = false,
    val acknowledgedBy: String? = null,
    val recommendedAction: String,
    val vitalData: VitalData? = null
) : Parcelable

@Parcelize
data class VitalData(
    val heartRate: Int? = null, // bpm
    val respirationRate: Int? = null, // breaths per minute
    val temperature: Float? = null, // celsius
    val activityLevel: Float? = null, // 0-100%
    val ruminationTime: Int? = null, // minutes
    val feedingTime: Int? = null // minutes
) : Parcelable

@Parcelize
data class MovementHistory(
    val animalId: String,
    val date: Long,
    val totalDistanceKm: Float,
    val averageSpeedKmh: Float,
    val maxSpeedKmh: Float,
    val activeHours: Int,
    val restingHours: Int,
    val pathPoints: List<LocationPoint> = emptyList()
) : Parcelable



enum class AnimalType {
    COW, BUFFALO, GOAT, SHEEP, HORSE, CAMEL, OTHER
}

enum class HealthStatus {
    EXCELLENT, GOOD, FAIR, POOR, CRITICAL, UNDER_TREATMENT
}

enum class PregnancyStatus {
    NOT_APPLICABLE, NOT_PREGNANT, PREGNANT, NEAR_DELIVERY, DELIVERED
}

enum class ZoneType {
    GRAZING, RESTING, WATERING, QUARANTINE, DANGER, RESTRICTED
}

enum class HealthAlertType {
    HIGH_HEART_RATE, LOW_HEART_RATE, HIGH_TEMPERATURE, LOW_ACTIVITY,
    NO_MOVEMENT, GRAZING_ABNORMAL, CALVING_IMMINENT, VACCINATION_DUE,
    INJURY_DETECTED, STRESS_DETECTED
}

enum class AlertSeverity {
    INFO, LOW, MEDIUM, HIGH, CRITICAL
}

enum class CollarStatus {
    ACTIVE, LOW_BATTERY, OFFLINE, MALFUNCTION
}