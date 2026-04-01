package com.chimeragaming.gamepulse.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.chimeragaming.gamepulse.databinding.DialogBatteryTestSetupBinding
import com.chimeragaming.gamepulse.utils.AppDetectionService
import com.chimeragaming.gamepulse.utils.DetectedSessionSuggestion
import com.chimeragaming.gamepulse.utils.GameCollectionRepository
import com.chimeragaming.gamepulse.utils.RunningAppInfo
import com.chimeragaming.gamepulse.utils.SharedPreferencesManager
import com.chimeragaming.gamepulse.utils.normalizeBatteryTestHistoryValue
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BatteryTestSetupDialog(
    context: Context,
    private val onStartTest: (
        durationMinutes: Int,
        gameName: String?,
        systemName: String?,
        packageName: String?
    ) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogBatteryTestSetupBinding
    private val prefsManager = SharedPreferencesManager(context)
    private val collectionRepository = GameCollectionRepository(context)

    private val durations = arrayOf(5, 10, 60, 120)
    private val durationLabels = arrayOf("5 Minutes", "10 Minutes", "1 Hour", "2 Hours")

    private var runningApps: List<RunningAppInfo> = emptyList()
    private var selectedApp: RunningAppInfo? = null
    private var systemHistory: List<String> = emptyList()
    private var gameHistory: List<String> = emptyList()
    private var selectedSystemBubble: String? = null
    private var selectedGameBubble: String? = null
    private var isUpdatingInputs = false

    private val promptedSuggestions = mutableSetOf<String>()
    private val promptedDuplicateValues = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogBatteryTestSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadHistory()
        loadRunningApps()
    }

    override fun onStart() {
        super.onStart()

        val displayMetrics = context.resources.displayMetrics
        val dialogWidth = (displayMetrics.widthPixels * 0.92f).toInt()
        val dialogHeight = (displayMetrics.heightPixels * 0.82f).toInt()

        window?.setLayout(dialogWidth, dialogHeight)
    }

    private fun setupUI() {
        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, durationLabels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.durationSpinner.adapter = spinnerAdapter
        binding.durationSpinner.setSelection(2)
        binding.warningText.visibility = View.VISIBLE

        setupInputs()

        binding.refreshAppsButton.setOnClickListener {
            loadRunningApps()
        }

        binding.appSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedApp = if (position > 0 && position <= runningApps.size) {
                    runningApps[position - 1]
                } else {
                    null
                }

                selectedApp?.let { app ->
                    maybeConfirmDetectedSession(app)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedApp = null
            }
        }

        binding.playingGameSwitch.setOnCheckedChangeListener { _, isChecked ->
            val visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.gameDetailsContainer.visibility = visibility
            binding.appSelectionContainer.visibility = visibility
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.startTestButton.setOnClickListener {
            startBatteryTest()
        }
    }

    private fun setupInputs() {
        binding.systemInput.doAfterTextChanged {
            if (!isUpdatingInputs) {
                syncSystemBubbleWithInput()
            }
        }

        binding.gameInput.doAfterTextChanged {
            if (!isUpdatingInputs) {
                syncGameBubbleWithInput()
            }
        }

        binding.systemInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                maybePromptForExistingValue(
                    currentValue = binding.systemInput.text?.toString().orEmpty(),
                    historyValues = systemHistory,
                    promptKeyPrefix = "system",
                    title = "Use saved system?",
                    onUseSaved = { value -> applySystemBubble(value) }
                )
            }
        }

        binding.gameInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                maybePromptForExistingValue(
                    currentValue = binding.gameInput.text?.toString().orEmpty(),
                    historyValues = gameHistory,
                    promptKeyPrefix = "game",
                    title = "Use saved game?",
                    onUseSaved = { value -> applyGameBubble(value) }
                )
            }
        }
    }

    private fun startBatteryTest() {
        val selectedIndex = binding.durationSpinner.selectedItemPosition
        val durationMinutes = durations[selectedIndex]

        var gameName: String? = null
        var systemName: String? = null
        val packageName = if (binding.playingGameSwitch.isChecked) {
            selectedApp?.packageName
        } else {
            null
        }

        if (binding.playingGameSwitch.isChecked) {
            gameName = resolveGameName()
            systemName = resolveSystemName()

            if (!systemName.isNullOrBlank()) {
                systemName = prefsManager.saveBatteryTestSystem(systemName) ?: systemName
            }

            if (!gameName.isNullOrBlank()) {
                gameName = prefsManager.saveBatteryTestGame(gameName) ?: gameName
            }

            if (!packageName.isNullOrBlank() && !systemName.isNullOrBlank() && !gameName.isNullOrBlank()) {
                prefsManager.saveBatteryTestPackageMatch(packageName, systemName, gameName)
            }
        }

        onStartTest(durationMinutes, gameName, systemName, packageName)
        dismiss()
    }

    private fun loadRunningApps() {
        val previousPackage = selectedApp?.packageName
        runningApps = AppDetectionService.getRunningApps(context)

        val appNames = mutableListOf("Select an app...")
        appNames.addAll(runningApps.map { app ->
            val detectedGame = app.detectedGameName?.takeIf { it.isNotBlank() }
            if (detectedGame != null) {
                "${app.appName} ($detectedGame)"
            } else {
                app.appName
            }
        })

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, appNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.appSpinner.adapter = adapter
        binding.noAppsText.visibility = if (runningApps.isEmpty()) View.VISIBLE else View.GONE

        val restoredIndex = previousPackage?.let { packageName ->
            runningApps.indexOfFirst { it.packageName.equals(packageName, ignoreCase = true) }
        } ?: -1

        if (restoredIndex >= 0) {
            binding.appSpinner.setSelection(restoredIndex + 1)
        }
    }

    private fun loadHistory() {
        val trackedHistory = collectionRepository.getTrackedGameHistory()
        systemHistory = mergeHistory(
            primaryValues = prefsManager.getBatteryTestSystemHistory(),
            secondaryValues = trackedHistory.map { it.system }
        )
        gameHistory = mergeHistory(
            primaryValues = prefsManager.getBatteryTestGameHistory(),
            secondaryValues = trackedHistory.map { it.name }
        )

        selectedSystemBubble = findHistoryMatch(systemHistory, binding.systemInput.text?.toString())
        selectedGameBubble = findHistoryMatch(gameHistory, binding.gameInput.text?.toString())
        renderHistoryBubbles()
    }

    private fun mergeHistory(
        primaryValues: List<String>,
        secondaryValues: List<String>
    ): List<String> {
        val mergedValues = mutableListOf<String>()
        val seenValues = mutableSetOf<String>()

        (primaryValues + secondaryValues).forEach { value ->
            val trimmedValue = value.trim()
            if (trimmedValue.isEmpty()) {
                return@forEach
            }

            val normalizedValue = normalizeBatteryTestHistoryValue(trimmedValue)
            if (seenValues.add(normalizedValue)) {
                mergedValues.add(trimmedValue)
            }
        }

        return mergedValues.take(12)
    }

    private fun renderHistoryBubbles() {
        renderBubbleGroup(
            labelView = binding.systemHistoryLabel,
            chipGroup = binding.systemHistoryChips,
            values = systemHistory,
            selectedValue = selectedSystemBubble,
            onChipSelected = { value -> applySystemBubble(value) }
        )

        renderBubbleGroup(
            labelView = binding.gameHistoryLabel,
            chipGroup = binding.gameHistoryChips,
            values = gameHistory,
            selectedValue = selectedGameBubble,
            onChipSelected = { value -> applyGameBubble(value) }
        )
    }

    private fun renderBubbleGroup(
        labelView: TextView,
        chipGroup: ChipGroup,
        values: List<String>,
        selectedValue: String?,
        onChipSelected: (String) -> Unit
    ) {
        chipGroup.removeAllViews()

        val hasValues = values.isNotEmpty()
        labelView.visibility = if (hasValues) View.VISIBLE else View.GONE
        chipGroup.visibility = if (hasValues) View.VISIBLE else View.GONE

        if (!hasValues) {
            return
        }

        values.forEach { value ->
            val chip = Chip(context).apply {
                id = View.generateViewId()
                text = value
                isCheckable = true
                isClickable = true
                isChecked = selectedValue != null &&
                    normalizeBatteryTestHistoryValue(selectedValue) ==
                    normalizeBatteryTestHistoryValue(value)
                setOnClickListener {
                    onChipSelected(value)
                }
            }

            chipGroup.addView(chip)
        }
    }

    private fun syncSystemBubbleWithInput() {
        selectedSystemBubble = findHistoryMatch(systemHistory, binding.systemInput.text?.toString())
        renderHistoryBubbles()
    }

    private fun syncGameBubbleWithInput() {
        selectedGameBubble = findHistoryMatch(gameHistory, binding.gameInput.text?.toString())
        renderHistoryBubbles()
    }

    private fun applySystemBubble(value: String) {
        selectedSystemBubble = value
        setInputValue(binding.systemInput, value)
        renderHistoryBubbles()
    }

    private fun applyGameBubble(value: String) {
        selectedGameBubble = value
        setInputValue(binding.gameInput, value)
        renderHistoryBubbles()
    }

    private fun setInputValue(input: EditText, value: String) {
        isUpdatingInputs = true
        input.setText(value)
        input.setSelection(value.length)
        isUpdatingInputs = false
    }

    private fun maybePromptForExistingValue(
        currentValue: String,
        historyValues: List<String>,
        promptKeyPrefix: String,
        title: String,
        onUseSaved: (String) -> Unit
    ) {
        val matchingValue = findHistoryMatch(historyValues, currentValue) ?: return
        val normalizedCurrentValue = normalizeBatteryTestHistoryValue(currentValue)
        val normalizedMatchingValue = normalizeBatteryTestHistoryValue(matchingValue)

        if (normalizedCurrentValue.isEmpty() || normalizedCurrentValue != normalizedMatchingValue) {
            return
        }

        val trimmedCurrentValue = currentValue.trim()
        if (trimmedCurrentValue == matchingValue) {
            return
        }

        val promptKey = "$promptKeyPrefix:$normalizedMatchingValue"
        if (!promptedDuplicateValues.add(promptKey)) {
            return
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage("\"$matchingValue\" is already saved. Tap the $matchingValue bubble or use the saved value now.")
            .setPositiveButton("Use Saved") { _, _ ->
                onUseSaved(matchingValue)
            }
            .setNegativeButton("Keep Typing", null)
            .show()
    }

    private fun maybeConfirmDetectedSession(app: RunningAppInfo) {
        val suggestion = AppDetectionService.getSuggestedSession(context, app) ?: return
        val promptKey = buildSuggestionKey(suggestion)
        if (!promptedSuggestions.add(promptKey)) {
            return
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Detected Session")
            .setMessage("Are you playing \"${suggestion.gameName}\" on \"${suggestion.systemName}\"?")
            .setPositiveButton("Yes") { _, _ ->
                applyDetectedSuggestion(suggestion)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun buildSuggestionKey(suggestion: DetectedSessionSuggestion): String {
        return listOf(
            suggestion.packageName.lowercase(),
            normalizeBatteryTestHistoryValue(suggestion.systemName),
            normalizeBatteryTestHistoryValue(suggestion.gameName)
        ).joinToString("|")
    }

    private fun applyDetectedSuggestion(suggestion: DetectedSessionSuggestion) {
        binding.playingGameSwitch.isChecked = true

        val savedSystem = prefsManager.saveBatteryTestSystem(suggestion.systemName) ?: suggestion.systemName
        val savedGame = prefsManager.saveBatteryTestGame(suggestion.gameName) ?: suggestion.gameName
        prefsManager.saveBatteryTestPackageMatch(
            packageName = suggestion.packageName,
            systemName = savedSystem,
            gameName = savedGame
        )

        loadHistory()
        applySystemBubble(savedSystem)
        applyGameBubble(savedGame)
    }

    private fun findHistoryMatch(
        historyValues: List<String>,
        value: String?
    ): String? {
        val normalizedValue = normalizeBatteryTestHistoryValue(value.orEmpty())
        if (normalizedValue.isEmpty()) {
            return null
        }

        return historyValues.firstOrNull { historyValue ->
            normalizeBatteryTestHistoryValue(historyValue) == normalizedValue
        }
    }

    private fun resolveGameName(): String? {
        val typedValue = binding.gameInput.text?.toString()?.trim()
        if (!typedValue.isNullOrEmpty()) {
            return findHistoryMatch(gameHistory, typedValue) ?: typedValue
        }

        return selectedGameBubble
            ?: selectedApp?.detectedGameName
            ?: selectedApp?.appName
    }

    private fun resolveSystemName(): String? {
        val typedValue = binding.systemInput.text?.toString()?.trim()
        if (!typedValue.isNullOrEmpty()) {
            return findHistoryMatch(systemHistory, typedValue) ?: typedValue
        }

        return selectedSystemBubble
            ?: selectedApp?.let { app ->
                AppDetectionService.getSuggestedSession(context, app)?.systemName
                    ?: AppDetectionService.getSystemName(app.packageName, app.appName)
                    ?: "Android"
            }
    }
}
