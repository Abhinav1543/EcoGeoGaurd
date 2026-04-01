package com.example.ecogeoguard.ui.farmer

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.*
import com.example.ecogeoguard.databinding.FragmentFarmerDashboardBinding
import com.example.ecogeoguard.services.AlertNotificationService
import com.example.ecogeoguard.ui.common.DashboardBaseFragment
import com.example.ecogeoguard.ui.farmer.adapter.WeatherAlertAdapter
import com.example.ecogeoguard.utils.RoleManager
import com.example.ecogeoguard.viewmodel.FarmerViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay


@AndroidEntryPoint
class FarmerDashboardFragment : DashboardBaseFragment() {

    override val role = UserRole.FARMER

    // Use different names to avoid hiding superclass properties
    private var _farmerBinding: FragmentFarmerDashboardBinding? = null
    private val farmerBinding get() = _farmerBinding!!

    private val farmerViewModel: FarmerViewModel by viewModels()

    @Inject
    lateinit var roleManager: RoleManager

    private lateinit var fieldSpinnerAdapter: ArrayAdapter<String>
    private val fieldNames = mutableListOf<String>()
    private val fieldIdMap = mutableMapOf<String, String>() // name -> id

    private lateinit var alertAdapter: WeatherAlertAdapter

    private val alertSimulationHandler = Handler(Looper.getMainLooper())
    private var alertCheckRunnable: Runnable? = null

    // Broadcast receiver for emergency alerts from service
    private val emergencyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "EMERGENCY_ALERT_RECEIVED") {
                val alertId = intent.getStringExtra("alert_id") ?: ""
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



        binding.roleContentContainer.removeAllViews()
        _farmerBinding = FragmentFarmerDashboardBinding.inflate(inflater, binding.roleContentContainer, true)


        return binding.root
    }

    // Add this function to force visible colors







    private fun fixContainerHeight() {
        try {
            // Don't change LayoutParams type - use the existing type
            // Just ensure the container has weight/layout params to take space

            // Get the parent of roleContentContainer (which is likely a LinearLayout)
            val parent = binding.roleContentContainer.parent as? android.widget.LinearLayout
            if (parent != null) {
                // Make sure the container has weight to take remaining space
                val params = binding.roleContentContainer.layoutParams as? android.widget.LinearLayout.LayoutParams
                params?.weight = 1f
                params?.height = 0
                binding.roleContentContainer.layoutParams = params
            }

            // Force layout refresh
            binding.roleContentContainer.requestLayout()
            farmerBinding.root.requestLayout()

            android.util.Log.d("FIX", "Container height fixed safely!")
        } catch (e: Exception) {
            android.util.Log.e("FIX", "Error fixing container: ${e.message}")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        setupUI()


        fixContainerHeight()

        setupObservers()
        setupAlertService()
        startPeriodicAlertCheck()

        view.postDelayed({
            binding.roleContentContainer.visibility = View.VISIBLE
            farmerBinding.root.visibility = View.VISIBLE
            fixContainerHeight()
            farmerBinding.root.requestLayout()
            farmerBinding.root.invalidate()
        }, 2000)

        // Register emergency receiver
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(emergencyReceiver, android.content.IntentFilter("EMERGENCY_ALERT_RECEIVED"))
    }

    private fun setupUI() {

        setupToolbar()
        setupFieldSpinner()
        setupAlertsRecyclerView()
        setupRefreshButton()
        setupViewDetailsButton()
        setupSilenceButton()
        setupEmergencyAcknowledgeButton()

        val testBtn = android.widget.Button(requireContext())
        testBtn.text = "TEST VISIBILITY"
        testBtn.setOnClickListener {
            android.util.Log.d("TEST", "Button clicked")
            farmerBinding.tvSoilMoisture.text = "68.2%"
            farmerBinding.tvSoilMoisture.setTextColor(android.graphics.Color.RED)
            farmerBinding.tvSoilMoisture.textSize = 30f
            farmerBinding.tvSoilMoisture.visibility = View.VISIBLE
            farmerBinding.progressMoisture.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Test UI Updated", Toast.LENGTH_SHORT).show()
        }

        // Add at top
        (farmerBinding.root as? android.widget.LinearLayout)?.addView(testBtn, 0)

        // Check if all views are accessible
        android.util.Log.d("FARMER_DEBUG", "spinnerFields exists: ${farmerBinding.spinnerFields != null}")
        android.util.Log.d("FARMER_DEBUG", "tvSoilMoisture exists: ${farmerBinding.tvSoilMoisture != null}")
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
                R.id.action_irrigation_history -> {
                    showIrrigationHistory()
                    true
                }
                R.id.action_simulate_rainfall -> {
                    showRainfallSimulator()
                    true
                }
                R.id.action_emergency_test -> {
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

    private fun setupFieldSpinner() {
        fieldSpinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fieldNames)
        fieldSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        farmerBinding.spinnerFields.adapter = fieldSpinnerAdapter

        // PEHLE SPINNER KO GONE KARO JAB TAK DATA NA AAYE
        farmerBinding.spinnerFields.visibility = View.GONE

        farmerBinding.spinnerFields.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (fieldNames.isNotEmpty() && position < fieldNames.size) {
                    val fieldName = fieldNames[position]
                    val fieldId = fieldIdMap[fieldName]
                    android.util.Log.d("FARMER_TEST", "Selected field: $fieldName, ID: $fieldId")
                    fieldId?.let {
                        farmerViewModel.selectField(it)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupAlertsRecyclerView() {
        alertAdapter = WeatherAlertAdapter { alert ->
            showAlertDetails(alert)
        }
        farmerBinding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertAdapter
        }
    }

    private fun setupRefreshButton() {
        farmerBinding.btnRefresh.setOnClickListener {
            farmerViewModel.refreshManually()
            animateRefreshButton()
        }
    }

    private fun setupViewDetailsButton() {

        farmerBinding.btnViewFieldDetails.setOnClickListener {

            val field = farmerViewModel.selectedField.value

            field?.let {

                try {
                    // Direct navigation (NO SafeArgs needed)
                    findNavController().navigate(
                        R.id.action_farmerDashboardFragment_to_fieldDetailFragment
                    )

                } catch (e: Exception) {
                    Toast.makeText(requireContext(),
                        "Field details coming soon",
                        Toast.LENGTH_SHORT).show()
                }

            } ?: run {
                Toast.makeText(requireContext(),
                    "No field selected",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSilenceButton() {
        farmerBinding.btnSilenceAlarm.setOnClickListener {
            AlertNotificationService.muteAllBuzzers(requireContext())
            farmerBinding.cardEmergencyBanner.visibility = View.GONE
            farmerViewModel.dismissEmergency()
            Toast.makeText(requireContext(), "Alarm silenced", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupEmergencyAcknowledgeButton() {
        farmerBinding.btnEmergencyAcknowledge.setOnClickListener {
            farmerBinding.emergencyOverlay.visibility = View.GONE
            AlertNotificationService.muteAllBuzzers(requireContext())
            farmerViewModel.dismissEmergency()

            // Log acknowledgment
            val alert = farmerViewModel.getEmergencyAlert()
            alert?.let {
                Snackbar.make(farmerBinding.root, "Emergency acknowledged", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setupObservers() {
        android.util.Log.d("FARMER_TEST", "Setting up observers")

        // FIRST - Collect fields immediately
        viewLifecycleOwner.lifecycleScope.launch {
            farmerViewModel.fields.collect { fields ->
                android.util.Log.d("FARMER_TEST", "Fields received: ${fields.size}")
                updateFieldSpinner(fields)

                if (fields.isNotEmpty() && farmerViewModel.selectedField.value == null) {
                    android.util.Log.d("FARMER_TEST", "Auto-selecting first field: ${fields[0].name}")
                    farmerViewModel.selectField(fields[0].id)
                }
            }
        }

        // SECOND - Collect sensor data
        viewLifecycleOwner.lifecycleScope.launch {
            farmerViewModel.currentSensorData.collect { data ->
                android.util.Log.d("FARMER_TEST", "Sensor data received: ${data != null}")
                data?.let {
                    updateSensorUI(it)
                    android.util.Log.d("FARMER_TEST", "Moisture value: ${it.soilMoisture}")
                }
            }
        }

        // THIRD - Collect crop health
        viewLifecycleOwner.lifecycleScope.launch {
            farmerViewModel.cropHealth.collect { health ->
                android.util.Log.d("FARMER_TEST", "Crop health received: ${health?.overallHealth}")
                health?.let { updateCropHealthUI(it) }
            }
        }

        // FOURTH - Collect irrigation
        viewLifecycleOwner.lifecycleScope.launch {
            farmerViewModel.irrigationRecommendation.collect { rec ->
                android.util.Log.d("FARMER_TEST", "Irrigation received: ${rec?.shouldIrrigate}")
                rec?.let { updateIrrigationUI(it) }
            }
        }

        // FIFTH - Collect alerts
        viewLifecycleOwner.lifecycleScope.launch {
            farmerViewModel.activeAlerts.collect { alerts ->
                android.util.Log.d("FARMER_TEST", "Alerts received: ${alerts.size}")
                alertAdapter.submitList(alerts)
            }
        }

        // SIXTH - Loading state
        viewLifecycleOwner.lifecycleScope.launch {
            farmerViewModel.isLoading.collect { loading ->
                android.util.Log.d("FARMER_TEST", "Loading state: $loading")
                farmerBinding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        // SEVENTH - Last updated
        viewLifecycleOwner.lifecycleScope.launch {
            farmerViewModel.lastUpdated.collect { timestamp ->
                updateLastUpdatedTime(timestamp)
            }
        }
    }

    private fun updateFieldSpinner(fields: List<Field>) {
        android.util.Log.d("FARMER_TEST", "updateFieldSpinner called with ${fields.size} fields")

        val previousSize = fieldNames.size
        fieldNames.clear()
        fieldIdMap.clear()

        fields.forEach { field ->
            fieldNames.add(field.name)
            fieldIdMap[field.name] = field.id
        }

        // AGAR FIELDS AA GAYE TO SPINNER UPDATE KARO
        if (fieldNames.isNotEmpty()) {
            fieldSpinnerAdapter.notifyDataSetChanged()

            // AGAR PEHLE KOI FIELD SELECT NAHI THI TO PEHLA FIELD SELECT KARO
            if (farmerViewModel.selectedField.value == null) {
                farmerBinding.spinnerFields.setSelection(0)
            }

            // SPINNER VISIBLE KARO (AGAR PEHLE GONE THA)
            farmerBinding.spinnerFields.visibility = View.VISIBLE
        } else {
            // AGAR FIELDS NAHI HAIN TO "NO FIELDS" DIKHAO
            farmerBinding.spinnerFields.visibility = View.GONE
            // OPTIONAL: TextView dikhao ki koi field nahi
        }
    }

    private fun checkViewVisibility() {
        android.util.Log.d("VISIBILITY", "=== VIEW VISIBILITY CHECK ===")

        val views = listOf(
            "tvSoilMoisture" to farmerBinding.tvSoilMoisture,
            "progressMoisture" to farmerBinding.progressMoisture,
            "tvTemperature" to farmerBinding.tvTemperature,
            "tvRainfall" to farmerBinding.tvRainfall,
            "tvHumidity" to farmerBinding.tvHumidity,
            "progressCropHealth" to farmerBinding.progressCropHealth,
            "tvCropHealth" to farmerBinding.tvCropHealth
        )

        views.forEach { (name, view) ->
            if (view != null) {
                android.util.Log.d("VISIBILITY", "$name - visibility: ${view.visibility} (0=VISIBLE, 4=INVISIBLE, 8=GONE)")
                android.util.Log.d("VISIBILITY", "$name - text: ${if (view is android.widget.TextView) view.text else "N/A"}")
            }
        }

        android.util.Log.d("VISIBILITY", "=== CHECK COMPLETE ===")
    }

    private fun updateSensorUI(data: FieldSensorData) {
        android.util.Log.d("FARMER_TEST", "updateSensorUI called with moisture: ${data.soilMoisture}")

        try {
            // Soil Moisture
            farmerBinding.tvSoilMoisture.text = "${"%.1f".format(data.soilMoisture)}%"
            farmerBinding.progressMoisture.progress = data.soilMoisture.toInt()

            // Temperature
            farmerBinding.tvTemperature.text = "${"%.1f".format(data.ambientTemperature)}°C"
            farmerBinding.tvSoilTemp.text = "Soil: ${"%.1f".format(data.soilTemperature)}°C"

            // Rainfall
            farmerBinding.tvRainfall.text = "${"%.1f".format(data.rainfall)} mm"
            val totalRainfall = data.rainfall * 24
            farmerBinding.tvRainfallTrend.text = "Last 24h: ${"%.1f".format(totalRainfall)}mm"

            // Humidity & Wind
            farmerBinding.tvHumidity.text = "${"%.0f".format(data.humidity)}%"
            farmerBinding.tvWindSpeed.text = "Wind: ${"%.1f".format(data.windSpeed)} km/h"

            // FORCE VIEW TO REFRESH - ADD THIS
            farmerBinding.tvSoilMoisture.invalidate()
            farmerBinding.progressMoisture.invalidate()
            farmerBinding.tvTemperature.invalidate()
            farmerBinding.tvRainfall.invalidate()
            farmerBinding.tvHumidity.invalidate()

            android.util.Log.d("FARMER_TEST", "UI updated and invalidated")

        } catch (e: Exception) {
            android.util.Log.e("FARMER_TEST", "Error updating UI: ${e.message}")
        }

        // Set colors based on values
        setMoistureColor(data.soilMoisture)
        setTemperatureColor(data.ambientTemperature)
        checkViewVisibility()

        if (data.soilMoisture > 0) {
            Toast.makeText(requireContext(), "Data updated: ${data.soilMoisture}%", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setMoistureColor(moisture: Float) {
        val color = when {
            moisture < 25 -> ContextCompat.getColor(requireContext(), R.color.dangerRed)
            moisture < 40 -> ContextCompat.getColor(requireContext(), R.color.warningOrange)
            moisture > 75 -> ContextCompat.getColor(requireContext(), R.color.info)
            else -> ContextCompat.getColor(requireContext(), R.color.success)
        }
        farmerBinding.tvSoilMoisture.setTextColor(color)

        val progressColor = when {
            moisture < 25 -> R.color.dangerRed
            moisture < 40 -> R.color.warningOrange
            moisture > 75 -> R.color.info
            else -> R.color.success
        }
        farmerBinding.progressMoisture.progressTintList = ContextCompat.getColorStateList(requireContext(), progressColor)
    }

    private fun setTemperatureColor(temp: Float) {
        val color = when {
            temp > 38 -> ContextCompat.getColor(requireContext(), R.color.dangerRed)
            temp > 35 -> ContextCompat.getColor(requireContext(), R.color.warningOrange)
            temp < 10 -> ContextCompat.getColor(requireContext(), R.color.info)
            else -> ContextCompat.getColor(requireContext(), R.color.textPrimary)
        }
        farmerBinding.tvTemperature.setTextColor(color)
    }

    private fun updateIrrigationUI(recommendation: IrrigationRecommendation) {
        if (recommendation.shouldIrrigate) {
            farmerBinding.ivIrrigationStatus.setImageResource(R.drawable.ic_warning)
            farmerBinding.ivIrrigationStatus.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.warningOrange)
            farmerBinding.tvIrrigationStatus.text = "⚠️ Irrigation Recommended"
            farmerBinding.tvIrrigationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warningOrange))

            farmerBinding.tvIrrigationDetails.text = """
                • Duration: ${recommendation.durationMinutes} minutes
                • Water needed: ${"%.0f".format(recommendation.waterAmountLiters)} liters
                • Reason: ${recommendation.reason}
            """.trimIndent()
            farmerBinding.tvIrrigationDetails.visibility = View.VISIBLE

            farmerBinding.btnIrrigateNow.visibility = View.VISIBLE
            farmerBinding.btnIrrigateNow.setOnClickListener {
                startIrrigation(recommendation)
            }
        } else {
            farmerBinding.ivIrrigationStatus.setImageResource(R.drawable.ic_check_circle)
            farmerBinding.ivIrrigationStatus.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.success)
            farmerBinding.tvIrrigationStatus.text = "✅ Soil moisture adequate"
            farmerBinding.tvIrrigationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
            farmerBinding.tvIrrigationDetails.visibility = View.GONE
            farmerBinding.btnIrrigateNow.visibility = View.GONE
        }
    }

//    private fun updateCropHealthUI(health: CropHealthIndex) {
//        // Overall health progress
//        farmerBinding.progressCropHealth.progress = health.overallHealth.toInt()
//        farmerBinding.tvCropHealth.text = "${"%.0f".format(health.overallHealth)}%"
//
//        // Set progress color based on health
//        val progressColor = when {
//            health.overallHealth < 40 -> R.color.dangerRed
//            health.overallHealth < 60 -> R.color.warningOrange
//            health.overallHealth < 80 -> R.color.info
//            else -> R.color.success
//        }
//        farmerBinding.progressCropHealth.progressTintList = ContextCompat.getColorStateList(requireContext(), progressColor)
//
//        // NDVI
//        farmerBinding.tvNdvi.text = "${"%.2f".format(health.ndvi)}"
//
//        // Pest Risk
//        farmerBinding.tvPestRisk.text = health.pestRiskLevel.name
//        farmerBinding.tvPestRisk.setTextColor(
//            when (health.pestRiskLevel) {
//                RiskLevel.LOW -> ContextCompat.getColor(requireContext(), R.color.success)
//                RiskLevel.MODERATE -> ContextCompat.getColor(requireContext(), R.color.info)
//                RiskLevel.HIGH -> ContextCompat.getColor(requireContext(), R.color.warningOrange)
//                RiskLevel.CRITICAL -> ContextCompat.getColor(requireContext(), R.color.dangerRed)
//            }
//        )
//
//        // Moisture Stress
//        farmerBinding.tvMoistureStress.text = "${"%.0f".format(health.moistureStress)}%"
//        farmerBinding.tvMoistureStress.setTextColor(
//            when {
//                health.moistureStress < 30 -> ContextCompat.getColor(requireContext(), R.color.success)
//                health.moistureStress < 50 -> ContextCompat.getColor(requireContext(), R.color.info)
//                health.moistureStress < 70 -> ContextCompat.getColor(requireContext(), R.color.warningOrange)
//                else -> ContextCompat.getColor(requireContext(), R.color.dangerRed)
//            }
//        )
//
//        // Disease Probability
//        farmerBinding.tvDiseaseProb.text = "${"%.0f".format(health.diseaseProbability)}%"
//
//        // Recommendation
//        farmerBinding.tvHealthRecommendation.text = health.recommendation
//    }

    private fun updateCropHealthUI(health: CropHealthIndex) {
        android.util.Log.d("FARMER_TEST", "updateCropHealthUI called with health: ${health.overallHealth}")

        try {
            // Overall health progress
            farmerBinding.progressCropHealth.progress = health.overallHealth.toInt()
            farmerBinding.tvCropHealth.text = "${"%.0f".format(health.overallHealth)}%"

            // NDVI
            farmerBinding.tvNdvi.text = "${"%.2f".format(health.ndvi)}"

            // Pest Risk
            farmerBinding.tvPestRisk.text = health.pestRiskLevel.name

            // Moisture Stress
            farmerBinding.tvMoistureStress.text = "${"%.0f".format(health.moistureStress)}%"

            // Disease Probability
            farmerBinding.tvDiseaseProb.text = "${"%.0f".format(health.diseaseProbability)}%"

            // Recommendation
            farmerBinding.tvHealthRecommendation.text = health.recommendation

            // FORCE REFRESH
            farmerBinding.progressCropHealth.invalidate()
            farmerBinding.tvCropHealth.invalidate()
            farmerBinding.tvNdvi.invalidate()

        } catch (e: Exception) {
            android.util.Log.e("FARMER_TEST", "Error updating crop health: ${e.message}")
        }

        // Set colors
        val progressColor = when {
            health.overallHealth < 40 -> R.color.dangerRed
            health.overallHealth < 60 -> R.color.warningOrange
            health.overallHealth < 80 -> R.color.info
            else -> R.color.success
        }
        farmerBinding.progressCropHealth.progressTintList = ContextCompat.getColorStateList(requireContext(), progressColor)

        farmerBinding.tvPestRisk.setTextColor(
            when (health.pestRiskLevel) {
                RiskLevel.LOW -> ContextCompat.getColor(requireContext(), R.color.success)
                RiskLevel.MODERATE -> ContextCompat.getColor(requireContext(), R.color.info)
                RiskLevel.HIGH -> ContextCompat.getColor(requireContext(), R.color.warningOrange)
                RiskLevel.CRITICAL -> ContextCompat.getColor(requireContext(), R.color.dangerRed)
            }
        )

        farmerBinding.tvMoistureStress.setTextColor(
            when {
                health.moistureStress < 30 -> ContextCompat.getColor(requireContext(), R.color.success)
                health.moistureStress < 50 -> ContextCompat.getColor(requireContext(), R.color.info)
                health.moistureStress < 70 -> ContextCompat.getColor(requireContext(), R.color.warningOrange)
                else -> ContextCompat.getColor(requireContext(), R.color.dangerRed)
            }
        )

        // Disease Probability
        farmerBinding.tvDiseaseProb.text = "${"%.0f".format(health.diseaseProbability)}%"
//
//        // Recommendation
        farmerBinding.tvHealthRecommendation.text = health.recommendation
    }

    private fun updateLastUpdatedTime(timestamp: Long) {
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        farmerBinding.tvLastUpdated.text = "Last updated: ${format.format(Date(timestamp))}"
    }

    private fun updateAlertBadge(count: Int) {
        // Update toolbar badge if you have one
        if (count > 0) {
            // You can show a badge on the notifications icon
        }
    }

    private fun showEmergencyBanner(alert: WeatherAlert) {
        farmerBinding.cardEmergencyBanner.visibility = View.VISIBLE
        farmerBinding.tvEmergencyTitle.text = when (alert.severity) {
            RiskLevel.CRITICAL -> "🚨 CRITICAL ALERT"
            RiskLevel.HIGH -> "⚠️ HIGH ALERT"
            else -> "⚠️ ALERT"
        }
        farmerBinding.tvEmergencyMessage.text = alert.message
    }

    private fun showEmergencyOverlay(title: String, message: String) {
        farmerBinding.emergencyOverlay.visibility = View.VISIBLE
        farmerBinding.tvEmergencyOverlayTitle.text = title
        farmerBinding.tvEmergencyOverlayMessage.text = message

        val alert = farmerViewModel.getEmergencyAlert()
        farmerBinding.tvEmergencyOverlayAction.text = alert?.recommendedAction ?: "Take immediate action!"
    }

    private fun triggerBuzzer() {
        AlertNotificationService.testBuzzer(requireContext())
    }

    private fun startIrrigation(recommendation: IrrigationRecommendation) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Start Irrigation")
            .setMessage("Start irrigation for ${recommendation.durationMinutes} minutes?")
            .setPositiveButton("Yes, Start") { _, _ ->
                Toast.makeText(
                    requireContext(),
                    "💧 Irrigation started for ${recommendation.durationMinutes} minutes",
                    Toast.LENGTH_LONG
                ).show()

                // Simulate irrigation effect
                farmerViewModel.simulateRainfall(5f) // Light rain simulation
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showIrrigationHistory() {
        val history = farmerViewModel.irrigationRecommendation.value
        if (history != null) {
            val items = arrayOf(
                "Last: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(history.timestamp))}",
                "Duration: ${history.durationMinutes} min",
                "Water: ${"%.0f".format(history.waterAmountLiters)} L",
                "Status: ${if (history.shouldIrrigate) "Needed" else "Not needed"}"
            )

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Irrigation History")
                .setItems(items) { _, _ -> }
                .setPositiveButton("OK", null)
                .show()
        } else {
            Toast.makeText(requireContext(), "No irrigation history", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRainfallSimulator() {
        val intensities = arrayOf("Light (2mm)", "Moderate (10mm)", "Heavy (25mm)", "Very Heavy (50mm)")
        val values = floatArrayOf(2f, 10f, 25f, 50f)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Simulate Rainfall")
            .setItems(intensities) { _, which ->
                farmerViewModel.simulateRainfall(values[which])
                Toast.makeText(
                    requireContext(),
                    "Simulating ${intensities[which]} rainfall",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun testEmergencyAlert() {
        // Create test critical alert
        val testAlert = WeatherAlert(
            id = "test_${System.currentTimeMillis()}",
            type = WeatherAlertType.LANDSLIDE,
            severity = RiskLevel.CRITICAL,
            message = "TEST ALERT: Simulated landslide detection",
            timestamp = System.currentTimeMillis(),
            expiryTimestamp = System.currentTimeMillis() + 3600000,
            affectedFields = listOf("field_001"),
            recommendedAction = "TEST: This is a test of the emergency system"
        )

        // We need to show this manually since it's a test
        showEmergencyOverlay("🚨 TEST EMERGENCY", testAlert.message)
        triggerBuzzer()
    }

    private fun showAlertDetails(alert: WeatherAlert) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${alert.type.name} Alert")
            .setMessage("""
            Severity: ${alert.severity}
            
            ${alert.message}
            
            Recommended Action:
            ${alert.recommendedAction}
            
            Time: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(alert.timestamp))}
        """.trimIndent())
            .setPositiveButton("Acknowledge") { _, _ ->
                Toast.makeText(requireContext(), "Alert acknowledged", Toast.LENGTH_SHORT).show()
                // In real app, send acknowledgment to server
            }
            .setNeutralButton("View on Map") { _, _ ->
                Toast.makeText(requireContext(), "Map view coming soon", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun setupAlertService() {
        AlertNotificationService.startService(requireContext())
    }

    private fun startPeriodicAlertCheck() {
        alertCheckRunnable = object : Runnable {
            override fun run() {
                if (isAdded) {
                    farmerViewModel.refreshManually()
                    alertCheckRunnable?.let {
                        alertSimulationHandler.postDelayed(it, 60000) // Check every minute
                    }
                }
            }
        }
        alertCheckRunnable?.let {
            alertSimulationHandler.postDelayed(it, 60000)
        }
    }

    private fun animateRefreshButton() {
        farmerBinding.btnRefresh.animate()
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
        findNavController().navigate(R.id.action_farmerDashboardFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        alertCheckRunnable?.let { alertSimulationHandler.removeCallbacks(it) }
        alertSimulationHandler.removeCallbacksAndMessages(null)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(emergencyReceiver)
        _farmerBinding = null
    }

    override fun onDashboardCreated() {
        // Farmer-specific initialization
    }
}