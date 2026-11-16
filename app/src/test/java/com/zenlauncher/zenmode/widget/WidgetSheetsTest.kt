package com.zenlauncher.zenmode.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.widget.WidgetFixtures.Moment
import com.zenlauncher.zenmode.widget.art.Backdrop
import com.zenlauncher.zenmode.widget.faces.BloomCell
import com.zenlauncher.zenmode.widget.faces.CircleFace
import com.zenlauncher.zenmode.widget.faces.Face
import com.zenlauncher.zenmode.widget.faces.FaceInput
import com.zenlauncher.zenmode.widget.faces.GoldFace
import com.zenlauncher.zenmode.widget.faces.QuietFace
import com.zenlauncher.zenmode.widget.faces.ScoreFace
import com.zenlauncher.zenmode.widget.faces.ScreenTimeFace
import com.zenlauncher.zenmode.widget.faces.WeekFace
import com.zenlauncher.zenmode.widget.faces.ZenoFace
import com.zenlauncher.zenmode.widget.glance.WidgetDemo
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Full-resolution contact sheets for design review, written to `build/widget-sheets/` (goldens
 * are downscaled; these aren't). Runs only when asked:
 * `WRITE_WIDGET_SHEETS=1 ./gradlew :app:testDebugUnitTest --tests "*WidgetSheetsTest"`.
 */
class WidgetSheetsTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private class Tile(val face: Face, val data: WidgetData, val w: Float, val h: Float, val dark: Boolean = false, val backdrop: Backdrop = Backdrop.LIGHT, val bloom: List<BloomCell>? = null)

    private fun sheet(name: String, rows: List<List<Tile>>, scale: Float = 2f, ground: Int = 0xFFD8D6CE.toInt()) {
        val gap = 20f
        val width = rows.maxOf { r -> r.sumOf { it.w.toDouble() }.toFloat() + gap * (r.size + 1) }
        val height = rows.sumOf { r -> r.maxOf { it.h }.toDouble() }.toFloat() + gap * (rows.size + 1)
        val out = Bitmap.createBitmap((width * scale).toInt(), (height * scale).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(ground)
        var y = gap
        rows.forEach { row ->
            var x = gap
            row.forEach { t ->
                val r = WidgetRenderer.render(paparazzi.context, t.face, FaceInput(t.data, t.w, t.h, t.dark, t.backdrop, 24f, bloom = t.bloom), scale, Long.MAX_VALUE)
                canvas.drawBitmap(r.bitmap, x * scale, y * scale, Paint(Paint.FILTER_BITMAP_FLAG))
                x += t.w + gap
            }
            y += row.maxOf { it.h } + gap
        }
        val dir = File("build/widget-sheets").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun write() {
        assumeTrue(System.getenv("WRITE_WIDGET_SHEETS") == "1")
        val d = WidgetFixtures.data(Moment.CALM)
        val week = listOf<Long?>(130, 170, 98, 76, 95, 260)
        val calm = d.copy(lastSevenMinutes = week + 112L)
        sheet("1-zeno", listOf(
            listOf(Tile(ZenoFace, WidgetFixtures.data(Moment.GOING, part = DayPart.MORNING), 184f, 220f), Tile(ZenoFace, d, 184f, 220f),
                Tile(ZenoFace, WidgetFixtures.data(Moment.STAKE, part = DayPart.EVENING), 184f, 220f), Tile(ZenoFace, WidgetFixtures.data(Moment.WON), 184f, 220f)),
            listOf(Tile(ZenoFace, WidgetFixtures.data(Moment.GOING, part = DayPart.NIGHT), 184f, 220f), Tile(ZenoFace, WidgetFixtures.data(Moment.PERFECT), 184f, 220f),
                Tile(ZenoFace, WidgetFixtures.data(Moment.OUT), 184f, 220f, dark = true), Tile(ZenoFace, WidgetFixtures.data(quiet = WidgetFixtures.running), 184f, 220f)),
            listOf(Tile(ZenoFace, d, 380f, 220f), Tile(ZenoFace, WidgetFixtures.data(Moment.WON), 380f, 220f)),
            listOf(Tile(ZenoFace, d, 380f, 102f), Tile(ZenoFace, d, 184f, 102f), Tile(ZenoFace, d, 57f, 102f))
        ))
        sheet("2-week-score", listOf(
            listOf(Tile(WeekFace, d, 380f, 220f), Tile(WeekFace, WidgetFixtures.data(Moment.WON), 380f, 220f)),
            listOf(Tile(WeekFace, d, 184f, 220f), Tile(WeekFace, WidgetFixtures.data(Moment.STAKE), 184f, 220f), Tile(ScoreFace, d, 184f, 220f), Tile(ScoreFace, WidgetFixtures.data(Moment.OUT, minutesToday = 278), 184f, 220f)),
            listOf(Tile(ScoreFace, d, 380f, 220f), Tile(WeekFace, d, 380f, 102f)),
            listOf(Tile(WeekFace, WidgetFixtures.data(Moment.ONE), 380f, 456f, bloom = WidgetDemo.bloom), Tile(ScreenTimeFace, calm, 380f, 456f))
        ))
        sheet("3-screen-quiet-gold-circle", listOf(
            listOf(Tile(ScreenTimeFace, calm, 380f, 220f), Tile(ScreenTimeFace, WidgetFixtures.data(Moment.OUT, minutesToday = 278).copy(lastSevenMinutes = week + 278L), 380f, 220f, dark = true)),
            listOf(Tile(ScreenTimeFace, calm, 184f, 220f), Tile(QuietFace, d, 184f, 220f), Tile(QuietFace, d.copy(quiet = WidgetFixtures.running), 184f, 220f), Tile(GoldFace, d, 184f, 220f)),
            listOf(Tile(QuietFace, d, 380f, 102f), Tile(GoldFace, WidgetFixtures.data(Moment.WON, gold = GoldState("2,724", true, true, 5, false)), 184f, 220f)),
            listOf(Tile(CircleFace, d, 380f, 220f), Tile(CircleFace, d.copy(circle = CircleState.Alone("Morning people")), 380f, 220f))
        ))
    }
}
