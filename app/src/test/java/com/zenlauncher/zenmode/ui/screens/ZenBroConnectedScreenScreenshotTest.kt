package com.zenlauncher.zenmode.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test

/** Golden for "You're Zen Bros now", Zen Bro connect screen 3 (Figma node 2026:2346). Settled frame. */
class ZenBroConnectedScreenScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 2700))

    @Test
    fun `zen bro connected - you have less screen time`() = snapshot(inviteSheetOpen = false)

    @Test
    fun `zen bro connected - invite sheet open`() = snapshot(inviteSheetOpen = true)

    private fun snapshot(inviteSheetOpen: Boolean) {
        paparazzi.golden {
            // Paparazzi leaves inspection mode off; turn it on so the entrance motion renders settled.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ZenTheme(darkTheme = false) {
                    ZenBroConnectedScreen(
                        buddyName = "SriniMas",
                        usage = DailyUsage(screenTimeInMillis = 59 * 60 * 1000L),
                        streaks = 13,
                        zenScore = 7,
                        buddyStats = BuddyStats(screenTimeMins = 4 * 60 + 59),
                        userCode = "k7Hq2Lm9XyZpQ4",
                        onBackClick = {},
                        onShareInviteLink = {},
                        onCopyInviteCode = {},
                        onMaybeLater = {},
                        initialInviteSheetOpen = inviteSheetOpen
                    )
                }
            }
        }
    }
}
