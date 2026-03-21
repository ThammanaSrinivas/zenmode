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

    @Test
    fun mindfulness_thresholds_areCorrect() {
        assertEquals(70, AppConstants.MINDFULNESS_HAPPY_MIN_PERCENT)
        assertEquals(40, AppConstants.MINDFULNESS_NEUTRAL_MIN_PERCENT)
    }

    @Test
    fun stats_sync_interval_isPositive() {
        assertTrue(AppConstants.STATS_SYNC_INTERVAL_MINUTES > 0)
        assertEquals(10, AppConstants.STATS_SYNC_INTERVAL_MINUTES)
    }

    @Test
    fun external_urls_areNotEmpty() {
        assertTrue(AppConstants.GITHUB_URL.isNotBlank())
        assertTrue(AppConstants.YT_BUDDY_INVITE_URL.isNotBlank())
        assertTrue(AppConstants.YT_BUDDY_CONFUSED_URL.isNotBlank())
    }
}
