package com.chimeragaming.gamepulse.model

import java.io.Serializable

/**
 * Data class for battery analysis results with game information
 */
data class BatteryAnalysisResult(
    val startTime: Long,
    val endTime: Long,
    val startBatteryLevel: Int,
    val endBatteryLevel: Int,
    val startVoltage: Float,
    val endVoltage: Float,
    val batteryDrainPercent: Float,
    val voltageDrop: Float,
    val estimatedLifeHours: Float,
    val averageRamUsageGB: Float,
    val averageTemperatureC: Float = 0f,
    val maxTemperatureC: Float = 0f,
    val topApps: List<AppEnergyUsage>,
    val isComplete: Boolean = false,
    val gameName: String? = null,
    val systemName: String? = null,
    val packageName: String? = null
) : Serializable {
    fun getDurationMinutes(): Long {
        return (endTime - startTime) / (1000 * 60)
    }
    
    fun getAverageDrainPerHour(): Float {
        val hours = getDurationMinutes() / 60.0f
        return if (hours > 0) batteryDrainPercent / hours else 0f
    }
    
    fun getDurationLabel(): String {
        val minutes = getDurationMinutes()
        return when {
            minutes < 60 -> "$minutes Minutes"
            minutes == 60L -> "1 Hour"
            else -> "${minutes / 60} Hours"
        }
    }

    fun getAverageTemperatureFormatted(): String {
        return if (averageTemperatureC > 0f) {
            String.format("%.1f C", averageTemperatureC)
        } else {
            "N/A"
        }
    }

    fun getMaxTemperatureFormatted(): String {
        return if (maxTemperatureC > 0f) {
            String.format("%.1f C", maxTemperatureC)
        } else {
            "N/A"
        }
    }
}
