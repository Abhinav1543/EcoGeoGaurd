package com.example.ecogeoguard.ui.farmer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.data.model.FieldSensorData
import com.example.ecogeoguard.databinding.ItemSensorHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class SensorHistoryAdapter : ListAdapter<FieldSensorData, SensorHistoryAdapter.ViewHolder>(SensorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSensorHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemSensorHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: FieldSensorData) {
            binding.apply {
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                tvTime.text = timeFormat.format(Date(data.timestamp))

                tvMoisture.text = "${"%.1f".format(data.soilMoisture)}%"
                tvTemperature.text = "${"%.1f".format(data.ambientTemperature)}°C"
                tvRainfall.text = "${"%.1f".format(data.rainfall)} mm"
                tvHumidity.text = "${"%.0f".format(data.humidity)}%"
            }
        }
    }

    class SensorDiffCallback : DiffUtil.ItemCallback<FieldSensorData>() {
        override fun areItemsTheSame(oldItem: FieldSensorData, newItem: FieldSensorData): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: FieldSensorData, newItem: FieldSensorData): Boolean {
            return oldItem == newItem
        }
    }
}