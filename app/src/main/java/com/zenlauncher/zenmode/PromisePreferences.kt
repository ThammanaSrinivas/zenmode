package com.zenlauncher.zenmode

import android.content.Context
import com.zenlauncher.zenmode.recap.weekStartOf
import java.time.DayOfWeek
import java.time.LocalDate

/** Why [PromisePreferences.editLock] won't let the promise be changed right now. */
enum class PromiseEditLock {
    /** Nothing blocks an edit. */
    NONE,
    /** Free only edits on Sundays; today isn't one. */
    SUNDAY_ONLY,
    /** This week's edit quota (1 for free, 2 for Pro) is used up. */
    WEEKLY_LIMIT_REACHED
}

object PromisePreferences {
    private const val PREFS_NAME = "zenmode_prefs"
    private const val KEY_DAILY_HOURS = "promise_daily_hours"
    private const val KEY_EDIT_WEEK_START = "promise_edit_week_start"
    private const val KEY_EDIT_COUNT = "promise_edit_count"

    private const val FREE_EDITS_PER_WEEK = 1
    private const val PRO_EDITS_PER_WEEK = 2

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

    /**
     * Free edits the promise once a week, Sundays only. Pro drops the day restriction and
     * gets a second edit. The quota resets on the calendar week (Monday), matching the
     * "Monday starts a new week" rule already shown in My Promise's rules note.
     */
    fun editLock(context: Context, isPro: Boolean, today: LocalDate = LocalDate.now()): PromiseEditLock {
        val limit = if (isPro) PRO_EDITS_PER_WEEK else FREE_EDITS_PER_WEEK
        if (editsUsedThisWeek(context, today) >= limit) return PromiseEditLock.WEEKLY_LIMIT_REACHED
        if (!isPro && today.dayOfWeek != DayOfWeek.SUNDAY) return PromiseEditLock.SUNDAY_ONLY
        return PromiseEditLock.NONE
    }

    /** Call once an edit has actually been committed, so it counts against this week's quota. */
    fun recordPromiseEdit(context: Context, today: LocalDate = LocalDate.now()) {
        val used = editsUsedThisWeek(context, today)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EDIT_WEEK_START, weekStartOf(today).toString())
            .putInt(KEY_EDIT_COUNT, used + 1)
            .apply()
    }

    private fun editsUsedThisWeek(context: Context, today: LocalDate): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedWeekStart = prefs.getString(KEY_EDIT_WEEK_START, null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (storedWeekStart != weekStartOf(today)) return 0
        return prefs.getInt(KEY_EDIT_COUNT, 0)
    }
}
