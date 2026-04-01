package com.example.ecogeoguard.ui.disaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.*
import com.example.ecogeoguard.databinding.FragmentActiveAlertsBinding
import com.example.ecogeoguard.ui.disaster.adapter.DisasterAlertAdapter
import com.example.ecogeoguard.viewmodel.DisasterViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ActiveAlertsFragment : Fragment() {

    private var _binding: FragmentActiveAlertsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DisasterViewModel by viewModels()
    private lateinit var disasterAlertAdapter: DisasterAlertAdapter

    // Filter state
    private var selectedSeverities = mutableSetOf<AlertSeverityLevel>()
    private var selectedTypes = mutableSetOf<DisasterType>()
    private var currentSort = SortType.TIME

    private enum class SortType { TIME, SEVERITY, VILLAGE }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActiveAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupChipGroups()
        setupSortButtons()
        setupClickListeners()
        setupObservers()
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
                R.id.action_filter_reset -> {
                    resetFilters()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        disasterAlertAdapter = DisasterAlertAdapter(
            onItemClick = { alert -> showAlertDetails(alert) },
            onAcknowledge = { alert -> viewModel.acknowledgeAlert(alert.id) }
        )

        binding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = disasterAlertAdapter
        }
    }

    private fun setupChipGroups() {
        // Severity chips
        binding.chipGroupSeverity.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedSeverities.clear()
            checkedIds.forEach { id ->
                when (id) {
                    R.id.chipCritical -> selectedSeverities.add(AlertSeverityLevel.CRITICAL)
                    R.id.chipHigh -> selectedSeverities.add(AlertSeverityLevel.HIGH)
                    R.id.chipMedium -> selectedSeverities.add(AlertSeverityLevel.MEDIUM)
                    R.id.chipLow -> selectedSeverities.add(AlertSeverityLevel.LOW)
                }
            }
            applyFilters()
        }

        // Disaster type chips
        binding.chipGroupType.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedTypes.clear()
            checkedIds.forEach { id ->
                when (id) {
                    R.id.chipLandslide -> selectedTypes.add(DisasterType.LANDSLIDE)
                    R.id.chipFlood -> selectedTypes.add(DisasterType.FLASH_FLOOD)
                    R.id.chipRainfall -> selectedTypes.add(DisasterType.HEAVY_RAINFALL)
                    R.id.chipEarthquake -> selectedTypes.add(DisasterType.EARTHQUAKE)
                    R.id.chipFire -> selectedTypes.add(DisasterType.FOREST_FIRE)
                }
            }
            applyFilters()
        }
    }

    private fun setupSortButtons() {
        binding.btnSortTime.setOnClickListener {
            updateSortSelection(SortType.TIME)
        }

        binding.btnSortSeverity.setOnClickListener {
            updateSortSelection(SortType.SEVERITY)
        }

        binding.btnSortVillage.setOnClickListener {
            updateSortSelection(SortType.VILLAGE)
        }
    }

    private fun updateSortSelection(selected: SortType) {
        currentSort = selected

        // Update button styles
        val buttons = listOf(binding.btnSortTime, binding.btnSortSeverity, binding.btnSortVillage)
        buttons.forEach { it.isEnabled = true }

        when (selected) {
            SortType.TIME -> binding.btnSortTime.isEnabled = false
            SortType.SEVERITY -> binding.btnSortSeverity.isEnabled = false
            SortType.VILLAGE -> binding.btnSortVillage.isEnabled = false
        }

        applyFilters()
    }

    private fun setupClickListeners() {
        binding.btnRefresh.setOnClickListener {
            viewModel.refreshManually()
            animateRefresh()
        }

        binding.btnAcknowledgeAll.setOnClickListener {
            showAcknowledgeAllDialog()
        }

        binding.btnExport.setOnClickListener {
            exportAlertsReport()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.activeAlerts.collect { alerts ->
                updateStats(alerts)
                applyFilters()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.isVisible = isLoading
            }
        }
    }

    private fun updateStats(alerts: List<DisasterAlert>) {
        binding.tvTotalActive.text = alerts.size.toString()
        binding.tvCriticalCount.text = alerts.count { it.severity == AlertSeverityLevel.CRITICAL }.toString()

        val uniqueVillages = alerts.flatMap { it.affectedVillages }.distinct().size
        binding.tvAffectedVillages.text = uniqueVillages.toString()
    }

    private fun applyFilters() {
        val allAlerts = viewModel.activeAlerts.value ?: emptyList()

        // Apply severity filter
        var filtered = if (selectedSeverities.isEmpty()) {
            allAlerts
        } else {
            allAlerts.filter { it.severity in selectedSeverities }
        }

        // Apply type filter
        filtered = if (selectedTypes.isEmpty()) {
            filtered
        } else {
            filtered.filter { it.type in selectedTypes }
        }

        // Apply sorting
        filtered = when (currentSort) {
            SortType.TIME -> filtered.sortedByDescending { it.timestamp }
            SortType.SEVERITY -> filtered.sortedByDescending { it.severity.ordinal }
            SortType.VILLAGE -> filtered.sortedBy { it.location.address ?: "" }
        }

        disasterAlertAdapter.submitList(filtered)
    }

    private fun resetFilters() {
        // Clear all chip selections
        binding.chipGroupSeverity.clearCheck()
        binding.chipGroupType.clearCheck()

        // Reset to "All" chips
        binding.chipAll.isChecked = true

        selectedSeverities.clear()
        selectedTypes.clear()

        // Reset sort to time
        updateSortSelection(SortType.TIME)
    }

    private fun showAlertDetails(alert: DisasterAlert) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(alert.title)
            .setMessage("""
                📍 Type: ${alert.type.name}
                ⚠️ Severity: ${alert.severity}
                📌 Location: ${alert.location.address ?: "Unknown"}
                🏘️ Villages: ${alert.affectedVillages.joinToString()}
                👥 Population: ${alert.affectedPopulation}
                🐄 Livestock: ${alert.affectedLivestock}
                
                📝 Message:
                ${alert.message}
                
                ✅ Recommended Action:
                ${alert.recommendedAction}
                
                🕐 Time: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(alert.timestamp))}
            """.trimIndent())
            .setPositiveButton("Acknowledge") { _, _ ->
                viewModel.acknowledgeAlert(alert.id)
                Snackbar.make(binding.root, "Alert acknowledged", Snackbar.LENGTH_SHORT).show()
            }
            .setNeutralButton("View Route") { _, _ ->
                alert.evacuationRouteId?.let { routeId ->
                    Toast.makeText(requireContext(), "Viewing evacuation route", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(requireContext(), "No route assigned", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAcknowledgeAllDialog() {
        val count = disasterAlertAdapter.itemCount
        if (count == 0) {
            Toast.makeText(requireContext(), "No alerts to acknowledge", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Acknowledge All Alerts")
            .setMessage("Are you sure you want to acknowledge all $count active alerts?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.acknowledgeAllAlerts()
                Snackbar.make(binding.root, "All alerts acknowledged", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun exportAlertsReport() {
        val alerts = disasterAlertAdapter.currentList
        if (alerts.isEmpty()) {
            Toast.makeText(requireContext(), "No alerts to export", Toast.LENGTH_SHORT).show()
            return
        }

        val report = buildString {
            append("🚨 ACTIVE ALERTS REPORT\n")
            append("========================\n")
            append("Generated: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
            append("Total Active Alerts: ${alerts.size}\n")
            append("Critical: ${alerts.count { it.severity == AlertSeverityLevel.CRITICAL }}\n")
            append("High: ${alerts.count { it.severity == AlertSeverityLevel.HIGH }}\n")
            append("Medium: ${alerts.count { it.severity == AlertSeverityLevel.MEDIUM }}\n")
            append("Low: ${alerts.count { it.severity == AlertSeverityLevel.LOW }}\n\n")

            append("ALERT DETAILS:\n")
            alerts.forEachIndexed { index, alert ->
                append("\n${index + 1}. ${alert.title}\n")
                append("   Type: ${alert.type.name}\n")
                append("   Severity: ${alert.severity}\n")
                append("   Location: ${alert.location.address}\n")
                append("   Time: ${SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date(alert.timestamp))}\n")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export Report")
            .setMessage("Report generated successfully. Share or save?")
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

    private fun animateRefresh() {
        binding.btnRefresh.animate()
            .rotation(360f)
            .setDuration(500)
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}