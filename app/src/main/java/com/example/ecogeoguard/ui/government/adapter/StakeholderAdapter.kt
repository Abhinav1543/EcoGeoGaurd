package com.example.ecogeoguard.ui.government.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.Stakeholder
import com.example.ecogeoguard.databinding.ItemStakeholderBinding
import java.text.SimpleDateFormat
import java.util.*

class StakeholderAdapter(
    private val onItemClick: (Stakeholder) -> Unit,
    private val onMessageClick: (Stakeholder) -> Unit,
    private val onPerformanceClick: (Stakeholder) -> Unit
) : ListAdapter<Stakeholder, StakeholderAdapter.StakeholderViewHolder>(StakeholderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StakeholderViewHolder {
        val binding = ItemStakeholderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StakeholderViewHolder(binding, onItemClick, onMessageClick, onPerformanceClick)
    }

    override fun onBindViewHolder(holder: StakeholderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StakeholderViewHolder(
        private val binding: ItemStakeholderBinding,
        private val onItemClick: (Stakeholder) -> Unit,
        private val onMessageClick: (Stakeholder) -> Unit,
        private val onPerformanceClick: (Stakeholder) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stakeholder: Stakeholder) {
            binding.apply {
                tvName.text = stakeholder.name
                tvRole.text = stakeholder.role.title
                tvVillage.text = stakeholder.villageId

                // Performance score
                tvPerformance.text = "${String.format("%.1f", stakeholder.performanceScore)}%"
                progressPerformance.progress = stakeholder.performanceScore.toInt()

                val performanceColor = when {
                    stakeholder.performanceScore >= 70 -> R.color.success
                    stakeholder.performanceScore >= 50 -> R.color.warning
                    else -> R.color.dangerRed
                }
                progressPerformance.progressTintList =
                    ContextCompat.getColorStateList(root.context, performanceColor)

                // Task status
                val totalTasks = stakeholder.tasksCompleted + stakeholder.pendingTasks
                tvTasks.text = "${stakeholder.tasksCompleted}/$totalTasks tasks"

                // Response time
                tvResponseTime.text = "${stakeholder.responseTime} min avg"

                // Last active
                val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                tvLastActive.text = "Active: ${dateFormat.format(Date(stakeholder.lastActive))}"

                // Status indicator
                val isActive = System.currentTimeMillis() - stakeholder.lastActive < 7 * 24 * 60 * 60 * 1000
                ivStatus.setImageResource(if (isActive) R.drawable.ic_online else R.drawable.img_23)

                // Click listeners
                root.setOnClickListener { onItemClick(stakeholder) }
                btnMessage.setOnClickListener { onMessageClick(stakeholder) }
                btnPerformance.setOnClickListener { onPerformanceClick(stakeholder) }
            }
        }
    }

    class StakeholderDiffCallback : DiffUtil.ItemCallback<Stakeholder>() {
        override fun areItemsTheSame(oldItem: Stakeholder, newItem: Stakeholder): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Stakeholder, newItem: Stakeholder): Boolean {
            return oldItem.performanceScore == newItem.performanceScore &&
                    oldItem.tasksCompleted == newItem.tasksCompleted
        }
    }
}