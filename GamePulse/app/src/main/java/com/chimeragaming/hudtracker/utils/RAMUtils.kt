package com.chimeragaming.gamepulse.utils

import android.app.ActivityManager
import android.content.Context
import com.chimeragaming.gamepulse.model.RAMInfo
import java.io.RandomAccessFile

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                          RAM UTILS                                    ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║           v0.3.2 - Fixed RAM Calculation (Excluding Cache)            ║
 * ╚═══════════════════════════════���═══════════════════════════════════════╝
 */
object RAMUtils {

    /**
     * Get current RAM usage information
     * v0.3.2: Now calculates actual used RAM (excluding cached/buffers)
     */
    fun getRAMInfo(context: Context): RAMInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        // Get actual total physical RAM from /proc/meminfo
        val totalMemoryMB = getTotalPhysicalRAM() / (1024 * 1024)

        // Get accurate used RAM (excluding cache/buffers)
        val usedMemoryMB = getActualUsedRAM()

        // Calculate available
        val availableMemoryMB = totalMemoryMB - usedMemoryMB

        return RAMInfo(
            totalMemoryMB = totalMemoryMB,
            availableMemoryMB = availableMemoryMB,
            usedMemoryMB = usedMemoryMB
        )
    }

    /**
     * Get total physical RAM from /proc/meminfo
     * This reads the actual hardware RAM
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
                0L
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    /**
     * v0.3.2 NEW: Get actual used RAM (excluding cached/buffers)
     * This matches what Android's built-in RAM monitor shows
     *
     * Formula: Used = MemTotal - MemFree - Buffers - Cached - SReclaimable
     */
    private fun getActualUsedRAM(): Long {
        return try {
            val reader = RandomAccessFile("/proc/meminfo", "r")

            var memTotal = 0L
            var memFree = 0L
            var buffers = 0L
            var cached = 0L
            var sReclaimable = 0L

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { currentLine ->
                    when {
                        currentLine.startsWith("MemTotal:") -> {
                            memTotal = extractValue(currentLine)
                        }
                        currentLine.startsWith("MemFree:") -> {
                            memFree = extractValue(currentLine)
                        }
                        currentLine.startsWith("Buffers:") -> {
                            buffers = extractValue(currentLine)
                        }
                        currentLine.startsWith("Cached:") -> {
                            cached = extractValue(currentLine)
                        }
                        currentLine.startsWith("SReclaimable:") -> {
                            sReclaimable = extractValue(currentLine)
                        }
                    }
                }
            }
            reader.close()

            // Calculate actual used RAM (excluding cache and buffers)
            val usedKB = memTotal - memFree - buffers - cached - sReclaimable

            // Convert KB to MB
            usedKB / 1024

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to simple calculation
            getFallbackUsedRAM()
        }
    }

    /**
     * Extract numeric value from /proc/meminfo line
     * Example: "MemTotal:       8123456 kB" -> 8123456
     */
    private fun extractValue(line: String): Long {
        return try {
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 2) {
                parts[1].toLongOrNull() ?: 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Fallback method if /proc/meminfo parsing fails
     */
    private fun getFallbackUsedRAM(): Long {
        return try {
            val reader = RandomAccessFile("/proc/meminfo", "r")

            // Read first 3 lines for basic calculation
            val memTotalLine = reader.readLine()
            val memFreeLine = reader.readLine()
            val memAvailableLine = reader.readLine()

            reader.close()

            val memTotal = extractValue(memTotalLine)
            val memAvailable = extractValue(memAvailableLine)

            // Used = Total - Available
            (memTotal - memAvailable) / 1024 // Convert to MB

        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
}