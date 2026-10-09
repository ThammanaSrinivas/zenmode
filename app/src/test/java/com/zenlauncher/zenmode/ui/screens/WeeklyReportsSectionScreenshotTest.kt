package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.recap.AppMinutes
import com.zenlauncher.zenmode.recap.DayRecord
import com.zenlauncher.zenmode.recap.WeeklyRecap
import com.zenlauncher.zenmode.recap.WeeklyReportsSection
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Golden coverage for Settings → "Weekly reports", the section that decides what a free
 * account can replay. The last two weeks are open to everyone and the weeks before them are
 * PRO, so the free golden is the one that has to keep showing exactly two live rows above the lock.
 */
class WeeklyReportsSectionScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 1400))

    /** Newest first, the order [com.zenlauncher.zenmode.recap.RecapStore.completedWeeks] returns. */
    private val reports = listOf(
        week(LocalDate.of(2026, 9, 7), minutesPerDay = 148, promiseHours = 3),
        week(LocalDate.of(2026, 8, 31), minutesPerDay = 212, promiseHours = 3),
        week(LocalDate.of(2026, 8, 24), minutesPerDay = 174, promiseHours = 3),
        week(LocalDate.of(2026, 8, 17), minutesPerDay = 131, promiseHours = 3)
    )

    private fun week(start: LocalDate, minutesPerDay: Long, promiseHours: Int) = WeeklyRecap(
        weekStart = start,
        days = (0L..6L).map { offset ->
            DayRecord(
                date = start.plusDays(offset),
                screenTimeMinutes = minutesPerDay + offset * 7,
                promiseHours = promiseHours,
                appMinutes = listOf(AppMinutes("com.example.feed", "Feed", minutesPerDay / 2)),
                lateNightMinutes = 12,
                pickups = 41
            )
        },
        previousWeekTotalMinutes = 1_280
    )

    @Test
    fun `weekly reports - free, last two weeks open`() {
        paparazzi.golden {
            ZenTheme(darkTheme = false) {
                Section(isPro = false)
            }
        }
    }

    @Test
    fun `weekly reports - pro, every week open`() {
        paparazzi.golden {
            ZenTheme(darkTheme = false) {
                Section(isPro = true)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Section(isPro: Boolean) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .fillMaxWidth()
                .background(ZenTheme.colors.bgPrimary)
                .padding(20.dp)
        ) {
            WeeklyReportsSection(
                reports = reports,
                isPro = isPro,
                downloadingWeek = null,
                onOpen = {},
                onDownload = {},
                onShare = {},
                onUnlockPro = {}
            )
        }
    }
}
