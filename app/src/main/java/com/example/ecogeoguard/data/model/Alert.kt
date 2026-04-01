// data/model/Alert.kt
package com.example.ecogeoguard.data.model

import android.os.Parcelable
import com.example.ecogeoguard.R
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class Alert(
    val id: String,
    val title: String,
    val message: String,
    val type: AlertType,
    val severity: Severity,
    val timestamp: Long,
    val villageId: String,
    val fieldId: String? = null,
    val sensorId: String? = null,
    val isRead: Boolean = false,
    val actionTaken: Boolean = false
) : Parcelable {

    enum class AlertType {
        LANDSLIDE, LIVESTOCK, RAINFALL, IRRIGATION, THEFT, SYSTEM
    }

    enum class Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    val formattedTime: String
        get() {
            val date = Date(timestamp)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            return format.format(date)
        }

    val formattedDate: String
        get() {
            val date = Date(timestamp)
            val format = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
            return format.format(date)
        }

    val colorRes: Int
        get() = when(severity) {
            Severity.LOW -> android.R.color.holo_green_light
            Severity.MEDIUM -> android.R.color.holo_orange_light
            Severity.HIGH -> android.R.color.holo_red_light
            Severity.CRITICAL -> android.R.color.holo_red_dark
        }

    fun getIconRes(): Int {
        return when(type) {
            AlertType.LANDSLIDE -> R.drawable.img
            AlertType.LIVESTOCK -> R.drawable.ic_livestock
            AlertType.RAINFALL -> R.drawable.img_1
            AlertType.IRRIGATION -> R.drawable.img_2
            AlertType.THEFT -> R.drawable.img_3
            AlertType.SYSTEM -> R.drawable.img_4
        }
    }
}