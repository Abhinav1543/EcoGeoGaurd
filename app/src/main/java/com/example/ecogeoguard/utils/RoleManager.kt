package com.example.ecogeoguard.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.ecogeoguard.data.model.UserRole  // FIXED IMPORT
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "ecogeoguard_prefs"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loginUser(
        userId: String,
        userName: String,
        userEmail: String,
        userPhone: String,
        role: UserRole
    ) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, userEmail)
            putString(KEY_USER_PHONE, userPhone)
            putString(KEY_USER_ROLE, role.name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun logoutUser() {
        prefs.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_PHONE)
            remove(KEY_USER_ROLE)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) &&
                prefs.getString(KEY_USER_ID, null) != null
    }

    fun getCurrentRole(): UserRole {
        val roleName = prefs.getString(KEY_USER_ROLE, UserRole.FARMER.name)
        return try {
            UserRole.valueOf(roleName ?: UserRole.FARMER.name)
        } catch (e: IllegalArgumentException) {
            UserRole.FARMER
        }
    }

    fun getCurrentUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun getCurrentUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getCurrentUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun getCurrentUserPhone(): String? {
        return prefs.getString(KEY_USER_PHONE, null)
    }

    fun isAdmin(): Boolean {
        return getCurrentRole() == UserRole.ADMIN
    }

    fun isFarmer(): Boolean {
        return getCurrentRole() == UserRole.FARMER
    }

    fun isLivestockOwner(): Boolean {
        return getCurrentRole() == UserRole.LIVESTOCK_OWNER
    }

    fun isDisasterTeam(): Boolean {
        return getCurrentRole() == UserRole.DISASTER_TEAM
    }

    fun isGovernment(): Boolean {
        return getCurrentRole() == UserRole.GOVERNMENT
    }

    fun updateUserProfile(name: String? = null, phone: String? = null) {
        prefs.edit().apply {
            name?.let { putString(KEY_USER_NAME, it) }
            phone?.let { putString(KEY_USER_PHONE, it) }
            apply()
        }
    }

    fun clearUserInfo() {
        prefs.edit().clear().apply()
    }
}