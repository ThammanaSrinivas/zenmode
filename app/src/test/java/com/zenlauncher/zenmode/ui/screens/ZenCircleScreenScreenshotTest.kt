package com.zenlauncher.zenmode.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test

/** Golden for the "My Zen Circle" dashboard (Figma node 2026:2493). Settled frame. */
class ZenCircleScreenScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 2700))

    @Test
    fun `zen circle - you leading`() = snapshot(sheet = null)

    @Test
    fun `zen circle - settings sheet open`() = snapshot(sheet = ZenCircleSheet.Settings)

    private fun snapshot(sheet: ZenCircleSheet?) {
        paparazzi.snapshot {
            // Paparazzi leaves inspection mode off; turn it on so the entrance motion renders settled.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ZenTheme(darkTheme = false) {
                    ZenCircleScreen(
                        members = listOf(
                            ZenCircleMember("You", isYou = true, screenTimeMinutes = 59, zenScore = 93, streaks = 13, changePercent = 30),
                            ZenCircleMember("SriniMas", isYou = false, screenTimeMinutes = 4 * 60 + 59, zenScore = 91, streaks = 5)
                        ),
                        userCode = "k7Hq2Lm9XyZpQ4",
                        onBackClick = {},
                        onShareInviteLink = {},
                        onCopyInviteCode = {},
                        onBackToHome = {},
                        onSendLove = {},
                        onSendMelt = {},
                        onWeeklyClick = {},
                        removingBuddy = false,
                        onRemoveBuddy = {},
                        onLeaveCircle = {},
                        initialSheet = sheet
                    )
                }
            }
        }
    }
}
