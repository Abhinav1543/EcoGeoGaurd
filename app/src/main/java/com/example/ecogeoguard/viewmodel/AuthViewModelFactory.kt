package com.example.ecogeoguard.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AuthViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {

            val prefs = context.getSharedPreferences(
                "auth_prefs",
                Context.MODE_PRIVATE
            )

            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(prefs) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
