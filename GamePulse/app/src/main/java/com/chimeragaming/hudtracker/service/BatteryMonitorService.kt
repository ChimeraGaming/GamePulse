package com.chimeragaming.gamepulse.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chimeragaming.gamepulse.R
import com.chimeragaming.gamepulse.model.BatteryInfo
import com.chimeragaming.gamepulse.utils.BatteryUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                    BATTERY MONITOR SERVICE                            ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║                v0.3.2 - Fixed Battery Life Estimation                 ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
class BatteryMonitorService : Service() {

    private val binder = LocalBinder()
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private val _batteryInfo = MutableStateFlow<BatteryInfo?>(null)
    val batteryInfo: StateFlow<BatteryInfo?> = _batteryInfo

    private var updateIntervalSeconds = 10L
    private var previousBatteryInfo: BatteryInfo? = null
    private var previousReadingTime = 0L
    private var readingCount = 0

    companion object {
        private const val CHANNEL_ID = "battery_monitor_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_SET_INTERVAL = "SET_INTERVAL"
        const val EXTRA_INTERVAL = "interval_seconds"
        private const val MIN_TIME_BETWEEN_READINGS = 5000L // 5 seconds minimum
    }

    inner class LocalBinder : Binder() {
        fun getService(): BatteryMonitorService = this@BatteryMonitorService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SET_INTERVAL -> {
                updateIntervalSeconds = intent.getLongExtra(EXTRA_INTERVAL, 10L)
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (true) {
                try {
                    updateBatteryInfo()

                    val currentBattery = _batteryInfo.value?.percentage ?: 100f
                    val interval = if (currentBattery < 15f) {
                        updateIntervalSeconds * 2
                    } else {
                        updateIntervalSeconds
                    }

                    delay(interval * 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(updateIntervalSeconds * 1000)
                }
            }
        }
    }

    private fun updateBatteryInfo() {
        try {
            val currentInfo = BatteryUtils.getBatteryInfo(this) ?: return
            val currentTime = System.currentTimeMillis()

            val estimatedLife = calculateEstimatedLife(currentInfo, currentTime)

            val updatedInfo = currentInfo.copy(estimatedLifeMinutes = estimatedLife)
            _batteryInfo.value = updatedInfo

            // Only update previous reading if enough time has passed
            if (previousReadingTime == 0L || (currentTime - previousReadingTime) >= MIN_TIME_BETWEEN_READINGS) {
                previousBatteryInfo = currentInfo
                previousReadingTime = currentTime
                readingCount++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateEstimatedLife(currentInfo: BatteryInfo, currentTime: Long): Int {
        // If charging or full, return 0 (N/A)
        if (currentInfo.status.contains("Charging", ignoreCase = true) ||
            currentInfo.status.contains("Full", ignoreCase = true)) {
            return 0
        }

        // If we don't have a previous reading yet, return -1 (Calculating...)
        if (previousBatteryInfo == null || previousReadingTime == 0L) {
            return -1
        }

        val timeDiffMillis = currentTime - previousReadingTime

        // If not enough time has passed, return -1 (Calculating...)
        if (timeDiffMillis < MIN_TIME_BETWEEN_READINGS) {
            return -1
        }

        val timeDiffSeconds = timeDiffMillis / 1000

        // Calculate estimated life
        val estimatedMinutes = BatteryUtils.estimateBatteryLife(
            currentInfo.voltage,
            previousBatteryInfo!!.voltage,
            timeDiffSeconds,
            currentInfo.level
        )

        // If calculation returns 0 (no drain detected), but we're discharging, keep calculating
        if (estimatedMinutes == 0 && readingCount < 3) {
            return -1 // Still calculating, need more readings
        }

        return estimatedMinutes
    }

    fun setUpdateInterval(seconds: Long) {
        updateIntervalSeconds = seconds
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors battery status and performance"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GamePulse")
            .setContentText("Monitoring battery performance")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging) // Built-in battery/charging icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}