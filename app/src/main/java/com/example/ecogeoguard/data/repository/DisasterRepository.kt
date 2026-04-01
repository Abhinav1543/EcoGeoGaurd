package com.example.ecogeoguard.data.repository

import com.example.ecogeoguard.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*
import kotlin.random.Random

@Singleton
class DisasterRepository @Inject constructor() {

    private val TAG = "DisasterRepo"

    // ==================== DATA STORAGE ====================
    private val alerts = mutableListOf<DisasterAlert>()
    private val sensorNodes = mutableListOf<DisasterSensorNode>()
    private val sensorReadings = mutableMapOf<String, MutableList<DisasterSensorReading>>()
    private val evacuationRoutes = mutableListOf<EvacuationRoute>()
    private val shelters = mutableListOf<Shelter>()
    private val riskAssessments = mutableListOf<RiskAssessment>()
    private val resourceRequests = mutableListOf<ResourceRequest>()
    private val weatherForecasts = mutableMapOf<String, WeatherForecast>()

    // ==================== STATE FLOWS ====================
    private val _alertsFlow = MutableStateFlow<List<DisasterAlert>>(emptyList())
    val alertsFlow: StateFlow<List<DisasterAlert>> = _alertsFlow.asStateFlow()

    private val _sensorsFlow = MutableStateFlow<List<DisasterSensorNode>>(emptyList())
    val sensorsFlow: StateFlow<List<DisasterSensorNode>> = _sensorsFlow.asStateFlow()

    private val _routesFlow = MutableStateFlow<List<EvacuationRoute>>(emptyList())
    val routesFlow: StateFlow<List<EvacuationRoute>> = _routesFlow.asStateFlow()

    private val _sheltersFlow = MutableStateFlow<List<Shelter>>(emptyList())
    val sheltersFlow: StateFlow<List<Shelter>> = _sheltersFlow.asStateFlow()

    private val _riskFlow = MutableStateFlow<List<RiskAssessment>>(emptyList())
    val riskFlow: StateFlow<List<RiskAssessment>> = _riskFlow.asStateFlow()

    private val _statsFlow = MutableStateFlow<DisasterStats?>(null)
    val statsFlow: StateFlow<DisasterStats?> = _statsFlow.asStateFlow()

    private var isSimulating = false

    // ==================== INITIALIZATION ====================
    init {
        initializeSampleData()
        startSimulation()
    }

    private fun initializeSampleData() {
        // Create sample villages/locations
        val villages = listOf(
            "Himalayan Village", "River Valley", "Hill Station", "Mountain Base",
            "Forest Edge", "Coastal Area", "Plateau Region"
        )

        // ========== CREATE SENSOR NODES ==========
        val sensorTypeList = listOf(
            DisasterSensorType.VIBRATION, DisasterSensorType.TILT,
            DisasterSensorType.SOIL_MOISTURE, DisasterSensorType.RAINFALL,
            DisasterSensorType.TEMPERATURE, DisasterSensorType.HUMIDITY,
            DisasterSensorType.PRESSURE, DisasterSensorType.GPS
        )

        villages.forEachIndexed { index, village ->
            repeat(3) { i ->
                val sensorId = "sensor_${village.take(3)}_${i}_${System.currentTimeMillis()}"
                val lat = 28.6129 + (index * 0.01) + Random.nextDouble() * 0.02
                val lng = 77.2295 + (i * 0.01) + Random.nextDouble() * 0.02

                val sensor = DisasterSensorNode(
                    id = sensorId,
                    type = sensorTypeList.random(),
                    location = LocationData(lat, lng, village),
                    villageId = "village_${index + 1}",
                    status = DisasterSensorStatus.entries.random(),
                    batteryLevel = 70 + Random.nextInt(30),
                    lastReading = System.currentTimeMillis(),
                    readings = mutableListOf(),
                    signalStrength = -50 - Random.nextInt(30),
                    firmwareVersion = "2.${Random.nextInt(5)}.${Random.nextInt(10)}",
                    installedDate = System.currentTimeMillis() - Random.nextLong(90L * 24 * 60 * 60 * 1000L)
                )
                sensorNodes.add(sensor)
            }
        }

        // ========== CREATE EVACUATION ROUTES ==========
        evacuationRoutes.addAll(
            listOf(
                EvacuationRoute(
                    id = "route_001",
                    name = "Highway 7 Evacuation Route",
                    fromVillage = "Himalayan Village",
                    toShelter = "District Shelter A",
                    waypoints = listOf(
                        LocationPoint(28.6150, 77.2320, 0),
                        LocationPoint(28.6200, 77.2350, 0),
                        LocationPoint(28.6250, 77.2400, 0)
                    ),
                    distanceKm = 12.5f,
                    estimatedTimeMin = 25,
                    capacity = 500,
                    currentOccupancy = Random.nextInt(200),
                    status = RouteStatusLevel.entries.random(),
                    riskLevel = RiskLevelType.entries.random(),
                    lastMaintained = System.currentTimeMillis() - Random.nextLong(30L * 24 * 60 * 60 * 1000)
                ),
                EvacuationRoute(
                    id = "route_002",
                    name = "Mountain Pass Route",
                    fromVillage = "Hill Station",
                    toShelter = "Community Center",
                    waypoints = listOf(
                        LocationPoint(28.6300, 77.2200, 0),
                        LocationPoint(28.6350, 77.2250, 0),
                        LocationPoint(28.6400, 77.2300, 0)
                    ),
                    distanceKm = 8.3f,
                    estimatedTimeMin = 18,
                    capacity = 300,
                    currentOccupancy = Random.nextInt(150),
                    status = RouteStatusLevel.entries.random(),
                    riskLevel = RiskLevelType.entries.random(),
                    lastMaintained = System.currentTimeMillis() - Random.nextLong(30L * 24 * 60 * 60 * 1000)
                ),
                EvacuationRoute(
                    id = "route_003",
                    name = "River Valley Evacuation",
                    fromVillage = "River Valley",
                    toShelter = "High Ground Shelter",
                    waypoints = listOf(
                        LocationPoint(28.6100, 77.2400, 0),
                        LocationPoint(28.6150, 77.2450, 0),
                        LocationPoint(28.6200, 77.2500, 0)
                    ),
                    distanceKm = 10.2f,
                    estimatedTimeMin = 22,
                    capacity = 400,
                    currentOccupancy = Random.nextInt(300),
                    status = RouteStatusLevel.entries.random(),
                    riskLevel = RiskLevelType.entries.random(),
                    lastMaintained = System.currentTimeMillis() - Random.nextLong(30L * 24 * 60 * 60 * 1000)
                )
            )
        )

        // ========== CREATE SHELTERS ==========
        shelters.addAll(
            listOf(
                Shelter(
                    id = "shelter_001",
                    name = "District Shelter A",
                    location = LocationData(28.6250, 77.2400, "Main Town"),
                    capacity = 1000,
                    currentOccupancy = Random.nextInt(500),
                    resources = ShelterResources(
                        foodSupply = 5000,
                        waterSupply = 10000,
                        medicalKits = 100,
                        blankets = 2000,
                        generators = 5,
                        lastUpdated = System.currentTimeMillis()
                    ),
                    status = ShelterStatusLevel.entries.random(),
                    contactPerson = "Rajesh Kumar",
                    contactNumber = "+91 98765 43210"
                ),
                Shelter(
                    id = "shelter_002",
                    name = "Community Center",
                    location = LocationData(28.6400, 77.2300, "Hill Station"),
                    capacity = 500,
                    currentOccupancy = Random.nextInt(300),
                    resources = ShelterResources(
                        foodSupply = 2500,
                        waterSupply = 5000,
                        medicalKits = 50,
                        blankets = 1000,
                        generators = 2,
                        lastUpdated = System.currentTimeMillis()
                    ),
                    status = ShelterStatusLevel.entries.random(),
                    contactPerson = "Priya Sharma",
                    contactNumber = "+91 98765 43211"
                ),
                Shelter(
                    id = "shelter_003",
                    name = "High Ground Shelter",
                    location = LocationData(28.6200, 77.2500, "River Valley"),
                    capacity = 750,
                    currentOccupancy = Random.nextInt(400),
                    resources = ShelterResources(
                        foodSupply = 3500,
                        waterSupply = 7500,
                        medicalKits = 75,
                        blankets = 1500,
                        generators = 3,
                        lastUpdated = System.currentTimeMillis()
                    ),
                    status = ShelterStatusLevel.entries.random(),
                    contactPerson = "Amit Singh",
                    contactNumber = "+91 98765 43212"
                )
            )
        )

        // ========== CREATE INITIAL ALERTS ==========
        createSampleAlerts()

        // ========== UPDATE FLOWS ==========
        _alertsFlow.value = alerts
        _sensorsFlow.value = sensorNodes
        _routesFlow.value = evacuationRoutes
        _sheltersFlow.value = shelters
        updateStats()
    }

    private fun createSampleAlerts() {
        alerts.addAll(
            listOf(
                DisasterAlert(
                    id = "alert_001",
                    type = DisasterType.LANDSLIDE,
                    severity = AlertSeverityLevel.CRITICAL,
                    title = "⚠️ CRITICAL: Landslide Imminent",
                    message = "High vibration and soil moisture detected. Landslide likely within 30 minutes.",
                    timestamp = System.currentTimeMillis() - 5 * 60 * 1000,
                    location = LocationData(28.6129, 77.2295, "Himalayan Village"),
                    affectedVillages = listOf("Himalayan Village", "Hill Station"),
                    affectedPopulation = 2500,
                    affectedLivestock = 800,
                    recommendedAction = "IMMEDIATE EVACUATION to District Shelter A via Highway 7",
                    isActive = true,
                    evacuationRouteId = "route_001"
                ),
                DisasterAlert(
                    id = "alert_002",
                    type = DisasterType.HEAVY_RAINFALL,
                    severity = AlertSeverityLevel.HIGH,
                    title = "🌧️ HEAVY RAINFALL WARNING",
                    message = "80mm rainfall expected in next 3 hours. Flood risk high.",
                    timestamp = System.currentTimeMillis() - 30 * 60 * 1000,
                    location = LocationData(28.6150, 77.2320, "River Valley"),
                    affectedVillages = listOf("River Valley", "Lowland Area"),
                    affectedPopulation = 3500,
                    affectedLivestock = 1200,
                    recommendedAction = "Prepare for evacuation. Move livestock to high ground.",
                    isActive = true,
                    evacuationRouteId = "route_003"
                ),
                DisasterAlert(
                    id = "alert_003",
                    type = DisasterType.FLASH_FLOOD,
                    severity = AlertSeverityLevel.MEDIUM,
                    title = "⚠️ Flash Flood Watch",
                    message = "River levels rising. Monitor closely.",
                    timestamp = System.currentTimeMillis() - 2 * 60 * 60 * 1000,
                    location = LocationData(28.6100, 77.2270, "River Valley"),
                    affectedVillages = listOf("River Valley"),
                    affectedPopulation = 1200,
                    affectedLivestock = 400,
                    recommendedAction = "Stay alert. Prepare important documents.",
                    isActive = true
                )
            )
        )
    }

    // ==================== SIMULATION ENGINE ====================
    private fun startSimulation() {
        if (isSimulating) return
        isSimulating = true

        Thread {
            while (isSimulating) {
                Thread.sleep(30000) // Update every 30 seconds
                simulateSensorReadings()
                checkForNewAlerts()
                updateRiskAssessments()
                updateShelterOccupancy()
                updateStats()

                // Update flows
                _alertsFlow.value = alerts.toList()
                _sensorsFlow.value = sensorNodes.toList()
                _routesFlow.value = evacuationRoutes.toList()
                _sheltersFlow.value = shelters.toList()
            }
        }.start()
    }

    private fun simulateSensorReadings() {
        sensorNodes.forEach { sensor ->
            val reading = DisasterSensorReading(
                sensorId = sensor.id,
                timestamp = System.currentTimeMillis(),
                values = generateSensorValues(sensor.type),
                quality = ReadingQualityLevel.entries.random()
            )

            val readings = sensorReadings.getOrPut(sensor.id) { mutableListOf() }
            readings.add(reading)
            if (readings.size > 1000) readings.removeAt(0)
        }
    }

    private fun generateSensorValues(type: DisasterSensorType): Map<String, Double> {
        return when (type) {
            DisasterSensorType.VIBRATION -> mapOf(
                "x" to Random.nextDouble() * 2.0,
                "y" to Random.nextDouble() * 2.0,
                "z" to Random.nextDouble() * 2.0,
                "magnitude" to Random.nextDouble() * 3.0
            )
            DisasterSensorType.TILT -> mapOf(
                "angle" to Random.nextDouble() * 15.0,
                "drift" to Random.nextDouble() * 0.5
            )
            DisasterSensorType.SOIL_MOISTURE -> mapOf(
                "moisture" to 30.0 + Random.nextDouble() * 50.0,
                "temperature" to 20.0 + Random.nextDouble() * 15.0
            )
            DisasterSensorType.RAINFALL -> mapOf(
                "intensity" to Random.nextDouble() * 50.0,
                "accumulation" to Random.nextDouble() * 100.0
            )
            DisasterSensorType.TEMPERATURE -> mapOf(
                "ambient" to 25.0 + Random.nextDouble() * 15.0
            )
            DisasterSensorType.HUMIDITY -> mapOf(
                "humidity" to 40.0 + Random.nextDouble() * 50.0
            )
            DisasterSensorType.PRESSURE -> mapOf(
                "pressure" to 980.0 + Random.nextDouble() * 50.0
            )
            else -> emptyMap()
        }
    }

    private fun checkForNewAlerts() {
        // Simulate random new alerts based on sensor readings
        if (Random.nextInt(100) < 5) { // 5% chance per update
            val villages = listOf("Himalayan Village", "River Valley", "Hill Station")
            val types = DisasterType.entries

            val alert = DisasterAlert(
                id = "alert_${System.currentTimeMillis()}",
                type = types.random(),
                severity = AlertSeverityLevel.entries.random(),
                title = generateAlertTitle(),
                message = generateAlertMessage(),
                timestamp = System.currentTimeMillis(),
                location = LocationData(28.6129 + Random.nextDouble(), 77.2295 + Random.nextDouble(), villages.random()),
                affectedVillages = listOf(villages.random(), villages.random()),
                affectedPopulation = Random.nextInt(1000, 5000),
                affectedLivestock = Random.nextInt(200, 2000),
                recommendedAction = generateRecommendedAction(),
                isActive = true
            )

            alerts.add(0, alert)
            if (alerts.size > 50) alerts.removeAt(alerts.size - 1)
        }
    }

    private fun generateAlertTitle(): String {
        val titles = listOf(
            "⚠️ LANDSLIDE WARNING",
            "🌧️ HEAVY RAINFALL ALERT",
            "🌊 FLASH FLOOD WARNING",
            "📡 SENSOR ANOMALY DETECTED",
            "🏔️ SLOPE INSTABILITY DETECTED"
        )
        return titles.random()
    }

    private fun generateAlertMessage(): String {
        val messages = listOf(
            "Critical vibration levels detected. Possible landslide within 2 hours.",
            "Rainfall exceeding 50mm/hr. Flood risk in low-lying areas.",
            "River water level rising rapidly. Prepare for evacuation.",
            "Multiple sensors detecting ground movement. Immediate attention required.",
            "Soil moisture critical. Slope failure possible."
        )
        return messages.random()
    }

    private fun generateRecommendedAction(): String {
        val actions = listOf(
            "IMMEDIATE EVACUATION to nearest shelter.",
            "Move to higher ground. Avoid river banks.",
            "Monitor situation. Prepare evacuation kit.",
            "Deploy response team to affected area.",
            "Alert all villagers. Use warning sirens."
        )
        return actions.random()
    }

    private fun updateRiskAssessments() {
        val villages = listOf("Himalayan Village", "River Valley", "Hill Station", "Mountain Base")

        villages.forEach { village ->
            val assessment = RiskAssessment(
                villageId = village,
                timestamp = System.currentTimeMillis(),
                overallRisk = RiskLevelType.entries.random(),
                landslideRisk = RiskLevelType.entries.random(),
                floodRisk = RiskLevelType.entries.random(),
                earthquakeRisk = RiskLevelType.entries.random(),
                factors = mapOf(
                    "rainfall" to Random.nextDouble() * 100,
                    "soilMoisture" to Random.nextDouble() * 100,
                    "vibration" to Random.nextDouble() * 10,
                    "slope" to Random.nextDouble() * 30
                ),
                recommendations = listOf(
                    "Monitor sensors hourly",
                    "Prepare evacuation routes",
                    "Alert community leaders"
                ),
                affectedArea = Random.nextFloat() * 10f,
                populationAtRisk = Random.nextInt(500, 5000),
                livestockAtRisk = Random.nextInt(200, 2000)
            )

            riskAssessments.add(assessment)
            if (riskAssessments.size > 100) riskAssessments.removeAt(0)
        }

        _riskFlow.value = riskAssessments.takeLast(10)
    }

    private fun updateShelterOccupancy() {
        shelters.forEachIndexed { index, shelter ->
            val newOccupancy = max(0, shelter.currentOccupancy + Random.nextInt(-20, 30))
            shelters[index] = shelter.copy(
                currentOccupancy = newOccupancy.coerceIn(0, shelter.capacity)
            )
        }
    }

    private fun updateStats() {
        val stats = DisasterStats(
            activeAlerts = alerts.count { it.isActive },
            criticalAlerts = alerts.count { it.severity == AlertSeverityLevel.CRITICAL && it.isActive },
            affectedVillages = alerts.flatMap { it.affectedVillages }.distinct().size,
            sensorsOnline = sensorNodes.count { it.status == DisasterSensorStatus.ONLINE },
            totalSensors = sensorNodes.size,
            avgResponseTime = Random.nextInt(5, 30),
            last24hEvents = Random.nextInt(3, 15)
        )
        _statsFlow.value = stats
    }

    // ==================== PUBLIC API METHODS ====================

    // ----- ALERT METHODS -----
    fun getAllAlerts(): List<DisasterAlert> = alerts

    fun getActiveAlerts(): List<DisasterAlert> = alerts.filter { it.isActive }

    fun getAlertById(alertId: String): DisasterAlert? = alerts.find { it.id == alertId }

    fun acknowledgeAlert(alertId: String, userId: String) {
        val index = alerts.indexOfFirst { it.id == alertId }
        if (index >= 0) {
            alerts[index] = alerts[index].copy(
                isActive = false,
                acknowledgedBy = userId
            )
        }
    }

    // ----- SENSOR METHODS -----
    fun getSensorNodes(): List<DisasterSensorNode> = sensorNodes

    fun getSensorById(sensorId: String): DisasterSensorNode? = sensorNodes.find { it.id == sensorId }

    fun getSensorReadings(sensorId: String, hours: Int = 24): List<DisasterSensorReading> {
        val cutoff = System.currentTimeMillis() - (hours * 3600000L)
        return sensorReadings[sensorId]?.filter { it.timestamp > cutoff } ?: emptyList()
    }

    // ----- EVACUATION ROUTE METHODS -----
    fun getEvacuationRoutes(): List<EvacuationRoute> = evacuationRoutes

    fun getRouteById(routeId: String): EvacuationRoute? = evacuationRoutes.find { it.id == routeId }

    fun updateEvacuationRoute(route: EvacuationRoute) {
        val index = evacuationRoutes.indexOfFirst { it.id == route.id }
        if (index >= 0) {
            evacuationRoutes[index] = route
        }
    }

    // ----- SHELTER METHODS -----
    fun getShelters(): List<Shelter> = shelters

    fun getShelterById(shelterId: String): Shelter? = shelters.find { it.id == shelterId }

    fun updateShelter(shelter: Shelter) {
        val index = shelters.indexOfFirst { it.id == shelter.id }
        if (index >= 0) {
            shelters[index] = shelter
        }
    }

    // ----- RESOURCE REQUEST METHODS -----
    fun addResourceRequest(request: ResourceRequest) {
        resourceRequests.add(request)
    }

    fun getResourceRequests(status: RequestStatusType? = null): List<ResourceRequest> {
        return if (status == null) resourceRequests
        else resourceRequests.filter { it.status == status }
    }

    fun updateResourceRequest(requestId: String, status: RequestStatusType, assignedTo: String?) {
        val index = resourceRequests.indexOfFirst { it.id == requestId }
        if (index >= 0) {
            resourceRequests[index] = resourceRequests[index].copy(
                status = status,
                assignedTo = assignedTo
            )
        }
    }

    // ----- RISK ASSESSMENT METHODS -----
    fun getRiskAssessments(): List<RiskAssessment> = riskAssessments

    fun getVillageRisk(villageId: String): RiskAssessment? {
        return riskAssessments.lastOrNull { it.villageId == villageId }
    }

    // ----- EMERGENCY BROADCAST -----
    fun triggerEmergencyBroadcast(message: String, severity: AlertSeverityLevel) {
        val alert = DisasterAlert(
            id = "broadcast_${System.currentTimeMillis()}",
            type = DisasterType.entries.random(),
            severity = severity,
            title = "🚨 EMERGENCY BROADCAST",
            message = message,
            timestamp = System.currentTimeMillis(),
            location = LocationData(28.6129, 77.2295, "District HQ"),
            affectedVillages = listOf("ALL VILLAGES"),
            affectedPopulation = 10000,
            affectedLivestock = 5000,
            recommendedAction = "Follow emergency protocols",
            isActive = true
        )
        alerts.add(0, alert)
    }

    // ----- STATS -----
    fun getStats(): DisasterStats? = _statsFlow.value

    // ----- SIMULATION CONTROL -----
    fun stopSimulation() {
        isSimulating = false
    }
}