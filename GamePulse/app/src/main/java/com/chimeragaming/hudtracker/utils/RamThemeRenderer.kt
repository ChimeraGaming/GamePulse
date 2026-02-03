package com.chimeragaming.gamepulse.utils

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chimeragaming.gamepulse.R

/**
 * Renderer for different RAM visualization themes
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
    
    private fun renderHeartContainers(container: LinearLayout, usedGB: Float, totalGB: Int) {
        val filledCount = usedGB.toInt()
        
        for (i in 0 until totalGB) {
            val textView = TextView(context).apply {
                text = if (i < filledCount) "♥" else "♡"
                textSize = 20f
                setTextColor(
                    if (i < filledCount) 
                        ContextCompat.getColor(context, R.color.github_dark_red)
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
            max = totalGB * 100 // Multiply by 100 for better precision
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
