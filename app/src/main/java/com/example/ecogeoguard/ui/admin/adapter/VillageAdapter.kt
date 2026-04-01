package com.example.ecogeoguard.ui.admin.adapter



import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.Village
import com.google.android.material.card.MaterialCardView

class VillageAdapter(
    private val onItemClick: (Village) -> Unit
) : ListAdapter<Village, VillageAdapter.VillageViewHolder>(VillageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VillageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_village, parent, false)
        return VillageViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: VillageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VillageViewHolder(
        itemView: View,
        private val onItemClick: (Village) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardVillage)
        private val tvName: TextView = itemView.findViewById(R.id.tvVillageName)
        private val tvDistrict: TextView = itemView.findViewById(R.id.tvVillageDistrict)
        private val tvSensors: TextView = itemView.findViewById(R.id.tvVillageSensors)
        private val tvRisk: TextView = itemView.findViewById(R.id.tvVillageRisk)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressSensorHealth)

        fun bind(village: Village) {
            tvName.text = village.name
            tvDistrict.text = village.district
            tvSensors.text = "${village.activeSensors}/${village.totalSensors} sensors"
            tvRisk.text = village.riskLevel.name

            // Set risk color
            val color = ContextCompat.getColor(itemView.context, village.riskColor)
            tvRisk.setTextColor(color)

            // Set sensor health progress
            progressBar.progress = village.sensorHealth

            // Set card stroke color based on risk
            cardView.strokeColor = color
            cardView.strokeWidth = if (village.riskLevel == Village.RiskLevel.CRITICAL) 3 else 2

            itemView.setOnClickListener {
                onItemClick(village)
            }
        }
    }
}

class VillageDiffCallback : DiffUtil.ItemCallback<Village>() {
    override fun areItemsTheSame(oldItem: Village, newItem: Village): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Village, newItem: Village): Boolean {
        return oldItem == newItem
    }
}