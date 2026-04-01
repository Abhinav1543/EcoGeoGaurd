package com.example.ecogeoguard.data.repository

import android.util.Log
import com.example.ecogeoguard.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*
import kotlin.random.Random

@Singleton
class LivestockRepository @Inject constructor() {

    private val TAG = "LivestockRepo"

    // Data storage
    private val animals = mutableListOf<Animal>()
    private val locations = mutableMapOf<String, MutableList<AnimalLocation>>()
    private val currentLocations = mutableMapOf<String, AnimalLocation>()
    private val safeZones = mutableListOf<SafeZone>()
    private val healthAlerts = mutableListOf<HealthAlert>()
    private val movementHistory = mutableMapOf<String, MutableList<MovementHistory>>()

    // Simulation parameters
    private val baseLatitudes = mutableMapOf<String, Double>()
    private val baseLongitudes = mutableMapOf<String, Double>()
    private val movementVectors = mutableMapOf<String, Pair<Double, Double>>()
    private var isSimulating = false

    // Flag to track if sample data has been initialized
    private var isInitialized = false

    // State flows for real-time updates
    private val _animalsFlow = MutableStateFlow<List<Animal>>(emptyList())
    val animalsFlow: StateFlow<List<Animal>> = _animalsFlow.asStateFlow()

    private val _locationsFlow = MutableStateFlow<Map<String, AnimalLocation>>(emptyMap())
    val locationsFlow: StateFlow<Map<String, AnimalLocation>> = _locationsFlow.asStateFlow()

    private val _alertsFlow = MutableStateFlow<List<HealthAlert>>(emptyList())
    val alertsFlow: StateFlow<List<HealthAlert>> = _alertsFlow.asStateFlow()

    init {
        initializeSampleData()
        startGlobalSimulation()
    }

    private fun initializeSampleData() {
        // Only initialize if animals list is empty
        if (animals.isNotEmpty()) return

        // Create sample safe zones
        safeZones.addAll(
            listOf(
                SafeZone(
                    id = "zone_001",
                    name = "Main Grazing Field",
                    type = ZoneType.GRAZING,
                    centerLatitude = 28.6129,
                    centerLongitude = 77.2295,
                    radiusMeters = 500f,
                    alertOnExit = true
                ),
                SafeZone(
                    id = "zone_002",
                    name = "Watering Hole",
                    type = ZoneType.WATERING,
                    centerLatitude = 28.6150,
                    centerLongitude = 77.2320,
                    radiusMeters = 100f,
                    alertOnExit = true
                ),
                SafeZone(
                    id = "zone_003",
                    name = "Resting Area",
                    type = ZoneType.RESTING,
                    centerLatitude = 28.6100,
                    centerLongitude = 77.2270,
                    radiusMeters = 200f,
                    alertOnExit = true,
                    restrictedHours = listOf(
                        TimeRange(22, 0, 5, 0) // Restricted at night
                    )
                ),
                SafeZone(
                    id = "zone_004",
                    name = "DANGER ZONE - Cliff Area",
                    type = ZoneType.DANGER,
                    centerLatitude = 28.6200,
                    centerLongitude = 77.2400,
                    radiusMeters = 300f,
                    alertOnExit = false,
                    alertOnEntry = true // Alert when animals enter danger zone
                )
            )
        )

        // Create sample animals
        val sampleAnimals = listOf(
            Animal(
                id = "cow_001",
                name = "Lakshmi",
                type = AnimalType.COW,
                breed = "Gir",
                age = 48,
                weight = 450f,
                ownerId = "farmer_001",
                tagNumber = "IN12345",
                collarId = "collar_001",
                healthStatus = HealthStatus.EXCELLENT,
                lastVaccinationDate = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
                nextVaccinationDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
                pregnancyStatus = PregnancyStatus.PREGNANT,
                expectedDeliveryDate = System.currentTimeMillis() + 45L * 24 * 60 * 60 * 1000
            ),
            Animal(
                id = "cow_002",
                name = "Ganga",
                type = AnimalType.COW,
                breed = "Sahiwal",
                age = 36,
                weight = 380f,
                ownerId = "farmer_001",
                tagNumber = "IN12346",
                collarId = "collar_002",
                healthStatus = HealthStatus.GOOD,
                lastVaccinationDate = System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000,
                nextVaccinationDate = System.currentTimeMillis() + 45L * 24 * 60 * 60 * 1000,
                pregnancyStatus = PregnancyStatus.NOT_PREGNANT
            ),
            Animal(
                id = "buff_001",
                name = "Kaveri",
                type = AnimalType.BUFFALO,
                breed = "Murrah",
                age = 60,
                weight = 550f,
                ownerId = "farmer_001",
                tagNumber = "IN12347",
                collarId = "collar_003",
                healthStatus = HealthStatus.FAIR,
                lastVaccinationDate = System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000,
                nextVaccinationDate = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000, // Overdue
                pregnancyStatus = PregnancyStatus.NOT_APPLICABLE
            ),
            Animal(
                id = "goat_001",
                name = "Chotu",
                type = AnimalType.GOAT,
                breed = "Jamunapari",
                age = 18,
                weight = 45f,
                ownerId = "farmer_001",
                tagNumber = "IN12348",
                collarId = "collar_004",
                healthStatus = HealthStatus.EXCELLENT,
                pregnancyStatus = PregnancyStatus.NOT_APPLICABLE
            ),
            Animal(
                id = "cow_003",
                name = "Nandini",
                type = AnimalType.COW,
                breed = "Holstein",
                age = 24,
                weight = 320f,
                ownerId = "farmer_001",
                tagNumber = "IN12349",
                collarId = "collar_005",
                healthStatus = HealthStatus.POOR,
                lastVaccinationDate = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000,
                nextVaccinationDate = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000, // Overdue
                pregnancyStatus = PregnancyStatus.NOT_PREGNANT
            )
        )

        animals.addAll(sampleAnimals)

        // Initialize base positions near safe zones
        sampleAnimals.forEachIndexed { index, animal ->
            val baseZone = safeZones[index % safeZones.size]
            val offset = Random.nextDouble() * 100 - 50
            val lat = baseZone.centerLatitude + offset / 111320.0
            val lng = baseZone.centerLongitude + offset / (111320.0 * cos(Math.toRadians(baseZone.centerLatitude)))

            baseLatitudes[animal.id] = lat
            baseLongitudes[animal.id] = lng

            // Random movement vector
            movementVectors[animal.id] = Pair(
                Random.nextDouble() * 0.0001 - 0.00005,
                Random.nextDouble() * 0.0001 - 0.00005
            )

            // Create initial location
            val initialLocation = AnimalLocation(
                animalId = animal.id,
                timestamp = System.currentTimeMillis(),
                latitude = lat,
                longitude = lng,
                batteryLevel = 85 + Random.nextInt(15),
                signalStrength = -50 - Random.nextInt(30)
            )

            currentLocations[animal.id] = initialLocation
            locations[animal.id] = mutableListOf(initialLocation)
        }

        // Create some initial health alerts
        createSampleAlerts()

        _animalsFlow.value = animals
        _locationsFlow.value = currentLocations
        _alertsFlow.value = healthAlerts
    }

    private fun createSampleAlerts() {
        healthAlerts.add(
            HealthAlert(
                id = "alert_001",
                animalId = "cow_003",
                animalName = "Nandini",
                type = HealthAlertType.VACCINATION_DUE,
                severity = AlertSeverity.HIGH,
                message = "Vaccination overdue by 30 days",
                timestamp = System.currentTimeMillis() - 2 * 60 * 60 * 1000,
                recommendedAction = "Schedule veterinary visit immediately",
                vitalData = VitalData(
                    heartRate = 72,
                    temperature = 38.9f,
                    activityLevel = 45f
                )
            )
        )

        healthAlerts.add(
            HealthAlert(
                id = "alert_002",
                animalId = "cow_001",
                animalName = "Lakshmi",
                type = HealthAlertType.CALVING_IMMINENT,
                severity = AlertSeverity.HIGH,
                message = "Pregnant cow showing signs of calving",
                timestamp = System.currentTimeMillis() - 45 * 60 * 1000,
                recommendedAction = "Monitor closely, prepare calving area",
                vitalData = VitalData(
                    heartRate = 85,
                    temperature = 38.5f,
                    activityLevel = 30f
                )
            )
        )

        healthAlerts.add(
            HealthAlert(
                id = "alert_003",
                animalId = "buff_001",
                animalName = "Kaveri",
                type = HealthAlertType.LOW_ACTIVITY,
                severity = AlertSeverity.MEDIUM,
                message = "Unusually low activity detected for 2 hours",
                timestamp = System.currentTimeMillis() - 3 * 60 * 60 * 1000,
                recommendedAction = "Check animal for illness or injury",
                vitalData = VitalData(
                    heartRate = 48,
                    temperature = 37.2f,
                    activityLevel = 12f
                )
            )
        )
    }

    private fun startGlobalSimulation() {
        if (isSimulating) return
        isSimulating = true

        Thread {
            while (isSimulating) {
                Thread.sleep(10000) // Update every 10 seconds
                updateAllLocations()
                checkGeofenceAlerts()
                updateHealthMetrics()
                // Don't reset animals list here, just update flows
                _locationsFlow.value = currentLocations.toMap()
                _alertsFlow.value = healthAlerts.toList()
            }
        }.start()
    }

    private fun updateAllLocations() {
        animals.forEach { animal ->
            updateAnimalLocation(animal.id)
        }
    }

    private fun updateAnimalLocation(animalId: String) {
        val currentLoc = currentLocations[animalId] ?: return
        val vector = movementVectors[animalId] ?: return

        // Random walk simulation
        val newLat = currentLoc.latitude + vector.first + (Random.nextDouble() - 0.5) * 0.0001
        val newLng = currentLoc.longitude + vector.second + (Random.nextDouble() - 0.5) * 0.0001

        // Occasionally change direction
        if (Random.nextInt(100) < 10) {
            movementVectors[animalId] = Pair(
                Random.nextDouble() * 0.0001 - 0.00005,
                Random.nextDouble() * 0.0001 - 0.00005
            )
        }

        // Calculate speed (simulated)
        val distance = calculateDistance(
            currentLoc.latitude, currentLoc.longitude,
            newLat, newLng
        )
        val speed = (distance / 10) * 3.6 // km/h (10 second interval)

        val newLocation = AnimalLocation(
            animalId = animalId,
            timestamp = System.currentTimeMillis(),
            latitude = newLat,
            longitude = newLng,
            speed = speed.toFloat(),
            batteryLevel = max(5, currentLoc.batteryLevel - Random.nextInt(1)),
            signalStrength = currentLoc.signalStrength + Random.nextInt(5) - 2
        )

        currentLocations[animalId] = newLocation

        // Add to history (keep last 1000 points)
        val history = locations.getOrPut(animalId) { mutableListOf() }
        history.add(newLocation)
        if (history.size > 1000) {
            history.removeAt(0)
        }
    }

    private fun checkGeofenceAlerts() {
        currentLocations.forEach { (animalId, location) ->
            val animal = animals.find { it.id == animalId } ?: return@forEach

            safeZones.forEach { zone ->
                val isInside = zone.isPointInside(location.latitude, location.longitude)

                // Check for danger zone entry
                if (zone.type == ZoneType.DANGER && isInside && zone.alertOnEntry) {
                    createGeofenceAlert(
                        animal = animal,
                        zone = zone,
                        message = "⚠️ DANGER: ${animal.name} has entered ${zone.name}!",
                        severity = AlertSeverity.CRITICAL
                    )
                }

                // Check for safe zone exit
                if (zone.alertOnExit && !isInside && wasInsideZone(animalId, zone)) {
                    // Check if within restricted hours
                    if (zone.restrictedHours != null) {
                        val calendar = Calendar.getInstance()
                        val hour = calendar.get(Calendar.HOUR_OF_DAY)
                        val minute = calendar.get(Calendar.MINUTE)

                        if (zone.restrictedHours.any { it.isWithin(hour, minute) }) {
                            createGeofenceAlert(
                                animal = animal,
                                zone = zone,
                                message = "⚠️ ${animal.name} has left ${zone.name} during restricted hours!",
                                severity = AlertSeverity.HIGH
                            )
                        }
                    } else {
                        createGeofenceAlert(
                            animal = animal,
                            zone = zone,
                            message = "⚠️ ${animal.name} has left ${zone.name}!",
                            severity = AlertSeverity.MEDIUM
                        )
                    }
                }
            }
        }
    }

    private fun wasInsideZone(animalId: String, zone: SafeZone): Boolean {
        val history = locations[animalId]
        if (history.isNullOrEmpty()) return false

        // Check last 5 positions
        val recentPositions = history.takeLast(5)
        return recentPositions.any { zone.isPointInside(it.latitude, it.longitude) }
    }

    private fun createGeofenceAlert(animal: Animal, zone: SafeZone, message: String, severity: AlertSeverity) {
        // Avoid duplicate alerts in last 30 minutes
        val recentAlert = healthAlerts.find {
            it.animalId == animal.id &&
                    it.type == HealthAlertType.STRESS_DETECTED &&
                    System.currentTimeMillis() - it.timestamp < 30 * 60 * 1000
        }

        if (recentAlert != null) return

        val alert = HealthAlert(
            id = "alert_${System.currentTimeMillis()}",
            animalId = animal.id,
            animalName = animal.name,
            type = HealthAlertType.STRESS_DETECTED,
            severity = severity,
            message = message,
            timestamp = System.currentTimeMillis(),
            recommendedAction = when (zone.type) {
                ZoneType.DANGER -> "Immediately check on ${animal.name} and guide back to safety!"
                else -> "Check location and bring ${animal.name} back to safe zone"
            }
        )

        healthAlerts.add(0, alert)
        if (healthAlerts.size > 50) {
            healthAlerts.removeAt(healthAlerts.size - 1)
        }
    }

    private fun updateHealthMetrics() {
        animals.forEach { animal ->
            val location = currentLocations[animal.id] ?: return@forEach

            // Simulate health metrics based on movement and random factors
            val activityLevel = calculateActivityLevel(animal.id)

            // Check for no movement (possible injury or death)
            if (activityLevel < 5 && System.currentTimeMillis() - (location.timestamp) > 30 * 60 * 1000) {
                createHealthAlert(
                    animal = animal,
                    type = HealthAlertType.NO_MOVEMENT,
                    severity = AlertSeverity.CRITICAL,
                    message = "⚠️ NO MOVEMENT DETECTED for ${animal.name} in last 30 minutes!",
                    recommendedAction = "IMMEDIATE ACTION: Go to last known location and check on animal!",
                    vitalData = VitalData(activityLevel = activityLevel)
                )
            }

            // Random health events (low probability)
            if (Random.nextInt(1000) < 5) { // 0.5% chance per update
                simulateRandomHealthEvent(animal)
            }
        }
    }

    private fun calculateActivityLevel(animalId: String): Float {
        val history = locations[animalId]
        if (history.isNullOrEmpty() || history.size < 10) return 50f

        val recent = history.takeLast(10)
        var totalDistance = 0.0

        for (i in 1 until recent.size) {
            totalDistance += calculateDistance(
                recent[i-1].latitude, recent[i-1].longitude,
                recent[i].latitude, recent[i].longitude
            )
        }

        // Convert to activity percentage (0-100)
        return (totalDistance / 1000).toFloat().coerceIn(0f, 100f)
    }

    private fun simulateRandomHealthEvent(animal: Animal) {
        val eventType = HealthAlertType.values().random()

        val (severity, message, action) = when (eventType) {
            HealthAlertType.HIGH_HEART_RATE -> Triple(
                AlertSeverity.HIGH,
                "Elevated heart rate detected in ${animal.name}",
                "Check for stress, fever, or recent activity"
            )
            HealthAlertType.LOW_HEART_RATE -> Triple(
                AlertSeverity.HIGH,
                "Low heart rate detected in ${animal.name}",
                "Immediate veterinary attention required"
            )
            HealthAlertType.HIGH_TEMPERATURE -> Triple(
                AlertSeverity.MEDIUM,
                "Elevated body temperature in ${animal.name}",
                "Check for fever or infection"
            )
            HealthAlertType.INJURY_DETECTED -> Triple(
                AlertSeverity.CRITICAL,
                "Possible injury detected - abnormal movement pattern",
                "Locate and examine animal immediately"
            )
            else -> return
        }

        createHealthAlert(
            animal = animal,
            type = eventType,
            severity = severity,
            message = message,
            recommendedAction = action
        )
    }

    private fun createHealthAlert(
        animal: Animal,
        type: HealthAlertType,
        severity: AlertSeverity,
        message: String,
        recommendedAction: String,
        vitalData: VitalData? = null
    ) {
        val alert = HealthAlert(
            id = "alert_${System.currentTimeMillis()}",
            animalId = animal.id,
            animalName = animal.name,
            type = type,
            severity = severity,
            message = message,
            timestamp = System.currentTimeMillis(),
            recommendedAction = recommendedAction,
            vitalData = vitalData
        )

        healthAlerts.add(0, alert)
        if (healthAlerts.size > 50) {
            healthAlerts.removeAt(healthAlerts.size - 1)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δφ = Math.toRadians(lat2 - lat1)
        val Δλ = Math.toRadians(lon2 - lon1)

        val a = sin(Δφ / 2) * sin(Δφ / 2) +
                cos(φ1) * cos(φ2) *
                sin(Δλ / 2) * sin(Δλ / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    // Public API methods
    fun getAllAnimals(): List<Animal> = animals

    fun getAnimalById(animalId: String): Animal? = animals.find { it.id == animalId }

    fun getCurrentLocation(animalId: String): AnimalLocation? = currentLocations[animalId]

    fun getLocationHistory(animalId: String, hours: Int = 24): List<AnimalLocation> {
        val cutoff = System.currentTimeMillis() - (hours * 3600000L)
        return locations[animalId]?.filter { it.timestamp > cutoff } ?: emptyList()
    }

    fun getSafeZones(): List<SafeZone> = safeZones

    fun getHealthAlerts(limit: Int = 20): List<HealthAlert> = healthAlerts.take(limit)

    fun getUnreadAlertCount(): Int = healthAlerts.count { !it.isAcknowledged }

    fun acknowledgeAlert(alertId: String, userId: String) {
        val index = healthAlerts.indexOfFirst { it.id == alertId }
        if (index >= 0) {
            val alert = healthAlerts[index]
            healthAlerts[index] = alert.copy(
                isAcknowledged = true,
                acknowledgedBy = userId
            )
        }
    }

    fun addSafeZone(zone: SafeZone) {
        safeZones.add(zone)
    }

    fun updateSafeZone(zone: SafeZone) {
        val index = safeZones.indexOfFirst { it.id == zone.id }
        if (index >= 0) {
            safeZones[index] = zone
        }
    }

    fun deleteSafeZone(zoneId: String) {
        safeZones.removeAll { it.id == zoneId }
    }

    fun simulateAnimalEntry(animalId: String, zoneId: String) {
        val animal = animals.find { it.id == animalId } ?: return
        val zone = safeZones.find { it.id == zoneId } ?: return

        // Teleport animal to zone center
        currentLocations[animalId] = AnimalLocation(
            animalId = animalId,
            timestamp = System.currentTimeMillis(),
            latitude = zone.centerLatitude,
            longitude = zone.centerLongitude,
            batteryLevel = 100,
            signalStrength = -50
        )
    }

    // Add this method to add new animals
    fun addAnimal(animal: Animal) {
        animals.add(animal)

        // Initialize location for new animal
        val baseZone = safeZones.random()
        val offset = Random.nextDouble() * 100 - 50
        val lat = baseZone.centerLatitude + offset / 111320.0
        val lng = baseZone.centerLongitude + offset / (111320.0 * cos(Math.toRadians(baseZone.centerLatitude)))

        baseLatitudes[animal.id] = lat
        baseLongitudes[animal.id] = lng

        movementVectors[animal.id] = Pair(
            Random.nextDouble() * 0.0001 - 0.00005,
            Random.nextDouble() * 0.0001 - 0.00005
        )

        val initialLocation = AnimalLocation(
            animalId = animal.id,
            timestamp = System.currentTimeMillis(),
            latitude = lat,
            longitude = lng,
            batteryLevel = 85 + Random.nextInt(15),
            signalStrength = -50 - Random.nextInt(30)
        )

        currentLocations[animal.id] = initialLocation
        locations[animal.id] = mutableListOf(initialLocation)

        // Update the flow
        _animalsFlow.value = animals.toList()
        _locationsFlow.value = currentLocations.toMap()
    }

    // Add this method to update animals
    fun updateAnimal(animal: Animal) {
        val index = animals.indexOfFirst { it.id == animal.id }
        if (index >= 0) {
            animals[index] = animal
            _animalsFlow.value = animals.toList()
        }
    }

    // Add this method to delete animals
    fun deleteAnimal(animalId: String) {
        animals.removeAll { it.id == animalId }
        currentLocations.remove(animalId)
        locations.remove(animalId)
        movementVectors.remove(animalId)
        baseLatitudes.remove(animalId)
        baseLongitudes.remove(animalId)

        _animalsFlow.value = animals.toList()
        _locationsFlow.value = currentLocations.toMap()
    }

    fun stopSimulation() {
        isSimulating = false
    }
}