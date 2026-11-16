package com.zenlauncher.zenmode.week

import com.zenlauncher.zenmode.coreapi.ZenScore
import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.coreapi.week.ScoredWeek
import com.zenlauncher.zenmode.coreapi.week.WeekMoment
import com.zenlauncher.zenmode.coreapi.week.WeeklyStreak
import com.zenlauncher.zenmode.recap.DayRecord
import com.zenlauncher.zenmode.recap.weekStartOf
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The week as the weekly streak reads it, put together from what this phone has on record.
 *
 * A day's Zen Score comes from two places:
 *  · **measured**: the last score [com.zenlauncher.zenmode.ZenScoreStore] computed that day,
 *    distracted sessions included. For a finished day it may be from before the day ended.
 *  · **estimated**: the recap's sealed full-day minutes ([DayRecord]), scored with no
 *    distracted sessions (their per-day log is gone by then). This is the one-time backfill
 *    for days before the score was kept, labelled an estimate here.
 *
 * Usage only grows and distractions only cost, so the true final score is at or below both.
 * A finished day therefore takes the lower of the two it has: the streak never claims a good
 * day the phone can't stand behind. A day with neither is no data, never a zero.
 *
 * Pure: callers read the stores and pass their contents in, so this is unit-tested without a
 * Context (see [com.zenlauncher.zenmode.widget.WidgetData] for the Android side).
 */
object WeekReader {

    /** Everything the widgets need about the week. */
    data class State(
        val today: LocalDate,
        val week: ScoredWeek,
        val lastWeek: ScoredWeek?,
        val streak: WeeklyStreak,
        val moment: WeekMoment
    )

    /** Finished weeks the streak looks back over: the recap's sixteen. */
    private const val LOOKBACK_WEEKS = 16L

    fun read(
        measured: Map<LocalDate, Int>,
        recorded: Map<LocalDate, DayRecord>,
        today: LocalDate,
        todayScore: Int?,
        part: DayPart
    ): State {
        val monday = weekStartOf(today)
        val todayIndex = ChronoUnit.DAYS.between(monday, today).toInt()
        val week = ScoredWeek(
            scores = (0 until ScoredWeek.DAYS).map { i ->
                val date = monday.plusDays(i.toLong())
                when {
                    date == today -> todayScore
                    date < today -> finishedDayScore(date, measured, recorded)
                    else -> null
                }
            },
            todayIndex = todayIndex
        )

        // Oldest first, starting at the first week anything was recorded: weeks before the
        // phone knew anything aren't lost weeks, they simply aren't part of the run.
        val earliest = (measured.keys + recorded.keys).minOrNull()
        val finished = if (earliest == null) emptyList() else {
            val first = maxOf(weekStartOf(earliest), monday.minusWeeks(LOOKBACK_WEEKS))
            generateSequence(first) { it.plusWeeks(1) }
                .takeWhile { it < monday }
                .map { start -> finishedWeek(start, measured, recorded) }
                .toList()
        }
        val lastWeek = finished.lastOrNull()?.takeIf { it.anyData }
        val streak = WeeklyStreak.compute(finished, week)
        val moment = WeekMoment.read(week, streak, todayScore, lastWeek?.isWon, part)
        return State(today, week, lastWeek, streak, moment)
    }

    /** A finished day: the lower of the measured score and the full-day estimate, whichever exist. */
    fun finishedDayScore(date: LocalDate, measured: Map<LocalDate, Int>, recorded: Map<LocalDate, DayRecord>): Int? {
        val estimate = recorded[date]?.let(::estimate)
        val live = measured[date]
        return when {
            live != null && estimate != null -> minOf(live, estimate)
            else -> live ?: estimate
        }
    }

    /** A sealed day scored from its minutes alone. An estimate: the day's distractions are gone. */
    fun estimate(day: DayRecord): Int =
        ZenScore.compute(
            screenTimeMillis = day.screenTimeMinutes * MILLIS_PER_MINUTE,
            distractedSessions = 0,
            promiseHours = day.promiseHours
        )

    private fun finishedWeek(start: LocalDate, measured: Map<LocalDate, Int>, recorded: Map<LocalDate, DayRecord>) =
        ScoredWeek(
            scores = (0 until ScoredWeek.DAYS).map { finishedDayScore(start.plusDays(it.toLong()), measured, recorded) },
            todayIndex = ScoredWeek.LAST_DAY
        )

    private const val MILLIS_PER_MINUTE = 60_000L
}
