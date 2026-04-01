package com.example.ecogeoguard.ui.farmer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.Field
import com.example.ecogeoguard.data.model.FieldSensorData
import com.example.ecogeoguard.data.model.IrrigationRecommendation
import com.example.ecogeoguard.databinding.FragmentFieldDetailBinding
import com.example.ecogeoguard.ui.farmer.adapter.SensorHistoryAdapter
import com.example.ecogeoguard.viewmodel.FarmerViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class FieldDetailFragment : Fragment() {

    private var _binding: FragmentFieldDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FarmerViewModel by viewModels()
    private lateinit var sensorHistoryAdapter: SensorHistoryAdapter
    private var currentField: Field? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFieldDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        loadFieldData()
    }

    private fun setupUI() {
        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_field -> {
                    showEditFieldDialog()
                    true
                }
                R.id.action_sensor_history -> {
                    toggleHistoryView()
                    true
                }
                R.id.action_export_data -> {
                    exportFieldData()
                    true
                }
                else -> false
            }
        }

        // Setup history RecyclerView
        sensorHistoryAdapter = SensorHistoryAdapter()
        binding.rvSensorHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sensorHistoryAdapter
        }

        // Setup chart toggle
        binding.btnToggleChart.setOnClickListener {
            toggleChartView()
        }

        // Setup irrigation button
        binding.btnManualIrrigation.setOnClickListener {
            showIrrigationOptions()
        }
    }

    private fun setupObservers() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                androidx.lifecycle.Lifecycle.State.STARTED
            ) {

                launch {
                    viewModel.selectedField.collect { field ->
                        field?.let {
                            currentField = it
                            updateFieldInfo(it)
                        }
                    }
                }

                launch {
                    viewModel.currentSensorData.collect { data ->
                        data?.let {
                            updateSensorData(it)
                            updateChart(listOf(it))
                        }
                    }
                }

                launch {
                    viewModel.sensorHistory.collect { history ->
                        sensorHistoryAdapter.submitList(history.take(20))
                        if (history.isNotEmpty()) {
                            updateChart(history)
                        }
                    }
                }

                launch {
                    viewModel.irrigationRecommendation.collect { rec ->
                        rec?.let { updateIrrigationInfo(it) }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility =
                            if (isLoading) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun loadFieldData() {
        arguments?.getString("fieldId")?.let { fieldId ->
            viewModel.selectField(fieldId)
        }
    }

    private fun updateFieldInfo(field: Field) {
        binding.tvFieldName.text = field.name
        binding.tvCropType.text = "Crop: ${field.cropType.name}"
        binding.tvSoilType.text = "Soil: ${field.soilType.name}"
        binding.tvArea.text = "Area: ${field.areaInAcres} acres"
        binding.tvLocation.text = "Location: ${field.location.address ?: "Unknown"}"
        binding.tvSensorId.text = "Sensor ID: ${field.sensorNodeId ?: "Not assigned"}"
    }

    private fun updateSensorData(data: FieldSensorData) {
        binding.tvCurrentMoisture.text = "${"%.1f".format(data.soilMoisture)}%"
        binding.tvCurrentTemp.text = "${"%.1f".format(data.ambientTemperature)}°C"
        binding.tvCurrentRainfall.text = "${"%.1f".format(data.rainfall)} mm"
        binding.tvBatteryLevel.text = "${"%.0f".format(data.batteryLevel)}%"

        // Update battery indicator
        val batteryColor = when {
            data.batteryLevel > 70 -> R.color.success
            data.batteryLevel > 30 -> R.color.warningOrange
            else -> R.color.dangerRed
        }
        binding.ivBatteryIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), batteryColor),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    private fun updateIrrigationInfo(rec: IrrigationRecommendation) {
        binding.tvLastIrrigation.text = "Last: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(rec.timestamp))}"
        binding.tvNextIrrigation.text = if (rec.shouldIrrigate) {
            "Recommended in ${rec.durationMinutes} min"
        } else {
            "Not needed"
        }
    }

    private fun updateChart(history: List<FieldSensorData>) {
        val entries = history.reversed().mapIndexed { index, data ->
            Entry(index.toFloat(), data.soilMoisture)
        }

        val dataSet = LineDataSet(entries, "Soil Moisture Trend").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primaryGreen)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
            lineWidth = 2f
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.primaryGreen))
            circleRadius = 3f
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.primaryGreenLight)
        }

        binding.chartSensorHistory.apply {
            data = LineData(dataSet)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisLeft.isEnabled = true
            axisRight.isEnabled = false
            description.isEnabled = false
            invalidate()
        }
    }

    private fun toggleHistoryView() {
        if (binding.rvSensorHistory.visibility == View.VISIBLE) {
            binding.rvSensorHistory.visibility = View.GONE
            binding.chartSensorHistory.visibility = View.VISIBLE
            binding.btnToggleChart.text = "Show History List"
        } else {
            binding.rvSensorHistory.visibility = View.VISIBLE
            binding.chartSensorHistory.visibility = View.GONE
            binding.btnToggleChart.text = "Show Chart"
        }
    }

    private fun toggleChartView() {
        if (binding.chartSensorHistory.visibility == View.VISIBLE) {
            binding.chartSensorHistory.visibility = View.GONE
            binding.rvSensorHistory.visibility = View.VISIBLE
        } else {
            binding.chartSensorHistory.visibility = View.VISIBLE
            binding.rvSensorHistory.visibility = View.GONE
        }
    }

    private fun showEditFieldDialog() {
        // Implementation for editing field details
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Field")
            .setMessage("Edit field details feature coming soon")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showIrrigationOptions() {
        val options = arrayOf("Start Manual Irrigation", "Schedule Irrigation", "View History")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Irrigation Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startManualIrrigation()
                    1 -> scheduleIrrigation()
                    2 -> viewIrrigationHistory()
                }
            }
            .show()
    }

    private fun startManualIrrigation() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_irrigation_duration, null)
        val slider = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.sliderDuration)
        val tvDuration = dialogView.findViewById<TextView>(R.id.tvDurationValue)
        val etNotes = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etIrrigationNotes)

        slider.addOnChangeListener { _, value, _ ->
            tvDuration.text = "${value.toInt()} minutes"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Start Manual Irrigation")
            .setView(dialogView)
            .setPositiveButton("Start") { _, _ ->
                val duration = slider.value.toInt()
                val notes = etNotes.text.toString()

                // Show loading
                binding.progressBar.visibility = View.VISIBLE

                // Simulate irrigation start
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "💧 Irrigation started for $duration minutes",
                        Toast.LENGTH_LONG
                    ).show()

                    // Simulate moisture increase
                    viewModel.simulateRainfall(5f)
                }, 1500)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun scheduleIrrigation() {
        Toast.makeText(requireContext(), "Schedule irrigation coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun viewIrrigationHistory() {
        Toast.makeText(requireContext(), "Irrigation history coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun exportFieldData() {
        Toast.makeText(requireContext(), "Exporting field data...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}