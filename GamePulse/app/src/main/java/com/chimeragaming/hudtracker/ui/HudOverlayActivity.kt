package com.chimeragaming.gamepulse.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chimeragaming.gamepulse.databinding.ActivityHudOverlayBinding
import com.chimeragaming.gamepulse.model.BatteryInfo
import com.chimeragaming.gamepulse.service.BatteryMonitorService
import com.chimeragaming.gamepulse.utils.BatteryThemeRenderer
import com.chimeragaming.gamepulse.utils.RAMUtils
import com.chimeragaming.gamepulse.utils.RamThemeRenderer
import com.chimeragaming.gamepulse.utils.SharedPreferencesManager
import kotlinx.coroutines.launch

/**
 * HUD Overlay Activity for displaying compact system information
 * v0.3: Updated to use correct total RAM and handle theme updates
 */
class HudOverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHudOverlayBinding
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var ramThemeRenderer: RamThemeRenderer
    private lateinit var batteryThemeRenderer: BatteryThemeRenderer

    private var batteryMonitorService: BatteryMonitorService? = null
    private var isServiceBound = false

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BatteryMonitorService.LocalBinder
            batteryMonitorService = binder.getService()
            isServiceBound = true

            // Observe battery info from service
            lifecycleScope.launch {
                batteryMonitorService?.batteryInfo?.collect { info ->
                    updateBatteryDisplay(info)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            batteryMonitorService = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHudOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPreferencesManager(this)
        ramThemeRenderer = RamThemeRenderer(this)
        batteryThemeRenderer = BatteryThemeRenderer(this)

        setupUI()
        startBatteryMonitoring()
        startPeriodicUpdates()
    }

    private fun setupUI() {
        binding.closeButton.setOnClickListener {
            finish()
        }

        binding.settingsButton.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun startBatteryMonitoring() {
        val intent = Intent(this, BatteryMonitorService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startPeriodicUpdates() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateRAMDisplay()
                handler.postDelayed(this, prefsManager.refreshRate * 1000L)
            }
        }
        handler.post(updateRunnable!!)
    }

    // v0.3: Fixed to show correct total RAM and type conversions
    private fun updateRAMDisplay() {
        val ramInfo = RAMUtils.getRAMInfo(this)
        ramInfo?.let {
            // Update header text - use getTotalMemoryFormatted() for proper display
            binding.ramHeaderText.text = String.format(
                "RAM: %.2f/%s GB",
                it.usedMemoryGB,
                it.getTotalMemoryFormatted()
            )

            // Render RAM indicators based on theme
            // v0.3 FIX: usedGB as Float, totalGB as Int
            ramThemeRenderer.renderRAM(
                binding.ramIndicatorsContainer,
                it.usedMemoryGB.toFloat(),
                it.totalMemoryGB.toInt(),
                prefsManager.ramTheme
            )
        }
    }

    private fun updateBatteryDisplay(batteryInfo: BatteryInfo?) {
        batteryThemeRenderer.renderBattery(
            binding.batteryStatsPanel,
            binding.batteryPowerCell,
            binding.batteryGauge,
            binding.batteryMinimal,
            batteryInfo,
            prefsManager.batteryTheme
        )
    }

    private fun showSettingsDialog() {
        val dialog = HudSettingsDialog(this) { refreshRate, ramTheme, batteryTheme ->
            // Save preferences
            prefsManager.refreshRate = refreshRate // If it expects Int            prefsManager.batteryTheme = batteryTheme

            // Restart periodic updates with new refresh rate
            updateRunnable?.let { handler.removeCallbacks(it) }
            startPeriodicUpdates()

            // Update displays immediately
            updateRAMDisplay()
            batteryMonitorService?.batteryInfo?.value?.let { updateBatteryDisplay(it) }
        }
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
        updateRunnable?.let { handler.removeCallbacks(it) }
    }
}