package com.example.ecogeoguard.ui.government.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.DisasterImpactReport
import com.example.ecogeoguard.databinding.ItemDisasterReportBinding
import java.text.SimpleDateFormat
import java.util.*

class DisasterReportAdapter(
    private val onItemClick: (DisasterImpactReport) -> Unit
) : ListAdapter<DisasterImpactReport, DisasterReportAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemDisasterReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReportViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReportViewHolder(
        private val binding: ItemDisasterReportBinding,
        private val onItemClick: (DisasterImpactReport) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(report: DisasterImpactReport) {
            binding.apply {
                tvDisasterType.text = report.disasterType.name
                tvSeverity.text = report.severity.name

                val severityColor = when (report.severity) {
                    com.example.ecogeoguard.data.model.AlertSeverityLevel.CRITICAL -> R.color.dangerRed
                    com.example.ecogeoguard.data.model.AlertSeverityLevel.HIGH -> R.color.warningOrange
                    com.example.ecogeoguard.data.model.AlertSeverityLevel.MEDIUM -> R.color.warning
                    else -> R.color.info
                }
                tvSeverity.setTextColor(ContextCompat.getColor(root.context, severityColor))

                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                tvDate.text = dateFormat.format(Date(report.date))

                tvAffected.text = "${report.totalAffectedPopulation} people"
                tvLoss.text = formatRupees(report.totalFinancialLoss)

                tvRecovery.text = "${String.format("%.1f", report.recoveryProgress)}%"
                progressRecovery.progress = report.recoveryProgress.toInt()

                val recoveryColor = when {
                    report.recoveryProgress >= 70 -> R.color.success
                    report.recoveryProgress >= 40 -> R.color.warning
                    else -> R.color.dangerRed
                }
                progressRecovery.progressTintList =
                    ContextCompat.getColorStateList(root.context, recoveryColor)

                root.setOnClickListener { onItemClick(report) }
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

    class ReportDiffCallback : DiffUtil.ItemCallback<DisasterImpactReport>() {
        override fun areItemsTheSame(oldItem: DisasterImpactReport, newItem: DisasterImpactReport): Boolean {
            return oldItem.reportId == newItem.reportId
        }

        override fun areContentsTheSame(oldItem: DisasterImpactReport, newItem: DisasterImpactReport): Boolean {
            return oldItem.recoveryProgress == newItem.recoveryProgress
        }
    }
}