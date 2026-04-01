package com.example.ecogeoguard.ui.government

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlin.random.Random
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ecogeoguard.R
import com.example.ecogeoguard.databinding.FragmentResourceMapBinding
import com.example.ecogeoguard.viewmodel.GovernmentViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class ResourceMapFragment : Fragment() {

    private var _binding: FragmentResourceMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GovernmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourceMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupMapMarkers()
        setupFilters()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupMapMarkers() {
        // Village markers (simulated)
        val villages = listOf(
            Triple("Himalayan Village", 28.6129, 77.2295),
            Triple("River Valley", 28.6150, 77.2320),
            Triple("Hill Station", 28.6100, 77.2270),
            Triple("Mountain Base", 28.6200, 77.2350),
            Triple("Forest Edge", 28.6050, 77.2400)
        )

        villages.forEach { (name, lat, lng) ->
            addVillageMarker(name, lat, lng)
        }

        // Shelter markers
        val shelters = listOf(
            Triple("District Shelter A", 28.6250, 77.2400),
            Triple("Community Center", 28.6400, 77.2300),
            Triple("High Ground Shelter", 28.6200, 77.2500)
        )

        shelters.forEach { (name, lat, lng) ->
            addShelterMarker(name, lat, lng)
        }
    }

    private fun addVillageMarker(name: String, lat: Double, lng: Double) {
        val markerView = layoutInflater.inflate(R.layout.view_map_marker, binding.mapContainer, false)
        val tvTitle = markerView.findViewById<TextView>(R.id.tvMarkerTitle)
        val tvSubtitle = markerView.findViewById<TextView>(R.id.tvMarkerSubtitle)

        tvTitle.text = name
        tvSubtitle.text = "Population: ${Random.nextInt(1000, 5000)}"

        // Position marker (simulated)
        markerView.x = ((lng - 77.22) * 1000).toFloat()
        markerView.y = ((lat - 28.60) * 2000).toFloat()

        markerView.setOnClickListener {
            showVillageDetails(name, lat, lng)
        }

        binding.mapContainer.addView(markerView)
    }

    private fun addShelterMarker(name: String, lat: Double, lng: Double) {
        val markerView = layoutInflater.inflate(R.layout.view_map_marker_shelter, binding.mapContainer, false)
        val tvTitle = markerView.findViewById<TextView>(R.id.tvMarkerTitle)

        tvTitle.text = name

        markerView.x = ((lng - 77.22) * 1000).toFloat()
        markerView.y = ((lat - 28.60) * 2000).toFloat()

        markerView.setOnClickListener {
            showShelterDetails(name)
        }

        binding.mapContainer.addView(markerView)
    }

    private fun setupFilters() {
        binding.chipAll.setOnClickListener {
            updateChipSelection(binding.chipAll)
            showAllMarkers()
        }

        binding.chipVillages.setOnClickListener {
            updateChipSelection(binding.chipVillages)
            showVillagesOnly()
        }

        binding.chipShelters.setOnClickListener {
            updateChipSelection(binding.chipShelters)
            showSheltersOnly()
        }

        binding.chipRiskZones.setOnClickListener {
            updateChipSelection(binding.chipRiskZones)
            showRiskZones()
        }
    }

    private fun updateChipSelection(selectedChip: Chip) {
        val chips = listOf(binding.chipAll, binding.chipVillages, binding.chipShelters, binding.chipRiskZones)
        chips.forEach { chip ->
            chip.isChecked = chip == selectedChip
        }
    }

    private fun showAllMarkers() {
        for (i in 0 until binding.mapContainer.childCount) {
            binding.mapContainer.getChildAt(i).visibility = View.VISIBLE
        }
    }

    private fun showVillagesOnly() {
        for (i in 0 until binding.mapContainer.childCount) {
            val view = binding.mapContainer.getChildAt(i)
            view.visibility = if (view.tag == "village") View.VISIBLE else View.GONE
        }
    }

    private fun showSheltersOnly() {
        for (i in 0 until binding.mapContainer.childCount) {
            val view = binding.mapContainer.getChildAt(i)
            view.visibility = if (view.tag == "shelter") View.VISIBLE else View.GONE
        }
    }

    private fun showRiskZones() {
        Toast.makeText(requireContext(), "Showing risk zones overlay", Toast.LENGTH_SHORT).show()
    }

    private fun showVillageDetails(name: String, lat: Double, lng: Double) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(name)
            .setMessage("""
                📍 Location: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}
                👥 Population: ${Random.nextInt(1000, 5000)}
                🚨 Risk Level: ${listOf("LOW", "MODERATE", "HIGH", "CRITICAL").random()}
                📡 Sensors: ${Random.nextInt(5, 20)}
                💰 Budget Allocated: ₹${String.format("%.2f", Random.nextDouble(10.0, 200.0))} L
            """.trimIndent())
            .setPositiveButton("View Analytics") { _, _ ->
                Toast.makeText(requireContext(), "Opening $name analytics", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Directions") { _, _ ->
                val uri = "geo:$lat,$lng?q=$lat,$lng"
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                startActivity(intent)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showShelterDetails(name: String) {
        val capacity = Random.nextInt(200, 1000)
        val currentOccupancy = Random.nextInt(0, capacity)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(name)
            .setMessage("""
                🏠 Shelter Information
                ====================
                
                Capacity: $capacity
                Current Occupancy: $currentOccupancy
                Available: ${capacity - currentOccupancy}
                
                🏥 Medical Facilities: ${if (Random.nextBoolean()) "Available" else "Limited"}
                🍽️ Food Supply: ${Random.nextInt(500, 5000)} meals
                💧 Water Supply: ${Random.nextInt(1000, 10000)} liters
                
                📞 Contact: +91 98765 43210
            """.trimIndent())
            .setPositiveButton("Navigate") { _, _ ->
                Toast.makeText(requireContext(), "Opening navigation", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.isVisible = isLoading
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}