package com.chimeragaming.gamepulse.ui

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.chimeragaming.gamepulse.R
import com.chimeragaming.gamepulse.databinding.ActivityBatteryTestResultsBinding
import com.chimeragaming.gamepulse.model.BatteryAnalysisResult
import com.chimeragaming.gamepulse.utils.ThemeManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                BATTERY TEST RESULTS ACTIVITY                          ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║              v0.3.2 - Theme Support + Crash Protection                ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
class BatteryTestResultsActivity : AppCompatActivity() {

    private var _binding: ActivityBatteryTestResultsBinding? = null
    private val binding get() = _binding!!

    private var result: BatteryAnalysisResult? = null

    companion object {
        const val EXTRA_BATTERY_RESULT = "extra_battery_result"
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
            _binding = ActivityBatteryTestResultsBinding.inflate(layoutInflater)
            setContentView(binding.root)

            result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(EXTRA_BATTERY_RESULT, BatteryAnalysisResult::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(EXTRA_BATTERY_RESULT) as? BatteryAnalysisResult
            }

            if (result == null) {
                finish()
                return
            }

            setupUI()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        try {
            if (_binding != null && result != null) {
                // Refresh display on orientation change
                displayGameInfo()
                displayBatteryUsage()
                displayPlaytimeEstimate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupUI() {
        if (_binding == null || isFinishing || isDestroyed) {
            return
        }

        try {
            displayGameInfo()
            displayBatteryUsage()
            displayPlaytimeEstimate()
            displayTopApps()

            binding.shareButton.setOnClickListener {
                shareScreenshot()
            }

            binding.doneButton.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun displayGameInfo() {
        if (_binding == null || isFinishing || isDestroyed || result == null) {
            return
        }

        try {
            binding.gameNameText.text = result!!.gameName ?: "Unknown Game"

            if (result!!.systemName != null) {
                binding.systemInfoLayout.visibility = View.VISIBLE
                binding.systemNameText.text = result!!.systemName
            } else {
                binding.systemInfoLayout.visibility = View.GONE
            }

            binding.durationText.text = result!!.getDurationLabel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun displayBatteryUsage() {
        if (_binding == null || isFinishing || isDestroyed || result == null) {
            return
        }

        try {
            binding.batteryDrainText.text = String.format("%.1f%%", result!!.batteryDrainPercent)
            binding.startBatteryText.text = String.format("%d%%", result!!.startBatteryLevel)
            binding.endBatteryText.text = String.format("%d%%", result!!.endBatteryLevel)
            binding.voltageDropText.text = String.format("%.2fV", result!!.voltageDrop)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun displayPlaytimeEstimate() {
        if (_binding == null || isFinishing || isDestroyed || result == null) {
            return
        }

        try {
            val estimateHours = result!!.estimatedLifeHours
            val hours = estimateHours.toInt()
            val minutes = ((estimateHours - hours) * 60).roundToInt()

            val estimateText = if (hours > 0 && minutes > 0) {
                "$hours HOURS $minutes MIN"
            } else if (hours > 0) {
                "$hours HOURS"
            } else {
                "$minutes MINUTES"
            }

            binding.playtimeEstimateText.text = estimateText
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun displayTopApps() {
        if (_binding == null || isFinishing || isDestroyed || result == null) {
            return
        }

        try {
            if (result!!.topApps.isEmpty()) {
                binding.topAppsCard.visibility = View.GONE
                return
            }

            binding.topAppsCard.visibility = View.VISIBLE
            binding.topAppsContainer.removeAllViews()

            result!!.topApps.take(5).forEach { app ->
                val appView = layoutInflater.inflate(
                    R.layout.item_battery_app_detail,
                    binding.topAppsContainer,
                    false
                )

                appView.findViewById<TextView>(R.id.appNameText).text = app.appName
                appView.findViewById<TextView>(R.id.batteryPercentText).text =
                    String.format("%.1f%%", app.batteryDrainPercent)

                val barText = createVisualBar(app.batteryDrainPercent, result!!.batteryDrainPercent)
                appView.findViewById<TextView>(R.id.batteryBarText).text = barText

                binding.topAppsContainer.addView(appView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createVisualBar(appDrain: Float, totalDrain: Float): String {
        return try {
            val percentage = if (totalDrain > 0) (appDrain / totalDrain * 100).coerceIn(0f, 100f) else 0f
            val filledBlocks = (percentage / 10).roundToInt()
            val emptyBlocks = 10 - filledBlocks

            "█".repeat(filledBlocks) + "░".repeat(emptyBlocks)
        } catch (e: Exception) {
            e.printStackTrace()
            "░░░░░░░░░░"
        }
    }

    private fun shareScreenshot() {
        if (_binding == null || isFinishing || isDestroyed) {
            return
        }

        try {
            binding.shareButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.Default) {
                        captureScreenshot()
                    }

                    val file = withContext(Dispatchers.IO) {
                        saveBitmapToFile(bitmap)
                    }

                    val uri = FileProvider.getUriForFile(
                        this@BatteryTestResultsActivity,
                        "${packageName}.fileprovider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    startActivity(Intent.createChooser(shareIntent, "Share Battery Test Results"))

                } catch (e: Exception) {
                    e.printStackTrace()
                    if (_binding != null && !isFinishing && !isDestroyed) {
                        Snackbar.make(binding.root, "Failed to share screenshot", Snackbar.LENGTH_SHORT).show()
                    }
                } finally {
                    if (_binding != null && !isFinishing && !isDestroyed) {
                        binding.shareButton.isEnabled = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun captureScreenshot(): Bitmap {
        if (_binding == null || isFinishing || isDestroyed) {
            throw IllegalStateException("Activity is not in valid state")
        }

        val view = binding.root
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val file = File(cacheDir, "battery_test_result_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            result = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _binding = null
    }
}