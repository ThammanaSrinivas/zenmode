package com.zenlauncher.zenmode

enum class MoodState {
    HAPPY,
    NEUTRAL,
    ANNOYED
}

object AppLogic {
    fun getMoodState(minutes: Long): MoodState {
        val percentage = getMindfulnessPercentage(minutes)
        return when {
             percentage >= AppConstants.MINDFULNESS_HAPPY_MIN_PERCENT -> MoodState.HAPPY
             percentage >= AppConstants.MINDFULNESS_NEUTRAL_MIN_PERCENT -> MoodState.NEUTRAL
             else -> MoodState.ANNOYED
        }
    }

    fun getMindfulnessPercentage(minutes: Long): Int {
        // Starts at 100%, depleted by max neutral threshold
        val maxMinutes = AppConstants.THRESHOLD_NEUTRAL_MINUTES
        val percentage = ((maxMinutes - minutes).toFloat() / maxMinutes * 100).toInt()
        return percentage.coerceIn(0, 100)
    }

    fun getMindfulnessColor(minutes: Long): Int {
        val percentage = getMindfulnessPercentage(minutes)
        return when {
             percentage >= AppConstants.MINDFULNESS_HAPPY_MIN_PERCENT -> R.color.zen_mindfulness_happy
             percentage >= AppConstants.MINDFULNESS_NEUTRAL_MIN_PERCENT -> R.color.zen_mindfulness_neutral
             else -> R.color.zen_mindfulness_annoyed
        }
    }
}
