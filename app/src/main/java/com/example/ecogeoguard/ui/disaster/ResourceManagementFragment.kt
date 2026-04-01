package com.example.ecogeoguard.ui.disaster

import android.os.Bundle
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
import com.example.ecogeoguard.data.model.*
import com.example.ecogeoguard.databinding.FragmentResourceManagementBinding
import com.example.ecogeoguard.databinding.DialogNewResourceRequestBinding
import com.example.ecogeoguard.ui.disaster.adapter.ResourceRequestAdapter
import com.example.ecogeoguard.viewmodel.DisasterViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ResourceManagementFragment : Fragment() {

    private var _binding: FragmentResourceManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DisasterViewModel by viewModels()
    private lateinit var requestAdapter: ResourceRequestAdapter

    private var currentTab = 0 // 0: Pending, 1: Approved, 2: Completed

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourceManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupTabLayout()
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
                R.id.action_report -> {
                    generateResourceReport()
                    true
                }
                R.id.action_settings -> {
                    showResourceSettings()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        requestAdapter = ResourceRequestAdapter(
            onItemClick = { request -> showRequestDetails(request) },
            onApproveClick = { request -> approveRequest(request) },
            onRejectClick = { request -> rejectRequest(request) },
            onCompleteClick = { request -> completeRequest(request) }
        )

        binding.rvRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = requestAdapter
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                filterRequests()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupClickListeners() {
        binding.btnNewRequest.setOnClickListener {
            showNewRequestDialog()
        }

        binding.btnAllocate.setOnClickListener {
            showAllocateResourcesDialog()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.resourceRequests.collect { requests ->
                updateStats(requests)
                filterRequests()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.isVisible = isLoading
            }
        }
    }

    private fun updateStats(requests: List<ResourceRequest>) {
        binding.tvTotalRequests.text = requests.size.toString()
        binding.tvPendingRequests.text = requests.count { it.status == RequestStatusType.PENDING }.toString()

        val completedToday = requests.count {
            it.status == RequestStatusType.COMPLETED &&
                    it.timestamp > System.currentTimeMillis() - 24 * 60 * 60 * 1000
        }
        binding.tvCompletedToday.text = completedToday.toString()

        // Update inventory status (mock data)
        updateInventoryStatus()

        // Update personnel status
        updatePersonnelStatus()
    }

    private fun updateInventoryStatus() {
        // Food
        binding.tvFoodCount.text = "2,500 meals"
        binding.tvFoodStatus.apply {
            text = "Good"
            setBackgroundResource(R.drawable.bg_status_green)
        }

        // Water
        binding.tvWaterCount.text = "5,000 L"
        binding.tvWaterStatus.apply {
            text = "Good"
            setBackgroundResource(R.drawable.bg_status_green)
        }

        // Medical Kits
        binding.tvMedicalCount.text = "150 kits"
        binding.tvMedicalStatus.apply {
            text = "Low"
            setBackgroundResource(R.drawable.bg_status_orange)
        }

        // Blankets
        binding.tvBlanketCount.text = "800 units"
        binding.tvBlanketStatus.apply {
            text = "Good"
            setBackgroundResource(R.drawable.bg_status_green)
        }

        // Tents
        binding.tvTentCount.text = "45 units"
        binding.tvTentStatus.apply {
            text = "Critical"
            setBackgroundResource(R.drawable.bg_status_red)
        }

        // Generators
        binding.tvGeneratorCount.text = "8 units"
        binding.tvGeneratorStatus.apply {
            text = "Good"
            setBackgroundResource(R.drawable.bg_status_green)
        }
    }

    private fun updatePersonnelStatus() {
        // Medical Team
        binding.tvMedicalTeam.text = "12/15 available"
        binding.progressMedicalTeam.progress = 12

        // Rescue Team
        binding.tvRescueTeam.text = "20/25 available"
        binding.progressRescueTeam.progress = 20

        // Engineering Team
        binding.tvEngineeringTeam.text = "5/8 available"
        binding.progressEngineeringTeam.progress = 5
        binding.progressEngineeringTeam.progressTintList =
            android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.warning, null)
            )

        // Logistics Team
        binding.tvLogisticsTeam.text = "10/10 available"
        binding.progressLogisticsTeam.progress = 10
    }

    private fun filterRequests() {
        val allRequests = viewModel.resourceRequests.value ?: emptyList()

        val filtered = when (currentTab) {
            0 -> allRequests.filter { it.status == RequestStatusType.PENDING }
            1 -> allRequests.filter { it.status == RequestStatusType.APPROVED || it.status == RequestStatusType.IN_PROGRESS }
            2 -> allRequests.filter { it.status == RequestStatusType.COMPLETED }
            else -> allRequests
        }

        requestAdapter.submitList(filtered)
    }

    private fun showNewRequestDialog() {
        val dialogBinding = DialogNewResourceRequestBinding.inflate(layoutInflater)

        // Setup resource type spinner
        val resourceTypes = ResourceTypeCategory.values().map { it.name }.toTypedArray()
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resourceTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerResourceType.adapter = adapter

        // Setup urgency spinner
        val urgencyLevels = AlertSeverityLevel.values().map { it.name }.toTypedArray()
        val urgencyAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, urgencyLevels)
        urgencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerUrgency.adapter = urgencyAdapter

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Resource Request")
            .setView(dialogBinding.root)
            .setPositiveButton("Submit") { _, _ ->
                createResourceRequest(dialogBinding)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun createResourceRequest(binding: DialogNewResourceRequestBinding) {
        val requesterName = binding.etRequesterName.text.toString()
        if (requesterName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter requester name", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = binding.etQuantity.text.toString().toIntOrNull() ?: 1
        val resourceType = ResourceTypeCategory.values()[binding.spinnerResourceType.selectedItemPosition]
        val urgency = AlertSeverityLevel.values()[binding.spinnerUrgency.selectedItemPosition]

        val request = ResourceRequest(
            id = "req_${System.currentTimeMillis()}",
            requesterId = "user_001",
            requesterName = requesterName,
            requesterRole = binding.etRole.text.toString().ifEmpty { "Field Staff" },
            resourceType = resourceType,
            quantity = quantity,
            urgency = urgency,
            location = LocationData(28.6129, 77.2295, binding.etLocation.text.toString()),
            timestamp = System.currentTimeMillis(),
            status = RequestStatusType.PENDING
        )

        viewModel.addResourceRequest(request)
        Snackbar.make(this.binding.root, "Request submitted", Snackbar.LENGTH_SHORT).show()
    }

    private fun showRequestDetails(request: ResourceRequest) {
        val statusColor = when (request.status) {
            RequestStatusType.PENDING -> R.color.warningOrange
            RequestStatusType.APPROVED -> R.color.success
            RequestStatusType.IN_PROGRESS -> R.color.info
            RequestStatusType.COMPLETED -> R.color.success
            RequestStatusType.REJECTED -> R.color.dangerRed
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Request Details")
            .setMessage("""
                📋 Request ID: ${request.id}
                👤 Requester: ${request.requesterName} (${request.requesterRole})
                📦 Resource: ${request.resourceType.name}
                🔢 Quantity: ${request.quantity}
                ⚠️ Urgency: ${request.urgency}
                📍 Location: ${request.location.address ?: "Unknown"}
                🕐 Time: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(request.timestamp))}
                📊 Status: ${request.status}
                
                ${if (request.assignedTo != null) "👥 Assigned to: ${request.assignedTo}" else ""}
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun approveRequest(request: ResourceRequest) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Approve Request")
            .setMessage("Assign this request to a team member?")
            .setPositiveButton("Assign to Me") { _, _ ->
                viewModel.approveRequest(request.id, "Current User")
                Snackbar.make(binding.root, "Request approved and assigned", Snackbar.LENGTH_SHORT).show()
            }
            .setNeutralButton("Assign Later") { _, _ ->
                viewModel.approveRequest(request.id, null)
                Snackbar.make(binding.root, "Request approved", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectRequest(request: ResourceRequest) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reject Request")
            .setMessage("Are you sure you want to reject this request?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.rejectRequest(request.id)
                Snackbar.make(binding.root, "Request rejected", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun completeRequest(request: ResourceRequest) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Complete Request")
            .setMessage("Mark this request as completed?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.completeRequest(request.id)
                Snackbar.make(binding.root, "Request completed", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showAllocateResourcesDialog() {
        val villages = arrayOf("Himalayan Village", "River Valley", "Hill Station", "Mountain Base")
        val resources = arrayOf("Food", "Water", "Medical Kits", "Blankets", "Tents", "Generators")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Allocate Resources")
            .setMessage("Select village and resource type")
            .setPositiveButton("Allocate") { _, _ ->
                Toast.makeText(requireContext(), "Resources allocated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateResourceReport() {
        val requests = viewModel.resourceRequests.value ?: emptyList()

        val report = buildString {
            append("📦 RESOURCE MANAGEMENT REPORT\n")
            append("=============================\n")
            append("Generated: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}\n\n")

            append("INVENTORY STATUS:\n")
            append("• Food: 2,500 meals (Good)\n")
            append("• Water: 5,000 L (Good)\n")
            append("• Medical Kits: 150 (Low)\n")
            append("• Blankets: 800 (Good)\n")
            append("• Tents: 45 (Critical)\n")
            append("• Generators: 8 (Good)\n\n")

            append("PERSONNEL STATUS:\n")
            append("• Medical Team: 12/15 available\n")
            append("• Rescue Team: 20/25 available\n")
            append("• Engineering: 5/8 available\n")
            append("• Logistics: 10/10 available\n\n")

            append("REQUESTS SUMMARY:\n")
            append("• Total: ${requests.size}\n")
            append("• Pending: ${requests.count { it.status == RequestStatusType.PENDING }}\n")
            append("• Approved: ${requests.count { it.status == RequestStatusType.APPROVED }}\n")
            append("• In Progress: ${requests.count { it.status == RequestStatusType.IN_PROGRESS }}\n")
            append("• Completed: ${requests.count { it.status == RequestStatusType.COMPLETED }}\n")
            append("• Rejected: ${requests.count { it.status == RequestStatusType.REJECTED }}\n")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Resource Report")
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

    private fun showResourceSettings() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Resource Settings")
            .setItems(arrayOf(
                "Low Stock Alerts",
                "Auto-Reorder Levels",
                "Supplier Contacts",
                "Delivery Routes"
            )) { _, which ->
                when (which) {
                    0 -> Toast.makeText(requireContext(), "Low stock alerts", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(requireContext(), "Auto-reorder levels", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(requireContext(), "Supplier contacts", Toast.LENGTH_SHORT).show()
                    3 -> Toast.makeText(requireContext(), "Delivery routes", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}