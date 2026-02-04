package com.chimeragaming.gamepulse.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
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
import com.chimeragaming.gamepulse.utils.ThemeManager
import kotlinx.coroutines.launch

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                    HUD OVERLAY ACTIVITY                               ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║         v0.3.2 - Theme Support + Dual Screen & Crash Protection       ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
class HudOverlayActivity : AppCompatActivity() {

    private var _binding: ActivityHudOverlayBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var ramThemeRenderer: RamThemeRenderer
    private lateinit var batteryThemeRenderer: BatteryThemeRenderer

    private var batteryMonitorService: BatteryMonitorService? = null
    private var isServiceBound = false

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                val binder = service as BatteryMonitorService.LocalBinder
                batteryMonitorService = binder.getService()
                isServiceBound = true

                lifecycleScope.launch {
                    batteryMonitorService?.batteryInfo?.collect { info ->
                        if (_binding != null) {
                            updateBatteryDisplay(info)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            batteryMonitorService = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // CRITICAL: Apply theme BEFORE setContentView
        try {
            ThemeManager.applyTheme(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onCreate(savedInstanceState)

        try {
            _binding = ActivityHudOverlayBinding.inflate(layoutInflater)
            setContentView(binding.root)

            prefsManager = SharedPreferencesManager(this)
            ramThemeRenderer = RamThemeRenderer(this)
            batteryThemeRenderer = BatteryThemeRenderer(this)

            setupUI()
            startBatteryMonitoring()
            startPeriodicUpdates()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        try {
            if (_binding != null) {
                updateRAMDisplay()
                batteryMonitorService?.batteryInfo?.value?.let { updateBatteryDisplay(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupUI() {
        try {
            binding.closeButton.setOnClickListener {
                finish()
            }

            binding.settingsButton.setOnClickListener {
                showSettingsDialog()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startBatteryMonitoring() {
        try {
            val intent = Intent(this, BatteryMonitorService::class.java)
            startForegroundService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPeriodicUpdates() {
        updateRunnable = object : Runnable {
            override fun run() {
                if (_binding != null && !isFinishing && !isDestroyed) {
                    try {
                        updateRAMDisplay()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val refreshRate = try {
                        prefsManager.refreshRate
                    } catch (e: Exception) {
                        10
                    }

                    handler.postDelayed(this, refreshRate * 1000L)
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun updateRAMDisplay() {
        if (_binding == null || isFinishing || isDestroyed) {
            return
        }

        try {
            val ramInfo = RAMUtils.getRAMInfo(this)
            ramInfo?.let {
                binding.ramHeaderText.text = String.format(
                    "RAM: %.2f/%s GB",
                    it.usedMemoryGB,
                    it.getTotalMemoryFormatted()
                )

                ramThemeRenderer.renderRAM(
                    binding.ramIndicatorsContainer,
                    it.usedMemoryGB.toFloat(),
                    it.totalMemoryGB.toInt(),
                    prefsManager.ramTheme
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBatteryDisplay(batteryInfo: BatteryInfo?) {
        if (_binding == null || isFinishing || isDestroyed) {
            return
        }

        try {
            batteryThemeRenderer.renderBattery(
                binding.batteryStatsPanel,
                binding.batteryPowerCell,
                binding.batteryGauge,
                binding.batteryMinimal,
                batteryInfo,
                prefsManager.batteryTheme
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSettingsDialog() {
        try {
            val dialog = HudSettingsDialog(this) { refreshRate, ramTheme, batteryTheme ->
                try {
                    prefsManager.refreshRate = refreshRate
                    prefsManager.ramTheme = ramTheme
                    prefsManager.batteryTheme = batteryTheme

                    updateRunnable?.let { handler.removeCallbacks(it) }
                    startPeriodicUpdates()

                    updateRAMDisplay()
                    batteryMonitorService?.batteryInfo?.value?.let { updateBatteryDisplay(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            if (isServiceBound) {
                unbindService(serviceConnection)
                isServiceBound = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            updateRunnable?.let { handler.removeCallbacks(it) }
            updateRunnable = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _binding = null
    }
}