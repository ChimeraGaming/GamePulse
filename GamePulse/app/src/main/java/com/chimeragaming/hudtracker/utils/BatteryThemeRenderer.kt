package com.chimeragaming.gamepulse.utils

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chimeragaming.gamepulse.R
import com.chimeragaming.gamepulse.model.BatteryInfo

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                    BATTERY THEME RENDERER                             ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║                v0.3.1 - Power Cell Battery Design                     ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
class BatteryThemeRenderer(private val context: Context) {

    companion object {
        const val THEME_STATS_PANEL = "stats_panel"
        const val THEME_POWER_CELL = "power_cell"
        const val THEME_GAUGE = "gauge"
        const val THEME_MINIMAL = "minimal"
        const val THEME_OFF = "off"
    }

    fun renderBattery(
        statsPanel: LinearLayout,
        powerCell: FrameLayout,
        gauge: FrameLayout,
        minimal: TextView,
        batteryInfo: BatteryInfo?,
        theme: String
    ) {
        statsPanel.visibility = View.GONE
        powerCell.visibility = View.GONE
        gauge.visibility = View.GONE
        minimal.visibility = View.GONE

        batteryInfo ?: return

        when (theme) {
            THEME_STATS_PANEL -> renderStatsPanel(statsPanel, batteryInfo)
            THEME_POWER_CELL -> renderPowerCell(powerCell, batteryInfo)
            THEME_GAUGE -> renderGauge(gauge, batteryInfo)
            THEME_MINIMAL -> renderMinimal(minimal, batteryInfo)
            THEME_OFF -> {}
        }
    }

    private fun renderStatsPanel(panel: LinearLayout, batteryInfo: BatteryInfo) {
        panel.visibility = View.VISIBLE

        val infoText = panel.getChildAt(0) as? TextView
        val statusText = panel.getChildAt(1) as? TextView
        val lifeText = panel.getChildAt(2) as? TextView

        infoText?.text = "Battery - ${batteryInfo.percentage.toInt()}% | Health: ${batteryInfo.health}"
        statusText?.text = "Status: ${batteryInfo.status} | Voltage: ${String.format("%.2f", batteryInfo.voltage)}V"
        lifeText?.text = "Estimated Life: ${batteryInfo.getEstimatedLifeFormatted()}"
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                    POWER CELL - BATTERY DESIGN                        ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun renderPowerCell(container: FrameLayout, batteryInfo: BatteryInfo) {
        container.visibility = View.VISIBLE
        container.removeAllViews()

        val batteryLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Battery tip (positive terminal)
        val batteryTip = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(20),
                dpToPx(6)
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setBackgroundColor(Color.parseColor("#666666"))
        }

        // Battery body container
        val batteryBody = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(60),
                dpToPx(120)
            )
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            setBackgroundResource(android.R.drawable.dialog_frame)
        }

        // Calculate fill level (0-100%)
        val percentage = batteryInfo.percentage.toInt().coerceIn(0, 100)
        val isCharging = batteryInfo.status.contains("Charging", ignoreCase = true)

        // Create 5 battery segments (each 20%)
        val segments = 5
        val filledSegments = (percentage / 20.0).toInt()

        for (i in segments downTo 1) {
            val segment = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                ).apply {
                    if (i < segments) topMargin = dpToPx(2)
                }

                // Determine color based on segment and status
                val color = when {
                    i > filledSegments -> Color.parseColor("#2A2A2A") // Empty
                    isCharging -> Color.parseColor("#00B0FF") // Electric Blue
                    percentage <= 10 -> Color.parseColor("#F44336") // Critical Red
                    percentage <= 20 -> Color.parseColor("#FF9800") // Orange
                    percentage <= 40 -> Color.parseColor("#FFC107") // Yellow
                    else -> Color.parseColor("#4CAF50") // Green
                }
                setBackgroundColor(color)
            }
            batteryBody.addView(segment)
        }

        // Charging indicator (lightning bolt)
        if (isCharging) {
            val chargingIcon = TextView(context).apply {
                text = "⚡"
                textSize = 32f
                setTextColor(Color.WHITE)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                gravity = Gravity.CENTER
                elevation = 4f
            }
            batteryBody.addView(chargingIcon)
        }

        // Percentage text below battery
        val percentageText = TextView(context).apply {
            text = "$percentage%"
            textSize = 18f
            setTextColor(getBatteryTextColor(batteryInfo))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
            }
            gravity = Gravity.CENTER
        }

        // Build layout
        batteryLayout.addView(batteryTip)
        batteryLayout.addView(batteryBody)
        batteryLayout.addView(percentageText)

        container.addView(batteryLayout)
    }

    private fun renderGauge(container: FrameLayout, batteryInfo: BatteryInfo) {
        container.visibility = View.VISIBLE
        container.removeAllViews()

        val gaugeLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
        }

        val percentageView = TextView(context).apply {
            text = "${batteryInfo.percentage.toInt()}%"
            textSize = 36f
            setTextColor(getBatteryColor(batteryInfo.percentage))
            gravity = Gravity.CENTER
        }

        val lifeView = TextView(context).apply {
            text = batteryInfo.getEstimatedLifeFormatted()
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.github_dark_text))
            gravity = Gravity.CENTER
        }

        gaugeLayout.addView(percentageView)
        gaugeLayout.addView(lifeView)
        container.addView(gaugeLayout)
    }

    private fun renderMinimal(minimal: TextView, batteryInfo: BatteryInfo) {
        minimal.visibility = View.VISIBLE
        minimal.text = "${batteryInfo.percentage.toInt()}%"
        minimal.setTextColor(getBatteryColor(batteryInfo.percentage))
    }

    private fun getBatteryColor(percent: Float): Int {
        val pct = percent.toInt()
        return when {
            pct >= 70 -> ContextCompat.getColor(context, R.color.github_dark_primary)
            pct >= 30 -> ContextCompat.getColor(context, R.color.github_dark_secondary)
            else -> ContextCompat.getColor(context, android.R.color.holo_red_light)
        }
    }

    private fun getBatteryTextColor(batteryInfo: BatteryInfo): Int {
        val isCharging = batteryInfo.status.contains("Charging", ignoreCase = true)
        val percentage = batteryInfo.percentage.toInt()

        return when {
            isCharging -> Color.parseColor("#00B0FF") // Electric Blue
            percentage <= 10 -> Color.parseColor("#F44336") // Critical Red
            percentage <= 20 -> Color.parseColor("#FF9800") // Orange
            percentage <= 40 -> Color.parseColor("#FFC107") // Yellow
            else -> Color.parseColor("#4CAF50") // Green
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}