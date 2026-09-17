package com.zenlauncher.zenmode

import android.content.Context

object PromisePreferences {
    private const val PREFS_NAME = "zenmode_prefs"
    private const val KEY_DAILY_HOURS = "promise_daily_hours"

    fun getDailyHours(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DAILY_HOURS, AppConstants.PLACEHOLDER_PROMISE_HOURS)
            .coerceIn(AppConstants.PROMISE_MIN_DAILY_HOURS, AppConstants.PROMISE_MAX_DAILY_HOURS)

    fun setDailyHours(context: Context, hours: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(
                KEY_DAILY_HOURS,
                hours.coerceIn(AppConstants.PROMISE_MIN_DAILY_HOURS, AppConstants.PROMISE_MAX_DAILY_HOURS)
            )
            .apply()
    }
}
