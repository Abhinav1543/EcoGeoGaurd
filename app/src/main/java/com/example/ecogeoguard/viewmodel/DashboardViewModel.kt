// viewmodel/DashboardViewModel.kt
package com.example.ecogeoguard.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecogeoguard.data.model.Alert
import com.example.ecogeoguard.data.model.WeatherData
import com.example.ecogeoguard.data.repository.AlertRepository
import com.example.ecogeoguard.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _notificationCount = MutableLiveData<Int>(0)
    val notificationCount: LiveData<Int> = _notificationCount

    private val _riskLevel = MutableLiveData<Int>(30)
    val riskLevel: LiveData<Int> = _riskLevel

    private val _weatherData = MutableLiveData<WeatherData>()
    val weatherData: LiveData<WeatherData> = _weatherData

    private val _recentAlerts = MutableLiveData<List<Alert>>(emptyList())
    val recentAlerts: LiveData<List<Alert>> = _recentAlerts

    private var simulationJob: Job? = null

    init {
        // Initialize with default data
        _weatherData.value = WeatherData(
            temperature = 28.5f,
            humidity = 65,
            rainfall = 12.4f,
            windSpeed = 8.2f,
            condition = "Partly Cloudy"
        )

        // Load initial data
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _notificationCount.value = alertRepository.getUnreadAlertsCount()
            _recentAlerts.value = alertRepository.getRecentAlerts()

            weatherRepository.getCurrentWeather()?.let {
                _weatherData.value = it
            }
        }
    }

    fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (true) {
                // Simulate data changes every 10 seconds
                simulateDataUpdate()
                delay(10000)
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    private fun simulateDataUpdate() {
        // Update risk level randomly (30-80)
        val newRisk = (30..80).random()
        _riskLevel.postValue(newRisk)

        // Update weather data
        val currentWeather = _weatherData.value ?: WeatherData(
            temperature = 28.5f,
            humidity = 65,
            rainfall = 12.4f,
            windSpeed = 8.2f,
            condition = "Partly Cloudy"
        )

        val updatedWeather = currentWeather.copy(
            temperature = currentWeather.temperature + (Random().nextFloat() * 2 - 1),
            humidity = (currentWeather.humidity + Random().nextInt(10) - 5).coerceIn(30, 90),
            rainfall = currentWeather.rainfall + Random().nextFloat() * 2,
            windSpeed = currentWeather.windSpeed + Random().nextFloat() * 2 - 1,
            condition = getRandomCondition()
        )
        _weatherData.postValue(updatedWeather)

        // Update notification count
        val newCount = alertRepository.getUnreadAlertsCount() + Random().nextInt(3)
        _notificationCount.postValue(newCount)
    }

    private fun getRandomCondition(): String {
        val conditions = listOf(
            "Sunny",
            "Partly Cloudy",
            "Cloudy",
            "Light Rain",
            "Heavy Rain",
            "Stormy"
        )
        return conditions.random()
    }

    fun refreshData() {
        viewModelScope.launch {
            // Fetch latest data from repositories
            _notificationCount.value = alertRepository.getUnreadAlertsCount()
            _recentAlerts.value = alertRepository.getRecentAlerts()

            // Get latest weather
            weatherRepository.getCurrentWeather()?.let {
                _weatherData.value = it
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            alertRepository.markAllAsRead()
            _notificationCount.value = 0
            _recentAlerts.value = alertRepository.getRecentAlerts()
        }
    }

    fun getCurrentWeather(): WeatherData {
        return _weatherData.value ?: WeatherData(
            temperature = 28.5f,
            humidity = 65,
            rainfall = 12.4f,
            windSpeed = 8.2f,
            condition = "Partly Cloudy"
        )
    }

    fun getRiskStatus(): String {
        val risk = _riskLevel.value ?: 30
        return when {
            risk > 70 -> "HIGH"
            risk > 40 -> "MODERATE"
            else -> "LOW"
        }
    }
}