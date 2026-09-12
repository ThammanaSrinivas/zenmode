package com.zenlauncher.zenmode

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.zenlauncher.zenmode.accessibility.ContentBlockRules
import com.zenlauncher.zenmode.accessibility.NodeQuery
import com.zenlauncher.zenmode.accessibility.SurfaceRule
import com.zenlauncher.zenmode.accessibility.SurfaceDetector
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

/**
 * Two jobs:
 *  1. Global lock-screen action for the home-screen lock button.
 *  2. In-app content blocking: when a tracked app (YouTube, Instagram, Snapchat)
 *     shows a short-form feed surface the user has blocked, press Back to leave
 *     it — same approach as the FlowBit reference app.
 *
 * Robustness matters: if this service throws out of a callback, Android disables
 * it (permanently, after a couple of times — users read that as "the permission
 * keeps resetting"). So the whole event path is wrapped, the node walk is
 * bounded, and high-frequency content-changed events are throttled.
 *
 * Matching logic lives in [SurfaceDetector] / [ContentBlockRules] so it
 * stays unit-testable; this class only wires Android callbacks to it.
 */
class ZenAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_LOCK_SCREEN = "com.zenlauncher.zenmode.ACTION_LOCK_SCREEN"

        private const val TAG = "ZenA11y"
        private const val MIN_ACTION_INTERVAL_MS = 1_500L
        private const val MIN_EVENT_INTERVAL_MS = 200L
        private const val MAX_NODES = 2_000
        private const val MAX_DEPTH = 80

        private const val CRASH_PREFS = "zen_a11y_crash"
        private const val KEY_LAST_CRASH = "last_crash"

        private var instance: ZenAccessibilityService? = null

        fun lockScreen(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
        }

        fun isRunning(): Boolean = instance != null

        /** Last recorded in-callback crash ("<Exception>: <msg> @ <time>"), or null. */
        fun lastCrash(context: Context): String? =
            context.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LAST_CRASH, null)

        fun clearLastCrash(context: Context) {
            context.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY_LAST_CRASH).apply()
        }

        fun isEnabledInSettings(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expectedComponent = "${context.packageName}/${context.packageName}.ZenAccessibilityService"
            return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var rules: ContentBlockRules = ContentBlockRules.EMPTY
    private var lastActionAt = 0L
    private var lastEventAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        rules = ContentBlockRules.default()

        getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_CRASH, null)?.let { last ->
                // Kept (not cleared) so the debug UI can surface it — clear from there.
                Log.w(TAG, "Service reconnected; last recorded crash: $last")
            }
        Log.i(TAG, "Service connected. canRetrieveWindowContent=${serviceInfo?.canRetrieveWindowContent}")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.w(TAG, "Service unbound (accessibility turned off, app updated, or system killed it)")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            handleEvent(event ?: return)
        } catch (t: Throwable) {
            // Never let this propagate — a throw here gets the service disabled.
            Log.e(TAG, "onAccessibilityEvent crashed", t)
            runCatching {
                val stamp = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US)
                    .format(java.util.Date())
                getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_LAST_CRASH, "${t.javaClass.simpleName}: ${t.message} @ $stamp")
                    .apply()
            }
        }
    }

    private fun handleEvent(event: AccessibilityEvent) {
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        // Throttle: content-changed can fire dozens of times per second while
        // scrolling; a full tree walk each time can ANR the service.
        val now = System.currentTimeMillis()
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            now - lastEventAt < MIN_EVENT_INTERVAL_MS
        ) return
        lastEventAt = now

        val pkg = event.packageName?.toString() ?: return
        val debug = ContentBlockPrefs.isDebugDumpEnabled(this)
        val appRule = rules.appRule(pkg) ?: return
        if (!ContentBlockPrefs.isAnyBlockEnabled(this)) return

        val root = rootInActiveWindow
        if (root == null) {
            if (debug) Log.d(TAG, "rootInActiveWindow is null")
            return
        }
        val snapshot = ScreenSnapshot.from(root)
        if (debug) snapshot.log()

        val surface = SurfaceDetector.detect(appRule, snapshot)
        if (debug) Log.d(TAG, "event pkg=$pkg detected surface=${surface?.id}")
        if (surface == null) return
        if (!ContentBlockPrefs.isSurfaceBlocked(this, pkg, surface.id)) return

        block(pkg, surface)
    }

    private fun block(pkg: String, surface: SurfaceRule) {
        val now = System.currentTimeMillis()
        if (now - lastActionAt < MIN_ACTION_INTERVAL_MS) return
        lastActionAt = now

        Log.i(TAG, "Blocking $pkg / ${surface.id}")
        performGlobalAction(GLOBAL_ACTION_BACK)
        Toast.makeText(this, "Blocked by ZenMode", Toast.LENGTH_SHORT).show()

        runCatching {
            ServiceLocator.analyticsManager.trackEvent(
                "content_surface_blocked",
                mapOf("app" to pkg, "surface" to surface.id)
            )
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        instance = null
    }

    /**
     * [NodeQuery] built from one bounded walk of the active-window node tree, so a
     * whole ruleset can be evaluated without re-walking. Also carries the dump.
     */
    private class ScreenSnapshot private constructor(
        private val viewIds: Set<String>,
        private val selectedViewIds: Set<String>,
        private val selectedTexts: Set<String>
    ) : NodeQuery {
        override fun hasViewId(viewId: String) = viewId in viewIds
        override fun isViewIdSelected(viewId: String) = viewId in selectedViewIds
        override fun hasSelectedText(text: String) = selectedTexts.any { it.equals(text, ignoreCase = true) }

        fun log() {
            Log.d(TAG, "dump: ${viewIds.size} view ids, selectedTexts=$selectedTexts")
            viewIds.sorted().forEach { id ->
                Log.d(TAG, "  $id${if (id in selectedViewIds) " [selected]" else ""}")
            }
        }

        companion object {
            fun from(root: AccessibilityNodeInfo): ScreenSnapshot {
                val viewIds = HashSet<String>()
                val selectedViewIds = HashSet<String>()
                val selectedTexts = HashSet<String>()
                var visited = 0

                fun walk(node: AccessibilityNodeInfo?, depth: Int) {
                    if (node == null || depth > MAX_DEPTH || visited >= MAX_NODES) return
                    visited++
                    try {
                        if (node.isVisibleToUser) {
                            node.viewIdResourceName?.let { id ->
                                viewIds += id
                                if (node.isSelected) selectedViewIds += id
                            }
                            if (node.isSelected) {
                                val label = (node.text ?: node.contentDescription)?.toString()?.trim()
                                if (!label.isNullOrEmpty() && label.length <= 40) selectedTexts += label
                            }
                        }
                        val count = node.childCount
                        for (i in 0 until count) walk(node.getChild(i), depth + 1)
                    } catch (_: Throwable) {
                        // stale / recycled node — skip it, keep walking siblings
                    }
                }
                walk(root, 0)
                return ScreenSnapshot(viewIds, selectedViewIds, selectedTexts)
            }
        }
    }
}
