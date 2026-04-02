package com.chimeragaming.gamepulse.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chimeragaming.gamepulse.BuildConfig
import com.chimeragaming.gamepulse.R
import com.chimeragaming.gamepulse.databinding.ActivityMainBinding
import com.chimeragaming.gamepulse.model.BatteryInfo
import com.chimeragaming.gamepulse.service.OverlayService
import com.chimeragaming.gamepulse.utils.BatteryUtils
import com.chimeragaming.gamepulse.utils.ThemeManager
import com.chimeragaming.gamepulse.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                        MAIN ACTIVITY                                  ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║             v0.3.2 - SNES Rainbow + Crash Protection                  ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var appUsageAdapter: AppUsageAdapter

    // All sections start collapsed (matching XML visibility="gone")
    private var batteryMonitoringExpanded = false
    private var ramMonitoringExpanded = false
    private var batteryTestExpanded = false
    private var themesExpanded = false
    private var diagnosticModeEnabled = false
    private var diagnosticTapCount = 0
    private var lastDiagnosticTapAtMs = 0L
    private var firstBatteryInfoAtMs = 0L
    private var latestBatteryInfo: BatteryInfo? = null

    companion object {
        private const val DIAGNOSTIC_TAP_TARGET = 5
        private const val DIAGNOSTIC_TAP_TIMEOUT_MS = 5000L
        private const val OPTIONAL_SENSOR_GRACE_MS = 1500L
        private const val ESTIMATED_LIFE_GRACE_MS = 3500L
        private const val DIAGNOSTIC_SNACKBAR_DURATION_MS = 900
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            showPermissionExplanation("Notification permission is required for background monitoring")
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            startOverlayService()
        } else {
            showPermissionExplanation("Overlay permission is required to display HUD")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // CRITICAL: Apply theme BEFORE setContentView
        try {
            ThemeManager.applyTheme(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Theme application failed", e)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupUI()
        setupObservers()
        requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        updateOverlayButtonText()
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                          SETUP UI COMPONENTS                          ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun setupUI() {
        try {
            // Update interval spinner
            val intervals = arrayOf("1 second", "10 seconds", "30 seconds", "60 seconds")
            val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.updateIntervalSpinner.adapter = spinnerAdapter

            // Set default to "10 seconds" (index 1)
            binding.updateIntervalSpinner.setSelection(1)

            binding.updateIntervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val intervalSeconds = when (position) {
                        0 -> 1   // 1 second
                        1 -> 10  // 10 seconds
                        2 -> 30  // 30 seconds
                        3 -> 60  // 60 seconds
                        else -> 10
                    }
                    viewModel.setUpdateInterval(intervalSeconds)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up spinner", e)
        }

        try {
            // RecyclerView setup
            appUsageAdapter = AppUsageAdapter()
            binding.appUsageRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = appUsageAdapter
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up RecyclerView", e)
        }

        // Setup version badge - SAFE
        try {
            binding.versionBadge?.text = "v${BuildConfig.VERSION_NAME}"
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting version badge text", e)
            binding.versionBadge?.text = "v0.4.2"
        }

        // Version badge click listener - SAFE
        try {
            binding.versionBadge?.setOnClickListener {
                copyWebsiteToClipboard()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting version badge listener", e)
        }

        // Changelog badge click listener - SAFE
        try {
            binding.changelogBadge?.setOnClickListener {
                openChangelog()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting changelog badge listener", e)
        }

        // Issues badge click listener - SAFE
        try {
            binding.issuesBadge?.setOnClickListener {
                openIssues()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting issues badge listener", e)
        }

        // Button listeners - SAFE
        try {
            binding.enableHudButton?.setOnClickListener {
                startActivity(Intent(this, HudOverlayActivity::class.java))
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting HUD button listener", e)
        }

        try {
            binding.enableOverlayButton?.setOnClickListener {
                if (isOverlayRunning()) {
                    stopOverlayService()
                } else {
                    if (Settings.canDrawOverlays(this)) {
                        startOverlayService()
                    } else {
                        requestOverlayPermission()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting overlay button listener", e)
        }

        // Game Collection button - SAFE
        try {
            binding.gameCollectionButton?.setOnClickListener {
                startActivity(Intent(this, GameCollectionActivity::class.java))
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting game collection button listener", e)
        }

        // Collapsible sections - SAFE
        try {
            binding.batteryMonitoringHeader?.let { header ->
                binding.batteryMonitoringContent?.let { content ->
                    header.setOnClickListener {
                        toggleSection(
                            header,
                            content,
                            "Battery Monitoring",
                            batteryMonitoringExpanded
                        ) { batteryMonitoringExpanded = it }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting battery monitoring section", e)
        }

        try {
            binding.ramMonitoringHeader?.let { header ->
                binding.ramMonitoringContent?.let { content ->
                    header.setOnClickListener {
                        toggleSection(
                            header,
                            content,
                            "RAM Monitoring",
                            ramMonitoringExpanded
                        ) { ramMonitoringExpanded = it }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting RAM monitoring section", e)
        }

        try {
            binding.batteryTestHeader?.let { header ->
                binding.batteryTestContent?.let { content ->
                    header.setOnClickListener {
                        toggleSection(
                            header,
                            content,
                            "Battery Test",
                            batteryTestExpanded
                        ) { batteryTestExpanded = it }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting battery test section", e)
        }

        try {
            binding.themesHeader?.let { header ->
                binding.themesContent?.let { content ->
                    header.setOnClickListener {
                        toggleSection(
                            header,
                            content,
                            "App Themes",
                            themesExpanded
                        ) { themesExpanded = it }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting themes section", e)
        }

        try {
            binding.startAnalysisButton.setOnClickListener {
                if (checkUsageStatsPermission()) {
                    showBatteryTestSetup()
                } else {
                    requestUsageStatsPermission()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting start analysis button", e)
        }

        try {
            binding.stopAnalysisButton.setOnClickListener {
                viewModel.stopBatteryAnalysis()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting stop analysis button", e)
        }

        try {
            binding.diagnosticFooterText.setOnClickListener {
                handleDiagnosticFooterTap()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting diagnostic footer listener", e)
        }

        updateOverlayButtonText()
        setupThemeSpinner()
        updateDiagnosticModeUi()

        // Apply SNES Rainbow colors if that theme is active
        applySNESRainbowColors()
    }

    private fun updateOverlayButtonText() {
        try {
            binding.enableOverlayButton?.text = if (isOverlayRunning()) {
                getString(R.string.disable_overlay)
            } else {
                getString(R.string.enable_overlay)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating overlay button text", e)
        }
    }

    private fun isOverlayRunning(): Boolean {
        return try {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            manager.getRunningServices(Integer.MAX_VALUE).any {
                it.service.className == OverlayService::class.java.name
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking overlay status", e)
            false
        }
    }

    private fun stopOverlayService() {
        try {
            val intent = Intent(this, OverlayService::class.java)
            stopService(intent)
            updateOverlayButtonText()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping overlay service", e)
        }
    }

    private fun setupThemeSpinner() {
        try {
            val spinner = binding.themeSpinner ?: return
            val currentText = binding.currentThemeText ?: return

            val themes = ThemeManager.getAvailableThemes()
            val themeLabels = ThemeManager.getThemeLabels()

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeLabels)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            val currentTheme = ThemeManager.getCurrentTheme(this)
            val currentIndex = themes.indexOf(currentTheme)
            if (currentIndex >= 0) {
                spinner.setSelection(currentIndex)
            }

            currentText.text = ThemeManager.getThemeDisplayName(currentTheme)

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedTheme = themes[position]
                    val savedTheme = ThemeManager.getCurrentTheme(this@MainActivity)
                    if (selectedTheme != savedTheme) {
                        ThemeManager.setTheme(this@MainActivity, selectedTheme)
                        currentText.text = ThemeManager.getThemeDisplayName(selectedTheme)

                        Snackbar.make(binding.root, "Theme applied! Tap Restart to see changes.", Snackbar.LENGTH_LONG)
                            .setAction("Restart") { recreate() }
                            .show()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up theme spinner", e)
        }
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                    SNES RAINBOW SECTION COLORS                        ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun applySNESRainbowColors() {
        val currentTheme = ThemeManager.getCurrentTheme(this)

        if (currentTheme == ThemeManager.THEME_SNES_RAINBOW) {
            // Apply SNES button colors to section headers
            try {
                // 🟦 X-Button Blue → Battery Monitoring
                binding.batteryMonitoringHeader?.setTextColor(
                    android.graphics.Color.parseColor("#3A66FF")
                )

                // 🟩 Y-Button Green → RAM Monitoring
                binding.ramMonitoringHeader?.setTextColor(
                    android.graphics.Color.parseColor("#3CB44A")
                )

                // 🟨 B-Button Yellow → Battery Test
                binding.batteryTestHeader?.setTextColor(
                    android.graphics.Color.parseColor("#E6C32F")
                )

                // 🟥 A-Button Red → App Themes
                binding.themesHeader?.setTextColor(
                    android.graphics.Color.parseColor("#D93A3A")
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Error applying SNES Rainbow colors", e)
            }
        }
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                         OBSERVERS                                     ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.batteryInfo.collect { batteryInfo ->
                batteryInfo?.let {
                    try {
                        latestBatteryInfo = it

                        if (firstBatteryInfoAtMs == 0L) {
                            firstBatteryInfoAtMs = System.currentTimeMillis()
                            scheduleOptionalBatteryRowRefreshes()
                        }

                        binding.batteryVoltageText.text = String.format("%.2f V", it.voltage)
                        binding.batteryLevelText.text = String.format("%.1f%%", it.percentage)
                        binding.batteryStatusText.text = it.status
                        binding.batteryHealthText.text = it.health
                        updateOptionalBatteryRows(it)
                        updateThermalSensorDisplay()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error updating battery info", e)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.ramInfo.collect { ramInfo ->
                ramInfo?.let {
                    try {
                        binding.ramUsageText.text = it.getUsedMemoryFormatted()
                        binding.ramTotalText.text = it.getTotalMemoryFormatted()
                        binding.ramProgressBar.progress = it.usagePercentage.toInt()
                        binding.ramPercentageText.text = String.format("%.1f%%", it.usagePercentage)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error updating RAM info", e)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isAnalysisRunning.collect { isRunning ->
                try {
                    binding.startAnalysisButton.isEnabled = !isRunning
                    binding.stopAnalysisButton.isEnabled = isRunning
                    binding.analysisProgressBar.visibility = if (isRunning) View.VISIBLE else View.GONE
                    binding.analysisStatusText.visibility = if (isRunning) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating analysis running state", e)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.analysisProgress.collect { progress ->
                try {
                    binding.analysisProgressBar.progress = progress
                    binding.analysisStatusText.text = "Analysis: $progress% complete"
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating analysis progress", e)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.analysisResult.collect { result ->
                result?.let {
                    if (it.isComplete) {
                        try {
                            val intent = Intent(this@MainActivity, BatteryTestResultsActivity::class.java).apply {
                                putExtra(BatteryTestResultsActivity.EXTRA_BATTERY_RESULT, it)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error starting BatteryTestResultsActivity", e)
                        }
                    }
                }
            }
        }
    }

    private fun updateThermalSensorDisplay() {
        if (!diagnosticModeEnabled) {
            binding.thermalSensorsLabel.visibility = View.GONE
            binding.thermalSensorsText.visibility = View.GONE
            return
        }

        val sensorReadings = BatteryUtils.getTemperatureSensorReadings(this)
        if (sensorReadings.isEmpty()) {
            binding.thermalSensorsLabel.visibility = View.GONE
            binding.thermalSensorsText.visibility = View.GONE
            return
        }

        binding.thermalSensorsLabel.visibility = View.VISIBLE
        binding.thermalSensorsText.visibility = View.VISIBLE
        binding.thermalSensorsText.text = sensorReadings.joinToString("\n") { reading ->
            "${reading.label}: ${String.format("%.1f C", reading.temperatureC)}"
        }
    }

    private fun updateOptionalBatteryRows(batteryInfo: BatteryInfo) {
        val elapsedSinceFirstReading = if (firstBatteryInfoAtMs > 0L) {
            System.currentTimeMillis() - firstBatteryInfoAtMs
        } else {
            0L
        }
        val showSensorSearchState = elapsedSinceFirstReading < OPTIONAL_SENSOR_GRACE_MS
        val showEstimatedLifeSearchState = elapsedSinceFirstReading < ESTIMATED_LIFE_GRACE_MS

        val hasDeviceTemperature = batteryInfo.temperature > 0f
        binding.batteryTemperatureRow.visibility = if (hasDeviceTemperature || showSensorSearchState) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.batteryTemperatureText.text = when {
            hasDeviceTemperature -> formatTemperatureValue(batteryInfo.temperature)
            showSensorSearchState -> getString(R.string.searching)
            else -> ""
        }

        val hasSocTemperature = batteryInfo.socTemperature > 0f
        binding.socTemperatureRow.visibility = if (hasSocTemperature || showSensorSearchState) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.socTemperatureText.text = when {
            hasSocTemperature -> formatTemperatureValue(batteryInfo.socTemperature)
            showSensorSearchState -> getString(R.string.searching)
            else -> ""
        }

        val canEstimateBatteryLife = !batteryInfo.status.contains("Charging", ignoreCase = true) &&
            !batteryInfo.status.contains("Full", ignoreCase = true)
        val hasEstimatedLife = batteryInfo.estimatedLifeMinutes > 0
        val isStillSearchingForEstimatedLife = canEstimateBatteryLife &&
            batteryInfo.estimatedLifeMinutes < 0 &&
            showEstimatedLifeSearchState

        binding.estimatedLifeRow.visibility = if (hasEstimatedLife || isStillSearchingForEstimatedLife) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.estimatedLifeText.text = when {
            hasEstimatedLife -> batteryInfo.getEstimatedLifeFormatted()
            isStillSearchingForEstimatedLife -> getString(R.string.searching)
            else -> ""
        }
    }

    private fun scheduleOptionalBatteryRowRefreshes() {
        lifecycleScope.launch {
            delay(OPTIONAL_SENSOR_GRACE_MS)
            latestBatteryInfo?.let { updateOptionalBatteryRows(it) }

            val estimatedLifeDelay = ESTIMATED_LIFE_GRACE_MS - OPTIONAL_SENSOR_GRACE_MS
            if (estimatedLifeDelay > 0L) {
                delay(estimatedLifeDelay)
                latestBatteryInfo?.let { updateOptionalBatteryRows(it) }
            }
        }
    }

    private fun formatTemperatureValue(temperatureC: Float): String {
        return if (temperatureC > 0f) {
            String.format("%.1f°C", temperatureC)
        } else {
            getString(R.string.na)
        }
    }

    private fun handleDiagnosticFooterTap() {
        val now = System.currentTimeMillis()
        if ((now - lastDiagnosticTapAtMs) > DIAGNOSTIC_TAP_TIMEOUT_MS) {
            diagnosticTapCount = 0
        }
        lastDiagnosticTapAtMs = now
        diagnosticTapCount++

        val togglingToEnabled = !diagnosticModeEnabled
        val tapsRemaining = DIAGNOSTIC_TAP_TARGET - diagnosticTapCount

        if (tapsRemaining <= 0) {
            diagnosticModeEnabled = togglingToEnabled
            diagnosticTapCount = 0
            updateDiagnosticModeUi()
            val message = if (diagnosticModeEnabled) {
                "Diagnostic Mode enabled"
            } else {
                "Diagnostic Mode disabled"
            }
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                .setDuration(DIAGNOSTIC_SNACKBAR_DURATION_MS)
                .show()
            return
        }

        val actionText = if (togglingToEnabled) {
            "enable Diagnostic Mode"
        } else {
            "disable Diagnostic Mode"
        }
        Snackbar.make(
            binding.root,
            "Tap $tapsRemaining more times to $actionText",
            Snackbar.LENGTH_SHORT
        )
            .setDuration(DIAGNOSTIC_SNACKBAR_DURATION_MS)
            .show()
    }

    private fun updateDiagnosticModeUi() {
        updateThermalSensorDisplay()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                      COLLAPSIBLE SECTIONS                             ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun toggleSection(
        header: TextView,
        content: View,
        sectionName: String,
        isExpanded: Boolean,
        setExpanded: (Boolean) -> Unit
    ) {
        try {
            if (isExpanded) {
                // Currently open, so close it
                content.visibility = View.GONE
                header.text = "▶ $sectionName"
                setExpanded(false)
            } else {
                // Currently closed, so open it
                content.visibility = View.VISIBLE
                header.text = "▼ $sectionName"
                setExpanded(true)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error toggling section: $sectionName", e)
        }
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                        BADGE ACTIONS                                  ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun copyWebsiteToClipboard() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("GamePulse Website", "https://github.com/ChimeraGaming/GamePulse")
            clipboard.setPrimaryClip(clip)
            Snackbar.make(binding.root, "Website copied to clipboard", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error copying website to clipboard", e)
        }
    }

    private fun openChangelog() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ChimeraGaming/GamePulse/blob/main/CHANGELOG.md"))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error opening changelog", e)
        }
    }

    private fun openIssues() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ChimeraGaming/GamePulse/issues"))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error opening issues", e)
        }
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                        PERMISSIONS                                    ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun startOverlayService() {
        try {
            val intent = Intent(this, OverlayService::class.java)
            startService(intent)
            updateOverlayButtonText()
            MaterialAlertDialogBuilder(this)
                .setTitle("Overlay Started")
                .setMessage("The overlay HUD is now running. You can see it on your home screen.")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting overlay service", e)
        }
    }

    private fun showBatteryTestSetup() {
        try {
            val dialog = BatteryTestSetupDialog(this) { durationMinutes, gameName, systemName, packageName ->
                viewModel.startBatteryAnalysis(durationMinutes, gameName, systemName, packageName)
            }
            dialog.show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing battery test setup", e)
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking usage stats permission", e)
            false
        }
    }

    private fun requestUsageStatsPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Usage Stats Permission Required")
            .setMessage("This permission is needed to detect running apps for battery analysis.")
            .setPositiveButton("Grant") { _, _ ->
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPermissionExplanation(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
