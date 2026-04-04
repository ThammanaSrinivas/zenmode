package com.zenlauncher.zenmode

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateMapOf

class ZenNotificationListenerService : NotificationListenerService() {

    companion object {
        /** Observable map: packageName -> active notification count. */
        val notificationCounts = mutableStateMapOf<String, Int>()

        private var instance: ZenNotificationListenerService? = null

        fun isRunning(): Boolean = instance != null

        fun isEnabledInSettings(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val componentName = ComponentName(context, ZenNotificationListenerService::class.java)
            return flat.contains(componentName.flattenToString())
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        rebuildCounts()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        rebuildCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        rebuildCounts()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        notificationCounts.clear()
    }

    private fun rebuildCounts() {
        val active = try {
            activeNotifications
        } catch (_: Exception) {
            return
        }
        val counts = mutableMapOf<String, Int>()
        for (sbn in active) {
            // Skip ongoing notifications (music players, VPNs, etc.)
            // and skip our own notifications
            if (!sbn.isOngoing && sbn.packageName != packageName) {
                val pkg = sbn.packageName
                counts[pkg] = (counts[pkg] ?: 0) + 1
            }
        }
        notificationCounts.clear()
        notificationCounts.putAll(counts)
    }
}
