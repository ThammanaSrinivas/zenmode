package com.zenlauncher.zenmode

import android.content.Context
import com.zenlauncher.zenmode.coreapi.zenPrefs

/**
 * The first-run Home guide (swipe right / swipe left / buddy). Set when onboarding finishes,
 * cleared once the guide is done or skipped, so existing users never see it.
 */
object HomeGuidePreferences {
    private const val KEY_PENDING = "home_guide_pending"

    fun isPending(context: Context): Boolean =
        context.zenPrefs().getBoolean(KEY_PENDING, false)

    fun setPending(context: Context, pending: Boolean) {
        context.zenPrefs()
            .edit()
            .putBoolean(KEY_PENDING, pending)
            .apply()
    }
}
