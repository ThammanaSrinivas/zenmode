package com.zenlauncher.zenmode.share

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.MoodState
import com.zenlauncher.zenmode.recap.DayRecord
import com.zenlauncher.zenmode.recap.WeeklyRecap
import com.zenlauncher.zenmode.ui.components.MoodBackdrop
import com.zenlauncher.zenmode.ui.screens.ZenCircleMember
import com.zenlauncher.zenmode.ui.screens.ZenCircleShareCard
import com.zenlauncher.zenmode.ui.screens.golden
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** The places a card is shared from, on a real phone's frame: does the card fit, do the actions read. */
class ShareSheetScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private fun screen(content: @Composable () -> Unit) = paparazzi.golden {
        CompositionLocalProvider(LocalInspectionMode provides true) {
            ZenTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize()) {
                    MoodBackdrop(MoodState.HAPPY)
                    content()
                }
            }
        }
    }

    @Test
    fun `share sheet - a one-moon streak`() = screen {
        StreakShareSheet(StreakShare(30, 4, 3, false, listOf(true, true, true, null, null, null, null), LocalDate.of(2026, 9, 13))) {}
    }

    @Test
    fun `weekly share studio - a kept week`() = screen {
        val monday = LocalDate.of(2026, 9, 7)
        val recap = WeeklyRecap(
            weekStart = monday,
            days = listOf(3.1, 4.6, 2.2, 1.2, 3.6, 5.1, 2.9).mapIndexed { i, h ->
                DayRecord(monday.plusDays(i.toLong()), (h * 60).toLong(), 4, emptyList(), 0, null)
            },
            previousWeekTotalMinutes = 1_700
        )
        WeeklyShareStudio(visible = true, recap = recap, canSharePdf = true, onSharePdf = {}, onDismiss = {})
    }

    @Test
    fun `zen circle card - leading today`() = screen {
        val members = listOf(
            ZenCircleMember("You", isYou = true, screenTimeMinutes = 96, zenScore = 91, streaks = 12),
            ZenCircleMember("Asha Rao", isYou = false, screenTimeMinutes = 140, zenScore = 82, streaks = 6),
            ZenCircleMember("Dev K", isYou = false, screenTimeMinutes = 205, zenScore = 64, streaks = 2)
        )
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ZenCircleShareCard(members = members, ranks = mapOf(0 to 1, 1 to 2, 2 to 3))
        }
    }
}
