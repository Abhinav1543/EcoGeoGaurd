package com.example.ecogeoguard.ui.admin

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.Alert
import com.example.ecogeoguard.data.model.UserRole
import com.example.ecogeoguard.databinding.FragmentAdminDashboardBinding
import com.example.ecogeoguard.services.AlertNotificationService
import com.example.ecogeoguard.ui.admin.adapter.AlertAdapter
import com.example.ecogeoguard.ui.admin.adapter.VillageAdapter
import com.example.ecogeoguard.ui.common.DashboardBaseFragment
import com.example.ecogeoguard.viewmodel.AdminViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AdminDashboardFragment : DashboardBaseFragment() {

    override val role = UserRole.ADMIN

    private var _adminBinding: FragmentAdminDashboardBinding? = null
    private val adminBinding get() = _adminBinding!!

    private val adminViewModel: AdminViewModel by viewModels()

    @Inject
    lateinit var roleManager: com.example.ecogeoguard.utils.RoleManager

    private lateinit var alertAdapter: AlertAdapter
    private lateinit var villageAdapter: VillageAdapter

    private val syncHandler = Handler(Looper.getMainLooper())
    private var syncRunnable: Runnable? = null
    private var alertSimulationHandler: Handler? = null
    private var alertSimulationRunnable: Runnable? = null

    // Broadcast receiver for emergency alerts
    private val emergencyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "EMERGENCY_ALERT_RECEIVED") {
                val alertId = intent.getStringExtra("alert_id") ?: ""
                val title = intent.getStringExtra("title") ?: ""
                val message = intent.getStringExtra("message") ?: ""
                val type = intent.getStringExtra("type") ?: ""
                val severity = intent.getStringExtra("severity") ?: ""
                val location = intent.getStringExtra("location") ?: ""

                // Show emergency UI
                showEmergencyAlertDialog(alertId, title, message, type, severity, location)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // First create the base layout
        super.onCreateView(inflater, container, savedInstanceState)

        // Then inflate admin content into the roleContentContainer
        _adminBinding = FragmentAdminDashboardBinding.inflate(inflater, binding.roleContentContainer, true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAdminUI()
        setupObservers()
        startDataRefresh()

        // Register broadcast receiver for emergency alerts
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(emergencyReceiver, android.content.IntentFilter("EMERGENCY_ALERT_RECEIVED"))
    }

    override fun onDashboardCreated() {
        // Admin-specific initialization after base is created
        // You can set custom toolbar title or other admin-specific setup here
    }

    private fun initAdminUI() {
        setupSystemHealthCards()
        setupAlertFeed()
        setupVillageGrid()
        setupCharts()
        setupQuickActions()
        setupToolbarMenu()
        updateLastSyncTime()

        // START NOTIFICATION SERVICE
        AlertNotificationService.startService(requireContext())
        Toast.makeText(requireContext(), "Alert monitoring started", Toast.LENGTH_SHORT).show()

        // Setup TEST BUTTON for immediate buzzer testing
        setupTestButton()

        // Setup random alerts for demo
        startAlertSimulation()
    }

    private fun setupTestButton() {
        // Add a temporary test button to test buzzers immediately
        adminBinding.cardManageUsers.setOnLongClickListener {
            // Test buzzer on long press of any card
            AlertNotificationService.testBuzzer(requireContext())
            Toast.makeText(requireContext(), "🚨 Testing Emergency Buzzer!", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun startAlertSimulation() {
        alertSimulationHandler = Handler(Looper.getMainLooper())

        alertSimulationRunnable = object : Runnable {
            override fun run() {
                if (isAdded && isVisible) {
                    simulateRandomAlert()

                    // Schedule next alert (between 60-120 seconds for realistic simulation)
                    val nextDelay = (60 + Random().nextInt(60)) * 1000L
                    alertSimulationHandler?.postDelayed(this, nextDelay)
                }
            }
        }

        // Start first alert after 15 seconds
        alertSimulationHandler?.postDelayed(alertSimulationRunnable!!, 15000)
    }

    private fun simulateRandomAlert() {
        val alertTypes = listOf("landslide", "heavy_rainfall", "livestock", "irrigation", "theft")
        val severities = listOf("low", "medium", "high", "critical")
        val locations = listOf("Village A", "Village B", "Village C", "Village D")

        // Increase probability for landslide/rainfall alerts (40% chance)
        val adjustedType = if (Random().nextInt(100) < 40) {
            if (Random().nextBoolean()) "landslide" else "heavy_rainfall"
        } else {
            alertTypes.random()
        }

        val severity = severities.random()
        val location = locations.random()

        // 🚨 BUZZER CRITERIA: landslide/heavy_rainfall + high/critical
        val isHighRisk = (adjustedType == "landslide" || adjustedType == "heavy_rainfall") &&
                (severity == "high" || severity == "critical")

        if (isHighRisk) {
            // Show emergency UI and trigger service
            showEmergencyAlertUI(adjustedType, severity, location)
        } else {
            // Show regular alert
            showRegularAlertUI(adjustedType, severity, location)
        }
    }

    private fun showEmergencyAlertUI(type: String, severity: String, location: String) {
        val title = when (type) {
            "landslide" -> "🚨 LANDSLIDE DETECTED"
            "heavy_rainfall" -> "🌧️ HEAVY RAINFALL WARNING"
            else -> "⚠️ EMERGENCY ALERT"
        }

        val message = when (type) {
            "landslide" -> "Immediate danger! Landslide detected at $location. Evacuate immediately!"
            "heavy_rainfall" -> "Warning! Heavy rainfall at $location. Risk of flooding!"
            else -> "Emergency at $location - ${severity.uppercase()} severity"
        }

        // Show full-screen emergency dialog
        showEmergencyAlertDialog(
            alertId = "sim_${System.currentTimeMillis()}",
            title = title,
            message = message,
            type = type,
            severity = severity,
            location = location
        )

        // Add to alert feed
        addAlertToFeed(type, severity, location, isEmergency = true)

        // Show persistent snackbar
        Snackbar.make(adminBinding.root, title, Snackbar.LENGTH_INDEFINITE)
            .setAction("SILENCE ALARM") {
                AlertNotificationService.muteAllBuzzers(requireContext())
                Toast.makeText(requireContext(), "Alarm silenced", Toast.LENGTH_SHORT).show()
            }
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.dangerRed))
            .setTextColor(Color.WHITE)
            .setActionTextColor(Color.YELLOW)
            .show()
    }

    private fun showRegularAlertUI(type: String, severity: String, location: String) {
        val title = when (type) {
            "landslide" -> "Landslide Alert"
            "heavy_rainfall" -> "Rainfall Alert"
            "livestock" -> "Livestock Movement"
            "irrigation" -> "Irrigation Needed"
            "theft" -> "Security Alert"
            else -> "System Alert"
        }

        val message = "Alert at $location - ${severity.uppercase()} severity"

        // Show snackbar
        Snackbar.make(adminBinding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(
                when (severity) {
                    "high" -> ContextCompat.getColor(requireContext(), R.color.warningOrange)
                    "critical" -> ContextCompat.getColor(requireContext(), R.color.dangerRed)
                    else -> ContextCompat.getColor(requireContext(), R.color.cautionYellow)
                }
            )
            .show()

        // Add to alert feed
        addAlertToFeed(type, severity, location, isEmergency = false)
    }

    private fun showEmergencyAlertDialog(
        alertId: String,
        title: String,
        message: String,
        type: String,
        severity: String,
        location: String
    ) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage("$message\n\n📍 Location: $location\n⚡ Severity: ${severity.uppercase()}\n\nThis alarm will continue until you silence it!")
            .setPositiveButton("SILENCE ALARM") { _, _ ->
                AlertNotificationService.muteAllBuzzers(requireContext())
                Toast.makeText(requireContext(), "Alarm silenced", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("VIEW DETAILS") { _, _ ->
                // Navigate to alert details (you can implement this later)
                Toast.makeText(requireContext(), "Viewing alert details...", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false) // Cannot dismiss without taking action
            .show()
    }

    private fun addAlertToFeed(type: String, severity: String, location: String, isEmergency: Boolean) {
        lifecycleScope.launch {
            val currentAlerts = alertAdapter.currentList.toMutableList()
            if (currentAlerts.size >= 5) {
                currentAlerts.removeAt(0)
            }

            // Add new alert
            val newAlert = Alert(
                id = System.currentTimeMillis().toString(),
                title = when (type) {
                    "landslide" -> if (isEmergency) "🚨 Landslide" else "Landslide Alert"
                    "heavy_rainfall" -> if (isEmergency) "🌧️ Heavy Rainfall" else "Rainfall Alert"
                    "livestock" -> "Livestock Movement"
                    "irrigation" -> "Irrigation Needed"
                    "theft" -> "Security Alert"
                    else -> "System Alert"
                },
                message = when (type) {
                    "landslide" -> if (isEmergency) "EMERGENCY: Landslide detected at $location" else "Landslide risk at $location"
                    "heavy_rainfall" -> if (isEmergency) "EMERGENCY: Heavy rainfall at $location" else "Rainfall at $location"
                    else -> "Alert at $location"
                },
                type = when (type) {
                    "landslide" -> Alert.AlertType.LANDSLIDE
                    "heavy_rainfall" -> Alert.AlertType.RAINFALL
                    "livestock" -> Alert.AlertType.LIVESTOCK
                    "irrigation" -> Alert.AlertType.IRRIGATION
                    "theft" -> Alert.AlertType.THEFT
                    else -> Alert.AlertType.SYSTEM
                },
                severity = when (severity) {
                    "low" -> Alert.Severity.LOW
                    "medium" -> Alert.Severity.MEDIUM
                    "high" -> Alert.Severity.HIGH
                    else -> Alert.Severity.CRITICAL
                },
                timestamp = System.currentTimeMillis(),
                villageId = location,
                isRead = false
            )

            currentAlerts.add(0, newAlert) // Add at beginning
            alertAdapter.submitList(currentAlerts)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clean up handlers
        syncRunnable?.let { syncHandler.removeCallbacks(it) }
        syncHandler.removeCallbacksAndMessages(null)

        alertSimulationRunnable?.let { alertSimulationHandler?.removeCallbacks(it) }
        alertSimulationHandler?.removeCallbacksAndMessages(null)

        // Unregister receiver
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(emergencyReceiver)

        // Stop notification service if leaving admin dashboard
        if (findNavController().currentDestination?.id != R.id.adminDashboardFragment) {
            AlertNotificationService.stopService(requireContext())
        }

        _adminBinding = null
    }

    // Add this method in AdminDashboardFragment class
    private fun setupToolbarMenu() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_notifications -> {
                    Toast.makeText(requireContext(),
                        "👤 Notification feature coming soon (Future Update)",
                        Toast.LENGTH_SHORT).show()

                    findNavController().navigate(R.id.notificationsFragment)
                    true
                }
                R.id.action_search -> {
                    showToast("Search (Coming Soon)")
                    true
                }
                R.id.action_emergency -> {
                    // Trigger test emergency alert
                    AlertNotificationService.testBuzzer(requireContext())
                    showToast("Test emergency alert triggered!")
                    true
                }
                R.id.action_mute -> {
                    AlertNotificationService.muteAllBuzzers(requireContext())
                    showToast("All buzzers muted")
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

    private fun showLogoutDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        // Clear user session
        roleManager.logoutUser()

        // Clear Firebase authentication
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

        // Stop alert service
        AlertNotificationService.stopService(requireContext())

        // Navigate to login screen
        findNavController().navigate(R.id.action_adminDashboardFragment_to_loginFragment)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun setupSystemHealthCards() {
        animateCardValue(adminBinding.cardActiveSensors, 0f, 47f, "47/50")
        animateCardValue(adminBinding.cardSystemUptime, 0f, 99.8f, "99.8%")
        animateCardValue(adminBinding.cardDataAccuracy, 0f, 96.5f, "96.5%")

        adminBinding.cardActiveSensors.findViewById<android.widget.ProgressBar>(R.id.progressBar).apply {
            max = 100
            progress = 94
        }
    }

    private fun animateCardValue(cardView: com.google.android.material.card.MaterialCardView,
                                 start: Float, end: Float, finalText: String) {
        val valueTextView = cardView.findViewById<android.widget.TextView>(R.id.tvValue) ?: return
        ValueAnimator.ofFloat(start, end).apply {
            duration = 1500
            addUpdateListener { animator ->
                val animatedValue = animator.animatedValue as Float
                valueTextView.text = if (end < 10) {
                    String.format("%.1f%%", animatedValue)
                } else {
                    finalText
                }
            }
            start()
        }
    }

    private fun updateLastSyncTime() {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        adminBinding.tvLastSync.text = "Last sync: ${timeFormat.format(Date())}"

        syncRunnable = Runnable {
            adminBinding.tvLastSync.text = "Last sync: ${timeFormat.format(Date())}"
            syncHandler.postDelayed(syncRunnable!!, 30000)
        }
        syncHandler.postDelayed(syncRunnable!!, 30000)
    }

    private fun setupAlertFeed() {
        alertAdapter = AlertAdapter { alert ->
            adminViewModel.markAlertAsRead(alert.id)
            Toast.makeText(requireContext(), "Alert: ${alert.title}", Toast.LENGTH_SHORT).show()
        }

        adminBinding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertAdapter
        }
    }

    private fun setupVillageGrid() {
        villageAdapter = VillageAdapter { village ->
            Toast.makeText(requireContext(), "Village: ${village.name}", Toast.LENGTH_SHORT).show()
        }

        adminBinding.rvVillages.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = villageAdapter
        }
    }

    private fun setupCharts() {
        setupRiskTrendChart()
        setupSensorHealthChart()
        setupAlertDistributionChart()
    }

    private fun setupRiskTrendChart() {
        val chart = adminBinding.chartRiskTrend

        val entries = mutableListOf<Entry>()
        val hours = listOf("00", "03", "06", "09", "12", "15", "18", "21")

        hours.forEachIndexed { index, hour ->
            val baseRisk = when (hour) {
                "00", "03" -> 65f + Random().nextFloat() * 10f
                "06", "09" -> 40f + Random().nextFloat() * 15f
                "12", "15" -> 30f + Random().nextFloat() * 10f
                else -> 50f + Random().nextFloat() * 15f
            }
            entries.add(Entry(index.toFloat(), baseRisk))
        }

        val dataSet = LineDataSet(entries, "Risk Level (%)")
        dataSet.color = Color.parseColor("#FF6B6B")
        dataSet.valueTextColor = Color.DKGRAY
        dataSet.lineWidth = 2.5f
        dataSet.setCircleColor(Color.parseColor("#FF6B6B"))
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#22FF6B6B")
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.valueTextSize = 10f

        val lineData = LineData(dataSet)
        chart.data = lineData

        val xAxis = chart.xAxis
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return hours.getOrNull(value.toInt()) ?: ""
            }
        }
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 11f
        xAxis.setDrawGridLines(false)

        chart.axisLeft.textSize = 11f
        chart.axisRight.isEnabled = false
        chart.legend.textSize = 12f
        chart.description.text = "24-Hour Risk Trend"
        chart.description.textSize = 12f

        chart.setTouchEnabled(true)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)

        chart.animateXY(1500, 1500)
        chart.invalidate()
    }

    private fun setupSensorHealthChart() {
        val chart = adminBinding.chartSensorHealth

        val entries = listOf(
            PieEntry(85f, "Healthy"),
            PieEntry(10f, "Low Battery"),
            PieEntry(3f, "No Signal"),
            PieEntry(2f, "Maintenance")
        )

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FFC107"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#F44336")
        )

        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f
        dataSet.selectionShift = 5f
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%"
            }
        }

        val pieData = PieData(dataSet)
        pieData.setValueTextColor(Color.BLACK)
        pieData.setValueTextSize(12f)

        chart.apply {
            data = pieData
            legend.isEnabled = true
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.textSize = 12f
            legend.xEntrySpace = 15f
            legend.yEntrySpace = 8f
            legend.yOffset = 15f

            setCenterText("50\nSensors")
            setCenterTextSize(14f)
            setCenterTextColor(Color.DKGRAY)
            setCenterTextTypeface(Typeface.DEFAULT_BOLD)

            holeRadius = 40f
            transparentCircleRadius = 45f

            setEntryLabelColor(Color.TRANSPARENT)
            setEntryLabelTextSize(0f)
            setDrawEntryLabels(false)

            description.isEnabled = true
            description.text = "Sensor Health Status"
            description.textSize = 14f
            description.textAlign = Paint.Align.CENTER
            description.yOffset = -25f

            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)

            animateY(1000, Easing.EaseInOutCubic)
            invalidate()
        }
    }

    private fun setupAlertDistributionChart() {
        val chart = adminBinding.chartAlertDistribution

        val entries = listOf(
            BarEntry(0f, 8f),    // Landslide - RED
            BarEntry(1f, 15f),   // Livestock - GREEN
            BarEntry(2f, 12f),   // Rainfall - BLUE
            BarEntry(3f, 5f),    // Irrigation - ORANGE
            BarEntry(4f, 3f)     // Theft - PURPLE
        )

        val labels = listOf("Landslide", "Livestock", "Rainfall", "Irrigation", "Theft")

        val dataSet = BarDataSet(entries, "Alert Count")

        // VARIED COLORS for different alert types
        dataSet.colors = listOf(
            Color.parseColor("#F44336"),  // Red for Landslide
            Color.parseColor("#4CAF50"),  // Green for Livestock
            Color.parseColor("#2196F3"),  // Blue for Rainfall
            Color.parseColor("#FF9800"),  // Orange for Irrigation
            Color.parseColor("#9C27B0")   // Purple for Theft
        )

        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 11f
        dataSet.setDrawValues(true)

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        chart.data = barData

        val xAxis = chart.xAxis
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return labels.getOrNull(value.toInt()) ?: ""
            }
        }
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 10f
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelCount = labels.size

        chart.axisLeft.textSize = 11f
        chart.axisRight.isEnabled = false
        chart.legend.textSize = 12f
        chart.description.text = "Today's Alert Distribution"
        chart.description.textSize = 12f

        chart.setFitBars(true)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)

        chart.animateY(1500)
        chart.invalidate()
    }

    private fun setupQuickActions() {

        adminBinding.cardManageUsers.setOnClickListener {
            Toast.makeText(requireContext(),
                "👤 Manage Users feature coming soon (Future Update)",
                Toast.LENGTH_SHORT).show()
        }

        adminBinding.cardManageVillages.setOnClickListener {
            Toast.makeText(requireContext(),
                "🏡 Village Manager will be added in next update",
                Toast.LENGTH_SHORT).show()
        }

        adminBinding.cardSensorNetwork.setOnClickListener {
            Toast.makeText(requireContext(),
                "📡 Sensor Network panel under development",
                Toast.LENGTH_SHORT).show()
        }

        adminBinding.cardAnalytics.setOnClickListener {
            Toast.makeText(requireContext(),
                "📊 Advanced AI Analytics coming soon",
                Toast.LENGTH_SHORT).show()
        }

        adminBinding.cardAlertConfig.setOnClickListener {
            Toast.makeText(requireContext(),
                "🚨 Alert Configuration will be available soon",
                Toast.LENGTH_SHORT).show()
        }

        adminBinding.cardSystemSettings.setOnClickListener {
            Toast.makeText(requireContext(),
                "⚙️ System Settings module coming in next version",
                Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            adminViewModel.systemHealth.collect { health ->
                health?.let {
                    updateSystemHealth(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            adminViewModel.alerts.collect { alerts ->
                alertAdapter.submitList(alerts)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            adminViewModel.villages.collect { villages ->
                villageAdapter.submitList(villages)
            }
        }
    }

    private fun updateSystemHealth(health: com.example.ecogeoguard.data.model.SystemHealth) {
        animateCardValue(adminBinding.cardActiveSensors, 0f, health.activeSensors.toFloat(), "${health.activeSensors}/${health.totalSensors}")
        animateCardValue(adminBinding.cardSystemUptime, 0f, health.uptimePercentage, "${String.format("%.1f", health.uptimePercentage)}%")
        animateCardValue(adminBinding.cardDataAccuracy, 0f, health.dataAccuracy, "${String.format("%.1f", health.dataAccuracy)}%")

        adminBinding.cardActiveSensors.findViewById<android.widget.ProgressBar>(R.id.progressBar).progress =
            health.sensorHealthPercentage
    }

    private fun startDataRefresh() {
        syncHandler.postDelayed({
            adminViewModel.refreshDashboardData()
            startDataRefresh()
        }, 10000)
    }
}