package com.example.ecogeoguard.utils

import android.location.Location

object GeoFenceUtils {

    fun isOutsideFence(
        animalLat: Double,
        animalLng: Double,
        safeLat: Double,
        safeLng: Double,
        radius: Double
    ): Boolean {

        val animal = Location("animal").apply {
            latitude = animalLat
            longitude = animalLng
        }

        val safeZone = Location("safeZone").apply {
            latitude = safeLat
            longitude = safeLng
        }

        return animal.distanceTo(safeZone) > radius
    }
}
