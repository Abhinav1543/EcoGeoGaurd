// viewmodel/AdminViewModel.kt
package com.example.ecogeoguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecogeoguard.data.model.Alert
import com.example.ecogeoguard.data.model.SystemHealth
import com.example.ecogeoguard.data.model.Village

import com.example.ecogeoguard.data.repository.AlertRepository
import com.example.ecogeoguard.data.repository.SystemRepository
import com.example.ecogeoguard.data.repository.VillageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val villageRepository: VillageRepository,
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _systemHealth = MutableStateFlow<SystemHealth?>(null)
    val systemHealth: StateFlow<SystemHealth?> = _systemHealth.asStateFlow()

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _villages = MutableStateFlow<List<Village>>(emptyList())
    val villages: StateFlow<List<Village>> = _villages.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            // Load system health
            _systemHealth.value = systemRepository.getSystemHealth()

            // Load alerts
            _alerts.value = alertRepository.getRecentAlerts()

            // Load villages
            _villages.value = villageRepository.getAllVillages()
        }
    }

    fun refreshDashboardData() {
        loadDashboardData()
    }

    fun markAlertAsRead(alertId: String) {
        viewModelScope.launch {
            alertRepository.markAsRead(alertId)
            loadDashboardData() // Refresh data
        }
    }

    fun updateVillageRisk(villageId: String, riskLevel: Village.RiskLevel) {
        viewModelScope.launch {
            villageRepository.updateRiskLevel(villageId, riskLevel)
            loadDashboardData() // Refresh data
        }
    }
}