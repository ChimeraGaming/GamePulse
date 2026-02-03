package com.chimeragaming.gamepulse.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.chimeragaming.gamepulse.model.BatteryInfo

/**
 * Utility class for battery-related operations
 */
object BatteryUtils {
    
    // Li-ion battery voltage characteristics
    private const val TYPICAL_VOLTAGE_RANGE = 1.2f // 4.2V (100%) to 3.0V (0%)
    private const val MIN_BATTERY_VOLTAGE = 3.0f // Minimum voltage for Li-ion battery
    private const val VOLTAGE_DROP_LIMIT = 10000 // Maximum estimated minutes cap
    
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
                health = getHealthString(health)
            )
        }
    }
    
    /**
     * Estimate battery life based on voltage drop
     * @param currentVoltage Current voltage in volts
     * @param previousVoltage Previous voltage reading
     * @param timeDifferenceSeconds Time between readings in seconds
     * @param currentLevel Current battery percentage
     */
    fun estimateBatteryLife(
        currentVoltage: Float,
        previousVoltage: Float,
        timeDifferenceSeconds: Long,
        currentLevel: Int
    ): Int {
        if (previousVoltage <= 0 || timeDifferenceSeconds <= 0) return 0
        
        val voltageDrop = previousVoltage - currentVoltage
        if (voltageDrop <= 0) return 0 // Not draining or charging - return 0 instead of Int.MAX_VALUE
        
        // Estimate voltage drop per minute
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
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }
}
