package com.chimeragaming.gamepulse.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * Utility for detecting running apps and extracting game information
 */
object AppDetectionService {
    
    /**
     * Get list of currently running apps
     */
    fun getRunningApps(context: Context): List<RunningAppInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val packageManager = context.packageManager
        val runningApps = mutableListOf<RunningAppInfo>()
        
        try {
            // Get apps used in the last 10 seconds
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 10000 // 10 seconds ago
            
            val usageStatsList = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ) ?: emptyList()
            
            // Get foreground app
            val recentApps = usageStatsList.sortedByDescending { it.lastTimeUsed }
                .take(10)
            
            for (usageStats in recentApps) {
                if (usageStats.lastTimeUsed > startTime) {
                    try {
                        val appInfo = packageManager.getApplicationInfo(usageStats.packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        
                        // Detect if it's a game or emulator
                        val isGame = (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                        val isEmulator = isKnownEmulator(usageStats.packageName)
                        
                        // Try to extract game name from emulator
                        val detectedGame = if (isEmulator) {
                            detectGameFromEmulator(context, usageStats.packageName)
                        } else null
                        
                        runningApps.add(
                            RunningAppInfo(
                                packageName = usageStats.packageName,
                                appName = appName,
                                isGame = isGame || isEmulator,
                                isEmulator = isEmulator,
                                detectedGameName = detectedGame,
                                lastUsedTime = usageStats.lastTimeUsed
                            )
                        )
                    } catch (e: PackageManager.NameNotFoundException) {
                        // App not found, skip
                    }
                }
            }
        } catch (e: Exception) {
            // Permission not granted or error occurred
        }
        
        // Filter to show only games/emulators and sort by recent usage
        return runningApps
            .filter { it.isGame }
            .sortedByDescending { it.lastUsedTime }
            .distinctBy { it.packageName }
    }
    
    /**
     * Check if package is a known emulator
     */
    private fun isKnownEmulator(packageName: String): Boolean {
        val emulators = listOf(
            "com.explusalpha.emulationsstation", // EmulationStation
            "org.dolphinemu.dolphinemu", // Dolphin
            "com.retroarch", // RetroArch
            "com.retroarch.aarch64", // RetroArch 64-bit
            "org.ppsspp.ppsspp", // PPSSPP
            "org.citra.citra_emu", // Citra
            "com.iyusuf.mupen64plusae", // Mupen64Plus
            "com.snes9x.emulator", // Snes9x
            "com.retrodev.emulationsstation", // RetroArch
            "com.chimeragaming.eden", // Eden (example)
            "xyz.aethersx2.android" // AetherSX2
        )
        return emulators.any { packageName.contains(it, ignoreCase = true) }
    }
    
    /**
     * Attempt to detect game name from emulator (placeholder implementation)
     * In a real implementation, this would query the emulator's content provider or shared preferences
     */
    private fun detectGameFromEmulator(context: Context, packageName: String): String? {
        // This is a placeholder - real implementation would need:
        // 1. Access to emulator's content provider if available
        // 2. Read from shared preferences if accessible
        // 3. Parse recent files or ROM directories
        // 4. Use Android's app usage stats to find related data
        
        // For now, return null and rely on manual input
        return null
    }
    
    /**
     * Get system/emulator name from package
     */
    fun getSystemName(packageName: String): String? {
        return when {
            packageName.contains("dolphinemu", ignoreCase = true) -> "GameCube/Wii (Dolphin)"
            packageName.contains("retroarch", ignoreCase = true) -> "RetroArch"
            packageName.contains("ppsspp", ignoreCase = true) -> "PSP (PPSSPP)"
            packageName.contains("citra", ignoreCase = true) -> "3DS (Citra)"
            packageName.contains("mupen64", ignoreCase = true) -> "N64 (Mupen64Plus)"
            packageName.contains("snes9x", ignoreCase = true) -> "SNES (Snes9x)"
            packageName.contains("eden", ignoreCase = true) -> "Eden"
            packageName.contains("aethersx2", ignoreCase = true) -> "PS2 (AetherSX2)"
            else -> null
        }
    }
}

/**
 * Data class for running app information
 */
data class RunningAppInfo(
    val packageName: String,
    val appName: String,
    val isGame: Boolean,
    val isEmulator: Boolean,
    val detectedGameName: String?,
    val lastUsedTime: Long
)
