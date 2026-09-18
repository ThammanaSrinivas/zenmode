package com.zenlauncher.zenmode.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Glyph viewport — the Z mark's own (ic_zen_mark_gradient.xml). */
private const val GLYPH = 32.18f

/** The Z mark from ic_zen_mark_gradient.xml, outlined. N is the same mark mirrored. */
private const val Z_MARK =
    "M29.708 0.13C31.07 0.15 32.17 1.27 32.15 2.63L32.03 11.72C32.02 12.36 31.77 12.97 31.33 13.42L13.76 31.44" +
        "C13.3 31.91 12.66 32.18 11.99 32.18H2.47C1.11 32.18 0 31.07 0 29.71V20.66C0 20 0.26 19.37 0.72 18.91" +
        "L18.89 0.74C19.36 0.27 20.01 0.01 20.67 0.01L29.708 0.13Z" +
        "M27.97 20.3C29.53 18.75 32.18 19.85 32.18 22.04V29.72C32.18 31.08 31.08 32.18 29.72 32.18H22.04" +
        "C19.85 32.18 18.75 29.53 20.3 27.97L24.14 24.14L27.97 20.3Z" +
        "M16.04 14.11C14.97 14.11 14.11 14.98 14.11 16.04C14.11 17.1 14.97 17.96 16.04 17.96" +
        "C17.1 17.96 17.96 17.1 17.96 16.04C17.96 14.98 17.1 14.11 16.04 14.11Z" +
        "M10.14 0C12.34 0 13.44 2.66 11.88 4.21L4.21 11.88C2.66 13.44 0 12.34 0 10.14V2.46C0 1.1 1.1 0 2.46 0H10.14Z"

/** The E: a stretched hexagon with the centre dot, a chevron above and below. */
private const val E_MARK =
    "M1 16.09L7.4 10H24.78L31.18 16.09L24.78 22.18H7.4Z" +
        "M16.09 14.4C15.16 14.4 14.4 15.16 14.4 16.09C14.4 17.02 15.16 17.78 16.09 17.78" +
        "C17.02 17.78 17.78 17.02 17.78 16.09C17.78 15.16 17.02 14.4 16.09 14.4Z" +
        "M11.2 6.9L16.09 1.9L20.98 6.9Z" +
        "M11.2 25.28H20.98L16.09 30.28Z"

/** Each glyph as its separate contours, so every outline traces in step. */
private val glyphs: List<List<Path>> by lazy {
    val mirror = Matrix().apply {
        translate(GLYPH, 0f)
        scale(-1f, 1f)
    }
    listOf(
        contours(Z_MARK),
        contours(E_MARK),
        contours(Z_MARK).onEach { it.transform(mirror) }
    )
}

private fun contours(pathData: String): List<Path> =
    pathData.split("M").filter { it.isNotBlank() }.map { PathParser().parsePathString("M$it").toPath() }

/**
 * The finish line: "Congrats, you're officially entering ZenMode". The toggle flips on,
 * Z · E · N trace themselves in glow green, then home. Tap anywhere to skip ahead.
 */
@Composable
internal fun EnteringZenModeScreen(onFinished: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val inspection = LocalInspectionMode.current
    val trace = remember { List(3) { Animatable(if (inspection) 1f else 0f) } }
    val glow = remember { Animatable(if (inspection) 1f else 0f) }
    var toggledOn by remember { mutableStateOf(inspection) }
    var finished by remember { mutableStateOf(false) }

    fun finish() {
        if (!finished) {
            finished = true
            onFinished()
        }
    }
    BackHandler { finish() }

    LaunchedEffect(Unit) {
        if (inspection) return@LaunchedEffect
        delay(350)
        toggledOn = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        trace.forEachIndexed { i, anim ->
            launch {
                delay(i * 220L)
                anim.animateTo(1f, tween(1_100, easing = FastOutSlowInEasing))
            }
        }
        delay(1_550)
        glow.animateTo(1f, tween(500))
        delay(1_300)
        finish()
    }

    DarkSystemBars()
    val deep = colorResource(R.color.onboarding_celebrate_deep)
    val mid = colorResource(R.color.onboarding_celebrate_mid)
    val line = colorResource(R.color.onboarding_celebrate_line)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(mid, deep), radius = 1400f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Enter ZenMode",
                onClick = { finish() }
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.rdp)
        ) {
            Spacer(Modifier.height(48.rdp))
            Text(
                text = "Congrats, you're officially entering ZenMode",
                fontFamily = ClashDisplay,
                fontSize = 32.rsp,
                lineHeight = 36.rsp,
                letterSpacing = (-0.5).sp,
                color = Color.White,
                modifier = Modifier.staggeredEntrance(0, rise = 24.dp)
            )
            Spacer(Modifier.weight(0.6f))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                ZenToggle(on = toggledOn)
            }
            Spacer(Modifier.height(20.rdp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Z E N" },
                horizontalArrangement = Arrangement.spacedBy(24.rdp)
            ) {
                glyphs.forEachIndexed { i, glyph ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Canvas(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        ) {
                            drawGlyph(glyph, trace[i].value, glow.value, line)
                        }
                        Spacer(Modifier.height(26.rdp))
                        Text(
                            text = "ZEN"[i].toString(),
                            fontFamily = Geist,
                            fontSize = 15.rsp,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.alpha(trace[i].value)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Text(
                text = "Quiet the noise, Together.",
                fontFamily = ClashDisplay,
                fontSize = 18.rsp,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .alpha(glow.value)
            )
            Spacer(Modifier.height(8.rdp))
            Text(
                text = "Tap anywhere to enter",
                fontFamily = Geist,
                fontSize = 13.rsp,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .alpha(glow.value)
            )
            Spacer(Modifier.height(32.rdp))
        }
    }
}

/** Traces each contour up to [progress] of its length; [glow] adds a soft bloom once drawn. */
private fun DrawScope.drawGlyph(contours: List<Path>, progress: Float, glow: Float, color: Color) {
    val scale = size.minDimension / GLYPH
    val measure = PathMeasure()
    val traced = contours.map { contour ->
        measure.setPath(contour, false)
        Path().also { measure.getSegment(0f, measure.length * progress, it, true) }
    }

    withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
        val width = 0.72f
        if (glow > 0f) {
            contours.forEach {
                drawPath(it, color.copy(alpha = 0.22f * glow), style = Stroke(width * 5f, join = StrokeJoin.Round))
            }
        }
        traced.forEach {
            drawPath(
                it,
                color,
                style = Stroke(
                    width = width,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.cornerPathEffect(1.2f)
                )
            )
        }
    }
}

@Composable
private fun ZenToggle(on: Boolean) {
    val knob by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 300f), label = "toggle")
    val track = colorResource(R.color.onboarding_celebrate_toggle)
    Box(
        modifier = Modifier
            .width(76.rdp)
            .height(36.rdp)
            .clip(RoundedCornerShape(percent = 50))
            .background(lerp(Color.White.copy(alpha = 0.25f), track, knob.coerceIn(0f, 1f)))
            .semantics { contentDescription = if (on) "ZenMode on" else "ZenMode off" }
    ) {
        Box(
            modifier = Modifier
                .padding(4.rdp)
                .offset(x = (40 * knob).rdp)
                .size(28.rdp)
                .graphicsLayer { shadowElevation = 6.dp.toPx(); shape = CircleShape; clip = true }
                .background(Color.White, CircleShape)
        )
    }
}
