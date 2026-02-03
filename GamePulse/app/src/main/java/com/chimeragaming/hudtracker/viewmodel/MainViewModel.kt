package com.chimeragaming.gamepulse.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chimeragaming.gamepulse.model.BatteryAnalysisResult
import com.chimeragaming.gamepulse.model.BatteryInfo
import com.chimeragaming.gamepulse.model.RAMInfo
import com.chimeragaming.gamepulse.service.BatteryAnalysisService
import com.chimeragaming.gamepulse.service.BatteryMonitorService
import com.chimeragaming.gamepulse.utils.RAMUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the main HUDTracker activity
 * v0.3: Added battery crash protection for analysis below 10%
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val MILLIS_TO_SECONDS = 1000L // Conversion factor from milliseconds to seconds
        private const val MIN_BATTERY_FOR_ANALYSIS = 10f // v0.3: Minimum battery to continue analysis
    }

    private val _batteryInfo = MutableStateFlow<BatteryInfo?>(null)
    val batteryInfo: StateFlow<BatteryInfo?> = _batteryInfo

    private val _ramInfo = MutableStateFlow<RAMInfo?>(null)
    val ramInfo: StateFlow<RAMInfo?> = _ramInfo

    private val _analysisResult = MutableStateFlow<BatteryAnalysisResult?>(null)
    val analysisResult: StateFlow<BatteryAnalysisResult?> = _analysisResult

    private val _analysisProgress = MutableStateFlow(0)
    val analysisProgress: StateFlow<Int> = _analysisProgress

    private val _isAnalysisRunning = MutableStateFlow(false)
    val isAnalysisRunning: StateFlow<Boolean> = _isAnalysisRunning

    private val _updateInterval = MutableStateFlow(10)
    val updateInterval: StateFlow<Int> = _updateInterval

    private var batteryMonitorService: BatteryMonitorService? = null
    private var analysisService: BatteryAnalysisService? = null
    private var isBatteryServiceBound = false
    private var isAnalysisServiceBound = false

    private val batteryServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BatteryMonitorService.LocalBinder
            batteryMonitorService = binder.getService()
            isBatteryServiceBound = true

            // Observe battery info from service
            viewModelScope.launch {
                batteryMonitorService?.batteryInfo?.collect { info ->
                    _batteryInfo.value = info

                    // v0.3: Monitor battery during analysis and stop if too low
                    if (_isAnalysisRunning.value) {
                        val batteryLevel = info?.percentage ?: 100f
                        if (batteryLevel < MIN_BATTERY_FOR_ANALYSIS) {
                            stopBatteryAnalysis()
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            batteryMonitorService = null
            isBatteryServiceBound = false
        }
    }

    private val analysisServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BatteryAnalysisService.LocalBinder
            analysisService = binder.getService()
            isAnalysisServiceBound = true

            // Observe analysis state
            viewModelScope.launch {
                analysisService?.isRunning?.collect { running ->
                    _isAnalysisRunning.value = running
                }
            }

            viewModelScope.launch {
                analysisService?.progressPercent?.collect { progress ->
                    _analysisProgress.value = progress
                }
            }

            viewModelScope.launch {
                analysisService?.analysisResult?.collect { result ->
                    _analysisResult.value = result
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            analysisService = null
            isAnalysisServiceBound = false
        }
    }

    init {
        startBatteryMonitoring()
        startRAMMonitoring()
    }

    private fun startBatteryMonitoring() {
        val intent = Intent(getApplication(), BatteryMonitorService::class.java)
        getApplication<Application>().startForegroundService(intent)
        getApplication<Application>().bindService(intent, batteryServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startRAMMonitoring() {
        viewModelScope.launch {
            while (true) {
                try {
                    _ramInfo.value = RAMUtils.getRAMInfo(getApplication())
                    delay(_updateInterval.value * MILLIS_TO_SECONDS)
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(_updateInterval.value * MILLIS_TO_SECONDS)
                }
            }
        }
    }

    fun setUpdateInterval(seconds: Int) {
        _updateInterval.value = seconds
        batteryMonitorService?.setUpdateInterval(seconds.toLong())
    }

    fun startBatteryAnalysis(durationMinutes: Int = 60, gameName: String? = null, systemName: String? = null) {
        // v0.3: Check battery level before starting analysis
        val currentBattery = _batteryInfo.value?.percentage ?: 100f
        if (currentBattery < 20f) {
            // Don't start analysis if battery is already low
            return
        }

        val intent = Intent(getApplication(), BatteryAnalysisService::class.java).apply {
            action = BatteryAnalysisService.ACTION_START_ANALYSIS
            putExtra(BatteryAnalysisService.EXTRA_DURATION_MINUTES, durationMinutes)
            putExtra(BatteryAnalysisService.EXTRA_GAME_NAME, gameName)
            putExtra(BatteryAnalysisService.EXTRA_SYSTEM_NAME, systemName)
        }
        getApplication<Application>().startForegroundService(intent)
        getApplication<Application>().bindService(intent, analysisServiceConnection, Context.BIND_AUTO_CREATE)
    }

    fun stopBatteryAnalysis() {
        analysisService?.stopAnalysis()
    }

    override fun onCleared() {
        super.onCleared()
        if (isBatteryServiceBound) {
            getApplication<Application>().unbindService(batteryServiceConnection)
        }
        if (isAnalysisServiceBound) {
            getApplication<Application>().unbindService(analysisServiceConnection)
        }
    }
}