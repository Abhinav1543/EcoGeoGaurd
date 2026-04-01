package com.example.ecogeoguard.utils



object RiskCalculator {

    fun calculateLandslideRisk(
        rainfall: Int, // mm
        soilMoisture: Int, // percentage
        vibration: Double, // Richter scale
        slopeAngle: Double // degrees
    ): Double {
        // Weighted factors for landslide risk
        val rainfallWeight = 0.4
        val soilMoistureWeight = 0.3
        val vibrationWeight = 0.2
        val slopeWeight = 0.1

        // Normalize values
        val normalizedRainfall = rainfall / 100.0
        val normalizedSoilMoisture = soilMoisture / 100.0
        val normalizedVibration = vibration / 5.0 // Max Richter scale considered
        val normalizedSlope = slopeAngle / 45.0 // Max safe slope angle

        // Calculate weighted risk (0-100)
        val risk = (normalizedRainfall * rainfallWeight +
                normalizedSoilMoisture * soilMoistureWeight +
                normalizedVibration * vibrationWeight +
                normalizedSlope * slopeWeight) * 100

        return risk.coerceIn(0.0, 100.0)
    }

    fun calculateFloodRisk(
        rainfall: Int,
        riverLevel: Double,
        drainageCapacity: Double
    ): Double {
        // Simple flood risk calculation
        val risk = (rainfall * 0.5 + riverLevel * 0.3 + (100 - drainageCapacity) * 0.2)
        return risk.coerceIn(0.0, 100.0)
    }

    fun getRiskLevel(riskScore: Double): String {
        return when {
            riskScore < 30 -> "LOW"
            riskScore < 60 -> "MEDIUM"
            riskScore < 80 -> "HIGH"
            else -> "CRITICAL"
        }
    }

    fun shouldTriggerAlert(riskScore: Double, previousRisk: Double? = null): Boolean {
        // Trigger alert if risk is HIGH or CRITICAL
        // OR if risk increased significantly from previous reading
        return when {
            riskScore >= 60 -> true
            previousRisk != null && (riskScore - previousRisk) >= 20 -> true
            else -> false
        }
    }

    fun calculateRisk(rainfall: Int, soilMoisture: Int, vibration: Double): Double {
        // Simple risk calculation without slope angle
        val rainfallWeight = 0.5
        val soilMoistureWeight = 0.3
        val vibrationWeight = 0.2

        val normalizedRainfall = rainfall / 100.0
        val normalizedSoilMoisture = soilMoisture / 100.0
        val normalizedVibration = vibration / 5.0

        val risk = (normalizedRainfall * rainfallWeight +
                normalizedSoilMoisture * soilMoistureWeight +
                normalizedVibration * vibrationWeight) * 100

        return risk.coerceIn(0.0, 100.0)
    }
}
