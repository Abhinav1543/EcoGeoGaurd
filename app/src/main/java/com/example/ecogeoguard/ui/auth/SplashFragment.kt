package com.example.ecogeoguard.ui.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieDrawable
import com.example.ecogeoguard.R
import com.example.ecogeoguard.databinding.FragmentSplashBinding
import com.example.ecogeoguard.utils.QuoteProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var isLoadingComplete = false
    private var hasNavigated = false   // 🔥 IMPORTANT

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupAnimations()
        startLoadingSequence()
    }

    private fun setupUI() {
        val (quote, author) = QuoteProvider.getRandomQuote()
        binding.tvQuote.text = "\"$quote\""
        binding.tvAuthor.text = "- $author"

        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        binding.tvCopyright.text =
            "© $year EcoGeoGuard | All Rights Reserved"

        binding.tvVersion.text = "v${getAppVersion()}"

        binding.lottieSplash.repeatCount = LottieDrawable.INFINITE
        binding.lottieSplash.playAnimation()
    }

    private fun setupAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        val slideInLeft =
            AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_left)
        val slideInRight =
            AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right)

        handler.postDelayed({ binding.tvAppName.startAnimation(fadeIn) }, 300)
        handler.postDelayed({ binding.tvTagline.startAnimation(slideUp) }, 600)
        handler.postDelayed({ binding.quoteCard.startAnimation(slideInLeft) }, 900)
        handler.postDelayed({ binding.progressBar.startAnimation(fadeIn) }, 1200)
        handler.postDelayed(
            { binding.loadingContainer.startAnimation(slideInRight) },
            1500
        )
    }

    private fun startLoadingSequence() {
        CoroutineScope(Dispatchers.Main).launch {
            simulateLoadingTasks()
            checkAuthentication() // 🔥 ONLY ONCE
        }
    }

    private suspend fun simulateLoadingTasks() {
        val tasks = listOf(
            "Initializing security protocols..." to 800L,
            "Loading environmental data..." to 1000L,
            "Syncing with satellite networks..." to 1200L,
            "Preparing dashboard..." to 800L,
            "Finalizing setup..." to 600L
        )

        for ((task, time) in tasks) {
            binding.tvLoadingText.text = task
            binding.progressBar.progress += (100 / tasks.size)
            delay(time)
        }

        binding.tvLoadingText.text = "Ready!"
        binding.progressBar.progress = 100
        isLoadingComplete = true
    }

    /* ===================================================== */
    /* ================= FIXED AUTO LOGIN ================== */
    /* ===================================================== */
    private fun checkAuthentication() {
        if (!isLoadingComplete || hasNavigated) return

        val user = FirebaseAuth.getInstance().currentUser
        navigateBasedOnAuth(user)
    }

    private fun navigateBasedOnAuth(user: FirebaseUser?) {
        if (!isAdded || _binding == null || hasNavigated) return
        hasNavigated = true

        binding.lottieSplash.cancelAnimation()

        if (user == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        if (!user.isEmailVerified) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        // 🔥 FETCH ROLE & NAME (REAL AUTO LOGIN)
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "USER"
                val name = doc.getString("name") ?: "User"

                // 🔥 CHECK IF ADMIN AND NAVIGATE TO ADMIN DASHBOARD
                if (role.uppercase() == "ADMIN") {
                    // Navigate to Admin Dashboard
                    findNavController().navigate(R.id.action_splashFragment_to_adminDashboardFragment)
                } else if(role.uppercase() == "FARMER") {

                    findNavController().navigate(R.id.action_splashFragment_to_farmerDashboardFragment)
                }
                else if(role.uppercase() == "LIVESTOCK_OWNER") {

                    findNavController().navigate(R.id.action_splashFragment_to_LiveStockDashboardFragment)
                }
                else if(role.uppercase() == "DISASTER_TEAM") {

                    findNavController().navigate(R.id.action_splashFragment_to_disasterDashboardFragment)
                }
                else if(role.uppercase() == "GOVERNMENT") {

                    findNavController().navigate(R.id.action_splashFragment_to_govtDashboardFragment)
                }

                else {
                    // Navigate to regular Dashboard with role and name
                    val action =
                        SplashFragmentDirections
                            .actionSplashFragmentToDashboardFragment(
                                userRole = role,
                                userName = name
                            )
                    findNavController().navigate(action)
                }
            }
            .addOnFailureListener {
                findNavController().navigate(R.id.loginFragment)
            }
    }

    private fun getAppVersion(): String {
        return try {
            requireContext()
                .packageManager
                .getPackageInfo(requireContext().packageName, 0)
                .versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        binding.lottieSplash.cancelAnimation()
        _binding = null
    }
}
