package com.zenlauncher.zenmode

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

    /**
     * Zen score (0-100, shown as x.y out of 10 in Circle UI) — delegates to
     * [com.zenlauncher.zenmode.coreapi.ZenScoreCalculator], which is where the real formula
     * lives (core-api, so StatSyncWorker in core-private can call it too; app can't be a
     * dependency of core-private). Deliberately not routed through [getMindfulnessPercentage]
     * even though the math is currently identical — this way "zen score" has its own single
     * source of truth from day one instead of two formulas that happen to agree for now.
     */
    fun calculateZenScore(minutes: Long): Int =
        com.zenlauncher.zenmode.coreapi.ZenScoreCalculator.calculateZenScore(minutes)

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

    /**
     * Count streak backwards from today: HAPPY/NEUTRAL count, ANNOYED breaks.
     * @param weeklyScreenTimeMillis 7-element list where last item is today.
     */
    fun getStreakCount(weeklyScreenTimeMillis: List<Long>): Int {
        var count = 0
        for (i in weeklyScreenTimeMillis.indices.reversed()) {
            val minutes = (weeklyScreenTimeMillis[i] / 1000) / 60
            if (getMoodState(minutes) == MoodState.ANNOYED) break
            count++
        }
        return count
    }
}
