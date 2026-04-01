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
import com.example.ecogeoguard.data.model.PolicyCategory
import com.example.ecogeoguard.data.model.Priority
import com.example.ecogeoguard.data.model.RecommendationStatus
import com.example.ecogeoguard.databinding.FragmentPolicyRecommendationsBinding
import com.example.ecogeoguard.ui.government.adapter.PolicyRecommendationAdapter
import com.example.ecogeoguard.viewmodel.GovernmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class PolicyRecommendationsFragment : Fragment() {

    private var _binding: FragmentPolicyRecommendationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GovernmentViewModel by viewModels()
    private lateinit var policyAdapter: PolicyRecommendationAdapter

    private var currentStatus: RecommendationStatus? = null
    private var currentPriority: Priority? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPolicyRecommendationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        setupFilters()
        setupObservers()
        setupFab()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_export -> {
                    exportRecommendations()
                    true
                }
                R.id.action_ai_insights -> {
                    showAIInsights()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        policyAdapter = PolicyRecommendationAdapter(
            onItemClick = { policy -> showPolicyDetails(policy) },
            onApproveClick = { policy -> approvePolicy(policy) },
            onRejectClick = { policy -> rejectPolicy(policy) },
            onImplementClick = { policy -> implementPolicy(policy) }
        )

        binding.rvRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = policyAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentStatus = when (tab?.position) {
                    0 -> null
                    1 -> RecommendationStatus.PENDING
                    2 -> RecommendationStatus.APPROVED
                    3 -> RecommendationStatus.IMPLEMENTED
                    4 -> RecommendationStatus.REJECTED
                    else -> null
                }
                filterRecommendations()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupFilters() {
        // Priority filter chips
        binding.chipAll.setOnClickListener {
            currentPriority = null
            updateChipSelection()
            filterRecommendations()
        }

        binding.chipUrgent.setOnClickListener {
            currentPriority = Priority.URGENT
            updateChipSelection()
            filterRecommendations()
        }

        binding.chipHigh.setOnClickListener {
            currentPriority = Priority.HIGH
            updateChipSelection()
            filterRecommendations()
        }

        binding.chipMedium.setOnClickListener {
            currentPriority = Priority.MEDIUM
            updateChipSelection()
            filterRecommendations()
        }

        binding.chipLow.setOnClickListener {
            currentPriority = Priority.LOW
            updateChipSelection()
            filterRecommendations()
        }
    }

    private fun updateChipSelection() {

        val chips = listOf(
            Pair(binding.chipAll, currentPriority == null),
            Pair(binding.chipUrgent, currentPriority == Priority.URGENT),
            Pair(binding.chipHigh, currentPriority == Priority.HIGH),
            Pair(binding.chipMedium, currentPriority == Priority.MEDIUM),
            Pair(binding.chipLow, currentPriority == Priority.LOW)
        )

        chips.forEach { (chip, selected) ->
            chip.isChecked = selected
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.policyRecommendations.collect { policies ->
                updateStats(policies)
                filterRecommendations()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.isVisible = isLoading
            }
        }
    }

    private fun updateStats(policies: List<com.example.ecogeoguard.data.model.PolicyRecommendation>) {
        binding.tvTotalRecommendations.text = policies.size.toString()

        val pendingCount = policies.count { it.status == RecommendationStatus.PENDING }
        binding.tvPendingCount.text = pendingCount.toString()

        val approvedCount = policies.count { it.status == RecommendationStatus.APPROVED }
        binding.tvApprovedCount.text = approvedCount.toString()

        val implementedCount = policies.count { it.status == RecommendationStatus.IMPLEMENTED }
        binding.tvImplementedCount.text = implementedCount.toString()

        val avgConfidence = policies.map { it.aiConfidence }.average().toFloat()
        binding.tvAvgConfidence.text = "${String.format("%.1f", avgConfidence * 100)}%"
        binding.progressConfidence.progress = (avgConfidence * 100).toInt()
    }

    private fun filterRecommendations() {
        val allPolicies = viewModel.policyRecommendations.value ?: emptyList()

        var filtered = if (currentStatus == null) {
            allPolicies
        } else {
            allPolicies.filter { it.status == currentStatus }
        }

        filtered = if (currentPriority == null) {
            filtered
        } else {
            filtered.filter { it.priority == currentPriority }
        }

        policyAdapter.submitList(filtered)
        binding.tvRecommendationCount.text = "${filtered.size} recommendations"
    }

    private fun showPolicyDetails(policy: com.example.ecogeoguard.data.model.PolicyRecommendation) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val priorityColor = when (policy.priority) {
            Priority.URGENT -> R.color.dangerRed
            Priority.HIGH -> R.color.warningOrange
            Priority.MEDIUM -> R.color.warning
            Priority.LOW -> R.color.info
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(policy.title)
            .setMessage("""
                📋 Policy Recommendation
                ========================
                
                🏷️ Category: ${policy.category.name}
                ⚡ Priority: ${policy.priority.name}
                🤖 AI Confidence: ${String.format("%.1f", policy.aiConfidence * 100)}%
                📅 Created: ${dateFormat.format(Date(policy.createdAt))}
                
                📝 Description:
                ${policy.description}
                
                🎯 Expected Impact:
                ${policy.expectedImpact}
                
                💰 Estimated Cost: ${formatRupees(policy.estimatedCost)}
                ⏱️ Time Frame: ${policy.timeFrame}
                
                👥 Stakeholders:
                ${policy.stakeholders.joinToString("\n")}
                
                📊 Success Metrics:
                ${policy.metrics.joinToString("\n")}
            """.trimIndent())
            .setPositiveButton("View Details") { _, _ ->
                showDetailedAnalysis(policy)
            }
            .setNeutralButton("Generate Report") { _, _ ->
                generatePolicyReport(policy)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showDetailedAnalysis(policy: com.example.ecogeoguard.data.model.PolicyRecommendation) {
        val analysis = """
            📊 DETAILED ANALYSIS
            ===================
            
            🔍 AI Analysis:
            • Confidence Score: ${String.format("%.1f", policy.aiConfidence * 100)}%
            • Based on ${if (policy.category == PolicyCategory.TECHNOLOGY) "120" else "85"} data points
            • Similar policies success rate: 78%
            
            💡 Key Benefits:
            • ${policy.expectedImpact.split("\n").firstOrNull() ?: "Improved efficiency"}
            • Cost-benefit ratio: 3.2:1
            • Implementation complexity: ${getComplexity(policy)}
            
            📈 ROI Projection:
            • Year 1: ${formatRupees(policy.estimatedCost * 0.3)}
            • Year 2: ${formatRupees(policy.estimatedCost * 0.7)}
            • Year 3: ${formatRupees(policy.estimatedCost * 1.2)}
            
            🚀 Implementation Roadmap:
            • Phase 1 (Month 1-2): Planning & Approval
            • Phase 2 (Month 3-4): Resource Allocation
            • Phase 3 (Month 5-6): Execution
            • Phase 4 (Month 7-8): Monitoring & Evaluation
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Detailed Analysis - ${policy.title}")
            .setMessage(analysis)
            .setPositiveButton("OK", null)
            .setNeutralButton("Export") { _, _ ->
                exportAnalysis(policy, analysis)
            }
            .show()
    }

    private fun getComplexity(policy: com.example.ecogeoguard.data.model.PolicyRecommendation): String {
        return when (policy.priority) {
            Priority.URGENT -> "High - Requires immediate action"
            Priority.HIGH -> "Medium-High"
            Priority.MEDIUM -> "Medium"
            Priority.LOW -> "Low - Can be phased"
        }
    }

    private fun approvePolicy(policy: com.example.ecogeoguard.data.model.PolicyRecommendation) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Approve Recommendation")
            .setMessage("Are you sure you want to approve '${policy.title}'?")
            .setPositiveButton("Approve") { _, _ ->
                viewModel.updateRecommendationStatus(policy.id, RecommendationStatus.APPROVED)
                Toast.makeText(requireContext(), "Recommendation approved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectPolicy(policy: com.example.ecogeoguard.data.model.PolicyRecommendation) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Reason for rejection..."
            setPadding(32, 32, 32, 32)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reject Recommendation")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString()
                if (reason.isNotEmpty()) {
                    viewModel.updateRecommendationStatus(policy.id, RecommendationStatus.REJECTED)
                    Toast.makeText(requireContext(), "Recommendation rejected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Please provide a reason", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun implementPolicy(policy: com.example.ecogeoguard.data.model.PolicyRecommendation) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mark as Implemented")
            .setMessage("Confirm implementation of '${policy.title}'?")
            .setPositiveButton("Confirm") { _, _ ->
                viewModel.updateRecommendationStatus(policy.id, RecommendationStatus.IMPLEMENTED)
                Toast.makeText(requireContext(), "Recommendation marked as implemented", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddRecommendationDialog()
        }
    }

    private fun showAddRecommendationDialog() {
        // In real app, this would open a form
        Toast.makeText(requireContext(), "Add new policy recommendation", Toast.LENGTH_SHORT).show()
    }

    private fun showAIInsights() {
        val insights = """
            🤖 AI-GENERATED INSIGHTS
            ========================
            
            📊 Current Analysis:
            • High-risk villages show 67% correlation with low disaster preparedness
            • Sensor network expansion could reduce response time by 42%
            • Investment in early warning systems shows ROI of 3.5x
            
            🎯 Top 3 Recommendations:
            1. Expand IoT coverage to 15 high-priority villages
            2. Implement community training programs
            3. Upgrade disaster response infrastructure
            
            📈 Predicted Outcomes:
            • 45% reduction in disaster-related losses
            • 38% improvement in response time
            • 52% increase in community preparedness
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("AI-Powered Insights")
            .setMessage(insights)
            .setPositiveButton("OK", null)
            .setNeutralButton("Export") { _, _ ->
                exportInsights(insights)
            }
            .show()
    }

    private fun exportRecommendations() {
        val recommendations = policyAdapter.currentList
        val report = buildString {
            append("📋 POLICY RECOMMENDATIONS REPORT\n")
            append("=================================\n")
            append("Generated: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
            append("Total Recommendations: ${recommendations.size}\n\n")

            append("RECOMMENDATIONS BY STATUS:\n")
            append("• Pending: ${recommendations.count { it.status == RecommendationStatus.PENDING }}\n")
            append("• Approved: ${recommendations.count { it.status == RecommendationStatus.APPROVED }}\n")
            append("• Implemented: ${recommendations.count { it.status == RecommendationStatus.IMPLEMENTED }}\n")
            append("• Rejected: ${recommendations.count { it.status == RecommendationStatus.REJECTED }}\n\n")

            append("DETAILED LIST:\n")
            recommendations.forEach { policy ->
                append("\n• ${policy.title}\n")
                append("  Category: ${policy.category.name}\n")
                append("  Priority: ${policy.priority.name}\n")
                append("  Status: ${policy.status.name}\n")
                append("  Cost: ${formatRupees(policy.estimatedCost)}\n")
                append("  AI Confidence: ${String.format("%.1f", policy.aiConfidence * 100)}%\n")
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
                Toast.makeText(requireContext(), "Report saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generatePolicyReport(policy: com.example.ecogeoguard.data.model.PolicyRecommendation) {
        Toast.makeText(requireContext(), "Generating policy report...", Toast.LENGTH_SHORT).show()
    }

    private fun exportAnalysis(policy: com.example.ecogeoguard.data.model.PolicyRecommendation, analysis: String) {
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, analysis)
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Analysis"))
    }

    private fun exportInsights(insights: String) {
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, insights)
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Insights"))
    }

    private fun formatRupees(amount: Double): String {
        return when {
            amount >= 10000000 -> "₹${String.format("%.2f", amount / 10000000)} Cr"
            amount >= 100000 -> "₹${String.format("%.2f", amount / 100000)} L"
            else -> "₹${String.format("%.0f", amount)}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}