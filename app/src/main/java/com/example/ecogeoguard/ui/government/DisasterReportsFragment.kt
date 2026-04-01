package com.example.ecogeoguard.ui.government

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.DisasterImpactReport
import com.example.ecogeoguard.databinding.FragmentDisasterReportsBinding
import com.example.ecogeoguard.ui.government.adapter.DisasterReportAdapter
import com.example.ecogeoguard.viewmodel.GovernmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class DisasterReportsFragment : Fragment() {

    private var _binding: FragmentDisasterReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GovernmentViewModel by viewModels()
    private lateinit var reportAdapter: DisasterReportAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisasterReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_export -> {
                    exportReportsData()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        reportAdapter = DisasterReportAdapter { report ->
            showReportDetails(report)
        }

        binding.rvReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reportAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.disasterReports.collect { reports ->
                reportAdapter.submitList(reports)
                updateStats(reports)
            }
        }
    }

    private fun updateStats(reports: List<DisasterImpactReport>) {
        binding.tvTotalReports.text = reports.size.toString()

        val totalLoss = reports.sumOf { it.totalFinancialLoss }
        binding.tvTotalLoss.text = formatRupees(totalLoss)

        val totalAffected = reports.sumOf { it.totalAffectedPopulation }
        binding.tvTotalAffected.text = formatNumber(totalAffected)

        val avgRecovery = reports.map { it.recoveryProgress }.average().toFloat()
        binding.tvAvgRecovery.text = "${String.format("%.1f", avgRecovery)}%"
        binding.progressRecovery.progress = avgRecovery.toInt()
    }

    private fun showReportDetails(report: DisasterImpactReport) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${report.disasterType.name} Report")
            .setMessage("""
                📊 DISASTER IMPACT REPORT
                ========================
                
                📅 Date: ${dateFormat.format(Date(report.date))}
                ⚠️ Severity: ${report.severity}
                
                📍 Affected Areas:
                ${report.affectedVillages.joinToString("\n")}
                
                👥 Impact:
                • Population Affected: ${formatNumber(report.totalAffectedPopulation)}
                • Livestock Loss: ${formatNumber(report.totalLivestockLoss)}
                • Crop Loss: ${String.format("%.1f", report.totalCropLoss)} hectares
                • Infrastructure Damage: ${formatRupees(report.totalInfrastructureDamage)}
                
                💰 Financial:
                • Total Loss: ${formatRupees(report.totalFinancialLoss)}
                • Relief Released: ${formatRupees(report.reliefFundsReleased)}
                • Recovery Progress: ${String.format("%.1f", report.recoveryProgress)}%
                
                📋 Recommendations:
                ${report.recommendations.joinToString("\n")}
            """.trimIndent())
            .setPositiveButton("Generate Report") { _, _ ->
                generatePDFReport(report)
            }
            .setNeutralButton("Share") { _, _ ->
                shareReport(report)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun exportReportsData() {
        val reports = viewModel.disasterReports.value ?: emptyList()
        val report = buildString {
            append("📊 DISASTER REPORTS SUMMARY\n")
            append("===========================\n")
            append("Generated: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
            append("Total Reports: ${reports.size}\n\n")

            append("REPORT DETAILS:\n")
            reports.forEach { disaster ->
                append("\n• ${disaster.disasterType.name} - ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(disaster.date))}\n")
                append("  Affected: ${formatNumber(disaster.totalAffectedPopulation)} people\n")
                append("  Loss: ${formatRupees(disaster.totalFinancialLoss)}\n")
                append("  Recovery: ${String.format("%.1f", disaster.recoveryProgress)}%\n")
            }
        }

        Toast.makeText(requireContext(), "Report exported", Toast.LENGTH_SHORT).show()
    }

    private fun generatePDFReport(report: DisasterImpactReport) {
        Toast.makeText(requireContext(), "Generating PDF report...", Toast.LENGTH_SHORT).show()
    }

    private fun shareReport(report: DisasterImpactReport) {
        val shareText = """
            Disaster Report: ${report.disasterType.name}
            Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(report.date))}
            Affected: ${report.totalAffectedPopulation} people
            Loss: ${formatRupees(report.totalFinancialLoss)}
            Recovery: ${String.format("%.1f", report.recoveryProgress)}%
        """.trimIndent()

        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Report"))
    }

    private fun formatRupees(amount: Double): String {
        return when {
            amount >= 10000000 -> "₹${String.format("%.2f", amount / 10000000)} Cr"
            amount >= 100000 -> "₹${String.format("%.2f", amount / 100000)} L"
            else -> "₹${String.format("%.0f", amount)}"
        }
    }

    private fun formatNumber(number: Int): String {
        return when {
            number >= 1000000 -> "${number / 1000000}M"
            number >= 1000 -> "${number / 1000}K"
            else -> number.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}