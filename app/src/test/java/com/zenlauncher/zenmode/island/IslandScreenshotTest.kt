package com.zenlauncher.zenmode.island

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.zenlauncher.zenmode.island.ui.CardActions
import com.zenlauncher.zenmode.island.ui.IslandSurface
import com.zenlauncher.zenmode.ui.screens.golden
import com.zenlauncher.zenmode.widget.WidgetFixtures
import com.zenlauncher.zenmode.widget.WidgetFixtures.Moment
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Goldens for the Zen Island: every pill, the bubble, and every open card, on a phone's top edge
 * (412dp wide, a centred punch hole, or a corner one), drawn by the same [IslandSurface] that ships.
 */
@OptIn(ExperimentalLayoutApi::class)
class IslandScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 1900,
            screenHeight = 3900,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
            softButtons = false
        ),
        maxPercentDifference = 0.1
    )

    // Cards print clock times ("Ends 8:06"): the same zone everywhere, so the goldens match on CI.
    private val machineZone: TimeZone = TimeZone.getDefault()

    @Before
    fun pinZone() = TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))

    @After
    fun restoreZone() = TimeZone.setDefault(machineZone)

    private val px = 2f
    private val width = 412f * px
    private val bar = 30f * px
    private val centred = IslandGeometry.of(width, bar, listOf(Cutout(width / 2 - 20f, 16f, width / 2 + 20f, 56f)), px, Placement.CAMERA)
    private val corner = IslandGeometry.of(width, bar, listOf(Cutout(36f, 16f, 76f, 56f)), px, Placement.CAMERA)

    private val now = WidgetFixtures.NOW
    private val minute = TimeUnit.MINUTES.toMillis(1)

    private val quiet = QuietItem(now - 2 * minute, now + 23 * minute)
    // Built once Paparazzi has loaded Android's graphics (a Bitmap cannot be made before).
    private val media by lazy { MediaItem("com.music", "Music", "Slow Mornings", "Quiet Rooms", art(), 0xFFF08AA0.toInt(), true, 38_000L, 214_000L, 0L, 1f) }
    private val call = CallItem("c", "Asha", now - 74_000L, null, listOf(IslandAction("Hang up", null)))
    private val ringing = CallItem("r", "Asha", now, null, listOf(IslandAction("Decline", null), IslandAction("Answer", null)), incoming = true)
    private val timer = LiveItem("t", "com.clock", "Clock", null, "Tea", "Timer", now + 252_000L, true, null, null, emptyList())
    private val delivery = LiveItem("d", "com.food", "Lunchbox", null, "Arriving at 1:20", "Your order is on its way", null, false, 0.62f, null, emptyList())
    private val message = NotePeek(
        "m", "com.chat", "Messages", null, null, false, "Asha", "Lunch at 1? I found a quiet place near the park.", now, null,
        listOf(IslandAction("Mark as read", null)), opens = true
    )
    private val lastHour = LastHourItem(48, 240, 192)

    private fun moment(m: Moment) = IslandSamples.moment(WidgetFixtures.data(m))

    private fun art(): Bitmap {
        val b = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888)
        val p = Paint().apply {
            shader = LinearGradient(0f, 0f, 112f, 112f, intArrayOf(0xFFFFB86B.toInt(), 0xFFE25C7A.toInt(), 0xFF5B3A9E.toInt()), null, Shader.TileMode.CLAMP)
        }
        Canvas(b).drawRect(0f, 0f, 112f, 112f, p)
        return b
    }

    private class Shot(val scene: IslandScene, val geometry: IslandGeometry, val height: Dp)

    @Composable
    private fun Sheet(shots: List<Shot>) {
        Column(Modifier.background(Color(0xFFD8D6CE)).padding(24.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                shots.forEach { s ->
                    Box(
                        Modifier
                            .size(412.dp, s.height)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFFEDEFE6), Color(0xFFDCE8C4))))
                    ) {
                        IslandSurface(s.scene, s.geometry, lite = false, actions = NoActions, onTap = {}, onSwipeUp = {}, onWindow = { _, _, _ -> }, nowOverride = now)
                    }
                }
            }
        }
    }

    private fun pill(main: IslandItem? = null, bubble: IslandItem? = null, peek: IslandPeek? = null, g: IslandGeometry = centred) =
        Shot(IslandScene(main = main, bubble = bubble, peek = peek), g, 60.dp)

    private fun card(item: Any, g: IslandGeometry = centred, height: Dp = 236.dp) = Shot(
        when (item) {
            is IslandPeek -> IslandScene(peek = item, open = true)
            else -> IslandScene(main = item as IslandItem, open = true)
        },
        g, height
    )

    @Test
    fun `island - every pill and the bubble`() = paparazzi.golden {
        Sheet(listOf(
            pill(moment(Moment.ONE)), pill(moment(Moment.WON)),
            pill(quiet), pill(quiet, bubble = media),
            pill(call, bubble = quiet), pill(ringing),
            pill(timer), pill(media),
            pill(lastHour), pill(peek = ChargingPeek(54)),
            pill(peek = message.copy(opens = false)), pill(peek = QuietDonePeek(25, 2)),
            pill(quiet, bubble = media, g = corner), pill(moment(Moment.STAKE), g = corner)
        ))
    }

    @Test
    fun `island - every open card`() = paparazzi.golden {
        Sheet(listOf(
            card(moment(Moment.ONE)), card(moment(Moment.WON)),
            card(moment(Moment.STAKE)), card(quiet.copy(held = 2)),
            card(media), card(message),
            card(ringing), card(call),
            card(delivery), card(timer),
            card(lastHour), card(QuietDonePeek(25, 2)),
            card(PromisePeek(4)), card(quiet, g = corner)
        ))
    }

    private object NoActions : CardActions {
        override fun open(item: Any) = Unit
        override fun send(action: IslandAction) = Unit
        override fun endQuiet() = Unit
        override fun pet() = Unit
        override fun mediaToggle() = Unit
        override fun mediaNext() = Unit
        override fun mediaPrevious() = Unit
        override fun touched() = Unit
    }
}
