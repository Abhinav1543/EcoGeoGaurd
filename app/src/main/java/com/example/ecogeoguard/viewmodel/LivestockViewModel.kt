package com.example.ecogeoguard.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.ecogeoguard.data.model.*
import com.example.ecogeoguard.data.repository.LivestockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LivestockViewModel @Inject constructor(
    private val repository: LivestockRepository
) : ViewModel() {

    private val _animals = MutableStateFlow<List<Animal>>(emptyList())
    val animals: StateFlow<List<Animal>> = _animals.asStateFlow()

    private val _selectedAnimal = MutableStateFlow<Animal?>(null)
    val selectedAnimal: StateFlow<Animal?> = _selectedAnimal.asStateFlow()

    private val _currentLocations = MutableStateFlow<Map<String, AnimalLocation>>(emptyMap())
    val currentLocations: StateFlow<Map<String, AnimalLocation>> = _currentLocations.asStateFlow()

    private val _locationHistory = MutableStateFlow<List<AnimalLocation>>(emptyList())
    val locationHistory: StateFlow<List<AnimalLocation>> = _locationHistory.asStateFlow()

    private val _safeZones = MutableStateFlow<List<SafeZone>>(emptyList())
    val safeZones: StateFlow<List<SafeZone>> = _safeZones.asStateFlow()

    private val _healthAlerts = MutableStateFlow<List<HealthAlert>>(emptyList())
    val healthAlerts: StateFlow<List<HealthAlert>> = _healthAlerts.asStateFlow()

    private val _unreadAlertCount = MutableStateFlow(0)
    val unreadAlertCount: StateFlow<Int> = _unreadAlertCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastUpdated = MutableStateFlow(System.currentTimeMillis())
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    private val _showEmergencyBuzzer = MutableStateFlow(false)
    val showEmergencyBuzzer: StateFlow<Boolean> = _showEmergencyBuzzer.asStateFlow()

    private var currentEmergencyAlert: HealthAlert? = null

    init {
        loadData()
        startRealTimeUpdates()
        observeRepositoryFlows()
    }

    private fun observeRepositoryFlows() {
        viewModelScope.launch {
            // Observe animals from repository
            repository.animalsFlow.collect { animalList ->
                _animals.value = animalList
            }
        }

        viewModelScope.launch {
            // Observe locations from repository
            repository.locationsFlow.collect { locationMap ->
                _currentLocations.value = locationMap
            }
        }

        viewModelScope.launch {
            // Observe alerts from repository
            repository.alertsFlow.collect { alertList ->
                _healthAlerts.value = alertList
                _unreadAlertCount.value = alertList.count { !it.isAcknowledged }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _animals.value = repository.getAllAnimals()
                _safeZones.value = repository.getSafeZones()
                _currentLocations.value = repository.locationsFlow.value
                refreshAlerts()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectAnimal(animalId: String) {
        viewModelScope.launch {
            _selectedAnimal.value = repository.getAnimalById(animalId)
            _locationHistory.value = repository.getLocationHistory(animalId, 24)
        }
    }

    private fun startRealTimeUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(15000) // Update every 15 seconds
                refreshData()
                _lastUpdated.value = System.currentTimeMillis()
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            // Don't manually set animals here - they come from repository flow
            _safeZones.value = repository.getSafeZones()
            refreshAlerts()

            // Check for critical alerts
            checkForEmergencyConditions()
        }
    }

    private fun refreshAlerts() {
        _healthAlerts.value = repository.getHealthAlerts()
        _unreadAlertCount.value = repository.getUnreadAlertCount()
    }

    private fun checkForEmergencyConditions() {
        val criticalAlerts = _healthAlerts.value.filter {
            it.severity == AlertSeverity.CRITICAL && !it.isAcknowledged
        }

        if (criticalAlerts.isNotEmpty() && !_showEmergencyBuzzer.value) {
            currentEmergencyAlert = criticalAlerts.first()
            _showEmergencyBuzzer.value = true
        } else if (criticalAlerts.isEmpty() && _showEmergencyBuzzer.value) {
            // Auto-dismiss after 30 seconds if no critical alerts
            viewModelScope.launch {
                delay(30000)
                if (_healthAlerts.value.none { it.severity == AlertSeverity.CRITICAL && !it.isAcknowledged }) {
                    _showEmergencyBuzzer.value = false
                }
            }
        }
    }

    fun dismissEmergency() {
        _showEmergencyBuzzer.value = false
        currentEmergencyAlert?.let {
            acknowledgeAlert(it.id)
        }
        currentEmergencyAlert = null
    }

    fun getEmergencyAlert(): HealthAlert? = currentEmergencyAlert

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            repository.acknowledgeAlert(alertId, "current_user")
            refreshAlerts()
        }
    }

    // ============= ANIMAL CRUD OPERATIONS =============

    fun addAnimal(animal: Animal) {
        viewModelScope.launch {
            repository.addAnimal(animal)
            // Don't manually update _animals - repository flow will handle it
        }
    }

    fun updateAnimal(animal: Animal) {
        viewModelScope.launch {
            repository.updateAnimal(animal)

            // Update selected animal if it's the same
            if (_selectedAnimal.value?.id == animal.id) {
                _selectedAnimal.value = animal
            }
        }
    }

    fun deleteAnimal(animalId: String) {
        viewModelScope.launch {
            repository.deleteAnimal(animalId)

            // Clear selected animal if it was deleted
            if (_selectedAnimal.value?.id == animalId) {
                _selectedAnimal.value = null
            }
        }
    }

    fun getAnimalById(id: String): LiveData<Animal?> {
        return animals.map { list ->
            list.find { it.id == id }
        }.asLiveData()
    }

    fun getMovementHistory(animalId: String): List<MovementHistory> {
        // Generate sample movement history
        return listOf(
            MovementHistory(
                animalId = animalId,
                date = System.currentTimeMillis() - 86400000,
                totalDistanceKm = 3.5f,
                averageSpeedKmh = 2.1f,
                maxSpeedKmh = 5.2f,
                activeHours = 8,
                restingHours = 16,
                pathPoints = emptyList()
            ),
            MovementHistory(
                animalId = animalId,
                date = System.currentTimeMillis() - 2 * 86400000,
                totalDistanceKm = 4.2f,
                averageSpeedKmh = 2.5f,
                maxSpeedKmh = 6.1f,
                activeHours = 9,
                restingHours = 15,
                pathPoints = emptyList()
            ),
            MovementHistory(
                animalId = animalId,
                date = System.currentTimeMillis() - 3 * 86400000,
                totalDistanceKm = 2.8f,
                averageSpeedKmh = 1.8f,
                maxSpeedKmh = 4.3f,
                activeHours = 7,
                restingHours = 17,
                pathPoints = emptyList()
            )
        )
    }

    fun addAnimalToZone(animalId: String, zoneId: String) {
        viewModelScope.launch {
            val animal = _animals.value.find { it.id == animalId }
            val zone = _safeZones.value.find { it.id == zoneId }

            animal?.let { a ->
                zone?.let { z ->
                    // Simulate teleporting animal to zone center
                    repository.simulateAnimalEntry(animalId, zoneId)
                }
            }
        }
    }

    // ============= ZONE MANAGEMENT =============

    fun addSafeZone(zone: SafeZone) {
        viewModelScope.launch {
            repository.addSafeZone(zone)
            _safeZones.value = repository.getSafeZones()
        }
    }

    fun updateSafeZone(zone: SafeZone) {
        viewModelScope.launch {
            repository.updateSafeZone(zone)
            _safeZones.value = repository.getSafeZones()
        }
    }

    fun deleteSafeZone(zoneId: String) {
        viewModelScope.launch {
            repository.deleteSafeZone(zoneId)
            _safeZones.value = repository.getSafeZones()
        }
    }

    fun simulateAnimalEntry(animalId: String, zoneId: String) {
        viewModelScope.launch {
            repository.simulateAnimalEntry(animalId, zoneId)
        }
    }

    fun refreshManually() {
        viewModelScope.launch {
            _isLoading.value = true
            _animals.value = repository.getAllAnimals()
            _safeZones.value = repository.getSafeZones()
            _currentLocations.value = repository.locationsFlow.value
            refreshAlerts()
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopSimulation()
    }
}