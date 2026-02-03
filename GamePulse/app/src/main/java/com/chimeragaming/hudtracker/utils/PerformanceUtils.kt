package com.chimeragaming.gamepulse.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.chimeragaming.gamepulse.model.AppEnergyUsage

/**
 * Utility class for app usage and performance analysis
 */
object PerformanceUtils {
    
    // Energy estimation constants
    private const val CPU_USAGE_PER_MINUTE = 0.5f // Estimated CPU usage percent per minute of foreground time
    private const val CPU_USAGE_MAX = 100f // Maximum CPU usage percentage
    private const val BATTERY_DRAIN_PER_MINUTE = 0.1f // Estimated battery drain percent per minute
    private const val BATTERY_DRAIN_MAX = 20f // Maximum battery drain percentage cap
    
    /**
     * Get app usage statistics for performance analysis
     */
    fun getAppUsageStats(context: Context, startTime: Long, endTime: Long): List<AppEnergyUsage> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        val packageManager = context.packageManager
        val appUsageList = mutableListOf<AppEnergyUsage>()
        
        for (stats in usageStats) {
            if (stats.totalTimeInForeground > 0) {
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(stats.packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    stats.packageName
                }
                
                // Estimate energy usage based on foreground time
                val foregroundMinutes = stats.totalTimeInForeground / (1000 * 60)
                val estimatedCPU = (foregroundMinutes * CPU_USAGE_PER_MINUTE).coerceAtMost(CPU_USAGE_MAX)
                val estimatedBatteryDrain = (foregroundMinutes * BATTERY_DRAIN_PER_MINUTE).coerceAtMost(BATTERY_DRAIN_MAX)
                
                appUsageList.add(
                    AppEnergyUsage(
                        appName = appName,
                        packageName = stats.packageName,
                        cpuUsagePercent = estimatedCPU,
                        ramUsageMB = 0, // Would need additional API access
                        networkUsageKB = 0, // Would need additional API access
                        screenOnTimeMinutes = foregroundMinutes,
                        batteryDrainPercent = estimatedBatteryDrain
                    )
                )
            }
        }
        
        return appUsageList.sortedByDescending { it.getTotalEnergyScore() }
    }
    
    /**
     * Calculate estimated CPU usage for an app
     */
    fun estimateCPUUsage(foregroundTimeMs: Long, totalTimeMs: Long): Float {
        if (totalTimeMs == 0L) return 0f
        val ratio = foregroundTimeMs.toFloat() / totalTimeMs.toFloat()
        return (ratio * 100).coerceIn(0f, 100f)
    }
}
