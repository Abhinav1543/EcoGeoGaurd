package com.example.ecogeoguard.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.ecogeoguard.R
import com.example.ecogeoguard.databinding.FragmentForgotPasswordBinding
import com.example.ecogeoguard.viewmodel.AuthViewModel
import com.example.ecogeoguard.viewmodel.AuthViewModelFactory
import com.google.android.material.snackbar.Snackbar

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!


    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(requireContext())
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.tvLogin.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSubmit.setOnClickListener {
            val input = binding.etEmailPhone.text.toString().trim()

            if (input.isEmpty()) {
                binding.textInputLayoutEmailPhone.error =
                    "Please enter registered email"
                return@setOnClickListener
            }

            hideKeyboard()
            viewModel.resetPassword(input)
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.LoginState.Loading -> {
                    showLoading(true)
                }

                is AuthViewModel.LoginState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }

                // ✅ Password reset success ka signal
                is AuthViewModel.LoginState.Success -> {
                    showLoading(false)
                    showSuccessDialog()
                }

                else -> {
                    showLoading(false)
                }
            }
        }
    }

    private fun showSuccessDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset Link Sent")
            .setMessage(
                "Password reset instructions have been sent to your registered email."
            )
            .setPositiveButton("OK") { _, _ ->
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressIndicator.isVisible = show
        binding.btnSubmit.isEnabled = !show
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(R.color.error, null))
            .show()
    }

    private fun hideKeyboard() {
        val imm = android.content.Context.INPUT_METHOD_SERVICE
        val inputMethodManager =
            requireContext().getSystemService(imm)
                    as android.view.inputmethod.InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            binding.root.windowToken, 0
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
