package com.chimeragaming.gamepulse.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import com.chimeragaming.gamepulse.databinding.DialogBatteryTestSetupBinding
import com.chimeragaming.gamepulse.utils.AppDetectionService
import com.chimeragaming.gamepulse.utils.RunningAppInfo

/**
 * Dialog for battery test setup with duration and game information
 */
class BatteryTestSetupDialog(
    context: Context,
    private val onStartTest: (durationMinutes: Int, gameName: String?, systemName: String?) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogBatteryTestSetupBinding
    
    // Test durations in minutes
    private val durations = arrayOf(5, 10, 60, 120)
    private val durationLabels = arrayOf("5 Minutes", "10 Minutes", "1 Hour", "2 Hours")
    
    private var runningApps: List<RunningAppInfo> = emptyList()
    private var selectedApp: RunningAppInfo? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogBatteryTestSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        loadRunningApps()
    }
    
    private fun setupUI() {
        // Setup duration spinner
        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, durationLabels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.durationSpinner.adapter = spinnerAdapter
        binding.durationSpinner.setSelection(2) // Default to 1 Hour
        
        // Show warning about loading to title screen
        binding.warningText.visibility = View.VISIBLE
        
        // Setup refresh apps button
        binding.refreshAppsButton?.setOnClickListener {
            loadRunningApps()
        }
        
        // Setup app selection spinner
        binding.appSpinner?.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && position <= runningApps.size) {
                    selectedApp = runningApps[position - 1]
                    updateGameInfoFields()
                } else {
                    selectedApp = null
                    binding.gameInput.setText("")
                    binding.systemInput.setText("")
                }
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedApp = null
            }
        })
        
        // Setup playing game switch
        binding.playingGameSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.gameDetailsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.appSelectionContainer?.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        // Setup buttons
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        
        binding.startTestButton.setOnClickListener {
            val selectedIndex = binding.durationSpinner.selectedItemPosition
            val durationMinutes = durations[selectedIndex]
            
            val gameName = if (binding.playingGameSwitch.isChecked) {
                binding.gameInput.text?.toString()?.takeIf { it.isNotBlank() }
            } else null
            
            val systemName = if (binding.playingGameSwitch.isChecked) {
                binding.systemInput.text?.toString()?.takeIf { it.isNotBlank() }
            } else null
            
            onStartTest(durationMinutes, gameName, systemName)
            dismiss()
        }
    }
    
    private fun loadRunningApps() {
        runningApps = AppDetectionService.getRunningApps(context)
        
        // Create spinner items
        val appNames = mutableListOf("Select an app...")
        appNames.addAll(runningApps.map { app ->
            val gameInfo = if (app.detectedGameName != null) {
                " (${app.detectedGameName})"
            } else ""
            "${app.appName}$gameInfo"
        })
        
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, appNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.appSpinner?.adapter = adapter
        
        // Show message if no apps found
        if (runningApps.isEmpty()) {
            binding.noAppsText?.visibility = View.VISIBLE
        } else {
            binding.noAppsText?.visibility = View.GONE
        }
    }
    
    private fun updateGameInfoFields() {
        selectedApp?.let { app ->
            // Try to auto-fill game name if detected
            if (app.detectedGameName != null) {
                binding.gameInput.setText(app.detectedGameName)
            }
            
            // Auto-fill system name if it's an emulator
            val systemName = AppDetectionService.getSystemName(app.packageName)
            if (systemName != null) {
                binding.systemInput.setText(systemName)
            }
        }
    }
}
