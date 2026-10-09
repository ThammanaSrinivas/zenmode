package com.zenlauncher.zenmode.ui.screens

import com.zenlauncher.zenmode.AppConstants
import org.junit.Assert.assertEquals
import org.junit.Test

/** The resistance ring counts down: the first second shows the full wait, the last shows 1. */
class ResistenceCountdownTest {

    private val total = AppConstants.COUNTDOWN_SECONDS

    @Test
    fun `counts down from the full wait to one`() {
        val shown = (1..total).map { countdownSecondsLeft(it, finished = false) }
        assertEquals((total downTo 1).toList(), shown)
    }

    @Test
    fun `finished holds on one`() {
        assertEquals(1, countdownSecondsLeft(elapsedSeconds = 3, finished = true))
    }

    @Test
    fun `out of range elapsed stays within the wait`() {
        assertEquals(total, countdownSecondsLeft(elapsedSeconds = 0, finished = false))
        assertEquals(1, countdownSecondsLeft(elapsedSeconds = total + 5, finished = false))
    }
}
