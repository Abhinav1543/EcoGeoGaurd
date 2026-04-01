package com.example.ecogeoguard.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class OtpViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    var verificationId: String? = null

    fun sendOtp(
        activity: Activity,
        phone: String,
        onSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phone")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    verifyOtp(credential, phone) {}
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    onError(e.message ?: "OTP failed")
                }

                override fun onCodeSent(
                    id: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = id
                    onSent()
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(
        credential: PhoneAuthCredential,
        phone: String,
        onSuccess: () -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                val uid = auth.currentUser!!.uid
                db.collection("users").document(uid)
                    .update(
                        mapOf(
                            "phone" to phone,
                            "phoneVerified" to true
                        )
                    )
                onSuccess()
            }
    }
}
