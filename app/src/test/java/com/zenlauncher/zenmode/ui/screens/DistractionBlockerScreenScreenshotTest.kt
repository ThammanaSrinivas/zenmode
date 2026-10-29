package com.zenlauncher.zenmode.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.accessibility.ContentBlockRules
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Snapshot coverage for the Distraction Blocker settings page (Figma nodes 2026:949 / 2026:992),
 * one golden per branch the screen takes off [BlockerState]: apps still loading, the loaded list
 * (quieted rows, rows with in-app feeds, every `usageLabel` wording), the accessibility banner,
 * the paused header, and the empty list. The search query, the All/Quieted/Open filter and each
 * row's "in-app feeds" expansion are internal `rememberSaveable` state with no way to drive them
 * from outside without a click, so those branches aren't reachable from a static snapshot; not
 * chased, per this task's "click handlers don't run in a static snapshot" guidance.
 * Inspection mode is forced on so entrance motion renders its settled frame.
 * Baselines: `./gradlew recordPaparazzi`, review under app/src/test/snapshots/.
 */
class DistractionBlockerScreenScreenshotTest {

    // Tall enough that the six-app list's last item (the debug tools) and its 96dp bottom padding
    // fit above the frosted foot: the page is a LazyColumn, so anything past the viewport would
    // be cut off.
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 3200))

    /** In-app feeds per package, built the same way DistractionBlockerActivity builds them. */
    private val surfaces = ContentBlockRules.default().apps.mapValues { (_, rule) ->
        rule.surfaces.map { it.id to it.label }
    }

    private fun app(packageName: String, label: String, minutesToday: Long) =
        BlockerApp(packageName, label, minutesToday, surfaces[packageName].orEmpty())

    // Sorted by today's minutes, like the activity: covers usageLabel's hours, minutes and
    // "not opened today" wordings.
    private val phoneApps = listOf(
        app(ContentBlockRules.INSTAGRAM, "Instagram", 134),
        app(ContentBlockRules.YOUTUBE, "YouTube", 72),
        app("com.whatsapp", "WhatsApp", 41),
        app(ContentBlockRules.SNAPCHAT, "Snapchat", 18),
        app("com.spotify.music", "Spotify", 6),
        app("com.google.android.calendar", "Google Calendar", 0)
    )

    @Test
    fun `distraction blocker - apps loading, reels off, free`() {
        // appsLoaded=false hits the spinner branch; reels off hits QuietReelsCard's "Off." copy,
        // and a free, unpaused user gets the PRO tag on the Pause pill.
        snapshot(BlockerState(appsLoaded = false, reelsQuieted = false, isPro = false))
    }

    @Test
    fun `distraction blocker - apps quieted, reels on, pro`() {
        // Loaded list: two apps quieted, three with in-app feeds (the chevron), initials tiles
        // (no real icons under layoutlib). 420 min saved -> savedParts' "7 HOURS"; a pro user
        // gets no PRO tag.
        snapshot(
            BlockerState(
                apps = phoneApps,
                appsLoaded = true,
                quieted = setOf(ContentBlockRules.INSTAGRAM, ContentBlockRules.SNAPCHAT),
                blockedSurfaces = setOf("${ContentBlockRules.YOUTUBE}/shorts"),
                reelsQuieted = true,
                stops = 128,
                minutesSaved = 420,
                isPro = true
            )
        )
    }

    @Test
    fun `distraction blocker - accessibility off`() {
        // accessibilityOn=false shows the amber banner; 60 min saved -> the singular "1 HOUR".
        snapshot(
            BlockerState(
                apps = phoneApps.take(3),
                appsLoaded = true,
                reelsQuieted = true,
                stops = 9,
                minutesSaved = 60,
                accessibilityOn = false
            )
        )
    }

    @Test
    fun `distraction blocker - paused`() {
        // isPaused and minutesLeft read System.currentTimeMillis(), not LocalZenClock. The state
        // is built inside the composition that renders it (see snapshot), with the pause ending
        // 1ms short of 30m out: minutesLeft = floor(29m59.999s - elapsed) + 1 reads "Paused 30m"
        // for any elapsed under 59.999s, and building-to-drawing in one composition takes
        // milliseconds, however slow the machine is before the render starts. Paused pill +
        // QuietReelsCard's "Paused." copy; 40 min saved -> "40 MINS".
        snapshot {
            BlockerState(
                apps = phoneApps,
                appsLoaded = true,
                quieted = setOf(ContentBlockRules.INSTAGRAM),
                reelsQuieted = true,
                pausedUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30) - 1,
                stops = 23,
                minutesSaved = 40,
                isPro = true
            )
        }
    }

    @Test
    fun `distraction blocker - no apps, debug crash note`() {
        // Loaded but empty hits EmptyApps (All filter, no search). The short page also leaves
        // room for the debug-build tools, with the debug dump on and a last-crash note.
        snapshot(
            BlockerState(
                appsLoaded = true,
                debugDump = true,
                lastCrash = "NullPointerException at 09:41"
            )
        )
    }

    private fun snapshot(state: BlockerState) = snapshot { state }

    /** Builds the state in the rendering composition, so a clock read in it can't go stale. */
    private fun snapshot(buildState: () -> BlockerState) {
        paparazzi.golden {
            // The goldens include the debug tools; pin them so `testReleaseUnitTest` (where
            // BuildConfig.DEBUG is false) renders the same page as `testDebugUnitTest`.
            val state = remember { buildState().copy(showDebugTools = true) }
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ZenTheme(darkTheme = false) {
                    DistractionBlockerScreen(
                        state = state,
                        onBackClick = {},
                        onPauseClick = {},
                        onReelsToggle = {},
                        onAppToggle = { _, _ -> },
                        onSurfaceToggle = { _, _, _ -> },
                        onEnableAccessibility = {}
                    )
                }
            }
        }
    }
}
