package com.zenlauncher.zenmode.island

import android.content.Context
import android.provider.Settings
import android.view.WindowManager
import com.zenlauncher.zenmode.ZenAccessibilityService
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

/**
 * Decides whether the island runs and which window carries it, and keeps it out of the way.
 *
 * It runs when the person has it on and the phone lets us draw over other apps: through the
 * accessibility service when that is on (the island sits round the camera, above the status bar),
 * otherwise through "Display over other apps" (docked just under the status bar). With neither,
 * it waits. [update] is cheap and idempotent, so anything that might change the answer calls it.
 *
 * It steps aside when the phone is sideways (a video, a game).
 */
object IslandHost {


    private var window: IslandWindow? = null
    private var host: Context? = null
    private var sources: IslandSources? = null

    val isActive: Boolean get() = window != null

    /** The island is carried by the accessibility service (so it should hear from SystemUI). */
    val onAccessibility: Boolean
        get() = window?.type == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

    /** Why it isn't showing, for Settings. */
    enum class Status { RUNNING, OFF, NEEDS_ACCESS }

    fun status(context: Context): Status = when {
        !IslandPrefs.isOn(context) -> Status.OFF
        canDraw(context) -> Status.RUNNING
        else -> Status.NEEDS_ACCESS
    }

    fun canDraw(context: Context): Boolean =
        ZenAccessibilityService.current() != null || ZenAccessibilityService.isEnabledInSettings(context) ||
            Settings.canDrawOverlays(context)

    fun update(context: Context) {
        val app = context.applicationContext
        val a11y = ZenAccessibilityService.current()
        val wanted = runCatching { IslandPrefs.isOn(app) }.getOrDefault(false)
        val (type, carrier) = when {
            !wanted -> null to null
            a11y != null -> WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY to a11y
            Settings.canDrawOverlays(app) -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY to app
            else -> null to null
        }
        if (window != null && window?.type == type && host === carrier) {
            sources?.refresh()
            return
        }
        stop()
        if (type == null || carrier == null) return
        val w = IslandWindow(carrier, type, IslandDevice.tier(app))
        if (runCatching { w.attach() }.isFailure) return
        window = w
        host = carrier
        sources = IslandSources(app).also { it.start() }
        applyHidden()
        runCatching {
            ServiceLocator.analyticsManager.trackEvent(
                "island_started",
                mapOf("host" to if (type == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY) "accessibility" else "overlay")
            )
        }
    }

    /** The accessibility service is going away: the island moves to the overlay or stops. */
    fun onAccessibilityGone(context: Context) {
        if (onAccessibility) stop()
        update(context)
    }

    fun stop() {
        sources?.stop()
        sources = null
        window?.detach()
        window = null
        host = null
        IslandStore.clear()
    }

    fun onConfigurationChanged() {
        val w = window ?: return
        w.onConfigurationChanged()
        applyHidden()
    }

    fun media(): IslandMedia? = sources?.media
    fun poke() = sources?.poke()
    fun preview() = sources?.preview()

    /**
     * Sideways, the island steps aside: that is where videos and games are. Upright it stays, over
     * full-screen apps and the pulled-down shade alike, as iPhone's island does. (Android gives an
     * overlay no reliable word on the shade or the status bar: the accessibility window list keeps
     * a full-screen system window in it, and insets read "hidden" for a window above the bar.)
     */
    private fun applyHidden() {
        IslandStore.setHidden(window?.isPortrait == false)
    }
}
