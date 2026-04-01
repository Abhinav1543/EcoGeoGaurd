package com.example.ecogeoguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecogeoguard.data.model.*
import com.example.ecogeoguard.data.repository.FarmerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class FarmerViewModel @Inject constructor(
    private val farmerRepository: FarmerRepository
) : ViewModel() {

    private val _fields = MutableStateFlow<List<Field>>(emptyList())
    val fields: StateFlow<List<Field>> = _fields.asStateFlow()

    private val _selectedField = MutableStateFlow<Field?>(null)
    val selectedField: StateFlow<Field?> = _selectedField.asStateFlow()

    private val _currentSensorData = MutableStateFlow<FieldSensorData?>(null)
    val currentSensorData: StateFlow<FieldSensorData?> = _currentSensorData.asStateFlow()

    private val _sensorHistory = MutableStateFlow<List<FieldSensorData>>(emptyList())
    val sensorHistory: StateFlow<List<FieldSensorData>> = _sensorHistory.asStateFlow()

    private val _irrigationRecommendation = MutableStateFlow<IrrigationRecommendation?>(null)
    val irrigationRecommendation: StateFlow<IrrigationRecommendation?> = _irrigationRecommendation.asStateFlow()

    private val _cropHealth = MutableStateFlow<CropHealthIndex?>(null)
    val cropHealth: StateFlow<CropHealthIndex?> = _cropHealth.asStateFlow()

    private val _activeAlerts = MutableStateFlow<List<WeatherAlert>>(emptyList())
    val activeAlerts: StateFlow<List<WeatherAlert>> = _activeAlerts.asStateFlow()

    private val _unreadAlertCount = MutableStateFlow(0)
    val unreadAlertCount: StateFlow<Int> = _unreadAlertCount.asStateFlow()

    private val _isLoading = MutableStateFlow(true) // ← TRUE rakho pehle
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastUpdated = MutableStateFlow(System.currentTimeMillis())
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    private val _showEmergencyBuzzer = MutableStateFlow(false)
    val showEmergencyBuzzer: StateFlow<Boolean> = _showEmergencyBuzzer.asStateFlow()

    private var currentEmergencyAlert: WeatherAlert? = null
    private var updateJob: kotlinx.coroutines.Job? = null

    init {
        loadFarmerData()
        startRealTimeUpdates() // ← YEH UNCOMMENT KARO
    }

    private fun loadFarmerData() {
        viewModelScope.launch {
            android.util.Log.d("FARMER_TEST", "Loading farmer data START")

            _isLoading.value = true // ← YEH IMPORTANT HAI

            try {
                val fields = farmerRepository.getFieldsForFarmer("farmer_001")
                android.util.Log.d("FARMER_TEST", "Fields loaded: ${fields.size}")

                _fields.value = fields
                refreshAlerts()

                fields.firstOrNull()?.let {
                    selectField(it.id)
                }

                // THODA DELAY DEKAR LOADING HATAYO (UI KO DIKHNE DO)
                kotlinx.coroutines.delay(500)

            } catch (e: Exception) {
                android.util.Log.e("FARMER_TEST", "Error loading data", e)
            } finally {
                _isLoading.value = false
                android.util.Log.d("FARMER_TEST", "Loading farmer data FINISHED")
            }
        }
    }

    fun selectField(fieldId: String) {
        viewModelScope.launch {
            val field = farmerRepository.getFieldById(fieldId)
            _selectedField.value = field

            field?.let {
                _currentSensorData.value = farmerRepository.getLatestSensorData(fieldId)
                _sensorHistory.value = farmerRepository.getSensorDataHistory(fieldId, 24)
                _irrigationRecommendation.value = farmerRepository.getLatestIrrigationRecommendation(fieldId)
                _cropHealth.value = farmerRepository.getLatestCropHealth(fieldId)
            }
        }
    }

    private fun startRealTimeUpdates() {
        updateJob?.cancel()

        updateJob = viewModelScope.launch {
            while (isActive) {
                delay(15000) // 15 seconds
                refreshData()
                _lastUpdated.value = System.currentTimeMillis()
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _selectedField.value?.let { field ->
                val newSensorData = farmerRepository.getLatestSensorData(field.id)
                val oldSensorData = _currentSensorData.value

                _currentSensorData.value = newSensorData
                _sensorHistory.value = farmerRepository.getSensorDataHistory(field.id, 24)
                _irrigationRecommendation.value = farmerRepository.getLatestIrrigationRecommendation(field.id)
                _cropHealth.value = farmerRepository.getLatestCropHealth(field.id)

                refreshAlerts()
                checkForEmergencyConditions(newSensorData, oldSensorData)
            }
        }
    }

    private fun refreshAlerts() {
        val fieldIds = _fields.value.map { it.id }
        _activeAlerts.value = farmerRepository.getActiveAlerts(fieldIds)
        _unreadAlertCount.value = farmerRepository.getUnreadAlertCount()
    }

    private fun checkForEmergencyConditions(
        newData: FieldSensorData?,
        oldData: FieldSensorData?
    ) {
        if (newData == null) return

        val landslideRisk = (newData.vibrationLevel ?: 0f) > 1.2f &&
                newData.rainfall > 15f &&
                newData.soilMoisture > 65f

        val droughtRisk = newData.soilMoisture < 20f &&
                newData.ambientTemperature > 38f

        val suddenChange = oldData != null && (
                abs((newData.vibrationLevel ?: 0f) - (oldData.vibrationLevel ?: 0f)) > 1.0f ||
                        abs(newData.rainfall - oldData.rainfall) > 20f ||
                        abs(newData.soilMoisture - oldData.soilMoisture) > 15f
                )

        val shouldTriggerBuzzer = landslideRisk || droughtRisk || suddenChange

        if (shouldTriggerBuzzer && !_showEmergencyBuzzer.value) {
            currentEmergencyAlert = _activeAlerts.value.firstOrNull {
                it.severity == RiskLevel.CRITICAL || it.severity == RiskLevel.HIGH
            }
            _showEmergencyBuzzer.value = true
        } else if (!shouldTriggerBuzzer && _showEmergencyBuzzer.value) {
            viewModelScope.launch {
                delay(30000)
                if (!checkCurrentEmergencyConditions()) {
                    _showEmergencyBuzzer.value = false
                }
            }
        }
    }

    private fun checkCurrentEmergencyConditions(): Boolean {
        val data = _currentSensorData.value ?: return false
        return (data.vibrationLevel ?: 0f) > 1.2f ||
                data.rainfall > 15f ||
                data.soilMoisture < 20f
    }

    fun dismissEmergency() {
        _showEmergencyBuzzer.value = false
    }

    fun getEmergencyAlert(): WeatherAlert? = currentEmergencyAlert

    fun simulateRainfall(intensity: Float) {
        _selectedField.value?.let { field ->
            viewModelScope.launch {
                farmerRepository.simulateRainfall(field.id, intensity)
            }
        }
    }

    fun refreshManually() {
        viewModelScope.launch {
            _isLoading.value = true
            refreshData()
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        updateJob?.cancel()
        farmerRepository.stopSimulation()
        super.onCleared()
    }
}