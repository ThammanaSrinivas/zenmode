package com.zenlauncher.zenmode

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateMapOf

/**
 * Keeps per-app notification counts for the home tiles' badges.
 *
 * Android wakes this service for every notification posted or removed anywhere on the
 * device — screen off, phone pocketed, all day — so the work done per callback is the
 * app's main standby battery cost. Three things keep it small:
 *
 *  1. [rebuildCounts] is only run while the home screen is actually on screen. The badges
 *     can't be seen otherwise; off-screen callbacks just set [dirty], and [refreshNow]
 *     catches up when Home comes back.
 *  2. Bursts are coalesced. A mail sync delivering ten notifications used to mean ten full
 *     rebuilds; now they collapse into one after [COALESCE_MS].
 *  3. The observable map is diffed rather than cleared and refilled, so a rebuild that
 *     finds nothing new doesn't invalidate every badge in the grid.
 *
 * Fetching `activeNotifications` is the expensive step — a binder round-trip that parcels
 * every active notification, extras and bitmaps included — which is why it sits behind all
 * three gates rather than running on each callback.
 */
class ZenNotificationListenerService : NotificationListenerService() {

    companion object {
        /** Observable map: packageName -> active notification count. */
        val notificationCounts = mutableStateMapOf<String, Int>()

        /** How long to wait for a burst of notifications to settle before rebuilding. */
        private const val COALESCE_MS = 250L

        private var instance: ZenNotificationListenerService? = null

        /** True while Home is on screen — see [refreshNow]. */
        @Volatile
        private var homeVisible = false

        /** A callback arrived while Home was off screen, so the counts need a rebuild. */
        @Volatile
        private var dirty = false

        fun isRunning(): Boolean = instance != null

        /**
         * Called by [MainActivity] as Home starts and stops. While Home is off screen the
         * listener stops rebuilding; on the way back in, any notification that arrived
         * meanwhile is folded in before the badges are drawn.
         */
        fun setHomeVisible(visible: Boolean) {
            homeVisible = visible
            if (visible) {
                refreshNow()
            } else {
                // A rebuild queued just before Home left would otherwise run off screen,
                // paying for the fetch with nothing to show. `dirty` survives, so it's not
                // lost — just deferred to the next time the badges can actually be seen.
                instance?.let { it.handler.removeCallbacks(it.coalescedRebuild) }
            }
        }

        /** Rebuilds immediately if anything changed while Home was away. */
        fun refreshNow() {
            val service = instance ?: return
            if (!dirty) return
            service.rebuildCounts()
        }

        fun isEnabledInSettings(context: Context): Boolean = EnabledComponents.contains(
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners"),
            context.packageName,
            ZenNotificationListenerService::class.java.name
        )
    }

    private val handler = Handler(Looper.getMainLooper())
    private val coalescedRebuild = Runnable { rebuildCounts() }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        rebuildCounts()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        scheduleRebuild()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        scheduleRebuild()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        handler.removeCallbacks(coalescedRebuild)
        instance = null
        dirty = false
        notificationCounts.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(coalescedRebuild)
        // onListenerDisconnected() is not guaranteed before teardown; without clearing the
        // static reference here the destroyed service stays retained (LeakCanary leak).
        if (instance === this) {
            instance = null
        }
    }

    /**
     * Defers the rebuild so a burst of callbacks costs one fetch, and skips it entirely
     * while Home is off screen — [refreshNow] picks the work up when Home returns.
     */
    private fun scheduleRebuild() {
        dirty = true
        if (!homeVisible) return
        handler.removeCallbacks(coalescedRebuild)
        handler.postDelayed(coalescedRebuild, COALESCE_MS)
    }

    private fun rebuildCounts() {
        handler.removeCallbacks(coalescedRebuild)
        val active = try {
            activeNotifications
        } catch (_: Exception) {
            return
        } ?: return

        dirty = false
        val entries = active.map { NotificationCounts.Entry(it.packageName, it.isOngoing) }
        NotificationCounts.applyTo(
            notificationCounts,
            NotificationCounts.countsOf(entries, packageName)
        )
    }
}
