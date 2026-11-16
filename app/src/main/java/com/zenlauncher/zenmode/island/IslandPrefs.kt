package com.zenlauncher.zenmode.island

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.zenlauncher.zenmode.coreapi.zenPrefs
import java.time.LocalDate

/**
 * The island's settings and its memory of the day, in the shared settings file.
 *
 * The master switch has no stored value until the person touches it: until then it follows
 * the phone ([IslandDevice.Tier.FULL] starts on, [IslandDevice.Tier.LITE] starts off).
 */
object IslandPrefs {

    const val KEY_ON = "island_on"
    const val KEY_NOTES = "island_notes"
    const val KEY_LIVE = "island_live"

    private const val KEY_MOMENT_DAY = "island_moment_day"
    private const val KEY_MOMENT_STARTED = "island_moment_started"
    private const val KEY_MOMENT_REASON = "island_moment_reason"
    private const val KEY_MOMENT_ENDS = "island_moment_ends"
    private const val KEY_MOMENT_DISMISSED = "island_moment_dismissed"
    private const val KEY_QUIET_DONE_SEEN = "island_quiet_done_seen"
    private const val KEY_PROMISE_SEEN = "island_promise_seen"

    /** The keys whose change means the island should look again at whether it runs. */
    val HOST_KEYS = setOf(KEY_ON, KEY_NOTES, KEY_LIVE)

    fun prefs(ctx: Context): SharedPreferences = ctx.zenPrefs()

    fun isOn(ctx: Context): Boolean {
        val p = prefs(ctx)
        return if (p.contains(KEY_ON)) p.getBoolean(KEY_ON, false) else IslandDevice.tier(ctx) == IslandDevice.Tier.FULL
    }

    /** The person chose, rather than the phone's default. */
    fun isChosen(ctx: Context): Boolean = prefs(ctx).contains(KEY_ON)

    fun setOn(ctx: Context, on: Boolean) = prefs(ctx).edit { putBoolean(KEY_ON, on) }

    fun notesOn(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_NOTES, true)
    fun setNotesOn(ctx: Context, on: Boolean) = prefs(ctx).edit { putBoolean(KEY_NOTES, on) }

    fun liveOn(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_LIVE, true)
    fun setLiveOn(ctx: Context, on: Boolean) = prefs(ctx).edit { putBoolean(KEY_LIVE, on) }

    fun momentLog(ctx: Context): MomentPlan.Log {
        val p = prefs(ctx)
        fun day(key: String) = p.getString(key, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return MomentPlan.Log(
            startedDay = day(KEY_MOMENT_DAY),
            startedAt = p.getLong(KEY_MOMENT_STARTED, 0L),
            reason = p.getString(KEY_MOMENT_REASON, null)?.let { r -> MomentPlan.Reason.entries.firstOrNull { it.name == r } },
            endsAt = p.getLong(KEY_MOMENT_ENDS, 0L),
            dismissedDay = day(KEY_MOMENT_DISMISSED)
        )
    }

    fun recordMoment(ctx: Context, today: LocalDate, show: MomentPlan.Show) = prefs(ctx).edit {
        putString(KEY_MOMENT_DAY, today.toString())
        putLong(KEY_MOMENT_STARTED, show.startedAt)
        putString(KEY_MOMENT_REASON, show.reason.name)
        putLong(KEY_MOMENT_ENDS, show.endsAt)
    }

    /** Swiped away: the week stays out of the island until tomorrow. */
    fun dismissMoment(ctx: Context, today: LocalDate) = prefs(ctx).edit { putString(KEY_MOMENT_DISMISSED, today.toString()) }

    /** Each finished Quiet session is announced once. */
    fun quietDoneSeen(ctx: Context): Long = prefs(ctx).getLong(KEY_QUIET_DONE_SEEN, 0L)
    fun setQuietDoneSeen(ctx: Context, endedAt: Long) = prefs(ctx).edit { putLong(KEY_QUIET_DONE_SEEN, endedAt) }

    /** Reaching the promise is said once a day. */
    fun promiseSeenOn(ctx: Context): String? = prefs(ctx).getString(KEY_PROMISE_SEEN, null)
    fun setPromiseSeen(ctx: Context, today: LocalDate) = prefs(ctx).edit { putString(KEY_PROMISE_SEEN, today.toString()) }
}
