package com.chimeragaming.gamepulse.model

/**
 * Data class representing RAM usage information
 * v0.3: Updated formatting to handle proper GB display
 */
data class RAMInfo(
    val totalMemoryMB: Long,
    val availableMemoryMB: Long,
    val usedMemoryMB: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    val usagePercentage: Float
        get() = if (totalMemoryMB > 0) {
            (usedMemoryMB.toFloat() / totalMemoryMB.toFloat()) * 100f
        } else {
            0f
        }

    /**
     * v0.3: Updated to show proper GB rounding (e.g., 8GB, 12GB, 16GB)
     */
    fun getTotalMemoryFormatted(): String {
        val gb = totalMemoryMB / 1024.0
        return when {
            gb >= 15.5 -> "16"  // 16GB devices
            gb >= 11.5 -> "12"  // 12GB devices
            gb >= 7.5 -> "8"    // 8GB devices
            gb >= 5.5 -> "6"    // 6GB devices
            gb >= 3.5 -> "4"    // 4GB devices
            gb >= 1.0 -> String.format("%.0f", gb)
            else -> String.format("%.2f GB", gb)
        }
    }

    fun getUsedMemoryFormatted(): String {
        return if (usedMemoryMB >= 1024) {
            String.format("%.2f GB", usedMemoryMB / 1024.0)
        } else {
            "$usedMemoryMB MB"
        }
    }

    // v0.3: Helper properties for displaying as GB
    val totalMemoryGB: Double
        get() = totalMemoryMB / 1024.0

    val usedMemoryGB: Double
        get() = usedMemoryMB / 1024.0

    val availableMemoryGB: Double
        get() = availableMemoryMB / 1024.0
}