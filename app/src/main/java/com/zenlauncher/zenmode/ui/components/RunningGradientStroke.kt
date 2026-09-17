package com.zenlauncher.zenmode.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

// ── Running gradient stroke ───────────────────────────────────────
// A rounded-rect outline whose gradient runs *along the stroke*, not across the
// bounds. A Brush can't do that (sweep gradients crawl on the short ends of a wide
// pill and race along the long edges), so the outline is sampled into short
// segments at a fixed pixel pitch and each one is coloured by its distance round
// the perimeter. That keeps the flow speed constant all the way round.
//
// The perimeter starts at the left-centre (where the search glyph sits) and runs
// down → along the bottom → up the right → back along the top. With phase = 0 the
// cyclic stops land exactly as Figma node 2026:1211 draws them: ember on the left,
// amber along the bottom, glow green on the right.
//
// All three animated inputs are lambdas so reading them only invalidates the draw
// phase — the running stroke never recomposes the search bar.

/**
 * @param colors cyclic gradient stops, evenly spaced; the last blends back into the first.
 * @param trace 0..1 — how much of the outline is drawn. Grows from the left-centre in
 *   both directions at once, so the two heads meet on the right.
 * @param phase 0..1 — how far the gradient has run round the perimeter.
 * @param glow 0..1 — strength of the soft halo and the travelling sheen.
 */
fun Modifier.runningGradientStroke(
    colors: List<Color>,
    strokeWidth: Dp,
    trace: () -> Float,
    phase: () -> Float,
    glow: () -> Float,
    sheenColor: Color = Color.White
): Modifier = drawWithCache {
    val stroke = strokeWidth.toPx()
    val inset = stroke / 2f
    val bounds = Rect(inset, inset, size.width - inset, size.height - inset)
    val path = pillOutline(bounds, radius = bounds.height / 2f)

    val measure = PathMeasure().apply { setPath(path, forceClosed = false) }
    val length = measure.length
    val segments = (length / 2.dp.toPx()).toInt().coerceIn(MinSegments, MaxSegments)
    val points = Array(segments + 1) { measure.getPosition(length * it / segments) }
    val lut = cyclicLut(colors)

    onDrawWithContent {
        drawContent()
        if (length <= 0f) return@onDrawWithContent

        val t = trace().coerceIn(0f, 1f)
        if (t <= 0f) return@onDrawWithContent
        val p = phase()
        val g = glow().coerceIn(0f, 1f)

        fun colorAt(fraction: Float): Color {
            val f = ((fraction - p) % 1f + 1f) % 1f
            return lut[(f * (LutSize - 1)).toInt()]
        }

        fun visible(fraction: Float) = min(fraction, 1f - fraction) <= t / 2f

        fun haloPass(width: Float, alpha: Float) {
            val bleed = width
            drawContext.canvas.saveLayer(
                Rect(-bleed, -bleed, size.width + bleed, size.height + bleed),
                Paint().apply { this.alpha = alpha }
            )
            for (i in 0 until segments step HaloStride) {
                val f = i.toFloat() / segments
                if (!visible(f)) continue
                val end = points[min(i + HaloStride, segments)]
                drawLine(colorAt(f), points[i], end, width, StrokeCap.Round)
            }
            drawContext.canvas.restore()
        }

        // halo — four widening passes. Translucent round-capped segments would stack
        // alpha wherever neighbours overlap and read as a string of beads, so each
        // pass is drawn opaque into its own layer and faded as a whole.
        if (g > 0f) {
            // widest and faintest first; stacked they fall off smoothly, not in bands
            haloPass(stroke * 8f, 0.045f * g)
            haloPass(stroke * 6f, 0.06f * g)
            haloPass(stroke * 4.2f, 0.09f * g)
            haloPass(stroke * 2.6f, 0.14f * g)
        }

        // the stroke itself
        for (i in 0 until segments) {
            val f = i.toFloat() / segments
            if (!visible(f)) continue
            drawLine(colorAt(f), points[i], points[i + 1], stroke, StrokeCap.Round)
        }

        // sheen — a short bright glint riding the gradient, the "running" read
        if (g > 0f) {
            val head = ((0.5f + p) % 1f + 1f) % 1f
            for (i in 0 until segments) {
                val f = i.toFloat() / segments
                if (!visible(f)) continue
                var d = abs(f - head)
                d = min(d, 1f - d)
                if (d > SheenReach) continue
                val falloff = exp(-(d * d) / (2f * SheenSigma * SheenSigma))
                drawLine(
                    sheenColor.copy(alpha = 0.55f * g * falloff),
                    points[i],
                    points[i + 1],
                    stroke,
                    StrokeCap.Butt
                )
            }
        }
    }
}

private const val MinSegments = 64
private const val MaxSegments = 480
private const val HaloStride = 3
private const val LutSize = 256
private const val SheenSigma = 0.025f
private const val SheenReach = 0.08f

/** Left-centre start, counter-clockwise on screen: left → bottom → right → top. */
private fun pillOutline(r: Rect, radius: Float): Path = Path().apply {
    val rad = min(radius, min(r.width, r.height) / 2f)
    val d = rad * 2f
    moveTo(r.left, r.center.y)
    lineTo(r.left, r.bottom - rad)
    arcTo(Rect(r.left, r.bottom - d, r.left + d, r.bottom), 180f, -90f, false)
    lineTo(r.right - rad, r.bottom)
    arcTo(Rect(r.right - d, r.bottom - d, r.right, r.bottom), 90f, -90f, false)
    lineTo(r.right, r.top + rad)
    arcTo(Rect(r.right - d, r.top, r.right, r.top + d), 0f, -90f, false)
    lineTo(r.left + rad, r.top)
    arcTo(Rect(r.left, r.top, r.left + d, r.top + d), 270f, -90f, false)
    lineTo(r.left, r.center.y)
}

/** Evenly spaced cyclic stops, interpolated in Oklab (Compose's lerp) so ember → amber
 *  → green stays luminous instead of going muddy through the middle. */
private fun cyclicLut(stops: List<Color>): Array<Color> {
    require(stops.isNotEmpty())
    val n = stops.size
    return Array(LutSize) { i ->
        val x = i.toFloat() / LutSize * n
        val a = x.toInt() % n
        lerp(stops[a], stops[(a + 1) % n], x - x.toInt())
    }
}
