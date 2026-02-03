package com.chimeragaming.gamepulse.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Theme preset data class for color customization
 */
data class ThemePreset(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("type")
    val type: ThemeType,
    
    @SerializedName("colors")
    val colors: Map<String, String>
) {
    enum class ThemeType {
        MAIN_APP,
        OVERLAY
    }
    
    companion object {
        // Main App Presets
        val GITHUB_DARK = ThemePreset(
            name = "GitHub Dark",
            type = ThemeType.MAIN_APP,
            colors = mapOf(
                "background" to "#0d1117",
                "card" to "#161b22",
                "text_primary" to "#c9d1d9",
                "text_secondary" to "#8b949e",
                "accent" to "#58a6ff",
                "progress" to "#3fb950"
            )
        )
        
        val AMOLED_BLACK = ThemePreset(
            name = "AMOLED Black",
            type = ThemeType.MAIN_APP,
            colors = mapOf(
                "background" to "#000000",
                "card" to "#1a1a1a",
                "text_primary" to "#ffffff",
                "text_secondary" to "#a0a0a0",
                "accent" to "#00d4ff",
                "progress" to "#00ff88"
            )
        )
        
        val GAMING_GREEN = ThemePreset(
            name = "Gaming Green",
            type = ThemeType.MAIN_APP,
            colors = mapOf(
                "background" to "#0a1a0a",
                "card" to "#152815",
                "text_primary" to "#c0ffc0",
                "text_secondary" to "#80c080",
                "accent" to "#00ff00",
                "progress" to "#39ff14"
            )
        )
        
        val CYBER_PURPLE = ThemePreset(
            name = "Cyber Purple",
            type = ThemeType.MAIN_APP,
            colors = mapOf(
                "background" to "#1a0a2e",
                "card" to "#2d1b4e",
                "text_primary" to "#e0c0ff",
                "text_secondary" to "#a080c0",
                "accent" to "#a020f0",
                "progress" to "#ff00ff"
            )
        )
        
        // Overlay Presets
        val DARK_TRANSPARENT = ThemePreset(
            name = "Dark Transparent",
            type = ThemeType.OVERLAY,
            colors = mapOf(
                "background" to "#cc000000", // 80% opacity
                "text" to "#ffffff",
                "border" to "#58a6ff",
                "border_width" to "2",
                "corner_radius" to "8"
            )
        )
        
        val SOLID_BLACK = ThemePreset(
            name = "Solid Black",
            type = ThemeType.OVERLAY,
            colors = mapOf(
                "background" to "#ff000000", // 100% opacity
                "text" to "#ffffff",
                "border" to "#ffffff",
                "border_width" to "1",
                "corner_radius" to "4"
            )
        )
        
        val CLEAR_GLASS = ThemePreset(
            name = "Clear Glass",
            type = ThemeType.OVERLAY,
            colors = mapOf(
                "background" to "#33000000", // 20% opacity
                "text" to "#ffffff",
                "border" to "#ffffff",
                "border_width" to "2",
                "corner_radius" to "12"
            )
        )
        
        val GAMING_RGB = ThemePreset(
            name = "Gaming RGB",
            type = ThemeType.OVERLAY,
            colors = mapOf(
                "background" to "#cc1a1a1a", // 80% opacity
                "text" to "#00ff00",
                "border" to "#00ff00",
                "border_width" to "3",
                "corner_radius" to "0"
            )
        )
        
        fun getAllMainAppPresets(): List<ThemePreset> {
            return listOf(GITHUB_DARK, AMOLED_BLACK, GAMING_GREEN, CYBER_PURPLE)
        }
        
        fun getAllOverlayPresets(): List<ThemePreset> {
            return listOf(DARK_TRANSPARENT, SOLID_BLACK, CLEAR_GLASS, GAMING_RGB)
        }
        
        fun toJson(preset: ThemePreset): String {
            return Gson().toJson(preset)
        }
        
        fun fromJson(json: String): ThemePreset? {
            return try {
                Gson().fromJson(json, ThemePreset::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
