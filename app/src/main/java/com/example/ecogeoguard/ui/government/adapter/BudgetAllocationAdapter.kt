package com.example.ecogeoguard.ui.government.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.BudgetAllocation
import com.example.ecogeoguard.databinding.ItemBudgetAllocationBinding
import java.text.SimpleDateFormat
import java.util.*
import java.text.NumberFormat

class BudgetAllocationAdapter(
    private val onItemClick: (BudgetAllocation) -> Unit
) : ListAdapter<BudgetAllocation, BudgetAllocationAdapter.BudgetViewHolder>(BudgetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetAllocationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BudgetViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BudgetViewHolder(
        private val binding: ItemBudgetAllocationBinding,
        private val onItemClick: (BudgetAllocation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(budget: BudgetAllocation) {
            binding.apply {
                tvSchemeName.text = budget.schemeName
                tvSchemeCode.text = budget.schemeCode

                val formatter = NumberFormat.getCurrencyInstance(Locale.US)
                tvAllocated.text = formatRupees(budget.allocatedAmount)
                tvUtilized.text = formatRupees(budget.utilizedAmount)

                val utilization = (budget.utilizedAmount / budget.allocatedAmount * 100).toInt()
                tvUtilizationRate.text = "${utilization}%"
                progressUtilization.progress = utilization

                val progressColor = when {
                    utilization < 50 -> R.color.success
                    utilization < 80 -> R.color.warning
                    else -> R.color.dangerRed
                }
                progressUtilization.progressTintList = ContextCompat.getColorStateList(root.context, progressColor)

                tvBeneficiaries.text = "${budget.beneficiaries} beneficiaries"
                tvVillagesCovered.text = "${budget.villagesCovered.size} villages"

                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                tvDateRange.text = "${dateFormat.format(Date(budget.startDate))} - ${dateFormat.format(Date(budget.endDate))}"

                root.setOnClickListener { onItemClick(budget) }
            }
        }

        private fun formatRupees(amount: Double): String {
            return when {
                amount >= 10000000 -> "₹${String.format("%.2f", amount / 10000000)} Cr"
                amount >= 100000 -> "₹${String.format("%.2f", amount / 100000)} L"
                else -> "₹${String.format("%.0f", amount)}"
            }
        }
    }

    class BudgetDiffCallback : DiffUtil.ItemCallback<BudgetAllocation>() {
        override fun areItemsTheSame(oldItem: BudgetAllocation, newItem: BudgetAllocation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BudgetAllocation, newItem: BudgetAllocation): Boolean {
            return oldItem.utilizedAmount == newItem.utilizedAmount &&
                    oldItem.performanceScore == newItem.performanceScore
        }
    }
}