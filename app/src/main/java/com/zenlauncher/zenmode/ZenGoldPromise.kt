package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.recap.DayRecord
import com.zenlauncher.zenmode.recap.WeeklyRecap
import com.zenlauncher.zenmode.recap.weekStartOf
import java.time.LocalDate

enum class GoldPromisePeriod { WEEKLY, MONTHLY }

/** One bar on the Zen Gold promise strip: a day (Weekly) or a week (Monthly).
 * [kept] is true/false once that unit is decided, null while it's still open —
 * a day that hasn't arrived yet, or (Monthly only) a week still in progress. */
data class PromiseUnit(val label: String, val kept: Boolean?)

data class ZenGoldPromiseState(
    val period: GoldPromisePeriod = GoldPromisePeriod.WEEKLY,
    val promiseHours: Int = AppConstants.PLACEHOLDER_PROMISE_HOURS,
    val dailyAverageMinutes: Int = 0,
    val units: List<PromiseUnit> = emptyList(),
    val unitsKept: Int = 0,
    val unitsMissed: Int = 0,
    /** Units still open — future days this week, or weeks not yet decided this month. */
    val unitsRemaining: Int = 0,
    val unlocked: Boolean = false
)

/**
 * Real Weekly/Monthly promise tracking for the Zen Gold "My Screen time" card, built from the
 * same on-device history the recap/streak surfaces already use — RecapStore's per-day records
 * plus today's live usage from UsageRepository. No separate Gold Streak backend exists yet
 * (see zenmode_core_private/docs/plans), so this is the real, on-device source of truth:
 * nothing here is a placeholder.
 *
 * Pure functions over plain data (no Context/RecapStore/UsageRepository dependency), so they're
 * unit-testable the same way [com.zenlauncher.zenmode.recap.RecapStory] is — callers (see
 * ZenGoldActivity) read RecapStore/UsageRepository themselves and pass the results in.
 *
 * Gold pay's actual unlock gate is always the WEEKLY result (it's a Monday–Sunday cash-flow
 * cycle — see GoldUnlockDisclaimer's "Sunday midnight" copy) regardless of which tab the card
 * is showing; callers should read [weekly]'s `unlocked`/`unitsKept` for that, never [monthly]'s.
 */
object ZenGoldPromise {

    private const val WEEK_LENGTH = 7
    private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

    /**
     * @param days finished-day history, as [com.zenlauncher.zenmode.recap.RecapStore.days] returns it.
     * @param todayMinutes today's live screen time in minutes, e.g. from
     *   `UsageRepository.getTodayUsage().screenTimeInMillis / 60_000L`.
     */
    fun weekly(
        days: Map<LocalDate, DayRecord>,
        todayMinutes: Long,
        promiseHours: Int,
        today: LocalDate = LocalDate.now()
    ): ZenGoldPromiseState {
        val weekStart = weekStartOf(today)

        var minutesSoFar = 0L
        var daysElapsed = 0
        val units = (0 until WEEK_LENGTH).map { offset ->
            val date = weekStart.plusDays(offset.toLong())
            val kept: Boolean? = when {
                date.isAfter(today) -> null
                date.isEqual(today) -> {
                    daysElapsed++
                    minutesSoFar += todayMinutes
                    todayMinutes <= promiseHours * 60L
                }
                else -> days[date]?.also {
                    daysElapsed++
                    minutesSoFar += it.screenTimeMinutes
                }?.keptPromise
            }
            PromiseUnit(DAY_LABELS[offset], kept)
        }

        return buildState(GoldPromisePeriod.WEEKLY, promiseHours, units, minutesSoFar, daysElapsed)
    }

    /**
     * A rolling calendar-month view: one bar per Monday-start week that overlaps the current
     * month (usually 4, sometimes 5). Fully finished weeks reuse [recapFor]'s real
     * daysKept/outcome; the week containing today is judged live — already-kept if it's hit
     * the unlock threshold, already-missed if the remaining days can no longer reach it,
     * otherwise still open (null, drawn the same as a future day/week).
     *
     * @param recapFor a finished week's [WeeklyRecap], e.g. `RecapStore::recap` — null when the
     *   week isn't fully on record (too old, or before install).
     */
    fun monthly(
        days: Map<LocalDate, DayRecord>,
        todayMinutes: Long,
        promiseHours: Int,
        recapFor: (LocalDate) -> WeeklyRecap?,
        today: LocalDate = LocalDate.now()
    ): ZenGoldPromiseState {
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        val currentWeekStart = weekStartOf(today)

        val weekStarts = generateSequence(weekStartOf(monthStart)) { it.plusWeeks(1) }
            .takeWhile { !it.isAfter(monthEnd) }
            .toList()

        val units = weekStarts.mapIndexed { index, weekStart ->
            val kept = weekOutcome(weekStart, currentWeekStart, today, promiseHours, days, todayMinutes, recapFor)
            PromiseUnit("W-%02d".format(index + 1), kept)
        }

        var minutesSoFar = 0L
        var daysElapsed = 0
        var cursor = monthStart
        while (!cursor.isAfter(today)) {
            if (cursor.isEqual(today)) {
                minutesSoFar += todayMinutes
                daysElapsed++
            } else {
                days[cursor]?.let {
                    minutesSoFar += it.screenTimeMinutes
                    daysElapsed++
                }
            }
            cursor = cursor.plusDays(1)
        }

        return buildState(GoldPromisePeriod.MONTHLY, promiseHours, units, minutesSoFar, daysElapsed)
    }

    private fun weekOutcome(
        weekStart: LocalDate,
        currentWeekStart: LocalDate,
        today: LocalDate,
        promiseHours: Int,
        days: Map<LocalDate, DayRecord>,
        todayMinutes: Long,
        recapFor: (LocalDate) -> WeeklyRecap?
    ): Boolean? = when {
        weekStart.isAfter(currentWeekStart) -> null
        weekStart.isEqual(currentWeekStart) -> {
            var kept = 0
            var decided = 0
            for (offset in 0 until WEEK_LENGTH) {
                val date = weekStart.plusDays(offset.toLong())
                if (date.isAfter(today)) continue
                decided++
                val dayKept = if (date.isEqual(today)) todayMinutes <= promiseHours * 60L else days[date]?.keptPromise
                if (dayKept == true) kept++
            }
            when {
                kept >= AppConstants.PROMISE_DAYS_TO_UNLOCK -> true
                kept + (WEEK_LENGTH - decided) < AppConstants.PROMISE_DAYS_TO_UNLOCK -> false
                else -> null
            }
        }
        // A finished week with no full record (e.g. before install) is unproven either way,
        // not a broken promise — leave it open rather than guessing.
        else -> recapFor(weekStart)?.let { it.daysKept >= AppConstants.PROMISE_DAYS_TO_UNLOCK }
    }

    private fun buildState(
        period: GoldPromisePeriod,
        promiseHours: Int,
        units: List<PromiseUnit>,
        minutesSoFar: Long,
        daysElapsed: Int
    ): ZenGoldPromiseState {
        val kept = units.count { it.kept == true }
        val missed = units.count { it.kept == false }
        val remaining = units.count { it.kept == null }
        val unlocked = when (period) {
            GoldPromisePeriod.WEEKLY -> kept >= AppConstants.PROMISE_DAYS_TO_UNLOCK
            // No weekly-style partial threshold exists at month scale yet, so Monthly reads
            // as a clean streak: every week decided so far has to have been kept.
            GoldPromisePeriod.MONTHLY -> missed == 0 && kept > 0
        }
        return ZenGoldPromiseState(
            period = period,
            promiseHours = promiseHours,
            dailyAverageMinutes = if (daysElapsed > 0) (minutesSoFar / daysElapsed).toInt() else 0,
            units = units,
            unitsKept = kept,
            unitsMissed = missed,
            unitsRemaining = remaining,
            unlocked = unlocked
        )
    }
}
