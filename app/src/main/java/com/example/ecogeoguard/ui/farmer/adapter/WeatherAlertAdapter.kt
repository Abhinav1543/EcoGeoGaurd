// ui/farmer/adapter/WeatherAlertAdapter.kt
package com.example.ecogeoguard.ui.farmer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.RiskLevel
import com.example.ecogeoguard.data.model.WeatherAlert
import com.example.ecogeoguard.data.model.WeatherAlertType
import com.example.ecogeoguard.databinding.ItemWeatherAlertBinding
import java.text.SimpleDateFormat
import java.util.*

class WeatherAlertAdapter(
    private val onItemClick: (WeatherAlert) -> Unit
) : ListAdapter<WeatherAlert, WeatherAlertAdapter.AlertViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemWeatherAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlertViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AlertViewHolder(
        private val binding: ItemWeatherAlertBinding,
        private val onItemClick: (WeatherAlert) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: WeatherAlert) {
            binding.apply {
                ivAlertIcon.imageTintList = null
                // Set icon based on alert type
                ivAlertIcon.setImageResource(
                    when (alert.type) {
                        WeatherAlertType.LANDSLIDE -> R.drawable.img_7
                        WeatherAlertType.HEAVY_RAINFALL -> R.drawable.img_1
                        WeatherAlertType.HAILSTORM -> R.drawable.img_12
                        WeatherAlertType.FROST -> R.drawable.img_13
                        WeatherAlertType.DROUGHT -> R.drawable.img_14
                        WeatherAlertType.STRONG_WINDS -> R.drawable.img_15
                        WeatherAlertType.FLOOD -> R.drawable.img_16
                    }
                )

                // Set severity color
                val severityColor = when (alert.severity) {
                    RiskLevel.LOW -> android.graphics.Color.parseColor("#4CAF50")
                    RiskLevel.MODERATE -> android.graphics.Color.parseColor("#2196F3")
                    RiskLevel.HIGH -> android.graphics.Color.parseColor("#FF9800")
                    RiskLevel.CRITICAL -> android.graphics.Color.parseColor("#F44336")
                }

                cardAlert.setCardBackgroundColor(severityColor)
                tvSeverity.text = alert.severity.name
                tvSeverity.setTextColor(severityColor)

                tvMessage.text = alert.message
                tvRecommendedAction.text = alert.recommendedAction

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                tvTime.text = timeFormat.format(Date(alert.timestamp))

                root.setOnClickListener {
                    onItemClick(alert)
                }
            }
        }
    }

    class AlertDiffCallback : DiffUtil.ItemCallback<WeatherAlert>() {
        override fun areItemsTheSame(oldItem: WeatherAlert, newItem: WeatherAlert): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WeatherAlert, newItem: WeatherAlert): Boolean {
            return oldItem == newItem
        }
    }
}