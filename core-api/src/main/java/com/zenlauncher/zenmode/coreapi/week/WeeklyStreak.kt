package com.zenlauncher.zenmode.coreapi.week

/**
 * The one streak ZenMode OS shows anywhere: **won weeks in a row**.
 *
 * The rule (CLAUDE.md "Product rules", identical on every platform; the
 * iPhone app's rule is the reference this was ported from):
 *
 *  · A *good day* is a day whose Zen Score is above 6.0. Android keeps the score in whole
 *    tenths (25–100, see [com.zenlauncher.zenmode.coreapi.ZenScore]), so that is `> 60`.
 *  · A *won week* is [GOOD_DAYS_TO_WIN] or more good days, Monday to Sunday, won the moment
 *    the 4th good day lands.
 *  · The streak counts won weeks in a row. The week in progress adds one once it's won and
 *    never breaks it; a finished week with 3 or fewer good days resets it to 0.
 *  · A day with no data isn't a good day, and it doesn't count as a zero.
 *
 * Pure Kotlin, no `android.*`, so the desktop app can compile it as it is.
 */
data class WeeklyStreak(
    /** Won weeks in a row, the current week included once it's won. */
    val weeks: Int,
    /** Good days so far this week. */
    val goodDaysThisWeek: Int
) {
    val isWonThisWeek: Boolean get() = goodDaysThisWeek >= GOOD_DAYS_TO_WIN

    companion object {
        /** A good day is a Zen Score strictly above this, in tenths (6.0). */
        const val GOOD_DAY_ABOVE_TENTHS = 60

        /** Good days that win a week. */
        const val GOOD_DAYS_TO_WIN = 4

        /** No data is never a good day. */
        fun isGood(scoreTenths: Int?): Boolean = scoreTenths != null && scoreTenths > GOOD_DAY_ABOVE_TENTHS

        /**
         * Folds [finishedWeeks] (oldest first) and the week in progress into the streak.
         * A finished week that isn't won resets the run; the current week only ever adds.
         */
        fun compute(finishedWeeks: List<ScoredWeek>, current: ScoredWeek): WeeklyStreak {
            var run = 0
            for (week in finishedWeeks) run = if (week.isWon) run + 1 else 0
            if (current.isWon) run += 1
            return WeeklyStreak(weeks = run, goodDaysThisWeek = current.goodDays)
        }
    }
}

/**
 * One Monday–Sunday week as the streak reads it: each day's Zen Score in whole tenths, or null
 * when that day has no data (still to come, before install, or usage access off).
 */
data class ScoredWeek(
    /** Seven entries, Monday first. Shorter lists are padded with no-data days. */
    val scores: List<Int?>,
    /** Index of today (0 = Monday). A finished week passes 6. */
    val todayIndex: Int = LAST_DAY
) {
    val days: List<Int?> = (scores + List(DAYS) { null }).take(DAYS)
    val today: Int = todayIndex.coerceIn(0, LAST_DAY)
    val goodDays: Int get() = days.count(WeeklyStreak::isGood)
    val isWon: Boolean get() = goodDays >= WeeklyStreak.GOOD_DAYS_TO_WIN
    val todayIsGood: Boolean get() = WeeklyStreak.isGood(days[today])
    val anyData: Boolean get() = days.any { it != null }

    companion object {
        const val DAYS = 7
        const val LAST_DAY = DAYS - 1
    }
}
