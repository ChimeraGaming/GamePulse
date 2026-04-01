package com.chimeragaming.gamepulse.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.chimeragaming.gamepulse.R
import com.chimeragaming.gamepulse.model.BatteryAnalysisResult
import com.chimeragaming.gamepulse.utils.BatteryUtils
import com.chimeragaming.gamepulse.utils.GameCollectionRepository
import com.chimeragaming.gamepulse.utils.PerformanceUtils
import com.chimeragaming.gamepulse.utils.RAMUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Service for battery consumption analysis with custom durations
 */
class BatteryAnalysisService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "battery_analysis_channel"
        const val ACTION_START_ANALYSIS = "com.chimeragaming.hudtracker.START_ANALYSIS"
        const val ACTION_STOP_ANALYSIS = "com.chimeragaming.hudtracker.STOP_ANALYSIS"
        
        // Intent extras
        const val EXTRA_DURATION_MINUTES = "duration_minutes"
        const val EXTRA_GAME_NAME = "game_name"
        const val EXTRA_SYSTEM_NAME = "system_name"
        const val EXTRA_PACKAGE_NAME = "package_name"
        
        // Analysis configuration
        private const val UPDATE_INTERVAL_SECONDS = 10L // Update every 10 seconds
        private const val MILLIS_PER_SECOND = 1000L
        private const val MILLIS_PER_MINUTE = 60_000L
    }
    
    private val binder = LocalBinder()
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    
    private val _analysisResult = MutableStateFlow<BatteryAnalysisResult?>(null)
    val analysisResult: StateFlow<BatteryAnalysisResult?> = _analysisResult
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning
    
    private val _progressPercent = MutableStateFlow(0)
    val progressPercent: StateFlow<Int> = _progressPercent
    
    private var startTime: Long = 0
    private var startElapsedRealtime: Long = 0
    private var startBatteryLevel = 0
    private var startVoltage = 0f
    private var durationMinutes = 60 // Default 1 hour
    private var gameName: String? = null
    private var systemName: String? = null
    private var trackedPackageName: String? = null
    private var ramSamplesTotalMB = 0L
    private var ramSampleCount = 0
    private var temperatureSamplesTotalC = 0f
    private var temperatureSampleCount = 0
    private var maxTemperatureC = 0f
    private var analysisJob: Job? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): BatteryAnalysisService = this@BatteryAnalysisService
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ANALYSIS -> {
                startAnalysis(
                    duration = intent.getIntExtra(EXTRA_DURATION_MINUTES, 60),
                    game = intent.getStringExtra(EXTRA_GAME_NAME),
                    system = intent.getStringExtra(EXTRA_SYSTEM_NAME),
                    packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                )
            }
            ACTION_STOP_ANALYSIS -> stopAnalysis()
        }
        return START_NOT_STICKY
    }
    
    fun startAnalysis(
        duration: Int = 60,
        game: String? = null,
        system: String? = null,
        packageName: String? = null
    ) {
        analysisJob?.cancel()

        durationMinutes = duration.coerceAtLeast(1)
        gameName = game
        systemName = system
        trackedPackageName = packageName
        
        _isRunning.value = true
        _analysisResult.value = null
        _progressPercent.value = 0
        startTime = System.currentTimeMillis()
        startElapsedRealtime = SystemClock.elapsedRealtime()
        ramSamplesTotalMB = 0L
        ramSampleCount = 0
        temperatureSamplesTotalC = 0f
        temperatureSampleCount = 0
        maxTemperatureC = 0f

        val batteryInfo = BatteryUtils.getBatteryInfo(this)
        startBatteryLevel = batteryInfo?.level ?: 0
        startVoltage = batteryInfo?.voltage ?: 0f
        recordSystemSample()

        startForeground(NOTIFICATION_ID, createNotification("Analysis in progress..."))
        
        val totalDurationMs = durationMinutes.toLong() * MILLIS_PER_MINUTE
        val targetElapsedRealtime = startElapsedRealtime + totalDurationMs

        analysisJob = serviceScope.launch {
            while (_isRunning.value) {
                val nowElapsedRealtime = SystemClock.elapsedRealtime()
                val remainingMs = (targetElapsedRealtime - nowElapsedRealtime).coerceAtLeast(0L)
                val elapsedMs = (totalDurationMs - remainingMs).coerceAtLeast(0L)

                recordSystemSample()
                _progressPercent.value = if (totalDurationMs > 0L) {
                    ((elapsedMs.toFloat() / totalDurationMs.toFloat()) * 100f)
                        .toInt()
                        .coerceIn(0, 100)
                } else {
                    100
                }
                
                val notification = createNotification(
                    "Analysis: ${_progressPercent.value}% complete"
                )
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, notification)

                if (remainingMs <= 0L) {
                    completeAnalysis(shouldSaveToCollection = true)
                    break
                }

                val nextDelayMs = minOf(UPDATE_INTERVAL_SECONDS * MILLIS_PER_SECOND, remainingMs)
                delay(nextDelayMs)
            }
        }
    }
    
    fun stopAnalysis() {
        if (!_isRunning.value) return
        analysisJob?.cancel()
        completeAnalysis(shouldSaveToCollection = false)
    }
    
    private fun completeAnalysis(shouldSaveToCollection: Boolean) {
        if (!_isRunning.value) {
            return
        }

        analysisJob?.cancel()
        analysisJob = null
        _isRunning.value = false
        val endTime = System.currentTimeMillis()
        val batteryInfo = BatteryUtils.getBatteryInfo(this)
        val endBatteryLevel = batteryInfo?.level ?: startBatteryLevel
        val endVoltage = batteryInfo?.voltage ?: startVoltage
        recordSystemSample()

        val batteryDrain = startBatteryLevel - endBatteryLevel
        val voltageDrop = startVoltage - endVoltage
        
        // Get app usage statistics for the analysis period
        val appUsageList = PerformanceUtils.getAppUsageStats(
            context = this,
            startTime = startTime,
            endTime = endTime,
            totalBatteryDrainPercent = batteryDrain.toFloat().coerceAtLeast(0f)
        )
        
        // Calculate estimated life based on drain rate
        val durationHours = (endTime - startTime) / (1000f * 60 * 60)
        val drainPerHour = if (durationHours > 0) batteryDrain / durationHours else 0f
        val estimatedLifeHours = if (drainPerHour > 0) 100f / drainPerHour else 0f
        val averageRamUsageGB = if (ramSampleCount > 0) {
            (ramSamplesTotalMB.toFloat() / ramSampleCount.toFloat()) / 1024f
        } else {
            0f
        }
        val averageTemperatureC = if (temperatureSampleCount > 0) {
            temperatureSamplesTotalC / temperatureSampleCount.toFloat()
        } else {
            0f
        }
        val resolvedPackageName = trackedPackageName?.takeIf { it.isNotBlank() }
            ?: appUsageList.firstOrNull { it.packageName != packageName }?.packageName

        val result = BatteryAnalysisResult(
            startTime = startTime,
            endTime = endTime,
            startBatteryLevel = startBatteryLevel,
            endBatteryLevel = endBatteryLevel,
            startVoltage = startVoltage,
            endVoltage = endVoltage,
            batteryDrainPercent = batteryDrain.toFloat(),
            voltageDrop = voltageDrop,
            estimatedLifeHours = estimatedLifeHours,
            averageRamUsageGB = averageRamUsageGB,
            averageTemperatureC = averageTemperatureC,
            maxTemperatureC = maxTemperatureC,
            topApps = appUsageList.take(10),
            isComplete = true,
            gameName = gameName,
            systemName = systemName,
            packageName = resolvedPackageName
        )

        if (shouldSaveToCollection) {
            GameCollectionRepository(this).recordBatteryAnalysis(result)
        }
        _analysisResult.value = result
        _progressPercent.value = 100
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Analysis",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Battery consumption analysis"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HUDTracker Battery Analysis")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        analysisJob?.cancel()
        serviceJob.cancel()
    }

    private fun recordSystemSample() {
        try {
            val ramInfo = RAMUtils.getRAMInfo(this)
            ramSamplesTotalMB += ramInfo.usedMemoryMB
            ramSampleCount++
        } catch (_: Exception) {
        }

        try {
            val batteryInfo = BatteryUtils.getBatteryInfo(this)
            val temperatureC = batteryInfo?.temperature ?: 0f
            if (temperatureC > 0f) {
                temperatureSamplesTotalC += temperatureC
                temperatureSampleCount++
                maxTemperatureC = maxOf(maxTemperatureC, temperatureC)
            }
        } catch (_: Exception) {
        }
    }
}
