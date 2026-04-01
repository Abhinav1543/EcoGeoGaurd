package com.example.ecogeoguard.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.ecogeoguard.R
import com.example.ecogeoguard.databinding.FragmentCompleteRegistrationBinding
import com.example.ecogeoguard.viewmodel.RegistrationFlowViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CompleteRegistrationFragment : Fragment() {

    private var _binding: FragmentCompleteRegistrationBinding? = null
    private val binding get() = _binding!!

    private val flowViewModel: RegistrationFlowViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompleteRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRoleSpinner()
        prefillData()

        binding.btnCompleteRegistration.setOnClickListener {
            saveUserToFirestore()
        }
    }

    /* ---------------- ROLE SPINNER ---------------- */

    private fun setupRoleSpinner() {
        val roles = listOf(
            "FARMER",
            "LIVESTOCK_OWNER",
            "DISASTER_TEAM",
            "GOVERNMENT",
            "ADMIN"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            roles
        )
        binding.spinnerRole.adapter = adapter
    }

    /* ---------------- PREFILL DATA ---------------- */

    private fun prefillData() {
        val data = flowViewModel.data.value ?: return

        // Phone already verified → show only
        binding.etPhone.setText(data.phone)
        binding.etPhone.isEnabled = false

        // Set selected role
        val roleIndex = when (data.role) {
            "FARMER" -> 0
            "LIVESTOCK_OWNER" -> 1
            "DISASTER_TEAM" -> 2
            "GOVERNMENT" -> 3
            "ADMIN" -> 4
            else -> 0
        }
        binding.spinnerRole.setSelection(roleIndex)
    }

    /* ---------------- FIRESTORE SAVE ---------------- */

    private fun saveUserToFirestore() {
        val data = flowViewModel.data.value
        val user = FirebaseAuth.getInstance().currentUser

        if (data == null || user == null) {
            Toast.makeText(
                requireContext(),
                "Session expired. Please register again.",
                Toast.LENGTH_LONG
            ).show()
            findNavController().popBackStack(R.id.registerFragment, false)
            return
        }

        binding.btnCompleteRegistration.isEnabled = false

        val selectedRole = binding.spinnerRole.selectedItem.toString()

        val userMap = hashMapOf(
            "uid" to user.uid,
            "name" to data.name,
            "email" to data.email,
            "phone" to data.phone,
            "role" to selectedRole,
            "emailVerified" to true,
            "phoneVerified" to true,
            "status" to "ACTIVE",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastLogin" to null
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Registration completed successfully!",
                    Toast.LENGTH_LONG
                ).show()

                // 🔐 Logout → fresh login
                FirebaseAuth.getInstance().signOut()

                findNavController().navigate(
                    R.id.action_completeRegistrationFragment_to_loginFragment
                )
            }
            .addOnFailureListener {
                binding.btnCompleteRegistration.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    it.localizedMessage ?: "Failed to save user data",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
