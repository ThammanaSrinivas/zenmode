package com.zenlauncher.zenmode.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakRangeTest {
    private val today = LocalDate.of(2026, 9, 13)

    @Test
    fun `a three-day streak started two days ago`() {
        assertEquals("SEP 11–PRESENT", currentStreakRange(3, today))
    }

    @Test
    fun `a one-day streak started today`() {
        assertEquals("SEP 13–PRESENT", currentStreakRange(1, today))
    }

    @Test
    fun `no streak yet`() {
        assertEquals("STARTS TODAY", currentStreakRange(0, today))
    }
}
