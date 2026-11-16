package com.zenlauncher.zenmode.quiet

import android.content.Context
import androidx.core.content.edit
import com.zenlauncher.zenmode.ContentBlockPrefs
import com.zenlauncher.zenmode.ZenAccessibilityService
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * A Quiet session: [MINUTES] minutes in which every Reels & Shorts surface is quiet, started
 * by the person (from the Quiet widget) and ended by the clock.
 *
 * Enforcement is the accessibility service's, exactly as for the blocker's own switches: the
 * end time lives in the blocker's prefs ([ContentBlockPrefs.KEY_QUIET_ENDS_AT]) so the
 * service's change listener sees it, and the service compares it with the clock on every
 * event. Nothing has to fire for quieting to stop on time. The widgets learn that a session
 * ended from their own refresh at [endsAt] ([com.zenlauncher.zenmode.widget.ZenWidgets]).
 *
 * Without the accessibility service a session would quiet nothing, so [isAvailable] gates every
 * place that starts one.
 */
object QuietSession {

    /** One preset for now: Reels & Shorts, for 25 minutes. */
    const val MINUTES = 25

    /** A session running now. */
    data class Running(val startedAt: Long, val endsAt: Long) {
        val totalMillis: Long get() = endsAt - startedAt
        fun millisLeft(now: Long) = (endsAt - now).coerceAtLeast(0L)
        fun progress(now: Long) = ((now - startedAt).toFloat() / totalMillis).coerceIn(0f, 1f)
    }

    /** The last session that ended, for the widget's "Done" moment. */
    data class Ended(val at: Long, val minutes: Int, val sessionsToday: Int)

    private const val KEY_STARTED_AT = "quiet_started_at"
    private const val KEY_LAST_ENDED_AT = "quiet_last_ended_at"
    private const val KEY_LAST_MINUTES = "quiet_last_minutes"
    private const val KEY_DAY = "quiet_day"
    private const val KEY_COUNT = "quiet_count_today"

    /** How long a finished session is still worth showing as "Done". */
    val DONE_SHOWS_FOR_MILLIS: Long = TimeUnit.MINUTES.toMillis(30)

    private fun prefs(ctx: Context) = ContentBlockPrefs.prefs(ctx)

    /** Quieting works only while the accessibility service is on. */
    fun isAvailable(ctx: Context): Boolean = ZenAccessibilityService.isEnabledInSettings(ctx)

    fun running(ctx: Context, now: Long = System.currentTimeMillis()): Running? {
        val p = prefs(ctx)
        val endsAt = p.getLong(ContentBlockPrefs.KEY_QUIET_ENDS_AT, 0L)
        if (endsAt <= now) return null
        return Running(p.getLong(KEY_STARTED_AT, now), endsAt)
    }

    /** The session that ended last, if it ended within [DONE_SHOWS_FOR_MILLIS] and nothing runs. */
    fun justEnded(ctx: Context, now: Long = System.currentTimeMillis(), today: LocalDate = LocalDate.now()): Ended? {
        if (running(ctx, now) != null) return null
        val p = prefs(ctx)
        // A session that ran out on the clock is finished the moment it ends: settle it here.
        settleExpired(ctx, now, today)
        val at = p.getLong(KEY_LAST_ENDED_AT, 0L)
        if (at <= 0L || now - at > DONE_SHOWS_FOR_MILLIS) return null
        return Ended(at, p.getInt(KEY_LAST_MINUTES, MINUTES), sessionsToday(ctx, today))
    }

    fun sessionsToday(ctx: Context, today: LocalDate = LocalDate.now()): Int {
        val p = prefs(ctx)
        return if (p.getString(KEY_DAY, null) == today.toString()) p.getInt(KEY_COUNT, 0) else 0
    }

    /** Starts a session now. Lifts a blocker pause: starting quiet is asking for quiet. */
    fun start(ctx: Context, now: Long = System.currentTimeMillis(), minutes: Int = MINUTES): Running {
        val endsAt = now + TimeUnit.MINUTES.toMillis(minutes.toLong())
        ContentBlockPrefs.resume(ctx)
        prefs(ctx).edit {
            putLong(KEY_STARTED_AT, now)
            putLong(ContentBlockPrefs.KEY_QUIET_ENDS_AT, endsAt)
        }
        return Running(now, endsAt)
    }

    /** Ends the running session now (End on the widget). A session nobody ended settles by itself. */
    fun end(ctx: Context, now: Long = System.currentTimeMillis(), today: LocalDate = LocalDate.now()) {
        val run = running(ctx, now) ?: return
        finish(ctx, run.startedAt, now, today)
    }

    private fun settleExpired(ctx: Context, now: Long, today: LocalDate) {
        val p = prefs(ctx)
        val endsAt = p.getLong(ContentBlockPrefs.KEY_QUIET_ENDS_AT, 0L)
        if (endsAt in 1..now) finish(ctx, p.getLong(KEY_STARTED_AT, endsAt), endsAt, today)
    }

    private fun finish(ctx: Context, startedAt: Long, endedAt: Long, today: LocalDate) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(endedAt - startedAt).toInt().coerceAtLeast(1)
        val count = sessionsToday(ctx, today) + 1
        prefs(ctx).edit {
            remove(ContentBlockPrefs.KEY_QUIET_ENDS_AT)
            remove(KEY_STARTED_AT)
            putLong(KEY_LAST_ENDED_AT, endedAt)
            putInt(KEY_LAST_MINUTES, minutes)
            putString(KEY_DAY, today.toString())
            putInt(KEY_COUNT, count)
        }
    }
}
