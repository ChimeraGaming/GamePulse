package com.chimeragaming.gamepulse.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.chimeragaming.gamepulse.model.BatteryInfo

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                         BATTERY UTILS                                 ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║                   v0.3.2 - Voltage-Based Estimation                   ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
object BatteryUtils {

    private const val TYPICAL_VOLTAGE_RANGE = 1.2f
    private const val MIN_BATTERY_VOLTAGE = 3.0f
    private const val VOLTAGE_DROP_LIMIT = 10000

    /**
     * Get current battery information
     */
    fun getBatteryInfo(context: Context): BatteryInfo? {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)

        return batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000.0f
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0f
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)

            BatteryInfo(
                voltage = voltage,
                level = level,
                scale = scale,
                temperature = temperature,
                status = getStatusString(status),
                health = getHealthString(health),
                estimatedLifeMinutes = -1 // Default to calculating
            )
        }
    }

    /**
     * Estimate battery life based on voltage drop
     * Returns: 0 = no drain detected or error, >0 = estimated minutes remaining
     */
    fun estimateBatteryLife(
        currentVoltage: Float,
        previousVoltage: Float,
        timeDifferenceSeconds: Long,
        currentLevel: Int
    ): Int {
        if (previousVoltage <= 0 || timeDifferenceSeconds <= 0) return 0

        val voltageDrop = previousVoltage - currentVoltage

        // No drain or voltage increased (charging?)
        if (voltageDrop <= 0) return 0

        // Calculate voltage drop per minute
        val voltageDropPerMinute = voltageDrop / (timeDifferenceSeconds / 60.0f)

        // Calculate remaining voltage above minimum
        val remainingVoltage = currentVoltage - MIN_BATTERY_VOLTAGE

        if (voltageDropPerMinute > 0 && remainingVoltage > 0) {
            val estimatedMinutes = (remainingVoltage / voltageDropPerMinute).toInt()
            return estimatedMinutes.coerceIn(0, VOLTAGE_DROP_LIMIT)
        }

        return 0
    }

    private fun getStatusString(status: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }
    }

    private fun getHealthString(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }
}