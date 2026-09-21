package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.coreapi.AppCategory
import com.zenlauncher.zenmode.coreapi.CoreConstants
import com.zenlauncher.zenmode.coreapi.ForegroundSession
import com.zenlauncher.zenmode.coreapi.SessionEventType
import com.zenlauncher.zenmode.coreapi.SessionLogBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLogBuilderTest {

    private val minute = 60_000L
    private val classify: (String) -> AppCategory = { pkg ->
        when (pkg) {
            "notion" -> AppCategory.FOCUS
            "instagram" -> AppCategory.ENTERTAINMENT
            "whatsapp" -> AppCategory.COMMUNICATION
            else -> AppCategory.OTHER
        }
    }
    private val labelOf: (String) -> String = { it.replaceFirstChar(Char::uppercase) }

    private fun build(
        windows: List<Pair<Long, Long>>,
        appSessions: List<ForegroundSession>,
        excluded: Set<String> = emptySet()
    ) = SessionLogBuilder.build(windows, appSessions, excluded, classify, labelOf)

    @Test
    fun `a glance shorter than the minimum is dropped entirely`() {
        val windows = listOf(0L to (CoreConstants.MIN_SESSION_DURATION_MS - 1))
        assertEquals(emptyList<Any>(), build(windows, emptyList()))
    }

    @Test
    fun `a focus-app session reads as intentional`() {
        val windows = listOf(0L to 5 * minute)
        val apps = listOf(ForegroundSession("notion", 0L, 5 * minute))
        val sessions = build(windows, apps)
        assertEquals(1, sessions.size)
        assertEquals(AppCategory.FOCUS, sessions[0].category)
        assertEquals(SessionEventType.INTENTIONAL, sessions[0].eventType)
        assertEquals("Notion", sessions[0].dominantLabel)
    }

    @Test
    fun `dominant app is whichever had the most time in the window`() {
        val windows = listOf(0L to 10 * minute)
        val apps = listOf(
            ForegroundSession("instagram", 0L, 2 * minute),
            ForegroundSession("whatsapp", 2 * minute, 9 * minute)
        )
        val sessions = build(windows, apps)
        assertEquals("whatsapp", sessions[0].dominantPackage)
        assertEquals(AppCategory.COMMUNICATION, sessions[0].category)
    }

    @Test
    fun `excluded packages (ZenMode itself) are never the dominant app`() {
        val windows = listOf(0L to 5 * minute)
        val apps = listOf(
            ForegroundSession("com.zenlauncher.zenmode", 0L, 4 * minute),
            ForegroundSession("instagram", 4 * minute, 5 * minute)
        )
        val sessions = build(windows, apps, excluded = setOf("com.zenlauncher.zenmode"))
        assertEquals("instagram", sessions[0].dominantPackage)
    }

    @Test
    fun `an unlock window with no app opened is dropped, not logged as a session`() {
        val windows = listOf(0L to 30_000L)
        val sessions = build(windows, emptyList())
        assertEquals(emptyList<Any>(), sessions)
    }

    @Test
    fun `a brief tap during a long mostly-idle unlock window reports its own real duration, not the whole window`() {
        val windows = listOf(0L to 30 * minute)
        val apps = listOf(ForegroundSession("weather", 5 * minute, 5 * minute + 5_000L))
        val sessions = build(windows, apps)
        assertEquals(1, sessions.size)
        assertEquals(5 * minute, sessions[0].startMillis)
        assertEquals(5 * minute + 5_000L, sessions[0].endMillis)
        assertEquals(5_000L, sessions[0].durationMillis)
    }

    @Test
    fun `an OTHER-category app (undeclared Play Store category, eg weather) reads as intentional`() {
        val windows = listOf(0L to 2 * minute)
        val apps = listOf(ForegroundSession("weather", 0L, 2 * minute))
        val sessions = build(windows, apps)
        assertEquals(1, sessions.size)
        assertEquals(AppCategory.OTHER, sessions[0].category)
        assertEquals(SessionEventType.INTENTIONAL, sessions[0].eventType)
    }

    @Test
    fun `reopening the same non-focus category right after the last session is a relapse`() {
        val gap = CoreConstants.RAPID_REOPEN_GAP_MS / 2
        val windows = listOf(
            0L to 2 * minute,
            (2 * minute + gap) to (2 * minute + gap + 2 * minute)
        )
        val apps = listOf(
            ForegroundSession("instagram", 0L, 2 * minute),
            ForegroundSession("instagram", 2 * minute + gap, 2 * minute + gap + 2 * minute)
        )
        val sessions = build(windows, apps)
        assertEquals(SessionEventType.ENTERTAINING, sessions[0].eventType)
        assertEquals(SessionEventType.DISRUPTED, sessions[1].eventType)
    }

    @Test
    fun `reopening the same category well after the gap window is a fresh session, not a relapse`() {
        val windows = listOf(
            0L to 2 * minute,
            (2 * minute + CoreConstants.RAPID_REOPEN_GAP_MS + minute) to
                (2 * minute + CoreConstants.RAPID_REOPEN_GAP_MS + 3 * minute)
        )
        val apps = listOf(
            ForegroundSession("instagram", 0L, 2 * minute),
            ForegroundSession(
                "instagram",
                2 * minute + CoreConstants.RAPID_REOPEN_GAP_MS + minute,
                2 * minute + CoreConstants.RAPID_REOPEN_GAP_MS + 3 * minute
            )
        )
        val sessions = build(windows, apps)
        assertEquals(SessionEventType.ENTERTAINING, sessions[1].eventType)
    }

    @Test
    fun `a session past the max duration is chunked, and the chunks cover the whole window`() {
        val total = CoreConstants.SESSION_MAX_DURATION_MS * 2 + 20 * minute
        val windows = listOf(0L to total)
        val apps = listOf(ForegroundSession("notion", 0L, total))
        val sessions = build(windows, apps)
        assertTrue(sessions.size > 1)
        assertEquals(0L, sessions.first().startMillis)
        assertEquals(total, sessions.last().endMillis)
        // Chunk boundaries are contiguous with no gap or overlap.
        sessions.zipWithNext().forEach { (a, b) -> assertEquals(a.endMillis, b.startMillis) }
    }
}
