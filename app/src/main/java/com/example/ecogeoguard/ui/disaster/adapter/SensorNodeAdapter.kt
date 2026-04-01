package com.example.ecogeoguard.ui.disaster.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.DisasterSensorNode
import com.example.ecogeoguard.data.model.DisasterSensorStatus
import com.example.ecogeoguard.databinding.ItemSensorNodeBinding
import java.text.SimpleDateFormat
import java.util.*

class SensorNodeAdapter(
    private val onItemClick: (DisasterSensorNode) -> Unit,
    private val onStatusClick: (DisasterSensorNode) -> Unit,
    private val onCalibrateClick: (DisasterSensorNode) -> Unit
) : ListAdapter<DisasterSensorNode, SensorNodeAdapter.SensorViewHolder>(SensorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val binding = ItemSensorNodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SensorViewHolder(binding, onItemClick, onStatusClick, onCalibrateClick)
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SensorViewHolder(
        private val binding: ItemSensorNodeBinding,
        private val onItemClick: (DisasterSensorNode) -> Unit,
        private val onStatusClick: (DisasterSensorNode) -> Unit,
        private val onCalibrateClick: (DisasterSensorNode) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(sensor: DisasterSensorNode) {
            binding.apply {
                tvSensorId.text = sensor.id
                tvSensorType.text = sensor.type.name
                tvLocation.text = sensor.location.address ?: "Unknown location"
                tvVillage.text = sensor.villageId

                // Battery level
                tvBatteryLevel.text = "${sensor.batteryLevel}%"
                val batteryColor = when {
                    sensor.batteryLevel > 70 -> R.color.success
                    sensor.batteryLevel > 30 -> R.color.warning
                    else -> R.color.dangerRed
                }
                tvBatteryLevel.setTextColor(ContextCompat.getColor(root.context, batteryColor))
                progressBattery.progress = sensor.batteryLevel
                progressBattery.progressTintList = ContextCompat.getColorStateList(root.context, batteryColor)

                // Signal strength
                tvSignalStrength.text = "${sensor.signalStrength} dBm"
                val signalColor = when {
                    sensor.signalStrength > -70 -> R.color.success
                    sensor.signalStrength > -85 -> R.color.warning
                    else -> R.color.dangerRed
                }
                tvSignalStrength.setTextColor(ContextCompat.getColor(root.context, signalColor))

                // Status
                val (statusText, statusColor, statusIcon) = when (sensor.status) {
                    DisasterSensorStatus.ONLINE -> Triple("ONLINE", R.color.success, R.drawable.ic_online)
                    DisasterSensorStatus.OFFLINE -> Triple("OFFLINE", R.color.textSecondary, R.drawable.img_23)
                    DisasterSensorStatus.LOW_BATTERY -> Triple("LOW BATTERY", R.color.warningOrange, R.drawable.battery_low)
                    DisasterSensorStatus.MAINTENANCE -> Triple("MAINTENANCE", R.color.info, R.drawable.img_33)
                    DisasterSensorStatus.ERROR -> Triple("ERROR", R.color.dangerRed, R.drawable.ic_error)
                }

                tvStatus.text = statusText
                tvStatus.setTextColor(ContextCompat.getColor(root.context, statusColor))
                ivStatusIcon.setImageResource(statusIcon)
                ivStatusIcon.setColorFilter(ContextCompat.getColor(root.context, statusColor))

                // Last reading time
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                tvLastReading.text = "Last: ${timeFormat.format(Date(sensor.lastReading))}"

                // Firmware
                tvFirmware.text = "v${sensor.firmwareVersion}"

                // Click listeners
                root.setOnClickListener { onItemClick(sensor) }
                btnStatus.setOnClickListener { onStatusClick(sensor) }
                btnCalibrate.setOnClickListener { onCalibrateClick(sensor) }
            }
        }
    }

    class SensorDiffCallback : DiffUtil.ItemCallback<DisasterSensorNode>() {
        override fun areItemsTheSame(oldItem: DisasterSensorNode, newItem: DisasterSensorNode): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DisasterSensorNode, newItem: DisasterSensorNode): Boolean {
            return oldItem.status == newItem.status &&
                    oldItem.batteryLevel == newItem.batteryLevel &&
                    oldItem.lastReading == newItem.lastReading
        }
    }
}