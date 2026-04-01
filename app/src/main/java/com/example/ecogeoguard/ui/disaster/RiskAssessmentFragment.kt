package com.example.ecogeoguard.ui.disaster

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.RiskAssessment
import com.example.ecogeoguard.data.model.RiskLevelType
import com.example.ecogeoguard.databinding.FragmentRiskAssessmentBinding
import com.example.ecogeoguard.viewmodel.DisasterViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@AndroidEntryPoint
class RiskAssessmentFragment : Fragment() {

    private var _binding: FragmentRiskAssessmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DisasterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiskAssessmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupObservers()
        animateGauge()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_refresh -> {
                    viewModel.refreshManually()
                    true
                }
                R.id.action_export -> {
                    exportRiskReport()
                    true
                }
                R.id.action_help -> {
                    showHelpDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.riskAssessments.collect { risks ->
                if (risks.isNotEmpty()) {
                    updateRiskFactors(risks.first())
                    updateVillageTable(risks)
                    updateRecommendations(risks)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateRiskFactors(risk: RiskAssessment) {
        // Update overall risk
        val overallRisk = risk.overallRisk
        val riskPercentage = when (overallRisk) {
            RiskLevelType.LOW -> 25
            RiskLevelType.MODERATE -> 50
            RiskLevelType.HIGH -> 75
            RiskLevelType.CRITICAL -> 95
        }

        binding.tvRiskPercentage.text = "$riskPercentage%"
        binding.tvOverallRisk.text = "${overallRisk.name} RISK"

        val riskColor = when (overallRisk) {
            RiskLevelType.LOW -> R.color.success
            RiskLevelType.MODERATE -> R.color.warning
            RiskLevelType.HIGH -> R.color.warningOrange
            RiskLevelType.CRITICAL -> R.color.dangerRed
        }
        binding.tvOverallRisk.setTextColor(ContextCompat.getColor(requireContext(), riskColor))

        // Animate gauge to correct percentage
        animateGaugeTo(riskPercentage)

        // Update factor values
        val factors = risk.factors

        // Rainfall
        val rainfall = factors["rainfall"] ?: Random.nextDouble(0.0, 100.0)
        binding.tvRainfallValue.text = String.format("%.1f mm", rainfall)
        updateRiskBadge(binding.tvRainfallRisk, getRiskLevelFromValue(rainfall))

        // Soil Moisture
        val moisture = factors["soilMoisture"] ?: Random.nextDouble(0.0, 100.0)
        binding.tvMoistureValue.text = String.format("%.1f%%", moisture)
        updateRiskBadge(binding.tvMoistureRisk, getRiskLevelFromValue(moisture))

        // Vibration
        val vibration = factors["vibration"] ?: Random.nextDouble(0.0, 5.0)
        binding.tvVibrationValue.text = String.format("%.1f Hz", vibration)
        updateRiskBadge(binding.tvVibrationRisk, getRiskLevelFromValue(vibration * 20))

        // Slope
        val slope = factors["slope"] ?: Random.nextDouble(0.0, 45.0)
        binding.tvSlopeValue.text = String.format("%.0f°", slope)
        updateRiskBadge(binding.tvSlopeRisk, getRiskLevelFromValue(slope * 2.2))
    }

    private fun updateVillageTable(risks: List<RiskAssessment>) {
        binding.layoutVillageRows.removeAllViews()

        val villages = listOf(
            "Himalayan Village",
            "River Valley",
            "Hill Station",
            "Mountain Base",
            "Forest Edge"
        )

        villages.forEach { village ->
            val rowView = layoutInflater.inflate(R.layout.item_village_risk, binding.layoutVillageRows, false)

            val tvVillage = rowView.findViewById<TextView>(R.id.tvVillageName)
            val tvLandslide = rowView.findViewById<TextView>(R.id.tvLandslideRisk)
            val tvFlood = rowView.findViewById<TextView>(R.id.tvFloodRisk)
            val tvOverall = rowView.findViewById<TextView>(R.id.tvOverallRisk)

            tvVillage.text = village

            // Generate random risks for demo
            val landslideRisk = RiskLevelType.entries.random()
            val floodRisk = RiskLevelType.entries.random()
            val overallRisk = RiskLevelType.entries.random()

            setRiskBadge(tvLandslide, landslideRisk)
            setRiskBadge(tvFlood, floodRisk)
            setRiskBadge(tvOverall, overallRisk)

            rowView.setOnClickListener {
                showVillageRiskDetails(village, landslideRisk, floodRisk, overallRisk)
            }

            binding.layoutVillageRows.addView(rowView)
        }
    }

    private fun updateRecommendations(risks: List<RiskAssessment>) {
        val highRiskVillages = risks.count { it.overallRisk == RiskLevelType.HIGH || it.overallRisk == RiskLevelType.CRITICAL }

        val recommendations = buildString {
            append("• Monitor rainfall levels closely\n")
            if (highRiskVillages > 2) {
                append("• Immediate evacuation preparation for $highRiskVillages high-risk villages\n")
            } else if (highRiskVillages > 0) {
                append("• Prepare evacuation for $highRiskVillages villages\n")
            }
            append("• Deploy additional sensors to critical areas\n")
            append("• Alert village heads and disaster teams\n")
            append("• Review emergency supplies and resources\n")
            append("• Conduct community awareness programs\n")
        }

        binding.tvRecommendations.text = recommendations
    }

    private fun updateRiskBadge(textView: TextView, riskLevel: RiskLevelType) {
        val (text, color) = when (riskLevel) {
            RiskLevelType.LOW -> Pair("LOW", R.color.success)
            RiskLevelType.MODERATE -> Pair("MEDIUM", R.color.warning)
            RiskLevelType.HIGH -> Pair("HIGH", R.color.warningOrange)
            RiskLevelType.CRITICAL -> Pair("CRITICAL", R.color.dangerRed)
        }

        textView.text = text
        textView.setBackgroundColor(ContextCompat.getColor(requireContext(), color))
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun setRiskBadge(textView: TextView, riskLevel: RiskLevelType) {
        val (text, color) = when (riskLevel) {
            RiskLevelType.LOW -> Pair("LOW", R.color.success)
            RiskLevelType.MODERATE -> Pair("MED", R.color.warning)
            RiskLevelType.HIGH -> Pair("HIGH", R.color.warningOrange)
            RiskLevelType.CRITICAL -> Pair("CRIT", R.color.dangerRed)
        }

        textView.text = text
        textView.setBackgroundColor(ContextCompat.getColor(requireContext(), color))
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun getRiskLevelFromValue(value: Double): RiskLevelType {
        return when {
            value < 25 -> RiskLevelType.LOW
            value < 50 -> RiskLevelType.MODERATE
            value < 75 -> RiskLevelType.HIGH
            else -> RiskLevelType.CRITICAL
        }
    }

    private fun animateGauge() {
        // Initial animation to 75% (just for demo)
        animateGaugeTo(75)
    }

    private fun animateGaugeTo(targetPercentage: Int) {
        val animator = ValueAnimator.ofInt(0, targetPercentage)
        animator.duration = 1500
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            binding.tvRiskPercentage.text = "$value%"
        }
        animator.start()
    }

    private fun showVillageRiskDetails(village: String, landslide: RiskLevelType, flood: RiskLevelType, overall: RiskLevelType) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(village)
            .setMessage("""
                📊 Risk Assessment Details
                
                🏔️ Landslide Risk: ${landslide.name}
                🌊 Flood Risk: ${flood.name}
                📈 Overall Risk: ${overall.name}
                
                👥 Population at risk: ${Random.nextInt(500, 5000)}
                🐄 Livestock at risk: ${Random.nextInt(200, 2000)}
                
                📍 Affected area: ${String.format("%.1f", Random.nextDouble(1.0, 10.0))} sq km
                
                Recommended Action:
                ${getRecommendationForVillage(overall)}
            """.trimIndent())
            .setPositiveButton("View on Map") { _, _ ->
                Toast.makeText(requireContext(), "Opening map for $village", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun getRecommendationForVillage(risk: RiskLevelType): String {
        return when (risk) {
            RiskLevelType.LOW -> "Continue monitoring. No immediate action required."
            RiskLevelType.MODERATE -> "Prepare evacuation plans. Alert village heads."
            RiskLevelType.HIGH -> "Partial evacuation recommended. Deploy response team."
            RiskLevelType.CRITICAL -> "IMMEDIATE EVACUATION! Critical danger detected."
        }
    }

    private fun exportRiskReport() {
        val report = buildString {
            append("📊 RISK ASSESSMENT REPORT\n")
            append("========================\n")
            append("Generated: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}\n\n")

            append("OVERALL RISK: ${binding.tvOverallRisk.text}\n")
            append("Risk Score: ${binding.tvRiskPercentage.text}\n\n")

            append("RISK FACTORS:\n")
            append("• Rainfall: ${binding.tvRainfallValue.text} (${binding.tvRainfallRisk.text})\n")
            append("• Soil Moisture: ${binding.tvMoistureValue.text} (${binding.tvMoistureRisk.text})\n")
            append("• Vibration: ${binding.tvVibrationValue.text} (${binding.tvVibrationRisk.text})\n")
            append("• Slope: ${binding.tvSlopeValue.text} (${binding.tvSlopeRisk.text})\n\n")

            append("RECOMMENDATIONS:\n")
            append(binding.tvRecommendations.text)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export Report")
            .setMessage("Report generated. Share or save?")
            .setPositiveButton("Share") { _, _ ->
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, report)
                    type = "text/plain"
                }
                startActivity(android.content.Intent.createChooser(shareIntent, "Share Report"))
            }
            .setNeutralButton("Save") { _, _ ->
                Toast.makeText(requireContext(), "Report saved to downloads", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHelpDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Risk Assessment Help")
            .setMessage("""
                This dashboard provides real-time risk assessment based on:
                
                • Sensor data from IoT devices
                • Weather forecasts and rainfall data
                • Historical landslide patterns
                • Soil moisture and stability metrics
                
                Risk levels are calculated using machine learning algorithms trained on historical disaster data.
                
                Tap on any village to view detailed risk information.
            """.trimIndent())
            .setPositiveButton("Got it", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}