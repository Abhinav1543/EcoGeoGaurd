package com.example.ecogeoguard.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ecogeoguard.R
import com.example.ecogeoguard.adapters.RoleAdapter
import com.example.ecogeoguard.data.model.UserRole
import com.example.ecogeoguard.databinding.FragmentRegisterEnhancedBinding
import com.example.ecogeoguard.viewmodel.RegisterViewModel
import com.example.ecogeoguard.viewmodel.RegistrationFlowViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterEnhancedBinding? = null
    private val binding get() = _binding!!

    // 🔹 Only validation logic
    private val validationViewModel: RegisterViewModel by viewModels()

    // 🔹 Shared registration flow
    private val flowViewModel: RegistrationFlowViewModel by activityViewModels()

    private lateinit var roleAdapter: RoleAdapter
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterEnhancedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupTextWatchers()
        setupRoleSelection()
        setupObservers()
        setupAnimations()
    }

    /* ---------------- UI SETUP ---------------- */

    private fun setupUI() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvLogin.setOnClickListener { findNavController().popBackStack() }

        binding.textInputLayoutEmail.setEndIconOnClickListener {
            checkEmailAvailability()
        }

        binding.textInputLayoutPhone.setEndIconOnClickListener {
            validatePhoneOnly()
        }

        binding.cbTerms.setOnCheckedChangeListener { _, _ ->
            updateRegisterButtonState()
        }

        // 🔥 REAL REGISTRATION (Firebase user created HERE)
        binding.btnRegister.setOnClickListener {
            if (!binding.cbTerms.isChecked) {
                showSnackbar("Please accept Terms & Conditions")
                return@setOnClickListener
            }

            if (!validateAllFields()) {
                showSnackbar("Please complete all details correctly")
                return@setOnClickListener
            }

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            showLoading(true)

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->

                    // ✅ SEND REAL VERIFICATION EMAIL
                    result.user?.sendEmailVerification()

                    saveDataToFlowViewModel()

                    flowViewModel.emailVerified.value = false
                    flowViewModel.phoneVerified.value = false
                    flowViewModel.currentStep.value =
                        RegistrationFlowViewModel.Step.VERIFICATION

                    showLoading(false)

                    findNavController().navigate(
                        R.id.action_registerFragment_to_accountVerificationFragment
                    )
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showSnackbar(e.localizedMessage ?: "Registration failed")
                }
        }
    }

    /* ---------------- TEXT WATCHERS ---------------- */

    private fun setupTextWatchers() {
        binding.etName.addTextChangedListener(createWatcher("name"))
        binding.etEmail.addTextChangedListener(createWatcher("email"))
        binding.etPhone.addTextChangedListener(createWatcher("phone"))
        binding.etPassword.addTextChangedListener(createWatcher("password"))
        binding.etConfirmPassword.addTextChangedListener(createWatcher("confirmPassword"))
    }

    private fun createWatcher(field: String): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateField(field, s.toString())
            }
        }
    }

    /* ---------------- VALIDATION ---------------- */

    private fun validateField(field: String, value: String) {
        when (field) {
            "confirmPassword" -> {
                val password = binding.etPassword.text.toString()
                binding.textInputLayoutConfirmPassword.error =
                    if (value == password) null else "Passwords don't match"
            }
            else -> {
                val isValid = validationViewModel.validateField(field, value)
                when (field) {
                    "email" -> binding.textInputLayoutEmail.isEndIconVisible = isValid
                    "phone" -> binding.textInputLayoutPhone.isEndIconVisible = isValid
                }
            }
        }
        updateRegisterButtonState()
    }

    private fun validateAllFields(): Boolean {
        val name = binding.etName.text.toString()
        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        return name.isNotEmpty()
                && validationViewModel.validateField("email", email)
                && validationViewModel.validateField("phone", phone)
                && validationViewModel.validateField("password", password)
                && password == confirmPassword
                && roleAdapter.selectedPosition != -1
    }

    private fun updateRegisterButtonState() {
        binding.btnRegister.isEnabled =
            validateAllFields() && binding.cbTerms.isChecked
    }

    /* ---------------- ROLE SELECTION ---------------- */

    private fun setupRoleSelection() {
        val roles = listOf(
            UserRole.ADMIN,
            UserRole.FARMER,
            UserRole.LIVESTOCK_OWNER,
            UserRole.DISASTER_TEAM,
            UserRole.GOVERNMENT
        )

        roleAdapter = RoleAdapter(roles) { _, _ ->
            updateRegisterButtonState()
        }

        binding.rvRoles.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvRoles.adapter = roleAdapter
    }

    /* ---------------- FLOW DATA ---------------- */

    private fun saveDataToFlowViewModel() {
        flowViewModel.data.value =
            RegistrationFlowViewModel.RegistrationData(
                name = binding.etName.text.toString().trim(),
                email = binding.etEmail.text.toString().trim(),
                phone = binding.etPhone.text.toString().trim(),
                password = binding.etPassword.text.toString(),
                role = getRoleFromPosition(roleAdapter.selectedPosition)
            )
    }

    private fun getRoleFromPosition(position: Int): String {
        return when (position) {
            0 -> "FARMER"
            1 -> "LIVESTOCK_OWNER"
            2 -> "AUTHORITY"
            3 -> "GOVERNMENT"
            4 -> "ADMIN"
            else -> "FARMER"
        }
    }

    /* ---------------- AVAILABILITY ---------------- */

    private fun checkEmailAvailability() {
        val email = binding.etEmail.text.toString()
        if (validationViewModel.validateField("email", email)) {
            validationViewModel.checkEmailAvailability(email)
        }
    }

    private fun validatePhoneOnly() {
        val phone = binding.etPhone.text.toString()
        validationViewModel.validateField("phone", phone)
    }

    /* ---------------- OBSERVERS ---------------- */

    private fun setupObservers() {
        validationViewModel.validationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RegisterViewModel.ValidationState.Valid -> clearError(state.field)
                is RegisterViewModel.ValidationState.Invalid ->
                    showFieldError(state.field, state.message)
            }
        }
    }

    private fun clearError(field: String) {
        when (field) {
            "name" -> binding.textInputLayoutName.error = null
            "email" -> binding.textInputLayoutEmail.error = null
            "phone" -> binding.textInputLayoutPhone.error = null
            "password" -> binding.textInputLayoutPassword.error = null
            "confirmPassword" -> binding.textInputLayoutConfirmPassword.error = null
        }
    }

    private fun showFieldError(field: String, message: String) {
        when (field) {
            "name" -> binding.textInputLayoutName.error = message
            "email" -> binding.textInputLayoutEmail.error = message
            "phone" -> binding.textInputLayoutPhone.error = message
            "password" -> binding.textInputLayoutPassword.error = message
            "confirmPassword" -> binding.textInputLayoutConfirmPassword.error = message
            "role" -> showSnackbar(message)
        }
    }

    /* ---------------- UI HELPERS ---------------- */

    private fun showLoading(show: Boolean) {
        binding.progressIndicator.isVisible = show
        binding.btnRegister.isEnabled = !show
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun setupAnimations() {
        val slideIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right)
        binding.registrationCard.startAnimation(slideIn)

        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        binding.footerSection.startAnimation(fadeIn)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
