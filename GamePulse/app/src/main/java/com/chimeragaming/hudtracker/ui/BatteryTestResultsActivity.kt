package com.chimeragaming.gamepulse.ui

import android.content.Intent
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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class BatteryTestResultsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBatteryTestResultsBinding
    private lateinit var result: BatteryAnalysisResult
    
    companion object {
        const val EXTRA_BATTERY_RESULT = "extra_battery_result"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatteryTestResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_BATTERY_RESULT, BatteryAnalysisResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_BATTERY_RESULT) as? BatteryAnalysisResult
        } ?: run {
            finish()
            return
        }
        
        setupUI()
    }
    
    private fun setupUI() {
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
    }
    
    private fun displayGameInfo() {
        binding.gameNameText.text = result.gameName ?: "Unknown Game"
        
        if (result.systemName != null) {
            binding.systemInfoLayout.visibility = View.VISIBLE
            binding.systemNameText.text = result.systemName
        } else {
            binding.systemInfoLayout.visibility = View.GONE
        }
        
        binding.durationText.text = result.getDurationLabel()
    }
    
    private fun displayBatteryUsage() {
        binding.batteryDrainText.text = String.format("%.1f%%", result.batteryDrainPercent)
        binding.startBatteryText.text = String.format("%d%%", result.startBatteryLevel)
        binding.endBatteryText.text = String.format("%d%%", result.endBatteryLevel)
        binding.voltageDropText.text = String.format("%.2fV", result.voltageDrop)
    }
    
    private fun displayPlaytimeEstimate() {
        val estimateHours = result.estimatedLifeHours
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
    }
    
    private fun displayTopApps() {
        if (result.topApps.isEmpty()) {
            binding.topAppsCard.visibility = View.GONE
            return
        }
        
        binding.topAppsCard.visibility = View.VISIBLE
        binding.topAppsContainer.removeAllViews()
        
        result.topApps.take(5).forEach { app ->
            val appView = layoutInflater.inflate(
                R.layout.item_battery_app_detail,
                binding.topAppsContainer,
                false
            )
            
            appView.findViewById<TextView>(R.id.appNameText).text = app.appName
            appView.findViewById<TextView>(R.id.batteryPercentText).text = 
                String.format("%.1f%%", app.batteryDrainPercent)
            
            val barText = createVisualBar(app.batteryDrainPercent, result.batteryDrainPercent)
            appView.findViewById<TextView>(R.id.batteryBarText).text = barText
            
            binding.topAppsContainer.addView(appView)
        }
    }
    
    private fun createVisualBar(appDrain: Float, totalDrain: Float): String {
        val percentage = if (totalDrain > 0) (appDrain / totalDrain * 100).coerceIn(0f, 100f) else 0f
        val filledBlocks = (percentage / 10).roundToInt()
        val emptyBlocks = 10 - filledBlocks
        
        return "█".repeat(filledBlocks) + "░".repeat(emptyBlocks)
    }
    
    private fun shareScreenshot() {
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
                    putExtra(Intent.EXTRA_TEXT, generateShareText())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(Intent.createChooser(shareIntent, "Share Battery Test Results"))
            } catch (e: Exception) {
                e.printStackTrace()
                Snackbar.make(
                    binding.root,
                    "Failed to share screenshot: ${e.message}",
                    Snackbar.LENGTH_SHORT
                ).show()
            } finally {
                binding.shareButton.isEnabled = true
            }
        }
    }
    
    private fun captureScreenshot(): Bitmap {
        val scrollView = binding.root
        val bitmap = Bitmap.createBitmap(
            scrollView.getChildAt(0).width,
            scrollView.getChildAt(0).height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        scrollView.getChildAt(0).draw(canvas)
        return bitmap
    }
    
    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val file = File(cacheDir, "battery_test_result_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
    
    private fun generateShareText(): String {
        val gameName = result.gameName ?: "Unknown Game"
        val system = result.systemName?.let { " ($it)" } ?: ""
        val hours = result.estimatedLifeHours.toInt()
        val minutes = ((result.estimatedLifeHours - hours) * 60).roundToInt()
        val estimate = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        
        return "Battery Test Results for $gameName$system\n" +
               "Duration: ${result.getDurationLabel()}\n" +
               "Battery Drain: ${String.format("%.1f%%", result.batteryDrainPercent)}\n" +
               "Full Playtime Estimate: $estimate at 100% battery\n" +
               "\n#HUDTracker #BatteryTest"
    }
}
