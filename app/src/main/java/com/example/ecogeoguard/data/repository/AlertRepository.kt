// data/repository/AlertRepository.kt
package com.example.ecogeoguard.data.repository

import com.example.ecogeoguard.data.model.Alert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor() {

    // Simulated database of alerts
    private val alerts = mutableListOf(
        Alert(
            id = "alert_001",
            title = "Landslide Risk Detected",
            message = "High soil moisture and vibration levels detected in Himalayan Village",
            type = Alert.AlertType.LANDSLIDE,
            severity = Alert.Severity.HIGH,
            timestamp = System.currentTimeMillis() - 1800000, // 30 minutes ago
            villageId = "village_001",
            isRead = false
        ),
        Alert(
            id = "alert_002",
            title = "Livestock Out of Safe Zone",
            message = "Cow ID-123 has moved beyond geofenced boundary",
            type = Alert.AlertType.LIVESTOCK,
            severity = Alert.Severity.MEDIUM,
            timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
            villageId = "village_002",
            isRead = true
        ),
        Alert(
            id = "alert_003",
            title = "Irrigation Required",
            message = "Soil moisture below 30% in Field A",
            type = Alert.AlertType.IRRIGATION,
            severity = Alert.Severity.LOW,
            timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
            villageId = "village_001",
            isRead = false
        ),
        Alert(
            id = "alert_004",
            title = "Heavy Rainfall Alert",
            message = "Rainfall exceeding 50mm in last 2 hours",
            type = Alert.AlertType.RAINFALL,
            severity = Alert.Severity.HIGH,
            timestamp = System.currentTimeMillis() - 900000, // 15 minutes ago
            villageId = "village_003",
            isRead = false
        ),
        Alert(
            id = "alert_005",
            title = "Sensor Offline",
            message = "Sensor Node #12 has been offline for 2 hours",
            type = Alert.AlertType.SYSTEM,
            severity = Alert.Severity.MEDIUM,
            timestamp = System.currentTimeMillis() - 10800000, // 3 hours ago
            villageId = "village_002",
            isRead = true
        )
    )

    fun getRecentAlerts(): List<Alert> {
        return alerts.sortedByDescending { it.timestamp }
    }

    fun getAlertsFlow(): Flow<List<Alert>> = flow {
        emit(alerts.sortedByDescending { it.timestamp })
    }

    fun markAsRead(alertId: String) {
        alerts.find { it.id == alertId }?.let { alert ->
            val index = alerts.indexOf(alert)
            alerts[index] = alert.copy(isRead = true)
        }
    }

    fun addAlert(alert: Alert) {
        alerts.add(alert)
    }

    fun getUnreadCount(): Int {
        return alerts.count { !it.isRead }
    }

    fun getUnreadAlertsCount(): Int {
        return alerts.count { !it.isRead }
    }

    fun markAllAsRead() {
        alerts.forEachIndexed { index, alert ->
            if (!alert.isRead) {
                alerts[index] = alert.copy(isRead = true)
            }
        }
    }

    fun getRecentAlerts(limit: Int = 5): List<Alert> {
        return alerts.sortedByDescending { it.timestamp }.take(limit)
    }
}