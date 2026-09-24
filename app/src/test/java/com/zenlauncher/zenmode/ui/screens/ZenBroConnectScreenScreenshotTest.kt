package com.zenlauncher.zenmode.ui.screens

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test

/** Golden for "My Zen Circle", Zen Bro connect screen 2 (Figma node 2026:2443). */
class ZenBroConnectScreenScreenshotTest {

    // Taller than a Pixel 5 so all three option cards land in one frame.
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 2700))

    @Test
    fun `zen bro connect - signed in`() {
        paparazzi.golden {
            ZenTheme(darkTheme = false) {
                ZenBroConnectScreen(
                    userCode = "k7Hq2Lm9XyZ",
                    onBackClick = {},
                    onShareLink = {},
                    onCopyCode = {},
                    onAddBuddy = { BuddyAddResult.Error("") },
                    onRandomConnect = {}
                )
            }
        }
    }
}
