package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.coreapi.ForegroundPairing
import com.zenlauncher.zenmode.coreapi.ForegroundPairing.Kind.PAUSED
import com.zenlauncher.zenmode.coreapi.ForegroundPairing.Kind.RESUMED
import com.zenlauncher.zenmode.coreapi.ForegroundSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundPairingTest {

    private val zen = "com.zenlauncher.zenmode"
    private val gms = "com.google.android.gms"
    private val linkedIn = "com.linkedin.android"

    private fun e(t: Long, pkg: String, kind: ForegroundPairing.Kind) = ForegroundPairing.Event(t, pkg, kind)

    private fun pair(vararg events: ForegroundPairing.Event, end: Long = 1_000L) =
        ForegroundPairing.pair(events.toList(), end, excluded = setOf(zen))

    @Test
    fun `an app's resume to pause is one session`() {
        val result = pair(e(10, linkedIn, RESUMED), e(40, linkedIn, PAUSED))
        assertEquals(listOf(ForegroundSession(linkedIn, 10, 40)), result.sessions)
    }

    @Test
    fun `a sign-in sheet over an app is credited to that app`() {
        val result = pair(
            e(10, linkedIn, RESUMED), e(20, linkedIn, PAUSED),
            e(20, gms, RESUMED), e(50, gms, PAUSED),
            e(50, linkedIn, RESUMED), e(60, linkedIn, PAUSED)
        )
        assertTrue(result.sessions.none { it.packageName == gms })
        assertEquals(50L, result.sessions.filter { it.packageName == linkedIn }.sumOf { it.endMillis - it.startMillis })
    }

    @Test
    fun `a sign-in sheet over ZenMode itself isn't screen time`() {
        val result = pair(
            e(10, zen, RESUMED), e(20, zen, PAUSED),
            e(20, gms, RESUMED), e(50, gms, PAUSED)
        )
        assertEquals(emptyList<ForegroundSession>(), result.sessions)
        assertTrue(result.sawActivity)
    }

    @Test
    fun `a sheet still open at the end is closed there, under its host`() {
        val result = pair(e(10, linkedIn, RESUMED), e(20, linkedIn, PAUSED), e(20, gms, RESUMED), end = 100)
        assertEquals(ForegroundSession(linkedIn, 20, 100), result.sessions.last())
    }

    @Test
    fun `a pause before any resume belongs to the previous day and is ignored`() {
        val result = pair(e(5, linkedIn, PAUSED))
        assertEquals(emptyList<ForegroundSession>(), result.sessions)
        assertFalse(result.sawActivity)
    }
}
