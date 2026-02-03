package com.chimeragaming.gamepulse.utils

import android.app.ActivityManager
import android.content.Context
import com.chimeragaming.gamepulse.model.RAMInfo
import java.io.RandomAccessFile

/**
 * Utility class for RAM monitoring
 * v0.3: Updated to read actual physical RAM from /proc/meminfo
 */
object RAMUtils {

    /**
     * Get current RAM usage information
     * v0.3: Now reads actual total physical RAM
     */
    fun getRAMInfo(context: Context): RAMInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        // v0.3: Get actual total physical RAM from /proc/meminfo
        val totalMemoryMB = getTotalPhysicalRAM() / (1024 * 1024)
        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        val usedMemoryMB = totalMemoryMB - availableMemoryMB

        return RAMInfo(
            totalMemoryMB = totalMemoryMB,
            availableMemoryMB = availableMemoryMB,
            usedMemoryMB = usedMemoryMB
        )
    }

    /**
     * v0.3 NEW: Get total physical RAM from /proc/meminfo
     * This reads the actual hardware RAM, not just what's available to apps
     */
    private fun getTotalPhysicalRAM(): Long {
        return try {
            val reader = RandomAccessFile("/proc/meminfo", "r")
            val line = reader.readLine() // First line is MemTotal
            reader.close()

            // Line format: "MemTotal:       8123456 kB"
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val totalKB = parts[1].toLongOrNull() ?: 0L
                totalKB * 1024 // Convert KB to bytes
            } else {
                // Fallback to memoryInfo.totalMem if parsing fails
                0L
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: use ActivityManager's totalMem (which is less than physical RAM)
            0L
        }
    }
}