package com.example.ecogeoguard.ui.government.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.PolicyRecommendation
import com.example.ecogeoguard.data.model.Priority
import com.example.ecogeoguard.data.model.RecommendationStatus
import com.example.ecogeoguard.databinding.ItemPolicyRecommendationBinding
import java.text.SimpleDateFormat
import java.util.*

class PolicyRecommendationAdapter(
    private val onItemClick: (PolicyRecommendation) -> Unit,
    private val onApproveClick: (PolicyRecommendation) -> Unit,
    private val onRejectClick: (PolicyRecommendation) -> Unit,
    private val onImplementClick: (PolicyRecommendation) -> Unit
) : ListAdapter<PolicyRecommendation, PolicyRecommendationAdapter.PolicyViewHolder>(PolicyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PolicyViewHolder {
        val binding = ItemPolicyRecommendationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PolicyViewHolder(binding, onItemClick, onApproveClick, onRejectClick, onImplementClick)
    }

    override fun onBindViewHolder(holder: PolicyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PolicyViewHolder(
        private val binding: ItemPolicyRecommendationBinding,
        private val onItemClick: (PolicyRecommendation) -> Unit,
        private val onApproveClick: (PolicyRecommendation) -> Unit,
        private val onRejectClick: (PolicyRecommendation) -> Unit,
        private val onImplementClick: (PolicyRecommendation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(policy: PolicyRecommendation) {
            binding.apply {
                tvTitle.text = policy.title
                tvCategory.text = policy.category.name

                // Priority badge
                val (priorityText, priorityColor) = when (policy.priority) {
                    Priority.URGENT -> Pair("URGENT", R.color.dangerRed)
                    Priority.HIGH -> Pair("HIGH", R.color.warningOrange)
                    Priority.MEDIUM -> Pair("MEDIUM", R.color.warning)
                    Priority.LOW -> Pair("LOW", R.color.info)
                }
                tvPriority.text = priorityText
                tvPriority.setBackgroundColor(ContextCompat.getColor(root.context, priorityColor))

                // Status badge
                val (statusText, statusColor) = when (policy.status) {
                    RecommendationStatus.PENDING -> Pair("PENDING", R.color.warningOrange)
                    RecommendationStatus.APPROVED -> Pair("APPROVED", R.color.success)
                    RecommendationStatus.IMPLEMENTED -> Pair("IMPLEMENTED", R.color.info)
                    RecommendationStatus.REJECTED -> Pair("REJECTED", R.color.dangerRed)
                }
                tvStatus.text = statusText
                tvStatus.setTextColor(ContextCompat.getColor(root.context, statusColor))

                // Description
                tvDescription.text = policy.description

                // Cost and Time
                tvCost.text = formatRupees(policy.estimatedCost)
                tvTimeFrame.text = policy.timeFrame

                // AI Confidence
                val confidencePercent = (policy.aiConfidence * 100).toInt()
                tvConfidence.text = "${confidencePercent}% AI Confidence"
                progressConfidence.progress = confidencePercent

                val confidenceColor = when {
                    confidencePercent >= 80 -> R.color.success
                    confidencePercent >= 60 -> R.color.warning
                    else -> R.color.dangerRed
                }
                progressConfidence.progressTintList =
                    ContextCompat.getColorStateList(root.context, confidenceColor)

                // Date
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                tvDate.text = dateFormat.format(Date(policy.createdAt))

                // Action buttons based on status
                when (policy.status) {
                    RecommendationStatus.PENDING -> {
                        btnApprove.visibility = View.VISIBLE
                        btnReject.visibility = View.VISIBLE
                        btnImplement.visibility = View.GONE
                    }
                    RecommendationStatus.APPROVED -> {
                        btnApprove.visibility = View.GONE
                        btnReject.visibility = View.VISIBLE
                        btnImplement.visibility = View.VISIBLE
                    }
                    else -> {
                        btnApprove.visibility = View.GONE
                        btnReject.visibility = View.GONE
                        btnImplement.visibility = View.GONE
                    }
                }

                // Click listeners
                root.setOnClickListener { onItemClick(policy) }
                btnApprove.setOnClickListener { onApproveClick(policy) }
                btnReject.setOnClickListener { onRejectClick(policy) }
                btnImplement.setOnClickListener { onImplementClick(policy) }
            }
        }

        private fun formatRupees(amount: Double): String {
            return when {
                amount >= 10000000 -> "₹${String.format("%.2f", amount / 10000000)} Cr"
                amount >= 100000 -> "₹${String.format("%.2f", amount / 100000)} L"
                else -> "₹${String.format("%.0f", amount)}"
            }
        }
    }

    class PolicyDiffCallback : DiffUtil.ItemCallback<PolicyRecommendation>() {
        override fun areItemsTheSame(oldItem: PolicyRecommendation, newItem: PolicyRecommendation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PolicyRecommendation, newItem: PolicyRecommendation): Boolean {
            return oldItem.status == newItem.status
        }
    }
}