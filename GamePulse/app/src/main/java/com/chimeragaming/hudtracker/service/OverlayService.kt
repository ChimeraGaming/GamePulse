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

/**
 * Service that displays a floating overlay with RAM and battery information
 */
class OverlayService : Service() {
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var displayManager: DisplayManager? = null
    
    private lateinit var ramTextView: TextView
    private lateinit var separatorTextView: TextView
    private lateinit var batteryTextView: TextView
    
    private lateinit var prefsManager: SharedPreferencesManager
    private val updateHandler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    override fun onCreate() {
        super.onCreate()
        
        prefsManager = SharedPreferencesManager(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        
        createOverlayView()
        startUpdating()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Stop updating
        updateRunnable?.let { updateHandler.removeCallbacks(it) }
        
        // Remove overlay view
        overlayView?.let {
            if (it.isAttachedToWindow) {
                windowManager?.removeView(it)
            }
        }
        
        overlayView = null
        windowManager = null
        displayManager = null
    }
    
    private fun createOverlayView() {
        // Inflate the overlay layout
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_view, null)
        
        // Get references to TextViews
        ramTextView = overlayView!!.findViewById(R.id.ramTextView)
        separatorTextView = overlayView!!.findViewById(R.id.separatorTextView)
        batteryTextView = overlayView!!.findViewById(R.id.batteryTextView)
        
        // Set up window layout parameters
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        // Position based on preference
        val position = prefsManager.overlayPosition
        params.gravity = when (position) {
            "top_left" -> Gravity.TOP or Gravity.START
            "top_right" -> Gravity.TOP or Gravity.END
            else -> Gravity.TOP or Gravity.END
        }
        
        params.x = 0
        params.y = 100 // Offset from top to avoid status bar
        
        // Add touch listener for dragging
        setupTouchListener(params)
        
        // Add click listener to open settings
        overlayView?.setOnClickListener {
            openSettings()
        }
        
        // Add overlay to window
        windowManager?.addView(overlayView, params)
    }
    
    private fun setupTouchListener(params: WindowManager.LayoutParams) {
        overlayView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(overlayView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // If movement was minimal, treat as click
                    val deltaX = Math.abs(event.rawX - initialTouchX)
                    val deltaY = Math.abs(event.rawY - initialTouchY)
                    if (deltaX < 10 && deltaY < 10) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun startUpdating() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateStats()
                val refreshRate = prefsManager.overlayRefreshRate
                updateHandler.postDelayed(this, refreshRate * 1000L)
            }
        }
        updateRunnable?.let { updateHandler.post(it) }
    }
    
    private fun updateStats() {
        // Get RAM info
        val ramInfo = RAMUtils.getRAMInfo(this)
        val usedGB = ramInfo.usedMemoryMB / 1024.0
        val totalGB = ramInfo.totalMemoryMB / 1024.0
        val ramText = String.format("RAM: %.1f/%.0f", usedGB, totalGB)
        
        // Get battery info
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
        
        // Update TextViews on UI thread
        updateHandler.post {
            ramTextView.text = ramText
            batteryTextView.text = batteryText
        }
    }
    
    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }
    
    companion object {
        /**
         * Start the overlay service
         */
        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.startService(intent)
        }
        
        /**
         * Stop the overlay service
         */
        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }
    }
}
