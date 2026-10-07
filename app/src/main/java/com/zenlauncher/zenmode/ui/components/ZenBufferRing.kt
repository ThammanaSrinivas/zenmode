package com.zenlauncher.zenmode.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp

/**
 * How much of today's budget is gone, as one ring.
 *
 * This is the card's hero statistic, and it exists because the number it replaced — a
 * "00:50 / 04:00" pair — asked the reader to work out which half was remaining and which was
 * the whole, in two different units. A ring needs no arithmetic: the filled arc is the part
 * of the day already spent, and the gap is what is left. The exact figure is spelled out once,
 * in words, beside it.
 *
 * @param usedFraction how much of the budget has been spent, 0..1.
 */
@Composable
fun ZenBufferRing(
    usedFraction: Float,
    track: Color,
    used: Color,
    modifier: Modifier = Modifier
) {
    val target = usedFraction.coerceIn(0f, 1f)
    val still = rememberReduceMotion() || LocalInspectionMode.current

    // Sweeps up to today's figure once, on open. A ring that simply appears reads as a static
    // icon; one that fills reads as a measurement being taken.
    val sweep = remember { Animatable(if (still) target else 0f) }
    LaunchedEffect(target, still) {
        if (still) sweep.snapTo(target)
        else sweep.animateTo(target, tween(900, delayMillis = 120, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.115f
        val inset = stroke / 2f
        val box = Size(size.minDimension - stroke, size.minDimension - stroke)
        val topLeft = Offset(
            (size.width - size.minDimension) / 2f + inset,
            (size.height - size.minDimension) / 2f + inset
        )

        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = box,
            style = Stroke(width = stroke)
        )
        if (sweep.value > 0f) {
            drawArc(
                color = used,
                // From the top, clockwise: the direction a clock face already taught everyone.
                startAngle = -90f,
                sweepAngle = 360f * sweep.value,
                useCenter = false,
                topLeft = topLeft,
                size = box,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}
