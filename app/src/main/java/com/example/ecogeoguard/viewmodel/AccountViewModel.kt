package com.example.ecogeoguard.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore

class AccountViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun registerUser(
        name: String,
        email: String,
        password: String,
        role: String,
        callback: (Boolean, String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val user = result.user
                if (user == null) {
                    callback(false, "Unexpected error occurred")
                    return@addOnSuccessListener
                }

                // Send verification
                user.sendEmailVerification()

                val map = hashMapOf(
                    "uid" to user.uid,
                    "name" to name,
                    "email" to email,
                    "role" to role,
                    "emailVerified" to false,
                    "phoneVerified" to false,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("users")
                    .document(user.uid)
                    .set(map)
                    .addOnSuccessListener {
                        callback(
                            true,
                            "Account created.\nVerification email sent.\nVerify email before login."
                        )
                    }
                    .addOnFailureListener {
                        callback(false, "Account created but data save failed")
                    }
            }
            .addOnFailureListener { e ->
                when (e) {
                    is FirebaseAuthUserCollisionException ->
                        callback(false, "This email is already registered")

                    is FirebaseAuthWeakPasswordException ->
                        callback(false, "Password too weak (min 6 characters)")

                    is FirebaseAuthInvalidCredentialsException ->
                        callback(false, "Invalid email format")

                    is FirebaseNetworkException ->
                        callback(false, "Network error. Check internet")

                    else ->
                        callback(false, "Error: ${e.localizedMessage}")
                }
            }
    }
}
