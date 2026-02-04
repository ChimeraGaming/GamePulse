package com.chimeragaming.gamepulse.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.chimeragaming.gamepulse.R
import com.chimeragaming.gamepulse.ui.MainActivity
import com.chimeragaming.gamepulse.utils.BatteryUtils
import com.chimeragaming.gamepulse.utils.RAMUtils
import com.chimeragaming.gamepulse.utils.SharedPreferencesManager

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                    FLOATING OVERLAY SERVICE                           ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║                  v0.3.1 - Crash Protection Added                      ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var displayManager: DisplayManager? = null

    private var ramTextView: TextView? = null
    private var separatorTextView: TextView? = null
    private var batteryTextView: TextView? = null

    private lateinit var prefsManager: SharedPreferencesManager
    private val updateHandler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var isDestroyed = false

    override fun onCreate() {
        super.onCreate()

        try {
            prefsManager = SharedPreferencesManager(this)
            windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

            if (windowManager == null) {
                stopSelf()
                return
            }

            createOverlayView()
            startUpdating()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        isDestroyed = true

        try {
            updateRunnable?.let { updateHandler.removeCallbacks(it) }
            updateRunnable = null

            overlayView?.let { view ->
                if (view.isAttachedToWindow) {
                    windowManager?.removeView(view)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            overlayView = null
            windowManager = null
            displayManager = null
            ramTextView = null
            separatorTextView = null
            batteryTextView = null
        }
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                       CREATE OVERLAY VIEW                             ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun createOverlayView() {
        try {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater
            if (inflater == null) {
                stopSelf()
                return
            }

            overlayView = inflater.inflate(R.layout.overlay_view, null)

            ramTextView = overlayView?.findViewById(R.id.ramTextView)
            separatorTextView = overlayView?.findViewById(R.id.separatorTextView)
            batteryTextView = overlayView?.findViewById(R.id.batteryTextView)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            val position = prefsManager.overlayPosition
            params.gravity = when (position) {
                "top_left" -> Gravity.TOP or Gravity.START
                "top_right" -> Gravity.TOP or Gravity.END
                else -> Gravity.TOP or Gravity.END
            }

            params.x = 0
            params.y = 100

            setupTouchListener(params)

            overlayView?.setOnClickListener {
                openSettings()
            }

            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                         TOUCH LISTENER                                ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun setupTouchListener(params: WindowManager.LayoutParams) {
        overlayView?.setOnTouchListener { view, event ->
            try {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isDestroyed || windowManager == null || overlayView?.isAttachedToWindow != true) {
                            return@setOnTouchListener false
                        }

                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()

                        when {
                            (params.gravity and Gravity.END) == Gravity.END -> {
                                params.x = initialX - deltaX
                            }
                            (params.gravity and Gravity.START) == Gravity.START -> {
                                params.x = initialX + deltaX
                            }
                            else -> {
                                params.x = initialX + deltaX
                            }
                        }

                        params.y = initialY + deltaY

                        try {
                            windowManager?.updateViewLayout(overlayView, params)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaX = Math.abs(event.rawX - initialTouchX)
                        val deltaY = Math.abs(event.rawY - initialTouchY)
                        if (deltaX < 10 && deltaY < 10) {
                            view.performClick()
                        }
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                         START UPDATING                                ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun startUpdating() {
        updateRunnable = object : Runnable {
            override fun run() {
                if (!isDestroyed && overlayView?.isAttachedToWindow == true) {
                    try {
                        updateStats()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val refreshRate = try {
                        prefsManager.overlayRefreshRate
                    } catch (e: Exception) {
                        10
                    }

                    updateHandler.postDelayed(this, refreshRate * 1000L)
                }
            }
        }
        updateRunnable?.let { updateHandler.post(it) }
    }

    /*
     * ╔═══════════════════════════════════════════════════════════════════════╗
     * ║                         UPDATE STATS                                  ║
     * ╚═══════════════════════════════════════════════════════════════════════╝
     */
    private fun updateStats() {
        if (isDestroyed || overlayView?.isAttachedToWindow != true) {
            return
        }

        try {
            val ramInfo = RAMUtils.getRAMInfo(this)
            val usedGB = ramInfo.usedMemoryMB / 1024.0
            val totalGB = ramInfo.getTotalMemoryFormatted()
            val ramText = String.format("RAM: %.1f/%s GB", usedGB, totalGB)

            val batteryInfo = BatteryUtils.getBatteryInfo(this)
            val batteryText = if (batteryInfo != null) {
                val percentage = batteryInfo.percentage.toInt()
                val lifeText = if (batteryInfo.estimatedLifeMinutes > 0) {
                    batteryInfo.getEstimatedLifeFormatted()
                } else {
                    "N/A"
                }
                "🔋 $lifeText | $percentage%"
            } else {
                "🔋 N/A | N/A"
            }

            updateHandler.post {
                try {
                    if (!isDestroyed && overlayView?.isAttachedToWindow == true) {
                        ramTextView?.text = ramText
                        batteryTextView?.text = batteryText
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openSettings() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun start(context: Context) {
            try {
                val intent = Intent(context, OverlayService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, OverlayService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}