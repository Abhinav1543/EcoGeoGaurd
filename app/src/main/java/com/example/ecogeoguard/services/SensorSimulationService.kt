package com.example.ecogeoguard.services

import android.os.Handler
import android.os.Looper
import com.example.ecogeoguard.data.model.SensorData
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

object SensorSimulationService {

    private val handler = Handler(Looper.getMainLooper())
    private val database = FirebaseDatabase.getInstance()
    private val sensorRef = database.getReference("sensorData/VIL_01")

    private val runnable = object : Runnable {
        override fun run() {

            val sensorData = SensorData(
                rainfall = Random.nextInt(0, 120),
                soilMoisture = Random.nextInt(10, 90),
                vibration = Random.nextDouble(0.0, 8.0)
            )

            sensorRef.setValue(sensorData)

            handler.postDelayed(this, 8000) // every 8 seconds
        }
    }

    fun startSimulation() {
        handler.post(runnable)
    }

    fun stopSimulation() {
        handler.removeCallbacks(runnable)
    }
}
