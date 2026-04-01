package com.example.ecogeoguard.ui.government

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.RiskLevelType
import com.example.ecogeoguard.databinding.FragmentVillageAnalyticsBinding
import com.example.ecogeoguard.ui.government.adapter.VillageAnalyticsAdapter
import com.example.ecogeoguard.viewmodel.GovernmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class VillageAnalyticsFragment : Fragment() {

    private var _binding: FragmentVillageAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GovernmentViewModel by viewModels()
    private lateinit var villageAdapter: VillageAnalyticsAdapter

    private var currentSort = SortBy.NAME
    private var currentFilter = FilterBy.ALL

    private enum class SortBy { NAME, RISK, HEALTH, POPULATION }
    private enum class FilterBy { ALL, HIGH_RISK, LOW_HEALTH }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVillageAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        setupSortOptions()
        setupSearch()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_export -> {
                    exportVillageData()
                    true
                }
                R.id.action_compare -> {
                    showCompareDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        villageAdapter = VillageAnalyticsAdapter(
            onItemClick = { village ->
                showVillageDetails(village)
            },
            onCompareClick = { village ->
                addToComparison(village)
            }
        )

        binding.rvVillages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = villageAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> currentFilter = FilterBy.ALL
                    1 -> currentFilter = FilterBy.HIGH_RISK
                    2 -> currentFilter = FilterBy.LOW_HEALTH
                }
                applyFilters()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSortOptions() {
        binding.btnSortName.setOnClickListener {
            currentSort = SortBy.NAME
            updateSortButtons()
            applyFilters()
        }

        binding.btnSortRisk.setOnClickListener {
            currentSort = SortBy.RISK
            updateSortButtons()
            applyFilters()
        }

        binding.btnSortHealth.setOnClickListener {
            currentSort = SortBy.HEALTH
            updateSortButtons()
            applyFilters()
        }

        binding.btnSortPopulation.setOnClickListener {
            currentSort = SortBy.POPULATION
            updateSortButtons()
            applyFilters()
        }
    }

    private fun updateSortButtons() {
        val buttons = listOf(
            binding.btnSortName to SortBy.NAME,
            binding.btnSortRisk to SortBy.RISK,
            binding.btnSortHealth to SortBy.HEALTH,
            binding.btnSortPopulation to SortBy.POPULATION
        )

        buttons.forEach { (button, sortType) ->
            button.isEnabled = currentSort != sortType
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                applyFilters()
            }
        })
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.villageAnalytics.collect { villages ->
                applyFilters()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.isVisible = isLoading
            }
        }
    }

    private fun applyFilters() {
        var villages = viewModel.villageAnalytics.value ?: emptyList()

        // Apply filter
        villages = when (currentFilter) {
            FilterBy.ALL -> villages
            FilterBy.HIGH_RISK -> villages.filter {
                it.overallRisk == RiskLevelType.HIGH || it.overallRisk == RiskLevelType.CRITICAL
            }
            FilterBy.LOW_HEALTH -> villages.filter { it.healthScore < 50 }
        }

        // Apply search
        val searchQuery = binding.etSearch.text.toString().lowercase()
        if (searchQuery.isNotEmpty()) {
            villages = villages.filter {
                it.villageName.lowercase().contains(searchQuery)
            }
        }

        // Apply sort
        villages = when (currentSort) {
            SortBy.NAME -> villages.sortedBy { it.villageName }
            SortBy.RISK -> villages.sortedByDescending { it.overallRisk.ordinal }
            SortBy.HEALTH -> villages.sortedByDescending { it.healthScore }
            SortBy.POPULATION -> villages.sortedByDescending { it.population }
        }

        villageAdapter.submitList(villages)
        binding.tvVillageCount.text = "${villages.size} villages"
    }

    private fun showVillageDetails(village: com.example.ecogeoguard.data.model.VillageAnalytics) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val riskColor = when (village.overallRisk) {
            RiskLevelType.LOW -> R.color.success
            RiskLevelType.MODERATE -> R.color.warning
            RiskLevelType.HIGH -> R.color.warningOrange
            RiskLevelType.CRITICAL -> R.color.dangerRed
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(village.villageName)
            .setMessage("""
                📊 Village Analytics Report
                ========================
                
                📍 Population: ${village.population}
                🐄 Livestock: ${village.livestock}
                👨‍🌾 Farmers: ${village.farmers}
                📡 Sensors: ${village.sensors}
                🚨 Active Alerts: ${village.activeAlerts}
                
                📈 Performance Metrics:
                • Health Score: ${String.format("%.1f", village.healthScore)}%
                • Crop Yield: ${String.format("%.1f", village.cropYield)} t/ha
                • Irrigation: ${String.format("%.1f", village.irrigationCoverage)}%
                • Vaccination: ${String.format("%.1f", village.vaccinationCoverage)}%
                • Disaster Prep: ${String.format("%.1f", village.disasterPreparedness)}%
                
                💰 Budget:
                • Allocated: ₹${String.format("%.2f", village.budgetAllocated)} L
                • Utilized: ₹${String.format("%.2f", village.budgetUtilized)} L
                • Utilization: ${String.format("%.1f", (village.budgetUtilized / village.budgetAllocated * 100))}%
                
                📅 Last Assessment: ${dateFormat.format(Date(village.lastAssessment))}
            """.trimIndent())
            .setPositiveButton("View on Map") { _, _ ->
                Toast.makeText(requireContext(), "Opening map for ${village.villageName}", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Compare") { _, _ ->
                addToComparison(village)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private val comparisonList = mutableListOf<com.example.ecogeoguard.data.model.VillageAnalytics>()

    private fun addToComparison(village: com.example.ecogeoguard.data.model.VillageAnalytics) {
        if (comparisonList.contains(village)) {
            comparisonList.remove(village)
            Toast.makeText(requireContext(), "Removed from comparison", Toast.LENGTH_SHORT).show()
        } else {
            if (comparisonList.size >= 3) {
                Toast.makeText(requireContext(), "Maximum 3 villages can be compared", Toast.LENGTH_SHORT).show()
                return
            }
            comparisonList.add(village)
            Toast.makeText(requireContext(), "Added to comparison", Toast.LENGTH_SHORT).show()
        }

        if (comparisonList.isNotEmpty()) {
            binding.btnCompare.visibility = View.VISIBLE
            binding.btnCompare.text = "Compare (${comparisonList.size}/3)"
        } else {
            binding.btnCompare.visibility = View.GONE
        }
    }

    private fun showCompareDialog() {
        if (comparisonList.isEmpty()) {
            Toast.makeText(requireContext(), "No villages selected for comparison", Toast.LENGTH_SHORT).show()
            return
        }

        val comparisonText = buildString {
            append("🏘️ VILLAGE COMPARISON\n")
            append("===================\n\n")

            comparisonList.forEach { village ->
                append("📍 ${village.villageName}\n")
                append("   • Health: ${String.format("%.1f", village.healthScore)}%\n")
                append("   • Crop Yield: ${String.format("%.1f", village.cropYield)} t/ha\n")
                append("   • Irrigation: ${String.format("%.1f", village.irrigationCoverage)}%\n")
                append("   • Disaster Prep: ${String.format("%.1f", village.disasterPreparedness)}%\n")
                append("   • Risk: ${village.overallRisk.name}\n\n")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Village Comparison")
            .setMessage(comparisonText)
            .setPositiveButton("Export") { _, _ ->
                Toast.makeText(requireContext(), "Comparison exported", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Clear") { _, _ ->
                comparisonList.clear()
                binding.btnCompare.visibility = View.GONE
                Toast.makeText(requireContext(), "Comparison cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun exportVillageData() {
        val villages = viewModel.villageAnalytics.value ?: emptyList()

        val report = buildString {
            append("📊 VILLAGE ANALYTICS REPORT\n")
            append("==========================\n")
            append("Generated: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
            append("Total Villages: ${villages.size}\n\n")

            append("VILLAGE DETAILS:\n")
            villages.forEach { village ->
                append("\n• ${village.villageName}\n")
                append("  Population: ${village.population}\n")
                append("  Health Score: ${String.format("%.1f", village.healthScore)}%\n")
                append("  Crop Yield: ${String.format("%.1f", village.cropYield)} t/ha\n")
                append("  Risk Level: ${village.overallRisk.name}\n")
                append("  Budget Utilized: ${String.format("%.2f", village.budgetUtilized)} L\n")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export Data")
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
                Toast.makeText(requireContext(), "Report saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}