package com.zenlauncher.zenmode.coreapi

import android.content.Context

/**
 * The daily screen-time promise (My Promise / Zen Gold). Lives in core-api, not the app
 * module, because `core-private`'s StatSyncWorker needs it too for the Zen Score sync — same
 * reasoning as [ZenScore] itself — and core-private cannot depend on the app module.
 */
object PromisePreferences {
    private const val PREFS_NAME = "zenmode_prefs"
    private const val KEY_DAILY_HOURS = "promise_daily_hours"

    fun getDailyHours(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DAILY_HOURS, CoreConstants.PLACEHOLDER_PROMISE_HOURS)
            .coerceIn(CoreConstants.PROMISE_MIN_DAILY_HOURS, CoreConstants.PROMISE_MAX_DAILY_HOURS)

    fun setDailyHours(context: Context, hours: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(
                KEY_DAILY_HOURS,
                hours.coerceIn(CoreConstants.PROMISE_MIN_DAILY_HOURS, CoreConstants.PROMISE_MAX_DAILY_HOURS)
            )
            .apply()
    }
}
