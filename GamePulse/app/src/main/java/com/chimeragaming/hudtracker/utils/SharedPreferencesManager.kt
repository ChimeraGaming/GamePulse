package com.chimeragaming.gamepulse.utils

import android.content.Context
import android.content.SharedPreferences
import com.chimeragaming.gamepulse.model.ThemePreset
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manager for storing and retrieving user preferences
 */
class SharedPreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "hud_tracker_prefs"
        
        // Keys - HUD Settings
        private const val KEY_REFRESH_RATE = "refresh_rate"
        private const val KEY_RAM_THEME = "ram_theme"
        private const val KEY_BATTERY_THEME = "battery_theme"
        private const val KEY_RAM_ENABLED = "ram_enabled"
        private const val KEY_BATTERY_ENABLED = "battery_enabled"
        
        // Keys - Overlay Settings
        private const val KEY_OVERLAY_POSITION = "overlay_position"
        private const val KEY_OVERLAY_REFRESH_RATE = "overlay_refresh_rate"
        private const val KEY_OVERLAY_THEME = "overlay_theme"
        
        // Keys - Telemetry Settings
        private const val KEY_TELEMETRY_ENABLED = "telemetry_enabled"
        private const val KEY_TELEMETRY_CONSENT_SHOWN = "telemetry_consent_shown"
        private const val KEY_COMMUNITY_SHARING = "community_sharing"
        
        // Keys - Theme Customization
        private const val KEY_CURRENT_MAIN_THEME = "current_main_theme"
        private const val KEY_CURRENT_OVERLAY_THEME = "current_overlay_theme"
        private const val KEY_CUSTOM_THEMES = "custom_themes"

        // Keys - Battery Test History
        private const val KEY_BATTERY_TEST_SYSTEM_HISTORY = "battery_test_system_history"
        private const val KEY_BATTERY_TEST_GAME_HISTORY = "battery_test_game_history"
        private const val KEY_BATTERY_TEST_PACKAGE_MATCHES = "battery_test_package_matches"
        private const val KEY_GAME_COLLECTION_DELETE_ENABLED = "game_collection_delete_enabled"
        private const val KEY_GAME_COLLECTION_EDIT_ENABLED = "game_collection_edit_enabled"
        
        // Default values
        const val DEFAULT_REFRESH_RATE = 10 // 10 seconds
        const val DEFAULT_RAM_THEME = "power_cores"
        const val DEFAULT_BATTERY_THEME = "stats_panel"
        const val DEFAULT_OVERLAY_POSITION = "top_right"
        private const val MAX_BATTERY_TEST_HISTORY = 12
        private const val MAX_BATTERY_TEST_PACKAGE_MATCHES = 24
    }
    
    // Refresh Rate (in seconds)
    var refreshRate: Int
        get() = prefs.getInt(KEY_REFRESH_RATE, DEFAULT_REFRESH_RATE)
        set(value) = prefs.edit().putInt(KEY_REFRESH_RATE, value).apply()
    
    // RAM Theme
    var ramTheme: String
        get() = prefs.getString(KEY_RAM_THEME, DEFAULT_RAM_THEME) ?: DEFAULT_RAM_THEME
        set(value) = prefs.edit().putString(KEY_RAM_THEME, value).apply()
    
    // Battery Theme
    var batteryTheme: String
        get() = prefs.getString(KEY_BATTERY_THEME, DEFAULT_BATTERY_THEME) ?: DEFAULT_BATTERY_THEME
        set(value) = prefs.edit().putString(KEY_BATTERY_THEME, value).apply()
    
    // RAM Enabled (for mutual exclusion)
    var ramEnabled: Boolean
        get() = prefs.getBoolean(KEY_RAM_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_RAM_ENABLED, value).apply()
    
    // Battery Enabled (for mutual exclusion)
    var batteryEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_ENABLED, value).apply()
    
    // Overlay Position
    var overlayPosition: String
        get() = prefs.getString(KEY_OVERLAY_POSITION, DEFAULT_OVERLAY_POSITION) ?: DEFAULT_OVERLAY_POSITION
        set(value) = prefs.edit().putString(KEY_OVERLAY_POSITION, value).apply()
    
    // Overlay Refresh Rate
    var overlayRefreshRate: Int
        get() = prefs.getInt(KEY_OVERLAY_REFRESH_RATE, DEFAULT_REFRESH_RATE)
        set(value) = prefs.edit().putInt(KEY_OVERLAY_REFRESH_RATE, value).apply()
    
    // Telemetry Enabled
    var telemetryEnabled: Boolean
        get() = prefs.getBoolean(KEY_TELEMETRY_ENABLED, false) // Opt-in by default
        set(value) = prefs.edit().putBoolean(KEY_TELEMETRY_ENABLED, value).apply()
    
    // Telemetry Consent Shown
    var telemetryConsentShown: Boolean
        get() = prefs.getBoolean(KEY_TELEMETRY_CONSENT_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_TELEMETRY_CONSENT_SHOWN, value).apply()
    
    // Community Sharing
    var communitySharing: Boolean
        get() = prefs.getBoolean(KEY_COMMUNITY_SHARING, false)
        set(value) = prefs.edit().putBoolean(KEY_COMMUNITY_SHARING, value).apply()

    var gameCollectionDeleteEnabled: Boolean
        get() = prefs.getBoolean(KEY_GAME_COLLECTION_DELETE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_GAME_COLLECTION_DELETE_ENABLED, value).apply()

    var gameCollectionEditEnabled: Boolean
        get() = prefs.getBoolean(KEY_GAME_COLLECTION_EDIT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_GAME_COLLECTION_EDIT_ENABLED, value).apply()
    
    // Current Main Theme
    var currentMainTheme: ThemePreset?
        get() {
            val json = prefs.getString(KEY_CURRENT_MAIN_THEME, null)
            return json?.let { ThemePreset.fromJson(it) } ?: ThemePreset.GITHUB_DARK
        }
        set(value) {
            val json = value?.let { ThemePreset.toJson(it) }
            prefs.edit().putString(KEY_CURRENT_MAIN_THEME, json).apply()
        }
    
    // Current Overlay Theme
    var currentOverlayTheme: ThemePreset?
        get() {
            val json = prefs.getString(KEY_CURRENT_OVERLAY_THEME, null)
            return json?.let { ThemePreset.fromJson(it) } ?: ThemePreset.DARK_TRANSPARENT
        }
        set(value) {
            val json = value?.let { ThemePreset.toJson(it) }
            prefs.edit().putString(KEY_CURRENT_OVERLAY_THEME, json).apply()
        }
    
    // Custom Themes (stored as JSON array)
    fun getCustomThemes(): List<ThemePreset> {
        val json = prefs.getString(KEY_CUSTOM_THEMES, null) ?: return emptyList()
        return try {
            val gson = Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<ThemePreset>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveCustomTheme(theme: ThemePreset) {
        val themes = getCustomThemes().toMutableList()
        // Remove existing theme with same name
        themes.removeAll { it.name == theme.name }
        themes.add(theme)
        
        val gson = Gson()
        val json = gson.toJson(themes)
        prefs.edit().putString(KEY_CUSTOM_THEMES, json).apply()
    }
    
    fun deleteCustomTheme(themeName: String) {
        val themes = getCustomThemes().toMutableList()
        themes.removeAll { it.name == themeName }
        
        val gson = Gson()
        val json = gson.toJson(themes)
        prefs.edit().putString(KEY_CUSTOM_THEMES, json).apply()
    }

    fun getBatteryTestSystemHistory(): List<String> {
        return loadStringHistory(KEY_BATTERY_TEST_SYSTEM_HISTORY)
    }

    fun getBatteryTestGameHistory(): List<String> {
        return loadStringHistory(KEY_BATTERY_TEST_GAME_HISTORY)
    }

    fun saveBatteryTestSystem(systemName: String): String? {
        return saveStringHistoryValue(
            key = KEY_BATTERY_TEST_SYSTEM_HISTORY,
            value = systemName,
            formatter = ::formatBatteryTestSystemLabel
        )
    }

    fun saveBatteryTestGame(gameName: String): String? {
        return saveStringHistoryValue(
            key = KEY_BATTERY_TEST_GAME_HISTORY,
            value = gameName,
            formatter = ::formatBatteryTestGameLabel
        )
    }

    fun getBatteryTestPackageMatches(): List<BatteryTestPackageMatch> {
        val json = prefs.getString(KEY_BATTERY_TEST_PACKAGE_MATCHES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<BatteryTestPackageMatch>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getBatteryTestPackageMatch(packageName: String): BatteryTestPackageMatch? {
        val normalizedPackage = packageName.trim()
        if (normalizedPackage.isEmpty()) {
            return null
        }

        return getBatteryTestPackageMatches()
            .filter { it.packageName.equals(normalizedPackage, ignoreCase = true) }
            .maxByOrNull { it.lastUsedTimestamp }
    }

    fun saveBatteryTestPackageMatch(
        packageName: String,
        systemName: String,
        gameName: String
    ): BatteryTestPackageMatch? {
        val normalizedPackage = packageName.trim()
        if (normalizedPackage.isEmpty()) {
            return null
        }

        val savedSystem = saveBatteryTestSystem(systemName) ?: return null
        val savedGame = saveBatteryTestGame(gameName) ?: return null
        val currentTime = System.currentTimeMillis()
        val matches = getBatteryTestPackageMatches().toMutableList()

        matches.removeAll { existing ->
            existing.packageName.equals(normalizedPackage, ignoreCase = true) &&
                normalizeBatteryTestHistoryValue(existing.systemName) ==
                normalizeBatteryTestHistoryValue(savedSystem) &&
                normalizeBatteryTestHistoryValue(existing.gameName) ==
                normalizeBatteryTestHistoryValue(savedGame)
        }

        val savedMatch = BatteryTestPackageMatch(
            packageName = normalizedPackage,
            systemName = savedSystem,
            gameName = savedGame,
            lastUsedTimestamp = currentTime
        )

        matches.add(0, savedMatch)
        prefs.edit()
            .putString(
                KEY_BATTERY_TEST_PACKAGE_MATCHES,
                gson.toJson(matches.take(MAX_BATTERY_TEST_PACKAGE_MATCHES))
            )
            .apply()

        return savedMatch
    }

    private fun loadStringHistory(key: String): List<String> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveStringHistoryValue(
        key: String,
        value: String,
        formatter: (String) -> String
    ): String? {
        val cleanedValue = collapseBatteryTestWhitespace(value)
        if (cleanedValue.isEmpty()) {
            return null
        }

        val formattedValue = formatter(cleanedValue)
        val normalizedValue = normalizeBatteryTestHistoryValue(formattedValue)
        val history = loadStringHistory(key).toMutableList()
        val existingIndex = history.indexOfFirst { existing ->
            normalizeBatteryTestHistoryValue(existing) == normalizedValue
        }

        if (existingIndex >= 0) {
            history.removeAt(existingIndex)
        }

        history.add(0, formattedValue)
        prefs.edit()
            .putString(key, gson.toJson(history.take(MAX_BATTERY_TEST_HISTORY)))
            .apply()

        return formattedValue
    }
}

data class BatteryTestPackageMatch(
    val packageName: String,
    val systemName: String,
    val gameName: String,
    val lastUsedTimestamp: Long
)

fun normalizeBatteryTestHistoryValue(value: String): String {
    return collapseBatteryTestWhitespace(value).lowercase()
}

private fun collapseBatteryTestWhitespace(value: String): String {
    return value.trim().replace("\\s+".toRegex(), " ")
}

private fun formatBatteryTestSystemLabel(value: String): String {
    val normalized = normalizeBatteryTestHistoryValue(value)
    val knownSystems = mapOf(
        "android" to "Android",
        "aethersx2" to "AetherSX2",
        "citra" to "Citra",
        "dolphin" to "Dolphin",
        "eden" to "Eden",
        "gamecube" to "GameCube",
        "n64" to "N64",
        "ppsspp" to "PPSSPP",
        "ps2" to "PS2",
        "psp" to "PSP",
        "retroarch" to "RetroArch",
        "snes" to "SNES",
        "snes9x" to "Snes9x",
        "wii" to "Wii",
        "yuzu" to "Yuzu"
    )

    return knownSystems[normalized] ?: formatBatteryTestWords(value, preserveShortUppercase = true)
}

private fun formatBatteryTestGameLabel(value: String): String {
    return formatBatteryTestWords(value, preserveShortUppercase = true)
}

private fun formatBatteryTestWords(
    value: String,
    preserveShortUppercase: Boolean
): String {
    return collapseBatteryTestWhitespace(value)
        .split(" ")
        .joinToString(" ") { token ->
            formatBatteryTestToken(token, preserveShortUppercase)
        }
}

private fun formatBatteryTestToken(
    token: String,
    preserveShortUppercase: Boolean
): String {
    if (token.isBlank()) {
        return token
    }

    val startIndex = token.indexOfFirst { it.isLetterOrDigit() }
    val endIndex = token.indexOfLast { it.isLetterOrDigit() }
    if (startIndex < 0 || endIndex < startIndex) {
        return token
    }

    val prefix = token.substring(0, startIndex)
    val suffix = token.substring(endIndex + 1)
    val core = token.substring(startIndex, endIndex + 1)
    val hasMixedCase = core.any { it.isLowerCase() } &&
        core.any { it.isUpperCase() } &&
        !core.drop(1).all { !it.isLetter() || it.isLowerCase() }

    val formattedCore = when {
        core.length <= 2 && preserveShortUppercase -> core.uppercase()
        core.any { it.isDigit() } && preserveShortUppercase -> core.uppercase()
        core.equals(core.uppercase()) -> core.lowercase().replaceFirstChar { it.uppercase() }
        core.equals(core.lowercase()) -> core.replaceFirstChar { it.uppercase() }
        hasMixedCase -> core.lowercase().replaceFirstChar { it.uppercase() }
        else -> core
    }

    return prefix + formattedCore + suffix
}
