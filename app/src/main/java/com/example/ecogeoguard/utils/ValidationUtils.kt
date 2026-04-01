package com.example.ecogeoguard.utils

import android.util.Patterns
import java.util.regex.Pattern

object ValidationUtils {

    // Email validation
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Phone validation (Indian format)
    fun isValidPhoneNumber(phone: String): Boolean {
        val cleaned = phone.replace("[^0-9]".toRegex(), "")
        return cleaned.length == 10 && cleaned.matches("[6-9][0-9]{9}".toRegex())
    }

    // Name validation
    fun isValidName(name: String): Boolean {
        return name.length in 2..50 && name.matches("[a-zA-Z\\s.]*".toRegex())
    }

    // Password validation with multiple rules
    fun validatePassword(password: String): Pair<Boolean, String> {
        return when {
            password.length < 8 -> Pair(false, "Password must be at least 8 characters")
            !password.any { it.isUpperCase() } -> Pair(false, "Password must contain uppercase letter")
            !password.any { it.isLowerCase() } -> Pair(false, "Password must contain lowercase letter")
            !password.any { it.isDigit() } -> Pair(false, "Password must contain a number")
            !password.contains("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]".toRegex()) ->
                Pair(false, "Password must contain special character")
            else -> Pair(true, "Password is valid")
        }
    }

    // Login input validation
    fun validateLoginInput(identifier: String, password: String): Pair<Boolean, String> {
        return when {
            identifier.isEmpty() -> Pair(false, "Please enter email or phone number")
            password.isEmpty() -> Pair(false, "Please enter password")
            password.length < 6 -> Pair(false, "Password must be at least 6 characters")
            else -> Pair(true, "Valid")
        }
    }

    // OTP validation
    fun isValidOTP(otp: String): Boolean {
        return otp.length == 6 && otp.matches("[0-9]{6}".toRegex())
    }

    // Role validation
    fun isValidRole(role: String): Boolean {
        val validRoles = listOf("FARMER", "LIVESTOCK_OWNER", "ADMIN", "AUTHORITY", "GOVERNMENT")
        return role in validRoles
    }

    // Validate all registration fields
    fun validateRegistration(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (!isValidName(name)) {
            errors["name"] = "Enter valid name (2-50 characters)"
        }

        if (!isValidEmail(email)) {
            errors["email"] = "Enter valid email address"
        }

        if (!isValidPhoneNumber(phone)) {
            errors["phone"] = "Enter valid 10-digit phone number"
        }

        val passwordValidation = validatePassword(password)
        if (!passwordValidation.first) {
            errors["password"] = passwordValidation.second
        }

        if (password != confirmPassword) {
            errors["confirmPassword"] = "Passwords don't match"
        }

        return errors
    }
}