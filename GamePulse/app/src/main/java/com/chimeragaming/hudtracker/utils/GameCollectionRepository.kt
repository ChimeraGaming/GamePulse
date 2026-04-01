package com.chimeragaming.gamepulse.utils

import android.content.Context
import android.content.SharedPreferences
import com.chimeragaming.gamepulse.model.BatteryAnalysisResult
import com.chimeragaming.gamepulse.model.GameInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class GameCollectionRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    fun recordBatteryAnalysis(result: BatteryAnalysisResult) {
        val gameName = result.gameName?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val systemName = result.systemName?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_SYSTEM_NAME
        val packageName = resolvePackageName(result)
        val durationMinutes = result.getDurationMinutes().coerceAtLeast(1L)
        val drainPerHour = result.getAverageDrainPerHour()
        val averageTemperatureC = result.averageTemperatureC

        val records = loadRecords().toMutableList()
        val existingIndex = records.indexOfFirst { record ->
            record.name.equals(gameName, ignoreCase = true) &&
                record.system.equals(systemName, ignoreCase = true)
        }

        if (existingIndex >= 0) {
            val existing = records[existingIndex]
            records[existingIndex] = existing.copy(
                packageName = existing.packageName ?: packageName,
                iconUrl = existing.iconUrl,
                trackedSinceTimestamp = minOf(existing.trackedSinceTimestamp, result.startTime),
                lastBatteryTestTimestamp = maxOf(existing.lastBatteryTestTimestamp, result.endTime),
                totalBatteryDrainPerHour = existing.totalBatteryDrainPerHour + drainPerHour,
                totalRamUsageGB = existing.totalRamUsageGB + result.averageRamUsageGB,
                totalTemperatureC = existing.totalTemperatureC + averageTemperatureC,
                lastTestDurationMinutes = durationMinutes,
                totalBatteryTestMinutes = existing.totalBatteryTestMinutes + durationMinutes,
                batteryTestCount = existing.batteryTestCount + 1
            )
        } else {
            records.add(
                TrackedGameRecord(
                    id = UUID.randomUUID().toString(),
                    name = gameName,
                    system = systemName,
                    packageName = packageName,
                    iconUrl = null,
                    trackedSinceTimestamp = result.startTime,
                    lastBatteryTestTimestamp = result.endTime,
                    totalBatteryDrainPerHour = drainPerHour,
                    totalRamUsageGB = result.averageRamUsageGB,
                    totalTemperatureC = averageTemperatureC,
                    lastTestDurationMinutes = durationMinutes,
                    totalBatteryTestMinutes = durationMinutes,
                    batteryTestCount = 1
                )
            )
        }

        saveRecords(records)
    }

    fun getTrackedGames(): List<GameInfo> {
        val records = loadRecords()
        if (records.isEmpty()) {
            return emptyList()
        }

        val now = System.currentTimeMillis()
        val packageCounts = records.mapNotNull { it.packageName }
            .groupingBy { it }
            .eachCount()

        return records.map { record ->
            val packageName = record.packageName
            val canUsePackageStats = !packageName.isNullOrBlank() && packageCounts[packageName] == 1
            val packagePlaytimeMinutes = if (canUsePackageStats) {
                PerformanceUtils.getPackagePlaytimeMinutes(
                    appContext,
                    packageName!!,
                    record.trackedSinceTimestamp,
                    now
                )
            } else {
                0L
            }
            val packageLastUsedTimestamp = if (canUsePackageStats) {
                PerformanceUtils.getPackageLastUsedTimestamp(
                    appContext,
                    packageName!!,
                    record.trackedSinceTimestamp,
                    now
                )
            } else {
                0L
            }

            GameInfo(
                id = record.id,
                name = record.name,
                system = record.system,
                packageName = packageName,
                batteryDrainPerHour = record.averageBatteryDrainPerHour(),
                ramUsageGB = record.averageRamUsageGB(),
                averageTemperatureC = record.averageTemperatureC(),
                lastTestDurationMinutes = record.lastTestDurationMinutes,
                averageSessionMinutes = record.averageSessionMinutes(),
                totalPlaytimeMinutes = maxOf(record.totalBatteryTestMinutes, packagePlaytimeMinutes),
                lastPlayedTimestamp = maxOf(record.lastBatteryTestTimestamp, packageLastUsedTimestamp),
                iconUrl = record.iconUrl
            )
        }
    }

    fun getTrackedGameHistory(): List<TrackedGameHistoryItem> {
        return loadRecords()
            .sortedByDescending { it.lastBatteryTestTimestamp }
            .map { record ->
                TrackedGameHistoryItem(
                    name = record.name,
                    system = record.system,
                    packageName = record.packageName,
                    lastPlayedTimestamp = record.lastBatteryTestTimestamp
                )
            }
    }

    fun findLatestTrackedGameForPackage(packageName: String): TrackedGameHistoryItem? {
        val normalizedPackage = packageName.trim()
        if (normalizedPackage.isEmpty()) {
            return null
        }

        return loadRecords()
            .filter { record ->
                !record.packageName.isNullOrBlank() &&
                    record.packageName.equals(normalizedPackage, ignoreCase = true)
            }
            .maxByOrNull { it.lastBatteryTestTimestamp }
            ?.let { record ->
                TrackedGameHistoryItem(
                    name = record.name,
                    system = record.system,
                    packageName = record.packageName,
                    lastPlayedTimestamp = record.lastBatteryTestTimestamp
                )
            }
    }

    fun deleteTrackedGame(gameId: String): Boolean {
        val normalizedId = gameId.trim()
        if (normalizedId.isEmpty()) {
            return false
        }

        val records = loadRecords().toMutableList()
        val removed = records.removeAll { record ->
            record.id == normalizedId
        }

        if (removed) {
            saveRecords(records)
        }

        return removed
    }

    fun updateTrackedGameImage(gameId: String, iconUrl: String?): Boolean {
        val normalizedId = gameId.trim()
        if (normalizedId.isEmpty()) {
            return false
        }

        val records = loadRecords().toMutableList()
        val recordIndex = records.indexOfFirst { it.id == normalizedId }
        if (recordIndex < 0) {
            return false
        }

        records[recordIndex] = records[recordIndex].copy(
            iconUrl = iconUrl?.trim()?.takeIf { it.isNotEmpty() }
        )
        saveRecords(records)
        return true
    }

    private fun resolvePackageName(result: BatteryAnalysisResult): String? {
        val explicitPackage = result.packageName?.trim()?.takeIf { it.isNotEmpty() }
        if (explicitPackage != null) {
            return explicitPackage
        }

        return result.topApps.firstOrNull { it.packageName != appContext.packageName }?.packageName
    }

    private fun loadRecords(): List<TrackedGameRecord> {
        val json = prefs.getString(KEY_TRACKED_GAMES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TrackedGameRecord>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveRecords(records: List<TrackedGameRecord>) {
        val json = gson.toJson(records)
        prefs.edit().putString(KEY_TRACKED_GAMES, json).apply()
    }

    companion object {
        private const val PREFS_NAME = "gamepulse_collection"
        private const val KEY_TRACKED_GAMES = "tracked_games"
        private const val DEFAULT_SYSTEM_NAME = "Android"
    }
}

private data class TrackedGameRecord(
    val id: String,
    val name: String,
    val system: String,
    val packageName: String?,
    val iconUrl: String? = null,
    val trackedSinceTimestamp: Long,
    val lastBatteryTestTimestamp: Long,
    val totalBatteryDrainPerHour: Float,
    val totalRamUsageGB: Float,
    val totalTemperatureC: Float = 0f,
    val lastTestDurationMinutes: Long = 0L,
    val totalBatteryTestMinutes: Long,
    val batteryTestCount: Int
) {
    fun averageBatteryDrainPerHour(): Float {
        return if (batteryTestCount > 0) {
            totalBatteryDrainPerHour / batteryTestCount.toFloat()
        } else {
            0f
        }
    }

    fun averageRamUsageGB(): Float {
        return if (batteryTestCount > 0) {
            totalRamUsageGB / batteryTestCount.toFloat()
        } else {
            0f
        }
    }

    fun averageTemperatureC(): Float {
        return if (batteryTestCount > 0) {
            totalTemperatureC / batteryTestCount.toFloat()
        } else {
            0f
        }
    }

    fun averageSessionMinutes(): Int {
        return if (batteryTestCount > 0) {
            (totalBatteryTestMinutes / batteryTestCount).toInt()
        } else {
            0
        }
    }
}

data class TrackedGameHistoryItem(
    val name: String,
    val system: String,
    val packageName: String?,
    val lastPlayedTimestamp: Long
)
