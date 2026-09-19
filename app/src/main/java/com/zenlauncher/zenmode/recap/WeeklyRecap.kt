package com.zenlauncher.zenmode.recap

import com.zenlauncher.zenmode.AppConstants
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * One day's usage as recorded on the device. Stored per day so a week can be rebuilt
 * after Android's own usage history (≈ 7–10 days) has rolled off.
 */
data class DayRecord(
    val date: LocalDate,
    val screenTimeMinutes: Long,
    /** The daily promise in force that day, in hours. */
    val promiseHours: Int,
    /** Foreground minutes per app, most-used first; ZenMode itself excluded. */
    val appMinutes: List<AppMinutes>,
    /** Minutes used between 22:00 and 04:00 — the usual doomscroll window. */
    val lateNightMinutes: Long,
    /** Unlocks that day, or null when the device doesn't expose them. */
    val pickups: Int?
) {
    val keptPromise: Boolean get() = screenTimeMinutes <= promiseHours * 60L
}

data class AppMinutes(val packageName: String, val label: String, val minutes: Long)

enum class RecapOutcome(val analyticsKey: String) { KEPT("kept"), MISSED("missed") }

/** A finished Monday–Sunday week, as the recap and the PDF report show it. */
data class WeeklyRecap(
    val weekStart: LocalDate,
    val days: List<DayRecord>,
    val previousWeekTotalMinutes: Long?,
    val daysToUnlock: Int = AppConstants.PROMISE_DAYS_TO_UNLOCK
) {
    val weekEnd: LocalDate get() = weekStart.plusDays(6)
    val totalMinutes: Long get() = days.sumOf { it.screenTimeMinutes }
    val dailyAverageMinutes: Long get() = if (days.isEmpty()) 0 else totalMinutes / days.size
    val daysKept: Int get() = days.count { it.keptPromise }
    val outcome: RecapOutcome get() = if (daysKept >= daysToUnlock) RecapOutcome.KEPT else RecapOutcome.MISSED
    val promiseHours: Int get() = days.lastOrNull()?.promiseHours ?: AppConstants.PLACEHOLDER_PROMISE_HOURS

    /** Positive when this week was lighter than last — minutes won back. */
    val minutesReclaimed: Long? get() = previousWeekTotalMinutes?.let { it - totalMinutes }

    val calmestDay: DayRecord? get() = days.minByOrNull { it.screenTimeMinutes }
    val loudestDay: DayRecord? get() = days.maxByOrNull { it.screenTimeMinutes }

    /** Apps summed across the week, most-used first. */
    val topApps: List<AppMinutes>
        get() = days.flatMap { it.appMinutes }
            .groupBy { it.packageName }
            .map { (pkg, entries) -> AppMinutes(pkg, entries.first().label, entries.sumOf { it.minutes }) }
            .sortedByDescending { it.minutes }

    val missedDays: List<DayRecord> get() = days.filterNot { it.keptPromise }
    val lateNightMinutes: Long get() = days.sumOf { it.lateNightMinutes }
    val pickups: Int? get() = days.mapNotNull { it.pickups }.takeIf { it.size == days.size }?.sum()

    /** How far past the promise the missed days went, in total. */
    val minutesOverPromise: Long
        get() = missedDays.sumOf { it.screenTimeMinutes - it.promiseHours * 60L }
}

fun DayRecord.shortDayName(locale: Locale = Locale.getDefault()): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)

fun DayRecord.fullDayName(locale: Locale = Locale.getDefault()): String =
    date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)

/** Monday of the week containing [date]; ZenMode weeks run Monday–Sunday. */
fun weekStartOf(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

/** "Sep 8 – 14", or "Sep 29 – Oct 5" across a month boundary. */
fun WeeklyRecap.rangeLabel(locale: Locale = Locale.getDefault()): String {
    val monthDay = DateTimeFormatter.ofPattern("MMM d", locale)
    val end = if (weekEnd.month == weekStart.month) DateTimeFormatter.ofPattern("d", locale) else monthDay
    return "${weekStart.format(monthDay)} – ${weekEnd.format(end)}"
}

/** "2h 05m", "45m" — the format every recap surface uses. */
fun formatMinutes(minutes: Long): String {
    val m = minutes.coerceAtLeast(0)
    return if (m >= 60) "${m / 60}h ${"%02d".format(m % 60)}m" else "${m}m"
}
