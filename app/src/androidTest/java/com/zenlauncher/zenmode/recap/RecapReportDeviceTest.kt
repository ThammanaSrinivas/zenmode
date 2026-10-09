package com.zenlauncher.zenmode.recap

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zenlauncher.zenmode.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * The weekly report PDF as Android draws it. The header's "OS" must run the brand gradient
 * the way the app and the share cards do: orange at the top-left, green at the bottom-right.
 */
@RunWith(AndroidJUnit4::class)
class RecapReportDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val monday = LocalDate.of(2026, 9, 7)
    private val recap = WeeklyRecap(
        weekStart = monday,
        days = listOf(3.1, 4.6, 2.2, 1.2, 3.6, 5.1, 2.9).mapIndexed { i, h ->
            DayRecord(monday.plusDays(i.toLong()), (h * 60).toLong(), 4, emptyList(), 0, null)
        },
        previousWeekTotalMinutes = 1_700
    )

    @Test
    fun theHeaderOsRunsTheBrandGradientFromOrangeToGreen() {
        val page = renderFirstPage()
        save(page, "report_page1.png")

        // The header band holds white "ZenMode " and the gradient "OS" on dark-green zen_900:
        // the pixels both saturated and bright are the "OS". Compare its left and right edges.
        val scale = page.width / 595f
        val os = mutableListOf<Pair<Int, Int>>()
        for (y in (40 * scale).toInt()..(62 * scale).toInt()) for (x in 0 until page.width) {
            if (isGradientInk(page.getPixel(x, y))) os += x to page.getPixel(x, y)
        }
        assertTrue("found the gradient OS in the header", os.size > 50)
        val xs = os.map { it.first }.sorted()
        val leftEdge = xs[xs.size / 8]
        val rightEdge = xs[xs.size * 7 / 8]
        val orange = ContextCompat.getColor(context, R.color.score_orange)
        val green = ContextCompat.getColor(context, R.color.score_grad_start)
        val left = os.filter { it.first <= leftEdge }.map { it.second }
        val right = os.filter { it.first >= rightEdge }.map { it.second }
        assertTrue("left of the OS leans orange", meanDistance(left, orange) < meanDistance(left, green))
        assertTrue("right of the OS leans green", meanDistance(right, green) < meanDistance(right, orange))
    }

    private fun renderFirstPage(): Bitmap {
        val file = File(context.cacheDir, "report_device_test.pdf")
        file.outputStream().use { RecapReport.write(context, recap, it) }
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                renderer.openPage(0).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap
                }
            }
        }
    }

    /** Brand-gradient ink: saturated (not the white text's edges) and bright (not zen_900). */
    private fun isGradientInk(pixel: Int): Boolean {
        val hsv = FloatArray(3)
        Color.colorToHSV(pixel, hsv)
        return hsv[1] > 0.5f && hsv[2] > 0.5f
    }

    private fun meanDistance(pixels: List<Int>, target: Int): Float = pixels.map { p ->
        Math.abs(Color.red(p) - Color.red(target)) + Math.abs(Color.green(p) - Color.green(target)) +
            Math.abs(Color.blue(p) - Color.blue(target))
    }.average().toFloat()

    /** Left in the app's cache for a look from the host: adb exec-out run-as … cat cache/report_device_test/… */
    private fun save(bitmap: Bitmap, name: String) {
        val folder = File(context.cacheDir, "report_device_test").apply { mkdirs() }
        File(folder, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
