package com.zenlauncher.zenmode.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import com.zenlauncher.zenmode.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── The clearing ──────────────────────────────────────────────────
// A dawn forest drawn from nothing but Canvas maths: no bitmap, no video, no Lottie, so it
// costs nothing to ship, stays sharp on any screen and can be retuned right here. Painted
// back to front — sky, sun, light shafts, far ridge, three ranks of conifers on parallax,
// ground mist, then fireflies over the top.
//
// Its palette is fixed rather than themed: this is one picture, and it is always first
// light. Host it under a forced-dark ZenTheme so text and controls on top already read
// light-on-ink and the system bars come out right.
//
// Motion runs on two clocks. `reveal` is the one-shot arrival — the sun climbing, the ranks
// rising into place. Everything after is ambient and never stops: canopy sway, mist drift,
// fireflies. "Remove animations" freezes both at their settled frame.

/** The scene's fixed palette. Values are `forest_*` primitives in res/values/colors.xml; the sun
 * and its ember halo reuse the brand's amber_500 / ember_500. */
private class ForestPalette(
    val skyTop: Color,
    val skyMid: Color,
    val skyGlow: Color,
    val sun: Color,
    val sunCore: Color,
    val ember: Color,
    val ridge: Color,
    val ranks: List<Color>,
    val mist: Color,
    val ground: Color,
    val firefly: Color
)

@Composable
private fun forestPalette() = ForestPalette(
    skyTop = colorResource(R.color.forest_sky_top),
    skyMid = colorResource(R.color.forest_sky_mid),
    skyGlow = colorResource(R.color.forest_sky_glow),
    sun = colorResource(R.color.amber_500),
    sunCore = colorResource(R.color.forest_sun_core),
    ember = colorResource(R.color.ember_500),
    ridge = colorResource(R.color.forest_ridge),
    ranks = listOf(
        colorResource(R.color.forest_rank_near),
        colorResource(R.color.forest_rank_mid),
        colorResource(R.color.forest_rank_far)
    ),
    mist = colorResource(R.color.forest_mist),
    ground = colorResource(R.color.forest_ground),
    firefly = colorResource(R.color.forest_firefly)
)

/** Horizon line, as a fraction of height. Everything else is placed off this one number. */
private const val HORIZON = 0.63f

/** One conifer, in units of its rank: [x] across the width, the rest multiplying rank size. */
private class Conifer(
    val x: Float,
    val height: Float,
    val halfWidth: Float,
    val lean: Float,
    val sway: Float
)

/** One drifting light. [y] is where it starts; it rises and wraps forever. */
private class Mote(
    val x: Float,
    val y: Float,
    val speed: Float,
    val radius: Float,
    val phase: Float,
    val warmth: Float
)

/**
 * A rank of trees, same every run: the forest is a drawing, not a slot machine, and a
 * screenshot test that redraws it must get the same trees back.
 */
private fun rank(seed: Int, count: Int) = Random(seed).let { rnd ->
    List(count) { i ->
        Conifer(
            x = (i + 0.5f) / count + (rnd.nextFloat() - 0.5f) * (0.85f / count),
            height = 0.70f + rnd.nextFloat() * 0.60f,
            halfWidth = 0.78f + rnd.nextFloat() * 0.44f,
            lean = (rnd.nextFloat() - 0.5f) * 0.22f,
            sway = 0.5f + rnd.nextFloat() * 0.9f
        )
    }
}

private fun motes(seed: Int, count: Int) = Random(seed).let { rnd ->
    List(count) {
        Mote(
            x = rnd.nextFloat(),
            y = rnd.nextFloat(),
            speed = 0.35f + rnd.nextFloat() * 0.9f,
            radius = 1.1f + rnd.nextFloat() * 2.2f,
            phase = rnd.nextFloat() * 6.283f,
            warmth = rnd.nextFloat()
        )
    }
}

/**
 * Full-bleed dawn forest. Put it first in a Box with `matchParentSize()`; content drawn
 * after sits on top of it.
 */
@Composable
fun ForestScene(modifier: Modifier = Modifier) {
    val still = rememberReduceMotion() || LocalInspectionMode.current

    // One-shot arrival. Previews and reduced motion open on the settled frame.
    val reveal = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(still) {
        if (!still) reveal.animateTo(1f, tween(1_900, easing = ZenMotion.EaseOut))
    }

    // Ambient clocks, read only inside the Canvas so each frame redraws without recomposing.
    val ambient = rememberInfiniteTransition(label = "forest")
    val breeze = ambient.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(11_000, easing = LinearEasing)),
        label = "breeze"
    )
    val rise = ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing)),
        label = "rise"
    )

    val forest = forestPalette()
    val trees = remember { listOf(rank(11, 26), rank(29, 15), rank(47, 9)) }
    val lights = remember { motes(83, 30) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val r = reveal.value
        val wind = if (still) 0f else breeze.value
        val drift = if (still) 0.2f else rise.value
        sky(forest, r)
        sun(forest, r, wind)
        rays(forest, r, wind)
        canopyShade(forest, r)
        ridge(forest, r)
        ranks(forest, trees, r, wind)
        mist(forest, r, drift)
        fireflies(forest, lights, r, drift, wind)
    }
}

// ── Layers ────────────────────────────────────────────────────────

/** Night at the top, first light gathering on the horizon. The whole wash warms as it lands. */
private fun DrawScope.sky(forest: ForestPalette, reveal: Float) {
    val warm = 0.35f + 0.65f * reveal
    drawRect(
        Brush.verticalGradient(
            0f to forest.skyTop,
            0.45f to forest.skyMid,
            HORIZON to lerp(forest.skyMid, forest.skyGlow, warm),
            1f to forest.ground
        )
    )
}

/** The disc climbs out of the treeline as the screen arrives, then breathes on the breeze. */
private fun DrawScope.sun(forest: ForestPalette, reveal: Float, wind: Float) {
    val breath = 1f + 0.03f * sin(wind * 0.7f)
    val centre = Offset(size.width * 0.5f, size.height * (HORIZON + 0.10f - 0.13f * reveal))
    val halo = size.width * 1.05f * breath * (0.55f + 0.45f * reveal)
    drawCircle(
        Brush.radialGradient(
            0f to forest.sun.copy(alpha = 0.30f * reveal),
            0.35f to forest.ember.copy(alpha = 0.16f * reveal),
            1f to Color.Transparent,
            center = centre,
            radius = halo
        ),
        radius = halo,
        center = centre
    )
    val disc = size.width * 0.13f * breath
    drawCircle(
        Brush.radialGradient(
            0f to forest.sunCore.copy(alpha = 0.92f * reveal),
            0.55f to forest.sun.copy(alpha = 0.55f * reveal),
            1f to Color.Transparent,
            center = centre,
            radius = disc * 2.2f
        ),
        radius = disc * 2.2f,
        center = centre
    )
}

/** Seven shafts fanning up out of the canopy, opening as the sun rises and swaying with it. */
private fun DrawScope.rays(forest: ForestPalette, reveal: Float, wind: Float) {
    val centre = Offset(size.width * 0.5f, size.height * (HORIZON + 0.02f))
    val length = size.height * 0.72f
    val brush = Brush.radialGradient(
        0f to forest.sun.copy(alpha = 0.13f * reveal),
        0.35f to forest.sun.copy(alpha = 0.05f * reveal),
        1f to Color.Transparent,
        center = centre,
        radius = length
    )
    val count = 7
    repeat(count) { i ->
        val fan = (i - (count - 1) / 2f) * 0.26f
        val angle = -PI.toFloat() / 2f + fan * (0.4f + 0.6f * reveal) + 0.02f * sin(wind + i)
        val spread = 0.02f + 0.012f * sin(wind * 0.8f + i * 1.7f)
        val path = Path().apply {
            moveTo(centre.x, centre.y)
            lineTo(centre.x + cos(angle - spread) * length, centre.y + sin(angle - spread) * length)
            lineTo(centre.x + cos(angle + spread) * length, centre.y + sin(angle + spread) * length)
            close()
        }
        drawPath(path, brush)
    }
}

/**
 * Night settling back over the top of the frame. Without it the shafts stripe straight through
 * the headline; with it they gather at the treeline, which is where the dawn is anyway.
 */
private fun DrawScope.canopyShade(forest: ForestPalette, reveal: Float) {
    drawRect(
        Brush.verticalGradient(
            0f to forest.skyTop.copy(alpha = 0.78f),
            0.30f to forest.skyTop.copy(alpha = 0.52f),
            HORIZON - 0.08f to Color.Transparent,
            1f to Color.Transparent
        ),
        alpha = 0.55f + 0.45f * reveal
    )
}

/** The far hills: two soft humps that give the treeline something to stand in front of. */
private fun DrawScope.ridge(forest: ForestPalette, reveal: Float) {
    val base = size.height * (HORIZON + 0.015f)
    val lift = size.height * 0.05f * (1f - reveal)
    val path = Path().apply {
        moveTo(-size.width * 0.1f, base + lift)
        cubicTo(
            size.width * 0.18f, base - size.height * 0.055f + lift,
            size.width * 0.38f, base - size.height * 0.015f + lift,
            size.width * 0.56f, base - size.height * 0.038f + lift
        )
        cubicTo(
            size.width * 0.74f, base - size.height * 0.062f + lift,
            size.width * 0.90f, base - size.height * 0.010f + lift,
            size.width * 1.1f, base - size.height * 0.030f + lift
        )
        lineTo(size.width * 1.1f, size.height)
        lineTo(-size.width * 0.1f, size.height)
        close()
    }
    drawPath(path, forest.ridge.copy(alpha = 0.55f + 0.45f * reveal))
}

/** Three ranks, far to near: each darker, taller and later than the one behind it. */
private fun DrawScope.ranks(forest: ForestPalette, trees: List<List<Conifer>>, reveal: Float, wind: Float) {
    val specs = listOf(
        Triple(HORIZON + 0.055f, 0.135f, 0.030f),
        Triple(HORIZON + 0.175f, 0.225f, 0.052f),
        Triple(HORIZON + 0.400f, 0.330f, 0.085f)
    )
    specs.forEachIndexed { i, (baseFraction, heightFraction, widthFraction) ->
        // Each rank arrives on its own beat, the nearest last and from furthest down.
        val staged = ((reveal - i * 0.12f) / (1f - 0.24f)).coerceIn(0f, 1f)
        val eased = 1f - (1f - staged) * (1f - staged)
        val baseY = size.height * baseFraction + size.height * 0.12f * (i + 1) * (1f - eased)
        val height = size.height * heightFraction
        val halfWidth = size.width * widthFraction
        // Nearer ranks sway wider and slower: the same breeze, more leverage.
        val amplitude = size.width * (0.004f + 0.006f * i)
        trees[i].forEach { tree ->
            val lean = tree.lean * halfWidth +
                amplitude * tree.sway * sin(wind * (0.6f + 0.15f * i) + tree.x * 9f)
            conifer(
                x = size.width * tree.x,
                baseY = baseY,
                height = height * tree.height,
                halfWidth = halfWidth * tree.halfWidth,
                lean = lean,
                color = forest.ranks[i].copy(alpha = 0.35f + 0.65f * eased)
            )
        }
    }
}

/** One tree: four skirts of needles over a short trunk, the top tiers leaning with the wind. */
private fun DrawScope.conifer(
    x: Float,
    baseY: Float,
    height: Float,
    halfWidth: Float,
    lean: Float,
    color: Color
) {
    drawRect(
        color = color,
        topLeft = Offset(x - halfWidth * 0.09f, baseY - height * 0.16f),
        size = Size(halfWidth * 0.18f, height * 0.18f)
    )
    val tiers = 4
    val path = Path()
    repeat(tiers) { i ->
        val top = i / tiers.toFloat()
        val bottom = ((i + 1.4f) / tiers).coerceAtMost(1f)
        val apexY = baseY - height * (1f - top)
        val skirtY = baseY - height * (1f - bottom)
        val w = halfWidth * (0.30f + 0.70f * bottom)
        val tilt = lean * (1f - top) * (1f - top)
        path.moveTo(x + tilt, apexY)
        path.lineTo(x - w + tilt * 0.3f, skirtY)
        path.lineTo(x + w + tilt * 0.3f, skirtY)
        path.close()
    }
    drawPath(path, color)
}

/** Three bands of ground fog, squashed circles drifting against each other. */
private fun DrawScope.mist(forest: ForestPalette, reveal: Float, drift: Float) {
    val bands = listOf(
        Triple(HORIZON + 0.10f, 0.9f, 1f),
        Triple(HORIZON + 0.22f, 1.2f, -1f),
        Triple(HORIZON + 0.36f, 1.5f, 1f)
    )
    bands.forEachIndexed { i, (yFraction, radiusFraction, direction) ->
        val y = size.height * yFraction
        val radius = size.width * radiusFraction
        val travel = ((drift * 0.5f + i * 0.33f) % 1f) * 2f - 0.5f
        val centre = Offset(size.width * (0.5f + direction * (travel - 0.5f) * 0.5f), y)
        val alpha = (0.05f + 0.03f * i) * reveal
        withTransform({ scale(1f, 0.10f, pivot = centre) }) {
            drawCircle(
                Brush.radialGradient(
                    listOf(forest.mist.copy(alpha = alpha), Color.Transparent),
                    center = centre,
                    radius = radius
                ),
                radius = radius,
                center = centre
            )
        }
    }
}

/** Fireflies: they rise, wrap, sway on the same breeze as the trees, and twinkle out of step. */
private fun DrawScope.fireflies(forest: ForestPalette, lights: List<Mote>, reveal: Float, drift: Float, wind: Float) {
    lights.forEach { mote ->
        val y = ((mote.y - drift * mote.speed) % 1f + 1f) % 1f
        // They live in the lower two-thirds, where the trees are, and fade out as they climb.
        val centre = Offset(
            x = size.width * (mote.x + 0.02f * sin(wind * 1.4f + mote.phase)),
            y = size.height * (0.22f + y * 0.74f)
        )
        val twinkle = 0.30f + 0.70f * (0.5f + 0.5f * sin(wind * 2.3f + mote.phase * 3f))
        val fade = (1f - y).coerceIn(0f, 1f)
        val alpha = twinkle * fade * reveal
        val colour = lerp(forest.firefly, forest.sun, mote.warmth)
        val glow = mote.radius * 7f
        drawCircle(
            Brush.radialGradient(
                listOf(colour.copy(alpha = 0.22f * alpha), Color.Transparent),
                center = centre,
                radius = glow
            ),
            radius = glow,
            center = centre
        )
        drawCircle(colour.copy(alpha = 0.85f * alpha), radius = mote.radius, center = centre)
    }
}

/** Exposed for the one screen that hosts the scene, so its glass matches the ink underneath. */
val ForestGlass: Color @Composable get() = colorResource(R.color.forest_glass)
val ForestGlassLine: Color @Composable get() = colorResource(R.color.forest_glass_line)
