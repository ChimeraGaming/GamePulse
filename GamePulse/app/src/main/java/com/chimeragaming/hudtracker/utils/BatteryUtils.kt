package com.chimeragaming.gamepulse.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import com.chimeragaming.gamepulse.model.BatteryInfo
import java.io.File
import kotlin.math.abs

object BatteryUtils {

    private const val MIN_BATTERY_VOLTAGE = 3.0f
    private const val VOLTAGE_DROP_LIMIT = 10000
    private const val THERMAL_SERVICE_NAME = "thermalservice"

    private val thermalDirectories = listOf(
        "/sys/class/thermal",
        "/sys/devices/virtual/thermal"
    )

    private val primaryThermalTypes = listOf(
        "skin",
        "shell",
        "quiet",
        "soc",
        "ap",
        "cpu-therm",
        "soc-therm"
    )

    private val secondaryThermalTypes = listOf(
        "cpu",
        "gpu",
        "board",
        "thermal"
    )

    private val excludedThermalTypes = listOf(
        "battery",
        "bms",
        "charger",
        "usb",
        "hotspot",
        "pmic",
        "modem",
        "power",
        "bcl_"
    )

    fun getBatteryInfo(context: Context): BatteryInfo? {
        return getBatteryStatusIntent(context)?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000.0f
            val batteryTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0f
            val readings = getAllTemperatureReadings(batteryTemperature)
            val cpuAverageTemperature = getCpuAverageTemperature(readings)
                ?: getSelectedTemperatureReading(readings)?.temperatureC
                ?: batteryTemperature.takeIf { it > 0f }
                ?: 0f
            val socTemperature = getSocTemperature(readings) ?: 0f
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)

            BatteryInfo(
                voltage = voltage,
                level = level,
                scale = scale,
                temperature = cpuAverageTemperature,
                socTemperature = socTemperature,
                status = getStatusString(status),
                health = getHealthString(health),
                estimatedLifeMinutes = -1
            )
        }
    }

    fun getTemperatureSensorReadings(context: Context): List<TemperatureSensorReading> {
        val batteryStatus = getBatteryStatusIntent(context) ?: return emptyList()
        val batteryTemperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0f
        val readings = getAllTemperatureReadings(batteryTemperature)
        val selectedReading = getSelectedTemperatureReading(readings)

        return readings.sortedWith(
            compareByDescending<ThermalReading> { it == selectedReading }
                .thenBy { getSensorPriority(it.type) }
                .thenByDescending { it.temperatureC }
                .thenBy { it.type }
        ).map { reading ->
            TemperatureSensorReading(
                label = formatSensorLabel(reading.type),
                temperatureC = reading.temperatureC,
                isSelected = reading == selectedReading
            )
        }
    }

    private fun getBatteryStatusIntent(context: Context): Intent? {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        return context.registerReceiver(null, intentFilter)
    }

    private fun getAllTemperatureReadings(fallbackBatteryTemperature: Float): List<ThermalReading> {
        return buildList {
            addAll(getThermalServiceReadings())
            addAll(getThermalZoneReadings())
            addAll(getHwmonReadings())
            if (fallbackBatteryTemperature > 0f) {
                add(ThermalReading(type = "battery", temperatureC = fallbackBatteryTemperature))
            }
        }.filter { reading ->
            reading.temperatureC.isFinite() && reading.temperatureC in 20f..120f
        }.distinctBy { reading ->
            "${reading.type}:${(reading.temperatureC * 10f).toInt()}"
        }
    }

    private fun getSelectedTemperatureReading(readings: List<ThermalReading>): ThermalReading? {
        val primaryReadings = readings.filter { reading ->
            isPrimaryThermalType(reading.type) && !isExcludedThermalType(reading.type)
        }
        if (primaryReadings.isNotEmpty()) {
            return getRepresentativeReading(primaryReadings)
        }

        val secondaryReadings = readings.filter { reading ->
            isSecondaryThermalType(reading.type) && !isExcludedThermalType(reading.type)
        }
        if (secondaryReadings.isNotEmpty()) {
            return getRepresentativeReading(secondaryReadings)
        }

        val fallbackReadings = readings.filterNot { reading ->
            isExcludedThermalType(reading.type)
        }
        if (fallbackReadings.isNotEmpty()) {
            return getRepresentativeReading(fallbackReadings)
        }

        return null
    }

    private fun getThermalServiceReadings(): List<ThermalReading> {
        return try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, THERMAL_SERVICE_NAME) as? IBinder
                ?: return emptyList()

            val stubClass = Class.forName("android.os.IThermalService\$Stub")
            val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
            val thermalService = asInterfaceMethod.invoke(null, binder) ?: return emptyList()

            val getCurrentTemperaturesMethod = thermalService.javaClass.methods.firstOrNull { method ->
                method.name == "getCurrentTemperatures" && method.parameterCount == 0
            } ?: return emptyList()

            extractThermalServiceReadings(
                getCurrentTemperaturesMethod.invoke(thermalService) ?: return emptyList()
            )
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun extractThermalServiceReadings(response: Any): List<ThermalReading> {
        val entries = when (response) {
            is Array<*> -> response.asList()
            is Iterable<*> -> response.toList()
            else -> emptyList()
        }

        return entries.mapNotNull { entry ->
            val item = entry ?: return@mapNotNull null
            val value = invokeNumberMethod(item, "getValue")?.toFloat() ?: return@mapNotNull null
            val typeCode = invokeNumberMethod(item, "getType")?.toInt()
            val name = invokeStringMethod(item, "getName").orEmpty().lowercase()
            val typeName = thermalTypeCodeName(typeCode)
            val label = listOf(name, typeName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "thermal" }

            ThermalReading(
                type = label,
                temperatureC = normalizeThermalValue(value)
            )
        }
    }

    private fun getThermalZoneReadings(): List<ThermalReading> {
        return thermalDirectories.flatMap { path ->
            val directory = File(path)
            directory.listFiles()
                ?.filter { file -> file.name.startsWith("thermal_zone") }
                ?.mapNotNull { zone -> readThermalZone(zone) }
                .orEmpty()
        }
    }

    private fun readThermalZone(zone: File): ThermalReading? {
        return try {
            val tempFile = File(zone, "temp")
            if (!tempFile.exists()) {
                return null
            }

            val rawValue = tempFile.readText().trim().toFloatOrNull() ?: return null
            val typeValue = File(zone, "type")
                .takeIf { it.exists() }
                ?.readText()
                ?.trim()
                ?.lowercase()
                .orEmpty()

            val normalizedTemperature = normalizeThermalValue(rawValue)
            if (normalizedTemperature <= 0f) {
                null
            } else {
                ThermalReading(
                    type = typeValue.ifBlank { zone.name.lowercase() },
                    temperatureC = normalizedTemperature
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getHwmonReadings(): List<ThermalReading> {
        return try {
            val hwmonDirectory = File("/sys/class/hwmon")
            hwmonDirectory.listFiles()
                ?.filter { file -> file.name.startsWith("hwmon") }
                ?.flatMap { hwmon -> readHwmonSensor(hwmon) }
                .orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun readHwmonSensor(hwmon: File): List<ThermalReading> {
        return try {
            val sensorName = File(hwmon, "name")
                .takeIf { it.exists() }
                ?.readText()
                ?.trim()
                ?.lowercase()
                .orEmpty()

            hwmon.listFiles()
                ?.filter { file ->
                    file.isFile && file.name.matches(Regex("^temp\\d+_input$"))
                }
                ?.mapNotNull { inputFile ->
                    val sensorIndex = inputFile.name
                        .removePrefix("temp")
                        .removeSuffix("_input")
                    val label = File(hwmon, "temp${sensorIndex}_label")
                        .takeIf { it.exists() }
                        ?.readText()
                        ?.trim()
                        ?.lowercase()
                        .orEmpty()
                    val rawValue = inputFile.readText().trim().toFloatOrNull() ?: return@mapNotNull null
                    val normalizedTemperature = normalizeThermalValue(rawValue)
                    if (normalizedTemperature <= 0f) {
                        null
                    } else {
                        val type = listOf(sensorName, label)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                            .ifBlank { hwmon.name.lowercase() }
                        ThermalReading(type = type, temperatureC = normalizedTemperature)
                    }
                }
                .orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun invokeNumberMethod(target: Any, methodName: String): Number? {
        return try {
            target.javaClass.getMethod(methodName).invoke(target) as? Number
        } catch (_: Exception) {
            null
        }
    }

    private fun invokeStringMethod(target: Any, methodName: String): String? {
        return try {
            target.javaClass.getMethod(methodName).invoke(target) as? String
        } catch (_: Exception) {
            null
        }
    }

    private fun thermalTypeCodeName(typeCode: Int?): String {
        return when (typeCode) {
            0 -> "cpu"
            1 -> "gpu"
            2 -> "battery"
            3 -> "skin"
            4 -> "usb"
            5 -> "power"
            6 -> "bcl_voltage"
            7 -> "bcl_current"
            8 -> "bcl_percent"
            9 -> "nand"
            10 -> "soc"
            11 -> "wifi"
            12 -> "camera"
            13 -> "flashlight"
            14 -> "modem"
            else -> ""
        }
    }

    private fun normalizeThermalValue(rawValue: Float): Float {
        val absoluteValue = abs(rawValue)
        return when {
            absoluteValue >= 1000f -> rawValue / 1000f
            absoluteValue >= 200f -> rawValue / 10f
            else -> rawValue
        }
    }

    private fun getRepresentativeReading(readings: List<ThermalReading>): ThermalReading? {
        if (readings.isEmpty()) {
            return null
        }

        val sortedReadings = readings.sortedWith(
            compareBy<ThermalReading> { it.temperatureC }
                .thenBy { it.type }
        )
        return sortedReadings[(sortedReadings.size - 1) / 2]
    }

    private fun getCpuAverageTemperature(readings: List<ThermalReading>): Float? {
        val cpuReadings = readings.filter { reading ->
            isCpuAverageSource(reading.type)
        }
        if (cpuReadings.isEmpty()) {
            return null
        }

        return cpuReadings.map { it.temperatureC }.average().toFloat()
    }

    private fun getSocTemperature(readings: List<ThermalReading>): Float? {
        val exactSocReadings = readings.filter { reading ->
            reading.type.contains("socd") && !isExcludedThermalType(reading.type)
        }
        if (exactSocReadings.isNotEmpty()) {
            return getRepresentativeReading(exactSocReadings)?.temperatureC
        }

        val socReadings = readings.filter { reading ->
            reading.type.contains("soc") && !isExcludedThermalType(reading.type)
        }
        if (socReadings.isNotEmpty()) {
            return getRepresentativeReading(socReadings)?.temperatureC
        }

        return null
    }

    private fun getSensorPriority(type: String): Int {
        return when {
            isPrimaryThermalType(type) -> 0
            isSecondaryThermalType(type) -> 1
            type.contains("battery") -> 3
            else -> 2
        }
    }

    private fun formatSensorLabel(type: String): String {
        if (type.isBlank()) {
            return "Unknown"
        }

        return type
            .replace('_', ' ')
            .replace('-', ' ')
            .replace('/', ' ')
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase().replaceFirstChar { firstChar ->
                    if (firstChar.isLowerCase()) {
                        firstChar.titlecase()
                    } else {
                        firstChar.toString()
                    }
                }
            }
    }

    private fun isPrimaryThermalType(type: String): Boolean {
        return primaryThermalTypes.any { keyword -> type.contains(keyword) }
    }

    private fun isSecondaryThermalType(type: String): Boolean {
        return secondaryThermalTypes.any { keyword -> type.contains(keyword) }
    }

    private fun isExcludedThermalType(type: String): Boolean {
        return excludedThermalTypes.any { keyword -> type.contains(keyword) }
    }

    private fun isCpuAverageSource(type: String): Boolean {
        if (!type.contains("cpu")) {
            return false
        }

        if (type.contains("therm") || type.contains("thermal")) {
            return false
        }

        return type.any { character -> character.isDigit() } || type.contains("cpuss")
    }

    fun estimateBatteryLife(
        currentVoltage: Float,
        previousVoltage: Float,
        timeDifferenceSeconds: Long,
        currentLevel: Int
    ): Int {
        if (previousVoltage <= 0 || timeDifferenceSeconds <= 0) return 0

        val voltageDrop = previousVoltage - currentVoltage
        if (voltageDrop <= 0) return 0

        val voltageDropPerMinute = voltageDrop / (timeDifferenceSeconds / 60.0f)
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

    data class TemperatureSensorReading(
        val label: String,
        val temperatureC: Float,
        val isSelected: Boolean
    )

    private data class ThermalReading(
        val type: String,
        val temperatureC: Float
    )
}
