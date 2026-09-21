package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.coreapi.ZenScore
import com.zenlauncher.zenmode.recap.DayRecord
import com.zenlauncher.zenmode.recap.RecapStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class MoodState {
    HAPPY,
    NEUTRAL,
    ANNOYED
}

object AppLogic {
    fun getMoodState(minutes: Long): MoodState {
        return when {
             minutes <= AppConstants.THRESHOLD_HAPPY_MINUTES -> MoodState.HAPPY
             minutes <= AppConstants.THRESHOLD_NEUTRAL_MINUTES -> MoodState.NEUTRAL
             else -> MoodState.ANNOYED
        }
    }

    fun getMindfulnessPercentage(minutes: Long): Int {
        // Starts at 100%, depleted by annoyed threshold (3h30m)
        val maxMinutes = AppConstants.THRESHOLD_NEUTRAL_MINUTES
        val percentage = ((maxMinutes - minutes).toFloat() / maxMinutes * 100).toInt()
        return percentage.coerceIn(0, 100)
    }

    // The real Zen Score formula lives in com.zenlauncher.zenmode.coreapi.ZenScore (core-api,
    // so core-private's StatSyncWorker can call it too) — see ZenScoreStore for the live,
    // per-day-cached value everything on screen actually reads. This object used to carry its
    // own now-dead calculateZenScore() wrapper around the old, screen-time-only formula
    // (ZenScoreCalculator, retired) with no callers left; removed rather than adapted.

    fun getMindfulnessColor(minutes: Long): Int {
        return when {
             minutes <= AppConstants.THRESHOLD_HAPPY_MINUTES -> R.color.zen_mindfulness_happy
             minutes <= AppConstants.THRESHOLD_NEUTRAL_MINUTES -> R.color.zen_mindfulness_neutral
             else -> R.color.zen_mindfulness_annoyed
        }
    }

    // Weekly variants — thresholds are 7× the daily ones
    fun getWeeklyMoodState(totalWeeklyMinutes: Long): MoodState {
        return when {
            totalWeeklyMinutes <= 7 * AppConstants.THRESHOLD_HAPPY_MINUTES -> MoodState.HAPPY
            totalWeeklyMinutes <= 7 * AppConstants.THRESHOLD_NEUTRAL_MINUTES -> MoodState.NEUTRAL
            else -> MoodState.ANNOYED
        }
    }

    fun getWeeklyMindfulnessPercentage(totalWeeklyMinutes: Long): Int {
        val maxMinutes = 7L * AppConstants.THRESHOLD_NEUTRAL_MINUTES
        val percentage = ((maxMinutes - totalWeeklyMinutes).toFloat() / maxMinutes * 100).toInt()
        return percentage.coerceIn(0, 100)
    }

    // ── Streaks, driven by Zen Score ─────────────────────────────────
    // The streak flame, "total mindful days" and "longest streak" (Home's streak milestone
    // overlay) used to be either a separate fixed-screen-time check (the old getStreakCount)
    // or flat AppConstants placeholders. Now all three read the same "mindful day" definition
    // the new ZenScore formula drives, using RecapStore's real per-day history (up to
    // RecapStore's retention window back) instead of invented numbers.
    //
    // Historical days have no stored session-quality data (SessionLogRepository is today-only
    // by design), so they're scored with a neutral 100 — the same default the live formula
    // already falls back to on a usage-free day. An approximation, not a claim that every past
    // day was distraction-free; today's own check should use the real live score instead
    // (see ZenScoreStore) wherever it's available.

    fun isMindfulDay(screenTimeMinutes: Long, promiseHours: Int, sessionQualityPercent: Int = 100): Boolean =
        ZenScore.compute(screenTimeMinutes * 60_000L, sessionQualityPercent, promiseHours) >=
            AppConstants.MINDFUL_DAY_ZEN_SCORE_THRESHOLD * 10

    private fun isMindfulDay(day: DayRecord) = isMindfulDay(day.screenTimeMinutes, day.promiseHours)

    /** Current streak, counting back from yesterday while [todayIsMindful]. */
    fun getStreakCount(recapStore: RecapStore, todayIsMindful: Boolean, today: LocalDate = LocalDate.now()): Int {
        if (!todayIsMindful) return 0
        val days = recapStore.days()
        var count = 1
        var cursor = today.minusDays(1)
        while (days[cursor]?.let(::isMindfulDay) == true) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    /** Every mindful day on record, [todayIsMindful] included — bounded by RecapStore's retention window. */
    fun getTotalMindfulDays(recapStore: RecapStore, todayIsMindful: Boolean): Int =
        recapStore.days().values.count(::isMindfulDay) + if (todayIsMindful) 1 else 0

    data class StreakRange(val days: Int, val start: LocalDate, val end: LocalDate)

    /** The longest run of consecutive mindful calendar days on record, [todayIsMindful] included. */
    fun getLongestStreak(recapStore: RecapStore, todayIsMindful: Boolean, today: LocalDate = LocalDate.now()): StreakRange? {
        val mindfulDates = (recapStore.days().values.filter(::isMindfulDay).map { it.date } +
            listOfNotNull(today.takeIf { todayIsMindful })).sorted()

        var best: StreakRange? = null
        var runStart: LocalDate? = null
        var previous: LocalDate? = null
        for (date in mindfulDates) {
            if (previous == null || date != previous.plusDays(1)) runStart = date
            val length = ChronoUnit.DAYS.between(runStart, date).toInt() + 1
            if (best == null || length > best!!.days) best = StreakRange(length, runStart!!, date)
            previous = date
        }
        return best
    }

    /** "JUL 31–SEP 12", matching the streak milestone card's existing label style. */
    fun formatStreakRange(range: StreakRange, locale: Locale = Locale.getDefault()): String {
        val formatter = DateTimeFormatter.ofPattern("MMM d", locale)
        fun format(date: LocalDate) = date.format(formatter).uppercase(locale)
        return "${format(range.start)}–${format(range.end)}"
    }
}
