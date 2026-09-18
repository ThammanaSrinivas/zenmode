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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.rememberStoryState
import com.zenlauncher.zenmode.ui.components.storyGestures
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
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
                dark = true
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

/** You at the centre, your gang orbiting, threads of light between you. */
@Composable
private fun CircleVisual() {
    val green = colorResource(R.color.zen_300)
    val glow = colorResource(R.color.os_grad_glow)
    val amber = colorResource(R.color.amber_500)
    val transition = rememberInfiniteTransition(label = "orbit")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "spin")
    val pulse by transition.animateFloat(
        0.85f, 1.1f,
        infiniteRepeatable(tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .aspectRatio(1f)
            .semantics { contentDescription = "You at the centre of your Zen Circle" }
    ) {
        val c = center
        val ring = size.minDimension * 0.38f
        drawCircle(Color.White.copy(alpha = 0.08f), ring, c, style = Stroke(1.dp.toPx()))
        drawCircle(Color.White.copy(alpha = 0.05f), ring * 0.62f, c, style = Stroke(1.dp.toPx()))

        val members = 5
        for (m in 0 until members) {
            val angle = Math.toRadians((spin + m * 360f / members).toDouble())
            val r = if (m % 2 == 0) ring else ring * 0.62f
            val p = Offset(c.x + (r * cos(angle)).toFloat(), c.y + (r * sin(angle)).toFloat())
            drawLine(
                Brush.linearGradient(listOf(glow.copy(alpha = 0.55f), Color.Transparent), c, p),
                c, p, strokeWidth = 1.5.dp.toPx()
            )
            val isBro = m == 0
            drawCircle((if (isBro) amber else green).copy(alpha = 0.22f), 22.dp.toPx() * pulse, p)
            drawCircle(if (isBro) amber else green, if (isBro) 13.dp.toPx() else 9.dp.toPx(), p)
        }
        drawCircle(glow.copy(alpha = 0.18f), 58.dp.toPx() * pulse, c)
        drawCircle(glow.copy(alpha = 0.35f), 38.dp.toPx(), c)
        drawCircle(Color.White, 22.dp.toPx(), c)
        drawCircle(glow, 22.dp.toPx(), c, style = Stroke(3.dp.toPx()))
    }
}

/** A gauge sweeping to today's score in the OS gradient, number rolling up with it. */
@Composable
private fun ScoreVisual() {
    val ember = colorResource(R.color.os_grad_ember)
    val amber = colorResource(R.color.os_grad_amber)
    val glow = colorResource(R.color.os_grad_glow)
    val target = 93
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) { sweep.animateTo(target / 100f, tween(1_800, easing = FastOutSlowInEasing)) }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .aspectRatio(1f)
            .semantics { contentDescription = "Zen Score $target" },
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
            // Tick marks around the dial.
            for (t in 0..27) {
                val a = Math.toRadians((135 + t * 10).toDouble())
                val r1 = size.minDimension / 2 - inset - stroke - 6.dp.toPx()
                val r2 = r1 - if (t % 9 == 0) 10.dp.toPx() else 5.dp.toPx()
                drawLine(
                    Color.White.copy(alpha = if (t % 9 == 0) 0.35f else 0.14f),
                    Offset(center.x + (r1 * cos(a)).toFloat(), center.y + (r1 * sin(a)).toFloat()),
                    Offset(center.x + (r2 * cos(a)).toFloat(), center.y + (r2 * sin(a)).toFloat()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = (sweep.value * 100).roundToInt().toString(),
                fontFamily = DepartureMono,
                fontSize = 72.rsp,
                letterSpacing = (-2).sp,
                color = Color.White
            )
            Text(
                text = "ZEN SCORE",
                fontFamily = DepartureMono,
                fontSize = 12.rsp,
                letterSpacing = 1.4.sp,
                color = glow
            )
            Spacer(Modifier.height(4.rdp))
            Text(
                text = "▲ 4.2 vs yesterday",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 13.rsp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

/** A stack of gold coins rising, one per promise kept. */
@Composable
private fun GoldVisual() {
    val amber = colorResource(R.color.amber_500)
    val amberDeep = colorResource(R.color.amber_800)
    val coin = colorResource(R.color.invest_gold_coin)
    val transition = rememberInfiniteTransition(label = "coins")
    val bob by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, tween(1_400, easing = FastOutSlowInEasing)) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .aspectRatio(1f)
            .semantics { contentDescription = "Gold coins stacking up as you keep your promise" }
    ) {
        val coinW = size.width * 0.52f
        val coinH = coinW * 0.34f
        val thickness = coinH * 0.34f
        val count = 5
        val baseY = size.height * 0.78f
        drawOval(
            Brush.radialGradient(listOf(amber.copy(alpha = 0.35f), Color.Transparent), Offset(center.x, baseY + coinH * 0.4f), coinW),
            topLeft = Offset(center.x - coinW, baseY - coinH * 0.2f),
            size = Size(coinW * 2, coinH * 1.6f)
        )
        for (i in 0 until count) {
            val shown = (rise.value * count - i).coerceIn(0f, 1f)
            if (shown <= 0f) continue
            val lift = (i * thickness * 1.1f) + (1f - shown) * -40.dp.toPx()
            val wobble = if (i == count - 1) bob * 10.dp.toPx() else 0f
            val x = center.x - coinW / 2 + (if (i % 2 == 0) -6 else 6).dp.toPx()
            val y = baseY - lift - wobble - coinH
            drawCoin(Offset(x, y), Size(coinW, coinH), thickness, coin, amberDeep, alpha = shown)
        }
    }
}

private fun DrawScope.drawCoin(topLeft: Offset, size: Size, thickness: Float, face: Color, rim: Color, alpha: Float) {
    drawOval(rim.copy(alpha = alpha), Offset(topLeft.x, topLeft.y + thickness), size)
    drawRect(rim.copy(alpha = alpha), Offset(topLeft.x, topLeft.y + size.height / 2), Size(size.width, thickness))
    drawOval(
        Brush.linearGradient(
            listOf(face, Color.White.copy(alpha = 0.9f), face),
            Offset(topLeft.x, topLeft.y),
            Offset(topLeft.x + size.width, topLeft.y + size.height)
        ),
        topLeft, size, alpha = alpha
    )
    val inner = Size(size.width * 0.72f, size.height * 0.62f)
    drawOval(
        rim.copy(alpha = 0.35f * alpha),
        Offset(topLeft.x + (size.width - inner.width) / 2, topLeft.y + (size.height - inner.height) / 2),
        inner,
        style = Stroke(1.5.dp.toPx())
    )
}

