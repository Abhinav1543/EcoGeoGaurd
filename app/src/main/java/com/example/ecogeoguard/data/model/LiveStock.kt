package com.example.ecogeoguard.data.model

data class Livestock(
    val animalId: String = "",
    val tagId: String,
    val type: String = "Cow",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val safeLat: Double = 0.0,
    val safeLng: Double = 0.0,
    val safeRadius: Double = 300.0 // meters
)
