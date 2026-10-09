package com.zenlauncher.zenmode

import android.content.Context
import com.zenlauncher.zenmode.coreapi.zenPrefs

object AppGridPreferences {
    private const val KEY_APP_COUNT = "home_app_count"

    const val DEFAULT_APP_COUNT = 8

    fun getAppCount(context: Context): Int =
        context.zenPrefs()
            .getInt(KEY_APP_COUNT, DEFAULT_APP_COUNT)

    fun setAppCount(context: Context, count: Int) {
        context.zenPrefs()
            .edit().putInt(KEY_APP_COUNT, count).apply()
    }
}
