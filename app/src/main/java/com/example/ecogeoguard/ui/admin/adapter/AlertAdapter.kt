package com.example.ecogeoguard.ui.admin.adapter



import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R

import com.example.ecogeoguard.data.model.Alert
import com.google.android.material.card.MaterialCardView

class AlertAdapter(
    private val onItemClick: (Alert) -> Unit
) : ListAdapter<Alert, AlertAdapter.AlertViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_alert, parent, false)
        return AlertViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AlertViewHolder(
        itemView: View,
        private val onItemClick: (Alert) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardAlert)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvAlertTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvAlertMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvAlertTime)
        private val tvType: TextView = itemView.findViewById(R.id.tvAlertType)

        fun bind(alert: Alert) {
            tvTitle.text = alert.title
            tvMessage.text = alert.message
            tvTime.text = alert.formattedTime
            tvType.text = alert.type.name

            // Set card color based on severity
            val color = ContextCompat.getColor(itemView.context, alert.colorRes)
            cardView.setCardBackgroundColor(color)

            // Mark as read visually
            if (alert.isRead) {
                cardView.alpha = 0.7f
            } else {
                cardView.alpha = 1.0f
            }

            itemView.setOnClickListener {
                onItemClick(alert)
            }
        }
    }
}

class AlertDiffCallback : DiffUtil.ItemCallback<Alert>() {
    override fun areItemsTheSame(oldItem: Alert, newItem: Alert): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Alert, newItem: Alert): Boolean {
        return oldItem == newItem
    }
}