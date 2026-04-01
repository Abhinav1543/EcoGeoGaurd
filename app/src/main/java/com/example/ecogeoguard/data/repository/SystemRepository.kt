// data/repository/SystemRepository.kt
package com.example.ecogeoguard.data.repository

import com.example.ecogeoguard.data.model.SystemHealth
import com.example.ecogeoguard.data.model.Village
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemRepository @Inject constructor(
    private val alertRepository: AlertRepository,
    private val villageRepository: VillageRepository
) {

    fun getSystemHealth(): SystemHealth {
        val villages = villageRepository.getAllVillages()
        val alerts = alertRepository.getRecentAlerts()

        val totalSensors = villages.sumOf { it.totalSensors }
        val activeSensors = villages.sumOf { it.activeSensors }

        // Calculate uptime based on active sensors
        val uptimePercentage = if (totalSensors > 0) {
            (activeSensors.toFloat() / totalSensors) * 100
        } else {
            100f
        }

        // Simulate data accuracy (could be based on sensor health, signal strength, etc.)
        val dataAccuracy = 96.5f

        // Calculate today's alerts
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)
        val alertsToday = alerts.count { it.timestamp >= oneDayAgo }
        val resolvedAlerts = alerts.count { it.isRead }

        // Simulate average response time (in minutes)
        val avgResponseTime = 45L // 45 minutes average response time

        return SystemHealth(
            totalSensors = totalSensors,
            activeSensors = activeSensors,
            uptimePercentage = uptimePercentage,
            dataAccuracy = dataAccuracy,
            lastUpdated = now,
            alertsToday = alertsToday,
            resolvedAlerts = resolvedAlerts,
            avgResponseTime = avgResponseTime
        )
    }

    fun getSystemHealthFlow(): Flow<SystemHealth> = flow {
        while (true) {
            emit(getSystemHealth())
            kotlinx.coroutines.delay(30000) // Update every 30 seconds
        }
    }

    fun simulateSensorFailure(villageId: String) {
        villageRepository.updateSensorStatus(villageId, 0)
    }

    fun getSystemMetrics(): Map<String, Any> {
        val villages = villageRepository.getAllVillages()
        val alerts = alertRepository.getRecentAlerts()

        return mapOf(
            "totalVillages" to villages.size,
            "highRiskVillages" to villages.count { it.riskLevel == Village.RiskLevel.HIGH || it.riskLevel == Village.RiskLevel.CRITICAL },
            "totalAlerts" to alerts.size,
            "unreadAlerts" to alerts.count { !it.isRead },
            "sensorHealth" to calculateSensorHealth(villages),
            "systemLoad" to calculateSystemLoad()
        )
    }

    private fun calculateSensorHealth(villages: List<Village>): Map<String, Int> {
        val total = villages.sumOf { it.totalSensors }
        val active = villages.sumOf { it.activeSensors }
        val offline = total - active

        return mapOf(
            "total" to total,
            "active" to active,
            "offline" to offline,
            "healthPercentage" to if (total > 0) (active * 100) / total else 100
        )
    }

    private fun calculateSystemLoad(): String {
        // Simulate system load based on active alerts and sensor data
        val alerts = alertRepository.getRecentAlerts()
        val unreadCount = alerts.count { !it.isRead }

        return when {
            unreadCount > 10 -> "HIGH"
            unreadCount > 5 -> "MEDIUM"
            else -> "LOW"
        }
    }
}