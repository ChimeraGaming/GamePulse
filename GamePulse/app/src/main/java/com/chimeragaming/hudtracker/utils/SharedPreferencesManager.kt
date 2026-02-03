package com.chimeragaming.gamepulse.utils

import android.content.Context
import android.content.SharedPreferences
import com.chimeragaming.gamepulse.model.ThemePreset
import com.google.gson.Gson

/**
 * Manager for storing and retrieving user preferences
 */
class SharedPreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
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
        
        // Default values
        const val DEFAULT_REFRESH_RATE = 10 // 10 seconds
        const val DEFAULT_RAM_THEME = "power_cores"
        const val DEFAULT_BATTERY_THEME = "stats_panel"
        const val DEFAULT_OVERLAY_POSITION = "top_right"
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
}
