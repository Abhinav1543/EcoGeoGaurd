package com.example.ecogeoguard.data.repository

import com.example.ecogeoguard.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.random.Random

@Singleton
class GovernmentRepository @Inject constructor() {

    private val TAG = "GovRepo"

    // Data storage
    private val districtOverview = mutableListOf<DistrictOverview>()
    private val villageAnalytics = mutableListOf<VillageAnalytics>()
    private val budgetAllocations = mutableListOf<BudgetAllocation>()
    private val disasterReports = mutableListOf<DisasterImpactReport>()
    private val stakeholders = mutableListOf<Stakeholder>()
    private val policyRecommendations = mutableListOf<PolicyRecommendation>()

    // State flows
    private val _districtStatsFlow = MutableStateFlow<DistrictStats?>(null)
    val districtStatsFlow: StateFlow<DistrictStats?> = _districtStatsFlow.asStateFlow()

    private val _villageAnalyticsFlow = MutableStateFlow<List<VillageAnalytics>>(emptyList())
    val villageAnalyticsFlow: StateFlow<List<VillageAnalytics>> = _villageAnalyticsFlow.asStateFlow()

    private val _budgetFlow = MutableStateFlow<List<BudgetAllocation>>(emptyList())
    val budgetFlow: StateFlow<List<BudgetAllocation>> = _budgetFlow.asStateFlow()

    private val _disasterReportsFlow = MutableStateFlow<List<DisasterImpactReport>>(emptyList())
    val disasterReportsFlow: StateFlow<List<DisasterImpactReport>> = _disasterReportsFlow.asStateFlow()

    private val _stakeholdersFlow = MutableStateFlow<List<Stakeholder>>(emptyList())
    val stakeholdersFlow: StateFlow<List<Stakeholder>> = _stakeholdersFlow.asStateFlow()

    private val _policyFlow = MutableStateFlow<List<PolicyRecommendation>>(emptyList())
    val policyFlow: StateFlow<List<PolicyRecommendation>> = _policyFlow.asStateFlow()

    private var isSimulating = false

    init {
        initializeSampleData()
        startSimulation()
    }

    private fun initializeSampleData() {
        // Create district overview
        districtOverview.add(
            DistrictOverview(
                districtId = "dist_001",
                districtName = "Himalayan District",
                totalVillages = 25,
                totalPopulation = 125000,
                totalLivestock = 45000,
                totalFarmers = 8500,
                totalSensors = 120,
                activeAlerts = 8,
                criticalAlerts = 3,
                avgHealthScore = 72.5f,
                cropProductivityScore = 68.3f,
                disasterReadinessScore = 65.8f
            )
        )

        // Create village analytics
        val villages = listOf(
            "Himalayan Village", "River Valley", "Hill Station", "Mountain Base",
            "Forest Edge", "Lake Town", "Green Valley", "Sunrise Village",
            "Meadow Fields", "Highland Farms"
        )

        villages.forEachIndexed { index, village ->
            val riskLevel = when {
                index < 2 -> RiskLevelType.CRITICAL
                index < 5 -> RiskLevelType.HIGH
                index < 8 -> RiskLevelType.MODERATE
                else -> RiskLevelType.LOW
            }

            villageAnalytics.add(
                VillageAnalytics(
                    villageId = "village_${String.format("%03d", index + 1)}",
                    villageName = village,
                    districtId = "dist_001",
                    population = 2000 + Random.nextInt(8000),
                    livestock = 500 + Random.nextInt(2000),
                    farmers = 100 + Random.nextInt(500),
                    sensors = 5 + Random.nextInt(15),
                    activeAlerts = Random.nextInt(5),
                    overallRisk = riskLevel,
                    healthScore = 50 + Random.nextInt(50).toFloat(),
                    cropYield = 2.0f + Random.nextFloat() * 3.0f,
                    irrigationCoverage = 30 + Random.nextInt(60).toFloat(),
                    vaccinationCoverage = 40 + Random.nextInt(50).toFloat(),
                    disasterPreparedness = 20 + Random.nextInt(70).toFloat(),
                    budgetAllocated = 50.0 + Random.nextDouble() * 200.0,
                    budgetUtilized = 30.0 + Random.nextDouble() * 150.0,
                    lastAssessment = System.currentTimeMillis()
                )
            )
        }

        // Create budget allocations
        val schemes = listOf(
            Triple("PM-KISAN", "PM-KISAN-001", BudgetCategory.AGRICULTURE),
            Triple("National Livestock Mission", "NLM-001", BudgetCategory.LIVESTOCK),
            Triple("SDRF", "SDRF-001", BudgetCategory.DISASTER_RELIEF),
            Triple("RKVY", "RKVY-001", BudgetCategory.AGRICULTURE),
            Triple("ATMA", "ATMA-001", BudgetCategory.AGRICULTURE),
            Triple("AHIDF", "AHIDF-001", BudgetCategory.LIVESTOCK),
            Triple("PMGSY", "PMGSY-001", BudgetCategory.INFRASTRUCTURE),
            Triple("NHM", "NHM-001", BudgetCategory.HEALTHCARE)
        )

        schemes.forEachIndexed { index, (name, code, category) ->
            val allocated = 50.0 + Random.nextDouble() * 450.0
            val utilized = allocated * (0.3 + Random.nextDouble() * 0.6)

            budgetAllocations.add(
                BudgetAllocation(
                    id = "budget_${String.format("%03d", index + 1)}",
                    schemeName = name,
                    schemeCode = code,
                    category = category,
                    allocatedAmount = allocated,
                    utilizedAmount = utilized,
                    remainingAmount = allocated - utilized,
                    villagesCovered = villages.take(Random.nextInt(3, 8)),
                    beneficiaries = 1000 + Random.nextInt(5000),
                    startDate = System.currentTimeMillis() - Random.nextLong(180L * 24 * 60 * 60 * 1000),
                    endDate = System.currentTimeMillis() + Random.nextLong(180L * 24 * 60 * 60 * 1000),
                    status = BudgetStatus.values().random(),
                    utilizationRate = (utilized / allocated * 100).toFloat(),
                    performanceScore = 40 + Random.nextInt(60).toFloat()
                )
            )
        }

        // Create sample disaster reports
        disasterReports.addAll(
            listOf(
                DisasterImpactReport(
                    reportId = "rep_001",
                    disasterType = DisasterType.LANDSLIDE,
                    severity = AlertSeverityLevel.HIGH,
                    date = System.currentTimeMillis() - 45 * 24 * 60 * 60 * 1000,
                    affectedVillages = listOf("Himalayan Village", "Hill Station"),
                    totalAffectedPopulation = 2500,
                    totalLivestockLoss = 150,
                    totalCropLoss = 45.5f,
                    totalInfrastructureDamage = 1250000.0,
                    totalFinancialLoss = 2850000.0,
                    reliefFundsReleased = 1500000.0,
                    reliefDistributed = true,
                    recoveryProgress = 85.0f,
                    recommendations = listOf(
                        "Strengthen slope stabilization",
                        "Early warning system installation",
                        "Community training programs"
                    )
                ),
                DisasterImpactReport(
                    reportId = "rep_002",
                    disasterType = DisasterType.FLASH_FLOOD,
                    severity = AlertSeverityLevel.HIGH,
                    date = System.currentTimeMillis() - 90 * 24 * 60 * 60 * 1000,
                    affectedVillages = listOf("River Valley", "Lowland Area"),
                    totalAffectedPopulation = 3800,
                    totalLivestockLoss = 320,
                    totalCropLoss = 78.2f,
                    totalInfrastructureDamage = 2100000.0,
                    totalFinancialLoss = 4250000.0,
                    reliefFundsReleased = 2800000.0,
                    reliefDistributed = true,
                    recoveryProgress = 72.0f,
                    recommendations = listOf(
                        "Improve drainage systems",
                        "Flood plain zoning",
                        "Emergency shelters construction"
                    )
                )
            )
        )

        // Create stakeholders
        val roles = listOf(UserRole.ADMIN, UserRole.FARMER, UserRole.LIVESTOCK_OWNER, UserRole.DISASTER_TEAM)
        val names = listOf("Rajesh Kumar", "Priya Sharma", "Amit Singh", "Neha Gupta", "Vikram Mehta")

        for (i in 1..20) {
            stakeholders.add(
                Stakeholder(
                    id = "user_${String.format("%03d", i)}",
                    name = names.random() + " " + i,
                    role = roles.random(),
                    villageId = villageAnalytics.random().villageId,
                    contactNumber = "+91 98765 ${String.format("%05d", i)}",
                    email = "user$i@ecogeoguard.gov.in",
                    lastActive = System.currentTimeMillis() - Random.nextLong(7 * 24 * 60 * 60 * 1000),
                    performanceScore = 50 + Random.nextInt(50).toFloat(),
                    tasksCompleted = Random.nextInt(50),
                    pendingTasks = Random.nextInt(10),
                    responseTime = Random.nextInt(5, 60)
                )
            )
        }

        // Create policy recommendations
        policyRecommendations.addAll(
            listOf(
                PolicyRecommendation(
                    id = "policy_001",
                    title = "Expand IoT Sensor Network",
                    category = PolicyCategory.TECHNOLOGY,
                    priority = Priority.HIGH,
                    description = "Increase sensor coverage in high-risk villages to improve early warning capabilities.",
                    expectedImpact = "Reduce disaster response time by 40%",
                    estimatedCost = 2500000.0,
                    timeFrame = "6 months",
                    stakeholders = listOf("District Administration", "Agriculture Dept", "Revenue Dept"),
                    metrics = listOf("Sensor count", "Alert accuracy", "Response time"),
                    aiConfidence = 0.92f,
                    createdAt = System.currentTimeMillis(),
                    status = RecommendationStatus.PENDING
                ),
                PolicyRecommendation(
                    id = "policy_002",
                    title = "Subsidized Smart Farming Equipment",
                    category = PolicyCategory.AGRICULTURE,
                    priority = Priority.HIGH,
                    description = "Provide subsidies for IoT-enabled irrigation and crop monitoring systems.",
                    expectedImpact = "Increase crop yield by 25%",
                    estimatedCost = 5000000.0,
                    timeFrame = "1 year",
                    stakeholders = listOf("Agriculture Dept", "Finance Dept", "Farmers"),
                    metrics = listOf("Irrigation efficiency", "Crop yield", "Water savings"),
                    aiConfidence = 0.88f,
                    createdAt = System.currentTimeMillis(),
                    status = RecommendationStatus.PENDING
                ),
                PolicyRecommendation(
                    id = "policy_003",
                    title = "Livestock Insurance Program",
                    category = PolicyCategory.LIVESTOCK,
                    priority = Priority.MEDIUM,
                    description = "Introduce government-subsidized insurance for livestock owners.",
                    expectedImpact = "Reduce financial loss during disasters by 60%",
                    estimatedCost = 15000000.0,
                    timeFrame = "2 years",
                    stakeholders = listOf("Animal Husbandry Dept", "Insurance Companies"),
                    metrics = listOf("Insurance coverage", "Claims processed", "Loss reduction"),
                    aiConfidence = 0.85f,
                    createdAt = System.currentTimeMillis(),
                    status = RecommendationStatus.PENDING
                ),
                PolicyRecommendation(
                    id = "policy_004",
                    title = "Disaster Response Training Program",
                    category = PolicyCategory.DISASTER_MANAGEMENT,
                    priority = Priority.URGENT,
                    description = "Train community volunteers in disaster response and first aid.",
                    expectedImpact = "Improve community resilience",
                    estimatedCost = 750000.0,
                    timeFrame = "3 months",
                    stakeholders = listOf("Disaster Management Authority", "Local NGOs"),
                    metrics = listOf("Trained volunteers", "Response time", "Satisfaction rate"),
                    aiConfidence = 0.95f,
                    createdAt = System.currentTimeMillis(),
                    status = RecommendationStatus.PENDING
                )
            )
        )

        updateDistrictStats()

        // Update flows
        _villageAnalyticsFlow.value = villageAnalytics
        _budgetFlow.value = budgetAllocations
        _disasterReportsFlow.value = disasterReports
        _stakeholdersFlow.value = stakeholders
        _policyFlow.value = policyRecommendations
    }

    private fun updateDistrictStats() {
        val stats = DistrictStats(
            totalVillages = villageAnalytics.size,
            totalPopulation = villageAnalytics.sumOf { it.population },
            totalFarmers = villageAnalytics.sumOf { it.farmers },
            totalLivestock = villageAnalytics.sumOf { it.livestock },
            avgHealthScore = villageAnalytics.map { it.healthScore }.average().toFloat(),
            avgCropYield = villageAnalytics.map { it.cropYield }.average().toFloat(),
            totalAlerts = villageAnalytics.sumOf { it.activeAlerts },
            resolvedAlerts = villageAnalytics.sumOf { it.activeAlerts } - 5,
            totalBudget = budgetAllocations.sumOf { it.allocatedAmount },
            utilizedBudget = budgetAllocations.sumOf { it.utilizedAmount },
            lastUpdated = System.currentTimeMillis()
        )
        _districtStatsFlow.value = stats
    }

    private fun startSimulation() {
        if (isSimulating) return
        isSimulating = true

        Thread {
            while (isSimulating) {
                Thread.sleep(60000) // Update every minute
                updateAnalytics()
                updateBudgetUtilization()
                updateStakeholderPerformance()
                updateDistrictStats()

                // Update flows
                _villageAnalyticsFlow.value = villageAnalytics.toList()
                _budgetFlow.value = budgetAllocations.toList()
                _stakeholdersFlow.value = stakeholders.toList()
            }
        }.start()
    }

    private fun updateAnalytics() {
        villageAnalytics.forEachIndexed { index, village ->
            val updatedHealth = (village.healthScore + (Random.nextFloat() - 0.5f) * 2).coerceIn(0f, 100f)
            villageAnalytics[index] = village.copy(
                healthScore = updatedHealth,
                activeAlerts = max(0, village.activeAlerts + Random.nextInt(-1, 2))
            )
        }
    }

    private fun updateBudgetUtilization() {
        budgetAllocations.forEachIndexed { index, budget ->
            val newUtilized = (budget.utilizedAmount + Random.nextDouble() * 1000).coerceAtMost(budget.allocatedAmount)
            budgetAllocations[index] = budget.copy(
                utilizedAmount = newUtilized,
                remainingAmount = budget.allocatedAmount - newUtilized,
                utilizationRate = (newUtilized / budget.allocatedAmount * 100).toFloat()
            )
        }
    }

    private fun updateStakeholderPerformance() {
        stakeholders.forEachIndexed { index, stakeholder ->
            stakeholders[index] = stakeholder.copy(
                performanceScore = (stakeholder.performanceScore + (Random.nextFloat() - 0.5f) * 2).coerceIn(0f, 100f),
                tasksCompleted = stakeholder.tasksCompleted + Random.nextInt(0, 2)
            )
        }
    }

    // Public API methods
    fun getDistrictOverview(): DistrictOverview? = districtOverview.firstOrNull()

    fun getVillageAnalytics(): List<VillageAnalytics> = villageAnalytics

    fun getVillageAnalyticsById(villageId: String): VillageAnalytics? = villageAnalytics.find { it.villageId == villageId }

    fun getBudgetAllocations(): List<BudgetAllocation> = budgetAllocations

    fun getBudgetByCategory(category: BudgetCategory): List<BudgetAllocation> = budgetAllocations.filter { it.category == category }

    fun getDisasterReports(): List<DisasterImpactReport> = disasterReports

    fun getStakeholders(): List<Stakeholder> = stakeholders

    fun getStakeholdersByRole(role: UserRole): List<Stakeholder> = stakeholders.filter { it.role == role }

    fun getPolicyRecommendations(): List<PolicyRecommendation> = policyRecommendations

    fun updateRecommendationStatus(recommendationId: String, status: RecommendationStatus) {
        val index = policyRecommendations.indexOfFirst { it.id == recommendationId }
        if (index >= 0) {
            policyRecommendations[index] = policyRecommendations[index].copy(
                status = status
            )
            _policyFlow.value = policyRecommendations.toList()
        }
    }

    fun addPolicyRecommendation(recommendation: PolicyRecommendation) {
        policyRecommendations.add(recommendation)
        _policyFlow.value = policyRecommendations.toList()
    }

    fun generatePerformanceReport(): Map<String, Any> {
        return mapOf(
            "districtStats" to _districtStatsFlow.value,
            "villagePerformance" to villageAnalytics.map { it.healthScore }.average(),
            "budgetUtilization" to budgetAllocations.map { it.utilizationRate }.average(),
            "stakeholderPerformance" to stakeholders.map { it.performanceScore }.average()
        ) as Map<String, Any>
    }

    fun stopSimulation() {
        isSimulating = false
    }
}