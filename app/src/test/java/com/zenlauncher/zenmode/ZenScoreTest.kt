package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.coreapi.CoreConstants
import com.zenlauncher.zenmode.coreapi.ZenScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZenScoreTest {

    private val hour = 3_600_000L
    private val promise = 4

    private fun score(screenTimeMillis: Long, distracted: Int = 0) =
        ZenScore.compute(screenTimeMillis, distractedSessions = distracted, promiseHours = promise)

    @Test
    fun `keeping the promise exactly is worth eight`() {
        assertEquals(80, score(promise * hour))
    }

    @Test
    fun `a kept day never drops below eight, however distracted`() {
        val hours = listOf(0L, 1L, 2L, 3L, 4L)
        hours.forEach { h -> assertTrue(score(h * hour, distracted = 500) >= CoreConstants.SCORE_KEPT_TENTHS) }
    }

    @Test
    fun `half the promise or less, undistracted, is a full ten`() {
        assertEquals(ZenScore.MAX_TENTHS, score(promise * hour / 2))
        assertEquals(ZenScore.MAX_TENTHS, score(0L))
    }

    @Test
    fun `three quarters of the promise lands at nine`() {
        assertEquals(90, score(promise * hour * 3 / 4))
    }

    @Test
    fun `distractions only eat into the unused-time bonus`() {
        // 0.3 + 0.3/2^1.3 + 0.3/3^1.3 = 0.49 off a 9.0 day.
        assertEquals(85, score(promise * hour * 3 / 4, distracted = 3))
    }

    @Test
    fun `going over costs a gentle, saturating penalty`() {
        assertEquals(76, score(promise * hour * 11 / 10)) // 10% over
        assertEquals(65, score(promise * hour * 3 / 2)) // 50% over
        assertEquals(58, score(2 * promise * hour)) // twice the promise
        assertEquals(52, score(100 * promise * hour)) // wildly over: the penalty caps at 2.8
    }

    @Test
    fun `the worst possible day still reads above the floor`() {
        val worst = score(100 * promise * hour, distracted = Int.MAX_VALUE)
        assertTrue(worst >= CoreConstants.SCORE_FLOOR_TENTHS)
        assertTrue(worst in 38..42) // 8 - 2.8 - ~1.2
    }

    @Test
    fun `more screen time never raises the score`() {
        val scores = (0..40).map { score(it * hour / 4, distracted = 2) }
        assertTrue(scores.zipWithNext().all { (a, b) -> b <= a })
    }

    @Test
    fun `another distraction never raises the score`() {
        val scores = (0..30).map { score(5 * hour, distracted = it) }
        assertTrue(scores.zipWithNext().all { (a, b) -> b <= a })
    }

    @Test
    fun `score never leaves the floor to 10 range`() {
        assertTrue(ZenScore.compute(-5, -3, 0) in CoreConstants.SCORE_FLOOR_TENTHS..ZenScore.MAX_TENTHS)
        assertTrue(
            ZenScore.compute(Long.MAX_VALUE / 2, Int.MAX_VALUE, 1) in CoreConstants.SCORE_FLOOR_TENTHS..ZenScore.MAX_TENTHS
        )
    }

    @Test
    fun `format shows one decimal out of ten`() {
        assertEquals("9.3", ZenScore.format(93))
        assertEquals("10.0", ZenScore.format(100))
        assertEquals("0.0", ZenScore.format(-4))
        assertEquals("0.4", ZenScore.formatDelta(-4))
    }
}
