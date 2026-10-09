package com.zenlauncher.zenmode.share

import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zenlauncher.zenmode.recap.DayRecord
import com.zenlauncher.zenmode.recap.WeeklyRecap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * The weekly story's clip, made on a real device: MediaCodec, EGL and MediaMuxer only exist on
 * Android, so this can't run on the JVM. It checks the clip is the story at true speed — a video
 * track as long as the arrival plus its hold — with its soundtrack muxed alongside.
 */
@RunWith(AndroidJUnit4::class)
class ShareClipTest {

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
    fun weeklyClipIsTheWholeStoryAtTrueSpeedWithItsSoundtrack() {
        val art = WeeklyArt(ShareKit(context), WeeklyShare(recap))
        val out = File(context.cacheDir, "share_clip_test.mp4").apply { delete() }
        var lastProgress = 0f

        ShareClip.encode(context, art, out) { lastProgress = it }

        assertTrue("clip written", out.length() > 50_000)
        assertEquals(1f, lastProgress, 0.001f)

        val extractor = MediaExtractor().apply { setDataSource(out.path) }
        val formats = (0 until extractor.trackCount).map { extractor.getTrackFormat(it) }
        val video = formats.firstOrNull { it.getString(MediaFormat.KEY_MIME)!!.startsWith("video/") }
        val audio = formats.firstOrNull { it.getString(MediaFormat.KEY_MIME)!!.startsWith("audio/") }
        assertNotNull("video track", video)
        assertNotNull("audio track", audio)

        val expectedUs = ShareClip.durationMs(art) * 1_000L
        val videoUs = video!!.getLong(MediaFormat.KEY_DURATION)
        assertEquals("plays at true speed", expectedUs.toDouble(), videoUs.toDouble(), 150_000.0)
        val (width, height) = ShareClip.encoderSize(ShareFormat.STORY)
        assertEquals(width, video.getInteger(MediaFormat.KEY_WIDTH))
        assertEquals(height, video.getInteger(MediaFormat.KEY_HEIGHT))
        extractor.release()

        // Kept for a look from the host: adb pull <cacheDir>/share_clip_test.mp4
    }
}
