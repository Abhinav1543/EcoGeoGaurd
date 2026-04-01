package com.example.ecogeoguard.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.ecogeoguard.R
import com.example.ecogeoguard.databinding.FragmentResetPasswordBinding
import com.example.ecogeoguard.viewmodel.AuthViewModel

class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Reset password button
        binding.btnResetPassword.setOnClickListener {
            resetPassword()
        }

        // Login link
        binding.tvLogin.setOnClickListener {
            findNavController().popBackStack(R.id.loginFragment, false)
        }
    }

    private fun resetPassword() {
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Validation
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        binding.btnResetPassword.isEnabled = false
        binding.btnResetPassword.text = "Resetting..."

        // In real app, you would call Firebase to reset password
        // For now, simulate success
        Toast.makeText(requireContext(), "Password reset successful!", Toast.LENGTH_LONG).show()

        // Navigate back to login
        findNavController().popBackStack(R.id.loginFragment, false)
    }

    private fun setupObservers() {
        // Add ViewModel observers if needed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}