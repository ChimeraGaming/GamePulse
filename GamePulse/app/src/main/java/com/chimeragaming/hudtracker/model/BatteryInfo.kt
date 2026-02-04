package com.chimeragaming.gamepulse.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                         BATTERY INFO MODEL                            ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║                   v0.3.2 - Fixed Estimation State                     ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
@Parcelize
data class BatteryInfo(
    val voltage: Float,
    val level: Int,
    val scale: Int,
    val temperature: Float,
    val status: String,
    val health: String,
    val estimatedLifeMinutes: Int = -1 // -1 = calculating, 0 = N/A (charging), >0 = actual time
) : Parcelable {

    val percentage: Float
        get() = if (scale > 0) (level.toFloat() / scale.toFloat()) * 100 else 0f

    fun getEstimatedLifeFormatted(): String {
        return when {
            estimatedLifeMinutes < 0 -> "Calculating..."
            estimatedLifeMinutes == 0 -> "N/A"
            estimatedLifeMinutes < 60 -> "$estimatedLifeMinutes min"
            else -> {
                val hours = estimatedLifeMinutes / 60
                val minutes = estimatedLifeMinutes % 60
                "${hours}h ${minutes}m"
            }
        }
    }
}