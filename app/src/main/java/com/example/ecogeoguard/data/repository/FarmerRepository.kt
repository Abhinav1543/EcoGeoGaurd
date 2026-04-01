// data/repository/FarmerRepository.kt
package com.example.ecogeoguard.data.repository

import com.example.ecogeoguard.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

@Singleton
class FarmerRepository @Inject constructor() {

    // Simulated database
    private val fields = mutableListOf<Field>()
    private val sensorDataHistory = mutableMapOf<String, MutableList<FieldSensorData>>()
    private val irrigationHistory = mutableMapOf<String, MutableList<IrrigationRecommendation>>()
    private val cropHealthHistory = mutableMapOf<String, MutableList<CropHealthIndex>>()
    private val activeWeatherAlerts = mutableListOf<WeatherAlert>()

    // Sensor simulation parameters
    private val baseSoilMoisture = mutableMapOf<String, Float>()
    private val lastRainfall = mutableMapOf<String, Float>()
    private val simulationThreads = mutableMapOf<String, Thread>()
    private var isSimulating = false

    init {
        // Initialize with sample data for demo
        initializeSampleData()
        startGlobalSimulation()
    }

    private fun initializeSampleData() {
        // Create sample fields for different farmers
        val sampleFields = listOf(
            Field(
                id = "field_001",
                name = "North Field - Rice",
                villageId = "village_001",
                areaInAcres = 5.2f,
                cropType = CropType.RICE,
                soilType = SoilType.CLAY,
                sensorNodeId = "sensor_001",
                location = LocationData(28.6129, 77.2295, "North Block")
            ),
            Field(
                id = "field_002",
                name = "East Field - Wheat",
                villageId = "village_001",
                areaInAcres = 3.8f,
                cropType = CropType.WHEAT,
                soilType = SoilType.LOAMY,
                sensorNodeId = "sensor_002",
                location = LocationData(28.6139, 77.2305, "East Block")
            ),
            Field(
                id = "field_003",
                name = "South Field - Vegetables",
                villageId = "village_001",
                areaInAcres = 2.1f,
                cropType = CropType.VEGETABLES,
                soilType = SoilType.SANDY,
                sensorNodeId = "sensor_003",
                location = LocationData(28.6119, 77.2285, "South Block")
            ),
            Field(
                id = "field_004",
                name = "Hill Slope - Maize",
                villageId = "village_002",
                areaInAcres = 4.5f,
                cropType = CropType.MAIZE,
                soilType = SoilType.RED,
                sensorNodeId = "sensor_004",
                location = LocationData(28.6159, 77.2325, "Hill Area")
            )
        )

        fields.addAll(sampleFields)

        // Initialize sensor data for each field
        sampleFields.forEach { field ->
            baseSoilMoisture[field.id] = 45f + Random.nextFloat() * 30f
            lastRainfall[field.id] = 0f

            val initialData = mutableListOf<FieldSensorData>()
            repeat(10) { i ->
                initialData.add(generateSensorData(field.id, System.currentTimeMillis() - (i * 60000)))
            }
            sensorDataHistory[field.id] = initialData

            // Generate initial irrigation recommendations
            irrigationHistory[field.id] = mutableListOf()

            // Generate initial crop health
            cropHealthHistory[field.id] = mutableListOf(
                generateCropHealth(field.id, field.cropType)
            )
        }
    }

    fun startGlobalSimulation() {
        if (isSimulating) return

        isSimulating = true

        Thread {
            while (isSimulating) {
                Thread.sleep(30000)
                fields.forEach { field ->
                    generateNewSensorData(field.id)
                    checkForAlerts(field.id)
                }
            }
        }.start()
    }

    private fun generateSensorData(fieldId: String, timestamp: Long): FieldSensorData {
        val baseMoisture = baseSoilMoisture[fieldId] ?: 50f
        val lastRain = lastRainfall[fieldId] ?: 0f

        // Simulate natural moisture fluctuation
        val timeOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val temperatureBase = when (timeOfDay) {
            in 0..5 -> 18f + Random.nextFloat() * 3f      // Night
            in 6..11 -> 22f + Random.nextFloat() * 8f    // Morning
            in 12..16 -> 30f + Random.nextFloat() * 6f   // Afternoon
            in 17..20 -> 25f + Random.nextFloat() * 5f   // Evening
            else -> 20f + Random.nextFloat() * 4f        // Late night
        }

        // Simulate rainfall effect on moisture
        val moistureDecrease = if (lastRain > 0) {
            // If it rained, moisture increases
            baseMoisture + lastRain * 2f
        } else {
            // Natural drying (0.1-0.5% per reading)
            baseMoisture - (0.1f + Random.nextFloat() * 0.4f)
        }.coerceIn(20f, 85f)

        baseSoilMoisture[fieldId] = moistureDecrease

        // Gradually decrease rainfall
        lastRainfall[fieldId] = (lastRain * 0.8f).coerceAtLeast(0f)

        return FieldSensorData(
            fieldId = fieldId,
            timestamp = timestamp,
            soilMoisture = moistureDecrease,
            soilTemperature = temperatureBase - 3f + Random.nextFloat() * 2f,
            ambientTemperature = temperatureBase,
            humidity = 50f + Random.nextFloat() * 30f,
            rainfall = lastRain,
            windSpeed = 5f + Random.nextFloat() * 15f,
            solarRadiation = when (timeOfDay) {
                in 10..15 -> 600f + Random.nextFloat() * 300f  // Peak sun
                in 7..9 -> 200f + Random.nextFloat() * 200f
                in 16..17 -> 200f + Random.nextFloat() * 200f
                else -> 10f + Random.nextFloat() * 50f
            },
            batteryLevel = 85f + Random.nextFloat() * 14f,
            signalStrength = -50 - Random.nextInt(30),
            vibrationLevel = if (Random.nextFloat() < 0.1) Random.nextFloat() * 2f else 0f
        )
    }

    private fun generateNewSensorData(fieldId: String) {
        val newData = generateSensorData(fieldId, System.currentTimeMillis())
        sensorDataHistory[fieldId]?.add(0, newData)

        // Keep only last 1000 readings
        if (sensorDataHistory[fieldId]?.size ?: 0 > 1000) {
            sensorDataHistory[fieldId]?.removeAt(1000)
        }

        // Generate irrigation recommendation
        val irrigation = calculateIrrigationNeeds(fieldId, newData)
        irrigationHistory[fieldId]?.add(0, irrigation)

        // Generate crop health update
        val field = fields.find { it.id == fieldId }
        field?.let {
            val health = generateCropHealth(fieldId, it.cropType, newData)
            cropHealthHistory[fieldId]?.add(0, health)
        }
    }

    private fun calculateIrrigationNeeds(fieldId: String, data: FieldSensorData): IrrigationRecommendation {
        val field = fields.find { it.id == fieldId }
        val cropType = field?.cropType ?: CropType.OTHER

        // Different crops have different moisture requirements
        val optimalMoisture = when (cropType) {
            CropType.RICE -> 70f
            CropType.WHEAT -> 55f
            CropType.MAIZE -> 60f
            CropType.SUGARCANE -> 65f
            CropType.COTTON -> 50f
            CropType.VEGETABLES -> 65f
            CropType.FRUITS -> 60f
            else -> 55f
        }

        val moistureDeficit = optimalMoisture - data.soilMoisture
        val shouldIrrigate = moistureDeficit > 10f && data.rainfall < 2f

        val riskFactors = mutableListOf<String>()
        if (data.soilMoisture < 30f) riskFactors.add("Critical moisture level")
        if (data.ambientTemperature > 35f) riskFactors.add("High temperature stress")
        if (data.windSpeed > 20f) riskFactors.add("High wind speed")

        val waterNeeded =
            (moistureDeficit.coerceAtLeast(0f) *
                    ((field?.areaInAcres ?: 0f) * 4047f)).coerceAtLeast(1000f) * 0.5f

        return IrrigationRecommendation(
            fieldId = fieldId,
            timestamp = data.timestamp,
            shouldIrrigate = shouldIrrigate,
            confidence = if (shouldIrrigate) 0.7f + Random.nextFloat() * 0.2f else 0.9f,
            durationMinutes = (waterNeeded / 50).toInt().coerceIn(15, 120),
            waterAmountLiters = waterNeeded,
            reason = if (shouldIrrigate)
                "Soil moisture at ${"%.1f".format(data.soilMoisture)}% (optimal ${"%.0f".format(optimalMoisture)}%)"
            else
                "Soil moisture adequate at ${"%.1f".format(data.soilMoisture)}%",
            riskFactors = riskFactors
        )
    }

    private fun generateCropHealth(fieldId: String, cropType: CropType, sensorData: FieldSensorData? = null): CropHealthIndex {
        val latestData = sensorData ?: sensorDataHistory[fieldId]?.firstOrNull()
        ?: return CropHealthIndex(
            fieldId = fieldId,
            timestamp = System.currentTimeMillis(),
            overallHealth = 75f,
            ndvi = 0.65f,
            moistureStress = 20f,
            temperatureStress = 15f,
            pestRiskLevel = RiskLevel.LOW,
            diseaseProbability = 10f,
            recommendation = "Crop is healthy. Continue regular monitoring."
        )

        // Calculate health indicators based on sensor data
        val moistureStress = when {
            latestData.soilMoisture < 30f -> 80f
            latestData.soilMoisture < 40f -> 50f
            latestData.soilMoisture < 50f -> 30f
            latestData.soilMoisture > 75f -> 40f
            else -> 10f
        }

        val temperatureStress = when {
            latestData.ambientTemperature > 38f -> 70f
            latestData.ambientTemperature > 35f -> 40f
            latestData.ambientTemperature < 10f -> 60f
            latestData.ambientTemperature < 15f -> 30f
            else -> 10f
        }

        // Simulate NDVI based on crop health
        val healthFactor = (100f - (moistureStress * 0.5f) - (temperatureStress * 0.3f)) / 100f
        val ndvi = 0.3f + healthFactor * 0.5f

        val pestRisk = when {
            healthFactor > 0.8f -> RiskLevel.LOW
            healthFactor > 0.6f -> RiskLevel.MODERATE
            healthFactor > 0.4f -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }

        val diseaseProb = (100f - healthFactor * 100f) * 0.7f

        val recommendation = when {
            moistureStress > 60f -> "URGENT: Critical moisture level. Irrigate immediately!"
            moistureStress > 40f -> "Irrigation recommended within 24 hours"
            temperatureStress > 50f -> "Heat stress detected. Consider shade nets or increased irrigation"
            pestRisk == RiskLevel.HIGH -> "Pest risk elevated. Inspect crops and consider organic pesticides"
            else -> "Crop condition is good. Maintain regular irrigation schedule"
        }

        return CropHealthIndex(
            fieldId = fieldId,
            timestamp = latestData.timestamp,
            overallHealth = healthFactor * 100f,
            ndvi = ndvi,
            moistureStress = moistureStress,
            temperatureStress = temperatureStress,
            pestRiskLevel = pestRisk,
            diseaseProbability = diseaseProb,
            recommendation = recommendation
        )
    }

    private fun checkForAlerts(fieldId: String) {
        val latestData = sensorDataHistory[fieldId]?.firstOrNull() ?: return
        val field = fields.find { it.id == fieldId } ?: return

        // Check for landslide risk (vibration + rainfall + soil moisture)
        if ((latestData.vibrationLevel ?: 0f) > 1.5f && latestData.rainfall > 10f && latestData.soilMoisture > 60f) {
            createWeatherAlert(
                type = WeatherAlertType.LANDSLIDE,
                severity = RiskLevel.CRITICAL,
                message = "⚠️ IMMEDIATE DANGER: Landslide risk detected in ${field.name}! High vibration, heavy rainfall, and saturated soil conditions.",
                affectedFields = listOf(fieldId),
                recommendedAction = "EVACUATE IMMEDIATELY! Move to higher ground. Do not stay near slopes."
            )
        }

        // Check for heavy rainfall
        if (latestData.rainfall > 30f) {
            createWeatherAlert(
                type = WeatherAlertType.HEAVY_RAINFALL,
                severity = RiskLevel.HIGH,
                message = "🌧️ HEAVY RAINFALL: ${"%.1f".format(latestData.rainfall)}mm/hr detected in ${field.name}",
                affectedFields = listOf(fieldId),
                recommendedAction = "Check drainage systems. Avoid working in fields. Watch for waterlogging."
            )
        }

        // Check for drought conditions
        if (latestData.soilMoisture < 25f && latestData.rainfall < 1f && latestData.ambientTemperature > 32f) {
            createWeatherAlert(
                type = WeatherAlertType.DROUGHT,
                severity = RiskLevel.HIGH,
                message = "🔥 DROUGHT WARNING: Critical soil moisture (${"%.1f".format(latestData.soilMoisture)}%) in ${field.name}",
                affectedFields = listOf(fieldId),
                recommendedAction = "Emergency irrigation required immediately to save crops!"
            )
        }
    }

    private fun createWeatherAlert(
        type: WeatherAlertType,
        severity: RiskLevel,
        message: String,
        affectedFields: List<String>,
        recommendedAction: String
    ) {
        val alert = WeatherAlert(
            id = "alert_${System.currentTimeMillis()}",
            type = type,
            severity = severity,
            message = message,
            timestamp = System.currentTimeMillis(),
            expiryTimestamp = System.currentTimeMillis() + 3600000, // 1 hour
            affectedFields = affectedFields,
            recommendedAction = recommendedAction
        )

        activeWeatherAlerts.add(0, alert)

        // Keep only last 50 alerts
        if (activeWeatherAlerts.size > 50) {
            activeWeatherAlerts.removeAt(activeWeatherAlerts.size - 1)
        }
    }

    // Public methods for UI
    fun getFieldsForFarmer(farmerId: String): List<Field> {
        // In real app, filter by farmerId
        return fields
    }

    fun getFieldById(fieldId: String): Field? {
        return fields.find { it.id == fieldId }
    }

    fun getLatestSensorData(fieldId: String): FieldSensorData? {
        return sensorDataHistory[fieldId]?.firstOrNull()
    }

    fun getSensorDataHistory(fieldId: String, hours: Int = 24): List<FieldSensorData> {
        val cutoff = System.currentTimeMillis() - (hours * 3600000L)
        return sensorDataHistory[fieldId]?.filter { it.timestamp > cutoff } ?: emptyList()
    }

    fun getLatestIrrigationRecommendation(fieldId: String): IrrigationRecommendation? {
        return irrigationHistory[fieldId]?.firstOrNull()
    }

    fun getIrrigationHistory(fieldId: String): List<IrrigationRecommendation> {
        return irrigationHistory[fieldId]?.take(10) ?: emptyList()
    }

    fun getLatestCropHealth(fieldId: String): CropHealthIndex? {
        return cropHealthHistory[fieldId]?.firstOrNull()
    }

    fun getActiveAlerts(fieldIds: List<String>? = null): List<WeatherAlert> {
        return if (fieldIds == null) {
            activeWeatherAlerts
        } else {
            activeWeatherAlerts.filter { alert ->
                alert.affectedFields.any { it in fieldIds }
            }
        }
    }

    fun getUnreadAlertCount(): Int {
        return activeWeatherAlerts.count { it.timestamp > System.currentTimeMillis() - 86400000 }
    }

    fun simulateRainfall(fieldId: String, intensity: Float) {
        lastRainfall[fieldId] = intensity
        generateNewSensorData(fieldId)
    }

    fun stopSimulation() {
        isSimulating = false
    }
}