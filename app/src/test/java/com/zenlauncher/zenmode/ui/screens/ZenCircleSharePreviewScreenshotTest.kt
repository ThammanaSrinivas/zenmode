package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test

/**
 * The share preview is a full-screen takeover, so nothing of the page it opened from may read
 * through it. It used to scrim at 0.78 black over an unblurred page, and the Zen Circle
 * screen's own "Share & Invite to Zen Circle" / "Back to the Home" buttons showed through,
 * right behind the preview's own two buttons.
 *
 * The backdrop here stands in for that page — the same two labels, in the brightest form they
 * take. They must be invisible in the golden. The real screen also blurs underneath, but
 * `Modifier.blur` is a no-op below API 31, so the scrim alone has to do this job and this is
 * the test of it.
 */
class ZenCircleSharePreviewScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val members = listOf(
        ZenCircleMember("You", isYou = true, screenTimeMinutes = 152, zenScore = 76, streaks = 1),
        ZenCircleMember("SriniMas", isYou = false, screenTimeMinutes = 299, zenScore = 61, streaks = 5)
    )

    @Test
    fun `share preview - nothing of the page behind reads through`() {
        paparazzi.golden {
            ZenTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize()) {
                    // Stand-in for the Zen Circle page underneath, at worst case: its own
                    // buttons, in white on the brand green, exactly where the preview's land.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F7A18))
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Share & Invite to Zen Circle", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Back to the Home", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    ZenCircleSharePreview(
                        visible = true,
                        members = members,
                        ranks = mapOf(0 to 1, 1 to 2),
                        shareText = "Join my Zen Circle on ZenMode OS. #ZenTogether",
                        onDismiss = {}
                    )
                }
            }
        }
    }
}
