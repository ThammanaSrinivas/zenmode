package com.zenlauncher.zenmode.share

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.zenlauncher.zenmode.PromiseUnit
import com.zenlauncher.zenmode.ZenGoldPromiseState
import com.zenlauncher.zenmode.recap.DayRecord
import com.zenlauncher.zenmode.recap.WeeklyRecap
import com.zenlauncher.zenmode.ui.screens.golden
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Goldens for every share card, tier by tier, as contact sheets: one image per family, each card
 * at a third of its 1080-wide export size, settled. These pin the pictures themselves; the words
 * on them are pinned by ShareCopyTest. A 1×-density device, so a dp here is a pixel there.
 */
class ShareCardsScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 1080,
            screenHeight = 1350,
            xdpi = 160,
            ydpi = 160,
            density = Density.MEDIUM,
            softButtons = false
        )
    )

    private val today = LocalDate.of(2026, 9, 13)

    private fun sheet(content: @Composable () -> Unit) = paparazzi.golden {
        CompositionLocalProvider(LocalInspectionMode provides true) { content() }
    }

    @Composable
    private fun Grid(arts: List<ShareArt>, width: Int = 360, height: Int = 450) {
        Column {
            arts.chunked(1080 / width).forEach { row ->
                Row { row.forEach { ShareArtView(it, Modifier.size(width.dp, height.dp)) } }
            }
        }
    }

    @Test
    fun `streak cards - from a spark to an orbit`() = sheet {
        val kit = rememberShareKit()
        Grid(listOf(0, 3, 7, 21, 30, 64, 120, 200, 400).map { StreakArt(kit, streak(it)) })
    }

    @Test
    fun `zen score cards - five states of water`() = sheet {
        val kit = rememberShareKit()
        Grid(listOf(95, 84, 70, 48, 25).map { ScoreArt(kit, ScoreShare(it, it - 6, 47, today)) })
    }

    @Test
    fun `zen gold cards - earning to a vault`() = sheet {
        val kit = rememberShareKit()
        Grid(golds.map { (rupees, week) -> GoldArt(kit, GoldShare(rupees, week, today)) })
    }

    @Test
    fun `weekly story - perfect, kept and missed, settled and mid-chart`() = sheet {
        val kit = rememberShareKit()
        val arts = weeks.map { WeeklyArt(kit, WeeklyShare(it)) }
        Column {
            Row { arts.forEach { ShareArtView(it, Modifier.size(360.dp, 640.dp)) } }
            // The kept week caught mid-arrival: numbers rolling, then bars rising.
            Row { listOf(1_500f, 3_200f, 4_400f).forEach { Frame(arts[1], it, Modifier.size(360.dp, 640.dp)) } }
        }
    }

    /** One frame of [art] at [timeMs], for checking the arrival rather than the settled card. */
    @Composable
    private fun Frame(art: ShareArt, timeMs: Float, modifier: Modifier) {
        Canvas(modifier) {
            drawIntoCanvas { c ->
                val scale = size.width / art.format.width
                c.nativeCanvas.save()
                c.nativeCanvas.scale(scale, scale)
                art.draw(c.nativeCanvas, timeMs)
                c.nativeCanvas.restore()
            }
        }
    }

    /** A streak of [days] kept days, a week won per seven, three of this week kept. */
    private fun streak(days: Int) =
        StreakShare(days, days / 7, 3, false, listOf(true, true, true, null, null, null, null), today)

    private fun week(vararg kept: Boolean?) = ZenGoldPromiseState(
        promiseHours = 4,
        units = listOf("M", "T", "W", "T", "F", "S", "S").mapIndexed { i, l -> PromiseUnit(l, kept.getOrNull(i)) },
        unitsKept = kept.count { it == true },
        unitsMissed = kept.count { it == false },
        unitsRemaining = kept.count { it == null },
        goalMet = kept.count { it == true } >= 5
    )

    private val golds = listOf(
        0L to week(true, true, false, true, null, null, null),
        0L to week(true, true, true, false, true, true, null),
        450L to week(true, true, true, false, true, true, true),
        2_350L to week(true, false, true, true, null, null, null),
        24_500L to week(true, true, true, true, true, null, null),
        1_25_000L to week(true, true, true, true, true, true, true)
    )

    private val monday = LocalDate.of(2026, 9, 7)

    private fun recap(vararg hours: Double, previous: Long? = 1_700) = WeeklyRecap(
        weekStart = monday,
        days = hours.mapIndexed { i, h -> DayRecord(monday.plusDays(i.toLong()), (h * 60).toLong(), 4, emptyList(), 0, null) },
        previousWeekTotalMinutes = previous
    )

    private val weeks = listOf(
        recap(2.1, 1.4, 3.2, 1.2, 2.6, 3.5, 2.9),
        recap(3.1, 4.6, 2.2, 1.2, 3.6, 5.1, 2.9),
        recap(5.1, 4.6, 3.2, 6.2, 4.6, 6.7, 3.9, previous = 1_500)
    )
}
