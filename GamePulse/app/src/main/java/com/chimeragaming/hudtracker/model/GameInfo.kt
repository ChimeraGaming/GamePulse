package com.chimeragaming.gamepulse.model

import java.io.Serializable

/**
 * Data class representing a game in the collection
 * v0.3: Game collection feature
 */
data class GameInfo(
    val id: String,
    val name: String,
    val system: String,
    val packageName: String? = null,
    val batteryDrainPerHour: Float, // Percentage per hour
    val ramUsageGB: Float,
    val averageSessionMinutes: Int,
    val totalPlaytimeMinutes: Long,
    val lastPlayedTimestamp: Long,
    val iconUrl: String? = null
) : Serializable {

    /**
     * Get formatted battery drain
     */
    fun getBatteryDrainFormatted(): String {
        return String.format("%.1f%%/hr", batteryDrainPerHour)
    }

    /**
     * Get formatted RAM usage
     */
    fun getRamUsageFormatted(): String {
        return String.format("%.2f GB", ramUsageGB)
    }

    /**
     * Get formatted playtime
     */
    fun getPlaytimeFormatted(): String {
        val hours = totalPlaytimeMinutes / 60
        val minutes = totalPlaytimeMinutes % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    /**
     * Get medal for battery efficiency (lower is better)
     */
    fun getBatteryMedal(rank: Int): String {
        return when (rank) {
            1 -> "🥇"
            2 -> "🥈"
            3 -> "🥉"
            else -> ""
        }
    }

    /**
     * Get medal for RAM efficiency (lower is better)
     */
    fun getRamMedal(rank: Int): String {
        return when (rank) {
            1 -> "🥇"
            2 -> "🥈"
            3 -> "🥉"
            else -> ""
        }
    }
}