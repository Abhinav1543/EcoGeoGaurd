package com.example.ecogeoguard.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.ecogeoguard.R
import com.example.ecogeoguard.MainActivity
import java.util.*
import kotlin.random.Random



class AlertNotificationService : Service() {



    private var periodicTimer: Timer? = null
    private var alertSimulationTimer: Timer? = null
    private var vibrator: Vibrator? = null
    private var notificationManager: NotificationManager? = null
    private var mediaPlayer: MediaPlayer? = null
    private var buzzingTimer: Timer? = null
    private var isBuzzing = false

    private val highRiskAlerts = mutableSetOf<String>()
    private val activeBuzzers = mutableMapOf<String, Timer>() // Track active buzzers

    // Timing
    private val notificationInterval = 30 * 60 * 1000L // 30 minutes
    private val alertSimulationInterval = 300 * 1000L
    private val BUZZER_DURATION = Long.MAX_VALUE // Buzz until stopped
    private val VIBRATION_PATTERN = longArrayOf(1000, 500) // 1 sec vibrate, 0.5 sec pause



    override fun onCreate() {
        super.onCreate()

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        Log.d("ALERT_SERVICE", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ALERT_SERVICE", "Received action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                Log.d("ALERT_SERVICE", "Starting monitoring...")
                startMonitoring()
            }
            ACTION_STOP -> {
                Log.d("ALERT_SERVICE", "Stopping monitoring...")
                stopMonitoring()
            }
            ACTION_MUTE_BUZZER -> {
                Log.d("ALERT_SERVICE", "Muting all buzzers...")
                muteAllBuzzers()
            }
            ACTION_MUTE_SINGLE -> {
                val alertId = intent.getStringExtra(EXTRA_ALERT_ID)
                if (alertId != null) {
                    Log.d("ALERT_SERVICE", "Muting single buzzer: $alertId")
                    muteSingleBuzzer(alertId)
                }
            }
            ACTION_TEST_BUZZER -> {
                Log.d("ALERT_SERVICE", "Testing buzzer...")
                triggerEmergencyBuzzer(
                    alertId = "test_${System.currentTimeMillis()}",
                    title = "🚨 TEST: Emergency Alert",
                    message = "This is a test of the emergency alert system",
                    type = "landslide",
                    severity = "critical",
                    location = "Test Zone"
                )
            }
            ACTION_ALERT_CLICKED -> {
                val alertId = intent.getStringExtra(EXTRA_ALERT_ID)
                if (alertId != null) {
                    Log.d("ALERT_SERVICE", "Alert clicked: $alertId")
                    markAlertAsRead(alertId)
                }
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        // Stop any existing monitoring
        stopMonitoring()

        // Start periodic notifications
        startPeriodicNotifications()

        // Start LOCAL alert simulation (NO Firebase needed)
        startLocalAlertSimulation()

        // Show initial notification
        showServiceRunningNotification()

        Log.d("ALERT_SERVICE", "Monitoring started successfully")
    }

    private fun stopMonitoring() {
        periodicTimer?.cancel()
        alertSimulationTimer?.cancel()
        muteAllBuzzers()
        Log.d("ALERT_SERVICE", "Monitoring stopped")
    }

    private fun muteAllBuzzers() {
        // Stop all active buzzers
        activeBuzzers.values.forEach { it.cancel() }
        activeBuzzers.clear()

        // Stop vibration
        vibrator?.cancel()

        // Stop media player
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        // Clear high risk alerts
        highRiskAlerts.clear()
        isBuzzing = false

        // Show muted notification
        showNotification(
            id = MUTED_NOTIFICATION_ID,
            title = "🔇 All Buzzers Muted",
            message = "Emergency buzzers have been silenced",
            priority = NotificationCompat.PRIORITY_DEFAULT,
            channel = CHANNEL_STATUS
        )

        Log.d("ALERT_SERVICE", "All buzzers muted")
    }

    private fun muteSingleBuzzer(alertId: String) {
        // Stop specific buzzer
        activeBuzzers[alertId]?.cancel()
        activeBuzzers.remove(alertId)

        // Remove from high risk alerts
        highRiskAlerts.remove(alertId)

        // If no more active buzzers, stop vibration and sound
        if (activeBuzzers.isEmpty()) {
            vibrator?.cancel()
            mediaPlayer?.stop()
            isBuzzing = false
        }

        // Update notification
        updateEmergencyNotification()

        Log.d("ALERT_SERVICE", "Buzzer muted for alert: $alertId")
    }

    private fun markAlertAsRead(alertId: String) {
        // This is called when admin clicks on the notification
        muteSingleBuzzer(alertId)

        // Show confirmation
        showNotification(
            id = READ_NOTIFICATION_ID,
            title = "✅ Alert Acknowledged",
            message = "You have read and acknowledged the emergency alert",
            priority = NotificationCompat.PRIORITY_LOW,
            channel = CHANNEL_STATUS
        )
    }

    private fun startPeriodicNotifications() {
        periodicTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    showPeriodicNotification()
                }
            }, 0, notificationInterval)
        }
    }

    private fun startLocalAlertSimulation() {
        alertSimulationTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    simulateRandomAlert()
                }
            }, 10000, alertSimulationInterval) // First after 10s, then every 45s
        }
    }

    private fun simulateRandomAlert() {
        val alertTypes = listOf("landslide", "heavy_rainfall", "livestock", "irrigation", "theft")
        val severities = listOf("low", "medium", "high", "critical")
        val locations = listOf("Village A", "Village B", "Village C", "Village D")

        val type = alertTypes.random()
        val severity = severities.random()
        val location = locations.random()
        val alertId = "sim_${System.currentTimeMillis()}"

        // Make landslides/heavy rainfall more frequent for testing
        val adjustedType = if (Random.nextBoolean() && Random.nextBoolean()) {
            if (Random.nextBoolean()) "landslide" else "heavy_rainfall"
        } else type

        val title = when (adjustedType) {
            "landslide" -> "🚨 LANDSLIDE DETECTED"
            "heavy_rainfall" -> "🌧️ HEAVY RAINFALL WARNING"
            else -> "${adjustedType.replaceFirstChar { it.uppercase() }} Alert"
        }

        val message = when (adjustedType) {
            "landslide" -> "Immediate danger! Landslide detected at $location. Evacuate immediately!"
            "heavy_rainfall" -> "Warning! Heavy rainfall at $location. Risk of flooding!"
            else -> "Alert at $location - ${severity.uppercase()} severity"
        }

        // 🚨 BUZZER CRITERIA: landslide/heavy_rainfall + high/critical
        val isHighRisk = (adjustedType == "landslide" || adjustedType == "heavy_rainfall") &&
                (severity == "high" || severity == "critical")

        Log.d("ALERT_SERVICE", "Simulating alert: type=$adjustedType, severity=$severity, highRisk=$isHighRisk")

        if (isHighRisk && !highRiskAlerts.contains(alertId)) {
            highRiskAlerts.add(alertId)
            triggerEmergencyBuzzer(alertId, title, message, adjustedType, severity, location)
        } else {
            // Regular notification for non-emergency
            showAlertNotification(alertId, adjustedType, severity, title, message)
        }
    }

    // 🚨 MAIN BUZZER FUNCTION - BUZZES CONTINUOUSLY
    private fun triggerEmergencyBuzzer(
        alertId: String,
        title: String,
        message: String,
        type: String,
        severity: String,
        location: String
    ) {
        Log.d("ALERT_SERVICE", "🚨🚨🚨 EMERGENCY BUZZER TRIGGERED: $title")

        // 1. START CONTINUOUS VIBRATION
        startContinuousVibration()

        // 2. START CONTINUOUS ALARM SOUND (CUSTOM)
        startContinuousAlarm()

        // 3. SHOW FULL-SCREEN NOTIFICATION
        showHighPriorityNotification(alertId, title, message, type, severity, location)

        // 4. START BUZZER TIMER (buzzes until stopped)
        val buzzerTimer = Timer()
        buzzerTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Keep buzzing - this runs continuously
                if (!isBuzzing) {
                    startContinuousVibration()
                    startContinuousAlarm()
                }
            }
        }, 0, 5000) // Check every 5 seconds

        activeBuzzers[alertId] = buzzerTimer
        isBuzzing = true

        // 5. ALSO SHOW IN-APP ALERT
        sendBroadcastAlert(alertId, title, message, type, severity, location)
    }

    private fun startContinuousVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
            vibrator?.vibrate(vibrationEffect)
        } else {
            vibrator?.vibrate(VIBRATION_PATTERN, 0)
        }
    }

    private fun startContinuousAlarm() {
        // Stop existing alarm
        mediaPlayer?.stop()
        mediaPlayer?.release()

        // Create new media player with CUSTOM sound
        mediaPlayer = MediaPlayer.create(this, R.raw.emergency_alarm).apply {
            isLooping = true // Loop continuously
            setVolume(1.0f, 1.0f) // Max volume
            start()
        }

        // If custom sound not found, use default alarm
        if (mediaPlayer == null) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer.create(this, alarmSound).apply {
                isLooping = true
                setVolume(1.0f, 1.0f)
                start()
            }
        }
    }

    private fun showHighPriorityNotification(
        alertId: String,
        title: String,
        message: String,
        type: String,
        severity: String,
        location: String
    ) {
        // Intent for when notification is clicked
        val clickIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("alert_id", alertId)
            putExtra("alert_type", type)
            putExtra("alert_title", title)
            putExtra("alert_message", message)
            putExtra("alert_location", location)
            putExtra("alert_severity", severity)
            action = "EMERGENCY_ALERT"
        }

        val clickPendingIntent = PendingIntent.getActivity(
            this,
            alertId.hashCode(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for mute button
        val muteIntent = Intent(this, AlertNotificationService::class.java).apply {
            action = ACTION_MUTE_SINGLE
            putExtra(EXTRA_ALERT_ID, alertId)
        }

        val mutePendingIntent = PendingIntent.getService(
            this,
            alertId.hashCode() + 1,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create rich notification with details
        val notification = NotificationCompat.Builder(this, CHANNEL_EMERGENCY)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle("🚨 $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$message\n\n📍 Location: $location\n⚡ Severity: ${severity.uppercase()}\n📅 Time: ${Date()}")
                .setBigContentTitle("🚨 $title")
                .setSummaryText("Emergency Alert"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(clickPendingIntent, true) // Takes over screen
            .setAutoCancel(false) // Don't auto-cancel - admin must act
            .setOngoing(true) // Cannot be dismissed by swiping
            .setOnlyAlertOnce(false) // Alert every time
            .setVibrate(VIBRATION_PATTERN)
            .setSound(android.net.Uri.parse("android.resource://${packageName}/${R.raw.emergency_alarm}"))
            .setColor(ContextCompat.getColor(this, R.color.dangerRed))
            .addAction(
                R.drawable.ic_mute,
                "SILENCE ALARM",
                mutePendingIntent
            )
            .addAction(
                R.drawable.ic_view,
                "VIEW DETAILS",
                clickPendingIntent
            )
            .setContentIntent(clickPendingIntent)
            .build()

        notificationManager?.notify(alertId.hashCode(), notification)
    }

    private fun updateEmergencyNotification() {
        // Update notification when buzzers are muted
        if (activeBuzzers.isEmpty()) {
            notificationManager?.cancel(EMERGENCY_NOTIFICATION_ID)
        }
    }

    private fun sendBroadcastAlert(
        alertId: String,
        title: String,
        message: String,
        type: String,
        severity: String,
        location: String
    ) {
        // Broadcast to activity if it's running
        val broadcastIntent = Intent("EMERGENCY_ALERT_RECEIVED").apply {
            putExtra("alert_id", alertId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("type", type)
            putExtra("severity", severity)
            putExtra("location", location)
            putExtra("timestamp", System.currentTimeMillis())
        }
        sendBroadcast(broadcastIntent)
    }

    private fun showAlertNotification(
        alertId: String,
        type: String,
        severity: String,
        title: String,
        message: String
    ) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(getAlertIcon(type))
            .setContentTitle("⚠️ $title")
            .setContentText(message)
            .setPriority(getNotificationPriority(severity))
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setColor(getAlertColor(severity))
            .build()

        notificationManager?.notify(alertId.hashCode(), notification)
    }

    private fun showServiceRunningNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("🟢 EcoGeoGuard Active")
            .setContentText("Monitoring 50 sensors across 12 villages")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(SERVICE_NOTIFICATION_ID, notification)
    }

    private fun showPeriodicNotification() {
        showNotification(
            id = System.currentTimeMillis().toInt(),
            title = "📊 System Status",
            message = "All systems operational. Monitoring continues.",
            priority = NotificationCompat.PRIORITY_LOW,
            channel = CHANNEL_STATUS
        )
    }

    private fun showNotification(id: Int, title: String, message: String, priority: Int, channel: String) {
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setAutoCancel(true)
            .build()

        notificationManager?.notify(id, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        Log.d("ALERT_SERVICE", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        // Actions
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_MUTE_BUZZER = "ACTION_MUTE_BUZZER"
        const val ACTION_MUTE_SINGLE = "ACTION_MUTE_SINGLE"
        const val ACTION_TEST_BUZZER = "ACTION_TEST_BUZZER"
        const val ACTION_ALERT_CLICKED = "ACTION_ALERT_CLICKED"

        // Extras
        const val EXTRA_ALERT_ID = "EXTRA_ALERT_ID"

        // Channels
        private const val CHANNEL_EMERGENCY = "channel_emergency"
        private const val CHANNEL_ALERTS = "channel_alerts"
        private const val CHANNEL_STATUS = "channel_status"

        // Notification IDs
        private const val SERVICE_NOTIFICATION_ID = 101
        private const val EMERGENCY_NOTIFICATION_ID = 102
        private const val MUTED_NOTIFICATION_ID = 103
        private const val READ_NOTIFICATION_ID = 104

        // Request Codes
        private const val MUTE_REQUEST_CODE = 1000

        fun startService(context: Context) {
            Log.d("ALERT_SERVICE", "Starting service from context")
            val intent = Intent(context, AlertNotificationService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AlertNotificationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun muteAllBuzzers(context: Context) {
            val intent = Intent(context, AlertNotificationService::class.java)
            intent.action = ACTION_MUTE_BUZZER

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun testBuzzer(context: Context) {
            val intent = Intent(context, AlertNotificationService::class.java).apply {
                action = ACTION_TEST_BUZZER
            }
            context.startService(intent)
        }
    }

    // Helper functions
    private fun getAlertIcon(type: String): Int = when (type) {
        "landslide" -> R.drawable.img_7
        "heavy_rainfall" -> R.drawable.img_1
        "livestock" -> R.drawable.ic_livestock
        "theft" -> R.drawable.img_3
        else -> R.drawable.ic_alert
    }

    private fun getAlertColor(severity: String): Int = when (severity) {
        "critical", "high" -> ContextCompat.getColor(this, R.color.dangerRed)
        "medium" -> ContextCompat.getColor(this, R.color.warningOrange)
        else -> ContextCompat.getColor(this, R.color.safeGreen)
    }

    private fun getNotificationPriority(severity: String): Int = when (severity) {
        "critical", "high" -> NotificationCompat.PRIORITY_MAX
        "medium" -> NotificationCompat.PRIORITY_HIGH
        else -> NotificationCompat.PRIORITY_DEFAULT
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Emergency Channel - MAX IMPORTANCE
            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "🚨 Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts requiring immediate attention"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(
                    android.net.Uri.parse("android.resource://${packageName}/${R.raw.emergency_alarm}"),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            // Alerts Channel
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "⚠️ Regular Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Regular alerts and notifications"
            }

            // Status Channel
            val statusChannel = NotificationChannel(
                CHANNEL_STATUS,
                "📊 Status Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Periodic system status updates"
            }

            notificationManager?.createNotificationChannels(
                listOf(emergencyChannel, alertsChannel, statusChannel)
            )
        }
    }
}