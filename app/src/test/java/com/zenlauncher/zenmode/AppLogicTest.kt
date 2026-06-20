package com.zenlauncher.zenmode

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLogicTest {

    @Test
    fun `getMoodState returns HAPPY at or below happy threshold`() {
        // <= THRESHOLD_HAPPY_MINUTES (120) → HAPPY
        assertEquals(MoodState.HAPPY, AppLogic.getMoodState(30L))
        assertEquals(MoodState.HAPPY, AppLogic.getMoodState(AppConstants.THRESHOLD_HAPPY_MINUTES.toLong()))
    }

    @Test
    fun `getMoodState returns NEUTRAL between happy and neutral thresholds`() {
        // (120, 210] → NEUTRAL
        val moderate = (AppConstants.THRESHOLD_HAPPY_MINUTES + 1).toLong()
        assertEquals(MoodState.NEUTRAL, AppLogic.getMoodState(moderate))
        assertEquals(MoodState.NEUTRAL, AppLogic.getMoodState(AppConstants.THRESHOLD_NEUTRAL_MINUTES.toLong()))
    }

    @Test
    fun `getMoodState returns ANNOYED above neutral threshold`() {
        val aboveNeutral = (AppConstants.THRESHOLD_NEUTRAL_MINUTES + 1).toLong()
        assertEquals(MoodState.ANNOYED, AppLogic.getMoodState(aboveNeutral))
    }

    @Test
    fun `getMindfulnessPercentage calcultes correctly`() {
        // Depletes linearly from 100% at 0 min to 0% at THRESHOLD_NEUTRAL_MINUTES (210).
        assertEquals(100, AppLogic.getMindfulnessPercentage(0L))

        // Half the neutral threshold (105 min) -> 50%
        assertEquals(50, AppLogic.getMindfulnessPercentage((AppConstants.THRESHOLD_NEUTRAL_MINUTES / 2).toLong()))

        // At neutral threshold -> 0%
        assertEquals(0, AppLogic.getMindfulnessPercentage(AppConstants.THRESHOLD_NEUTRAL_MINUTES.toLong()))

        // Beyond neutral threshold -> clamped to 0%
        assertEquals(0, AppLogic.getMindfulnessPercentage((AppConstants.THRESHOLD_NEUTRAL_MINUTES + 40).toLong()))
    }

    @Test
    fun `getMindfulnessColor returns correct color resource`() {
        // <= happy threshold -> happy color
        assertEquals(R.color.zen_mindfulness_happy, AppLogic.getMindfulnessColor(0L))

        // between thresholds -> neutral color
        assertEquals(
            R.color.zen_mindfulness_neutral,
            AppLogic.getMindfulnessColor((AppConstants.THRESHOLD_HAPPY_MINUTES + 1).toLong())
        )

        // above neutral threshold -> annoyed color
        assertEquals(
            R.color.zen_mindfulness_annoyed,
            AppLogic.getMindfulnessColor((AppConstants.THRESHOLD_NEUTRAL_MINUTES + 1).toLong())
        )
    }
}
