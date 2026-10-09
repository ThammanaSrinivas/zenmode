package com.zenlauncher.zenmode

import android.content.Context
import com.zenlauncher.zenmode.coreapi.zenPrefs
import java.time.LocalDate
import java.time.LocalTime

/**
 * Settings and bookkeeping for the daily check-in overlay (see [ZenCheckIn]): whether it's
 * on, what time the evening card appears, and the last date each slot was shown so neither
 * fires twice in a day.
 *
 * The evening time is the one piece of this the user controls — Settings → Daily check-in.
 */
object ZenCheckInPreferences {
    private const val KEY_ENABLED = "check_in_enabled"
    private const val KEY_EVENING_MINUTE_OF_DAY = "check_in_evening_minute_of_day"
    private const val KEY_LAST_SHOWN_PREFIX = "check_in_last_shown_"

    /** On by default: it's the surface that makes the weekly streak rule visible at all. */
    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** When the evening card appears. Stored as minutes past midnight, local time. */
    fun getEveningTime(context: Context): LocalTime {
        val stored = prefs(context).getInt(KEY_EVENING_MINUTE_OF_DAY, DEFAULT_MINUTE_OF_DAY)
        return LocalTime.of(stored / 60, stored % 60)
    }

    fun setEveningTime(context: Context, time: LocalTime) {
        prefs(context).edit()
            .putInt(
                KEY_EVENING_MINUTE_OF_DAY,
                (time.hour * 60 + time.minute).coerceIn(EARLIEST_MINUTE_OF_DAY, LATEST_MINUTE_OF_DAY)
            )
            .apply()
    }

    fun getLastShown(context: Context, slot: ZenCheckInSlot): LocalDate? =
        prefs(context).getString(KEY_LAST_SHOWN_PREFIX + slot.name.lowercase(), null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun setLastShown(context: Context, slot: ZenCheckInSlot, date: LocalDate = LocalDate.now()) {
        prefs(context).edit()
            .putString(KEY_LAST_SHOWN_PREFIX + slot.name.lowercase(), date.toString())
            .apply()
    }

    /**
     * Picker bounds. "Evening" is the promise of the feature, so the window runs from late
     * afternoon to just before midnight — a check-in at 06:00 would report on a day that
     * hasn't happened.
     */
    val earliest: LocalTime get() = LocalTime.of(EARLIEST_MINUTE_OF_DAY / 60, 0)
    val latest: LocalTime get() = LocalTime.of(LATEST_MINUTE_OF_DAY / 60, LATEST_MINUTE_OF_DAY % 60)

    private const val DEFAULT_MINUTE_OF_DAY = 20 * 60 // 20:00
    private const val EARLIEST_MINUTE_OF_DAY = 16 * 60 // 16:00
    private const val LATEST_MINUTE_OF_DAY = 23 * 60 + 30 // 23:30

    private fun prefs(context: Context) =
        context.zenPrefs()
}
