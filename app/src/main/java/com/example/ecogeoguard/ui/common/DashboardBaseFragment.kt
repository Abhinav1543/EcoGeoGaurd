// ui/common/DashboardBaseFragment.kt
package com.example.ecogeoguard.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.ecogeoguard.R
import com.example.ecogeoguard.data.model.UserRole
import com.example.ecogeoguard.data.model.WeatherData
import com.example.ecogeoguard.databinding.FragmentDashboardBaseBinding
import com.example.ecogeoguard.viewmodel.DashboardViewModel
import com.google.android.material.badge.BadgeDrawable
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class DashboardBaseFragment : Fragment() {

    protected lateinit var binding: FragmentDashboardBaseBinding
    protected val viewModel: DashboardViewModel by viewModels()
    protected abstract val role: UserRole

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupNavigation()
        setupObservers()
        startDataSimulation()
        onDashboardCreated()
    }

    private fun setupUI() {

        binding.toolbar.title = when (role) {
            UserRole.ADMIN -> "Admin Dashboard"
            UserRole.FARMER -> "Farmer Dashboard"
            UserRole.LIVESTOCK_OWNER -> "Livestock Dashboard"
            UserRole.DISASTER_TEAM -> "Disaster Response"
            UserRole.GOVERNMENT -> "Government Analytics"
        }

        setupNotificationBadge()
    }

    private fun setupNavigation() {

        val navController = findNavController()
        binding.bottomNavigation.setupWithNavController(navController)

        binding.bottomNavigation.menu.clear()

        when (role) {
            UserRole.ADMIN -> {
                binding.bottomNavigation.inflateMenu(R.menu.menu_admin_nav)
            }
            UserRole.FARMER -> binding.bottomNavigation.inflateMenu(R.menu.menu_farmer_nav)
            UserRole.LIVESTOCK_OWNER -> binding.bottomNavigation.inflateMenu(R.menu.menu_livestock_nav)
            UserRole.DISASTER_TEAM -> binding.bottomNavigation.inflateMenu(R.menu.menu_farmer_nav)
            UserRole.GOVERNMENT -> binding.bottomNavigation.inflateMenu(R.menu.menu_livestock_nav)
        }

        // 🔥 CRASH SAFE NAVIGATION (IMPORTANT)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            try {
                findNavController().navigate(item.itemId)
                true
            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "🚧 Feature coming in next update",
                    Toast.LENGTH_SHORT
                ).show()

                true
            }
        }

        // toolbar menu clicks
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {

                R.id.action_notifications -> {
                    try {
                        findNavController().navigate(R.id.notificationsFragment)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Notifications coming soon", Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                R.id.action_search -> {
                    Toast.makeText(requireContext(), "Search (Coming Soon)", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.action_emergency -> {
                    showEmergencyAlert()
                    true
                }

                else -> false
            }
        }
    }

    private fun showEmergencyAlert() {
        Toast.makeText(
            requireContext(),
            "Emergency Alert Triggered!",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun logoutUser() {
        findNavController().navigate(R.id.action_adminDashboardFragment_to_loginFragment)
    }

    private fun setupNotificationBadge() {
        val badge = binding.bottomNavigation.getOrCreateBadge(R.id.notificationsFragment)
        badge.backgroundColor = requireContext().getColor(R.color.dangerRed)
        badge.badgeGravity = BadgeDrawable.TOP_END
        badge.isVisible = false
    }

    private fun setupObservers() {

        viewModel.notificationCount.observe(viewLifecycleOwner) { count ->
            val badge = binding.bottomNavigation.getOrCreateBadge(R.id.notificationsFragment)
            badge.number = count
            badge.isVisible = count > 0
        }

        viewModel.riskLevel.observe(viewLifecycleOwner) { risk ->
            if (binding.riskBanner.tag != risk) {
                updateRiskBanner(risk)
                binding.riskBanner.tag = risk
            }
        }

        viewModel.weatherData.observe(viewLifecycleOwner) { weather ->
            updateWeatherWidget(weather)
        }
    }

    private fun updateRiskBanner(riskLevel: Int) {
        if (riskLevel > 70) {
            binding.riskBanner.apply {
                visibility = View.VISIBLE
                text = "⚠️ HIGH RISK ALERT: Immediate action required!"
            }
        } else if (riskLevel > 40) {
            binding.riskBanner.apply {
                visibility = View.VISIBLE
                text = "⚠️ Moderate Risk: Stay alert"
            }
        } else {
            binding.riskBanner.visibility = View.GONE
        }
    }

    private fun updateWeatherWidget(weather: WeatherData) {

        val weatherContainer = binding.root.findViewById<View>(R.id.widgetWeather)

        if (weatherContainer != null) {

            weatherContainer.findViewById<TextView>(R.id.tvTemperature)?.text =
                "${weather.temperature}°C"

            weatherContainer.findViewById<TextView>(R.id.tvHumidity)?.text =
                "Humidity: ${weather.humidity}%"

            weatherContainer.findViewById<TextView>(R.id.tvRainfall)?.text =
                "Rain: ${weather.rainfall}mm"

            weatherContainer.findViewById<TextView>(R.id.tvCondition)?.text =
                weather.condition

            val iconView = weatherContainer.findViewById<ImageView>(R.id.ivWeatherIcon)

            iconView.imageTintList = null
            iconView?.setImageResource(
                when {
                    weather.rainfall > 20 -> R.drawable.img_5
                    weather.temperature > 35 -> R.drawable.ic_sun
                    weather.condition.contains("Sunny", true) -> R.drawable.ic_sun
                    weather.condition.contains("Cloudy", true) -> R.drawable.ic_partly_cloud
                    weather.condition.contains("Rain", true) -> R.drawable.img_5
                    else -> R.drawable.ic_partly_cloud
                }
            )

            weatherContainer.findViewById<android.widget.ImageButton>(R.id.btnRefreshWeather)
                ?.setOnClickListener {
                    viewModel.refreshData()
                }
        }
    }

    private fun startDataSimulation() {
        viewModel.startSimulation()
    }

    abstract fun onDashboardCreated()

    override fun onDestroyView() {
        viewModel.stopSimulation()
        super.onDestroyView()
    }
}
