package com.example.ecogeoguard.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecogeoguard.utils.ValidationUtils
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val prefs: SharedPreferences
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    /* ===================== LOGIN STATE ===================== */

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> get() = _loginState

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val role: String, val name: String) : LoginState()
        data class Error(val message: String) : LoginState()
        object EmailNotVerified : LoginState()
        object RequiresVerification : LoginState()
        object LoggedOut : LoginState()
        object AskEnableBiometric : LoginState()
    }

    /* ===================== NORMAL LOGIN ===================== */

    fun loginWithEmailPassword(loginId: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val validation = ValidationUtils.validateLoginInput(loginId, password)
                if (!validation.first) {
                    _loginState.value = LoginState.Error(validation.second)
                    return@launch
                }

                val isEmail = ValidationUtils.isValidEmail(loginId)
                val isPhone = ValidationUtils.isValidPhoneNumber(loginId)

                if (!isEmail && !isPhone) {
                    _loginState.value = LoginState.Error("Invalid email or phone number")
                    return@launch
                }

                if (isPhone) {
                    loginWithPhone(loginId, password)
                    return@launch
                }

                val result =
                    auth.signInWithEmailAndPassword(loginId, password).await()
                val user = result.user
                    ?: run {
                        _loginState.value = LoginState.Error("Login failed")
                        return@launch
                    }

                if (!user.isEmailVerified) {
                    _loginState.value = LoginState.EmailNotVerified
                    return@launch
                }

                handlePostLogin(user.uid, askBiometric = true)

            } catch (e: Exception) {
                handleLoginError(e)
            }
        }
    }

    /* ===================== PHONE LOGIN ===================== */

    private suspend fun loginWithPhone(phone: String, password: String) {
        try {
            val query = db.collection("users")
                .whereEqualTo("phone", phone)
                .limit(1)
                .get()
                .await()

            if (query.isEmpty) {
                _loginState.value = LoginState.Error("Phone number not registered")
                return
            }

            val email = query.documents.first().getString("email") ?: ""
            if (email.isEmpty()) {
                _loginState.value = LoginState.Error("No email linked with this phone")
                return
            }

            val result =
                auth.signInWithEmailAndPassword(email, password).await()
            handlePostLogin(result.user!!.uid, askBiometric = true)

        } catch (e: Exception) {
            handleLoginError(e)
        }
    }

    /* ===================== POST LOGIN ===================== */

    private suspend fun handlePostLogin(
        uid: String,
        askBiometric: Boolean
    ) {
        val doc = db.collection("users").document(uid).get().await()

        if (!doc.exists()) {
            _loginState.value = LoginState.RequiresVerification
            return
        }

        val emailVerified = doc.getBoolean("emailVerified") ?: false
        val phoneVerified = doc.getBoolean("phoneVerified") ?: false

        if (!emailVerified || !phoneVerified) {
            _loginState.value = LoginState.RequiresVerification
            return
        }

        val role = doc.getString("role") ?: "USER"
        val name = doc.getString("name") ?: "User"

        db.collection("users")
            .document(uid)
            .update("lastLogin", System.currentTimeMillis())

        if (askBiometric && !isBiometricEnabled()) {
            _loginState.value = LoginState.AskEnableBiometric
        } else {
            _loginState.value = LoginState.Success(role, name)
        }
    }

    /* ===================== BIOMETRIC ===================== */

    fun enableBiometric(enable: Boolean) {
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enable)
            .apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun loginWithBiometric() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user == null) {
                _loginState.value =
                    LoginState.Error("Please login once using email & password")
                return@launch
            }

            try {
                handlePostLogin(user.uid, askBiometric = false)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Biometric authentication failed")
            }
        }
    }

    /* ===================== AUTO LOGIN (FAST) ===================== */

    fun autoLoginFast() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user == null) {
                _loginState.value = LoginState.LoggedOut
                return@launch
            }

            try {
                handlePostLogin(user.uid, askBiometric = false)
            } catch (e: Exception) {
                _loginState.value = LoginState.LoggedOut
            }
        }
    }

    /* ===================== RESET PASSWORD ===================== */

    fun resetPassword(emailOrPhone: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // CASE 1: EMAIL
                if (ValidationUtils.isValidEmail(emailOrPhone)) {
                    auth.sendPasswordResetEmail(emailOrPhone).await()
                    _loginState.value =
                        LoginState.Success("RESET", "EMAIL")
                    return@launch
                }

                // CASE 2: PHONE
                if (ValidationUtils.isValidPhoneNumber(emailOrPhone)) {
                    val query = db.collection("users")
                        .whereEqualTo("phone", emailOrPhone)
                        .limit(1)
                        .get()
                        .await()

                    if (query.isEmpty) {
                        _loginState.value =
                            LoginState.Error("Phone number not registered")
                        return@launch
                    }

                    val email =
                        query.documents.first().getString("email")

                    if (email.isNullOrEmpty()) {
                        _loginState.value =
                            LoginState.Error("No email linked with this phone")
                        return@launch
                    }

                    auth.sendPasswordResetEmail(email).await()
                    _loginState.value =
                        LoginState.Success("RESET", "PHONE")
                    return@launch
                }

                _loginState.value =
                    LoginState.Error("Enter valid email or phone number")

            } catch (e: Exception) {
                _loginState.value =
                    LoginState.Error(e.message ?: "Password reset failed")
            }
        }
    }


    /* ===================== LOGOUT ===================== */

    fun logout() {
        auth.signOut()
        prefs.edit().clear().apply()
        _loginState.value = LoginState.LoggedOut
    }

    /* ===================== ERROR HANDLING ===================== */

    private fun handleLoginError(e: Exception) {
        val msg = when (e) {
            is FirebaseAuthInvalidCredentialsException -> "Invalid credentials"
            is FirebaseAuthInvalidUserException -> "Account not found"
            is FirebaseNetworkException -> "Network error"
            is FirebaseException -> e.message ?: "Firebase error"
            else -> "Login failed"
        }
        _loginState.value = LoginState.Error(msg)
    }
}
