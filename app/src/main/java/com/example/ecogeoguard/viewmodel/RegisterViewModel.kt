package com.example.ecogeoguard.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecogeoguard.utils.ValidationUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel : ViewModel() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /* ---------------- VALIDATION STATE ---------------- */

    private val _validationState = MutableLiveData<ValidationState>()
    val validationState: LiveData<ValidationState> get() = _validationState

    sealed class ValidationState {
        data class Valid(val field: String) : ValidationState()
        data class Invalid(val field: String, val message: String) : ValidationState()
    }

    /* ---------------- FIELD VALIDATION ---------------- */

    fun validateField(field: String, value: String): Boolean {
        return when (field) {

            "name" -> {
                val valid = ValidationUtils.isValidName(value)
                if (valid) {
                    _validationState.value = ValidationState.Valid("name")
                } else {
                    _validationState.value =
                        ValidationState.Invalid("name", "Name must be 2–50 characters")
                }
                valid
            }

            "email" -> {
                val valid = ValidationUtils.isValidEmail(value)
                if (valid) {
                    _validationState.value = ValidationState.Valid("email")
                } else {
                    _validationState.value =
                        ValidationState.Invalid("email", "Invalid email format")
                }
                valid
            }

            "phone" -> {
                val valid = ValidationUtils.isValidPhoneNumber(value)
                if (valid) {
                    _validationState.value = ValidationState.Valid("phone")
                } else {
                    _validationState.value =
                        ValidationState.Invalid("phone", "Invalid phone number (10 digits)")
                }
                valid
            }

            "password" -> {
                val result = ValidationUtils.validatePassword(value)
                if (result.first) {
                    _validationState.value = ValidationState.Valid("password")
                } else {
                    _validationState.value =
                        ValidationState.Invalid("password", result.second)
                }
                result.first
            }

            else -> true
        }
    }

    /* ---------------- EMAIL AVAILABILITY ---------------- */

    fun checkEmailAvailability(email: String) {
        viewModelScope.launch {
            try {
                val query = db.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .await()

                if (query.isEmpty) {
                    _validationState.value =
                        ValidationState.Valid("email_availability")
                } else {
                    _validationState.value =
                        ValidationState.Invalid("email", "Email already registered")
                }
            } catch (_: Exception) {
                // silently ignore (network hiccup etc.)
            }
        }
    }

    /* ---------------- PHONE AVAILABILITY ---------------- */

    fun checkPhoneAvailability(phone: String) {
        viewModelScope.launch {
            try {
                val query = db.collection("users")
                    .whereEqualTo("phone", phone)
                    .limit(1)
                    .get()
                    .await()

                if (query.isEmpty) {
                    _validationState.value =
                        ValidationState.Valid("phone_availability")
                } else {
                    _validationState.value =
                        ValidationState.Invalid("phone", "Phone number already registered")
                }
            } catch (_: Exception) {
                // silently ignore
            }
        }
    }
}
