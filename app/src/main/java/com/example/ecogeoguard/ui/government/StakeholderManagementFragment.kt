package com.example.ecogeoguard.ui.government

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.UserRole
import com.example.ecogeoguard.databinding.FragmentStakeholderManagementBinding
import com.example.ecogeoguard.ui.government.adapter.StakeholderAdapter
import com.example.ecogeoguard.viewmodel.GovernmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class StakeholderManagementFragment : Fragment() {

    private var _binding: FragmentStakeholderManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GovernmentViewModel by viewModels()
    private lateinit var stakeholderAdapter: StakeholderAdapter

    private var currentRole: UserRole? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStakeholderManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        setupSearch()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_export -> {
                    exportStakeholderReport()
                    true
                }
                R.id.action_add -> {
                    showAddStakeholderDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        stakeholderAdapter = StakeholderAdapter(
            onItemClick = { stakeholder ->
                showStakeholderDetails(stakeholder)
            },
            onMessageClick = { stakeholder ->
                sendMessage(stakeholder)
            },
            onPerformanceClick = { stakeholder ->
                showPerformanceDetails(stakeholder)
            }
        )

        binding.rvStakeholders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stakeholderAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentRole = when (tab?.position) {
                    0 -> null
                    1 -> UserRole.ADMIN
                    2 -> UserRole.FARMER
                    3 -> UserRole.LIVESTOCK_OWNER
                    4 -> UserRole.DISASTER_TEAM
                    5 -> UserRole.GOVERNMENT
                    else -> null
                }
                filterByRole()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filterBySearch()
            }
        })
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.stakeholders.collect { stakeholders ->
                updateStats(stakeholders)
                filterByRole()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.isVisible = isLoading
            }
        }
    }

    private fun updateStats(stakeholders: List<com.example.ecogeoguard.data.model.Stakeholder>) {
        binding.tvTotalStakeholders.text = stakeholders.size.toString()

        val activeCount = stakeholders.count {
            System.currentTimeMillis() - it.lastActive < 7 * 24 * 60 * 60 * 1000
        }
        binding.tvActiveCount.text = activeCount.toString()

        val avgPerformance = stakeholders.map { it.performanceScore }.average().toFloat()
        binding.tvAvgPerformance.text = "${String.format("%.1f", avgPerformance)}%"
        binding.progressAvgPerformance.progress = avgPerformance.toInt()

        val progressColor = when {
            avgPerformance >= 70 -> R.color.success
            avgPerformance >= 50 -> R.color.warning
            else -> R.color.dangerRed
        }
        binding.progressAvgPerformance.progressTintList =
            ContextCompat.getColorStateList(requireContext(), progressColor)
    }

    private fun filterByRole() {
        val stakeholders = viewModel.stakeholders.value ?: emptyList()

        val filtered = if (currentRole == null) {
            stakeholders
        } else {
            stakeholders.filter { it.role == currentRole }
        }

        stakeholderAdapter.submitList(filtered)
        binding.tvStakeholderCount.text = "${filtered.size} stakeholders"
    }

    private fun filterBySearch() {
        val stakeholders = viewModel.stakeholders.value ?: emptyList()
        val searchQuery = binding.etSearch.text.toString().lowercase()

        val filtered = if (searchQuery.isEmpty()) {
            if (currentRole == null) stakeholders else stakeholders.filter { it.role == currentRole }
        } else {
            (if (currentRole == null) stakeholders else stakeholders.filter { it.role == currentRole })
                .filter {
                    it.name.lowercase().contains(searchQuery) ||
                            it.contactNumber.contains(searchQuery) ||
                            it.email.lowercase().contains(searchQuery)
                }
        }

        stakeholderAdapter.submitList(filtered)
        binding.tvStakeholderCount.text = "${filtered.size} stakeholders"
    }

    private fun showStakeholderDetails(stakeholder: com.example.ecogeoguard.data.model.Stakeholder) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val performanceColor = when {
            stakeholder.performanceScore >= 70 -> R.color.success
            stakeholder.performanceScore >= 50 -> R.color.warning
            else -> R.color.dangerRed
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(stakeholder.name)
            .setMessage("""
                📋 Stakeholder Details
                =====================
                
                👤 Role: ${stakeholder.role.title}
                📍 Village: ${stakeholder.villageId}
                📞 Contact: ${stakeholder.contactNumber}
                📧 Email: ${stakeholder.email}
                
                📊 Performance Metrics:
                • Score: ${String.format("%.1f", stakeholder.performanceScore)}%
                • Tasks Completed: ${stakeholder.tasksCompleted}
                • Pending Tasks: ${stakeholder.pendingTasks}
                • Response Time: ${stakeholder.responseTime} minutes
                
                🕐 Last Active: ${dateFormat.format(Date(stakeholder.lastActive))}
            """.trimIndent())
            .setPositiveButton("Send Message") { _, _ ->
                sendMessage(stakeholder)
            }
            .setNeutralButton("Performance Report") { _, _ ->
                showPerformanceDetails(stakeholder)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showPerformanceDetails(stakeholder: com.example.ecogeoguard.data.model.Stakeholder) {
        val performanceTrend = listOf(
            "Jan: ${String.format("%.1f", stakeholder.performanceScore - 10)}%",
            "Feb: ${String.format("%.1f", stakeholder.performanceScore - 5)}%",
            "Mar: ${String.format("%.1f", stakeholder.performanceScore)}%"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Performance Report - ${stakeholder.name}")
            .setMessage("""
                📊 Performance Analysis
                ======================
                
                Current Score: ${String.format("%.1f", stakeholder.performanceScore)}%
                Tasks Completed: ${stakeholder.tasksCompleted}
                Pending Tasks: ${stakeholder.pendingTasks}
                Response Time: ${stakeholder.responseTime} minutes
                
                📈 Trend:
                • ${performanceTrend[0]}
                • ${performanceTrend[1]}
                • ${performanceTrend[2]} (Current)
                
                🎯 Recommendations:
                ${getRecommendations(stakeholder)}
            """.trimIndent())
            .setPositiveButton("Export Report") { _, _ ->
                exportPerformanceReport(stakeholder)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun getRecommendations(stakeholder: com.example.ecogeoguard.data.model.Stakeholder): String {
        return when {
            stakeholder.performanceScore < 50 -> "• Needs immediate training\n• Schedule performance review\n• Assign mentor"
            stakeholder.performanceScore < 70 -> "• Additional training recommended\n• Set improvement goals\n• Monthly check-ins"
            else -> "• Excellent performance\n• Consider for leadership role\n• Recognition award"
        }
    }

    private fun sendMessage(stakeholder: com.example.ecogeoguard.data.model.Stakeholder) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Type your message..."
            setPadding(32, 32, 32, 32)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Send Message to ${stakeholder.name}")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val message = input.text.toString()
                if (message.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Message sent to ${stakeholder.name}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddStakeholderDialog() {
        val roles = arrayOf("Admin", "Farmer", "Livestock Owner", "Disaster Team", "Government Official")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Stakeholder")
            .setItems(roles) { _, which ->
                val role = when (which) {
                    0 -> UserRole.ADMIN
                    1 -> UserRole.FARMER
                    2 -> UserRole.LIVESTOCK_OWNER
                    3 -> UserRole.DISASTER_TEAM
                    else -> UserRole.GOVERNMENT
                }
                showAddStakeholderForm(role)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddStakeholderForm(role: UserRole) {
        // In real app, show form with name, contact, email fields
        Toast.makeText(requireContext(), "Add ${role.title} form", Toast.LENGTH_SHORT).show()
    }

    private fun exportStakeholderReport() {
        val stakeholders = stakeholderAdapter.currentList
        val report = buildString {
            append("📋 STAKEHOLDER MANAGEMENT REPORT\n")
            append("================================\n")
            append("Generated: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
            append("Total Stakeholders: ${stakeholders.size}\n\n")

            append("STAKEHOLDER DETAILS:\n")
            stakeholders.forEach { stakeholder ->
                append("\n• ${stakeholder.name}\n")
                append("  Role: ${stakeholder.role.title}\n")
                append("  Performance: ${String.format("%.1f", stakeholder.performanceScore)}%\n")
                append("  Tasks: ${stakeholder.tasksCompleted}/${stakeholder.tasksCompleted + stakeholder.pendingTasks}\n")
                append("  Contact: ${stakeholder.contactNumber}\n")
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
            .setNegativeButton("Close", null)
            .show()
    }

    private fun exportPerformanceReport(stakeholder: com.example.ecogeoguard.data.model.Stakeholder) {
        val report = """
            PERFORMANCE REPORT
            =================
            Name: ${stakeholder.name}
            Role: ${stakeholder.role.title}
            Performance Score: ${String.format("%.1f", stakeholder.performanceScore)}%
            Tasks Completed: ${stakeholder.tasksCompleted}
            Pending Tasks: ${stakeholder.pendingTasks}
            Response Time: ${stakeholder.responseTime} minutes
            Last Active: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(stakeholder.lastActive))}
        """.trimIndent()

        Toast.makeText(requireContext(), "Report exported", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}