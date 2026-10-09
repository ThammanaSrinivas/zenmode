package com.zenlauncher.zenmode.share

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenlauncher.zenmode.PromiseUnit
import com.zenlauncher.zenmode.ZenGoldPromiseState
import com.zenlauncher.zenmode.recap.DayRecord
import com.zenlauncher.zenmode.recap.WeeklyRecap
import com.zenlauncher.zenmode.testing.TestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import kotlin.math.abs

/**
 * The share cards on a real Android graphics stack. Screenshot tests draw them with layoutlib;
 * here the exported image is drawn by the device's own software canvas, and the in-app preview
 * by its hardware renderer — and the two have to be the same card.
 */
@RunWith(AndroidJUnit4::class)
class ShareArtDeviceTest {

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    private val today = LocalDate.of(2026, 9, 13)
    private val week = listOf(true, true, true, null, null, null, null)
    private val monday = LocalDate.of(2026, 9, 7)

    private fun arts(kit: ShareKit): List<Pair<String, ShareArt>> = listOf(
        "streak_moon" to StreakArt(kit, StreakShare(30, 4, 3, false, week, today)),
        "streak_orbit" to StreakArt(kit, StreakShare(400, 57, 5, false, week, today)),
        "score_calm" to ScoreArt(kit, ScoreShare(84, 78, 47, today)),
        "gold_earning" to GoldArt(kit, GoldShare(0, 0, earningWeek, today)),
        "week_kept" to WeeklyArt(kit, WeeklyShare(keptWeek))
    )

    @Test
    fun everyFamilyExportsAFullSizeCard() {
        arts(ShareKit(rule.activity)).forEach { (name, art) ->
            val bitmap = ShareExport.render(art)
            assertEquals(name, art.format.width, bitmap.width)
            assertEquals(name, art.format.height, bitmap.height)
            assertTrue("$name drew a picture, not a flat fill", distinctColours(bitmap) > 500)
            save(bitmap, "export_$name.png")
        }
    }

    @Test
    fun thePreviewOnScreenIsTheCardThatIsExported() {
        val art = StreakArt(ShareKit(rule.activity), StreakShare(30, 4, 3, false, week, today))
        rule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ShareArtView(art, Modifier.size(360.dp, 450.dp).testTag("card"))
            }
        }
        val preview = rule.onNodeWithTag("card").captureToImage().asAndroidBitmap()
        save(preview, "preview_streak_moon.png")
        val export = Bitmap.createScaledBitmap(ShareExport.render(art), preview.width, preview.height, true)
        val difference = meanDifference(preview, export)
        assertTrue("preview and export differ by $difference per channel", difference < 10f)
    }

    private fun distinctColours(bitmap: Bitmap): Int {
        val seen = HashSet<Int>()
        for (y in 0 until bitmap.height step 7) for (x in 0 until bitmap.width step 7) seen += bitmap.getPixel(x, y)
        return seen.size
    }

    private fun meanDifference(a: Bitmap, b: Bitmap): Float {
        var total = 0L
        var count = 0
        for (y in 0 until a.height step 3) for (x in 0 until a.width step 3) {
            val p = a.getPixel(x, y)
            val q = b.getPixel(x, y)
            total += abs(Color.red(p) - Color.red(q)) + abs(Color.green(p) - Color.green(q)) + abs(Color.blue(p) - Color.blue(q))
            count += 3
        }
        return total.toFloat() / count
    }

    /** Left in the app's cache for a look from the host: adb exec-out run-as … cat cache/share_device_test/… */
    private fun save(bitmap: Bitmap, name: String) {
        val folder = File(rule.activity.cacheDir, "share_device_test").apply { mkdirs() }
        File(folder, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val earningWeek = ZenGoldPromiseState(
        promiseHours = 4,
        units = listOf("M", "T", "W", "T", "F", "S", "S").zip(listOf(true, true, false, true, null, null, null)) { l, k -> PromiseUnit(l, k) },
        unitsKept = 3,
        unitsMissed = 1,
        unitsRemaining = 3,
        goalMet = false
    )

    private val keptWeek = WeeklyRecap(
        weekStart = monday,
        days = listOf(3.1, 4.6, 2.2, 1.2, 3.6, 5.1, 2.9).mapIndexed { i, h ->
            DayRecord(monday.plusDays(i.toLong()), (h * 60).toLong(), 4, emptyList(), 0, null)
        },
        previousWeekTotalMinutes = 1_700
    )
}
