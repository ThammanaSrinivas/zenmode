package com.zenlauncher.zenmode.island.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.coreapi.week.DayMark
import com.zenlauncher.zenmode.coreapi.week.MomentFeeling
import com.zenlauncher.zenmode.ui.components.rememberReduceMotion
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.IslandPalette
import com.zenlauncher.zenmode.ui.theme.WidgetPalette
import com.zenlauncher.zenmode.widget.art.WidgetKit
import com.zenlauncher.zenmode.widget.art.ZenoPainter
import com.zenlauncher.zenmode.widget.art.ZenoSkin
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

// The small pieces every island face is built from, in albeit's type: Geist for words,
// Departure Mono for every number, Clash Display for the one line that matters.

internal object IslandType {
    val number = TextStyle(fontFamily = DepartureMono, fontSize = 15.sp, letterSpacing = 0.04.em)
    val word = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    val eyebrow = TextStyle(fontFamily = DepartureMono, fontSize = 10.sp, letterSpacing = 0.12.em)
    val title = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
    val body = TextStyle(fontFamily = Geist, fontSize = 13.sp, lineHeight = 18.sp)
    val headline = TextStyle(fontFamily = ClashDisplay, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 25.sp)
    val big = TextStyle(fontFamily = DepartureMono, fontSize = 40.sp, letterSpacing = (-0.01).em)
}

/** Zeno, drawn by the same painter as the widgets, so he is the same Zeno everywhere. */
@Composable
internal fun ZenoGlyph(feeling: MomentFeeling, modifier: Modifier, skin: ZenoSkin = ZenoPainter.skinFor(feeling)) {
    Canvas(modifier) {
        drawIntoCanvas { c ->
            val w = size.width
            ZenoPainter.draw(c.nativeCanvas, 0f, (size.height - w * ZenoPainter.ASPECT) / 2f, w, feeling, skin)
        }
    }
}

/** albeit's light: a soft glow in the moment's hue, behind whatever sits on it. */
internal fun Modifier.glow(hue: Color, strength: Float, radius: Float = 0.75f): Modifier = drawBehind {
    if (strength <= 0f) return@drawBehind
    val r = size.minDimension * radius + size.maxDimension * 0.15f
    drawCircle(
        brush = Brush.radialGradient(listOf(hue.copy(alpha = strength), hue.copy(alpha = 0f)), center = center, radius = r),
        radius = r,
        center = center
    )
}

/** A crescent moon with a halo: a Quiet session. */
@Composable
internal fun MoonGlyph(modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension * 0.36f
        val c = center
        drawCircle(Brush.radialGradient(listOf(IslandPalette.moon.copy(alpha = 0.28f), Color.Transparent), c, r * 2.1f), r * 2.1f, c)
        val moon = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(c.x - r, c.y - r, c.x + r, c.y + r))
        }
        val bite = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(c.x - r * 0.35f, c.y - r * 1.25f, c.x + r * 1.65f, c.y + r * 0.75f))
        }
        val crescent = Path().apply { op(moon, bite, androidx.compose.ui.graphics.PathOperation.Difference) }
        drawPath(crescent, IslandPalette.moon)
    }
}

/** A charging bolt. */
@Composable
internal fun BoltGlyph(modifier: Modifier, color: Color = IslandPalette.zen) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.58f, h * 0.06f)
            lineTo(w * 0.18f, h * 0.56f)
            lineTo(w * 0.47f, h * 0.56f)
            lineTo(w * 0.40f, h * 0.94f)
            lineTo(w * 0.82f, h * 0.42f)
            lineTo(w * 0.53f, h * 0.42f)
            close()
        }
        drawPath(p, color)
    }
}

/** A phone handset, for calls. */
@Composable
internal fun PhoneGlyph(modifier: Modifier, color: Color = IslandPalette.zen) {
    Canvas(modifier) {
        val s = size.minDimension
        val p = Path().apply {
            moveTo(s * 0.22f, s * 0.12f)
            cubicTo(s * 0.30f, s * 0.10f, s * 0.36f, s * 0.14f, s * 0.40f, s * 0.22f)
            lineTo(s * 0.46f, s * 0.34f)
            cubicTo(s * 0.48f, s * 0.40f, s * 0.46f, s * 0.45f, s * 0.41f, s * 0.48f)
            lineTo(s * 0.37f, s * 0.51f)
            cubicTo(s * 0.43f, s * 0.62f, s * 0.50f, s * 0.70f, s * 0.60f, s * 0.75f)
            lineTo(s * 0.64f, s * 0.70f)
            cubicTo(s * 0.67f, s * 0.66f, s * 0.72f, s * 0.64f, s * 0.78f, s * 0.67f)
            lineTo(s * 0.88f, s * 0.73f)
            cubicTo(s * 0.94f, s * 0.77f, s * 0.95f, s * 0.84f, s * 0.92f, s * 0.89f)
            cubicTo(s * 0.86f, s * 0.97f, s * 0.75f, s * 0.98f, s * 0.64f, s * 0.93f)
            cubicTo(s * 0.42f, s * 0.84f, s * 0.20f, s * 0.62f, s * 0.10f, s * 0.38f)
            cubicTo(s * 0.05f, s * 0.26f, s * 0.10f, s * 0.15f, s * 0.22f, s * 0.12f)
            close()
        }
        drawPath(p, color)
    }
}

/** Four bars moving with the music (still when paused, on a lighter phone, or with reduced motion). */
@Composable
internal fun MusicBars(playing: Boolean, tint: Color, lite: Boolean, modifier: Modifier) {
    val reduce = rememberReduceMotion()
    val still = !playing || reduce || lite
    val levels: List<Float> = if (still) {
        listOf(0.35f, 0.55f, 0.42f, 0.28f)
    } else {
        val t = rememberInfiniteTransition(label = "music bars")
        listOf(520, 380, 610, 450).mapIndexed { i, ms ->
            val v by t.animateFloat(
                initialValue = 0.22f + i * 0.05f,
                targetValue = 0.95f - i * 0.06f,
                animationSpec = infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "bar $i"
            )
            v
        }
    }
    Canvas(modifier) {
        val n = levels.size
        val gap = size.width * 0.14f
        val bw = (size.width - gap * (n - 1)) / n
        levels.forEachIndexed { i, l ->
            val h = size.height * l.coerceIn(0.15f, 1f)
            drawRoundRect(
                color = tint,
                topLeft = Offset(i * (bw + gap), (size.height - h) / 2f),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2f)
            )
        }
    }
}

/** An app icon, artwork or avatar, rounded. */
@Composable
internal fun Picture(bitmap: Bitmap?, size: Dp, round: Dp, modifier: Modifier = Modifier, fallback: @Composable () -> Unit = {}) {
    Box(modifier.size(size).clip(RoundedCornerShape(round)), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            fallback()
        }
    }
}

/** A button on the island: filled for the main action, outlined on the raised tone otherwise. */
@Composable
internal fun IslandPill(label: String, primary: Boolean, onClick: () -> Unit, fill: Color = IslandPalette.zenFill, on: Color = IslandPalette.zenOn) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (primary) fill else IslandPalette.raised)
            .then(if (primary) Modifier else Modifier.border(1.dp, IslandPalette.track, CircleShape))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = IslandType.word, color = if (primary) on else IslandPalette.text, maxLines = 1)
    }
}

@Composable
internal fun Eyebrow(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text.uppercase(Locale.ROOT), style = IslandType.eyebrow, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/** A thin rounded progress line, lit in [color]. */
@Composable
internal fun ProgressLine(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(5.dp)) {
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(IslandPalette.track, cornerRadius = r)
        val w = size.width * fraction.coerceIn(0f, 1f)
        if (w > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color), endX = w),
                size = Size(w.coerceAtLeast(size.height), size.height),
                cornerRadius = r
            )
        }
    }
}

/** A ring that fills as a session runs (the bubble for Quiet and timers). */
@Composable
internal fun Ring(fraction: Float, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.12f
        val inset = stroke / 2f
        val arc = Size(size.width - stroke, size.height - stroke)
        drawArc(IslandPalette.track, -90f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
        drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), arc, style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

/** The week's seven cells, drawn by the widgets' own kit (the same cells as everywhere). */
@Composable
internal fun WeekCells(marks: List<DayMark>, cell: Dp, gap: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val kit = remember { WidgetKit(context) }
    Canvas(modifier.size(cell * marks.size + gap * (marks.size - 1), cell)) {
        drawIntoCanvas { c ->
            val n = c.nativeCanvas
            n.save()
            n.scale(density, density)
            kit.cells(n, marks, 0f, 0f, cell.value, gap.value, WidgetPalette.night)
            n.restore()
        }
    }
}

/** The streak flame, the widgets' own. */
@Composable
internal fun Flame(size: Dp, lit: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val kit = remember { WidgetKit(context) }
    Canvas(modifier.size(size)) {
        drawIntoCanvas { c ->
            val n = c.nativeCanvas
            n.save()
            n.scale(density, density)
            if (lit) kit.flame(n, 0f, 0f, size.value) else kit.flame(n, 0f, 0f, size.value, WidgetPalette.night.outline, WidgetPalette.night.hair)
            n.restore()
        }
    }
}

/** Two items side by side with room between them. */
@Composable
internal fun Spread(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { content() }
}

/** Wall-clock now, ticking every [everyMillis] while [running]. */
@Composable
internal fun rememberNow(running: Boolean, everyMillis: Long = 1_000L): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running, everyMillis) {
        while (running) {
            now.longValue = System.currentTimeMillis()
            delay(everyMillis - System.currentTimeMillis() % everyMillis)
        }
    }
    return now
}

internal object IslandTime {
    /** "18:42" for a countdown, "1:04:12" past an hour. */
    fun clock(millis: Long): String {
        val total = TimeUnit.MILLISECONDS.toSeconds(millis.coerceAtLeast(0L))
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%d:%02d", m, s)
    }

    /** "58m", "1h 05m". */
    fun minutes(minutes: Long): String {
        val m = minutes.coerceAtLeast(0L)
        return if (m >= 60) String.format(Locale.ROOT, "%dh %02dm", m / 60, m % 60) else "${m}m"
    }
}
