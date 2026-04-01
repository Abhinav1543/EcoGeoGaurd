package com.example.ecogeoguard.ui.livestock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.*
import com.example.ecogeoguard.databinding.FragmentAnimalDetailBinding
import com.example.ecogeoguard.databinding.DialogAddAnimalBinding
import com.example.ecogeoguard.databinding.DialogEditAnimalBinding
import com.example.ecogeoguard.viewmodel.LivestockViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AnimalDetailFragment : Fragment() {

    private var _binding: FragmentAnimalDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LivestockViewModel by viewModels()
    private val args: AnimalDetailFragmentArgs by navArgs()

    private var currentAnimal: Animal? = null
    private var currentLocation: AnimalLocation? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        loadAnimalData()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnEdit.setOnClickListener {
            currentAnimal?.let { showEditAnimalDialog(it) }
        }

        binding.btnDelete.setOnClickListener {
            currentAnimal?.let { showDeleteConfirmation(it) }
        }

        binding.btnShowOnMap.setOnClickListener {
            currentLocation?.let { loc ->
                showLocationOnMap(loc)
            }
        }

        binding.btnVaccinationReminder.setOnClickListener {
            currentAnimal?.let { setVaccinationReminder(it) }
        }

        binding.btnHealthReport.setOnClickListener {
            currentAnimal?.let { generateHealthReport(it) }
        }

        binding.btnMovementHistory.setOnClickListener {
            currentAnimal?.let { showMovementHistory(it) }
        }

        binding.btnAddToZone.setOnClickListener {
            currentAnimal?.let { showAddToZoneDialog(it) }
        }
    }

    private fun setupObservers() {
        // Observe animal data
        viewModel.getAnimalById(args.animalId).observe(viewLifecycleOwner) { animal ->
            animal?.let {
                currentAnimal = it
                updateUI(it)
            }
        }

        // Observe location
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentLocations.collect { locations ->
                currentLocation = locations[args.animalId]
                updateLocationUI(currentLocation)
            }
        }

        // Observe health alerts for this animal
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.healthAlerts.collect { alerts ->
                val animalAlerts = alerts.filter { it.animalId == args.animalId }
                updateAlertsUI(animalAlerts)
            }
        }
    }

    private fun loadAnimalData() {
        viewModel.selectAnimal(args.animalId)
    }

    private fun updateUI(animal: Animal) {
        binding.apply {
            tvAnimalName.text = animal.name
            tvAnimalType.text = "${animal.type.name} • ${animal.breed}"
            tvTagNumber.text = "Tag: ${animal.tagNumber}"
            tvCollarId.text = "Collar: ${animal.collarId ?: "Not assigned"}"

            // Health status
            tvHealthStatus.text = animal.healthStatus.name
            val statusColor = when (animal.healthStatus) {
                HealthStatus.EXCELLENT, HealthStatus.GOOD -> R.color.success
                HealthStatus.FAIR -> R.color.warning
                HealthStatus.POOR, HealthStatus.CRITICAL -> R.color.dangerRed
                HealthStatus.UNDER_TREATMENT -> R.color.info
            }
            tvHealthStatus.backgroundTintList = ContextCompat.getColorStateList(requireContext(), statusColor)

            // Vital stats
            tvAge.text = animal.age.toString()
            tvWeight.text = String.format("%.1f", animal.weight)

            // Pregnancy badge
            ivPregnancyBadge.visibility = when (animal.pregnancyStatus) {
                PregnancyStatus.PREGNANT, PregnancyStatus.NEAR_DELIVERY -> View.VISIBLE
                else -> View.GONE
            }

            // Medical info
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            tvLastVaccination.text = animal.lastVaccinationDate?.let {
                dateFormat.format(Date(it))
            } ?: "Not recorded"

            tvNextVaccination.text = animal.nextVaccinationDate?.let {
                dateFormat.format(Date(it))
            } ?: "Not scheduled"

            tvPregnancyStatus.text = when (animal.pregnancyStatus) {
                PregnancyStatus.PREGNANT -> "Pregnant"
                PregnancyStatus.NEAR_DELIVERY -> "Near Delivery"
                PregnancyStatus.DELIVERED -> "Delivered"
                PregnancyStatus.NOT_PREGNANT -> "Not Pregnant"
                else -> "N/A"
            }

            if (animal.expectedDeliveryDate != null) {
                tvExpectedDelivery.text = "Due: ${dateFormat.format(Date(animal.expectedDeliveryDate))}"
                tvExpectedDelivery.visibility = View.VISIBLE
            } else {
                tvExpectedDelivery.visibility = View.GONE
            }

            // Load profile image
            if (animal.profileImageUrl != null) {
                Glide.with(this@AnimalDetailFragment)
                    .load(animal.profileImageUrl)
                    .placeholder(R.drawable.img_26)
                    .into(ivAnimalProfile)
            }
        }
    }

    private fun updateLocationUI(location: AnimalLocation?) {
        if (location != null) {
            binding.apply {
                tvCoordinates.text = String.format("%.6f° N, %.6f° E", location.latitude, location.longitude)

                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                tvLastSeenDetail.text = "Last seen: ${timeFormat.format(Date(location.timestamp))}"

                tvBatteryDetail.text = "🔋 ${location.batteryLevel}%"
                tvBatteryDetail.setTextColor(
                    when {
                        location.batteryLevel > 70 -> ContextCompat.getColor(requireContext(), R.color.success)
                        location.batteryLevel > 30 -> ContextCompat.getColor(requireContext(), R.color.warning)
                        else -> ContextCompat.getColor(requireContext(), R.color.dangerRed)
                    }
                )

                tvSignalDetail.text = "📶 ${location.signalStrength} dBm"
                tvSignalDetail.setTextColor(
                    when {
                        location.signalStrength > -70 -> ContextCompat.getColor(requireContext(), R.color.success)
                        location.signalStrength > -85 -> ContextCompat.getColor(requireContext(), R.color.warning)
                        else -> ContextCompat.getColor(requireContext(), R.color.dangerRed)
                    }
                )

                if (location.speed != null) {
                    tvSpeed.text = "Speed: ${String.format("%.1f", location.speed)} km/h"
                    tvSpeed.visibility = View.VISIBLE
                }
            }
        } else {
            binding.tvCoordinates.text = "Location unavailable"
            binding.tvLastSeenDetail.text = "Last seen: Unknown"
        }
    }

    private fun updateAlertsUI(alerts: List<HealthAlert>) {
        val activeAlerts = alerts.count { !it.isAcknowledged }
        binding.tvActiveAlertsCount.text = activeAlerts.toString()

        if (activeAlerts > 0) {
            binding.cardActiveAlerts.visibility = View.VISIBLE
            binding.tvActiveAlertsCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.dangerRed))
        } else {
            binding.cardActiveAlerts.visibility = View.GONE
        }
    }

    private fun showEditAnimalDialog(animal: Animal) {
        val dialogBinding = DialogEditAnimalBinding.inflate(layoutInflater)

        // Setup spinners
        setupTypeSpinner(dialogBinding)
        setupHealthStatusSpinner(dialogBinding)
        setupPregnancySpinner(dialogBinding)

        // Load current values
        dialogBinding.etName.setText(animal.name)
        dialogBinding.etBreed.setText(animal.breed)
        dialogBinding.etAge.setText(animal.age.toString())
        dialogBinding.etWeight.setText(animal.weight.toString())
        dialogBinding.etTagNumber.setText(animal.tagNumber)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Animal")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val updatedAnimal = animal.copy(
                    name = dialogBinding.etName.text.toString(),
                    breed = dialogBinding.etBreed.text.toString(),
                    age = dialogBinding.etAge.text.toString().toIntOrNull() ?: animal.age,
                    weight = dialogBinding.etWeight.text.toString().toFloatOrNull() ?: animal.weight,
                    tagNumber = dialogBinding.etTagNumber.text.toString(),
                    type = getSelectedAnimalType(dialogBinding),
                    healthStatus = getSelectedHealthStatus(dialogBinding),
                    pregnancyStatus = getSelectedPregnancyStatus(dialogBinding)
                )
                viewModel.updateAnimal(updatedAnimal)
                Snackbar.make(binding.root, "Animal updated successfully", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showDeleteConfirmation(animal: Animal) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Animal")
            .setMessage("Are you sure you want to delete ${animal.name}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteAnimal(animal.id)
                Toast.makeText(requireContext(), "Animal deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLocationOnMap(location: AnimalLocation) {
        // In a real app, this would open a map
        val uri = "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
        startActivity(intent)
    }

    private fun setVaccinationReminder(animal: Animal) {
        val calendar = Calendar.getInstance()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Vaccination Reminder")
            .setMessage("Set reminder for next vaccination date?")
            .setPositiveButton("30 Days") { _, _ ->
                calendar.add(Calendar.DAY_OF_YEAR, 30)
                val updatedAnimal = animal.copy(
                    nextVaccinationDate = calendar.timeInMillis
                )
                viewModel.updateAnimal(updatedAnimal)
                Snackbar.make(binding.root, "Reminder set for 30 days", Snackbar.LENGTH_SHORT).show()
            }
            .setNeutralButton("60 Days") { _, _ ->
                calendar.add(Calendar.DAY_OF_YEAR, 60)
                val updatedAnimal = animal.copy(
                    nextVaccinationDate = calendar.timeInMillis
                )
                viewModel.updateAnimal(updatedAnimal)
                Snackbar.make(binding.root, "Reminder set for 60 days", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateHealthReport(animal: Animal) {
        val reports = listOf(
            "✅ Health Status: ${animal.healthStatus}",
            "💉 Last Vaccination: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(animal.lastVaccinationDate ?: System.currentTimeMillis()))}",
            "📊 Age: ${animal.age} months",
            "⚖️ Weight: ${animal.weight} kg",
            "🤰 Pregnancy: ${animal.pregnancyStatus}",
            "📈 Activity Level: ${(70 + Random().nextInt(20))}%",
            "❤️ Heart Rate: ${(60 + Random().nextInt(30))} bpm",
            "🌡️ Temperature: ${(38.0 + Random().nextDouble() * 1.5).toFloat()}°C"
        )

        val reportText = reports.joinToString("\n")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Health Report - ${animal.name}")
            .setMessage(reportText)
            .setPositiveButton("Download PDF") { _, _ ->
                Toast.makeText(requireContext(), "PDF download started", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Share") { _, _ ->
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, reportText)
                    type = "text/plain"
                }
                startActivity(android.content.Intent.createChooser(shareIntent, "Share Report"))
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showMovementHistory(animal: Animal) {
        val history = viewModel.getMovementHistory(animal.id)

        if (history.isEmpty()) {
            Toast.makeText(requireContext(), "No movement history available", Toast.LENGTH_SHORT).show()
            return
        }

        val items = history.map {
            val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(it.date))
            "$date: ${String.format("%.2f", it.totalDistanceKm)} km, Avg speed: ${String.format("%.1f", it.averageSpeedKmh)} km/h"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Movement History - ${animal.name}")
            .setItems(items) { _, _ -> }
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAddToZoneDialog(animal: Animal) {
        val zones = viewModel.safeZones.value ?: emptyList()
        val zoneNames = zones.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add to Safe Zone")
            .setItems(zoneNames) { _, which ->
                val selectedZone = zones[which]
                viewModel.addAnimalToZone(animal.id, selectedZone.id)
                Snackbar.make(binding.root, "${animal.name} added to ${selectedZone.name}", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupTypeSpinner(binding: DialogEditAnimalBinding) {
        val types = AnimalType.values().map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter
    }

    private fun setupHealthStatusSpinner(binding: DialogEditAnimalBinding) {
        val statuses = HealthStatus.values().map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerHealthStatus.adapter = adapter
    }

    private fun setupPregnancySpinner(binding: DialogEditAnimalBinding) {
        val statuses = PregnancyStatus.values().map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPregnancy.adapter = adapter
    }

    private fun getSelectedAnimalType(binding: DialogEditAnimalBinding): AnimalType {
        return AnimalType.values()[binding.spinnerType.selectedItemPosition]
    }

    private fun getSelectedHealthStatus(binding: DialogEditAnimalBinding): HealthStatus {
        return HealthStatus.values()[binding.spinnerHealthStatus.selectedItemPosition]
    }

    private fun getSelectedPregnancyStatus(binding: DialogEditAnimalBinding): PregnancyStatus {
        return PregnancyStatus.values()[binding.spinnerPregnancy.selectedItemPosition]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}