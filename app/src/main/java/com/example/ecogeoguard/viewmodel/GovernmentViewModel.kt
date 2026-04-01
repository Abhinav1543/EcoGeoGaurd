package com.example.ecogeoguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecogeoguard.data.model.*
import com.example.ecogeoguard.data.repository.GovernmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GovernmentViewModel @Inject constructor(
    private val repository: GovernmentRepository
) : ViewModel() {

    // State flows
    private val _districtStats = MutableStateFlow<DistrictStats?>(null)
    val districtStats: StateFlow<DistrictStats?> = _districtStats.asStateFlow()

    private val _villageAnalytics = MutableStateFlow<List<VillageAnalytics>>(emptyList())
    val villageAnalytics: StateFlow<List<VillageAnalytics>> = _villageAnalytics.asStateFlow()

    private val _budgetAllocations = MutableStateFlow<List<BudgetAllocation>>(emptyList())
    val budgetAllocations: StateFlow<List<BudgetAllocation>> = _budgetAllocations.asStateFlow()

    private val _disasterReports = MutableStateFlow<List<DisasterImpactReport>>(emptyList())
    val disasterReports: StateFlow<List<DisasterImpactReport>> = _disasterReports.asStateFlow()

    private val _stakeholders = MutableStateFlow<List<Stakeholder>>(emptyList())
    val stakeholders: StateFlow<List<Stakeholder>> = _stakeholders.asStateFlow()

    private val _policyRecommendations = MutableStateFlow<List<PolicyRecommendation>>(emptyList())
    val policyRecommendations: StateFlow<List<PolicyRecommendation>> = _policyRecommendations.asStateFlow()

    private val _selectedVillage = MutableStateFlow<VillageAnalytics?>(null)
    val selectedVillage: StateFlow<VillageAnalytics?> = _selectedVillage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastUpdated = MutableStateFlow(System.currentTimeMillis())
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    init {
        loadData()
        observeRepositoryFlows()
        startRealTimeUpdates()
    }

    private fun observeRepositoryFlows() {
        viewModelScope.launch {
            repository.districtStatsFlow.collect { stats ->
                _districtStats.value = stats
            }
        }

        viewModelScope.launch {
            repository.villageAnalyticsFlow.collect { villages ->
                _villageAnalytics.value = villages
            }
        }

        viewModelScope.launch {
            repository.budgetFlow.collect { budgets ->
                _budgetAllocations.value = budgets
            }
        }

        viewModelScope.launch {
            repository.disasterReportsFlow.collect { reports ->
                _disasterReports.value = reports
            }
        }

        viewModelScope.launch {
            repository.stakeholdersFlow.collect { stakeholders ->
                _stakeholders.value = stakeholders
            }
        }

        viewModelScope.launch {
            repository.policyFlow.collect { policies ->
                _policyRecommendations.value = policies
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _districtStats.value = repository.getDistrictOverview()?.let {
                    DistrictStats(
                        totalVillages = repository.getVillageAnalytics().size,
                        totalPopulation = repository.getVillageAnalytics().sumOf { it.population },
                        totalFarmers = repository.getVillageAnalytics().sumOf { it.farmers },
                        totalLivestock = repository.getVillageAnalytics().sumOf { it.livestock },
                        avgHealthScore = repository.getVillageAnalytics().map { it.healthScore }.average().toFloat(),
                        avgCropYield = repository.getVillageAnalytics().map { it.cropYield }.average().toFloat(),
                        totalAlerts = repository.getVillageAnalytics().sumOf { it.activeAlerts },
                        resolvedAlerts = repository.getVillageAnalytics().sumOf { it.activeAlerts } - 5,
                        totalBudget = repository.getBudgetAllocations().sumOf { it.allocatedAmount },
                        utilizedBudget = repository.getBudgetAllocations().sumOf { it.utilizedAmount },
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                _villageAnalytics.value = repository.getVillageAnalytics()
                _budgetAllocations.value = repository.getBudgetAllocations()
                _disasterReports.value = repository.getDisasterReports()
                _stakeholders.value = repository.getStakeholders()
                _policyRecommendations.value = repository.getPolicyRecommendations()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startRealTimeUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(60000) // Update every minute
                refreshData()
                _lastUpdated.value = System.currentTimeMillis()
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _villageAnalytics.value = repository.getVillageAnalytics()
            _budgetAllocations.value = repository.getBudgetAllocations()
            _stakeholders.value = repository.getStakeholders()
            _policyRecommendations.value = repository.getPolicyRecommendations()
        }
    }

    fun selectVillage(villageId: String) {
        viewModelScope.launch {
            _selectedVillage.value = repository.getVillageAnalyticsById(villageId)
        }
    }

    fun updateRecommendationStatus(recommendationId: String, status: RecommendationStatus) {
        viewModelScope.launch {
            repository.updateRecommendationStatus(recommendationId, status)
        }
    }

    fun getVillageAnalytics(): List<VillageAnalytics> = repository.getVillageAnalytics()

    fun getBudgetByCategory(category: BudgetCategory): List<BudgetAllocation> = repository.getBudgetByCategory(category)

    fun getStakeholdersByRole(role: UserRole): List<Stakeholder> = repository.getStakeholdersByRole(role)

    fun generatePerformanceReport(): Map<String, Any> = repository.generatePerformanceReport()

    fun refreshManually() {
        viewModelScope.launch {
            _isLoading.value = true
            refreshData()
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopSimulation()
    }
}