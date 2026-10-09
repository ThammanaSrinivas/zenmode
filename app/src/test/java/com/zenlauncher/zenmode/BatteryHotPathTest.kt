package com.zenlauncher.zenmode

import android.graphics.drawable.Drawable
import com.zenlauncher.zenmode.NotificationCounts.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * The three hot paths that run far more often than they look like they do: the notification
 * listener (woken for every notification on the device), the home-grid ordering (every Home
 * resume) and the accessibility surface gate (several times a second while a tracked app is
 * scrolling). Each is pure so the behaviour can be pinned down without a device.
 */
class BatteryHotPathTest {

    // ── Notification counts ───────────────────────────────────────

    @Test
    fun `counts group by package, skipping ongoing and our own`() {
        val counts = NotificationCounts.countsOf(
            listOf(
                Entry("com.mail", isOngoing = false),
                Entry("com.mail", isOngoing = false),
                Entry("com.chat", isOngoing = false),
                Entry("com.player", isOngoing = true),
                Entry("com.zenlauncher.zenmode", isOngoing = false)
            ),
            selfPackage = "com.zenlauncher.zenmode"
        )

        assertEquals(mapOf("com.mail" to 2, "com.chat" to 1), counts)
    }

    @Test
    fun `counts of nothing is empty`() {
        assertTrue(NotificationCounts.countsOf(emptyList(), "com.zenlauncher.zenmode").isEmpty())
    }

    @Test
    fun `applying an unchanged rebuild reports no change`() {
        val target = mutableMapOf("com.mail" to 2)

        val changed = NotificationCounts.applyTo(target, mapOf("com.mail" to 2))

        assertFalse(changed)
        assertEquals(mapOf("com.mail" to 2), target)
    }

    @Test
    fun `applying writes new counts and drops cleared packages`() {
        val target = mutableMapOf("com.mail" to 2, "com.chat" to 1)

        val changed = NotificationCounts.applyTo(target, mapOf("com.mail" to 3))

        assertTrue(changed)
        assertEquals(mapOf("com.mail" to 3), target)
    }

    @Test
    fun `applying an empty rebuild clears everything`() {
        val target = mutableMapOf("com.mail" to 2)

        assertTrue(NotificationCounts.applyTo(target, emptyMap()))
        assertTrue(target.isEmpty())
    }

    // ── Home grid ordering ────────────────────────────────────────

    private fun app(key: String) = AppInfo(
        label = key,
        packageName = key,
        icon = mock<Drawable>(),
        key = key
    )

    @Test
    fun `with no pins the alphabetical order is kept as-is`() {
        val apps = listOf(app("a"), app("b"), app("c"))

        assertEquals(apps, LauncherActivities.orderForHome(apps, emptyList()))
    }

    @Test
    fun `pinned apps lead in the chosen order, the rest stay alphabetical`() {
        val apps = listOf(app("a"), app("b"), app("c"), app("d"))

        val ordered = LauncherActivities.orderForHome(apps, listOf("c", "a"))

        assertEquals(listOf("c", "a", "b", "d"), ordered.map { it.key })
    }

    @Test
    fun `a pin for an app that is not installed is ignored`() {
        val apps = listOf(app("a"), app("b"))

        val ordered = LauncherActivities.orderForHome(apps, listOf("gone", "b"))

        assertEquals(listOf("b", "a"), ordered.map { it.key })
    }

    // ── Accessibility surface gate ────────────────────────────────

    private fun snapshot(vararg blockedKeys: String) = ContentBlockPrefs.Snapshot(
        pausedUntil = 0L,
        quietedApps = emptySet(),
        blockedSurfaceKeys = blockedKeys.toSet(),
        isDebugDumpEnabled = false
    )

    @Test
    fun `hasBlockedSurface is true only for the package the block belongs to`() {
        val prefs = snapshot(ContentBlockPrefs.surfaceKey("com.instagram.android", "reels"))

        assertTrue(prefs.hasBlockedSurface("com.instagram.android"))
        assertFalse(prefs.hasBlockedSurface("com.google.android.youtube"))
    }

    @Test
    fun `hasBlockedSurface is false when nothing is blocked`() {
        assertFalse(snapshot().hasBlockedSurface("com.instagram.android"))
    }

    @Test
    fun `hasBlockedSurface does not match a package that merely shares a prefix`() {
        val prefs = snapshot(ContentBlockPrefs.surfaceKey("com.app", "feed"))

        assertTrue(prefs.hasBlockedSurface("com.app"))
        assertFalse(prefs.hasBlockedSurface("com.ap"))
    }

    @Test
    fun `a package with a blocked surface still reports its other surfaces unblocked`() {
        val prefs = snapshot(ContentBlockPrefs.surfaceKey("com.instagram.android", "reels"))

        assertTrue(prefs.hasBlockedSurface("com.instagram.android"))
        assertTrue(prefs.isSurfaceBlocked("com.instagram.android", "reels"))
        assertFalse(prefs.isSurfaceBlocked("com.instagram.android", "stories"))
    }
}
