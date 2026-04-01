package com.example.ecogeoguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecogeoguard.data.model.*
import com.example.ecogeoguard.data.repository.DisasterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisasterViewModel @Inject constructor(
    private val repository: DisasterRepository
) : ViewModel() {

    // ==================== STATE FLOWS ====================

    // Alerts
    private val _alerts = MutableStateFlow<List<DisasterAlert>>(emptyList())
    val alerts: StateFlow<List<DisasterAlert>> = _alerts.asStateFlow()

    private val _activeAlerts = MutableStateFlow<List<DisasterAlert>>(emptyList())
    val activeAlerts: StateFlow<List<DisasterAlert>> = _activeAlerts.asStateFlow()

    private val _selectedAlert = MutableStateFlow<DisasterAlert?>(null)
    val selectedAlert: StateFlow<DisasterAlert?> = _selectedAlert.asStateFlow()

    // Sensors
    private val _sensors = MutableStateFlow<List<DisasterSensorNode>>(emptyList())
    val sensors: StateFlow<List<DisasterSensorNode>> = _sensors.asStateFlow()

    private val _selectedSensor = MutableStateFlow<DisasterSensorNode?>(null)
    val selectedSensor: StateFlow<DisasterSensorNode?> = _selectedSensor.asStateFlow()

    private val _sensorReadings = MutableStateFlow<List<DisasterSensorReading>>(emptyList())
    val sensorReadings: StateFlow<List<DisasterSensorReading>> = _sensorReadings.asStateFlow()

    // Evacuation Routes
    private val _evacuationRoutes = MutableStateFlow<List<EvacuationRoute>>(emptyList())
    val evacuationRoutes: StateFlow<List<EvacuationRoute>> = _evacuationRoutes.asStateFlow()

    private val _selectedRoute = MutableStateFlow<EvacuationRoute?>(null)
    val selectedRoute: StateFlow<EvacuationRoute?> = _selectedRoute.asStateFlow()

    // Shelters
    private val _shelters = MutableStateFlow<List<Shelter>>(emptyList())
    val shelters: StateFlow<List<Shelter>> = _shelters.asStateFlow()

    private val _selectedShelter = MutableStateFlow<Shelter?>(null)
    val selectedShelter: StateFlow<Shelter?> = _selectedShelter.asStateFlow()

    // Risk Assessments
    private val _riskAssessments = MutableStateFlow<List<RiskAssessment>>(emptyList())
    val riskAssessments: StateFlow<List<RiskAssessment>> = _riskAssessments.asStateFlow()

    private val _selectedVillageRisk = MutableStateFlow<RiskAssessment?>(null)
    val selectedVillageRisk: StateFlow<RiskAssessment?> = _selectedVillageRisk.asStateFlow()

    // Resource Requests
    private val _resourceRequests = MutableStateFlow<List<ResourceRequest>>(emptyList())
    val resourceRequests: StateFlow<List<ResourceRequest>> = _resourceRequests.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<ResourceRequest>>(emptyList())
    val pendingRequests: StateFlow<List<ResourceRequest>> = _pendingRequests.asStateFlow()

    // Stats
    private val _stats = MutableStateFlow<DisasterStats?>(null)
    val stats: StateFlow<DisasterStats?> = _stats.asStateFlow()

    private val _evacuationSummary = MutableStateFlow<EvacuationSummary?>(null)
    val evacuationSummary: StateFlow<EvacuationSummary?> = _evacuationSummary.asStateFlow()

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastUpdated = MutableStateFlow(System.currentTimeMillis())
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    private val _showEmergencyBuzzer = MutableStateFlow(false)
    val showEmergencyBuzzer: StateFlow<Boolean> = _showEmergencyBuzzer.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentEmergencyAlert: DisasterAlert? = null

    // ==================== INITIALIZATION ====================

    init {
        loadData()
        observeRepositoryFlows()
        startRealTimeUpdates()
    }

    private fun observeRepositoryFlows() {
        viewModelScope.launch {
            repository.alertsFlow.collect { alertList ->
                _alerts.value = alertList
                _activeAlerts.value = alertList.filter { it.isActive }
                checkForCriticalAlerts(alertList)
                updateEvacuationSummary()
            }
        }

        viewModelScope.launch {
            repository.sensorsFlow.collect { sensorList ->
                _sensors.value = sensorList
            }
        }

        viewModelScope.launch {
            repository.routesFlow.collect { routeList ->
                _evacuationRoutes.value = routeList
                updateEvacuationSummary()
            }
        }

        viewModelScope.launch {
            repository.sheltersFlow.collect { shelterList ->
                _shelters.value = shelterList
                updateEvacuationSummary()
            }
        }

        viewModelScope.launch {
            repository.riskFlow.collect { riskList ->
                _riskAssessments.value = riskList
            }
        }

        viewModelScope.launch {
            repository.statsFlow.collect { statsData ->
                _stats.value = statsData
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _alerts.value = repository.getAllAlerts()
                _activeAlerts.value = repository.getActiveAlerts()
                _sensors.value = repository.getSensorNodes()
                _evacuationRoutes.value = repository.getEvacuationRoutes()
                _shelters.value = repository.getShelters()
                _riskAssessments.value = repository.getRiskAssessments()
                _resourceRequests.value = repository.getResourceRequests()
                _pendingRequests.value = repository.getResourceRequests(RequestStatusType.PENDING)
                _stats.value = repository.getStats()
                updateEvacuationSummary()
            } catch (e: Exception) {
                _errorMessage.value = "Error loading data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startRealTimeUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(30000) // Update every 30 seconds
                refreshData()
                _lastUpdated.value = System.currentTimeMillis()
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _activeAlerts.value = repository.getActiveAlerts()
            _pendingRequests.value = repository.getResourceRequests(RequestStatusType.PENDING)
            _stats.value = repository.getStats()
            updateEvacuationSummary()
        }
    }

    // ==================== ALERT METHODS ====================

    fun selectAlert(alertId: String) {
        viewModelScope.launch {
            _selectedAlert.value = repository.getAlertById(alertId)
        }
    }

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            repository.acknowledgeAlert(alertId, "disaster_team")
            refreshData()
        }
    }

    fun acknowledgeAllAlerts() {
        viewModelScope.launch {
            _activeAlerts.value.forEach { alert ->
                repository.acknowledgeAlert(alert.id, "disaster_team")
            }
            refreshData()
        }
    }

    private fun checkForCriticalAlerts(alertList: List<DisasterAlert>) {
        val criticalAlerts = alertList.filter {
            it.severity == AlertSeverityLevel.CRITICAL && it.isActive
        }

        if (criticalAlerts.isNotEmpty() && !_showEmergencyBuzzer.value) {
            currentEmergencyAlert = criticalAlerts.first()
            _showEmergencyBuzzer.value = true
        } else if (criticalAlerts.isEmpty() && _showEmergencyBuzzer.value) {
            viewModelScope.launch {
                delay(30000)
                if (_activeAlerts.value.none { it.severity == AlertSeverityLevel.CRITICAL }) {
                    _showEmergencyBuzzer.value = false
                }
            }
        }
    }

    fun dismissEmergency() {
        _showEmergencyBuzzer.value = false
        currentEmergencyAlert = null
    }

    fun getEmergencyAlert(): DisasterAlert? = currentEmergencyAlert

    // ==================== SENSOR METHODS ====================

    fun selectSensor(sensorId: String) {
        viewModelScope.launch {
            _selectedSensor.value = repository.getSensorById(sensorId)
            _sensorReadings.value = repository.getSensorReadings(sensorId, 24)
        }
    }

    fun getSensorReadings(sensorId: String, hours: Int = 24): List<DisasterSensorReading> {
        return repository.getSensorReadings(sensorId, hours)
    }

    fun getSensorsByVillage(villageId: String): List<DisasterSensorNode> {
        return _sensors.value.filter { it.villageId == villageId }
    }

    fun getSensorsByType(type: DisasterSensorType): List<DisasterSensorNode> {
        return _sensors.value.filter { it.type == type }
    }

    fun getSensorsByStatus(status: DisasterSensorStatus): List<DisasterSensorNode> {
        return _sensors.value.filter { it.status == status }
    }

    // ==================== EVACUATION ROUTE METHODS ====================

    fun selectRoute(routeId: String) {
        viewModelScope.launch {
            _selectedRoute.value = repository.getRouteById(routeId)
        }
    }

    fun updateRouteStatus(routeId: String, status: RouteStatusLevel) {
        viewModelScope.launch {
            val route = repository.getRouteById(routeId)
            route?.let {
                val updatedRoute = it.copy(status = status)
                repository.updateEvacuationRoute(updatedRoute)
            }
        }
    }

    fun getRoutesByVillage(fromVillage: String): List<EvacuationRoute> {
        return _evacuationRoutes.value.filter { it.fromVillage == fromVillage }
    }

    fun getRoutesByRiskLevel(riskLevel: RiskLevelType): List<EvacuationRoute> {
        return _evacuationRoutes.value.filter { it.riskLevel == riskLevel }
    }

    fun getOpenRoutes(): List<EvacuationRoute> {
        return _evacuationRoutes.value.filter { it.status == RouteStatusLevel.OPEN }
    }

    // ==================== SHELTER METHODS ====================

    fun selectShelter(shelterId: String) {
        viewModelScope.launch {
            _selectedShelter.value = repository.getShelterById(shelterId)
        }
    }

    fun updateShelterStatus(shelterId: String, status: ShelterStatusLevel) {
        viewModelScope.launch {
            val shelter = repository.getShelterById(shelterId)
            shelter?.let {
                val updatedShelter = it.copy(status = status)
                repository.updateShelter(updatedShelter)
            }
        }
    }

    fun updateShelterResources(shelterId: String, resources: ShelterResources) {
        viewModelScope.launch {
            val shelter = repository.getShelterById(shelterId)
            shelter?.let {
                val updatedShelter = it.copy(resources = resources)
                repository.updateShelter(updatedShelter)
            }
        }
    }

    fun getOpenShelters(): List<Shelter> {
        return _shelters.value.filter { it.status == ShelterStatusLevel.OPEN }
    }

    fun getAvailableCapacity(): Int {
        return _shelters.value.sumOf { it.capacity - it.currentOccupancy }
    }

    // ==================== RISK ASSESSMENT METHODS ====================

    fun selectVillageRisk(villageId: String) {
        viewModelScope.launch {
            _selectedVillageRisk.value = repository.getVillageRisk(villageId)
        }
    }

    fun getVillageRisk(villageId: String): RiskAssessment? {
        return repository.getVillageRisk(villageId)
    }

    fun getHighRiskVillages(): List<RiskAssessment> {
        return _riskAssessments.value.filter {
            it.overallRisk == RiskLevelType.HIGH || it.overallRisk == RiskLevelType.CRITICAL
        }
    }

    // ==================== RESOURCE REQUEST METHODS ====================

    fun addResourceRequest(request: ResourceRequest) {
        viewModelScope.launch {
            repository.addResourceRequest(request)
            refreshResourceRequests()
        }
    }

    fun updateResourceRequestStatus(requestId: String, status: RequestStatusType, assignedTo: String?) {
        viewModelScope.launch {
            repository.updateResourceRequest(requestId, status, assignedTo)
            refreshResourceRequests()
        }
    }

    fun approveRequest(requestId: String, assignedTo: String?) {
        updateResourceRequestStatus(requestId, RequestStatusType.APPROVED, assignedTo)
    }

    fun rejectRequest(requestId: String) {
        updateResourceRequestStatus(requestId, RequestStatusType.REJECTED, null)
    }

    fun completeRequest(requestId: String) {
        updateResourceRequestStatus(requestId, RequestStatusType.COMPLETED, null)
    }

    private suspend fun refreshResourceRequests() {
        _resourceRequests.value = repository.getResourceRequests()
        _pendingRequests.value = repository.getResourceRequests(RequestStatusType.PENDING)
    }

    // ==================== EMERGENCY BROADCAST ====================

    fun triggerEmergencyBroadcast(message: String, severity: AlertSeverityLevel) {
        viewModelScope.launch {
            repository.triggerEmergencyBroadcast(message, severity)
        }
    }

    // ==================== STATISTICS ====================

    private fun updateEvacuationSummary() {
        val summary = EvacuationSummary(
            totalEvacuated = _shelters.value.sumOf { it.currentOccupancy },
            activeEvacuations = _activeAlerts.value.size,
            sheltersOpen = _shelters.value.count { it.status == ShelterStatusLevel.OPEN },
            availableCapacity = getAvailableCapacity(),
            routesOpen = _evacuationRoutes.value.count { it.status == RouteStatusLevel.OPEN },
            lastUpdated = System.currentTimeMillis()
        )
        _evacuationSummary.value = summary
    }

    // ==================== UTILITY METHODS ====================

    fun refreshManually() {
        viewModelScope.launch {
            _isLoading.value = true
            refreshData()
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ==================== CLEANUP ====================

    override fun onCleared() {
        super.onCleared()
        repository.stopSimulation()
    }
}