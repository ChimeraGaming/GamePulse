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

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                      HUD SETTINGS DIALOG                              ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║                      v0.3.1 - Fixed Layout                            ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
class HudSettingsDialog(
    context: Context,
    private val onSave: (refreshRate: Int, ramTheme: String, batteryTheme: String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogHudSettingsBinding
    private val prefsManager = SharedPreferencesManager(context)

    private val refreshRates = arrayOf(1, 10, 60)
    private val refreshRateLabels = arrayOf("1 Second", "10 Seconds", "1 Minute")

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

    override fun onStart() {
        super.onStart()
        window?.let { dialogWindow ->
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)

            val width = (size.x * 0.9).toInt()
            dialogWindow.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun setupUI() {
        setupRefreshRateSpinner()
        setupRamThemeSpinner()
        setupBatteryThemeSpinner()
        setupButtons()
    }

    private fun setupRefreshRateSpinner() {
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, refreshRateLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.refreshRateSpinner.adapter = adapter

        val currentRate = prefsManager.refreshRate
        val currentIndex = refreshRates.indexOf(currentRate)
        if (currentIndex >= 0) {
            binding.refreshRateSpinner.setSelection(currentIndex)
            binding.refreshRateSelectedText.text = refreshRateLabels[currentIndex]
        }

        binding.refreshRateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.refreshRateSelectedText.text = refreshRateLabels[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRamThemeSpinner() {
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, ramThemeLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.ramThemeSpinner.adapter = adapter

        val currentIndex = ramThemes.indexOf(prefsManager.ramTheme)
        if (currentIndex >= 0) {
            binding.ramThemeSpinner.setSelection(currentIndex)
            binding.ramThemeSelectedText.text = ramThemeLabels[currentIndex]
        }

        binding.ramThemeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRamTheme = ramThemes[position]
                binding.ramThemeSelectedText.text = ramThemeLabels[position]
                validateThemeSelection()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupBatteryThemeSpinner() {
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, batteryThemeLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.batteryThemeSpinner.adapter = adapter

        val currentIndex = batteryThemes.indexOf(prefsManager.batteryTheme)
        if (currentIndex >= 0) {
            binding.batteryThemeSpinner.setSelection(currentIndex)
            binding.batteryThemeSelectedText.text = batteryThemeLabels[currentIndex]
        }

        binding.batteryThemeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBatteryTheme = batteryThemes[position]
                binding.batteryThemeSelectedText.text = batteryThemeLabels[position]
                validateThemeSelection()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
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
        val bothOff = selectedRamTheme == RamThemeRenderer.THEME_OFF &&
                selectedBatteryTheme == BatteryThemeRenderer.THEME_OFF

        binding.saveButton.isEnabled = !bothOff
    }
}