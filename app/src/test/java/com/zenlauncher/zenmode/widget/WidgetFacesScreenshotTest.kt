package com.zenlauncher.zenmode.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.zenlauncher.zenmode.ui.screens.golden
import com.zenlauncher.zenmode.widget.WidgetFixtures.Moment
import com.zenlauncher.zenmode.widget.art.Backdrop
import com.zenlauncher.zenmode.widget.faces.Face
import com.zenlauncher.zenmode.widget.faces.FaceInput
import com.zenlauncher.zenmode.widget.faces.BloomCell
import com.zenlauncher.zenmode.widget.faces.CircleFace
import com.zenlauncher.zenmode.widget.faces.GoldFace
import com.zenlauncher.zenmode.widget.faces.QuietFace
import com.zenlauncher.zenmode.widget.faces.ScoreFace
import com.zenlauncher.zenmode.widget.faces.ScreenTimeFace
import com.zenlauncher.zenmode.widget.faces.WeekFace
import com.zenlauncher.zenmode.widget.faces.ZenoFace
import org.junit.Rule
import org.junit.Test

/**
 * Goldens for the widget faces: each one at Android's real cell sizes (one row is 102dp high,
 * two are 220, four are 456; 184 and 380 wide on four columns, 130 and 276 on five), as contact
 * sheets, drawn at 2× exactly as a phone would get them.
 */
@OptIn(ExperimentalLayoutApi::class)
class WidgetFacesScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 1700,
            screenHeight = 3400,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
            softButtons = false
        ),
        maxPercentDifference = 0.1
    )

    private class Tile(
        val face: Face, val data: WidgetData, val w: Float, val h: Float, val dark: Boolean = false,
        val backdrop: Backdrop = Backdrop.LIGHT, val private: Boolean = false, val bloom: List<BloomCell>? = null
    )

    @Composable
    private fun Sheet(tiles: List<Tile>, ground: Color = Color(0xFFD8D6CE)) {
        val context = LocalContext.current
        Column(Modifier.background(ground).padding(24.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                tiles.forEach { t ->
                    val r = WidgetRenderer.render(context, t.face, FaceInput(t.data, t.w, t.h, t.dark, t.backdrop, 28f, t.private, t.bloom), density = 2f, budgetBytes = Long.MAX_VALUE)
                    Image(r.bitmap.asImageBitmap(), r.face.description, Modifier.size(t.w.dp, t.h.dp))
                }
            }
        }
    }

    @Test
    fun `zeno - across the week`() = paparazzi.golden {
        Sheet(Moment.entries.map { Tile(ZenoFace, WidgetFixtures.data(it), 184f, 220f) } +
            listOf(
                Tile(ZenoFace, WidgetFixtures.data(Moment.GOING, part = com.zenlauncher.zenmode.coreapi.week.DayPart.NIGHT), 184f, 220f, dark = true),
                Tile(ZenoFace, WidgetFixtures.data(Moment.STAKE), 184f, 220f, dark = true),
                Tile(ZenoFace, WidgetFixtures.data(quiet = WidgetFixtures.running), 184f, 220f),
                Tile(ZenoFace, WidgetFixtures.data(usageAccess = false), 184f, 220f),
                Tile(ZenoFace, WidgetFixtures.data(Moment.ONE, minutesToday = 205), 184f, 220f),
                Tile(ZenoFace, WidgetFixtures.data(pokes = 1, answering = true), 184f, 220f)
            ))
    }

    @Test
    fun `zeno - every size`() = paparazzi.golden {
        val d = WidgetFixtures.data(Moment.ONE)
        Sheet(listOf(
            Tile(ZenoFace, d, 57f, 102f), Tile(ZenoFace, d, 130f, 102f), Tile(ZenoFace, d, 184f, 102f),
            Tile(ZenoFace, d, 276f, 102f), Tile(ZenoFace, d, 380f, 102f),
            Tile(ZenoFace, d, 130f, 220f), Tile(ZenoFace, d, 276f, 220f), Tile(ZenoFace, d, 380f, 220f),
            Tile(ZenoFace, WidgetFixtures.data(Moment.WON), 380f, 220f), Tile(ZenoFace, d, 184f, 338f)
        ))
    }

    @Test
    fun `backdrops - light, glass and plain`() = paparazzi.golden {
        val d = WidgetFixtures.data(Moment.ONE)
        Sheet(Backdrop.entries.flatMap { b ->
            listOf(Tile(ZenoFace, d, 184f, 220f, backdrop = b), Tile(ZenoFace, d, 184f, 220f, dark = true, backdrop = b), Tile(WeekFace, d, 380f, 220f, backdrop = b))
        }, ground = Color(0xFF6B8F7A))
    }

    @Test
    fun `zen score - calm, drifting, over, every size`() = paparazzi.golden {
        val calm = WidgetFixtures.data(Moment.CALM)
        val drift = WidgetFixtures.data(Moment.STAKE, minutesToday = 200)
        val over = WidgetFixtures.data(Moment.OUT, minutesToday = 278, yesterday = 68)
        Sheet(listOf(
            Tile(ScoreFace, calm, 184f, 102f), Tile(ScoreFace, drift, 184f, 102f), Tile(ScoreFace, over, 130f, 102f),
            Tile(ScoreFace, calm, 380f, 102f), Tile(ScoreFace, over, 276f, 102f, dark = true),
            Tile(ScoreFace, calm, 184f, 220f), Tile(ScoreFace, drift, 184f, 220f), Tile(ScoreFace, over, 130f, 220f, dark = true),
            Tile(ScoreFace, calm, 380f, 220f), Tile(ScoreFace, over, 380f, 220f), Tile(ScoreFace, WidgetFixtures.data(Moment.WON), 380f, 220f),
            Tile(ScoreFace, calm, 380f, 220f, private = true), Tile(ScoreFace, calm, 184f, 338f), Tile(ScoreFace, WidgetFixtures.data(usageAccess = false), 184f, 220f)
        ))
    }

    @Test
    fun `this week - streak, days, week and bloom`() = paparazzi.golden {
        val going = WidgetFixtures.data(Moment.GOING)
        val bloom = List(7 * 24) { i ->
            val d = i / 24; val h = i % 24
            when {
                d > 4 || (d == 4 && h > 19) -> BloomCell.FUTURE
                d == 4 && h == 19 -> BloomCell.NOW
                h < 7 -> BloomCell.REST
                h in 9..12 && (d + h) % 3 != 0 -> BloomCell.AWAY
                h >= 21 && (d * 7 + h) % 4 == 0 -> BloomCell.UNPLANNED
                (d + h) % 3 == 0 -> BloomCell.ON_PURPOSE
                else -> BloomCell.LIGHT
            }
        }
        Sheet(listOf(
            Tile(WeekFace, going, 184f, 102f), Tile(WeekFace, going, 130f, 102f), Tile(WeekFace, going, 184f, 220f),
            Tile(WeekFace, WidgetFixtures.data(Moment.STAKE), 184f, 220f), Tile(WeekFace, WidgetFixtures.data(Moment.FIRST), 184f, 220f),
            Tile(WeekFace, going, 380f, 102f), Tile(WeekFace, going, 276f, 102f),
            Tile(WeekFace, going, 380f, 220f), Tile(WeekFace, WidgetFixtures.data(Moment.WON), 380f, 220f),
            Tile(WeekFace, WidgetFixtures.data(Moment.STAKE), 380f, 220f, dark = true), Tile(WeekFace, WidgetFixtures.data(Moment.PERFECT), 276f, 220f),
            Tile(WeekFace, WidgetFixtures.data(Moment.ONE), 380f, 456f, bloom = bloom), Tile(WeekFace, going, 276f, 338f)
        ))
    }

    @Test
    fun `screen time - on track, close, over, every size`() = paparazzi.golden {
        val week = listOf<Long?>(130, 170, 98, 76, 95, 260)
        val track = WidgetFixtures.data(Moment.CALM, minutesToday = 112).copy(lastSevenMinutes = week + 112L)
        val close = WidgetFixtures.data(Moment.ONE, minutesToday = 220).copy(lastSevenMinutes = week + 220L)
        val over = WidgetFixtures.data(Moment.OUT, minutesToday = 278).copy(lastSevenMinutes = listOf(null, null) + week.drop(2) + 278L)
        Sheet(listOf(
            Tile(ScreenTimeFace, track, 184f, 102f), Tile(ScreenTimeFace, over, 130f, 102f), Tile(ScreenTimeFace, track, 380f, 102f),
            Tile(ScreenTimeFace, over, 276f, 102f, dark = true),
            Tile(ScreenTimeFace, track, 184f, 220f), Tile(ScreenTimeFace, close, 184f, 220f), Tile(ScreenTimeFace, over, 130f, 220f, dark = true),
            Tile(ScreenTimeFace, track, 380f, 220f), Tile(ScreenTimeFace, over, 380f, 220f), Tile(ScreenTimeFace, close, 276f, 220f, dark = true),
            Tile(ScreenTimeFace, track, 380f, 456f), Tile(ScreenTimeFace, over, 380f, 456f, dark = true),
            Tile(ScreenTimeFace, track, 184f, 220f, private = true), Tile(ScreenTimeFace, WidgetFixtures.data(usageAccess = false), 184f, 220f)
        ))
    }

    @Test
    fun `quiet, gold and circle`() = paparazzi.golden {
        val d = WidgetFixtures.data(Moment.GOING)
        val open = WidgetFixtures.data(Moment.WON, gold = GoldState("2,724", invested = true, open = true, kept = 5, outOfReach = false))
        Sheet(listOf(
            Tile(QuietFace, d, 380f, 102f), Tile(QuietFace, d.copy(quiet = WidgetFixtures.running), 380f, 102f), Tile(QuietFace, d.copy(quiet = WidgetFixtures.done), 380f, 102f),
            Tile(QuietFace, d, 276f, 102f), Tile(QuietFace, d, 184f, 102f), Tile(QuietFace, d.copy(quiet = WidgetFixtures.running), 184f, 102f),
            Tile(QuietFace, d, 184f, 220f), Tile(QuietFace, d.copy(quiet = WidgetFixtures.running), 184f, 220f), Tile(QuietFace, d.copy(quiet = WidgetFixtures.done), 184f, 220f),
            Tile(QuietFace, d.copy(quiet = QuietState(false, null, null)), 184f, 220f, dark = true), Tile(QuietFace, d.copy(quiet = WidgetFixtures.running), 380f, 220f),
            Tile(GoldFace, d, 184f, 220f), Tile(GoldFace, open, 184f, 220f), Tile(GoldFace, d, 184f, 102f), Tile(GoldFace, d, 380f, 102f, dark = true),
            Tile(GoldFace, d.copy(gold = GoldState("0", invested = false, open = false, kept = 0, outOfReach = true)), 130f, 220f),
            Tile(CircleFace, d, 380f, 220f), Tile(CircleFace, d.copy(circle = WidgetFixtures.leading), 380f, 220f),
            Tile(CircleFace, d.copy(circle = CircleState.Alone("Morning people")), 380f, 220f), Tile(CircleFace, d.copy(circle = CircleState.SignedOut), 380f, 220f, dark = true),
            Tile(CircleFace, d, 184f, 220f), Tile(CircleFace, d.copy(circle = CircleState.NoCircle), 184f, 220f)
        ))
    }
}
