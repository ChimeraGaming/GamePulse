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
import com.chimeragaming.gamepulse.model.BatteryAnalysisResult
import com.chimeragaming.gamepulse.utils.BatteryUtils
import com.chimeragaming.gamepulse.utils.PerformanceUtils
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
        
        // Analysis configuration
        private const val UPDATE_INTERVAL_SECONDS = 10L // Update every 10 seconds
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
    private var startBatteryLevel = 0
    private var startVoltage = 0f
    private var durationMinutes = 60 // Default 1 hour
    private var gameName: String? = null
    private var systemName: String? = null
    
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
                durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 60)
                gameName = intent.getStringExtra(EXTRA_GAME_NAME)
                systemName = intent.getStringExtra(EXTRA_SYSTEM_NAME)
                startAnalysis()
            }
            ACTION_STOP_ANALYSIS -> stopAnalysis()
        }
        return START_NOT_STICKY
    }
    
    fun startAnalysis(duration: Int = 60, game: String? = null, system: String? = null) {
        if (_isRunning.value) return
        
        durationMinutes = duration
        gameName = game
        systemName = system
        
        _isRunning.value = true
        startTime = System.currentTimeMillis()
        
        val batteryInfo = BatteryUtils.getBatteryInfo(this)
        startBatteryLevel = batteryInfo?.level ?: 0
        startVoltage = batteryInfo?.voltage ?: 0f
        
        startForeground(NOTIFICATION_ID, createNotification("Analysis in progress..."))
        
        serviceScope.launch {
            // Run for specified duration (converted to seconds)
            val totalDuration = durationMinutes * 60L
            val updateInterval = UPDATE_INTERVAL_SECONDS
            
            for (elapsed in 0..totalDuration step updateInterval) {
                if (!_isRunning.value) break
                
                _progressPercent.value = ((elapsed.toFloat() / totalDuration) * 100).toInt()
                
                // Update notification with progress
                val notification = createNotification(
                    "Analysis: ${_progressPercent.value}% complete"
                )
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, notification)
                
                delay(updateInterval * 1000)
            }
            
            if (_isRunning.value) {
                completeAnalysis()
            }
        }
    }
    
    fun stopAnalysis() {
        if (!_isRunning.value) return
        completeAnalysis()
    }
    
    private fun completeAnalysis() {
        val endTime = System.currentTimeMillis()
        val batteryInfo = BatteryUtils.getBatteryInfo(this)
        val endBatteryLevel = batteryInfo?.level ?: startBatteryLevel
        val endVoltage = batteryInfo?.voltage ?: startVoltage
        
        val batteryDrain = startBatteryLevel - endBatteryLevel
        val voltageDrop = startVoltage - endVoltage
        
        // Get app usage statistics for the analysis period
        val appUsageList = PerformanceUtils.getAppUsageStats(this, startTime, endTime)
        
        // Calculate estimated life based on drain rate
        val durationHours = (endTime - startTime) / (1000f * 60 * 60)
        val drainPerHour = if (durationHours > 0) batteryDrain / durationHours else 0f
        val estimatedLifeHours = if (drainPerHour > 0) endBatteryLevel / drainPerHour else 0f
        
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
            topApps = appUsageList.take(10),
            isComplete = true,
            gameName = gameName,
            systemName = systemName,
            packageName = null
        )
        
        _analysisResult.value = result
        _isRunning.value = false
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
        serviceJob.cancel()
    }
}
