package com.zenlauncher.zenmode

import android.content.Context

object AppGridPreferences {
    private const val PREFS_NAME = "zenmode_prefs"
    private const val KEY_APP_COUNT = "home_app_count"

    const val DEFAULT_APP_COUNT = 8

    val APP_COUNT_OPTIONS = listOf(4, 8, 12, 16)

    fun getAppCount(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_APP_COUNT, DEFAULT_APP_COUNT)

    fun setAppCount(context: Context, count: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_APP_COUNT, count).apply()
    }
}
