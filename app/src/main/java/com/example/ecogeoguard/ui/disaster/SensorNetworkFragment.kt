package com.example.ecogeoguard.ui.disaster

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.ecogeoguard.data.model.DisasterSensorNode
import com.example.ecogeoguard.data.model.DisasterSensorStatus
import com.example.ecogeoguard.data.model.DisasterSensorType
import com.example.ecogeoguard.databinding.FragmentSensorNetworkBinding
import com.example.ecogeoguard.ui.disaster.adapter.SensorNodeAdapter
import com.example.ecogeoguard.viewmodel.DisasterViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class SensorNetworkFragment : Fragment() {

    private var _binding: FragmentSensorNetworkBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DisasterViewModel by viewModels()
    private lateinit var sensorAdapter: SensorNodeAdapter

    // Filter state
    private var selectedTypes = mutableSetOf<DisasterSensorType>()
    private var selectedStatuses = mutableSetOf<DisasterSensorStatus>()
    private var searchQuery = ""
    private var currentSort = SortType.ID

    private enum class SortType { ID, TYPE, BATTERY, SIGNAL }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupChipGroups()
        setupSearch()
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
                R.id.action_map_view -> {
                    Toast.makeText(requireContext(), "Map view coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_export -> {
                    exportSensorReport()
                    true
                }
                R.id.action_settings -> {
                    showSensorSettings()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        sensorAdapter = SensorNodeAdapter(
            onItemClick = { sensor -> showSensorDetails(sensor) },
            onStatusClick = { sensor -> toggleSensorStatus(sensor) },
            onCalibrateClick = { sensor -> calibrateSensor(sensor) }
        )

        binding.rvSensors.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sensorAdapter
        }
    }

    private fun setupChipGroups() {
        // Type chips
        binding.chipGroupSensorType.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedTypes.clear()
            checkedIds.forEach { id ->
                when (id) {
                    R.id.chipVibration -> selectedTypes.add(DisasterSensorType.VIBRATION)
                    R.id.chipTilt -> selectedTypes.add(DisasterSensorType.TILT)
                    R.id.chipMoisture -> selectedTypes.add(DisasterSensorType.SOIL_MOISTURE)
                    R.id.chipRainfall -> selectedTypes.add(DisasterSensorType.RAINFALL)
                    R.id.chipTemp -> selectedTypes.add(DisasterSensorType.TEMPERATURE)
                }
            }
            applyFilters()
        }

        // Status chips
        binding.chipGroupStatus.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedStatuses.clear()
            checkedIds.forEach { id ->
                when (id) {
                    R.id.chipOnline -> selectedStatuses.add(DisasterSensorStatus.ONLINE)
                    R.id.chipOffline -> selectedStatuses.add(DisasterSensorStatus.OFFLINE)
                    R.id.chipLowBattery -> selectedStatuses.add(DisasterSensorStatus.LOW_BATTERY)
                    R.id.chipError -> selectedStatuses.add(DisasterSensorStatus.ERROR)
                }
            }
            applyFilters()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString() ?: ""
                applyFilters()
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnSort.setOnClickListener {
            showSortOptions()
        }

        binding.btnRefreshAll.setOnClickListener {
            viewModel.refreshManually()
            animateRefresh()
        }

        binding.btnCalibrateAll.setOnClickListener {
            showCalibrateAllDialog()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.sensors.collect { sensors ->
                updateStats(sensors)
                applyFilters()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.isVisible = isLoading
            }
        }

        lifecycleScope.launch {
            viewModel.lastUpdated.collect { timestamp ->
                updateLastUpdated(timestamp)
            }
        }
    }

    private fun updateStats(sensors: List<DisasterSensorNode>) {
        binding.tvTotalSensors.text = sensors.size.toString()
        binding.tvOnlineSensors.text = sensors.count { it.status == DisasterSensorStatus.ONLINE }.toString()
        binding.tvAlertingSensors.text = sensors.count {
            it.status == DisasterSensorStatus.ERROR || it.status == DisasterSensorStatus.LOW_BATTERY
        }.toString()
    }

    private fun updateLastUpdated(timestamp: Long) {
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        binding.tvLastUpdated.text = "Updated: ${format.format(Date(timestamp))}"
    }

    private fun applyFilters() {
        val allSensors = viewModel.sensors.value ?: emptyList()

        // Apply type filter
        var filtered = if (selectedTypes.isEmpty()) {
            allSensors
        } else {
            allSensors.filter { it.type in selectedTypes }
        }

        // Apply status filter
        filtered = if (selectedStatuses.isEmpty()) {
            filtered
        } else {
            filtered.filter { it.status in selectedStatuses }
        }

        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.id.contains(searchQuery, ignoreCase = true) ||
                        it.location.address?.contains(searchQuery, ignoreCase = true) == true ||
                        it.type.name.contains(searchQuery, ignoreCase = true)
            }
        }

        // Apply sorting
        filtered = when (currentSort) {
            SortType.ID -> filtered.sortedBy { it.id }
            SortType.TYPE -> filtered.sortedBy { it.type.name }
            SortType.BATTERY -> filtered.sortedByDescending { it.batteryLevel }
            SortType.SIGNAL -> filtered.sortedByDescending { it.signalStrength }
        }

        sensorAdapter.submitList(filtered)
    }

    private fun showSortOptions() {
        val options = arrayOf("ID", "Type", "Battery Level", "Signal Strength")
        val currentIndex = when (currentSort) {
            SortType.ID -> 0
            SortType.TYPE -> 1
            SortType.BATTERY -> 2
            SortType.SIGNAL -> 3
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort Sensors By")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                currentSort = when (which) {
                    0 -> SortType.ID
                    1 -> SortType.TYPE
                    2 -> SortType.BATTERY
                    3 -> SortType.SIGNAL
                    else -> SortType.ID
                }
                applyFilters()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSensorDetails(sensor: DisasterSensorNode) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sensor Details: ${sensor.id}")
            .setMessage("""
                📡 Type: ${sensor.type.name}
                📍 Location: ${sensor.location.address ?: "Unknown"}
                🏘️ Village: ${sensor.villageId}
                
                ⚡ Status: ${sensor.status.name}
                🔋 Battery: ${sensor.batteryLevel}%
                📶 Signal: ${sensor.signalStrength} dBm
                
                🕐 Last Reading: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sensor.lastReading))}
                📅 Installed: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(sensor.installedDate))}
                🔧 Firmware: ${sensor.firmwareVersion}
            """.trimIndent())
            .setPositiveButton("View Readings") { _, _ ->
                showSensorReadings(sensor)
            }
            .setNeutralButton("Calibrate") { _, _ ->
                calibrateSensor(sensor)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showSensorReadings(sensor: DisasterSensorNode) {
        val readings = viewModel.getSensorReadings(sensor.id, 24)

        val readingsText = if (readings.isEmpty()) {
            "No recent readings available"
        } else {
            readings.takeLast(5).joinToString("\n") { reading ->
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reading.timestamp))
                val values = reading.values.map { "${it.key}: ${String.format("%.1f", it.value)}" }.joinToString(", ")
                "$time - $values (${reading.quality})"
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Recent Readings - ${sensor.id}")
            .setMessage(readingsText)
            .setPositiveButton("Refresh") { _, _ ->
                viewModel.refreshManually()
                showSensorReadings(sensor)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun toggleSensorStatus(sensor: DisasterSensorNode) {
        val newStatus = when (sensor.status) {
            DisasterSensorStatus.ONLINE -> DisasterSensorStatus.OFFLINE
            DisasterSensorStatus.OFFLINE -> DisasterSensorStatus.ONLINE
            else -> sensor.status
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change Sensor Status")
            .setMessage("Change ${sensor.id} status from ${sensor.status} to $newStatus?")
            .setPositiveButton("Yes") { _, _ ->
                // In real app, update in repository
                Snackbar.make(binding.root, "Sensor status updated", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun calibrateSensor(sensor: DisasterSensorNode) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Calibrate Sensor")
            .setMessage("Start calibration for ${sensor.id}?")
            .setPositiveButton("Start") { _, _ ->
                // Show calibration progress
                val progressDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Calibrating")
                    .setMessage("Please wait...")
                    .setCancelable(false)
                    .show()

                // Simulate calibration
                binding.root.postDelayed({
                    progressDialog.dismiss()
                    Snackbar.make(binding.root, "Sensor calibrated successfully", Snackbar.LENGTH_SHORT).show()
                }, 2000)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCalibrateAllDialog() {
        val count = sensorAdapter.itemCount
        if (count == 0) {
            Toast.makeText(requireContext(), "No sensors to calibrate", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Calibrate All Sensors")
            .setMessage("This will calibrate all $count sensors. Continue?")
            .setPositiveButton("Yes") { _, _ ->
                val progressDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Calibrating All Sensors")
                    .setMessage("Please wait...")
                    .setCancelable(false)
                    .show()

                binding.root.postDelayed({
                    progressDialog.dismiss()
                    Snackbar.make(binding.root, "All sensors calibrated", Snackbar.LENGTH_SHORT).show()
                }, 3000)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showSensorSettings() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sensor Network Settings")
            .setItems(arrayOf(
                "Alert Thresholds",
                "Sampling Frequency",
                "Network Configuration",
                "Firmware Updates"
            )) { _, which ->
                when (which) {
                    0 -> Toast.makeText(requireContext(), "Alert thresholds", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(requireContext(), "Sampling frequency", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(requireContext(), "Network config", Toast.LENGTH_SHORT).show()
                    3 -> Toast.makeText(requireContext(), "Firmware updates", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun exportSensorReport() {
        val sensors = sensorAdapter.currentList
        if (sensors.isEmpty()) {
            Toast.makeText(requireContext(), "No sensors to export", Toast.LENGTH_SHORT).show()
            return
        }

        val report = buildString {
            append("📡 SENSOR NETWORK REPORT\n")
            append("========================\n")
            append("Generated: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
            append("Total Sensors: ${sensors.size}\n")
            append("Online: ${sensors.count { it.status == DisasterSensorStatus.ONLINE }}\n")
            append("Offline: ${sensors.count { it.status == DisasterSensorStatus.OFFLINE }}\n")
            append("Low Battery: ${sensors.count { it.status == DisasterSensorStatus.LOW_BATTERY }}\n")
            append("Error: ${sensors.count { it.status == DisasterSensorStatus.ERROR }}\n\n")

            append("SENSOR DETAILS:\n")
            sensors.forEachIndexed { index, sensor ->
                append("\n${index + 1}. ${sensor.id}\n")
                append("   Type: ${sensor.type.name}\n")
                append("   Location: ${sensor.location.address}\n")
                append("   Status: ${sensor.status}\n")
                append("   Battery: ${sensor.batteryLevel}%\n")
                append("   Signal: ${sensor.signalStrength} dBm\n")
            }
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

    private fun animateRefresh() {
        binding.btnRefreshAll.animate()
            .rotation(360f)
            .setDuration(500)
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}