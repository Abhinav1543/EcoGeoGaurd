package com.example.ecogeoguard.ui.livestock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.*
import com.example.ecogeoguard.databinding.FragmentLivestockDashboardBinding
import com.example.ecogeoguard.databinding.DialogAddAnimalBinding
import com.example.ecogeoguard.services.AlertNotificationService
import com.example.ecogeoguard.ui.common.DashboardBaseFragment
import com.example.ecogeoguard.ui.livestock.adapter.AnimalAdapter
import com.example.ecogeoguard.ui.livestock.adapter.HealthAlertAdapter
import com.example.ecogeoguard.utils.RoleManager
import com.example.ecogeoguard.viewmodel.LivestockViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.launch
import android.util.Log

@AndroidEntryPoint
class LivestockDashboardFragment : DashboardBaseFragment() {

    override val role = UserRole.LIVESTOCK_OWNER

    private var _livestockBinding: FragmentLivestockDashboardBinding? = null
    private val livestockBinding get() = _livestockBinding!!

    private val livestockViewModel: LivestockViewModel by viewModels()

    @Inject
    lateinit var roleManager: RoleManager

    private lateinit var animalAdapter: AnimalAdapter
    private lateinit var alertAdapter: HealthAlertAdapter

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
        super.onCreateView(inflater, container, savedInstanceState)

        Log.d("LIVESTOCK_DEBUG", "onCreateView: Starting")
        Log.d("LIVESTOCK_DEBUG", "roleContentContainer exists: ${binding.roleContentContainer != null}")

        binding.roleContentContainer.removeAllViews()
        _livestockBinding = FragmentLivestockDashboardBinding.inflate(inflater, binding.roleContentContainer, true)

        Log.d("LIVESTOCK_DEBUG", "livestockBinding created: ${_livestockBinding != null}")
        Log.d("LIVESTOCK_DEBUG", "Container child count after inflate: ${binding.roleContentContainer.childCount}")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        setupAlertService()
        startAutoRefresh()
        setupStatsClickListeners()

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
            livestockBinding.root.requestLayout()
            Log.d("LIVESTOCK_DEBUG", "Container height fixed")
        } catch (e: Exception) {
            Log.e("LIVESTOCK_DEBUG", "Error fixing container: ${e.message}")
        }
    }

    private fun setupUI() {
        Log.d("LIVESTOCK_DEBUG", "setupUI: Starting")

        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()
        setupSilenceButton()
        setupEmergencyAcknowledgeButton()

        Log.d("LIVESTOCK_DEBUG", "rvAnimals exists: ${livestockBinding.rvAnimals != null}")
        Log.d("LIVESTOCK_DEBUG", "rvAlerts exists: ${livestockBinding.rvAlerts != null}")
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
                R.id.action_add_animal -> {
                    showAddAnimalDialog()
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

    private fun setupRecyclerViews() {
        animalAdapter = AnimalAdapter(
            onItemClick = { animal ->
                navigateToAnimalDetail(animal.id)
            },
            onLocationClick = { animal ->
                showAnimalOnMap(animal)
            }
        )
        livestockBinding.rvAnimals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = animalAdapter
        }

        alertAdapter = HealthAlertAdapter(
            onItemClick = { alert ->
                showAlertDetails(alert)
            },
            onAcknowledge = { alert ->
                livestockViewModel.acknowledgeAlert(alert.id)
            }
        )
        livestockBinding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertAdapter
        }
    }

    private fun setupClickListeners() {
        livestockBinding.btnRefresh.setOnClickListener {
            livestockViewModel.refreshManually()
            animateRefreshButton()
        }

        livestockBinding.btnViewAllAnimals.setOnClickListener {
            showAllAnimalsDialog()
        }

        livestockBinding.btnManageZones.setOnClickListener {
            navigateToSafeZoneManager()
        }

        livestockBinding.btnTrackAll.setOnClickListener {
            showTrackAllAnimals()
        }

        livestockBinding.btnAddAnimal.setOnClickListener {
            showAddAnimalDialog()
        }

        livestockBinding.btnHealthReport.setOnClickListener {
            generateOverallHealthReport()
        }
    }

    private fun setupStatsClickListeners() {
        livestockBinding.cardTotalAnimals.setOnClickListener {
            showAllAnimalsDialog()
        }

        livestockBinding.cardActiveAlerts.setOnClickListener {
            if (livestockViewModel.healthAlerts.value.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No active alerts", Toast.LENGTH_SHORT).show()
            } else {
                livestockBinding.rvAlerts.smoothScrollToPosition(0)
            }
        }

        livestockBinding.cardOnlineNow.setOnClickListener {
            val onlineCount = livestockBinding.tvOnlineNow.text.toString().toInt()
            Toast.makeText(requireContext(), "$onlineCount animals are online now", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSilenceButton() {
        livestockBinding.btnSilenceAlarm.setOnClickListener {
            AlertNotificationService.muteAllBuzzers(requireContext())
            livestockBinding.cardEmergencyBanner.visibility = View.GONE
            livestockViewModel.dismissEmergency()
            Toast.makeText(requireContext(), "🔕 All alarms silenced", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupEmergencyAcknowledgeButton() {
        livestockBinding.btnEmergencyAcknowledge.setOnClickListener {
            livestockBinding.emergencyOverlay.visibility = View.GONE
            AlertNotificationService.muteAllBuzzers(requireContext())
            livestockViewModel.dismissEmergency()
            Snackbar.make(livestockBinding.root, "Emergency acknowledged", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                livestockViewModel.animals.collect { animals ->
                    Log.d("LIVESTOCK_DEBUG", "Animals received: ${animals.size}")
                    animalAdapter.submitList(animals)
                    updateStats(animals)
                }
            }

            launch {
                livestockViewModel.currentLocations.collect { locations ->
                    Log.d("LIVESTOCK_DEBUG", "Locations received: ${locations.size}")
                    animalAdapter.updateLocations(locations)
                }
            }

            launch {
                livestockViewModel.healthAlerts.collect { alerts ->
                    Log.d("LIVESTOCK_DEBUG", "Alerts received: ${alerts.size}")
                    alertAdapter.submitList(alerts)
                    val unreadCount = alerts.count { !it.isAcknowledged }
                    livestockBinding.tvAlertCount.text = "$unreadCount new"

                    if (unreadCount > 0) {
                        livestockBinding.tvAlertCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.dangerRed))
                    } else {
                        livestockBinding.tvAlertCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                    }
                }
            }

            launch {
                livestockViewModel.safeZones.collect { zones ->
                    Log.d("LIVESTOCK_DEBUG", "Zones received: ${zones.size}")
                    updateSafeZoneChips(zones)
                }
            }

            launch {
                livestockViewModel.showEmergencyBuzzer.collect { show ->
                    if (show) {
                        val alert = livestockViewModel.getEmergencyAlert()
                        alert?.let {
                            showEmergencyOverlay(
                                "🚨 CRITICAL: ${it.animalName}",
                                it.message
                            )
                            triggerBuzzer()
                        }
                    } else {
                        livestockBinding.emergencyOverlay.visibility = View.GONE
                        livestockBinding.cardEmergencyBanner.visibility = View.GONE
                    }
                }
            }

            launch {
                livestockViewModel.lastUpdated.collect { timestamp ->
                    updateLastUpdatedTime(timestamp)
                }
            }

            launch {
                livestockViewModel.isLoading.collect { isLoading ->
                    livestockBinding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun updateStats(animals: List<Animal>) {
        livestockBinding.tvTotalAnimals.text = animals.size.toString()

        val activeAlerts = livestockViewModel.healthAlerts.value?.count { !it.isAcknowledged } ?: 0
        livestockBinding.tvActiveAlerts.text = activeAlerts.toString()

        val onlineNow = animals.count { animal ->
            livestockViewModel.currentLocations.value?.get(animal.id)?.let { loc ->
                System.currentTimeMillis() - loc.timestamp < 15 * 60 * 1000
            } ?: false
        }
        livestockBinding.tvOnlineNow.text = onlineNow.toString()
    }

    private fun updateSafeZoneChips(zones: List<SafeZone>) {
        livestockBinding.layoutSafeZones.removeAllViews()

        zones.forEach { zone ->
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = zone.name
            chip.isClickable = true
            chip.isCheckable = false
            chip.setChipBackgroundColorResource(
                when (zone.type) {
                    ZoneType.DANGER -> R.color.dangerRed
                    ZoneType.QUARANTINE -> R.color.warningOrange
                    ZoneType.RESTRICTED -> R.color.warning
                    ZoneType.GRAZING -> R.color.success
                    ZoneType.RESTING -> R.color.info
                    ZoneType.WATERING -> R.color.info
                    else -> R.color.primaryGreen
                }
            )
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            chip.setPadding(16, 8, 16, 8)
            chip.setOnClickListener {
                showZoneDetails(zone)
            }
            livestockBinding.layoutSafeZones.addView(chip)

            val params = chip.layoutParams as? ViewGroup.MarginLayoutParams
            params?.setMargins(0, 0, 16, 0)
            chip.layoutParams = params
        }
    }

    private fun updateLastUpdatedTime(timestamp: Long) {
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        livestockBinding.tvLastUpdated.text = "Last updated: ${format.format(Date(timestamp))}"
    }

    private fun showEmergencyOverlay(title: String, message: String) {
        livestockBinding.emergencyOverlay.visibility = View.VISIBLE
        livestockBinding.tvEmergencyOverlayTitle.text = title
        livestockBinding.tvEmergencyOverlayMessage.text = message

        val alert = livestockViewModel.getEmergencyAlert()
        livestockBinding.tvEmergencyOverlayAction.text = alert?.recommendedAction ?: "Take immediate action!"
    }

    private fun triggerBuzzer() {
        AlertNotificationService.testBuzzer(requireContext())
    }

    private fun navigateToAnimalDetail(animalId: String) {
        try {
            val bundle = Bundle().apply {
                putString("animalId", animalId)
            }
            findNavController().navigate(R.id.animalDetailFragment, bundle)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Animal details coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToSafeZoneManager() {
        try {
            findNavController().navigate(R.id.safeZoneFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Safe zone manager coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAnimalOnMap(animal: Animal) {
        val location = livestockViewModel.currentLocations.value?.get(animal.id)
        if (location != null) {
            val uri = "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "Location unavailable", Toast.LENGTH_SHORT).show()
        }
    }



    private fun showTrackAllAnimals() {
        val locations = livestockViewModel.currentLocations.value
        if (locations.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No active locations", Toast.LENGTH_SHORT).show()
            return
        }

        val message = buildString {
            append("📍 Current Locations:\n\n")
            livestockViewModel.animals.value?.forEach { animal ->
                val loc = locations[animal.id]
                if (loc != null) {
                    append("• ${animal.name}: ")
                    append(String.format("%.4f, %.4f", loc.latitude, loc.longitude))
                    append("\n")
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Track All Animals")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Open Maps") { _, _ ->
                val firstLoc = locations.values.firstOrNull()
                firstLoc?.let {
                    val uri = "geo:0,0?q=${it.latitude},${it.longitude}"
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                    startActivity(intent)
                }
            }
            .show()
    }

    private fun showAddAnimalDialog() {
        val dialogBinding = DialogAddAnimalBinding.inflate(layoutInflater)

        setupTypeSpinner(dialogBinding)
        setupHealthStatusSpinner(dialogBinding)
        setupPregnancySpinner(dialogBinding)

        dialogBinding.cbPregnant.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.layoutDueDate.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        dialogBinding.btnSelectDate.setOnClickListener {
            showDatePicker { timestamp ->
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                dialogBinding.tvDueDate.text = dateFormat.format(Date(timestamp))
                dialogBinding.tvDueDate.tag = timestamp
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Animal")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                createAnimal(dialogBinding)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showAllAnimalsDialog() {
        val animals = livestockViewModel.animals.value ?: emptyList()
        if (animals.isEmpty()) {
            Toast.makeText(requireContext(), "No animals found", Toast.LENGTH_SHORT).show()
            return
        }

        val animalNames = animals.map { "${it.name} (${it.type.name})" }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("All Animals")
            .setItems(animalNames) { _, which ->
                navigateToAnimalDetail(animals[which].id)
            }
            .setPositiveButton("OK", null)
            .show()
    }

    private fun generateOverallHealthReport() {
        val animals = livestockViewModel.animals.value ?: emptyList()
        val alerts = livestockViewModel.healthAlerts.value ?: emptyList()

        val healthyCount = animals.count { it.healthStatus == HealthStatus.EXCELLENT || it.healthStatus == HealthStatus.GOOD }
        val fairCount = animals.count { it.healthStatus == HealthStatus.FAIR }
        val poorCount = animals.count { it.healthStatus == HealthStatus.POOR || it.healthStatus == HealthStatus.CRITICAL }

        val report = buildString {
            append("📊 LIVESTOCK HEALTH REPORT\n")
            append("========================\n\n")
            append("Total Animals: ${animals.size}\n")
            append("✅ Healthy: $healthyCount\n")
            append("⚠️ Fair: $fairCount\n")
            append("🔴 Critical: $poorCount\n")
            append("🚨 Active Alerts: ${alerts.count { !it.isAcknowledged }}\n\n")
            append("Animals requiring attention:\n")

            animals.filter { it.healthStatus == HealthStatus.POOR || it.healthStatus == HealthStatus.CRITICAL }
                .forEach {
                    append("• ${it.name} - ${it.healthStatus}\n")
                }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Health Report")
            .setMessage(report)
            .setPositiveButton("Share") { _, _ ->
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, report)
                    type = "text/plain"
                }
                startActivity(android.content.Intent.createChooser(shareIntent, "Share Report"))
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showZoneDetails(zone: SafeZone) {
        val animalsInZone = livestockViewModel.currentLocations.value?.filter { (_, loc) ->
            zone.isPointInside(loc.latitude, loc.longitude)
        }?.keys?.mapNotNull { animalId ->
            livestockViewModel.animals.value?.find { it.id == animalId }?.name
        } ?: emptyList()

        val message = buildString {
            append("📍 Type: ${zone.type.name}\n")
            append("📏 Radius: ${zone.radiusMeters}m\n")
            append("🚪 Alert on exit: ${if (zone.alertOnExit) "Yes" else "No"}\n")
            append("🚶 Alert on entry: ${if (zone.alertOnEntry) "Yes" else "No"}\n")
            if (zone.restrictedHours != null) {
                append("⏰ Restricted hours: ${zone.restrictedHours[0].startHour}:${zone.restrictedHours[0].startMinute} - ${zone.restrictedHours[0].endHour}:${zone.restrictedHours[0].endMinute}\n")
            }
            append("\n🐄 Animals in zone: ${animalsInZone.size}\n")
            if (animalsInZone.isNotEmpty()) {
                append("\n" + animalsInZone.joinToString("\n"))
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(zone.name)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Edit") { _, _ ->
                Toast.makeText(requireContext(), "Edit in Zone Manager", Toast.LENGTH_SHORT).show()
                navigateToSafeZoneManager()
            }
            .show()
    }

    private fun showAlertDetails(alert: HealthAlert) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${alert.type.name} Alert")
            .setMessage("""
                Animal: ${alert.animalName}
                Severity: ${alert.severity}
                
                ${alert.message}
                
                Recommended Action:
                ${alert.recommendedAction}
                
                Time: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(alert.timestamp))}
            """.trimIndent())
            .setPositiveButton("Acknowledge") { _, _ ->
                livestockViewModel.acknowledgeAlert(alert.id)
                Toast.makeText(requireContext(), "Alert acknowledged", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("View Animal") { _, _ ->
                navigateToAnimalDetail(alert.animalId)
            }
            .setNegativeButton("Close", null)
            .show()
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
                    livestockViewModel.refreshManually()
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
        livestockBinding.btnRefresh.animate()
            .rotation(360f)
            .setDuration(500)
            .start()
    }

    private fun createAnimal(binding: DialogAddAnimalBinding) {
        val name = binding.etName.text.toString()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter animal name", Toast.LENGTH_SHORT).show()
            return
        }

        val breed = binding.etBreed.text.toString()
        val age = binding.etAge.text.toString().toIntOrNull() ?: 0
        val weight = binding.etWeight.text.toString().toFloatOrNull() ?: 0f
        val tagNumber = binding.etTagNumber.text.toString()
        val type = AnimalType.values()[binding.spinnerType.selectedItemPosition]
        val healthStatus = HealthStatus.values()[binding.spinnerHealthStatus.selectedItemPosition]
        val pregnancyStatus = if (binding.cbPregnant.isChecked) {
            PregnancyStatus.PREGNANT
        } else {
            PregnancyStatus.values()[binding.spinnerPregnancy.selectedItemPosition]
        }

        val expectedDeliveryDate = if (binding.cbPregnant.isChecked) {
            binding.tvDueDate.tag as? Long
        } else null

        val animal = Animal(
            id = "animal_${System.currentTimeMillis()}",
            name = name,
            type = type,
            breed = breed,
            age = age,
            weight = weight,
            ownerId = "farmer_001",
            tagNumber = tagNumber,
            collarId = "collar_${System.currentTimeMillis()}",
            healthStatus = healthStatus,
            lastVaccinationDate = System.currentTimeMillis(),
            nextVaccinationDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
            pregnancyStatus = pregnancyStatus,
            expectedDeliveryDate = expectedDeliveryDate,
            createdAt = System.currentTimeMillis()
        )

        // FIX: Actually add the animal to ViewModel
        livestockViewModel.addAnimal(animal)
        Toast.makeText(requireContext(), "Animal added successfully", Toast.LENGTH_SHORT).show()
    }

    private fun setupTypeSpinner(binding: DialogAddAnimalBinding) {
        val types = AnimalType.values().map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter
    }

    private fun setupHealthStatusSpinner(binding: DialogAddAnimalBinding) {
        val statuses = HealthStatus.values().map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerHealthStatus.adapter = adapter
    }

    private fun setupPregnancySpinner(binding: DialogAddAnimalBinding) {
        val statuses = PregnancyStatus.values().map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPregnancy.adapter = adapter
    }

    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Expected Delivery Date")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            onDateSelected(selection)
        }

        datePicker.show(parentFragmentManager, "date_picker")
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
        findNavController().navigate(R.id.action_livestockDashboardFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshRunnable?.let { refreshHandler.removeCallbacks(it) }
        refreshHandler.removeCallbacksAndMessages(null)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(emergencyReceiver)
        _livestockBinding = null
    }

    override fun onDashboardCreated() {
        // Livestock-specific initialization
    }
}