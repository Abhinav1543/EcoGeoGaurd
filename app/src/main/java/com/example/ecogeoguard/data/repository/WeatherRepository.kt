// data/repository/WeatherRepository.kt
package com.example.ecogeoguard.data.repository

import com.example.ecogeoguard.data.model.WeatherData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor() {

    fun getCurrentWeather(): WeatherData {
        // Simulated weather data
        return WeatherData(
            temperature = 28.5f + (Math.random() * 10 - 5).toFloat(),
            humidity = 65 + (Math.random() * 20 - 10).toInt(),
            rainfall = 12.4f + (Math.random() * 10).toFloat(),
            windSpeed = 8.2f + (Math.random() * 5 - 2.5).toFloat(),
            condition = getRandomCondition()
        )
    }

    private fun getRandomCondition(): String {
        val conditions = listOf(
            "Sunny",
            "Partly Cloudy",
            "Cloudy",
            "Light Rain",
            "Heavy Rain",
            "Stormy"
        )
        return conditions.random()
    }
}