package com.example.ecogeoguard.ui.disaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.EvacuationRoute
import com.example.ecogeoguard.data.model.RouteStatusLevel
import com.example.ecogeoguard.databinding.FragmentEvacuationMapBinding
import com.example.ecogeoguard.viewmodel.DisasterViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EvacuationMapFragment : Fragment() {

    private var _binding: FragmentEvacuationMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DisasterViewModel by viewModels()
    private lateinit var routesAdapter: ArrayAdapter<String>
    private val routeNames = mutableListOf<String>()
    private val routeMap = mutableMapOf<String, EvacuationRoute>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvacuationMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSpinner()
        setupClickListeners()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_satellite -> {
                    Toast.makeText(requireContext(), "Satellite view", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_terrain -> {
                    Toast.makeText(requireContext(), "Terrain view", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_traffic -> {
                    Toast.makeText(requireContext(), "Traffic layer", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSpinner() {
        routesAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, routeNames)
        routesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRoutes.adapter = routesAdapter

        binding.spinnerRoutes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val routeName = routeNames[position]
                val route = routeMap[routeName]
                route?.let { displayRouteDetails(it) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                hideRouteDetails()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnRefreshRoutes.setOnClickListener {
            viewModel.refreshManually()
            animateRefresh()
        }

        binding.btnZoomIn.setOnClickListener {
            Toast.makeText(requireContext(), "Zoom In", Toast.LENGTH_SHORT).show()
            // In real app: map.animateCamera(CameraUpdateFactory.zoomIn())
        }

        binding.btnZoomOut.setOnClickListener {
            Toast.makeText(requireContext(), "Zoom Out", Toast.LENGTH_SHORT).show()
            // In real app: map.animateCamera(CameraUpdateFactory.zoomOut())
        }

        binding.btnMyLocation.setOnClickListener {
            Toast.makeText(requireContext(), "Centering on your location", Toast.LENGTH_SHORT).show()
            // In real app: map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
        }

        binding.btnLayers.setOnClickListener {
            showLayerOptions()
        }

        binding.btnNavigate.setOnClickListener {
            val route = getSelectedRoute()
            route?.let {
                startNavigation(it)
            }
        }

        // Map marker clicks (simulated)
        binding.mapMarker1.setOnClickListener {
            showMarkerInfo("⚠️ Danger Zone", "Himalayan Village - Landslide risk area")
        }

        binding.mapMarker2.setOnClickListener {
            showMarkerInfo("🏠 Shelter", "District Shelter A - 245/500 capacity")
        }

        binding.mapMarker3.setOnClickListener {
            showMarkerInfo("⚠️ Warning Zone", "River Valley - Flood watch active")
        }

        binding.mapMarker4.setOnClickListener {
            showMarkerInfo("🏠 Shelter", "Community Center - 180/300 capacity")
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.evacuationRoutes.collect { routes ->
                updateRouteList(routes)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.isVisible = isLoading
            }
        }
    }

    private fun updateRouteList(routes: List<EvacuationRoute>) {
        routeNames.clear()
        routeMap.clear()

        routes.forEach { route ->
            val displayName = "${route.name} (${route.fromVillage} → ${route.toShelter})"
            routeNames.add(displayName)
            routeMap[displayName] = route
        }

        routesAdapter.notifyDataSetChanged()

        if (routeNames.isNotEmpty()) {
            binding.spinnerRoutes.setSelection(0)
        } else {
            hideRouteDetails()
            binding.tvNoRoute.visibility = View.VISIBLE
        }
    }

    private fun displayRouteDetails(route: EvacuationRoute) {
        binding.layoutRouteDetails.visibility = View.VISIBLE
        binding.tvNoRoute.visibility = View.GONE

        binding.tvRouteFrom.text = route.fromVillage
        binding.tvRouteTo.text = route.toShelter
        binding.tvRouteDistance.text = String.format("%.1f km", route.distanceKm)
        binding.tvRouteTime.text = "${route.estimatedTimeMin} min"

        val statusColor = when (route.status) {
            RouteStatusLevel.OPEN -> R.color.success
            RouteStatusLevel.CLOSED -> R.color.dangerRed
            RouteStatusLevel.UNDER_MAINTENANCE -> R.color.warningOrange
            RouteStatusLevel.CONGESTED -> R.color.warning
            RouteStatusLevel.ALTERNATIVE -> R.color.info
        }
        binding.tvRouteStatus.text = route.status.name
        binding.tvRouteStatus.setTextColor(ContextCompat.getColor(requireContext(), statusColor))

        binding.tvRouteCapacity.text = "${route.currentOccupancy}/${route.capacity}"

        // Highlight route on map (simulated)
        highlightRouteOnMap(route)
    }

    private fun hideRouteDetails() {
        binding.layoutRouteDetails.visibility = View.GONE
        binding.tvNoRoute.visibility = View.VISIBLE
    }

    private fun getSelectedRoute(): EvacuationRoute? {
        val position = binding.spinnerRoutes.selectedItemPosition
        return if (position >= 0 && position < routeNames.size) {
            routeMap[routeNames[position]]
        } else null
    }

    private fun highlightRouteOnMap(route: EvacuationRoute) {
        // In a real app with Google Maps, you would:
        // 1. Clear existing polylines
        // 2. Draw a new polyline for the selected route
        // 3. Set color based on status

        // For now, just show a toast
        Toast.makeText(requireContext(), "Highlighting route: ${route.name}", Toast.LENGTH_SHORT).show()
    }

    private fun startNavigation(route: EvacuationRoute) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Start Navigation")
            .setMessage("Open navigation to ${route.toShelter}?")
            .setPositiveButton("Open Maps") { _, _ ->
                // In real app: Open Google Maps with directions
                val uri = "google.navigation:q=${route.toShelter}"
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                startActivity(intent)
            }
            .setNeutralButton("Share Route") { _, _ ->
                shareRoute(route)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareRoute(route: EvacuationRoute) {
        val shareText = """
            🚌 Evacuation Route: ${route.name}
            From: ${route.fromVillage}
            To: ${route.toShelter}
            Distance: ${route.distanceKm} km
            Time: ${route.estimatedTimeMin} min
            Status: ${route.status}
            Capacity: ${route.currentOccupancy}/${route.capacity}
        """.trimIndent()

        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Route"))
    }

    private fun showMarkerInfo(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("View Details") { _, _ ->
                Toast.makeText(requireContext(), "Opening details...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showLayerOptions() {
        val layers = arrayOf("Normal", "Satellite", "Terrain", "Traffic")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Map Layers")
            .setItems(layers) { _, which ->
                Toast.makeText(requireContext(), "${layers[which]} view selected", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun animateRefresh() {
        binding.btnRefreshRoutes.animate()
            .rotation(360f)
            .setDuration(500)
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}