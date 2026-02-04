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
import com.chimeragaming.gamepulse.service.OverlayService
import com.chimeragaming.gamepulse.utils.ThemeManager
import com.chimeragaming.gamepulse.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                        MAIN ACTIVITY                                  ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║                 v0.3.2 - SNES Rainbow Theme Support                   ║
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
        ThemeManager.applyTheme(this)

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

        // RecyclerView setup
        appUsageAdapter = AppUsageAdapter()
        binding.appUsageRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appUsageAdapter
        }

        // Setup version badge
        try {
            binding.versionBadge!!.text = "v${BuildConfig.VERSION_NAME}"
        } catch (e: Exception) {
            e.printStackTrace()
            binding.versionBadge!!.text = "v0.3.2"
        }

        // Version badge click listener
        binding.versionBadge!!.setOnClickListener {
            copyWebsiteToClipboard()
        }

        // Changelog badge click listener
        binding.changelogBadge!!.setOnClickListener {
            openChangelog()
        }

        // Issues badge click listener
        binding.issuesBadge!!.setOnClickListener {
            openIssues()
        }

        // Button listeners
        binding.enableHudButton!!.setOnClickListener {
            startActivity(Intent(this, HudOverlayActivity::class.java))
        }

        binding.enableOverlayButton!!.setOnClickListener {
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

        // Game Collection button
        binding.gameCollectionButton!!.setOnClickListener {
            startActivity(Intent(this, GameCollectionActivity::class.java))
        }

        // Collapsible sections
        binding.batteryMonitoringHeader!!.setOnClickListener {
            toggleSection(
                binding.batteryMonitoringHeader!!,
                binding.batteryMonitoringContent!!,
                "Battery Monitoring",
                batteryMonitoringExpanded
            ) { batteryMonitoringExpanded = it }
        }

        binding.ramMonitoringHeader!!.setOnClickListener {
            toggleSection(
                binding.ramMonitoringHeader!!,
                binding.ramMonitoringContent!!,
                "RAM Monitoring",
                ramMonitoringExpanded
            ) { ramMonitoringExpanded = it }
        }

        binding.batteryTestHeader!!.setOnClickListener {
            toggleSection(
                binding.batteryTestHeader!!,
                binding.batteryTestContent!!,
                "Battery Test",
                batteryTestExpanded
            ) { batteryTestExpanded = it }
        }

        binding.themesHeader!!.setOnClickListener {
            toggleSection(
                binding.themesHeader!!,
                binding.themesContent!!,
                "App Themes",
                themesExpanded
            ) { themesExpanded = it }
        }

        binding.startAnalysisButton.setOnClickListener {
            if (checkUsageStatsPermission()) {
                showBatteryTestSetup()
            } else {
                requestUsageStatsPermission()
            }
        }

        binding.stopAnalysisButton.setOnClickListener {
            viewModel.stopBatteryAnalysis()
        }

        updateOverlayButtonText()
        setupThemeSpinner()

        // Apply SNES Rainbow colors if that theme is active
        applySNESRainbowColors()
    }

    private fun updateOverlayButtonText() {
        binding.enableOverlayButton!!.text = if (isOverlayRunning()) {
            getString(R.string.disable_overlay)
        } else {
            getString(R.string.enable_overlay)
        }
    }

    private fun isOverlayRunning(): Boolean {
        return try {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            manager.getRunningServices(Integer.MAX_VALUE).any {
                it.service.className == OverlayService::class.java.name
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun stopOverlayService() {
        try {
            val intent = Intent(this, OverlayService::class.java)
            stopService(intent)
            updateOverlayButtonText()
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
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
                e.printStackTrace()
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
                    binding.batteryVoltageText.text = String.format("%.2f V", it.voltage)
                    binding.batteryLevelText.text = String.format("%.1f%%", it.percentage)
                    binding.batteryStatusText.text = it.status
                    binding.batteryHealthText.text = it.health
                    binding.batteryTemperatureText.text = String.format("%.1f°C", it.temperature)
                    binding.estimatedLifeText.text = it.getEstimatedLifeFormatted()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.ramInfo.collect { ramInfo ->
                ramInfo?.let {
                    binding.ramUsageText.text = it.getUsedMemoryFormatted()
                    binding.ramTotalText.text = it.getTotalMemoryFormatted()
                    binding.ramProgressBar.progress = it.usagePercentage.toInt()
                    binding.ramPercentageText.text = String.format("%.1f%%", it.usagePercentage)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isAnalysisRunning.collect { isRunning ->
                binding.startAnalysisButton.isEnabled = !isRunning
                binding.stopAnalysisButton.isEnabled = isRunning
                binding.analysisProgressBar.visibility = if (isRunning) View.VISIBLE else View.GONE
                binding.analysisStatusText.visibility = if (isRunning) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.analysisProgress.collect { progress ->
                binding.analysisProgressBar.progress = progress
                binding.analysisStatusText.text = "Analysis: $progress% complete"
            }
        }

        lifecycleScope.launch {
            viewModel.analysisResult.collect { result ->
                result?.let {
                    if (it.isComplete) {
                        val intent = Intent(this@MainActivity, BatteryTestResultsActivity::class.java).apply {
                            putExtra(BatteryTestResultsActivity.EXTRA_BATTERY_RESULT, it)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
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
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                        BADGE ACTIONS                                  ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun copyWebsiteToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("GamePulse Website", "https://github.com/ChimeraGaming/GamePulse")
        clipboard.setPrimaryClip(clip)
        Snackbar.make(binding.root, "Website copied to clipboard", Snackbar.LENGTH_SHORT).show()
    }

    private fun openChangelog() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ChimeraGaming/GamePulse/blob/main/CHANGELOG.md"))
        startActivity(intent)
    }

    private fun openIssues() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ChimeraGaming/GamePulse/issues"))
        startActivity(intent)
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
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateOverlayButtonText()
        MaterialAlertDialogBuilder(this)
            .setTitle("Overlay Started")
            .setMessage("The overlay HUD is now running. You can see it on your home screen.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showBatteryTestSetup() {
        val dialog = BatteryTestSetupDialog(this) { durationMinutes, gameName, systemName ->
            viewModel.startBatteryAnalysis(durationMinutes, gameName, systemName)
        }
        dialog.show()
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
        return mode == AppOpsManager.MODE_ALLOWED
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