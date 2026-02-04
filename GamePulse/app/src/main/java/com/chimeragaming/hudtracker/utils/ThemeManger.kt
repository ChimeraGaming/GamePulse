package com.chimeragaming.gamepulse.utils

import android.app.Activity
import android.content.Context
import com.chimeragaming.gamepulse.R

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                        THEME MANAGER                                  ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║                  v0.3.2 - Added SNES Rainbow Theme                    ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
object ThemeManager {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    // Theme constants
    const val THEME_DARK = "dark"
    const val THEME_AMOLED = "amoled"
    const val THEME_LIGHT = "light"
    const val THEME_REACTOR_NEON = "reactor_neon"
    const val THEME_CYBERPUNK = "cyberpunk"
    const val THEME_NEON_EMBER = "neon_ember"
    const val THEME_SNES = "snes"
    const val THEME_SNES_RAINBOW = "snes_rainbow"

    /**
     * Get all available theme options
     */
    fun getAvailableThemes(): Array<String> {
        return arrayOf(
            THEME_DARK,
            THEME_AMOLED,
            THEME_LIGHT,
            THEME_REACTOR_NEON,
            THEME_CYBERPUNK,
            THEME_NEON_EMBER,
            THEME_SNES,
            THEME_SNES_RAINBOW
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
            "Reactor Neon",
            "Cyberpunk",
            "Neon Ember",
            "SNES",
            "SNES Rainbow"
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
     * Set theme (save to preferences)
     */
    fun setTheme(context: Context, theme: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    /**
     * Apply theme to activity (must be called BEFORE setContentView)
     */
    fun applyTheme(activity: Activity) {
        val theme = getCurrentTheme(activity)

        val themeResId = when (theme) {
            THEME_AMOLED -> R.style.Theme_HUDTracker_Amoled
            THEME_LIGHT -> R.style.Theme_HUDTracker_Light
            THEME_REACTOR_NEON -> R.style.Theme_HUDTracker_ReactorNeon
            THEME_CYBERPUNK -> R.style.Theme_HUDTracker_Cyberpunk
            THEME_NEON_EMBER -> R.style.Theme_HUDTracker_NeonEmber
            THEME_SNES -> R.style.Theme_HUDTracker_SNES
            THEME_SNES_RAINBOW -> R.style.Theme_HUDTracker_SNESRainbow
            else -> R.style.Theme_HUDTracker // Dark default
        }

        activity.setTheme(themeResId)
    }

    /**
     * Get theme display name
     */
    fun getThemeDisplayName(theme: String): String {
        return when (theme) {
            THEME_DARK -> "Dark (Default)"
            THEME_AMOLED -> "AMOLED Black"
            THEME_LIGHT -> "Light"
            THEME_REACTOR_NEON -> "Reactor Neon"
            THEME_CYBERPUNK -> "Cyberpunk"
            THEME_NEON_EMBER -> "Neon Ember"
            THEME_SNES -> "SNES"
            THEME_SNES_RAINBOW -> "SNES Rainbow"
            else -> "Dark (Default)"
        }
    }
}