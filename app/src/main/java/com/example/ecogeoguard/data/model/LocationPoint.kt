package com.example.ecogeoguard.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
) : Parcelable