package com.example.ecogeoguard.ui.government

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.BudgetCategory
import com.example.ecogeoguard.databinding.FragmentBudgetTrackingBinding
import com.example.ecogeoguard.ui.government.adapter.BudgetAllocationAdapter
import com.example.ecogeoguard.viewmodel.GovernmentViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class BudgetTrackingFragment : Fragment() {

    private var _binding: FragmentBudgetTrackingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GovernmentViewModel by viewModels()
    private lateinit var budgetAdapter: BudgetAllocationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        budgetAdapter = BudgetAllocationAdapter { budget ->
            Toast.makeText(requireContext(), "Viewing ${budget.schemeName}", Toast.LENGTH_SHORT).show()
        }

        binding.rvBudgets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = budgetAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val category = when (tab?.position) {
                    0 -> null
                    1 -> BudgetCategory.AGRICULTURE
                    2 -> BudgetCategory.LIVESTOCK
                    3 -> BudgetCategory.DISASTER_RELIEF
                    4 -> BudgetCategory.INFRASTRUCTURE
                    else -> null
                }
                filterByCategory(category)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.budgetAllocations.collect { budgets ->
                budgetAdapter.submitList(budgets)
                updateSummary(budgets)
            }
        }
    }

    private fun updateSummary(budgets: List<com.example.ecogeoguard.data.model.BudgetAllocation>) {
        val totalAllocated = budgets.sumOf { it.allocatedAmount }
        val totalUtilized = budgets.sumOf { it.utilizedAmount }
        val utilizationRate = if (totalAllocated > 0) (totalUtilized / totalAllocated * 100).toFloat() else 0f

        val formatter = NumberFormat.getCurrencyInstance(Locale.US)

        binding.tvTotalAllocated.text = formatRupees(totalAllocated)
        binding.tvTotalUtilized.text = formatRupees(totalUtilized)
        binding.tvUtilizationRate.text = "${String.format("%.1f", utilizationRate)}%"
        binding.progressBudget.progress = utilizationRate.toInt()

        val progressColor = when {
            utilizationRate < 50 -> R.color.success
            utilizationRate < 80 -> R.color.warning
            else -> R.color.dangerRed
        }
        binding.progressBudget.progressTintList = ContextCompat.getColorStateList(requireContext(), progressColor)
    }

    private fun filterByCategory(category: BudgetCategory?) {
        if (category == null) {
            lifecycleScope.launch {
                viewModel.budgetAllocations.collect { budgets ->
                    budgetAdapter.submitList(budgets)
                }
            }
        } else {
            val filtered = viewModel.getBudgetByCategory(category)
            budgetAdapter.submitList(filtered)
        }
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