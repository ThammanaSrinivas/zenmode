package com.zenlauncher.zenmode.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ZenScore
import com.zenlauncher.zenmode.ui.components.FanMember
import com.zenlauncher.zenmode.ui.components.MemberCardFan
import com.zenlauncher.zenmode.ui.components.rememberBrandOsGradient
import com.zenlauncher.zenmode.ui.components.rememberStoryState
import com.zenlauncher.zenmode.ui.components.storyGestures
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 02 · Value props, told as stories: Zen Bro & Gang, Zen Score, Zen Gold. Auto-advances
 * like Revolut's intro — tap the right side to skip ahead, the left to go back, hold to
 * pause. The one dark screen in the flow, so the visuals glow.
 */
@Composable
internal fun StoriesStep(
    progress: StepProgress,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val stories = OnboardingContent.stories
    val story = rememberStoryState(stories.size)
    val index = story.index

    val glow by animateColorAsState(
        when (index) {
            0 -> colorResource(R.color.zen_500)
            1 -> colorResource(R.color.ember_500)
            else -> colorResource(R.color.amber_500)
        },
        tween(700),
        label = "storyGlow"
    )
    val night = colorResource(R.color.onboarding_night)
    DarkSystemBars()

    OnboardingPage(
        background = night,
        modifier = Modifier.background(night),
        topBar = {
            OnboardingTopBar(
                segments = stories.size,
                currentIndex = index,
                currentFraction = story.fraction,
                onBack = onBack,
                dark = true,
                // The bar here counts stories; the percentage still counts the whole flow.
                percent = progress.percent((index + story.fraction) / stories.size)
            )
        },
        bottomBar = {
            OnboardingButton(
                text = "Let's get started",
                onClick = onContinue,
                style = OnboardingButtonStyle.Light
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = 0.32f), Color.Transparent),
                        center = Offset.Unspecified,
                        radius = 900f
                    )
                )
                .storyGestures(story)
        ) {
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    (fadeIn(tween(450)) + scaleIn(tween(450), initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(200)))
                },
                label = "story"
            ) { i ->
                StoryPage(story = stories[i], index = i, accent = glow)
            }
        }
    }

}

@Composable
private fun StoryPage(story: OnboardingContent.Story, index: Int, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingMargin)
    ) {
        Spacer(Modifier.height(8.rdp))
        OnboardingEyebrow(story.eyebrow, color = accent)
        Spacer(Modifier.height(10.rdp))
        OnboardingHeadline(text = story.headline, color = Color.White, size = 36f)
        Spacer(Modifier.height(12.rdp))
        OnboardingBody(text = story.body, color = Color.White.copy(alpha = 0.72f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (index) {
                0 -> CircleVisual()
                1 -> ScoreVisual()
                else -> GoldVisual()
            }
        }
    }
}

// ── Visuals ───────────────────────────────────────────────────────

/**
 * The Zen Circle leaderboard from the Figma "Sharable" card (node 74:8055): members'
 * mood cards fanned out in an arc, the day's leader in front, and the ranking strip below.
 */
@Composable
private fun CircleVisual() {
    // Leader first. Sample people: this is the pitch, before the user has a circle.
    val ranked = remember {
        listOf(
            FanMember("Anjali", 42, 91, 13),
            FanMember("Ajay", 104, 79, 4),
            FanMember("Priya", 88, 84, 6),
            FanMember("Raju", 175, 66, 2),
            FanMember("Kamal", 150, 71, 3)
        )
    }
    val leader = ranked.first()
    val spread = remember { Animatable(0f) }
    LaunchedEffect(Unit) { spread.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 110f)) }
    val transition = rememberInfiniteTransition(label = "fan")
    val phase by transition.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(4_200, easing = LinearEasing)),
        label = "float"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics(mergeDescendants = true) {
                contentDescription = "Your Zen Circle leaderboard. ${leader.name} leads with a Zen Score of " +
                    ZenScore.format(leader.zenScore)
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MemberCardFan(
            ranked = ranked,
            spread = spread.value,
            floatPhase = phase,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Spacer(Modifier.height(12.rdp))
        LeaderboardStrip(leader)
        Spacer(Modifier.height(8.rdp))
    }
}

/** "Ranking ▲#01 · Zen Bro · Zen Score 9.1", the strip under the Figma fan. */
@Composable
private fun LeaderboardStrip(leader: FanMember) {
    val line = Color.White.copy(alpha = 0.14f)
    val green = colorResource(R.color.zen_300)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.rdp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, line, RoundedCornerShape(16.rdp))
            .padding(vertical = 10.rdp)
    ) {
        StripCell("Ranking", Modifier.weight(1f)) {
            Text("▲ #01", fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 16.rsp, color = green)
        }
        StripCell("Zen Bro", Modifier.weight(1f)) {
            Text(leader.name, fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 16.rsp, color = green, maxLines = 1)
        }
        StripCell("Zen Score", Modifier.weight(1f)) {
            Text(
                ZenScore.format(leader.zenScore),
                fontFamily = DepartureMono,
                fontSize = 18.rsp,
                style = TextStyle(brush = rememberBrandOsGradient())
            )
        }
    }
}

@Composable
private fun StripCell(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontFamily = Geist, fontSize = 12.rsp, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
        Spacer(Modifier.height(2.rdp))
        value()
    }
}

/** A gauge sweeping to today's score out of 10 in the OS gradient, number rolling up with it. */
@Composable
private fun ScoreVisual() {
    val ember = colorResource(R.color.os_grad_ember)
    val amber = colorResource(R.color.os_grad_amber)
    val glow = colorResource(R.color.os_grad_glow)
    val target = 93 // tenths: 9.3 of 10
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) { sweep.animateTo(target / ZenScore.MAX_TENTHS.toFloat(), tween(1_800, easing = FastOutSlowInEasing)) }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .aspectRatio(1f)
            .semantics { contentDescription = "Zen Score ${ZenScore.format(target)} out of ${ZenScore.MAX_DISPLAY}" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2 + 8.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(Color.White.copy(alpha = 0.08f), 135f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            rotate(135f) {
                drawArc(
                    // Stops span the 270° arc (0.75 of a turn); the tail wraps back to ember so the
                    // round start cap doesn't pick up the end colour.
                    Brush.sweepGradient(0f to ember, 0.375f to amber, 0.75f to glow, 0.85f to glow, 1f to ember, center = center),
                    0f, 270f * sweep.value, false, topLeft, arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            // One major tick per point on the 0–10 dial, minor ticks between.
            for (t in 0..20) {
                val a = Math.toRadians((135 + t * 13.5).toDouble())
                val major = t % 2 == 0
                val r1 = size.minDimension / 2 - inset - stroke - 6.dp.toPx()
                val r2 = r1 - if (major) 10.dp.toPx() else 5.dp.toPx()
                drawLine(
                    Color.White.copy(alpha = if (major) 0.35f else 0.14f),
                    Offset(center.x + (r1 * cos(a)).toFloat(), center.y + (r1 * sin(a)).toFloat()),
                    Offset(center.x + (r2 * cos(a)).toFloat(), center.y + (r2 * sin(a)).toFloat()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = ZenScore.format((sweep.value * ZenScore.MAX_TENTHS).roundToInt()),
                    fontFamily = DepartureMono,
                    fontSize = 64.rsp,
                    letterSpacing = (-2).sp,
                    color = Color.White
                )
                Text(
                    text = "/${ZenScore.MAX_DISPLAY}",
                    fontFamily = DepartureMono,
                    fontSize = 20.rsp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 10.rdp)
                )
            }
            Text(
                text = "Zen Score",
                fontFamily = DepartureMono,
                fontSize = 12.rsp,
                letterSpacing = 1.4.sp,
                color = glow
            )
            Spacer(Modifier.height(4.rdp))
            Text(
                text = "▲ 0.4 vs yesterday",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 13.rsp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Gold ──────────────────────────────────────────────────────────

private class CoinPalette(
    val highlight: Color,
    val light: Color,
    val face: Color,
    val deep: Color,
    val edge: Color,
    val shadow: Color
)

/**
 * Minted gold coins dropping onto a stack, one per promise kept, with a coin spinning
 * above it. Each coin has a reeded edge, a bevelled rim, the Z mark struck in relief
 * and a highlight that sweeps across the metal.
 */
@Composable
private fun GoldVisual() {
    val palette = CoinPalette(
        highlight = colorResource(R.color.coin_gold_highlight),
        light = colorResource(R.color.coin_gold_light),
        face = colorResource(R.color.coin_gold_face),
        deep = colorResource(R.color.coin_gold_deep),
        edge = colorResource(R.color.coin_gold_edge),
        shadow = colorResource(R.color.coin_gold_shadow)
    )
    val glow = colorResource(R.color.amber_500)
    val emblem = remember { PathParser().parsePathString(Z_MARK).toPath() }

    val transition = rememberInfiniteTransition(label = "coins")
    val bob by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    // Runs past 1 so the highlight rests off the coin between sweeps.
    val shine by transition.animateFloat(
        -0.4f, 1.8f,
        infiniteRepeatable(tween(3_400, easing = LinearEasing)),
        label = "shine"
    )
    val spin by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(3_600, easing = LinearEasing)),
        label = "spin"
    )
    val twinkle by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1_300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "twinkle"
    )
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, tween(1_600, easing = FastOutSlowInEasing)) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .aspectRatio(1f)
            .semantics { contentDescription = "Gold coins stacking up as you keep your promise" }
    ) {
        val coinW = size.width * 0.56f
        val coinH = coinW * 0.36f
        val thickness = coinH * 0.30f
        val count = 6
        val baseY = size.height * 0.9f
        val jitter = listOf(-4, 5, -2, 6, -5, 2)

        // Warm light pooling under the stack.
        drawOval(
            Brush.radialGradient(
                listOf(glow.copy(alpha = 0.32f), Color.Transparent),
                Offset(center.x, baseY - coinH * 0.2f),
                coinW
            ),
            topLeft = Offset(center.x - coinW, baseY - coinH * 0.9f),
            size = Size(coinW * 2, coinH * 1.6f)
        )
        drawOval(
            Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(center.x - coinW * 0.55f, baseY - coinH * 0.45f),
            size = Size(coinW * 1.1f, coinH * 0.9f)
        )

        var stackTop = baseY - coinH
        for (i in 0 until count) {
            val shown = (rise.value * count - i).coerceIn(0f, 1f)
            if (shown <= 0f) continue
            val drop = (1f - shown) * 90.dp.toPx()
            val wobble = if (i == count - 1) bob * 3.dp.toPx() * rise.value else 0f
            val y = baseY - coinH - thickness - i * thickness - drop - wobble
            val x = center.x - coinW / 2 + jitter[i].dp.toPx()
            drawStackedCoin(Offset(x, y), Size(coinW, coinH), thickness, palette, emblem, shine - i * 0.12f, shown)
            stackTop = y
        }

        // The coin being earned: spins above the stack once it's built.
        val heroAlpha = ((rise.value - 0.75f) / 0.25f).coerceIn(0f, 1f)
        if (heroAlpha > 0f) {
            val d = coinW * 0.62f
            val heroCenter = Offset(center.x, stackTop - d * 0.72f - bob * 10.dp.toPx())
            drawSpinningCoin(heroCenter, d, spin, palette, emblem, shine, heroAlpha)

            // Glints around it.
            val glints = listOf(Offset(-0.62f, -0.35f), Offset(0.66f, -0.1f), Offset(-0.5f, 0.55f), Offset(0.55f, 0.62f))
            glints.forEachIndexed { k, g ->
                val a = ((if (k % 2 == 0) twinkle else 1f - twinkle) * heroAlpha).coerceIn(0f, 1f)
                drawGlint(Offset(heroCenter.x + g.x * d, heroCenter.y + g.y * d), (if (k % 2 == 0) 7 else 5).dp.toPx(), palette.highlight.copy(alpha = a))
            }
        }
    }
}

/** A coin lying flat: reeded edge first, then the face on top. */
private fun DrawScope.drawStackedCoin(
    topLeft: Offset,
    size: Size,
    thickness: Float,
    p: CoinPalette,
    emblem: Path,
    shine: Float,
    alpha: Float
) {
    val w = size.width
    val h = size.height
    val cx = topLeft.x + w / 2
    val cy = topLeft.y + h / 2
    val top = Rect(topLeft, size)
    val bottom = top.translate(0f, thickness)

    // Edge: the band between the face's lower half and the same half one thickness down.
    val edge = Path().apply {
        moveTo(top.left, cy)
        lineTo(bottom.left, cy + thickness)
        arcTo(bottom, 180f, -180f, false)
        lineTo(top.right, cy)
        arcTo(top, 0f, 180f, false)
        close()
    }
    drawPath(
        edge,
        Brush.horizontalGradient(
            0f to p.shadow, 0.14f to p.edge, 0.36f to p.light, 0.5f to p.face, 0.78f to p.deep, 1f to p.shadow,
            startX = top.left, endX = top.right
        ),
        alpha = alpha
    )
    // Reeding: ridges bunch up toward the sides as the edge curves away.
    val ridges = 44
    for (k in 1 until ridges) {
        val theta = PI * k / ridges
        val x = cx - (w / 2) * cos(theta).toFloat()
        val yTop = cy + (h / 2) * sin(theta).toFloat()
        drawLine(p.shadow.copy(alpha = 0.32f * alpha), Offset(x, yTop), Offset(x, yTop + thickness), strokeWidth = 0.8.dp.toPx())
    }
    drawCoinFace(top, p, emblem, shine, alpha)
}

/** A coin on its edge turning about its vertical axis. */
private fun DrawScope.drawSpinningCoin(
    center: Offset,
    diameter: Float,
    spinDegrees: Float,
    p: CoinPalette,
    emblem: Path,
    shine: Float,
    alpha: Float
) {
    val rad = Math.toRadians(spinDegrees.toDouble())
    val squeeze = abs(cos(rad)).toFloat().coerceAtLeast(0.04f)
    val faceW = diameter * squeeze
    val depth = diameter * 0.085f * sin(rad).toFloat()
    val face = Rect(Offset(center.x - faceW / 2, center.y - diameter / 2), Size(faceW, diameter))

    // Thickness: the edge swept sideways behind the face.
    val layers = 8
    for (l in layers downTo 1) {
        val shift = depth * l / layers
        drawOval(
            Brush.verticalGradient(listOf(p.deep, p.edge, p.shadow), startY = face.top, endY = face.bottom),
            topLeft = face.topLeft + Offset(shift, 0f),
            size = face.size,
            alpha = alpha
        )
    }
    drawCoinFace(face, p, emblem, shine, alpha)
}

/** Face, bevelled rim, recessed field, the Z mark in relief and a sweeping highlight. */
private fun DrawScope.drawCoinFace(face: Rect, p: CoinPalette, emblem: Path, shine: Float, alpha: Float) {
    val w = face.width
    val h = face.height
    drawOval(
        Brush.linearGradient(listOf(p.highlight, p.light, p.face, p.deep), face.topLeft, face.bottomRight),
        face.topLeft, face.size, alpha = alpha
    )
    // Rim: a dark groove with a lit lip just inside it.
    val rim = face.inset(0.07f)
    drawOval(p.edge.copy(alpha = 0.55f * alpha), rim.topLeft, rim.size, style = Stroke(w * 0.016f))
    val lip = face.inset(0.095f)
    drawOval(p.highlight.copy(alpha = 0.6f * alpha), lip.topLeft + Offset(0f, h * 0.012f), lip.size, style = Stroke(w * 0.01f))
    // Field: lit from the opposite side to the face, so it reads as sunk.
    val field = face.inset(0.13f)
    drawOval(
        Brush.linearGradient(listOf(p.deep.copy(alpha = 0.55f), p.face.copy(alpha = 0.2f), p.light.copy(alpha = 0.5f)), field.topLeft, field.bottomRight),
        field.topLeft, field.size, alpha = alpha
    )

    // The Z mark, struck in relief: shadow below, lit face on top.
    val markW = w * 0.36f
    val sx = markW / GLYPH
    val sy = sx * (h / w)
    val markTopLeft = Offset(face.center.x - markW / 2, face.center.y - GLYPH * sy / 2)
    withTransform({
        translate(markTopLeft.x, markTopLeft.y + h * 0.018f)
        scale(sx, sy, pivot = Offset.Zero)
    }) { drawPath(emblem, p.shadow.copy(alpha = 0.55f * alpha)) }
    withTransform({
        translate(markTopLeft.x, markTopLeft.y)
        scale(sx, sy, pivot = Offset.Zero)
    }) {
        drawPath(emblem, Brush.linearGradient(listOf(p.highlight, p.light, p.face), Offset.Zero, Offset(GLYPH, GLYPH)), alpha = alpha)
    }

    // Specular sweep, clipped to the face.
    if (shine in -0.3f..1.3f) {
        val clip = Path().apply { addOval(face) }
        clipPath(clip) {
            val x = face.left + shine * w
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.55f * alpha), Color.Transparent),
                    Offset(x - w * 0.22f, face.top),
                    Offset(x + w * 0.22f, face.bottom)
                ),
                face.topLeft, face.size
            )
        }
    }
    // Thin dark outline so stacked faces separate.
    drawOval(p.edge.copy(alpha = 0.6f * alpha), face.topLeft, face.size, style = Stroke(0.8.dp.toPx()))
}

/** Shrinks an oval's box by [fraction] of its own width/height on every side. */
private fun Rect.inset(fraction: Float): Rect =
    Rect(left + width * fraction, top + height * fraction, right - width * fraction, bottom - height * fraction)

/** Four-point sparkle. */
private fun DrawScope.drawGlint(at: Offset, radius: Float, color: Color) {
    if (color.alpha <= 0f) return
    val waist = radius * 0.22f
    val star = Path().apply {
        moveTo(at.x, at.y - radius)
        quadraticTo(at.x + waist, at.y - waist, at.x + radius, at.y)
        quadraticTo(at.x + waist, at.y + waist, at.x, at.y + radius)
        quadraticTo(at.x - waist, at.y + waist, at.x - radius, at.y)
        quadraticTo(at.x - waist, at.y - waist, at.x, at.y - radius)
        close()
    }
    drawPath(star, color)
}
