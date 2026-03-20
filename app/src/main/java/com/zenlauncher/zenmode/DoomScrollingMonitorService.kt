package com.zenlauncher.zenmode

import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

class DoomScrollingMonitorService : Service() {

    companion object {
        var isRunning = false
            private set
    }

    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 30000L // 30 seconds
    private val usageThreshold = 15 * 60 * 1000L // 15 minutes
    private val USAGE_LOOKBACK_TIME = 1000 * 60 * 60L // 1 hour

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, checkInterval)
        }
    }

    private lateinit var powerManager: android.os.PowerManager

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        
        startForegroundServiceNotification()
        startMonitoring()
    }

    private fun startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "ZenModeMonitor"
            val channelName = "App Usage Monitor"
            val channel = android.app.NotificationChannel(
                channelId,
                channelName,
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(channel)

            val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setContentTitle("ZenMode Active")
                .setContentText("Stop Doom scrolling...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var type = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                if (Build.VERSION.SDK_INT >= 34) { // Android 14
                    type = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                }
                startForeground(1, notification, type)
            } else {
                startForeground(1, notification)
            }
        }
    }


    private fun startMonitoring() {
        handler.post(monitorRunnable)
    }

    private var currentDoomSessionStart: Long = 0
    private var lastDoomPackage: String? = null
    private fun checkForegroundApp() {
        if (!hasUsageStatsPermission()) return
        
        // If screen is off, pause/stop tracking (or just don't increment)
        if (!powerManager.isInteractive) {
             // Optionally reset if we consider screen off breaking the session
             // resetTracking() 
             // For now, let's just return. If they wake up and are still in app, session continues?
             // Usually screen off breaks "doom scrolling".
             resetTracking()
             return
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - USAGE_LOOKBACK_TIME // Look back 1 hour

        // Use queryEvents for accurate "Foreground" detection
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = android.app.usage.UsageEvents.Event()
        var lastForegroundPackage: String? = null
        var lastForegroundTime: Long = 0

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundPackage = event.packageName
                lastForegroundTime = event.timeStamp
            }
        }

        if (lastForegroundPackage != null) {
            // Found the last app that moved to foreground
            
            // Check if it's a doom app
            if (isDoomApp(lastForegroundPackage)) {
                if (lastDoomPackage == lastForegroundPackage) {
                    // Continuing same session
                    if (currentDoomSessionStart == 0L) {
                        currentDoomSessionStart = System.currentTimeMillis()
                    }
                    
                    val sessionDuration = System.currentTimeMillis() - currentDoomSessionStart
                    if (sessionDuration > usageThreshold) {
                        showOverlay()
                    }
                } else {
                    // New doom app
                    lastDoomPackage = lastForegroundPackage
                    currentDoomSessionStart = System.currentTimeMillis()
                }
            } else {
                // Not a doom app
                resetTracking()
                removeOverlay()
            }
        } else {
             // Could not determine foreground app (rare if lookback is long enough)
             // fallback to old logic or just wait
        }
    }

    private fun resetTracking() {
        currentDoomSessionStart = 0
        lastDoomPackage = null
    }

    private fun isDoomApp(packageName: String): Boolean {
        // Explicit list
        val doomPackages = listOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.google.android.youtube",
            "com.netflix.mediaclient"
        )
        if (doomPackages.contains(packageName)) return true

        // Category check
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (info.category) {
                    ApplicationInfo.CATEGORY_SOCIAL,
                    ApplicationInfo.CATEGORY_VIDEO,
                    ApplicationInfo.CATEGORY_GAME -> true
                    else -> false
                }
            } else {
                false
            }
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return // Already showing

        // Analytics
        val tracker = ServiceLocator.analyticsTracker
        val durationSec = (System.currentTimeMillis() - currentDoomSessionStart) / 1000
        tracker.trackMindlessScrollDetected(
            durationSec = durationSec,
            appName = lastDoomPackage ?: "unknown",
            appCategory = "social", // Simplified, as category isn't dynamically fetched for event here
            nudgeShown = true
        )
        tracker.trackMindfulScrollPromptShown("duration_15_min")

        if (!Settings.canDrawOverlays(this)) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT, // Or MATCH_PARENT to cover full screen? User said "bottom alert/overlay" but design looks full card.
            // Image design looks like a card that is somewhat centered or fills screen. 
            // User requested Gravity.CENTER.
            // "Type,TYPE_APPLICATION_OVERLAY,... Gravity,Gravity.CENTER"
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, // Allows interaction
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM
        // params.width = WindowManager.LayoutParams.MATCH_PARENT // If we want full width card
        // params.height = WindowManager.LayoutParams.WRAP_CONTENT

        // Let's match layout exactly.
        
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.dialog_doom_scrolling, null)

        val closeButton = overlayView?.findViewById<View>(R.id.btn_close)
        closeButton?.setOnClickListener {
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)

            // Analytics
            ServiceLocator.analyticsTracker.trackMindfulScrollPromptResponse("pause_now", "social")

            resetTracking()
            removeOverlay()
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
             appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(monitorRunnable)
        removeOverlay()
        super.onDestroy()
    }
}
