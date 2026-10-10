package com.zenlauncher.zenmode.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

/**
 * The backdrop's drift, as a function of time. It used to be an InfiniteTransition; these pin
 * the same motion now that it runs on its own slow clock.
 */
class BackdropDriftTest {

    private val tolerance = 0.0001f

    @Test
    fun `phase starts at zero and is half way round after half a lap`() {
        assertEquals(0f, BackdropDrift.phase(0f), tolerance)
        assertEquals(PI.toFloat(), BackdropDrift.phase(14f), tolerance)
    }

    @Test
    fun `phase starts round again after a full lap`() {
        assertEquals(BackdropDrift.phase(3f), BackdropDrift.phase(28f + 3f), tolerance)
    }

    @Test
    fun `swell runs from its smallest to its largest and back`() {
        assertEquals(0.92f, BackdropDrift.swell(0f), tolerance)
        assertEquals(1.08f, BackdropDrift.swell(9f), tolerance)
        assertEquals(0.92f, BackdropDrift.swell(18f), tolerance)
    }

    @Test
    fun `swell plays the way back as the mirror of the way out`() {
        assertEquals(BackdropDrift.swell(2f), BackdropDrift.swell(18f - 2f), tolerance)
    }

    @Test
    fun `one tick is a small step, so the slow clock still reads as continuous`() {
        val tick = BackdropDrift.TICK_MILLIS / 1000f
        var seconds = 0f
        while (seconds < 28f) {
            val turn = abs(BackdropDrift.phase(seconds + tick) - BackdropDrift.phase(seconds))
            // Under half a percent of a lap, except the wrap at the end of one.
            assertTrue("phase step $turn at $seconds", turn < 0.03f || turn > 6f)
            // Under a thirtieth of the swell's 0.16 range.
            val swell = abs(BackdropDrift.swell(seconds + tick) - BackdropDrift.swell(seconds))
            assertTrue("swell step $swell at $seconds", swell < 0.005f)
            seconds += tick
        }
    }
}
