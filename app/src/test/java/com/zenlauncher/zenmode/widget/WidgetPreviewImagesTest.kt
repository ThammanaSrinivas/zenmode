package com.zenlauncher.zenmode.widget

import android.graphics.Bitmap
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.widget.art.Backdrop
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
import java.time.LocalDate

/**
 * Writes the static picker previews (`res/drawable-nodpi/widget_preview_*.png`) that launchers
 * before Android 15 show, drawn by the widgets' own faces from the demo week, so a preview can't
 * drift from the widget. Android 15 and later draw theirs live (`providePreview`).
 *
 * Runs only when asked: `WRITE_WIDGET_PREVIEWS=1 ./gradlew :app:testDebugUnitTest --tests "*WidgetPreviewImagesTest"`.
 */
class WidgetPreviewImagesTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val previews: List<Triple<String, Face, Pair<Float, Float>>> = listOf(
        Triple("zeno", ZenoFace, 184f to 220f),
        Triple("week", WeekFace, 380f to 220f),
        Triple("screen_time", ScreenTimeFace, 380f to 220f),
        Triple("score", ScoreFace, 184f to 220f),
        Triple("quiet", QuietFace, 380f to 102f),
        Triple("gold", GoldFace, 184f to 220f),
        Triple("circle", CircleFace, 380f to 220f)
    )

    @Test
    fun writePreviews() {
        assumeTrue(System.getenv("WRITE_WIDGET_PREVIEWS") == "1")
        val data = WidgetDemo.data(LocalDate.of(2026, 11, 12))
        val out = File("src/main/res/drawable-nodpi").apply { mkdirs() }
        previews.forEach { (name, face, size) ->
            val (w, h) = size
            val input = FaceInput(data, w, h, dark = false, backdrop = Backdrop.LIGHT, radius = 28f, bloom = WidgetDemo.bloom)
            val bitmap = WidgetRenderer.render(paparazzi.context, face, input, density = 2f, budgetBytes = Long.MAX_VALUE).bitmap
            File(out, "widget_preview_$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
