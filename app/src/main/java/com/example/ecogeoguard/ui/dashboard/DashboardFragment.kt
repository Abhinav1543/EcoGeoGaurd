package com.example.ecogeoguard.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ecogeoguard.databinding.FragmentDashboardBinding
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth


class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userRole = arguments?.getString("userRole") ?: "USER"
        val userName = arguments?.getString("userName") ?: "User"

        // Display user info
        binding.tvWelcome.text = "Welcome, $userName!"
        binding.tvRole.text = "Role: $userRole"

        // Role-specific content
        setupDashboardForRole(userRole)

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            // Navigate back to login
            findNavController().navigate(
                com.example.ecogeoguard.R.id.loginFragment
            )
        }

    }

    private fun setupDashboardForRole(role: String) {
        when (role.uppercase()) {
            "FARMER" -> {
                binding.tvDashboardTitle.text = "👨‍🌾 Farmer Dashboard"
                binding.tvFeatures.text = "• Crop Monitoring\n• Irrigation Control\n• Weather Alerts"
            }
            "ADMIN" -> {
                binding.tvDashboardTitle.text = "⚙️ Admin Dashboard"
                binding.tvFeatures.text = "• User Management\n• System Settings\n• Analytics"
            }
            "LIVESTOCK_OWNER" -> {
                binding.tvDashboardTitle.text = "🐄 Livestock Dashboard"
                binding.tvFeatures.text = "• Animal Tracking\n• Health Monitoring\n• Feeding Schedule"
            }
            "AUTHORITY" -> {
                binding.tvDashboardTitle.text = "🚨 Disaster Authority Dashboard"
                binding.tvFeatures.text = "• Disaster Alerts\n• Area Monitoring\n• Emergency Response"
            }
            "GOVERNMENT" -> {
                binding.tvDashboardTitle.text = "🏛️ Government Dashboard"
                binding.tvFeatures.text = "• Analytics & Reports\n• Policy Planning\n• Resource Allocation"
            }
            else -> {
                binding.tvDashboardTitle.text = "📊 Dashboard"
                binding.tvFeatures.text = "• View Statistics\n• Manage Profile\n• Check Notifications"
            }
        }

        Toast.makeText(requireContext(), "Welcome to $role Dashboard", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}