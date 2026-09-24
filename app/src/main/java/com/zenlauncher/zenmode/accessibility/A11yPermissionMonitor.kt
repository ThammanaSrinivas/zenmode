package com.zenlauncher.zenmode.accessibility

import android.content.Context
import android.os.Build
import com.zenlauncher.zenmode.ZenAccessibilityService
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

/**
 * Reports when the accessibility permission appears and disappears, so resets on real devices
 * show up in analytics (Logcat never leaves the phone).
 *
 * A force-stop kills the process before anything could be sent, so loss isn't reported when it
 * happens: we remember that it was granted, and report the loss on the next check that finds it
 * gone. Checked on Home resume and when the service connects; only transitions send an event.
 */
object A11yPermissionMonitor {
    private const val PREFS = "zen_a11y_health"
    private const val KEY_GRANTED_AT = "granted_at"

    enum class Transition { GRANTED, LOST }

    fun transition(enabled: Boolean, wasGranted: Boolean): Transition? = when {
        enabled && !wasGranted -> Transition.GRANTED
        !enabled && wasGranted -> Transition.LOST
        else -> null
    }

    fun check(context: Context, now: Long = System.currentTimeMillis()) {
        if (!ServiceLocator.isInitialized) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val grantedAt = prefs.getLong(KEY_GRANTED_AT, 0L)
        when (transition(ZenAccessibilityService.isEnabledInSettings(context), grantedAt > 0L)) {
            Transition.GRANTED -> {
                prefs.edit().putLong(KEY_GRANTED_AT, now).apply()
                ServiceLocator.analyticsTracker.trackA11yPermissionGranted()
            }
            Transition.LOST -> {
                prefs.edit().remove(KEY_GRANTED_AT).apply()
                ServiceLocator.analyticsTracker.trackA11yPermissionLost(
                    manufacturer = Build.MANUFACTURER.orEmpty(),
                    model = Build.MODEL.orEmpty(),
                    sdk = Build.VERSION.SDK_INT,
                    serviceRunning = ZenAccessibilityService.isRunning(),
                    hoursSinceGranted = (now - grantedAt).coerceAtLeast(0L) / 3_600_000L
                )
            }
            null -> Unit
        }
    }
}
