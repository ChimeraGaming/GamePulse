package com.chimeragaming.gamepulse.model

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                        RAM INFO DATA MODEL                            ║
 * ║                   GamePulse Performance Tracker                       ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *
 *  Data class representing RAM usage information for Android devices.
 *  Handles memory detection and formatting for display in HUD overlays.
 *
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

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║  FORMAT TOTAL MEMORY (2GB INCREMENTS: 2→4→6→8...→32GB)               ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    fun getTotalMemoryFormatted(): String {
        val gb = totalMemoryMB / 1024.0
        return when {
            gb > 32 -> String.format("%.0f", gb)
            gb > 30 -> "32"
            gb > 28 -> "30"
            gb > 26 -> "28"
            gb > 24 -> "26"
            gb > 22 -> "24"
            gb > 20 -> "22"
            gb > 18 -> "20"
            gb > 16 -> "18"
            gb > 14 -> "16"
            gb > 12 -> "14"
            gb > 10 -> "12"
            gb > 8 -> "10"
            gb > 6 -> "8"
            gb > 4 -> "6"
            gb > 2 -> "4"
            else -> "2"
        }
    }

    fun getUsedMemoryFormatted(): String {
        return if (usedMemoryMB >= 1024) {
            String.format("%.2f GB", usedMemoryMB / 1024.0)
        } else {
            "$usedMemoryMB MB"
        }
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                     HELPER PROPERTIES - GB CONVERSIONS                ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */

    val totalMemoryGB: Double
        get() = totalMemoryMB / 1024.0

    val usedMemoryGB: Double
        get() = usedMemoryMB / 1024.0

    val availableMemoryGB: Double
        get() = availableMemoryMB / 1024.0
}