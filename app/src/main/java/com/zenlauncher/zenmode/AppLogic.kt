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
}
