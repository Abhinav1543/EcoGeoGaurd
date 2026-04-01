package com.example.ecogeoguard.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val altitude: Double? = null,
    val accuracy: Float? = null
) : Parcelable {

    fun getLatLngString(): String = "$latitude, $longitude"

    fun distanceTo(other: LocationData): Double {
        val R = 6371e3 // Earth's radius in meters
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val latDiff = Math.toRadians(other.latitude - latitude)
        val lonDiff = Math.toRadians(other.longitude - longitude)

        val a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c // distance in meters
    }
}