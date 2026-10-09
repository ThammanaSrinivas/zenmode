package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test

/** Golden for the post-update "What's new" overlay, over a Home mood wash. Settled frame. */
class WhatsNewOverlayScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `whats new - light`() = snapshot(darkTheme = false)

    @Test
    fun `whats new - dark`() = snapshot(darkTheme = true)

    private fun snapshot(darkTheme: Boolean) {
        paparazzi.golden {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ZenTheme(darkTheme = darkTheme) {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(ZenTheme.colors.washHappy))) {
                        WhatsNewOverlay(visible = true, versionName = "3.05", onDismiss = {})
                    }
                }
            }
        }
    }
}
