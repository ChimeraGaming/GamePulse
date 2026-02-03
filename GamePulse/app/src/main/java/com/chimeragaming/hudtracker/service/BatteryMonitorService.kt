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

/**
 * Foreground service for continuous battery monitoring
 * v0.3: Added low battery protection to prevent crashes below 10%
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

                    // v0.3: Adjust update interval based on battery level
                    val currentBattery = _batteryInfo.value?.percentage ?: 100f
                    val interval = if (currentBattery < 15f) {
                        // Slower updates when battery is low
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

            // Calculate estimated battery life if we have previous reading
            val estimatedLife = if (previousBatteryInfo != null && previousReadingTime > 0) {
                val timeDiffSeconds = (currentTime - previousReadingTime) / 1000
                BatteryUtils.estimateBatteryLife(
                    currentInfo.voltage,
                    previousBatteryInfo!!.voltage,
                    timeDiffSeconds,
                    currentInfo.level
                )
            } else {
                0
            }

            val updatedInfo = currentInfo.copy(estimatedLifeMinutes = estimatedLife)
            _batteryInfo.value = updatedInfo

            previousBatteryInfo = currentInfo
            previousReadingTime = currentTime
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setUpdateInterval(seconds: Long) {
        updateIntervalSeconds = seconds
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors battery voltage and life"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HUDTracker")
            .setContentText("Monitoring battery and performance")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "battery_monitor_channel"
        const val ACTION_SET_INTERVAL = "com.chimeragaming.hudtracker.SET_INTERVAL"
        const val EXTRA_INTERVAL = "interval"
    }
}