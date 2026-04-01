//package com.example.ecogeoguard.ui.livestock.adapter
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import com.example.ecogeoguard.R
//import com.example.ecogeoguard.data.model.AlertSeverity
//import com.example.ecogeoguard.data.model.HealthAlert
//import com.example.ecogeoguard.databinding.ItemHealthAlertBinding
//import java.text.SimpleDateFormat
//import java.util.*
//
//class HealthAlertAdapter(
//    private val onItemClick: (HealthAlert) -> Unit,
//    private val onAcknowledge: (HealthAlert) -> Unit
//) : ListAdapter<HealthAlert, HealthAlertAdapter.AlertViewHolder>(AlertDiffCallback()) {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
//        val binding = ItemHealthAlertBinding.inflate(
//            LayoutInflater.from(parent.context),
//            parent,
//            false
//        )
//        return AlertViewHolder(binding, onItemClick, onAcknowledge)
//    }
//
//    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
//        holder.bind(getItem(position))
//    }
//
//    class AlertViewHolder(
//        private val binding: ItemHealthAlertBinding,
//        private val onItemClick: (HealthAlert) -> Unit,
//        private val onAcknowledge: (HealthAlert) -> Unit
//    ) : RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(alert: HealthAlert) {
//            binding.apply {
//                // Alert icon
//                ivAlertIcon.setImageResource(
//                    when (alert.type) {
//                        com.example.ecogeoguard.data.model.HealthAlertType.HIGH_HEART_RATE,
//                        com.example.ecogeoguard.data.model.HealthAlertType.LOW_HEART_RATE -> R.drawable.img_19
//                        com.example.ecogeoguard.data.model.HealthAlertType.HIGH_TEMPERATURE -> R.drawable.img_20
//                        com.example.ecogeoguard.data.model.HealthAlertType.NO_MOVEMENT -> R.drawable.img_21
//                        com.example.ecogeoguard.data.model.HealthAlertType.VACCINATION_DUE -> R.drawable.ic_vaccine
//                        com.example.ecogeoguard.data.model.HealthAlertType.CALVING_IMMINENT -> R.drawable.img_22
//                        else -> R.drawable.ic_alert
//                    }
//                )
//
//                // Severity color
//                val severityColor = when (alert.severity) {
//                    com.example.ecogeoguard.data.model.AlertSeverity.INFO ->
//                        ContextCompat.getColor(root.context, R.color.info)
//                    com.example.ecogeoguard.data.model.AlertSeverity.LOW ->
//                        ContextCompat.getColor(root.context, R.color.success)
//                    com.example.ecogeoguard.data.model.AlertSeverity.MEDIUM ->
//                        ContextCompat.getColor(root.context, R.color.warning)
//                    com.example.ecogeoguard.data.model.AlertSeverity.HIGH ->
//                        ContextCompat.getColor(root.context, R.color.warningOrange)
//                    com.example.ecogeoguard.data.model.AlertSeverity.CRITICAL ->
//                        ContextCompat.getColor(root.context, R.color.dangerRed)
//                }
//
//
//                cardAlert.setCardBackgroundColor(severityColor)
//
//                tvAlertTitle.text = "${alert.animalName} • ${alert.type.name.replace('_', ' ')}"
//                tvAlertMessage.text = alert.message
//                tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(alert.timestamp))
//
//                // Acknowledge button
//                btnAcknowledge.visibility = if (!alert.isAcknowledged) View.VISIBLE else View.GONE
//                btnAcknowledge.setOnClickListener {
//                    onAcknowledge(alert)
//                }
//
//                // Click listener
//                root.setOnClickListener {
//                    onItemClick(alert)
//                }
//            }
//        }
//    }
//
//    class AlertDiffCallback : DiffUtil.ItemCallback<HealthAlert>() {
//        override fun areItemsTheSame(oldItem: HealthAlert, newItem: HealthAlert): Boolean {
//            return oldItem.id == newItem.id
//        }
//
//        override fun areContentsTheSame(oldItem: HealthAlert, newItem: HealthAlert): Boolean {
//            return oldItem.isAcknowledged == newItem.isAcknowledged &&
//                    oldItem.severity == newItem.severity
//        }
//    }
//}

package com.example.ecogeoguard.ui.livestock.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.HealthAlert
import com.example.ecogeoguard.data.model.HealthAlertType
import com.example.ecogeoguard.databinding.ItemHealthAlertBinding
import java.text.SimpleDateFormat
import java.util.*

class HealthAlertAdapter(
    private val onItemClick: (HealthAlert) -> Unit,
    private val onAcknowledge: (HealthAlert) -> Unit
) : ListAdapter<HealthAlert, HealthAlertAdapter.AlertViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemHealthAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlertViewHolder(binding, onItemClick, onAcknowledge)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AlertViewHolder(
        private val binding: ItemHealthAlertBinding,
        private val onItemClick: (HealthAlert) -> Unit,
        private val onAcknowledge: (HealthAlert) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: HealthAlert) {
            binding.apply {
                ivAlertIcon.imageTintList = null
                // Alert icon
                ivAlertIcon.setImageResource(
                    when (alert.type) {
                        HealthAlertType.HIGH_HEART_RATE,
                        HealthAlertType.LOW_HEART_RATE -> R.drawable.img_19
                        HealthAlertType.HIGH_TEMPERATURE -> R.drawable.img_20
                        HealthAlertType.NO_MOVEMENT -> R.drawable.img_21
                        HealthAlertType.VACCINATION_DUE -> R.drawable.ic_vaccine
                        HealthAlertType.CALVING_IMMINENT -> R.drawable.img_22
                        HealthAlertType.INJURY_DETECTED -> R.drawable.img_24
                        HealthAlertType.STRESS_DETECTED -> R.drawable.img_25
                        else -> R.drawable.ic_alert
                    }
                )

                // Severity color
                val severityColor = when (alert.severity) {
                    com.example.ecogeoguard.data.model.AlertSeverity.INFO ->
                        ContextCompat.getColor(root.context, R.color.info)
                    com.example.ecogeoguard.data.model.AlertSeverity.LOW ->
                        ContextCompat.getColor(root.context, R.color.success)
                    com.example.ecogeoguard.data.model.AlertSeverity.MEDIUM ->
                        ContextCompat.getColor(root.context, R.color.warning)
                    com.example.ecogeoguard.data.model.AlertSeverity.HIGH ->
                        ContextCompat.getColor(root.context, R.color.warningOrange)
                    com.example.ecogeoguard.data.model.AlertSeverity.CRITICAL ->
                        ContextCompat.getColor(root.context, R.color.dangerRed)
                }

                cardAlert.setCardBackgroundColor(severityColor)

                tvAlertTitle.text = "${alert.animalName} • ${alert.type.name.replace('_', ' ')}"
                tvAlertMessage.text = alert.message
                tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(alert.timestamp))

                // Acknowledge button
                btnAcknowledge.visibility = if (!alert.isAcknowledged) View.VISIBLE else View.GONE
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
    }

    class AlertDiffCallback : DiffUtil.ItemCallback<HealthAlert>() {
        override fun areItemsTheSame(oldItem: HealthAlert, newItem: HealthAlert): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HealthAlert, newItem: HealthAlert): Boolean {
            return oldItem.isAcknowledged == newItem.isAcknowledged &&
                    oldItem.severity == newItem.severity
        }
    }
}