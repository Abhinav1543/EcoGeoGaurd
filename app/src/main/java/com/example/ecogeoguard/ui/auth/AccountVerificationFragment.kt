package com.example.ecogeoguard.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.ecogeoguard.R
import com.example.ecogeoguard.databinding.FragmentAccountVerificationBinding
import com.example.ecogeoguard.viewmodel.RegistrationFlowViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class AccountVerificationFragment : Fragment() {

    private var _binding: FragmentAccountVerificationBinding? = null
    private val binding get() = _binding!!

    private val flowViewModel: RegistrationFlowViewModel by activityViewModels()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private var verificationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        refreshEmailStatus()
        updateUIState()
    }

    /* ---------------- UI SETUP ---------------- */

    private fun setupUI() {

        // ✅ REAL EMAIL CHECK (Firebase backend)
        binding.btnCheckEmail.setOnClickListener {
            refreshEmailStatus()
        }

        // 📱 SEND OTP (Firebase TEST number – FREE)
        binding.btnSendOtp.setOnClickListener {
            val phone = flowViewModel.data.value?.phone ?: ""
            if (phone.isEmpty()) {
                toast("Phone number missing")
                return@setOnClickListener
            }
            sendOtp(phone)
        }

        // 📱 VERIFY OTP
        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (otp.length != 6 || verificationId == null) {
                toast("Enter valid 6-digit OTP")
                return@setOnClickListener
            }
            verifyOtp(otp)
        }

        // ▶️ CONTINUE
        binding.btnContinue.setOnClickListener {
            if (flowViewModel.emailVerified.value == true &&
                flowViewModel.phoneVerified.value == true
            ) {
                findNavController().navigate(
                    R.id.action_accountVerificationFragment_to_completeRegistrationFragment
                )
            } else {
                toast("Please verify Email and Phone first")
            }
        }
    }

    /* ---------------- EMAIL VERIFICATION ---------------- */

    private fun refreshEmailStatus() {
        val user = auth.currentUser
        if (user == null) {
            toast("User not logged in")
            return
        }

        user.reload().addOnSuccessListener {
            if (user.isEmailVerified) {
                flowViewModel.emailVerified.value = true
                toast("Email verified successfully")
            } else {
                flowViewModel.emailVerified.value = false
                toast("Email not verified yet. Check inbox.")
            }
            updateUIState()
        }.addOnFailureListener {
            toast("Failed to refresh email status")
        }
    }

    /* ---------------- PHONE OTP (FREE TEST MODE) ---------------- */

    private fun sendOtp(phone: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phone") // MUST match test number
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    flowViewModel.phoneVerified.value = true
                    updateUIState()
                    toast("Phone auto verified")
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    toast(e.localizedMessage ?: "OTP failed")
                }

                override fun onCodeSent(
                    verId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = verId
                    toast("OTP sent (use test OTP)")
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOtp(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)

        auth.currentUser
            ?.linkWithCredential(credential)
            ?.addOnSuccessListener {
                flowViewModel.phoneVerified.value = true
                updateUIState()
                toast("Phone verified successfully")
            }
            ?.addOnFailureListener {
                toast("Invalid OTP")
            }
    }

    /* ---------------- UI STATE ---------------- */

    private fun updateUIState() {

        // Email
        if (flowViewModel.emailVerified.value == true) {
            binding.tvEmailStatus.text = "Email: Verified"
            binding.tvEmailStatus.setTextColor(
                resources.getColor(android.R.color.holo_green_dark, null)
            )
        } else {
            binding.tvEmailStatus.text = "Email: Not Verified"
            binding.tvEmailStatus.setTextColor(
                resources.getColor(android.R.color.holo_red_dark, null)
            )
        }

        // Phone
        if (flowViewModel.phoneVerified.value == true) {
            binding.tvPhoneStatus.text = "Phone: Verified"
            binding.tvPhoneStatus.setTextColor(
                resources.getColor(android.R.color.holo_green_dark, null)
            )
        } else {
            binding.tvPhoneStatus.text = "Phone: Not Verified"
            binding.tvPhoneStatus.setTextColor(
                resources.getColor(android.R.color.holo_red_dark, null)
            )
        }

        binding.btnContinue.isEnabled =
            flowViewModel.emailVerified.value == true &&
                    flowViewModel.phoneVerified.value == true
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
