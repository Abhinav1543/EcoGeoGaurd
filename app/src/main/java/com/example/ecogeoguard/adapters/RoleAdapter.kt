package com.example.ecogeoguard.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.UserRole
import com.example.ecogeoguard.databinding.ItemRoleBinding
import androidx.core.view.isVisible


class RoleAdapter(
    private val roles: List<UserRole>,
    private val onRoleSelected: (Int, UserRole) -> Unit
) : RecyclerView.Adapter<RoleAdapter.RoleViewHolder>() {

    var selectedPosition = -1

    inner class RoleViewHolder(private val binding: ItemRoleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(role: UserRole, position: Int) {
            binding.apply {
                tvRoleTitle.text = role.title
                tvRoleDescription.text = role.description
                ivRoleIcon.setImageResource(role.iconRes)

                // Set selection state
                if (position == selectedPosition) {
                    root.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, R.color.primaryGreenLight)
                    )
                    ivCheck.isVisible = true
                    tvRoleTitle.setTextColor(
                        ContextCompat.getColor(root.context, R.color.bgWhite)
                    )
                    tvRoleDescription.setTextColor(
                        ContextCompat.getColor(root.context, R.color.bgWhite)
                    )
                } else {
                    root.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, R.color.bgWhite)
                    )
                    ivCheck.isVisible = false
                    tvRoleTitle.setTextColor(
                        ContextCompat.getColor(root.context, R.color.textPrimary)
                    )
                    tvRoleDescription.setTextColor(
                        ContextCompat.getColor(root.context, R.color.textSecondary)
                    )
                }

                root.setOnClickListener {
                    val previousSelected = selectedPosition
                    selectedPosition = position

                    // Update previous item
                    if (previousSelected != -1) {
                        notifyItemChanged(previousSelected)
                    }

                    // Update current item
                    notifyItemChanged(selectedPosition)

                    // Notify listener
                    onRoleSelected(position, role)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoleViewHolder {
        val binding = ItemRoleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RoleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoleViewHolder, position: Int) {
        holder.bind(roles[position], position)
    }

    override fun getItemCount(): Int = roles.size

    fun getSelectedRole(): UserRole? {
        return if (selectedPosition != -1) {
            roles[selectedPosition]
        } else {
            null
        }
    }
}