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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.SafeZone
import com.example.ecogeoguard.data.model.ZoneType
import com.example.ecogeoguard.databinding.FragmentSafeZoneBinding
import com.example.ecogeoguard.databinding.DialogAddZoneBinding
import com.example.ecogeoguard.databinding.ItemZoneBinding
import com.example.ecogeoguard.viewmodel.LivestockViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class SafeZoneFragment : Fragment() {

    private var _binding: FragmentSafeZoneBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LivestockViewModel by viewModels()
    private lateinit var zoneAdapter: ZoneAdapter

    // Cache for animal counts per zone
    private val zoneAnimalCounts = mutableMapOf<String, Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSafeZoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        zoneAdapter = ZoneAdapter(
            onItemClick = { zone -> showZoneDetails(zone) },
            onEditClick = { zone -> showEditZoneDialog(zone) },
            onDeleteClick = { zone -> showDeleteConfirmation(zone) },
            onToggleActive = { zone -> toggleZoneActive(zone) }
        )

        binding.rvZones.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = zoneAdapter
        }
    }

    private fun setupObservers() {
        // Observe safe zones
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.safeZones.collect { zones ->
                zoneAdapter.submitList(zones)
                binding.tvZoneCount.text = "${zones.size} zones"
                // Recalculate animal counts when zones change
                calculateAnimalCounts()
            }
        }

        // Observe locations and recalculate counts when they change
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentLocations.collect { locations ->
                calculateAnimalCounts()
                zoneAdapter.notifyDataSetChanged()
            }
        }

        // Observe animals (in case new animals are added)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.animals.collect { animals ->
                calculateAnimalCounts()
                zoneAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun calculateAnimalCounts() {
        val locations = viewModel.currentLocations.value ?: return
        val zones = viewModel.safeZones.value ?: return

        zoneAnimalCounts.clear()

        zones.forEach { zone ->
            val count = locations.count { (_, loc) ->
                zone.isPointInside(loc.latitude, loc.longitude)
            }
            zoneAnimalCounts[zone.id] = count
        }
    }

    private fun setupClickListeners() {
        binding.btnAddZone.setOnClickListener {
            showAddZoneDialog()
        }

        binding.btnRefresh.setOnClickListener {
            viewModel.refreshManually()
        }
    }

    private fun showAddZoneDialog() {
        val dialogBinding = DialogAddZoneBinding.inflate(layoutInflater)

        // Setup zone type spinner
        setupZoneTypeSpinner(dialogBinding)

        // Setup time restriction checkbox
        dialogBinding.cbRestrictedHours.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.layoutTimeRange.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Setup time pickers
        dialogBinding.btnStartTime.setOnClickListener {
            showTimePicker { hour, minute ->
                dialogBinding.tvStartTime.text = String.format("%02d:%02d", hour, minute)
            }
        }

        dialogBinding.btnEndTime.setOnClickListener {
            showTimePicker { hour, minute ->
                dialogBinding.tvEndTime.text = String.format("%02d:%02d", hour, minute)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Safe Zone")
            .setView(dialogBinding.root)
            .setPositiveButton("Create") { _, _ ->
                createZone(dialogBinding)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showEditZoneDialog(zone: SafeZone) {
        val dialogBinding = DialogAddZoneBinding.inflate(layoutInflater)

        setupZoneTypeSpinner(dialogBinding)

        // Load existing values
        dialogBinding.etZoneName.setText(zone.name)
        dialogBinding.etRadius.setText(zone.radiusMeters.toString())
        dialogBinding.cbAlertOnExit.isChecked = zone.alertOnExit
        dialogBinding.cbAlertOnEntry.isChecked = zone.alertOnEntry

        // Set zone type selection
        val typeIndex = ZoneType.values().indexOf(zone.type)
        dialogBinding.spinnerZoneType.setSelection(typeIndex)

        dialogBinding.cbRestrictedHours.isChecked = zone.restrictedHours != null
        if (zone.restrictedHours != null) {
            dialogBinding.layoutTimeRange.visibility = View.VISIBLE
            dialogBinding.tvStartTime.text = String.format("%02d:%02d",
                zone.restrictedHours[0].startHour,
                zone.restrictedHours[0].startMinute)
            dialogBinding.tvEndTime.text = String.format("%02d:%02d",
                zone.restrictedHours[0].endHour,
                zone.restrictedHours[0].endMinute)
        }

        dialogBinding.cbRestrictedHours.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.layoutTimeRange.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        dialogBinding.btnStartTime.setOnClickListener {
            showTimePicker { hour, minute ->
                dialogBinding.tvStartTime.text = String.format("%02d:%02d", hour, minute)
            }
        }

        dialogBinding.btnEndTime.setOnClickListener {
            showTimePicker { hour, minute ->
                dialogBinding.tvEndTime.text = String.format("%02d:%02d", hour, minute)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Safe Zone")
            .setView(dialogBinding.root)
            .setPositiveButton("Update") { _, _ ->
                updateZone(zone.id, dialogBinding)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showDeleteConfirmation(zone: SafeZone) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Safe Zone")
            .setMessage("Are you sure you want to delete '${zone.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSafeZone(zone.id)
                Snackbar.make(binding.root, "Zone deleted", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showZoneDetails(zone: SafeZone) {
        val locations = viewModel.currentLocations.value ?: emptyMap()
        val animals = viewModel.animals.value ?: emptyList()

        val animalsInZone = locations.filter { (_, loc) ->
            zone.isPointInside(loc.latitude, loc.longitude)
        }.mapNotNull { (animalId, loc) ->
            animals.find { it.id == animalId }?.let { animal ->
                "${animal.name} (${String.format("%.2f", loc.latitude)}, ${String.format("%.2f", loc.longitude)})"
            }
        }

        val stats = buildString {
            append("📍 Type: ${zone.type.name}\n")
            append("📏 Radius: ${zone.radiusMeters} meters\n")
            append("📍 Center: ${String.format("%.4f", zone.centerLatitude)}, ${String.format("%.4f", zone.centerLongitude)}\n")
            append("🚪 Alert on exit: ${if (zone.alertOnExit) "Yes" else "No"}\n")
            append("🚶 Alert on entry: ${if (zone.alertOnEntry) "Yes" else "No"}\n")
            if (zone.restrictedHours != null) {
                append("⏰ Restricted: ${zone.restrictedHours[0].startHour}:${String.format("%02d", zone.restrictedHours[0].startMinute)} - ${zone.restrictedHours[0].endHour}:${String.format("%02d", zone.restrictedHours[0].endMinute)}\n")
            }
            append("\n🐄 Animals in zone: ${animalsInZone.size}\n")
            if (animalsInZone.isNotEmpty()) {
                append("\n" + animalsInZone.joinToString("\n"))
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(zone.name)
            .setMessage(stats)
            .setPositiveButton("OK", null)
            .setNeutralButton("Edit") { _, _ ->
                showEditZoneDialog(zone)
            }
            .show()
    }

    private fun toggleZoneActive(zone: SafeZone) {
        val updatedZone = zone.copy(isActive = !zone.isActive)
        viewModel.updateSafeZone(updatedZone)

        val message = if (updatedZone.isActive) "Zone activated" else "Zone deactivated"
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun createZone(binding: DialogAddZoneBinding) {
        val name = binding.etZoneName.text.toString()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter zone name", Toast.LENGTH_SHORT).show()
            return
        }

        val radius = binding.etRadius.text.toString().toFloatOrNull() ?: 100f
        val type = ZoneType.values()[binding.spinnerZoneType.selectedItemPosition]

        val restrictedHours = if (binding.cbRestrictedHours.isChecked) {
            val startTime = binding.tvStartTime.text.toString().split(":")
            val endTime = binding.tvEndTime.text.toString().split(":")

            listOf(
                com.example.ecogeoguard.data.model.TimeRange(
                    startHour = startTime[0].toInt(),
                    startMinute = startTime[1].toInt(),
                    endHour = endTime[0].toInt(),
                    endMinute = endTime[1].toInt()
                )
            )
        } else null

        val zone = SafeZone(
            id = "zone_${System.currentTimeMillis()}",
            name = name,
            type = type,
            centerLatitude = 28.6129, // Default center
            centerLongitude = 77.2295,
            radiusMeters = radius,
            isActive = true,
            alertOnExit = binding.cbAlertOnExit.isChecked,
            alertOnEntry = binding.cbAlertOnEntry.isChecked,
            restrictedHours = restrictedHours,
            createdAt = System.currentTimeMillis()
        )

        viewModel.addSafeZone(zone)
        Snackbar.make(this.binding.root, "Zone created successfully", Snackbar.LENGTH_SHORT).show()
    }

    private fun updateZone(zoneId: String, binding: DialogAddZoneBinding) {
        val name = binding.etZoneName.text.toString()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter zone name", Toast.LENGTH_SHORT).show()
            return
        }

        val radius = binding.etRadius.text.toString().toFloatOrNull() ?: 100f
        val type = ZoneType.values()[binding.spinnerZoneType.selectedItemPosition]

        val restrictedHours = if (binding.cbRestrictedHours.isChecked) {
            val startTime = binding.tvStartTime.text.toString().split(":")
            val endTime = binding.tvEndTime.text.toString().split(":")

            listOf(
                com.example.ecogeoguard.data.model.TimeRange(
                    startHour = startTime[0].toInt(),
                    startMinute = startTime[1].toInt(),
                    endHour = endTime[0].toInt(),
                    endMinute = endTime[1].toInt()
                )
            )
        } else null

        val zone = SafeZone(
            id = zoneId,
            name = name,
            type = type,
            centerLatitude = 28.6129,
            centerLongitude = 77.2295,
            radiusMeters = radius,
            isActive = true,
            alertOnExit = binding.cbAlertOnExit.isChecked,
            alertOnEntry = binding.cbAlertOnEntry.isChecked,
            restrictedHours = restrictedHours,
            createdAt = System.currentTimeMillis()
        )

        viewModel.updateSafeZone(zone)
        Snackbar.make(this.binding.root, "Zone updated successfully", Snackbar.LENGTH_SHORT).show()
    }

    private fun setupZoneTypeSpinner(binding: DialogAddZoneBinding) {
        val types = ZoneType.values().map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerZoneType.adapter = adapter
    }

    private fun showTimePicker(onTimeSet: (Int, Int) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Select Time")
            .build()

        picker.addOnPositiveButtonClickListener {
            onTimeSet(picker.hour, picker.minute)
        }

        picker.show(parentFragmentManager, "time_picker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Zone Adapter Inner Class
    inner class ZoneAdapter(
        private val onItemClick: (SafeZone) -> Unit,
        private val onEditClick: (SafeZone) -> Unit,
        private val onDeleteClick: (SafeZone) -> Unit,
        private val onToggleActive: (SafeZone) -> Unit
    ) : RecyclerView.Adapter<ZoneAdapter.ZoneViewHolder>() {

        private var zones = listOf<SafeZone>()

        fun submitList(newZones: List<SafeZone>) {
            zones = newZones
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
            val binding = ItemZoneBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ZoneViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
            holder.bind(zones[position])
        }

        override fun getItemCount() = zones.size

        inner class ZoneViewHolder(
            private val binding: ItemZoneBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(zone: SafeZone) {
                binding.apply {
                    tvZoneName.text = zone.name
                    tvZoneType.text = zone.type.name
                    tvRadius.text = "${zone.radiusMeters}m"

                    // Set color based on zone type
                    val color = when (zone.type) {
                        ZoneType.DANGER -> R.color.dangerRed
                        ZoneType.QUARANTINE -> R.color.warningOrange
                        ZoneType.RESTRICTED -> R.color.warning
                        ZoneType.GRAZING -> R.color.success
                        ZoneType.RESTING -> R.color.info
                        ZoneType.WATERING -> R.color.info
                        else -> R.color.primaryGreen
                    }
                    cardZone.setCardBackgroundColor(ContextCompat.getColor(root.context, color).withAlpha(50))

                    // Active status
                    ivActiveStatus.setImageResource(
                        if (zone.isActive) R.drawable.ic_online else R.drawable.img_23
                    )

                    // Get fresh locations each time
                    val locations = viewModel.currentLocations.value ?: emptyMap()

                    // Animal count in zone
                    val animalCount = locations.count { (_, loc) ->
                        zone.isPointInside(loc.latitude, loc.longitude)
                    }
                    tvAnimalCount.text = "$animalCount animals"

                    // Click listeners
                    root.setOnClickListener { onItemClick(zone) }
                    btnEdit.setOnClickListener { onEditClick(zone) }
                    btnDelete.setOnClickListener { onDeleteClick(zone) }
                    ivActiveStatus.setOnClickListener { onToggleActive(zone) }
                }
            }
        }
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return (alpha shl 24) or (this and 0x00ffffff)
    }
}