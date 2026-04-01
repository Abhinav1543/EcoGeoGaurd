package com.example.ecogeoguard.ui.government.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.VillageAnalytics
import com.example.ecogeoguard.data.model.RiskLevelType
import com.example.ecogeoguard.databinding.ItemVillageAnalyticsBinding
import java.text.SimpleDateFormat
import java.util.*

class VillageAnalyticsAdapter(
    private val onItemClick: (VillageAnalytics) -> Unit,
    private val onCompareClick: (VillageAnalytics) -> Unit
) : ListAdapter<VillageAnalytics, VillageAnalyticsAdapter.VillageViewHolder>(VillageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VillageViewHolder {
        val binding = ItemVillageAnalyticsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VillageViewHolder(binding, onItemClick, onCompareClick)
    }

    override fun onBindViewHolder(holder: VillageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VillageViewHolder(
        private val binding: ItemVillageAnalyticsBinding,
        private val onItemClick: (VillageAnalytics) -> Unit,
        private val onCompareClick: (VillageAnalytics) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(village: VillageAnalytics) {
            binding.apply {
                tvVillageName.text = village.villageName

                // Risk badge
                val (riskText, riskColor) = when (village.overallRisk) {
                    RiskLevelType.LOW -> Pair("LOW", R.color.success)
                    RiskLevelType.MODERATE -> Pair("MODERATE", R.color.warning)
                    RiskLevelType.HIGH -> Pair("HIGH", R.color.warningOrange)
                    RiskLevelType.CRITICAL -> Pair("CRITICAL", R.color.dangerRed)
                }
                tvRiskLevel.text = riskText
                tvRiskLevel.setBackgroundColor(ContextCompat.getColor(root.context, riskColor))

                // Population
                tvPopulation.text = formatNumber(village.population)

                // Health Score
                tvHealthScore.text = "${String.format("%.1f", village.healthScore)}%"
                progressHealth.progress = village.healthScore.toInt()
                progressHealth.progressTintList = ContextCompat.getColorStateList(
                    root.context,
                    when {
                        village.healthScore >= 70 -> R.color.success
                        village.healthScore >= 50 -> R.color.warning
                        else -> R.color.dangerRed
                    }
                )

                // Crop Yield
                tvCropYield.text = "${String.format("%.1f", village.cropYield)} t/ha"

                // Budget Utilization
                val utilization = (village.budgetUtilized / village.budgetAllocated * 100).toInt()
                tvBudgetUtilization.text = "${utilization}%"
                progressBudget.progress = utilization

                // Compare button
                btnCompare.setOnClickListener { onCompareClick(village) }

                // Click listener
                root.setOnClickListener { onItemClick(village) }
            }
        }

        private fun formatNumber(number: Int): String {
            return when {
                number >= 1000000 -> "${number / 1000000}M"
                number >= 1000 -> "${number / 1000}K"
                else -> number.toString()
            }
        }
    }

    class VillageDiffCallback : DiffUtil.ItemCallback<VillageAnalytics>() {
        override fun areItemsTheSame(oldItem: VillageAnalytics, newItem: VillageAnalytics): Boolean {
            return oldItem.villageId == newItem.villageId
        }

        override fun areContentsTheSame(oldItem: VillageAnalytics, newItem: VillageAnalytics): Boolean {
            return oldItem.healthScore == newItem.healthScore &&
                    oldItem.overallRisk == newItem.overallRisk &&
                    oldItem.activeAlerts == newItem.activeAlerts
        }
    }
}