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
import com.example.ecogeoguard.data.model.RequestStatusType
import com.example.ecogeoguard.data.model.ResourceRequest
import com.example.ecogeoguard.databinding.ItemResourceRequestBinding
import java.text.SimpleDateFormat
import java.util.*

class ResourceRequestAdapter(
    private val onItemClick: (ResourceRequest) -> Unit,
    private val onApproveClick: (ResourceRequest) -> Unit,
    private val onRejectClick: (ResourceRequest) -> Unit,
    private val onCompleteClick: (ResourceRequest) -> Unit
) : ListAdapter<ResourceRequest, ResourceRequestAdapter.RequestViewHolder>(RequestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemResourceRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding, onItemClick, onApproveClick, onRejectClick, onCompleteClick)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RequestViewHolder(
        private val binding: ItemResourceRequestBinding,
        private val onItemClick: (ResourceRequest) -> Unit,
        private val onApproveClick: (ResourceRequest) -> Unit,
        private val onRejectClick: (ResourceRequest) -> Unit,
        private val onCompleteClick: (ResourceRequest) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: ResourceRequest) {
            binding.apply {
                tvRequester.text = request.requesterName
                tvRole.text = request.requesterRole
                tvResourceType.text = request.resourceType.name
                tvQuantity.text = "x${request.quantity}"
                tvLocation.text = request.location.address ?: "Unknown location"

                // Urgency badge
                val (urgencyText, urgencyColor) = when (request.urgency) {
                    AlertSeverityLevel.CRITICAL -> Pair("CRITICAL", R.color.dangerRed)
                    AlertSeverityLevel.HIGH -> Pair("HIGH", R.color.warningOrange)
                    AlertSeverityLevel.MEDIUM -> Pair("MEDIUM", R.color.warning)
                    AlertSeverityLevel.LOW -> Pair("LOW", R.color.info)
                    else -> Pair("INFO", R.color.info)
                }
                tvUrgency.text = urgencyText
                tvUrgency.setBackgroundColor(ContextCompat.getColor(root.context, urgencyColor))

                // Status
                val (statusText, statusColor) = when (request.status) {
                    RequestStatusType.PENDING -> Pair("PENDING", R.color.warningOrange)
                    RequestStatusType.APPROVED -> Pair("APPROVED", R.color.success)
                    RequestStatusType.IN_PROGRESS -> Pair("IN PROGRESS", R.color.info)
                    RequestStatusType.COMPLETED -> Pair("COMPLETED", R.color.success)
                    RequestStatusType.REJECTED -> Pair("REJECTED", R.color.dangerRed)
                }
                tvStatus.text = statusText
                tvStatus.setTextColor(ContextCompat.getColor(root.context, statusColor))

                // Time
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                tvTime.text = timeFormat.format(Date(request.timestamp))

                // Action buttons based on status
                when (request.status) {
                    RequestStatusType.PENDING -> {
                        btnApprove.visibility = View.VISIBLE
                        btnReject.visibility = View.VISIBLE
                        btnComplete.visibility = View.GONE
                    }
                    RequestStatusType.APPROVED, RequestStatusType.IN_PROGRESS -> {
                        btnApprove.visibility = View.GONE
                        btnReject.visibility = View.VISIBLE
                        btnComplete.visibility = View.VISIBLE
                    }
                    else -> {
                        btnApprove.visibility = View.GONE
                        btnReject.visibility = View.GONE
                        btnComplete.visibility = View.GONE
                    }
                }

                // Click listeners
                root.setOnClickListener { onItemClick(request) }
                btnApprove.setOnClickListener { onApproveClick(request) }
                btnReject.setOnClickListener { onRejectClick(request) }
                btnComplete.setOnClickListener { onCompleteClick(request) }
            }
        }
    }

    class RequestDiffCallback : DiffUtil.ItemCallback<ResourceRequest>() {
        override fun areItemsTheSame(oldItem: ResourceRequest, newItem: ResourceRequest): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ResourceRequest, newItem: ResourceRequest): Boolean {
            return oldItem.status == newItem.status &&
                    oldItem.assignedTo == newItem.assignedTo
        }
    }
}