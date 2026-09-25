package com.zenlauncher.zenmode.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.GoldPromisePeriod
import com.zenlauncher.zenmode.PromiseUnit
import com.zenlauncher.zenmode.ZenGoldPromiseState
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test

/**
 * Snapshot coverage for the Zen Gold home page (Figma nodes 2026:1648 / 2026:1435), across the
 * three branches [com.zenlauncher.zenmode.ui.screens.promiseStatusMessage] and the screen's own
 * unlocked-state styling can take for the WEEKLY promise (the only period gold pay's unlock gate
 * ever reads — see [ZenGoldPromiseState.goalMet]'s doc comment). The Weekly/Monthly toggle is
 * internal `remember` state with no way to drive it from outside without a click, so the Monthly
 * tab itself isn't reachable from a static snapshot; not chased, per this task's "click handlers
 * don't run in a static snapshot" guidance.
 * Inspection mode is forced on so entrance motion renders its settled frame.
 * Baselines: `./gradlew recordPaparazzi`, review under app/src/test/snapshots/.
 */
class ZenGoldScreenScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 2700))

    @Test
    fun `zen gold - promise on track, one day from unlock`() {
        // unitsKept=4, unitsRemaining=2 -> daysUntilUnlock=1, hits GoldUnlockDisclaimer's
        // "Stay under tomorrow and it opens then" sub-branch (not the general ">1 days" one).
        snapshot(
            weekly = ZenGoldPromiseState(
                period = GoldPromisePeriod.WEEKLY,
                promiseHours = 4,
                dailyAverageMinutes = 187,
                units = listOf(
                    PromiseUnit("M", true), PromiseUnit("T", true), PromiseUnit("W", true),
                    PromiseUnit("T", true), PromiseUnit("F", null), PromiseUnit("S", null),
                    PromiseUnit("S", null)
                ),
                unitsKept = 4,
                unitsMissed = 0,
                unitsRemaining = 2,
                goalMet = false
            )
        )
    }

    @Test
    fun `zen gold - promise kept, gold pay unlocked`() {
        snapshot(
            weekly = ZenGoldPromiseState(
                period = GoldPromisePeriod.WEEKLY,
                promiseHours = 4,
                dailyAverageMinutes = 152,
                units = listOf(
                    PromiseUnit("M", true), PromiseUnit("T", true), PromiseUnit("W", true),
                    PromiseUnit("T", true), PromiseUnit("F", true), PromiseUnit("S", null),
                    PromiseUnit("S", null)
                ),
                unitsKept = 5,
                unitsMissed = 0,
                unitsRemaining = 2,
                goalMet = true
            )
        )
    }

    @Test
    fun `zen gold - promise broken, goal out of reach this week`() {
        // unitsKept=1, unitsRemaining=2 -> 1+2 less than PROMISE_DAYS_TO_UNLOCK(5), so
        // goalOutOfReach is true: hits promiseStatusMessage's "can't open this week any
        // more" branch, distinct from the plain not-yet-met branch above.
        snapshot(
            weekly = ZenGoldPromiseState(
                period = GoldPromisePeriod.WEEKLY,
                promiseHours = 4,
                dailyAverageMinutes = 301,
                units = listOf(
                    PromiseUnit("M", false), PromiseUnit("T", false), PromiseUnit("W", false),
                    PromiseUnit("T", false), PromiseUnit("F", true), PromiseUnit("S", null),
                    PromiseUnit("S", null)
                ),
                unitsKept = 1,
                unitsMissed = 4,
                unitsRemaining = 2,
                goalMet = false
            )
        )
    }

    private fun snapshot(weekly: ZenGoldPromiseState) {
        paparazzi.golden {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ZenTheme(darkTheme = false) {
                    ZenGoldScreen(
                        weekly = weekly,
                        onBackClick = {}
                    )
                }
            }
        }
    }
}
