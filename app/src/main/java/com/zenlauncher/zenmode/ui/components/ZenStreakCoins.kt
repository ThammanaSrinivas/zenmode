package com.zenlauncher.zenmode.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.R
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/** The trailing slot in a streak row: the day currently being earned, or one just secured. */
enum class TodayCoin {
    /** Under budget with time still on the clock — almost banked, not at risk. */
    InProgress,

    /** The day is secured. Carries a tick as well as a colour, so the state isn't colour-only. */
    Banked
}

/**
 * The streak as struck coins: **one coin per day banked**, with today's slot at the end.
 *
 * Coins rather than an abstract row of shapes, because a coin can be counted and carries its
 * own worth — the row reads as "this is what I have put away" without a sentence explaining
 * it. The trailing slot is the day in hand: [TodayCoin.InProgress] shows it part-struck and
 * breathing gently, [TodayCoin.Banked] finishes it and stamps a tick.
 *
 * The in-progress slot is deliberately *calm*. An earlier version drew it as a dashed outline,
 * which read as an alarm — a hole in the row where a coin should be. A day that is going well
 * should not look like a warning, so it is now a real coin, simply not finished yet.
 */
@Composable
fun ZenStreakCoins(
    banked: Int,
    today: TodayCoin,
    modifier: Modifier = Modifier
) {
    val gold = colorResource(R.color.amber_500)
    val goldDeep = colorResource(R.color.amber_800)

    val still = rememberReduceMotion() || LocalInspectionMode.current
    val phase = remember { Animatable(0f) }
    LaunchedEffect(still) {
        if (still) {
            phase.snapTo(0f); return@LaunchedEffect
        }
        while (true) {
            phase.snapTo(0f)
            phase.animateTo(1f, tween(SWEEP_MS, easing = LinearEasing))
        }
    }

    val shownBanked = min(banked, MAX_COINS)
    val overflow = banked - shownBanked
    val total = shownBanked + 1

    Canvas(modifier = modifier.clearAndSetSemantics {}) {
        val slots = total + if (overflow > 0) 1.2f else 0f
        val step = size.width / slots
        val r = min(step * 0.42f, size.height * 0.46f)
        val cy = size.height / 2f

        for (i in 0 until shownBanked) {
            // A slow shimmer runs the row so the banked days read as treasure, not as ticks.
            val lit = (1f - abs(phase.value * 1.3f - 0.15f - i / total.toFloat()) / 0.12f)
                .coerceIn(0f, 1f)
            drawCoin(Offset(step * (i + 0.5f), cy), r, gold, goldDeep, lit)
        }

        val todayCentre = Offset(step * (shownBanked + 0.5f), cy)
        when (today) {
            TodayCoin.InProgress -> {
                val breath =
                    if (still) 0.5f else 0.42f + 0.22f * (0.5f + 0.5f * sin(phase.value * TWO_PI))
                // A halo instead of a hole: the day is under way, and the row says so quietly.
                drawCircle(gold, radius = r * 1.22f, center = todayCentre, alpha = breath * 0.26f)
                drawCoin(todayCentre, r, gold, goldDeep, lit = 0f, alpha = 0.42f + breath * 0.2f)
            }

            TodayCoin.Banked -> {
                drawCoin(todayCentre, r, gold, goldDeep, lit = 1f)
                drawTick(todayCentre, r, goldDeep)
            }
        }

        if (overflow > 0) {
            val cx = step * (total + 0.55f)
            for (d in 0..2) {
                drawCircle(
                    color = goldDeep,
                    radius = 1.3.dp.toPx(),
                    center = Offset(cx + d * 4.2.dp.toPx(), cy),
                    alpha = 0.6f
                )
            }
        }
    }
}

/** One struck day: lit from the upper left, with a milled rim and a catch-light. */
private fun DrawScope.drawCoin(
    centre: Offset,
    r: Float,
    gold: Color,
    goldDeep: Color,
    lit: Float,
    alpha: Float = 1f
) {
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(lightenGold(gold), gold, goldDeep),
            start = Offset(centre.x - r, centre.y - r),
            end = Offset(centre.x + r, centre.y + r)
        ),
        radius = r,
        center = centre,
        alpha = alpha
    )
    drawCircle(
        color = goldDeep,
        radius = r * 0.97f,
        center = centre,
        alpha = 0.55f * alpha,
        style = Stroke(width = 1.1.dp.toPx())
    )
    drawCircle(
        color = goldDeep,
        radius = r * 0.52f,
        center = centre,
        alpha = 0.40f * alpha,
        style = Stroke(width = 1.dp.toPx())
    )
    drawCircle(
        color = Color.White,
        radius = r * 0.30f,
        center = Offset(centre.x - r * 0.32f, centre.y - r * 0.34f),
        alpha = (0.30f + lit * 0.5f) * alpha
    )
}

/** The banked stamp. A shape, not just a hue, so the state survives without colour. */
private fun DrawScope.drawTick(centre: Offset, r: Float, colour: Color) {
    val path = Path().apply {
        moveTo(centre.x - r * 0.42f, centre.y + r * 0.02f)
        lineTo(centre.x - r * 0.12f, centre.y + r * 0.34f)
        lineTo(centre.x + r * 0.46f, centre.y - r * 0.34f)
    }
    drawPath(
        path = path,
        color = colour,
        style = Stroke(width = r * 0.22f, cap = StrokeCap.Round)
    )
}

private fun lightenGold(c: Color) = Color(
    red = c.red + (1f - c.red) * 0.40f,
    green = c.green + (1f - c.green) * 0.30f,
    blue = c.blue + (1f - c.blue) * 0.15f,
    alpha = c.alpha
)

private const val MAX_COINS = 21
private const val SWEEP_MS = 3_600
private const val TWO_PI = (2.0 * Math.PI).toFloat()
