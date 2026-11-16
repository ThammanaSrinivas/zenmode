package com.zenlauncher.zenmode.island

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zenlauncher.zenmode.island.ui.IslandRoot

/**
 * The window the island lives in, above every app.
 *
 * Carried by the accessibility service ([WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY]:
 * above the status bar, so it can sit round the camera and take a tap there, and on the lock
 * screen too) or, without it, by "Display over other apps" ([WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]:
 * below the status bar, so it docks just under it, where a tap still reaches it).
 *
 * The window is only ever as big as what is drawn, so every touch outside the island still
 * reaches the app under it. It is centred on the island ([IslandGeometry.centerX]) and grows
 * downwards, so resizing it never moves the island: the content stays put and is only clipped.
 */
class IslandWindow(
    /** The accessibility service itself, or the application. */
    private val context: Context,
    val type: Int,
    val tier: IslandDevice.Tier
) {
    val placement: Placement =
        if (type == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY) Placement.CAMERA else Placement.DOCKED

    private val wm: WindowManager = context.getSystemService(WindowManager::class.java)
    private val owner = Owner()
    private val root = TouchFrame(context)
    private var attached = false
    private var width = 1
    private var height = 1
    private var touchable = false

    /** Read by the composition; changes when the phone turns or the display changes. */
    val geometry = mutableStateOf(measure())

    private val params = WindowManager.LayoutParams(
        1, 1, type,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        title = "ZenIsland"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            fitInsetsTypes = 0
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        windowAnimations = 0
    }

    fun attach() {
        if (attached) return
        owner.start()
        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)
        val compose = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { IslandRoot(this@IslandWindow) }
        }
        root.addView(compose, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        place()
        wm.addView(root, params)
        attached = true
    }

    fun detach() {
        if (!attached) return
        attached = false
        runCatching { wm.removeViewImmediate(root) }
        root.removeAllViews()
        owner.stop()
    }

    /** The phone turned or the display changed: measure again. */
    fun onConfigurationChanged() {
        geometry.value = measure()
        place()
        if (attached) runCatching { wm.updateViewLayout(root, params) }
    }

    val isPortrait: Boolean
        get() = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE

    /**
     * Sizes the window to what the island needs now, in px. Called by the composition: growth is
     * applied at once (before the island grows into it), shrinking only once it has finished.
     */
    fun request(widthPx: Int, heightPx: Int, wantsTouch: Boolean) {
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        if (w == width && h == height && wantsTouch == touchable) return
        width = w
        height = h
        touchable = wantsTouch
        place()
        if (attached) runCatching { wm.updateViewLayout(root, params) }
    }

    private fun place() {
        val g = geometry.value
        params.width = width
        params.height = height
        params.x = Math.round(g.centerX - g.screenWidth / 2f)
        params.y = Math.round(g.top - EDGE_DP * g.density).coerceAtLeast(0)
        params.flags = if (touchable) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
    }

    /** Where the island sits on this screen, from the display (not this small window's insets). */
    private fun measure(): IslandGeometry {
        val display = context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display?.getRealMetrics(metrics) ?: metrics.setTo(context.resources.displayMetrics)
        val density = metrics.density
        val screenWidth = metrics.widthPixels.toFloat()
        val cutouts = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            display?.cutout?.boundingRects.orEmpty().map { Cutout(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat()) }
        } else {
            emptyList()
        }
        val topCutout = cutouts.filter { it.top < 80 * density }.maxOfOrNull { it.bottom } ?: 0f
        val statusBar = maxOf(statusBarHeight(), topCutout)
        return IslandGeometry.of(screenWidth, statusBar, cutouts, density, placement)
    }

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    private fun statusBarHeight(): Float {
        val res = Resources.getSystem()
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) res.getDimensionPixelSize(id).toFloat() else 24 * res.displayMetrics.density
    }

    /** Catches a touch outside the island, which closes an open card. */
    private class TouchFrame(context: Context) : FrameLayout(context) {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                if (IslandStore.scene.value.open) IslandStore.close()
                return false
            }
            return super.dispatchTouchEvent(event)
        }

        init {
            clipChildren = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        }
    }

    /** The lifecycle a composition needs, outside any activity. */
    private class Owner : SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

        fun start() {
            if (registry.currentState != Lifecycle.State.INITIALIZED) return
            saved.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            if (registry.currentState == Lifecycle.State.INITIALIZED) return
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    companion object {
        /** Room above the pill for its edge and glow. */
        const val EDGE_DP = 0f
    }
}
