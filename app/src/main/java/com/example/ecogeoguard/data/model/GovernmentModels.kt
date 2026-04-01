package com.example.ecogeoguard.data.model


import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class DistrictOverview(
    val districtId: String,
    val districtName: String,
    val totalVillages: Int,
    val totalPopulation: Int,
    val totalLivestock: Int,
    val totalFarmers: Int,
    val totalSensors: Int,
    val activeAlerts: Int,
    val criticalAlerts: Int,
    val avgHealthScore: Float,
    val cropProductivityScore: Float,
    val disasterReadinessScore: Float,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class VillageAnalytics(
    val villageId: String,
    val villageName: String,
    val districtId: String,
    val population: Int,
    val livestock: Int,
    val farmers: Int,
    val sensors: Int,
    val activeAlerts: Int,
    val overallRisk: RiskLevelType,
    val healthScore: Float, // 0-100
    val cropYield: Float, // tons/hectare
    val irrigationCoverage: Float, // percentage
    val vaccinationCoverage: Float, // percentage
    val disasterPreparedness: Float, // percentage
    val budgetAllocated: Double, // in lakhs
    val budgetUtilized: Double,
    val lastAssessment: Long,
    val trends: Map<String, List<Float>> = emptyMap()
) : Parcelable

@Parcelize
data class BudgetAllocation(
    val id: String,
    val schemeName: String,
    val schemeCode: String,
    val category: BudgetCategory,
    val allocatedAmount: Double,
    val utilizedAmount: Double,
    val remainingAmount: Double,
    val villagesCovered: List<String>,
    val beneficiaries: Int,
    val startDate: Long,
    val endDate: Long,
    val status: BudgetStatus,
    val utilizationRate: Float,
    val performanceScore: Float
) : Parcelable

@Parcelize
data class DisasterImpactReport(
    val reportId: String,
    val disasterType: DisasterType,
    val severity: AlertSeverityLevel,
    val date: Long,
    val affectedVillages: List<String>,
    val totalAffectedPopulation: Int,
    val totalLivestockLoss: Int,
    val totalCropLoss: Float, // hectares
    val totalInfrastructureDamage: Double,
    val totalFinancialLoss: Double,
    val reliefFundsReleased: Double,
    val reliefDistributed: Boolean,
    val recoveryProgress: Float, // percentage
    val recommendations: List<String>
) : Parcelable

@Parcelize
data class Stakeholder(
    val id: String,
    val name: String,
    val role: UserRole,
    val villageId: String,
    val contactNumber: String,
    val email: String,
    val lastActive: Long,
    val performanceScore: Float,
    val tasksCompleted: Int,
    val pendingTasks: Int,
    val responseTime: Int // minutes
) : Parcelable

@Parcelize
data class PolicyRecommendation(
    val id: String,
    val title: String,
    val category: PolicyCategory,
    val priority: Priority,
    val description: String,
    val expectedImpact: String,
    val estimatedCost: Double,
    val timeFrame: String, // e.g., "3 months", "1 year"
    val stakeholders: List<String>,
    val metrics: List<String>,
    val aiConfidence: Float,
    val createdAt: Long,
    val status: RecommendationStatus
) : Parcelable

@Parcelize
data class DistrictStats(
    val totalVillages: Int,
    val totalPopulation: Int,
    val totalFarmers: Int,
    val totalLivestock: Int,
    val avgHealthScore: Float,
    val avgCropYield: Float,
    val totalAlerts: Int,
    val resolvedAlerts: Int,
    val totalBudget: Double,
    val utilizedBudget: Double,
    val lastUpdated: Long
) : Parcelable

@Parcelize
data class PerformanceMetrics(
    val metricName: String,
    val currentValue: Float,
    val targetValue: Float,
    val previousValue: Float,
    val trend: Trend,
    val unit: String
) : Parcelable

enum class BudgetCategory {
    AGRICULTURE, LIVESTOCK, DISASTER_RELIEF, INFRASTRUCTURE, HEALTHCARE, EDUCATION
}

enum class BudgetStatus {
    PLANNED, ACTIVE, COMPLETED, ON_HOLD, AUDIT
}

enum class PolicyCategory {
    AGRICULTURE, LIVESTOCK, DISASTER_MANAGEMENT, INFRASTRUCTURE, TECHNOLOGY, COMMUNITY
}

enum class Priority {
    URGENT, HIGH, MEDIUM, LOW
}

enum class RecommendationStatus {
    PENDING, APPROVED, IMPLEMENTED, REJECTED
}

enum class Trend {
    UP, DOWN, STABLE
}