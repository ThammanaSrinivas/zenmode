package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.recap.DayRecord
import com.zenlauncher.zenmode.recap.weekStartOf
import java.time.LocalDate

/**
 * The streak ZenMode OS shows on Home, in the resistance screen and in the daily check-in
 * overlay: **days kept, judged by the week**.
 *
 * The rule, and why it differs from a classic day-by-day streak:
 *
 *  · A single day over the promise does **not** break the streak. It simply doesn't add a day
 *    to it — the number holds rather than falling to zero.
 *  · The streak breaks only when a **finished week** was lost, i.e. fewer than
 *    [AppConstants.PROMISE_DAYS_TO_UNLOCK] of its 7 days were kept. That's the same
 *    Monday–Sunday verdict gold pay already runs on (see [ZenGoldPromise] and
 *    [com.zenlauncher.zenmode.recap.WeeklyRecap.outcome]), so a user can never be told the
 *    week was won and the streak was lost in the same breath.
 *
 * So one bad Tuesday costs nothing; a bad *week* starts you over. That's the promise ZenMode
 * OS makes to the user, and it's deliberately more forgiving than a day-by-day run. It is the
 * only streak in the app: Home's flame, the check-in card and the streak share card
 * (share/StreakShare.kt) all read this one.
 *
 * Pure functions over plain data, like [ZenGoldPromise] — callers read RecapStore /
 * UsageRepository themselves and pass the results in, so this is unit-testable without a
 * Context.
 */
object PromiseStreak {

    private const val WEEK_LENGTH = 7

    /**
     * @param days how many days have been kept inside the current unbroken run — the number
     *   shown next to the flame. Zero when nothing has been kept yet.
     * @param weeksKept finished Monday–Sunday weeks won back-to-back, newest first. The
     *   "real" unit of the streak: this is what a bad day can't touch.
     * @param startDate the Monday the run began, or null when there is no run yet. Used for
     *   the milestone card's date range, which must not be guessed from [days] — kept days
     *   inside a run aren't contiguous.
     * @param daysKeptThisWeek / [daysLeftThisWeek] this week's progress toward the 5-of-7
     *   verdict, for the check-in overlay's "you still have N days" line.
     * @param weekAtRisk this week can no longer reach 5 of 7, so the run *will* break at
     *   Sunday midnight. Nothing has broken yet — that's the point of the weekly rule — but
     *   the check-in overlay says so plainly rather than cheering a lost week.
     */
    data class State(
        val days: Int = 0,
        val weeksKept: Int = 0,
        val startDate: LocalDate? = null,
        val daysKeptThisWeek: Int = 0,
        val daysLeftThisWeek: Int = 0,
        val weekAtRisk: Boolean = false
    ) {
        /** Nothing kept yet — the overlay opens with an invitation rather than a number. */
        val isEmpty: Boolean get() = days == 0
    }

    /**
     * @param days finished-day history, as [com.zenlauncher.zenmode.recap.RecapStore.days] returns it.
     * @param todayKept whether today is **currently** under the promise. Unlike gold pay —
     *   which never unlocks on a day that can still be lost — the streak counts today while
     *   it's still being kept, so the flame reflects the day the user is actually having.
     */
    fun of(
        days: Map<LocalDate, DayRecord>,
        todayKept: Boolean,
        today: LocalDate = LocalDate.now()
    ): State {
        val thisWeekStart = weekStartOf(today)
        val runStart = runStartOf(days, thisWeekStart)
        val weeksKept = ChronoWeeks.between(runStart, thisWeekStart)

        // Kept days inside the run, today included while it's still under the promise. A day
        // with no record (before install, usage access off) is unproven and never counted.
        var keptDays = 0
        var cursor = runStart
        while (cursor.isBefore(today)) {
            if (days[cursor]?.keptPromise == true) keptDays++
            cursor = cursor.plusDays(1)
        }
        if (todayKept) keptDays++

        val keptThisWeek = (0 until WEEK_LENGTH)
            .map { thisWeekStart.plusDays(it.toLong()) }
            .count { date ->
                if (date.isEqual(today)) todayKept else days[date]?.keptPromise == true
            }
        // Today can still be won when it's under the promise right now, so it counts as open
        // alongside the days after it.
        val openThisWeek = (0 until WEEK_LENGTH)
            .map { thisWeekStart.plusDays(it.toLong()) }
            .count { date -> date.isAfter(today) || (date.isEqual(today) && todayKept) }

        return State(
            days = keptDays,
            weeksKept = weeksKept,
            startDate = runStart.takeIf { keptDays > 0 },
            daysKeptThisWeek = keptThisWeek,
            daysLeftThisWeek = openThisWeek,
            weekAtRisk = keptThisWeek + openThisWeek < AppConstants.PROMISE_DAYS_TO_UNLOCK
        )
    }

    /**
     * The Monday the current run began: walk back week by week while each **finished** week
     * was won, and stop at the first that wasn't.
     *
     * A finished week that isn't fully on record stops the walk too. It isn't counted as
     * broken — we simply can't prove it was kept, and the streak never claims a week it
     * can't show (the same reasoning as [ZenGoldPromise]'s monthly bars leaving an
     * unrecorded week undecided).
     */
    private fun runStartOf(days: Map<LocalDate, DayRecord>, thisWeekStart: LocalDate): LocalDate {
        var runStart = thisWeekStart
        var candidate = thisWeekStart.minusWeeks(1)
        while (weekWasKept(days, candidate)) {
            runStart = candidate
            candidate = candidate.minusWeeks(1)
        }
        return runStart
    }

    /** A finished week with all 7 days on record and at least 5 of them kept. */
    private fun weekWasKept(days: Map<LocalDate, DayRecord>, weekStart: LocalDate): Boolean {
        val week = (0 until WEEK_LENGTH).mapNotNull { days[weekStart.plusDays(it.toLong())] }
        if (week.size < WEEK_LENGTH) return false
        return week.count { it.keptPromise } >= AppConstants.PROMISE_DAYS_TO_UNLOCK
    }

    /** Whole weeks between two Mondays. */
    private object ChronoWeeks {
        fun between(from: LocalDate, to: LocalDate): Int =
            (java.time.temporal.ChronoUnit.DAYS.between(from, to) / WEEK_LENGTH).toInt()
    }
}
