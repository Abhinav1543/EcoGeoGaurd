package com.example.ecogeoguard.ui.disaster.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.AlertSeverityLevel
import com.example.ecogeoguard.data.model.DisasterAlert
import com.example.ecogeoguard.data.model.DisasterType
import com.example.ecogeoguard.databinding.ItemAlertBinding
import java.text.SimpleDateFormat
import java.util.*

class DisasterAlertAdapter(
    private val onItemClick: (DisasterAlert) -> Unit,
    private val onAcknowledge: (DisasterAlert) -> Unit
) : ListAdapter<DisasterAlert, DisasterAlertAdapter.AlertViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlertViewHolder(binding, onItemClick, onAcknowledge)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AlertViewHolder(
        private val binding: ItemAlertBinding,
        private val onItemClick: (DisasterAlert) -> Unit,
        private val onAcknowledge: (DisasterAlert) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: DisasterAlert) {
            binding.apply {
                // Set icon based on disaster type
                val iconRes = when (alert.type) {
                    DisasterType.LANDSLIDE -> R.drawable.img
                    DisasterType.FLASH_FLOOD -> R.drawable.img_7
                    DisasterType.HEAVY_RAINFALL -> R.drawable.img_5
                    DisasterType.EARTHQUAKE -> R.drawable.img_32
                    DisasterType.FOREST_FIRE -> R.drawable.ic_fire
                    else -> R.drawable.ic_alert
                }
                ivAlertIcon.setImageResource(iconRes)

                // Set severity color
                val severityColor = when (alert.severity) {
                    AlertSeverityLevel.CRITICAL -> R.color.dangerRed
                    AlertSeverityLevel.HIGH -> R.color.warningOrange
                    AlertSeverityLevel.MEDIUM -> R.color.warning
                    AlertSeverityLevel.LOW -> R.color.info
                    else -> R.color.info
                }
                cardAlert.setCardBackgroundColor(
                    ContextCompat.getColor(root.context, severityColor).withAlpha(30)
                )

                // Set severity badge
                tvSeverity.text = alert.severity.name
                tvSeverity.setTextColor(ContextCompat.getColor(root.context, severityColor))
                tvSeverity.setBackgroundColor(ContextCompat.getColor(root.context, severityColor).withAlpha(30))

                // Alert content
                tvAlertTitle.text = alert.title
                tvAlertMessage.text = alert.message
                tvLocation.text = alert.location.address ?: "Unknown location"
                tvVillages.text = "Affects: ${alert.affectedVillages.joinToString(", ")}"

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                tvTime.text = timeFormat.format(Date(alert.timestamp))

                // Acknowledge button
                btnAcknowledge.visibility = if (alert.isActive) View.VISIBLE else View.GONE
                btnAcknowledge.setOnClickListener {
                    onAcknowledge(alert)
                    btnAcknowledge.visibility = View.GONE
                }

                // Click listener
                root.setOnClickListener {
                    onItemClick(alert)
                }
            }
        }

        private fun Int.withAlpha(alpha: Int): Int {
            return (alpha shl 24) or (this and 0x00ffffff)
        }
    }

    class AlertDiffCallback : DiffUtil.ItemCallback<DisasterAlert>() {
        override fun areItemsTheSame(oldItem: DisasterAlert, newItem: DisasterAlert): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DisasterAlert, newItem: DisasterAlert): Boolean {
            return oldItem.isActive == newItem.isActive &&
                    oldItem.severity == newItem.severity
        }
    }
}