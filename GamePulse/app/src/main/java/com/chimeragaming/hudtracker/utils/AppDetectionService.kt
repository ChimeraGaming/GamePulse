package com.chimeragaming.gamepulse.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object AppDetectionService {

    private const val RECENT_USAGE_WINDOW_MS = 2 * 60 * 1000L
    private const val MAX_RUNNING_APPS = 10

    private val emulatorHints = listOf(
        "aethersx2",
        "citra",
        "dolphin",
        "duckstation",
        "eden",
        "emulator",
        "mupen",
        "ppsspp",
        "retroarch",
        "snes9x",
        "vita3k",
        "yuzu"
    )

    private val ignoredPackageHints = listOf(
        "android.launcher",
        "docsui",
        "documentsui",
        "gboard",
        "inputmethod",
        "keyboard",
        "launcher",
        "packageinstaller",
        "permissioncontroller",
        "pixellauncher",
        "quicksearchbox",
        "settings",
        "systemui"
    )

    fun getRunningApps(context: Context): List<RunningAppInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()
        val packageManager = context.packageManager
        val knownPackages = loadKnownPackages(context)
        val recentPackages = collectRecentPackages(usageStatsManager)

        return recentPackages.entries
            .sortedByDescending { it.value }
            .mapNotNull { (packageName, lastUsedTime) ->
                buildRunningAppInfo(
                    context = context,
                    packageManager = packageManager,
                    packageName = packageName,
                    lastUsedTime = lastUsedTime,
                    knownPackages = knownPackages
                )
            }
            .take(MAX_RUNNING_APPS)
    }

    fun getSuggestedSession(
        context: Context,
        runningApp: RunningAppInfo
    ): DetectedSessionSuggestion? {
        return getSuggestedSession(
            context = context,
            packageName = runningApp.packageName,
            appName = runningApp.appName,
            isGame = runningApp.isGame,
            isEmulator = runningApp.isEmulator
        )
    }

    fun getSystemName(
        packageName: String,
        appName: String = ""
    ): String? {
        val packageKey = packageName.lowercase()
        val appKey = appName.lowercase()

        return when {
            packageKey.contains("aethersx2") || appKey.contains("aethersx2") -> "PS2 (AetherSX2)"
            packageKey.contains("citra") || appKey.contains("citra") -> "3DS (Citra)"
            packageKey.contains("dolphin") || appKey.contains("dolphin") -> "GameCube/Wii (Dolphin)"
            packageKey.contains("duckstation") || appKey.contains("duckstation") -> "PS1 (DuckStation)"
            packageKey.contains("eden") || appKey.contains("eden") -> "Eden"
            packageKey.contains("mupen64") || appKey.contains("mupen") -> "N64 (Mupen64Plus)"
            packageKey.contains("ppsspp") || appKey.contains("ppsspp") -> "PSP (PPSSPP)"
            packageKey.contains("retroarch") || appKey.contains("retroarch") -> "RetroArch"
            packageKey.contains("snes9x") || appKey.contains("snes9x") -> "SNES (Snes9x)"
            packageKey.contains("vita3k") || appKey.contains("vita3k") -> "PS Vita (Vita3K)"
            packageKey.contains("yuzu") || appKey.contains("yuzu") -> "Switch (Yuzu)"
            else -> null
        }
    }

    private fun buildRunningAppInfo(
        context: Context,
        packageManager: PackageManager,
        packageName: String,
        lastUsedTime: Long,
        knownPackages: Set<String>
    ): RunningAppInfo? {
        if (packageName.equals(context.packageName, ignoreCase = true)) {
            return null
        }

        if (shouldIgnorePackage(packageName)) {
            return null
        }

        val appInfo = try {
            packageManager.getApplicationInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }

        if (packageManager.getLaunchIntentForPackage(packageName) == null) {
            return null
        }

        val appName = packageManager.getApplicationLabel(appInfo).toString().trim().ifBlank { packageName }
        val isEmulator = isKnownEmulator(packageName, appName)
        val hasKnownHistory = knownPackages.contains(packageName.lowercase())
        val hasGameFlag = (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        val isGame = hasGameFlag || isEmulator || hasKnownHistory || looksLikeGamingApp(packageName, appName)

        if (!shouldIncludeApp(appInfo, appName, isGame, hasKnownHistory)) {
            return null
        }

        val suggestion = getSuggestedSession(
            context = context,
            packageName = packageName,
            appName = appName,
            isGame = isGame,
            isEmulator = isEmulator
        )
        val detectedGameName = suggestion?.gameName?.takeIf { suggestedName ->
            isEmulator || !suggestedName.equals(appName, ignoreCase = true)
        }

        return RunningAppInfo(
            packageName = packageName,
            appName = appName,
            isGame = isGame,
            isEmulator = isEmulator,
            detectedGameName = detectedGameName,
            lastUsedTime = lastUsedTime
        )
    }

    private fun collectRecentPackages(
        usageStatsManager: UsageStatsManager
    ): Map<String, Long> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - RECENT_USAGE_WINDOW_MS
        val packages = mutableMapOf<String, Long>()

        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val packageName = event.packageName ?: continue
                if (isForegroundEvent(event)) {
                    recordLatestPackage(packages, packageName, event.timeStamp)
                }
            }
        } catch (_: Exception) {
        }

        try {
            val usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            usageStatsList.orEmpty()
                .filter { stats ->
                    stats.lastTimeUsed >= startTime &&
                        stats.totalTimeInForeground > 0
                }
                .forEach { stats ->
                    recordLatestPackage(packages, stats.packageName, stats.lastTimeUsed)
                }
        } catch (_: Exception) {
        }

        return packages
    }

    private fun recordLatestPackage(
        packages: MutableMap<String, Long>,
        packageName: String,
        timestamp: Long
    ) {
        if (packageName.isBlank() || timestamp <= 0L) {
            return
        }

        val currentTimestamp = packages[packageName] ?: 0L
        if (timestamp > currentTimestamp) {
            packages[packageName] = timestamp
        }
    }

    private fun isForegroundEvent(event: UsageEvents.Event): Boolean {
        return event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
            event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
    }

    private fun loadKnownPackages(context: Context): Set<String> {
        val prefsManager = SharedPreferencesManager(context)
        val collectionRepository = GameCollectionRepository(context)

        return buildSet {
            prefsManager.getBatteryTestPackageMatches()
                .mapTo(this) { it.packageName.lowercase() }

            collectionRepository.getTrackedGameHistory()
                .mapNotNullTo(this) { historyItem ->
                    historyItem.packageName?.lowercase()
                }
        }
    }

    private fun shouldIncludeApp(
        appInfo: ApplicationInfo,
        appName: String,
        isGame: Boolean,
        hasKnownHistory: Boolean
    ): Boolean {
        if (looksLikeIgnoredLabel(appName)) {
            return false
        }

        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

        if (isGame || hasKnownHistory) {
            return true
        }

        return !isSystemApp
    }

    private fun shouldIgnorePackage(packageName: String): Boolean {
        val normalizedPackage = packageName.lowercase()
        return ignoredPackageHints.any { hint ->
            normalizedPackage.contains(hint)
        }
    }

    private fun looksLikeIgnoredLabel(appName: String): Boolean {
        val normalizedName = appName.lowercase()
        return normalizedName.contains("keyboard") ||
            normalizedName.contains("launcher") ||
            normalizedName.contains("settings")
    }

    private fun isKnownEmulator(
        packageName: String,
        appName: String
    ): Boolean {
        val packageKey = packageName.lowercase()
        val appKey = appName.lowercase()

        return emulatorHints.any { hint ->
            packageKey.contains(hint) || appKey.contains(hint)
        }
    }

    private fun looksLikeGamingApp(
        packageName: String,
        appName: String
    ): Boolean {
        val packageKey = packageName.lowercase()
        val appKey = appName.lowercase()

        return packageKey.contains("game") ||
            appKey.contains("game") ||
            emulatorHints.any { hint ->
                packageKey.contains(hint) || appKey.contains(hint)
            }
    }

    private fun detectGameFromEmulator(
        context: Context,
        packageName: String
    ): String? {
        val prefsManager = SharedPreferencesManager(context)
        prefsManager.getBatteryTestPackageMatch(packageName)?.let { savedMatch ->
            return savedMatch.gameName
        }

        return GameCollectionRepository(context)
            .findLatestTrackedGameForPackage(packageName)
            ?.name
    }

    private fun getSuggestedSession(
        context: Context,
        packageName: String,
        appName: String,
        isGame: Boolean,
        isEmulator: Boolean
    ): DetectedSessionSuggestion? {
        val prefsManager = SharedPreferencesManager(context)
        prefsManager.getBatteryTestPackageMatch(packageName)?.let { savedMatch ->
            return DetectedSessionSuggestion(
                gameName = savedMatch.gameName,
                systemName = savedMatch.systemName,
                packageName = savedMatch.packageName
            )
        }

        GameCollectionRepository(context)
            .findLatestTrackedGameForPackage(packageName)
            ?.let { trackedMatch ->
                return DetectedSessionSuggestion(
                    gameName = trackedMatch.name,
                    systemName = trackedMatch.system,
                    packageName = trackedMatch.packageName ?: packageName
                )
            }

        if (!isGame && !isEmulator) {
            return null
        }

        if (isEmulator) {
            val detectedGameName = detectGameFromEmulator(context, packageName)
            val systemName = getSystemName(packageName, appName)
            if (!detectedGameName.isNullOrBlank() && !systemName.isNullOrBlank()) {
                return DetectedSessionSuggestion(
                    gameName = detectedGameName,
                    systemName = systemName,
                    packageName = packageName
                )
            }
            return null
        }

        return DetectedSessionSuggestion(
            gameName = appName,
            systemName = "Android",
            packageName = packageName
        )
    }
}

data class RunningAppInfo(
    val packageName: String,
    val appName: String,
    val isGame: Boolean,
    val isEmulator: Boolean,
    val detectedGameName: String?,
    val lastUsedTime: Long
)

data class DetectedSessionSuggestion(
    val gameName: String,
    val systemName: String,
    val packageName: String
)
