package com.zenlauncher.zenmode

import org.junit.Test
import org.junit.Assert.*

class AppConstantsTest {
    @Test
    fun constants_valuesAreCorrect() {
        assertEquals(60, AppConstants.THRESHOLD_HAPPY_MINUTES)
        assertEquals(120, AppConstants.THRESHOLD_NEUTRAL_MINUTES)
        assertEquals(60, AppConstants.GOAL_UNLOCKS_COUNT)
    }
}
