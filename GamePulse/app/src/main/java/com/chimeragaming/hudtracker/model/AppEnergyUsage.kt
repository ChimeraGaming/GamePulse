package com.chimeragaming.gamepulse.model

import java.io.Serializable

/**
 * Data class representing app energy usage analysis
 */
data class AppEnergyUsage(
    val appName: String,
    val packageName: String,
    val cpuUsagePercent: Float,
    val ramUsageMB: Long,
    val networkUsageKB: Long,
    val screenOnTimeMinutes: Long,
    val batteryDrainPercent: Float,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {
    fun getTotalEnergyScore(): Float {
        // Calculate a weighted energy score
        return (cpuUsagePercent * 0.3f) +
               (ramUsageMB / 100f * 0.2f) +
               (networkUsageKB / 1024f * 0.1f) +
               (screenOnTimeMinutes * 0.2f) +
               (batteryDrainPercent * 0.2f)
    }
}
