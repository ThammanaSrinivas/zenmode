package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.coreapi.ZenScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZenScoreTest {

    private val hour = 3_600_000L

    @Test
    fun `a quiet day under the promise with perfect session quality scores a full ten`() {
        assertEquals(
            ZenScore.MAX_TENTHS,
            ZenScore.compute(screenTimeMillis = hour, sessionQualityPercent = 100, promiseHours = 4)
        )
    }

    @Test
    fun `twice the promise with zero session quality scores zero`() {
        assertEquals(0, ZenScore.compute(screenTimeMillis = 8 * hour, sessionQualityPercent = 0, promiseHours = 4))
    }

    @Test
    fun `more screen time never raises the score`() {
        val scores = (0..10).map { ZenScore.compute(it * hour, sessionQualityPercent = 50, promiseHours = 4) }
        assertTrue(scores.zipWithNext().all { (a, b) -> b <= a })
    }

    @Test
    fun `better session quality never lowers the score at the same screen time`() {
        val screenTime = 3 * hour // between the free and zero ratios, so adherence is fractional
        val worse = ZenScore.compute(screenTime, sessionQualityPercent = 0, promiseHours = 4)
        val better = ZenScore.compute(screenTime, sessionQualityPercent = 100, promiseHours = 4)
        assertTrue(better > worse)
    }

    @Test
    fun `score never leaves the 0 to 10 range`() {
        assertEquals(ZenScore.MAX_TENTHS, ZenScore.compute(-5, -3, 0))
        assertEquals(0, ZenScore.compute(Long.MAX_VALUE / 2, Int.MAX_VALUE, 1))
    }

    @Test
    fun `format shows one decimal out of ten`() {
        assertEquals("9.3", ZenScore.format(93))
        assertEquals("10.0", ZenScore.format(100))
        assertEquals("0.0", ZenScore.format(-4))
        assertEquals("0.4", ZenScore.formatDelta(-4))
    }
}
