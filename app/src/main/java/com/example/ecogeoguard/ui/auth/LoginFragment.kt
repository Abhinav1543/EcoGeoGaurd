package com.example.ecogeoguard.ui.auth

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ecogeoguard.R
import com.example.ecogeoguard.databinding.FragmentLoginEnhancedBinding
import com.example.ecogeoguard.utils.QuoteProvider
import com.example.ecogeoguard.viewmodel.AuthViewModel
import com.example.ecogeoguard.viewmodel.AuthViewModelFactory
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginEnhancedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(requireContext())
    }

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginEnhancedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBiometric()
        setupUI()
        setupTextWatchers()
        setupObservers()
        setupAnimations()

        // 🚀 FAST AUTO LOGIN (NO DELAY)
        viewModel.autoLoginFast()
    }

    /* ========================= UI ========================= */

    private fun setupUI() {
        val quote = QuoteProvider.getRandomQuote()
        binding.tvQuote.text = "\"${quote.text}\""
        binding.tvQuoteAuthor.text = "- ${quote.author}"

        binding.btnLogin.setOnClickListener { performLogin() }

        binding.btnBiometric.setOnClickListener {
            if (viewModel.isBiometricEnabled()) {
                showBiometricPrompt()
            } else {
                showSnackbar("Enable biometric after login first")
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.forgotPasswordFragment)
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }

    /* ===================== TEXT WATCHERS ===================== */

    private fun setupTextWatchers() {
        // 🔧 FIX keyboard issue (email + phone both)
        binding.etLoginId.inputType =
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        binding.etLoginId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateLoginId(s.toString())
            }
        })

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
        })
    }

    private fun validateLoginId(value: String) {
        when {
            value.isEmpty() ->
                setError(binding.textInputLayoutLogin, "Email or phone required")

            value.contains("@") &&
                    android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches() ->
                clearError(binding.textInputLayoutLogin)

            value.matches("[0-9]{10}".toRegex()) ->
                clearError(binding.textInputLayoutLogin)

            else ->
                setError(binding.textInputLayoutLogin, "Invalid email or phone")
        }
    }

    private fun validatePassword(value: String) {
        when {
            value.isEmpty() ->
                setError(binding.textInputLayoutPassword, "Password required")
            value.length < 6 ->
                setError(binding.textInputLayoutPassword, "Min 6 characters")
            else ->
                clearError(binding.textInputLayoutPassword)
        }
    }

    private fun setError(layout: TextInputLayout, msg: String) {
        layout.error = msg
        layout.isErrorEnabled = true
    }

    private fun clearError(layout: TextInputLayout) {
        layout.error = null
        layout.isErrorEnabled = false
    }

    /* ========================= LOGIN ========================= */

    private fun performLogin() {
        hideKeyboard()
        showLoading(true)

        viewModel.loginWithEmailPassword(
            binding.etLoginId.text.toString().trim(),
            binding.etPassword.text.toString().trim()
        )
    }

    /* ===================== BIOMETRIC ===================== */

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(requireContext())

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    viewModel.loginWithBiometric()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    showSnackbar(errString.toString())
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Use fingerprint to login")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun showBiometricPrompt() {
        val manager = BiometricManager.from(requireContext())
        if (manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BiometricManager.BIOMETRIC_SUCCESS
        ) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            showSnackbar("Biometric not supported")
        }
    }

    /* ===================== OBSERVERS ===================== */

    private fun setupObservers() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {

                is AuthViewModel.LoginState.Loading ->
                    showLoading(true)

                is AuthViewModel.LoginState.AskEnableBiometric -> {
                    showLoading(false)
                    showBiometricOptInDialog()
                }

                is AuthViewModel.LoginState.Success -> {
                    showLoading(false)
                    navigateToDashboard(state.role, state.name)
                }

                is AuthViewModel.LoginState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }

                is AuthViewModel.LoginState.LoggedOut -> {
                    showLoading(false)
                }

                else -> {}
            }
        }
    }

    private fun showBiometricOptInDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Enable Fingerprint?")
            .setMessage("Use fingerprint for faster login next time.")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.enableBiometric(true)
                viewModel.loginWithBiometric()
            }
            .setNegativeButton("No") { _, _ ->
                viewModel.enableBiometric(false)
                viewModel.loginWithBiometric()
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToDashboard(role: String, name: String) {
        when (role.uppercase()) {
            "ADMIN" -> {
                findNavController().navigate(R.id.action_loginFragment_to_adminDashboardFragment)
            }
            "FARMER" -> {
                findNavController().navigate(R.id.action_loginFragment_to_farmerDashboardFragment)
            }
            "LIVESTOCK_OWNER" ->{
                findNavController().navigate(R.id.action_loginFragment_to_LiveStockDashboardFragment)
            }
            "DISASTER_TEAM" ->{
                findNavController().navigate(R.id.action_loginFragment_to_disasterDashboardFragment)
            }
            "GOVERNMENT" ->{
                findNavController().navigate(R.id.action_loginFragment_to_govtDashboardFragment)
            }
            else -> {
                // Navigate to regular Dashboard with role and name
                val action = LoginFragmentDirections.actionLoginFragmentToDashboardFragment(
                    userRole = role,
                    userName = name
                )
                findNavController().navigate(action)
            }
        }
    }

    /* ===================== HELPERS ===================== */

    private fun showLoading(show: Boolean) {
        binding.progressIndicator.isVisible = show
        binding.btnLogin.isEnabled = !show
        binding.btnBiometric.isEnabled = !show
    }

    private fun showError(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(R.color.error, null))
            .show()

        binding.loginCard.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
        )
    }

    private fun showSnackbar(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
    }

    private fun setupAnimations() {
        binding.loginCard.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        )
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                    as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
