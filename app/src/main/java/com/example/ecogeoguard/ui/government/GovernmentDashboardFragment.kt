package com.example.ecogeoguard.ui.government

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.UserRole
import com.example.ecogeoguard.databinding.DialogGenerateReportBinding
import com.example.ecogeoguard.databinding.FragmentGovernmentDashboardBinding
import com.example.ecogeoguard.services.AlertNotificationService
import com.example.ecogeoguard.ui.common.DashboardBaseFragment
import com.example.ecogeoguard.ui.government.charts.DistrictOverviewChart
import com.example.ecogeoguard.ui.government.charts.TrendChart
import com.example.ecogeoguard.utils.RoleManager
import com.example.ecogeoguard.viewmodel.GovernmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.util.Log
import androidx.lifecycle.repeatOnLifecycle

@AndroidEntryPoint
class GovernmentDashboardFragment : DashboardBaseFragment() {

    override val role = UserRole.GOVERNMENT

    private var _govBinding: FragmentGovernmentDashboardBinding? = null
    private val govBinding get() = _govBinding!!

    private val governmentViewModel: GovernmentViewModel by viewModels()

    @Inject
    lateinit var roleManager: RoleManager

    private val refreshHandler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        Log.d("GOV_DEBUG", "onCreateView: Starting")
        Log.d("GOV_DEBUG", "roleContentContainer exists: ${binding.roleContentContainer != null}")

        binding.roleContentContainer.removeAllViews()
        _govBinding = FragmentGovernmentDashboardBinding.inflate(inflater, binding.roleContentContainer, true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupCharts()
        setupObservers()
        startAutoRefresh()

        view.postDelayed({
            fixContainerHeight()
        }, 500)
    }

    private fun fixContainerHeight() {
        try {
            val parent = binding.roleContentContainer.parent as? android.widget.LinearLayout
            if (parent != null) {
                val params = binding.roleContentContainer.layoutParams as? android.widget.LinearLayout.LayoutParams
                params?.weight = 1f
                params?.height = 0
                binding.roleContentContainer.layoutParams = params
            }
            binding.roleContentContainer.requestLayout()
            govBinding.root.requestLayout()
        } catch (e: Exception) {
            Log.e("GOV_DEBUG", "Error fixing container: ${e.message}")
        }
    }

    private fun setupUI() {
        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_notifications -> {
                    try {
                        findNavController().navigate(R.id.notificationsFragment)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Notifications coming soon", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.action_export -> {
                    showGenerateReportDialog()
                    true
                }
                R.id.action_logout -> {
                    showLogoutDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupCharts() {
        // Add charts to layout programmatically (or you can add them in XML)
        addDistrictChart()
        addTrendChart()
    }

    private fun addDistrictChart() {

        val districtChart = DistrictOverviewChart(requireContext())

        val height = (250 * resources.displayMetrics.density).toInt()

        districtChart.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height
        )

        districtChart.setPadding(0, 16, 0, 16)

        // ✅ DIRECTLY USE CONTAINER (NO findViewById)
        govBinding.chartDistrictContainer.addView(districtChart)

        // Save ID for next chart
        districtChartId = districtChart.id

        // Data
        districtChart.setData(
            listOf(
                Triple("Agriculture", 65f, Color.parseColor("#4CAF50")),
                Triple("Livestock", 72f, Color.parseColor("#2196F3")),
                Triple("Disaster", 58f, Color.parseColor("#FF9800")),
                Triple("Infrastructure", 45f, Color.parseColor("#9C27B0")),
                Triple("Healthcare", 82f, Color.parseColor("#00BCD4"))
            )
        )

        districtChart.postDelayed({
            districtChart.animateTo(
                listOf(
                    Triple("Agriculture", 68f, Color.parseColor("#4CAF50")),
                    Triple("Livestock", 75f, Color.parseColor("#2196F3")),
                    Triple("Disaster", 62f, Color.parseColor("#FF9800")),
                    Triple("Infrastructure", 48f, Color.parseColor("#9C27B0")),
                    Triple("Healthcare", 85f, Color.parseColor("#00BCD4"))
                ),
                1000
            )
        }, 500)
    }

    private fun addTrendChart() {

        val trendChart = TrendChart(requireContext())

        val height = (300 * resources.displayMetrics.density).toInt()

        trendChart.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height
        )

        trendChart.setPadding(0, 16, 0, 16)

        // ✅ USE SECOND CONTAINER
        govBinding.chartTrendContainer.addView(trendChart)

        trendChart.setData(
            listOf(65f, 68f, 72f, 70f, 75f, 78f, 82f),
            listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul")
        )

        trendChart.postDelayed({
            trendChart.animateTo(
                listOf(68f, 71f, 75f, 73f, 78f, 81f, 85f),
                1000
            )
        }, 500)
    }

    private fun setupClickListeners() {
        govBinding.btnRefresh.setOnClickListener {
            governmentViewModel.refreshManually()
            animateRefreshButton()
        }

        govBinding.btnVillageAnalytics.setOnClickListener {
            navigateToVillageAnalytics()
        }

        govBinding.btnBudgetTracking.setOnClickListener {
            navigateToBudgetTracking()
        }

        govBinding.btnDisasterReports.setOnClickListener {
            navigateToDisasterReports()
        }

        govBinding.btnPolicyRecommendations.setOnClickListener {
            navigateToPolicyRecommendations()
        }

        govBinding.btnStakeholderManagement.setOnClickListener {
            navigateToStakeholderManagement()
        }

        govBinding.btnExportReport.setOnClickListener {
            showGenerateReportDialog()
        }
    }

    private fun setupObservers() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {

                // ✅ DISTRICT STATS
                launch {
                    governmentViewModel.districtStats.collect { stats ->
                        _govBinding?.let { binding ->
                            stats?.let {
                                binding.tvTotalVillages.text = it.totalVillages.toString()
                                binding.tvTotalPopulation.text = formatNumber(it.totalPopulation)
                                binding.tvTotalFarmers.text = formatNumber(it.totalFarmers)
                                binding.tvTotalLivestock.text = formatNumber(it.totalLivestock)
                                binding.tvTotalAlerts.text = it.totalAlerts.toString()
                                binding.tvHealthScore.text = "${String.format("%.1f", it.avgHealthScore)}%"

                                binding.tvCropYield.text = "${String.format("%.1f", it.avgCropYield)} t/ha"
                                binding.progressCropYield.progress = (it.avgCropYield / 5 * 100).toInt()

                                val totalBudgetInCr = it.totalBudget / 10000000
                                val utilizedBudgetInCr = it.utilizedBudget / 10000000

                                binding.tvTotalBudget.text = "₹${String.format("%.1f", totalBudgetInCr)} Cr"
                                binding.tvUtilizedBudget.text = "₹${String.format("%.1f", utilizedBudgetInCr)} Cr"

                                val utilizationRate =
                                    if (it.totalBudget > 0)
                                        (it.utilizedBudget.toFloat() / it.totalBudget * 100)
                                    else 0f

                                binding.tvUtilizationRate.text =
                                    "${String.format("%.1f", utilizationRate)}%"
                                binding.progressBudget.progress = utilizationRate.toInt()
                            }
                        }
                    }
                }

                // ✅ VILLAGE ANALYTICS
                launch {
                    governmentViewModel.villageAnalytics.collect { villages ->
                        _govBinding?.let { binding ->
                            if (villages.isNotEmpty()) {

                                val avgCropYield =
                                    villages.map { it.cropYield }.average().toFloat()

                                val avgIrrigation =
                                    villages.map { it.irrigationCoverage }.average().toFloat()

                                val avgVaccination =
                                    villages.map { it.vaccinationCoverage }.average().toFloat()

                                val avgDisasterPrep =
                                    villages.map { it.disasterPreparedness }.average().toFloat()

                                binding.tvCropYield.text =
                                    "${String.format("%.1f", avgCropYield)} t/ha"
                                binding.progressCropYield.progress =
                                    (avgCropYield / 5 * 100).toInt()

                                binding.tvIrrigationCoverage.text =
                                    "${String.format("%.1f", avgIrrigation)}%"
                                binding.progressIrrigation.progress = avgIrrigation.toInt()

                                binding.tvVaccinationCoverage.text =
                                    "${String.format("%.1f", avgVaccination)}%"
                                binding.progressVaccination.progress =
                                    avgVaccination.toInt()

                                binding.tvDisasterReadiness.text =
                                    "${String.format("%.1f", avgDisasterPrep)}%"
                                binding.progressDisaster.progress =
                                    avgDisasterPrep.toInt()
                            }
                        }
                    }
                }

                // ✅ LAST UPDATED
                launch {
                    governmentViewModel.lastUpdated.collect { timestamp ->
                        _govBinding?.let { binding ->
                            val format =
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            binding.tvLastUpdated.text =
                                "Updated: ${format.format(Date(timestamp))}"
                        }
                    }
                }

                // ✅ LOADING
                launch {
                    governmentViewModel.isLoading.collect { isLoading ->
                        _govBinding?.loadingOverlay?.isVisible = isLoading
                    }
                }
            }
        }
    }

    private fun updateLastUpdatedTime(timestamp: Long) {
        _govBinding?.let { binding ->
            val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            binding.tvLastUpdated.text = "Updated: ${format.format(Date(timestamp))}"
        }
    }

    private fun formatNumber(number: Int): String {
        return when {
            number >= 10000000 -> "${number / 10000000} Cr"
            number >= 100000 -> "${number / 1000}K"
            else -> number.toString()
        }
    }

    private fun animateRefreshButton() {
        govBinding.btnRefresh.animate()
            .rotation(360f)
            .setDuration(500)
            .start()
    }

    private fun showGenerateReportDialog() {
        val dialogBinding = DialogGenerateReportBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Custom range toggle
        dialogBinding.btnCustomRange.setOnClickListener {
            val isVisible = dialogBinding.layoutCustomDate.visibility == View.VISIBLE
            dialogBinding.layoutCustomDate.visibility = if (isVisible) View.GONE else View.VISIBLE
            dialogBinding.btnCustomRange.text = if (isVisible) "Custom Range" else "Hide Custom Range"
        }

        // Date pickers
        dialogBinding.btnStartDate.setOnClickListener {
            showDatePicker { date ->
                dialogBinding.btnStartDate.text = date
            }
        }

        dialogBinding.btnEndDate.setOnClickListener {
            showDatePicker { date ->
                dialogBinding.btnEndDate.text = date
            }
        }

        // Last Month button
        dialogBinding.btnLastMonth.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -1)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            dialogBinding.btnStartDate.text = dateFormat.format(calendar.time)
            dialogBinding.btnEndDate.text = dateFormat.format(Date())
            dialogBinding.layoutCustomDate.visibility = View.GONE
            dialogBinding.btnCustomRange.text = "Custom Range"
        }

        // Last Quarter button
        dialogBinding.btnLastQuarter.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -3)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            dialogBinding.btnStartDate.text = dateFormat.format(calendar.time)
            dialogBinding.btnEndDate.text = dateFormat.format(Date())
            dialogBinding.layoutCustomDate.visibility = View.GONE
            dialogBinding.btnCustomRange.text = "Custom Range"
        }

        // Last Year button
        dialogBinding.btnLastYear.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, -1)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            dialogBinding.btnStartDate.text = dateFormat.format(calendar.time)
            dialogBinding.btnEndDate.text = dateFormat.format(Date())
            dialogBinding.layoutCustomDate.visibility = View.GONE
            dialogBinding.btnCustomRange.text = "Custom Range"
        }

        // Generate button
        dialogBinding.btnGenerate.setOnClickListener {
            val reportType = when (dialogBinding.chipGroupReportType.checkedChipId) {
                R.id.chipDistrict -> "District Overview"
                R.id.chipVillage -> "Village Analytics"
                R.id.chipBudget -> "Budget Tracking"
                R.id.chipDisaster -> "Disaster Reports"
                else -> "Performance Report"
            }

            val format = when (dialogBinding.chipGroupFormat.checkedChipId) {
                R.id.chipPDF -> "PDF"
                R.id.chipExcel -> "Excel"
                R.id.chipPowerPoint -> "PowerPoint"
                else -> "CSV"
            }

            val includeCharts = dialogBinding.cbCharts.isChecked
            val includeTables = dialogBinding.cbTables.isChecked
            val includeSummary = dialogBinding.cbSummary.isChecked
            val includeRecommendations = dialogBinding.cbRecommendations.isChecked

            val startDate = dialogBinding.btnStartDate.text.toString()
            val endDate = dialogBinding.btnEndDate.text.toString()

            Toast.makeText(
                requireContext(),
                "Generating $reportType report in $format format\nPeriod: $startDate - $endDate",
                Toast.LENGTH_LONG
            ).show()

            dialog.dismiss()
        }

        // Cancel button
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(Date(selection))
            onDateSelected(date)
        }

        datePicker.show(parentFragmentManager, "date_picker")
    }

    private fun navigateToVillageAnalytics() {
        try {
            findNavController().navigate(R.id.villageAnalyticsFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Village analytics coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToBudgetTracking() {
        try {
            findNavController().navigate(R.id.budgetTrackingFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Budget tracking coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToDisasterReports() {
        try {
            findNavController().navigate(R.id.disasterReportsFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Disaster reports coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToPolicyRecommendations() {
        try {
            findNavController().navigate(R.id.policyRecommendationsFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Policy insights coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToStakeholderManagement() {
        try {
            findNavController().navigate(R.id.stakeholderManagementFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Stakeholder management coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAutoRefresh() {
        refreshRunnable = object : Runnable {
            override fun run() {
                if (isAdded) {
                    governmentViewModel.refreshManually()
                    refreshRunnable?.let {
                        refreshHandler.postDelayed(it, 60000) // Refresh every minute
                    }
                }
            }
        }
        refreshRunnable?.let {
            refreshHandler.postDelayed(it, 60000)
        }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        roleManager.logoutUser()
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        AlertNotificationService.stopService(requireContext())
        findNavController().navigate(R.id.action_governmentDashboardFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshRunnable?.let { refreshHandler.removeCallbacks(it) }
        refreshHandler.removeCallbacksAndMessages(null)
        _govBinding = null
    }

    override fun onDashboardCreated() {
        // Government-specific initialization
    }

    companion object {
        var districtChartId = 0
    }
}