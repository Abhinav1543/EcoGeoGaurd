// data/repository/VillageRepository.kt
package com.example.ecogeoguard.data.repository

import com.example.ecogeoguard.data.model.Village
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VillageRepository @Inject constructor() {

    // Simulated database of villages
    private val villages = mutableListOf(
        Village(
            id = "village_001",
            name = "Himalayan Village",
            district = "Uttarakhand",
            state = "Uttarakhand",
            population = 1520,
            totalSensors = 15,
            activeSensors = 14,
            riskLevel = Village.RiskLevel.HIGH,
            lastUpdated = System.currentTimeMillis(),
            latitude = 30.7333,
            longitude = 79.0667
        ),
        Village(
            id = "village_002",
            name = "Green Valley",
            district = "Himachal Pradesh",
            state = "Himachal Pradesh",
            population = 2310,
            totalSensors = 12,
            activeSensors = 10,
            riskLevel = Village.RiskLevel.MEDIUM,
            lastUpdated = System.currentTimeMillis() - 86400000,
            latitude = 31.1048,
            longitude = 77.1734
        ),
        Village(
            id = "village_003",
            name = "RiverSide",
            district = "Assam",
            state = "Assam",
            population = 1850,
            totalSensors = 18,
            activeSensors = 17,
            riskLevel = Village.RiskLevel.LOW,
            lastUpdated = System.currentTimeMillis() - 43200000,
            latitude = 26.1445,
            longitude = 91.7362
        ),
        Village(
            id = "village_004",
            name = "Mountain Top",
            district = "Sikkim",
            state = "Sikkim",
            population = 920,
            totalSensors = 8,
            activeSensors = 6,
            riskLevel = Village.RiskLevel.CRITICAL,
            lastUpdated = System.currentTimeMillis() - 172800000,
            latitude = 27.5330,
            longitude = 88.5122
        ),
        Village(
            id = "village_005",
            name = "Farmers Paradise",
            district = "Punjab",
            state = "Punjab",
            population = 3100,
            totalSensors = 20,
            activeSensors = 19,
            riskLevel = Village.RiskLevel.LOW,
            lastUpdated = System.currentTimeMillis() - 21600000,
            latitude = 31.1471,
            longitude = 75.3412
        )
    )

    fun getAllVillages(): List<Village> {
        return villages.sortedBy { it.name }
    }

    fun getVillagesFlow(): Flow<List<Village>> = flow {
        emit(villages.sortedBy { it.name })
    }

    fun getVillageById(id: String): Village? {
        return villages.find { it.id == id }
    }

    fun updateRiskLevel(villageId: String, riskLevel: Village.RiskLevel) {
        villages.find { it.id == villageId }?.let { village ->
            val index = villages.indexOf(village)
            villages[index] = village.copy(
                riskLevel = riskLevel,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    fun updateSensorStatus(villageId: String, activeSensors: Int) {
        villages.find { it.id == villageId }?.let { village ->
            val index = villages.indexOf(village)
            villages[index] = village.copy(
                activeSensors = activeSensors,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
}