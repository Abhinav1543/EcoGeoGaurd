package com.example.ecogeoguard.ui.disaster

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.*
import com.example.ecogeoguard.databinding.FragmentDisasterDashboardBinding
import com.example.ecogeoguard.services.AlertNotificationService
import com.example.ecogeoguard.ui.common.DashboardBaseFragment
import com.example.ecogeoguard.ui.disaster.adapter.DisasterAlertAdapter
import com.example.ecogeoguard.utils.RoleManager
import com.example.ecogeoguard.viewmodel.DisasterViewModel
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
class DisasterDashboardFragment : DashboardBaseFragment() {

    override val role = UserRole.DISASTER_TEAM

    // Use different names to avoid hiding superclass properties
    private var _disasterBinding: FragmentDisasterDashboardBinding? = null
    private val disasterBinding
        get() = _disasterBinding ?: throw IllegalStateException("Binding is null")

    private val disasterViewModel: DisasterViewModel by viewModels()

    @Inject
    lateinit var roleManager: RoleManager

    private lateinit var disasterAlertAdapter: DisasterAlertAdapter

    private val refreshHandler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null

    private val emergencyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "EMERGENCY_ALERT_RECEIVED") {
                val title = intent.getStringExtra("title") ?: "Emergency Alert"
                val message = intent.getStringExtra("message") ?: ""
                showEmergencyOverlay(title, message)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // First call super to create base layout
        super.onCreateView(inflater, container, savedInstanceState)

        Log.d("DISASTER_DEBUG", "onCreateView: Starting")
        Log.d("DISASTER_DEBUG", "roleContentContainer exists: ${binding.roleContentContainer != null}")

        // Clear container first
        binding.roleContentContainer.removeAllViews()

        // Then inflate disaster content
        _disasterBinding = FragmentDisasterDashboardBinding.inflate(inflater, binding.roleContentContainer, true)

        Log.d("DISASTER_DEBUG", "disasterBinding created: ${_disasterBinding != null}")
        Log.d("DISASTER_DEBUG", "Container child count after inflate: ${binding.roleContentContainer.childCount}")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        setupAlertService()
        startAutoRefresh()

        view.postDelayed({
            fixContainerHeight()
        }, 500)

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(emergencyReceiver, android.content.IntentFilter("EMERGENCY_ALERT_RECEIVED"))
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
            disasterBinding.root.requestLayout()
        } catch (e: Exception) {
            Log.e("DISASTER_DEBUG", "Error fixing container: ${e.message}")
        }
    }

    private fun setupUI() {
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupSilenceButton()
        setupEmergencyAcknowledgeButton()
    }

    private fun setupToolbar() {
        // Use base class toolbar
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
                R.id.action_simulate_emergency -> {
                    testEmergencyAlert()
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

    private fun setupRecyclerView() {
        disasterAlertAdapter = DisasterAlertAdapter(
            onItemClick = { alert ->
                showAlertDetails(alert)
            },
            onAcknowledge = { alert ->
                disasterViewModel.acknowledgeAlert(alert.id)
            }
        )
        disasterBinding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = disasterAlertAdapter
        }
    }

    private fun setupClickListeners() {
        disasterBinding.btnRefresh.setOnClickListener {
            disasterViewModel.refreshManually()
            animateRefreshButton()
        }

        disasterBinding.btnViewAllAlerts.setOnClickListener {
            navigateToActiveAlerts()
        }

        disasterBinding.btnAcknowledgeAll.setOnClickListener {
            disasterViewModel.acknowledgeAllAlerts()
            Snackbar.make(disasterBinding.root, "All alerts acknowledged", Snackbar.LENGTH_SHORT).show()
        }

        disasterBinding.cardActiveAlerts.setOnClickListener {
            navigateToActiveAlerts()
        }

        disasterBinding.cardCriticalAlerts.setOnClickListener {
            showCriticalAlertsOnly()
        }

        disasterBinding.cardSensorsOnline.setOnClickListener {
            navigateToSensorNetwork()
        }

        disasterBinding.btnViewMap.setOnClickListener {
            navigateToEvacuationMap()
        }

        disasterBinding.btnSensorNetwork.setOnClickListener {
            navigateToSensorNetwork()
        }

        disasterBinding.btnRiskAssessment.setOnClickListener {
            navigateToRiskAssessment()
        }

        disasterBinding.btnResources.setOnClickListener {
            navigateToResourceManagement()
        }

        disasterBinding.btnEmergencyBroadcast.setOnClickListener {
            showEmergencyBroadcastDialog()
        }
    }

    private fun setupSilenceButton() {
        disasterBinding.btnSilenceAlarm.setOnClickListener {
            AlertNotificationService.muteAllBuzzers(requireContext())
            disasterBinding.cardEmergencyBanner.visibility = View.GONE
            disasterViewModel.dismissEmergency()
            Toast.makeText(requireContext(), "🔕 Alarm silenced", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupEmergencyAcknowledgeButton() {
        disasterBinding.btnEmergencyAcknowledge.setOnClickListener {
            disasterBinding.emergencyOverlay.visibility = View.GONE
            AlertNotificationService.muteAllBuzzers(requireContext())
            disasterViewModel.dismissEmergency()
            Snackbar.make(disasterBinding.root, "Emergency acknowledged", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(
                androidx.lifecycle.Lifecycle.State.STARTED
            ) {

                launch {
                    disasterViewModel.stats.collect { stats ->
                        stats?.let {
                            _disasterBinding?.let { binding ->
                                binding.tvActiveAlerts.text = it.activeAlerts.toString()
                                binding.tvCriticalAlerts.text = it.criticalAlerts.toString()
                                binding.tvAffectedVillages.text = it.affectedVillages.toString()
                                binding.tvSensorsOnline.text = "${it.sensorsOnline}/${it.totalSensors}"
                                binding.tvAvgResponse.text = "${it.avgResponseTime} min"
                                binding.tvEvents24h.text = it.last24hEvents.toString()
                            }
                        }
                    }
                }

                launch {
                    disasterViewModel.activeAlerts.collect { alerts ->
                        disasterAlertAdapter.submitList(alerts.take(5))
                    }
                }

                launch {
                    disasterViewModel.evacuationSummary.collect { summary ->
                        summary?.let {
                            _disasterBinding?.let { binding ->
                                binding.tvEvacuated.text = it.totalEvacuated.toString()
                                binding.tvSheltersOpen.text = it.sheltersOpen.toString()
                                binding.tvRoutesOpen.text = it.routesOpen.toString()
                            }
                        }
                    }
                }

                launch {
                    disasterViewModel.riskAssessments.collect { risks ->
                        updateRiskBars(risks.take(4))
                    }
                }

                launch {
                    disasterViewModel.showEmergencyBuzzer.collect { show ->
                        _disasterBinding?.let { binding ->
                            if (show) {
                                val alert = disasterViewModel.getEmergencyAlert()
                                alert?.let {
                                    showEmergencyOverlay(
                                        "🚨 CRITICAL: ${it.type.name}",
                                        it.message
                                    )
                                    triggerBuzzer()
                                }
                            } else {
                                binding.emergencyOverlay.visibility = View.GONE
                                binding.cardEmergencyBanner.visibility = View.GONE
                            }
                        }
                    }
                }

                launch {
                    disasterViewModel.lastUpdated.collect { timestamp ->
                        updateLastUpdatedTime(timestamp)
                    }
                }

                launch {
                    disasterViewModel.isLoading.collect { loading ->
                        _disasterBinding?.loadingOverlay?.isVisible = loading
                    }
                }
            }
        }
    }

    private fun updateRiskBars(risks: List<RiskAssessment>) {
        disasterBinding.layoutRiskBars.removeAllViews()

        risks.forEach { risk ->
            val barLayout = layoutInflater.inflate(R.layout.item_risk_bar, disasterBinding.layoutRiskBars, false) as LinearLayout

            val tvVillageName = barLayout.findViewById<TextView>(R.id.tvVillageName)
            val tvRiskLevel = barLayout.findViewById<TextView>(R.id.tvRiskLevel)
            val progressBar = barLayout.findViewById<View>(R.id.progressBar)

            tvVillageName.text = risk.villageId
            tvRiskLevel.text = risk.overallRisk.name

            val riskColor = when (risk.overallRisk) {
                RiskLevelType.LOW -> R.color.success
                RiskLevelType.MODERATE -> R.color.warning
                RiskLevelType.HIGH -> R.color.warningOrange
                RiskLevelType.CRITICAL -> R.color.dangerRed
            }

            tvRiskLevel.setTextColor(ContextCompat.getColor(requireContext(), riskColor))

            // Set progress bar width based on risk level
            val params = progressBar.layoutParams
            params.width = when (risk.overallRisk) {
                RiskLevelType.LOW -> 100
                RiskLevelType.MODERATE -> 200
                RiskLevelType.HIGH -> 300
                RiskLevelType.CRITICAL -> 400
            }
            progressBar.layoutParams = params
            progressBar.setBackgroundColor(ContextCompat.getColor(requireContext(), riskColor))

            disasterBinding.layoutRiskBars.addView(barLayout)
        }

        val highRiskCount = risks.count { it.overallRisk == RiskLevelType.HIGH || it.overallRisk == RiskLevelType.CRITICAL }
        disasterBinding.tvHighRiskCount.text = "$highRiskCount high risk"
    }

    private fun updateLastUpdatedTime(timestamp: Long) {
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        disasterBinding.tvLastUpdated.text = "Updated: ${format.format(Date(timestamp))}"
    }

    private fun showEmergencyOverlay(title: String, message: String) {
        disasterBinding.emergencyOverlay.visibility = View.VISIBLE
        disasterBinding.tvEmergencyOverlayTitle.text = title
        disasterBinding.tvEmergencyOverlayMessage.text = message

        val alert = disasterViewModel.getEmergencyAlert()
        disasterBinding.tvEmergencyOverlayAction.text = alert?.recommendedAction ?: "Take immediate action!"
    }

    private fun triggerBuzzer() {
        AlertNotificationService.testBuzzer(requireContext())
    }

    private fun showAlertDetails(alert: DisasterAlert) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(alert.title)
            .setMessage("""
                Type: ${alert.type.name}
                Severity: ${alert.severity}
                Location: ${alert.location.address ?: "Unknown"}
                Villages: ${alert.affectedVillages.joinToString()}
                Population at risk: ${alert.affectedPopulation}
                Livestock at risk: ${alert.affectedLivestock}
                
                ${alert.message}
                
                Recommended Action:
                ${alert.recommendedAction}
                
                Time: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(alert.timestamp))}
            """.trimIndent())
            .setPositiveButton("Acknowledge") { _, _ ->
                disasterViewModel.acknowledgeAlert(alert.id)
                Toast.makeText(requireContext(), "Alert acknowledged", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("View Route") { _, _ ->
                alert.evacuationRouteId?.let {
                    Toast.makeText(requireContext(), "Viewing evacuation route", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(requireContext(), "No route assigned", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showEmergencyBroadcastDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Emergency message..."
            setPadding(32, 32, 32, 32)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🚨 Send Emergency Broadcast")
            .setMessage("This will notify ALL users in affected areas")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val message = input.text.toString()
                if (message.isNotEmpty()) {
                    disasterViewModel.triggerEmergencyBroadcast(message, AlertSeverityLevel.CRITICAL)
                    Toast.makeText(requireContext(), "Emergency broadcast sent!", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCriticalAlertsOnly() {
        val criticalCount = disasterBinding.tvCriticalAlerts.text.toString().toInt()
        if (criticalCount > 0) {
            Toast.makeText(requireContext(), "Showing $criticalCount critical alerts", Toast.LENGTH_SHORT).show()
            navigateToActiveAlerts()
        } else {
            Toast.makeText(requireContext(), "No critical alerts", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToActiveAlerts() {
        try {
            findNavController().navigate(R.id.activeAlertsFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Active alerts coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToEvacuationMap() {
        try {
            findNavController().navigate(R.id.evacuationMapFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Evacuation map coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToSensorNetwork() {
        try {
            // Fix: Use the correct fragment ID
            findNavController().navigate(R.id.disasterSensorNetworkFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Sensor network coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToRiskAssessment() {
        try {
            findNavController().navigate(R.id.riskAssessmentFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Risk assessment coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToResourceManagement() {
        try {
            findNavController().navigate(R.id.resourceManagementFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Resource management coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testEmergencyAlert() {
        showEmergencyOverlay("🚨 TEST EMERGENCY", "This is a test of the emergency system")
        triggerBuzzer()
    }

    private fun setupAlertService() {
        AlertNotificationService.startService(requireContext())
    }

    private fun startAutoRefresh() {
        refreshRunnable = object : Runnable {
            override fun run() {
                if (isAdded) {
                    disasterViewModel.refreshManually()
                    refreshRunnable?.let {
                        refreshHandler.postDelayed(it, 30000)
                    }
                }
            }
        }
        refreshRunnable?.let {
            refreshHandler.postDelayed(it, 30000)
        }
    }

    private fun animateRefreshButton() {
        disasterBinding.btnRefresh.animate()
            .rotation(360f)
            .setDuration(500)
            .start()
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
        // Fix: Use correct action ID from navigation graph
        findNavController().navigate(R.id.action_disasterDashboardFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshRunnable?.let { refreshHandler.removeCallbacks(it) }
        refreshHandler.removeCallbacksAndMessages(null)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(emergencyReceiver)
        _disasterBinding = null
    }

    override fun onDashboardCreated() {
        // Disaster team specific initialization
    }
}