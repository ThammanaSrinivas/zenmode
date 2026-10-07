package com.zenlauncher.zenmode

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Which of the day's two check-in moments an overlay belongs to. Persisted (one
 * last-shown date each), so each moment fires at most once a day — a user who was under
 * the promise at 20:00 and over it by 21:00 gets the evening card once, not twice.
 */
enum class ZenCheckInSlot { LAST_HOUR, EVENING }

/**
 * What the check-in overlay says today.
 *
 * · [CELEBRATION] — the evening slot with today still under the promise. Confetti.
 * · [ENCOURAGEMENT] — the evening slot with today already over it. No confetti: the point
 *   is that the *week* is what breaks a streak, not the day (see [PromiseStreak]).
 * · [LAST_HOUR] — roughly an hour of promise left, while there's still something to save.
 */
enum class ZenCheckInKind(val slot: ZenCheckInSlot) {
    CELEBRATION(ZenCheckInSlot.EVENING),
    ENCOURAGEMENT(ZenCheckInSlot.EVENING),
    LAST_HOUR(ZenCheckInSlot.LAST_HOUR);

    val confetti: Boolean get() = this == CELEBRATION

    /** Analytics/preference key — stable across copy changes, unlike [name]'s casing. */
    val key: String get() = name.lowercase()
}

/** Everything the overlay needs to draw itself, resolved once when the card is raised. */
data class ZenCheckInCard(
    val kind: ZenCheckInKind,
    val streak: PromiseStreak.State,
    val promiseHours: Int,
    val todayMinutes: Long,
    /** Promise minutes still unspent today; 0 once the promise is gone. */
    val minutesLeft: Long,
    val headline: String,
    val body: String,
    val actionLabel: String
)

/**
 * Decides when the daily check-in overlay is due and what it should say.
 *
 * Two moments a day, both drawn on Home (ZenMode OS *is* the launcher, so Home is where the
 * user lands between apps — no system overlay permission needed, and nothing is drawn over
 * another app):
 *
 *  1. **Evening**, at the time the user picks in Settings → Daily check-in
 *     ([ZenCheckInPreferences]). Celebrates with confetti when today is still under the
 *     promise; otherwise reminds the user that the week, not the day, is what counts.
 *  2. **Last hour**, when about an hour of the promise is left — the moment a nudge can
 *     still change the outcome. Only for promises over
 *     [AppConstants.CHECK_IN_LAST_HOUR_MIN_PROMISE_HOURS] hours: on a 1- or 2-hour promise
 *     "an hour left" lands almost as soon as the phone is picked up, which is noise, not help.
 *
 * Pure functions, like [PromiseStreak] and [ZenGoldPromise] — the caller reads prefs, usage
 * and history and passes them in.
 */
object ZenCheckIn {

    /**
     * The card to raise right now, or null when neither moment is due.
     *
     * The last hour wins a tie: it's time-critical and the evening card will still be
     * waiting afterwards (different slot, so showing one never consumes the other).
     *
     * @param lastShown the date that [slot]'s card last appeared, or null if never.
     */
    fun due(
        now: LocalDateTime,
        eveningAt: LocalTime,
        promiseHours: Int,
        todayMinutes: Long,
        streak: PromiseStreak.State,
        lastShown: (ZenCheckInSlot) -> LocalDate?
    ): ZenCheckInCard? {
        val today = now.toLocalDate()
        fun alreadyShown(slot: ZenCheckInSlot) = lastShown(slot) == today

        val promiseMinutes = promiseHours * 60L
        val minutesLeft = (promiseMinutes - todayMinutes).coerceAtLeast(0)

        if (!alreadyShown(ZenCheckInSlot.LAST_HOUR) && isLastHour(promiseHours, minutesLeft)) {
            return card(ZenCheckInKind.LAST_HOUR, streak, promiseHours, todayMinutes, minutesLeft)
        }

        if (!alreadyShown(ZenCheckInSlot.EVENING) && !now.toLocalTime().isBefore(eveningAt)) {
            val kind = if (minutesLeft > 0) ZenCheckInKind.CELEBRATION else ZenCheckInKind.ENCOURAGEMENT
            return card(kind, streak, promiseHours, todayMinutes, minutesLeft)
        }

        return null
    }

    /**
     * In the closing window of a promise worth warning about. Zero minutes left means the
     * promise is already spent — there's nothing left to protect, so the evening card's
     * honest framing takes over instead of a warning that arrives too late.
     */
    private fun isLastHour(promiseHours: Int, minutesLeft: Long): Boolean =
        promiseHours > AppConstants.CHECK_IN_LAST_HOUR_MIN_PROMISE_HOURS &&
            minutesLeft in 1..AppConstants.CHECK_IN_LAST_HOUR_WINDOW_MINUTES

    /** Exposed for the overlay's own preview/debug paths, which name a kind directly. */
    fun card(
        kind: ZenCheckInKind,
        streak: PromiseStreak.State,
        promiseHours: Int,
        todayMinutes: Long,
        minutesLeft: Long = (promiseHours * 60L - todayMinutes).coerceAtLeast(0)
    ): ZenCheckInCard = ZenCheckInCard(
        kind = kind,
        streak = streak,
        promiseHours = promiseHours,
        todayMinutes = todayMinutes,
        minutesLeft = minutesLeft,
        headline = headline(kind, streak),
        body = body(kind, streak, promiseHours, minutesLeft),
        actionLabel = actionLabel(kind)
    )

    // ── Copy ──────────────────────────────────────────────────────
    // Written per streak length rather than one line for everyone: a first day and a
    // sixty-day run are not the same achievement, and the overlay is the one surface that
    // can say so. ZenMode OS voice — second person, plain, no exclamation marks.

    internal fun headline(kind: ZenCheckInKind, streak: PromiseStreak.State): String {
        val days = streak.days
        return when (kind) {
            ZenCheckInKind.CELEBRATION -> when {
                days <= 1 -> "One day under the line. This is where it starts."
                days < 4 -> "$days days under. Early, but it's becoming a shape."
                days < 8 -> "$days days under the line, and it's holding."
                days < 15 -> "$days days. This isn't luck any more."
                days < 31 -> "$days days under your own promise. That's a habit."
                days < 61 -> "$days days. Most people never get a month in."
                else -> "$days days under the line. This is just who you are now."
            }

            ZenCheckInKind.ENCOURAGEMENT -> when {
                streak.weekAtRisk -> "Today went over, and so has the week."
                days == 0 -> "Today went over. Nothing's lost yet."
                days == 1 -> "Today went over. Your one day still stands."
                else -> "Today went over. Your $days-day streak still stands."
            }

            // Loss-framed, deliberately: this is the one card in the app that is allowed
            // to make the user uncomfortable, and what it names is always something they
            // already own rather than something they might win.
            // The win, stated plainly, and nothing else. This is the loudest line on the
            // card by design: the user arriving here has kept a promise for days, and the
            // hour is the footnote, not the headline.
            ZenCheckInKind.LAST_HOUR -> when {
                days <= 0 -> "Today can be day one"
                days == 1 -> "1-day streak"
                else -> "$days-day streak"
            }
        }
    }

    internal fun body(
        kind: ZenCheckInKind,
        streak: PromiseStreak.State,
        promiseHours: Int,
        minutesLeft: Long
    ): String = when (kind) {
        // The requested line, verbatim in spirit: the user is told plainly where they stand.
        ZenCheckInKind.CELEBRATION ->
            "Your screen limit is under promise — ${durationPhrase(minutesLeft)} of your " +
                "$promiseHours-hour day still unspent. " + weekLine(streak)

        ZenCheckInKind.ENCOURAGEMENT -> if (streak.weekAtRisk) {
            "This week can't reach ${AppConstants.PROMISE_DAYS_TO_UNLOCK} of " +
                "${AppConstants.PROMISE_DAYS_PER_WEEK} any more, so the streak restarts Monday. " +
                "Nothing you did today changed the days behind you."
        } else {
            "One day over doesn't break a streak here — the week does. " + weekLine(streak)
        }

        // One ask, achievable today, and never a threat. The weekly quota is not argued
        // here — it has its own quiet line on the card.
        ZenCheckInKind.LAST_HOUR ->
            if (streak.days <= 0) "Stay under it and you bank your first day."
            else "Stay under it and you bank day ${streak.days}."
    }

    /**
     * Every day left this week, today included, now has to be kept for the week to reach
     * [AppConstants.PROMISE_DAYS_TO_UNLOCK] — so losing today loses the week, and the week
     * is what breaks the streak.
     */
    internal fun mustWinToday(streak: PromiseStreak.State): Boolean =
        streak.daysLeftThisWeek > 0 &&
            streak.daysKeptThisWeek + streak.daysLeftThisWeek == AppConstants.PROMISE_DAYS_TO_UNLOCK

    /** Where this week stands against the 5-of-7 verdict that actually decides the streak. */
    private fun weekLine(streak: PromiseStreak.State): String {
        val needed = AppConstants.PROMISE_DAYS_TO_UNLOCK - streak.daysKeptThisWeek
        return when {
            needed <= 0 -> "This week is already won."
            streak.weekAtRisk -> "This week is out of reach, but Monday starts clean."
            needed == 1 -> "One more day under keeps the week."
            else -> "$needed more days under keeps the week, and you have " +
                "${streak.daysLeftThisWeek} left to find them."
        }
    }

    private fun actionLabel(kind: ZenCheckInKind) = when (kind) {
        ZenCheckInKind.CELEBRATION -> "Keep it going"
        ZenCheckInKind.ENCOURAGEMENT -> "Win the week"
        ZenCheckInKind.LAST_HOUR -> "Lock in today"
    }

    /**
     * The buffer in the largest unit that stays whole, for the card's hero figure:
     * "50 min", "2 hr", "1h 20m". One number, one unit, nothing to reconcile.
     */
    internal fun bufferCompact(minutes: Long): String = when {
        minutes <= 0 -> "0 min"
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0L -> "${minutes / 60} hr"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }

    /** Spoken form of the hero status, where the ring conveys nothing on its own. */
    internal fun bufferSpoken(minutesLeft: Long, promiseHours: Int): String =
        "${durationPhrase(minutesLeft)} of buffer left today, out of $promiseHours hours"

    internal fun coinsSpoken(days: Int): String =
        "${(days - 1).coerceAtLeast(0)} days banked, today almost banked"

    /**
     * The week as one quiet sentence, already worked out for the reader: how many days of the
     * quota are banked and how many are still needed. Never how many days remain as well —
     * that is a third number doing no work.
     */
    internal fun weekQuotaLine(streak: PromiseStreak.State): String {
        val needed = AppConstants.PROMISE_DAYS_TO_UNLOCK
        val banked = streak.daysKeptThisWeek.coerceAtMost(needed)
        val toGo = (needed - banked).coerceAtLeast(0)
        return when {
            toGo == 0 -> "This week: $banked of $needed banked \u00B7 quota met"
            else -> "This week: $banked of $needed banked \u00B7 $toGo to go"
        }
    }

    /** "45 minutes" / "1 hr 05 mins" / "no time" — DepartureMono renders the digits. */
    internal fun durationPhrase(minutes: Long): String {
        if (minutes <= 0) return "no time"
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours == 0L -> "$mins ${if (mins == 1L) "minute" else "minutes"}"
            mins == 0L -> "$hours ${if (hours == 1L) "hour" else "hours"}"
            else -> "${hours}h ${mins}m"
        }
    }
}
