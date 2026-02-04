package com.chimeragaming.gamepulse.utils

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chimeragaming.gamepulse.R

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                      RAM THEME RENDERER                               ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║                v0.3.2 - Zelda Heart Containers                        ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
class RamThemeRenderer(private val context: Context) {

    companion object {
        const val THEME_POWER_CORES = "power_cores"
        const val THEME_HEART_CONTAINERS = "heart_containers"
        const val THEME_DIAMONDS = "diamonds"
        const val THEME_HEXAGONS = "hexagons"
        const val THEME_PROGRESS_BAR = "progress_bar"
        const val THEME_OFF = "off"
    }

    fun renderRAM(
        container: LinearLayout,
        usedGB: Float,
        totalGB: Int,
        theme: String
    ) {
        container.removeAllViews()

        when (theme) {
            THEME_POWER_CORES -> renderPowerCores(container, usedGB, totalGB)
            THEME_HEART_CONTAINERS -> renderHeartContainers(container, usedGB, totalGB)
            THEME_DIAMONDS -> renderDiamonds(container, usedGB, totalGB)
            THEME_HEXAGONS -> renderHexagons(container, usedGB, totalGB)
            THEME_PROGRESS_BAR -> renderProgressBar(container, usedGB, totalGB)
            THEME_OFF -> container.visibility = ViewGroup.GONE
        }

        if (theme != THEME_OFF) {
            container.visibility = ViewGroup.VISIBLE
        }
    }

    private fun renderPowerCores(container: LinearLayout, usedGB: Float, totalGB: Int) {
        val filledCount = usedGB.toInt()

        for (i in 0 until totalGB) {
            val textView = TextView(context).apply {
                text = if (i < filledCount) "●" else "○"
                textSize = 20f
                setTextColor(
                    if (i < filledCount)
                        ContextCompat.getColor(context, R.color.github_dark_green)
                    else
                        ContextCompat.getColor(context, R.color.github_dark_border)
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 4
                }
            }
            container.addView(textView)
        }
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║              HEART CONTAINERS - ZELDA STYLE WITH QUARTERS             ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun renderHeartContainers(container: LinearLayout, usedGB: Float, totalGB: Int) {
        val fullHearts = usedGB.toInt()
        val decimal = usedGB - fullHearts

        // Render full hearts
        for (i in 0 until fullHearts) {
            container.addView(createHeart(HeartState.FULL))
        }

        // Render partial heart based on decimal
        if (fullHearts < totalGB && decimal > 0.0f) {
            val heartState = when {
                decimal <= 0.25f -> HeartState.QUARTER
                decimal <= 0.50f -> HeartState.HALF
                decimal <= 0.75f -> HeartState.THREE_QUARTER
                else -> HeartState.FULL
            }
            container.addView(createHeart(heartState))
        }

        // Render empty hearts for remaining
        val heartsRendered = if (decimal > 0.0f) fullHearts + 1 else fullHearts
        for (i in heartsRendered until totalGB) {
            container.addView(createHeart(HeartState.EMPTY))
        }
    }

    private enum class HeartState {
        EMPTY,
        QUARTER,
        HALF,
        THREE_QUARTER,
        FULL
    }

    private fun createHeart(state: HeartState): TextView {
        return TextView(context).apply {
            text = when (state) {
                HeartState.EMPTY -> "♡"        // Empty outline
                HeartState.QUARTER -> "♥¼"     // Heart + 1/4 indicator
                HeartState.HALF -> "♥½"        // Heart + 1/2 indicator
                HeartState.THREE_QUARTER -> "♥¾" // Heart + 3/4 indicator
                HeartState.FULL -> "♥"         // Full red heart
            }
            textSize = when (state) {
                HeartState.EMPTY, HeartState.FULL -> 24f
                else -> 20f // Slightly smaller for quarter indicators
            }
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(when (state) {
                HeartState.EMPTY -> ContextCompat.getColor(context, R.color.github_dark_border)
                else -> ContextCompat.getColor(context, R.color.github_dark_red)
            })
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 6
            }
        }
    }

    private fun renderDiamonds(container: LinearLayout, usedGB: Float, totalGB: Int) {
        val filledCount = usedGB.toInt()

        for (i in 0 until totalGB) {
            val textView = TextView(context).apply {
                text = if (i < filledCount) "◆" else "◇"
                textSize = 20f
                setTextColor(
                    if (i < filledCount)
                        ContextCompat.getColor(context, R.color.github_dark_accent)
                    else
                        ContextCompat.getColor(context, R.color.github_dark_border)
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 4
                }
            }
            container.addView(textView)
        }
    }

    private fun renderHexagons(container: LinearLayout, usedGB: Float, totalGB: Int) {
        val filledCount = usedGB.toInt()

        for (i in 0 until totalGB) {
            val textView = TextView(context).apply {
                text = if (i < filledCount) "⬢" else "⬡"
                textSize = 20f
                setTextColor(
                    if (i < filledCount)
                        ContextCompat.getColor(context, R.color.github_dark_green)
                    else
                        ContextCompat.getColor(context, R.color.github_dark_border)
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 4
                }
            }
            container.addView(textView)
        }
    }

    private fun renderProgressBar(container: LinearLayout, usedGB: Float, totalGB: Int) {
        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = totalGB * 100
            progress = (usedGB * 100).toInt()
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val percentageText = TextView(context).apply {
            text = String.format("%.1f%%", (usedGB / totalGB) * 100)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.github_dark_text))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 8
            }
        }

        container.addView(progressBar)
        container.addView(percentageText)
    }
}