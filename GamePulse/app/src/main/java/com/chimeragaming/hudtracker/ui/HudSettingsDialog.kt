package com.chimeragaming.gamepulse.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.chimeragaming.gamepulse.databinding.DialogHudSettingsBinding
import com.chimeragaming.gamepulse.utils.BatteryThemeRenderer
import com.chimeragaming.gamepulse.utils.RamThemeRenderer
import com.chimeragaming.gamepulse.utils.SharedPreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog for configuring HUD settings
 * v0.3: Widened dialog to 90% of screen width
 */
class HudSettingsDialog(
    context: Context,
    private val onSave: (refreshRate: Int, ramTheme: String, batteryTheme: String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogHudSettingsBinding
    private val prefsManager = SharedPreferencesManager(context)

    // Refresh rates in seconds
    private val refreshRates = arrayOf(1, 10, 60)
    private val refreshRateLabels = arrayOf("1 Second", "10 Seconds", "1 Minute")

    // RAM themes
    private val ramThemes = arrayOf(
        RamThemeRenderer.THEME_POWER_CORES,
        RamThemeRenderer.THEME_HEART_CONTAINERS,
        RamThemeRenderer.THEME_DIAMONDS,
        RamThemeRenderer.THEME_HEXAGONS,
        RamThemeRenderer.THEME_PROGRESS_BAR,
        RamThemeRenderer.THEME_OFF
    )
    private val ramThemeLabels = arrayOf(
        "Power Cores",
        "Heart Containers",
        "Diamonds",
        "Hexagons",
        "Progress Bar",
        "Off"
    )

    // Battery themes
    private val batteryThemes = arrayOf(
        BatteryThemeRenderer.THEME_STATS_PANEL,
        BatteryThemeRenderer.THEME_POWER_CELL,
        BatteryThemeRenderer.THEME_GAUGE,
        BatteryThemeRenderer.THEME_MINIMAL,
        BatteryThemeRenderer.THEME_OFF
    )
    private val batteryThemeLabels = arrayOf(
        "Stats Panel",
        "Power Cell",
        "Gauge",
        "Minimal",
        "Off"
    )

    private var selectedRamTheme = prefsManager.ramTheme
    private var selectedBatteryTheme = prefsManager.batteryTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogHudSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    // v0.3: Set dialog width to 90% of screen
    override fun onStart() {
        super.onStart()
        window?.let { dialogWindow ->
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)

            val width = (size.x * 0.9).toInt() // 90% of screen width
            dialogWindow.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun setupUI() {
        // Setup refresh rate spinner
        val refreshRateAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, refreshRateLabels)
        refreshRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.refreshRateSpinner.adapter = refreshRateAdapter

        // Set current refresh rate
        val currentRateIndex = refreshRates.indexOf(prefsManager.refreshRate.toInt())
        if (currentRateIndex >= 0) {
            binding.refreshRateSpinner.setSelection(currentRateIndex)
        }

        // Setup RAM theme spinner
        val ramThemeAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, ramThemeLabels)
        ramThemeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.ramThemeSpinner.adapter = ramThemeAdapter

        // Set current RAM theme
        val currentRamIndex = ramThemes.indexOf(prefsManager.ramTheme)
        if (currentRamIndex >= 0) {
            binding.ramThemeSpinner.setSelection(currentRamIndex)
        }

        // Setup Battery theme spinner
        val batteryThemeAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, batteryThemeLabels)
        batteryThemeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.batteryThemeSpinner.adapter = batteryThemeAdapter

        // Set current Battery theme
        val currentBatteryIndex = batteryThemes.indexOf(prefsManager.batteryTheme)
        if (currentBatteryIndex >= 0) {
            binding.batteryThemeSpinner.setSelection(currentBatteryIndex)
        }

        // Setup listeners for mutual exclusion
        binding.ramThemeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRamTheme = ramThemes[position]
                validateThemeSelection()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.batteryThemeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBatteryTheme = batteryThemes[position]
                validateThemeSelection()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup buttons
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.saveButton.setOnClickListener {
            val selectedRateIndex = binding.refreshRateSpinner.selectedItemPosition
            val refreshRate = refreshRates[selectedRateIndex]

            val selectedRamIndex = binding.ramThemeSpinner.selectedItemPosition
            val ramTheme = ramThemes[selectedRamIndex]

            val selectedBatteryIndex = binding.batteryThemeSpinner.selectedItemPosition
            val batteryTheme = batteryThemes[selectedBatteryIndex]

            // Validate that both can't be off
            if (ramTheme == RamThemeRenderer.THEME_OFF && batteryTheme == BatteryThemeRenderer.THEME_OFF) {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Invalid Configuration")
                    .setMessage("Both RAM and Battery themes cannot be set to Off at the same time.")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }

            onSave(refreshRate, ramTheme, batteryTheme)
            dismiss()
        }
    }

    private fun validateThemeSelection() {
        // If RAM is set to Off, disable Battery Off option
        // If Battery is set to Off, disable RAM Off option
        // This is a simplified validation - a more sophisticated approach would
        // disable specific spinner items, but Android Spinner doesn't support that easily
    }
}