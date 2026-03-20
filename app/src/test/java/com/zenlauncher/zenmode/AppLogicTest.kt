package com.zenlauncher.zenmode

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLogicTest {

    @Test
    fun `getMoodState returns HAPPY when minutes are low (high mindfulness)`() {
        // 30 mins → percentage = (120-30)/120*100 = 75% → >= 70% HAPPY threshold
        val mood = AppLogic.getMoodState(30L)
        assertEquals(MoodState.HAPPY, mood)
    }

    @Test
    fun `getMoodState returns NEUTRAL when minutes are moderate`() {
        // 60 mins → percentage = (120-60)/120*100 = 50% → >= 40% NEUTRAL but < 70% HAPPY
        val mood = AppLogic.getMoodState(60L)
        assertEquals(MoodState.NEUTRAL, mood)
    }

    @Test
    fun `getMoodState returns ANNOYED when minutes above neutral threshold`() {
        val mood = AppLogic.getMoodState(AppConstants.THRESHOLD_NEUTRAL_MINUTES.toLong())
        assertEquals(MoodState.ANNOYED, mood)
    }

    @Test
    fun `getMindfulnessPercentage calcultes correctly`() {
        // 0 minutes -> 100%
        assertEquals(100, AppLogic.getMindfulnessPercentage(0L))
        
        // 60 minutes -> 50%
        assertEquals(50, AppLogic.getMindfulnessPercentage(60L))
        
        // 120 minutes -> 0%
        assertEquals(0, AppLogic.getMindfulnessPercentage(120L))
        
        // > 120 minutes -> 0%
        assertEquals(0, AppLogic.getMindfulnessPercentage(150L))
    }

    @Test
    fun `getMindfulnessColor returns correct color resource`() {
        // High percentage -> Happy color
        assertEquals(R.color.zen_mindfulness_happy, AppLogic.getMindfulnessColor(0L))
        
        // Medium percentage -> Neutral color (Assuming logic from AppLogic)
        // Let's check boundary. If 100% is happy.
        
        // Low percentage -> Annoyed color
        assertEquals(R.color.zen_mindfulness_annoyed, AppLogic.getMindfulnessColor(120L))
    }
}
