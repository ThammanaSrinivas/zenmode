package com.zenlauncher.zenmode

import android.content.Context

/**
 * The first-run Home guide (swipe right / swipe left / buddy). Set when onboarding finishes,
 * cleared once the guide is done or skipped, so existing users never see it.
 */
object HomeGuidePreferences {
    private const val PREFS_NAME = "zenmode_prefs"
    private const val KEY_PENDING = "home_guide_pending"

    fun isPending(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_PENDING, false)

    fun setPending(context: Context, pending: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PENDING, pending)
            .apply()
    }
}
