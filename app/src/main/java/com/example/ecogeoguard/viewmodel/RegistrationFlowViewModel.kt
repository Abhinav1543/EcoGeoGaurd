package com.example.ecogeoguard.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RegistrationFlowViewModel : ViewModel() {

    enum class Step {
        DETAILS,
        VERIFICATION,
        COMPLETE
    }

    data class RegistrationData(
        val name: String = "",
        val email: String = "",
        val phone: String = "",
        val password: String = "",
        val role: String = ""
    )

    val currentStep = MutableLiveData(Step.DETAILS)
    val data = MutableLiveData(RegistrationData())

    val emailVerified = MutableLiveData(false)
    val phoneVerified = MutableLiveData(false)
}
