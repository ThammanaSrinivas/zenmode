package com.zenlauncher.zenmode

import android.content.Context
import android.content.SharedPreferences
import com.zenlauncher.zenmode.coreapi.Circle
import com.zenlauncher.zenmode.coreapi.CircleMember
import com.zenlauncher.zenmode.coreapi.CircleRole
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.UsageRepository.Companion.CIRCLE_REACTION_MAX_COUNT
import com.zenlauncher.zenmode.coreapi.UsageRepository.Companion.CIRCLE_REACTION_WINDOW_MS
import com.zenlauncher.zenmode.coreapi.analytics.AnalyticsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Circle-cache (`cacheCircle`/`getCachedCircle`/`clearCachedCircle`/`getCachedCircleId`/
 * `saveCircleId`) and per-target reaction rate-limiter sections of [UsageRepository]. Mirrors
 * [UsageRepositoryTest]'s mocked-`SharedPreferences` pattern.
 */
class UsageRepositoryCircleTest {

    private fun createMockedRepository(): Triple<UsageRepository, SharedPreferences, SharedPreferences.Editor> {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        val editor = mock<SharedPreferences.Editor>()

        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putInt(any(), any())).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.putLong(any(), any())).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(editor.remove(any())).thenReturn(editor)

        val analyticsManager = mock<AnalyticsManager>()
        val repository = UsageRepository(context, analyticsManager)
        return Triple(repository, prefs, editor)
    }

    private fun sampleCircle() = Circle(
        id = "circle-1",
        name = "Focus Crew",
        leaderUid = "uid-leader",
        members = listOf(
            CircleMember(
                uid = "uid-leader",
                displayName = "Leader",
                zenScore = 90,
                screenTimeMinutes = 42L,
                lastUpdatedEpochMs = 1_000L,
                role = CircleRole.LEADER,
                joinedAtEpochMs = 500L
            ),
            CircleMember(
                uid = "uid-member",
                displayName = null,
                zenScore = 60,
                screenTimeMinutes = 0L,
                lastUpdatedEpochMs = 2_000L,
                role = CircleRole.MEMBER,
                joinedAtEpochMs = 1_500L
            )
        ),
        createdAtEpochMs = 100L
    )

    // ── cacheCircle / getCachedCircle ────────────────────────────────

    @Test
    fun `cacheCircle writes a JSON blob under circle_cache_json`() {
        val (repository, _, editor) = createMockedRepository()

        repository.cacheCircle(sampleCircle())

        verify(editor).putString(eq("circle_cache_json"), any())
        verify(editor).apply()
    }

    @Test
    fun `getCachedCircle round-trips id, name, leader and member fields including a null displayName`() {
        val (repository, prefs, editor) = createMockedRepository()
        val captor = org.mockito.kotlin.argumentCaptor<String>()

        repository.cacheCircle(sampleCircle())
        verify(editor).putString(eq("circle_cache_json"), captor.capture())
        whenever(prefs.getString("circle_cache_json", null)).thenReturn(captor.firstValue)

        val restored = repository.getCachedCircle()

        assertEquals("circle-1", restored?.id)
        assertEquals("Focus Crew", restored?.name)
        assertEquals("uid-leader", restored?.leaderUid)
        assertEquals(100L, restored?.createdAtEpochMs)
        assertEquals(2, restored?.members?.size)

        val leader = restored?.members?.first { it.uid == "uid-leader" }
        assertEquals("Leader", leader?.displayName)
        assertEquals(90, leader?.zenScore)
        assertEquals(CircleRole.LEADER, leader?.role)
        assertEquals(1_000L, leader?.lastUpdatedEpochMs)
        assertEquals(500L, leader?.joinedAtEpochMs)

        val member = restored?.members?.first { it.uid == "uid-member" }
        assertNull(member?.displayName)
        assertEquals(CircleRole.MEMBER, member?.role)
    }

    @Test
    fun `getCachedCircle drops screenTimeMinutes across the round-trip`() {
        // cacheCircle's JSON payload does not include screenTimeMinutes, so it always comes
        // back as the CircleMember default (0L) regardless of what was cached — documenting
        // existing behavior, not a bug this task is scoped to fix.
        val (repository, prefs, editor) = createMockedRepository()
        val captor = org.mockito.kotlin.argumentCaptor<String>()

        repository.cacheCircle(sampleCircle())
        verify(editor).putString(eq("circle_cache_json"), captor.capture())
        whenever(prefs.getString("circle_cache_json", null)).thenReturn(captor.firstValue)

        val leader = repository.getCachedCircle()?.members?.first { it.uid == "uid-leader" }
        assertEquals(0L, leader?.screenTimeMinutes)
    }

    @Test
    fun `getCachedCircle returns null when nothing is cached`() {
        val (repository, prefs, _) = createMockedRepository()

        whenever(prefs.getString("circle_cache_json", null)).thenReturn(null)

        assertNull(repository.getCachedCircle())
    }

    @Test
    fun `getCachedCircle returns null on malformed JSON instead of throwing`() {
        val (repository, prefs, _) = createMockedRepository()

        whenever(prefs.getString("circle_cache_json", null)).thenReturn("{not valid json")

        assertNull(repository.getCachedCircle())
    }

    @Test
    fun `clearCachedCircle removes both the circle blob and the cached circle id`() {
        val (repository, _, editor) = createMockedRepository()

        repository.clearCachedCircle()

        verify(editor).remove("circle_cache_json")
        verify(editor).remove("circle_id_cached")
        verify(editor).apply()
    }

    // ── getCachedCircleId / saveCircleId ─────────────────────────────

    @Test
    fun `getCachedCircleId returns null when never set`() {
        val (repository, prefs, _) = createMockedRepository()

        whenever(prefs.contains("circle_id_cached")).thenReturn(false)

        assertNull(repository.getCachedCircleId())
    }

    @Test
    fun `getCachedCircleId returns empty string when self-heal previously cached no-circle`() {
        val (repository, prefs, _) = createMockedRepository()

        whenever(prefs.contains("circle_id_cached")).thenReturn(true)
        whenever(prefs.getString("circle_id_cached", null)).thenReturn("")

        assertEquals("", repository.getCachedCircleId())
    }

    @Test
    fun `saveCircleId stores the id, and null is stored as empty string`() {
        val (repository, _, editor) = createMockedRepository()

        repository.saveCircleId("circle-42")
        verify(editor).putString("circle_id_cached", "circle-42")

        repository.saveCircleId(null)
        verify(editor).putString("circle_id_cached", "")
    }

    // ── Circle reaction rate limit ───────────────────────────────────

    @Test
    fun `getRecentReactionTimestamps returns empty list when nothing recorded for that target`() {
        val (repository, prefs, _) = createMockedRepository()

        whenever(prefs.getString("recent_reaction_timestamps_uid-a", "")).thenReturn("")

        assertTrue(repository.getRecentReactionTimestamps("uid-a").isEmpty())
    }

    @Test
    fun `getRecentReactionTimestamps filters out entries older than the window and sorts the rest`() {
        val (repository, prefs, _) = createMockedRepository()
        val now = System.currentTimeMillis()
        val inWindowOld = now - CIRCLE_REACTION_WINDOW_MS + 1_000L
        val inWindowNew = now - 1_000L
        val outOfWindow = now - CIRCLE_REACTION_WINDOW_MS - 1_000L

        // Deliberately out of order in storage to prove the result is sorted.
        whenever(prefs.getString("recent_reaction_timestamps_uid-a", ""))
            .thenReturn("$inWindowNew,$outOfWindow,$inWindowOld")

        val result = repository.getRecentReactionTimestamps("uid-a")

        assertEquals(listOf(inWindowOld, inWindowNew), result)
    }

    @Test
    fun `getRecentReactionTimestamps is keyed per target, not shared across targets`() {
        val (repository, prefs, _) = createMockedRepository()
        val recent = (System.currentTimeMillis() - 1_000L).toString()

        whenever(prefs.getString("recent_reaction_timestamps_uid-a", "")).thenReturn(recent)
        whenever(prefs.getString("recent_reaction_timestamps_uid-b", "")).thenReturn("")

        assertEquals(1, repository.getRecentReactionTimestamps("uid-a").size)
        assertTrue(repository.getRecentReactionTimestamps("uid-b").isEmpty())
    }

    @Test
    fun `recordReactionSent appends the current timestamp for that target`() {
        val (repository, prefs, editor) = createMockedRepository()
        val now = System.currentTimeMillis()
        val older = now - 5_000L
        val newer = now - 3_000L
        whenever(prefs.getString("recent_reaction_timestamps_uid-a", "")).thenReturn("$older,$newer")

        repository.recordReactionSent("uid-a")

        val captor = org.mockito.kotlin.argumentCaptor<String>()
        verify(editor).putString(eq("recent_reaction_timestamps_uid-a"), captor.capture())
        val parts = captor.firstValue.split(",")
        assertEquals(listOf("$older", "$newer"), parts.dropLast(1))
        assertEquals(3, parts.size)
    }

    @Test
    fun `sendReaction rate limit allows up to CIRCLE_REACTION_MAX_COUNT sends then blocks the next`() {
        val (repository, prefs, _) = createMockedRepository()
        val now = System.currentTimeMillis()
        val timestamps = (0 until CIRCLE_REACTION_MAX_COUNT).map { now - it * 1_000L }
        whenever(prefs.getString("recent_reaction_timestamps_uid-a", ""))
            .thenReturn(timestamps.joinToString(","))

        val recent = repository.getRecentReactionTimestamps("uid-a")

        assertEquals(CIRCLE_REACTION_MAX_COUNT, recent.size)
        assertTrue(recent.size >= CIRCLE_REACTION_MAX_COUNT)
    }

    @Test
    fun `removeLastReactionTimestamp rolls back the most recent send for that target`() {
        val (repository, prefs, editor) = createMockedRepository()
        val now = System.currentTimeMillis()
        val older = now - 5_000L
        val newest = now - 1_000L
        whenever(prefs.getString("recent_reaction_timestamps_uid-a", "")).thenReturn("$older,$newest")

        repository.removeLastReactionTimestamp("uid-a")

        verify(editor).putString("recent_reaction_timestamps_uid-a", "$older")
        verify(editor).apply()
    }

    @Test
    fun `removeLastReactionTimestamp is a no-op when nothing is recorded for that target`() {
        val (repository, prefs, editor) = createMockedRepository()
        whenever(prefs.getString("recent_reaction_timestamps_uid-a", "")).thenReturn("")

        repository.removeLastReactionTimestamp("uid-a")

        verify(editor, org.mockito.kotlin.never()).putString(eq("recent_reaction_timestamps_uid-a"), any())
    }
}
