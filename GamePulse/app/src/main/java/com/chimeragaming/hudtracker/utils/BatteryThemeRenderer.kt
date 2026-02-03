package com.chimeragaming.gamepulse.utils

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chimeragaming.gamepulse.R
import com.chimeragaming.gamepulse.model.BatteryInfo

/**
 * Renderer for different battery visualization themes
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
        // Hide all views first
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
            THEME_OFF -> {} // All views already hidden
        }
    }

    private fun renderStatsPanel(panel: LinearLayout, batteryInfo: BatteryInfo) {
        panel.visibility = View.VISIBLE

        // Update the text views in the stats panel (safe casts)
        val infoText = panel.getChildAt(0) as? TextView
        val statusText = panel.getChildAt(1) as? TextView
        val lifeText = panel.getChildAt(2) as? TextView

        // BatteryInfo model doesn't include capacity; use percentage instead
        infoText?.text = "Battery - ${batteryInfo.percentage.toInt()}% | Health: ${batteryInfo.health}"
        statusText?.text = "Status: ${batteryInfo.status} | Voltage: ${String.format("%.2f", batteryInfo.voltage)}V"
        lifeText?.text = "Estimated Life: ${batteryInfo.getEstimatedLifeFormatted()}"
    }

    private fun renderPowerCell(container: FrameLayout, batteryInfo: BatteryInfo) {
        container.visibility = View.VISIBLE
        container.removeAllViews()

        // Create a simple power cell visualization
        val cellView = TextView(context).apply {
            text = "${batteryInfo.percentage.toInt()}%"
            textSize = 32f
            setTextColor(getBatteryColor(batteryInfo.percentage))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
        }

        container.addView(cellView)
    }

    private fun renderGauge(container: FrameLayout, batteryInfo: BatteryInfo) {
        container.visibility = View.VISIBLE
        container.removeAllViews()

        // Create a gauge with percentage and estimated life
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
}