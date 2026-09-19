package com.zenlauncher.zenmode

import android.app.Activity
import android.os.Build

/**
 * Home's swipe pages slide in from the side they live on and back out the same way, so
 * the gesture and the motion agree: swipe right and Zen Score arrives from the left,
 * swipe left and Zen Gold arrives from the right. Settings, opened from any page's ☰,
 * moves forward the same way as Zen Gold. Left to the system default, every activity
 * slid in from the right (or zoomed, depending on the phone).
 */
enum class HomePageSide(
    private val openEnter: Int,
    private val openExit: Int,
    private val closeEnter: Int,
    private val closeExit: Int
) {
    /** Zen Score, reached by swiping right from Home. */
    LEFT(R.anim.page_enter_from_left, R.anim.page_exit_to_right, R.anim.page_enter_from_right, R.anim.page_exit_to_left),

    /** Zen Gold (swiping left from Home) and Settings (any ☰). */
    RIGHT(R.anim.page_enter_from_right, R.anim.page_exit_to_left, R.anim.page_enter_from_left, R.anim.page_exit_to_right);

    /** Call from the page's onCreate. */
    fun applyOnCreate(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, openEnter, openExit)
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, closeEnter, closeExit)
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(openEnter, openExit)
        }
    }

    /** Call from the page's finish(), after super. Android 14+ already has it from [applyOnCreate]. */
    fun applyOnFinish(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(closeEnter, closeExit)
        }
    }
}
