package com.chimeragaming.gamepulse.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Manager for app-wide theme selection
 * v0.3: Added theme management with multiple options
 */
object ThemeManager {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    // Theme constants
    const val THEME_DARK = "dark"
    const val THEME_AMOLED = "amoled"
    const val THEME_LIGHT = "light"
    const val THEME_GAMING_NEON = "gaming_neon"
    const val THEME_CYBERPUNK = "cyberpunk"
    const val THEME_RETRO = "retro"

    /**
     * Get all available theme options
     */
    fun getAvailableThemes(): Array<String> {
        return arrayOf(
            THEME_DARK,
            THEME_AMOLED,
            THEME_LIGHT,
            THEME_GAMING_NEON,
            THEME_CYBERPUNK,
            THEME_RETRO
        )
    }

    /**
     * Get theme display names
     */
    fun getThemeLabels(): Array<String> {
        return arrayOf(
            "Dark (Default)",
            "AMOLED Black",
            "Light",
            "Gaming Neon",
            "Cyberpunk",
            "Retro Console"
        )
    }

    /**
     * Get currently selected theme
     */
    fun getCurrentTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, THEME_DARK) ?: THEME_DARK
    }

    /**
     * Set and apply theme
     */
    fun setTheme(context: Context, theme: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme).apply()

        // Apply theme based on selection
        when (theme) {
            THEME_LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            THEME_DARK, THEME_AMOLED, THEME_GAMING_NEON, THEME_CYBERPUNK, THEME_RETRO -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    /**
     * Apply theme on app start
     */
    fun applyTheme(context: Context) {
        val theme = getCurrentTheme(context)
        setTheme(context, theme)
    }

    /**
     * Get theme display name
     */
    fun getThemeDisplayName(theme: String): String {
        return when (theme) {
            THEME_DARK -> "Dark (Default)"
            THEME_AMOLED -> "AMOLED Black"
            THEME_LIGHT -> "Light"
            THEME_GAMING_NEON -> "Gaming Neon"
            THEME_CYBERPUNK -> "Cyberpunk"
            THEME_RETRO -> "Retro Console"
            else -> "Dark (Default)"
        }
    }
}