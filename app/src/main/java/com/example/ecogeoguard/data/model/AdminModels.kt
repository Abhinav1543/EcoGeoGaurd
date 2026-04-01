// data/model/AdminModels.kt
package com.example.ecogeoguard.data.model

import android.os.Parcelable
import com.example.ecogeoguard.R
import kotlinx.parcelize.Parcelize
import java.util.*

// ================= USER ROLE =================
enum class UserRole(val id: String, val title: String, val description: String, val iconRes: Int) {
    ADMIN(
        id = "admin",
        title = "System Administrator",
        description = "Manage users, villages, sensors, and system settings",
        iconRes = R.drawable.ic_admin
    ),
    FARMER(
        id = "farmer",
        title = "Farmer",
        description = "Monitor fields, irrigation, and crop health",
        iconRes = R.drawable.ic_farmer
    ),
    LIVESTOCK_OWNER(
        id = "livestock_owner",
        title = "Livestock Owner",
        description = "Track animals and manage safe zones",
        iconRes = R.drawable.ic_livestock
    ),
    DISASTER_TEAM(
        id = "disaster_team",
        title = "Disaster Response Team",
        description = "Monitor risks and manage emergencies",
        iconRes = R.drawable.ic_authority
    ),
    GOVERNMENT(
        id = "government",
        title = "Government Official",
        description = "View analytics and generate reports",
        iconRes = R.drawable.ic_government
    );

    companion object {
        fun fromId(id: String): UserRole? {
            return values().find { it.id == id }
        }
    }
}

// ================= USER CLASS =================
@Parcelize
data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val villageId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

// ================= OTHER ENUMS =================
enum class AlertType {
    LANDSLIDE, LIVESTOCK_STRAYING, HEAVY_RAINFALL, IRRIGATION_NEEDED, THEFT_ALERT
}

enum class AdminAlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class SignalStrength {
    POOR, FAIR, GOOD, EXCELLENT
}

// ================= DATA CLASSES =================
data class SensorStatus(
    val id: String,
    val type: String,
    val batteryLevel: Float, // 0-100%
    val signalStrength: SignalStrength,
    val lastSeen: Date,
    val location: String,
    val healthStatus: String
)

data class WeatherData(
    val temperature: Float,
    val humidity: Int,
    val rainfall: Float,
    val windSpeed: Float,
    val condition: String
)