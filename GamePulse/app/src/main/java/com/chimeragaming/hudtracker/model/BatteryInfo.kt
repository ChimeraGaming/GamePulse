package com.chimeragaming.gamepulse.model

/**
 * Data class representing battery information
 */
data class BatteryInfo(
    val voltage: Float,
    val level: Int,
    val scale: Int,
    val temperature: Float,
    val status: String,
    val health: String,
    val estimatedLifeMinutes: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val percentage: Float
        get() = (level.toFloat() / scale.toFloat()) * 100f
        
    fun getEstimatedLifeFormatted(): String {
        if (estimatedLifeMinutes <= 0) {
            return "N/A"
        }
        val hours = estimatedLifeMinutes / 60
        val minutes = estimatedLifeMinutes % 60
        return String.format("%dh %dm", hours, minutes)
    }
}
