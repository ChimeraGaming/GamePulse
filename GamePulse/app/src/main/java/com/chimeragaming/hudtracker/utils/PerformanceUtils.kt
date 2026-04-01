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
    private const val CPU_USAGE_MAX = 100f
    
    /**
     * Get app usage statistics for performance analysis
     */
    fun getAppUsageStats(
        context: Context,
        startTime: Long,
        endTime: Long,
        totalBatteryDrainPercent: Float
    ): List<AppEnergyUsage> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList()

        val usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        val usageStats = usageStatsMap.values.filter { stats ->
            stats.totalTimeInForeground > 0L
        }
        if (usageStats.isEmpty()) {
            return emptyList()
        }

        val totalForegroundTimeMs = usageStats
            .map { stats -> stats.totalTimeInForeground }
            .sum()
            .coerceAtLeast(1L)

        val packageManager = context.packageManager

        return usageStats.map { stats ->
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(stats.packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                stats.packageName
            }

            val foregroundTimeMs = stats.totalTimeInForeground
            val foregroundShare = foregroundTimeMs.toFloat() / totalForegroundTimeMs.toFloat()
            val estimatedCpu = (foregroundShare * 100f).coerceAtMost(CPU_USAGE_MAX)
            val estimatedBatteryDrain =
                (totalBatteryDrainPercent.coerceAtLeast(0f) * foregroundShare).coerceAtLeast(0f)

            AppEnergyUsage(
                appName = appName,
                packageName = stats.packageName,
                cpuUsagePercent = estimatedCpu,
                ramUsageMB = 0L,
                networkUsageKB = 0L,
                screenOnTimeMinutes = foregroundTimeMs / 60000L,
                batteryDrainPercent = estimatedBatteryDrain
            )
        }.sortedByDescending { usage -> usage.getTotalEnergyScore() }
    }

    fun getPackagePlaytimeMinutes(
        context: Context,
        packageName: String,
        startTime: Long,
        endTime: Long
    ): Long {
        val stats = getPackageUsageStats(context, packageName, startTime, endTime) ?: return 0L
        return stats.totalTimeInForeground / (1000L * 60L)
    }

    fun getPackageLastUsedTimestamp(
        context: Context,
        packageName: String,
        startTime: Long,
        endTime: Long
    ): Long {
        val stats = getPackageUsageStats(context, packageName, startTime, endTime) ?: return 0L
        return stats.lastTimeUsed
    }
    
    /**
     * Calculate estimated CPU usage for an app
     */
    fun estimateCPUUsage(foregroundTimeMs: Long, totalTimeMs: Long): Float {
        if (totalTimeMs == 0L) return 0f
        val ratio = foregroundTimeMs.toFloat() / totalTimeMs.toFloat()
        return (ratio * 100).coerceIn(0f, 100f)
    }

    private fun getPackageUsageStats(
        context: Context,
        packageName: String,
        startTime: Long,
        endTime: Long
    ) = try {
        if (packageName.isBlank() || endTime <= startTime) {
            null
        } else {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager == null) {
                null
            } else {
                usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)[packageName]
            }
        }
    } catch (_: Exception) {
        null
    }
}
