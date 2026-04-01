package com.example.ecogeoguard.ui.livestock.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.Animal
import com.example.ecogeoguard.data.model.AnimalLocation
import com.example.ecogeoguard.data.model.HealthStatus
import com.example.ecogeoguard.databinding.ItemAnimalBinding
import java.text.SimpleDateFormat
import java.util.*

class AnimalAdapter(
    private val onItemClick: (Animal) -> Unit,
    private val onLocationClick: (Animal) -> Unit
) : ListAdapter<Animal, AnimalAdapter.AnimalViewHolder>(AnimalDiffCallback()) {

    private var locations: Map<String, AnimalLocation> = emptyMap()

    fun updateLocations(newLocations: Map<String, AnimalLocation>) {
        locations = newLocations
        notifyDataSetChanged()
    }

    // Add this method to manually refresh the list
    fun refreshList() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder {
        val binding = ItemAnimalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnimalViewHolder(binding, onItemClick, onLocationClick)
    }

    override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
        holder.bind(getItem(position), locations[getItem(position).id])
    }

    class AnimalViewHolder(
        private val binding: ItemAnimalBinding,
        private val onItemClick: (Animal) -> Unit,
        private val onLocationClick: (Animal) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(animal: Animal, location: AnimalLocation?) {
            binding.apply {
                // Animal info
                tvAnimalName.text = animal.name
                tvAnimalType.text = "${animal.type.name} • ${animal.breed}"
                tvTagNumber.text = "Tag: ${animal.tagNumber}"

                // Health status
                tvHealthStatus.text = animal.healthStatus.name
                val healthColor = when (animal.healthStatus) {
                    HealthStatus.EXCELLENT, HealthStatus.GOOD -> R.color.success
                    HealthStatus.FAIR -> R.color.warning
                    HealthStatus.POOR, HealthStatus.CRITICAL -> R.color.dangerRed
                    HealthStatus.UNDER_TREATMENT -> R.color.info
                }
                tvHealthStatus.setTextColor(ContextCompat.getColor(root.context, healthColor))
                tvHealthStatus.setBackgroundColor(ContextCompat.getColor(root.context, healthColor).withAlpha(30))

                // Location info
                if (location != null) {
                    tvLastSeen.text = "Last seen: ${getTimeAgo(location.timestamp)}"
                    tvBatteryLevel.text = "🔋 ${location.batteryLevel}%"

                    // Signal strength indicator
                    val signalColor = when {
                        location.signalStrength > -70 -> R.color.success
                        location.signalStrength > -85 -> R.color.warning
                        else -> R.color.dangerRed
                    }
                    tvSignalStrength.setTextColor(
                        ContextCompat.getColor(root.context, signalColor)
                    )
                    tvSignalStrength.text = "📶 ${location.signalStrength} dBm"

                    // Online/offline status
                    val isOnline = System.currentTimeMillis() - location.timestamp < 15 * 60 * 1000
                    ivOnlineStatus.setImageResource(
                        if (isOnline) R.drawable.ic_online else R.drawable.img_23
                    )
                } else {
                    tvLastSeen.text = "Location unavailable"
                    tvBatteryLevel.text = "🔋 --%"
                    tvSignalStrength.text = "📶 No signal"
                    ivOnlineStatus.setImageResource(R.drawable.img_23)
                }

                // Pregnancy indicator
                ivPregnancy.visibility = when (animal.pregnancyStatus) {
                    com.example.ecogeoguard.data.model.PregnancyStatus.PREGNANT,
                    com.example.ecogeoguard.data.model.PregnancyStatus.NEAR_DELIVERY -> View.VISIBLE
                    else -> View.GONE
                }

                // Click listeners
                root.setOnClickListener {
                    onItemClick(animal)
                }

                btnLocation.setOnClickListener {
                    onLocationClick(animal)
                }
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60 * 1000 -> "Just now"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
                else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    class AnimalDiffCallback : DiffUtil.ItemCallback<Animal>() {
        override fun areItemsTheSame(oldItem: Animal, newItem: Animal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Animal, newItem: Animal): Boolean {
            return oldItem.name == newItem.name &&
                    oldItem.healthStatus == newItem.healthStatus &&
                    oldItem.pregnancyStatus == newItem.pregnancyStatus
        }
    }
}

// Extension function for alpha
private fun Int.withAlpha(alpha: Int): Int {
    return (alpha shl 24) or (this and 0x00ffffff)
}