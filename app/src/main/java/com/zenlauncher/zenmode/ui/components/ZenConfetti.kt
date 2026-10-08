package com.zenlauncher.zenmode.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * The celebration burst behind the daily check-in card when a promise was kept
 * (see ZenCheckInOverlay).
 *
 * Drawn procedurally on one [Canvas] rather than pulled in as a dependency — the same
 * approach the milestone dot grid and the forest scene already take, and it keeps the
 * palette on the brand's own three gradient stops (ember → amber → zen glow) instead of
 * a library's rainbow.
 *
 * Paper tumbles rather than spins: each piece's width is scaled by the cosine of its
 * rotation, which reads as a flip without any matrix work per frame.
 */
@Composable
fun ZenConfetti(
    play: Boolean,
    modifier: Modifier = Modifier,
    pieceCount: Int = DEFAULT_PIECE_COUNT
) {
    // Honour the system "Remove animations" setting: with animations off a silent, still
    // card is the right outcome, not a frozen heap of paper mid-fall. Previews likewise draw
    // nothing rather than a single arbitrary frame.
    val animationsOn = !rememberReduceMotion() && !LocalInspectionMode.current

    val palette = listOf(
        colorResource(R.color.os_grad_ember),
        colorResource(R.color.os_grad_amber),
        colorResource(R.color.os_grad_glow),
        colorResource(R.color.zen_700)
    )

    // Fixed seed: the same celebration every time, so a screenshot or a bug report is
    // reproducible and nothing is recomputed on recomposition.
    val pieces = remember(pieceCount, palette) {
        val random = Random(SEED)
        List(pieceCount) {
            Piece(
                startX = random.nextFloat(),
                delay = random.nextFloat() * MAX_DELAY_FRACTION,
                fallScale = 0.8f + random.nextFloat() * 0.5f,
                width = 5f + random.nextFloat() * 6f,
                aspect = 0.45f + random.nextFloat() * 1.1f,
                driftAmplitude = 0.02f + random.nextFloat() * 0.06f,
                driftPhase = random.nextFloat() * TWO_PI,
                driftFrequency = 1.5f + random.nextFloat() * 2.5f,
                spins = 1.5f + random.nextFloat() * 3.5f,
                color = palette[random.nextInt(palette.size)]
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(play, animationsOn) {
        if (play && animationsOn) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        } else {
            progress.snapTo(0f)
        }
    }

    if (!animationsOn) return

    Canvas(modifier = modifier.clearAndSetSemantics {}) {
        val overall = progress.value
        if (overall <= 0f) return@Canvas

        for (piece in pieces) {
            // Each piece runs its own 0..1 life inside the overall sweep, so they don't all
            // leave the top edge on the same frame.
            val span = 1f - piece.delay
            val local = ((overall - piece.delay) / span).coerceIn(0f, 1f)
            if (local <= 0f) continue

            val pieceWidth = piece.width.dp.toPx()
            val pieceHeight = pieceWidth * piece.aspect

            // Gravity: a mild power curve, so paper accelerates instead of sliding down at
            // a constant rate.
            val travel = local.pow(GRAVITY_EXPONENT) * piece.fallScale
            val y = -pieceHeight + travel * (size.height + pieceHeight * 2f)
            if (y > size.height) continue

            val drift = sin(piece.driftPhase + local * piece.driftFrequency * TWO_PI) * piece.driftAmplitude
            val x = (piece.startX + drift) * size.width

            // Tumble: |cos| of the spin angle squeezes the face to a sliver edge-on.
            val spin = cos(local * piece.spins * TWO_PI)
            val faceWidth = (pieceWidth * abs(spin)).coerceAtLeast(MIN_FACE_PX)

            val alpha = when {
                local < FADE_IN_FRACTION -> local / FADE_IN_FRACTION
                local > FADE_OUT_FROM -> ((1f - local) / (1f - FADE_OUT_FROM)).coerceIn(0f, 1f)
                else -> 1f
            }

            drawRoundRect(
                color = piece.color,
                topLeft = Offset(x - faceWidth / 2f, y),
                size = Size(faceWidth, pieceHeight),
                cornerRadius = CornerRadius(1.5.dp.toPx()),
                alpha = alpha
            )
        }
    }
}

private class Piece(
    val startX: Float,
    val delay: Float,
    val fallScale: Float,
    /** In dp, converted per-frame inside the draw scope. */
    val width: Float,
    val aspect: Float,
    val driftAmplitude: Float,
    val driftPhase: Float,
    val driftFrequency: Float,
    val spins: Float,
    val color: Color
)

private const val SEED = 2137 // the streaks overlay's Figma node, for no reason but memory
private const val DEFAULT_PIECE_COUNT = 72
private const val DURATION_MS = 3_400
private const val MAX_DELAY_FRACTION = 0.35f
private const val GRAVITY_EXPONENT = 1.35f
private const val FADE_IN_FRACTION = 0.06f
private const val FADE_OUT_FROM = 0.78f
private const val MIN_FACE_PX = 1.5f
private const val TWO_PI = (2.0 * Math.PI).toFloat()
