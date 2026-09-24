package com.zenlauncher.zenmode.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

private enum class GuideStep { SWIPE_RIGHT, SWIPE_LEFT, BUDDY }

private val GuideScrim = Color.Black.copy(alpha = 0.82f)
// Home isn't blurred while the buddy card is spotlit (the card must stay sharp), so the scrim
// alone has to keep the app grid from showing through the copy.
private val SpotlightScrim = Color.Black.copy(alpha = 0.9f)

/**
 * First-run guide over Home, shown once after onboarding: swipe right for Zen Score, swipe
 * left for Zen Gold, then a spotlight on the buddy card. The swipe steps advance on the swipe
 * they teach (or Next), and the overlay swallows every touch so Home can't open a page mid-guide.
 *
 * [buddyCardBounds] is the right-hand stats card in window coordinates; the last step cuts a
 * hole around it. [hasBuddies] switches that step from "add a buddy" to "open your circle".
 */
@Composable
fun HomeGuideOverlay(
    buddyCardBounds: Rect?,
    hasBuddies: Boolean,
    buddyLabel: String,
    onBuddyAction: () -> Unit,
    onFinished: () -> Unit,
    /** True while the buddy card is spotlit -- Home stays sharp then, blurred otherwise. */
    onSpotlightChange: (Boolean) -> Unit = {}
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = GuideStep.entries[stepIndex]
    fun next() {
        if (stepIndex < GuideStep.entries.lastIndex) stepIndex++ else onFinished()
    }
    BackHandler(onBack = onFinished)
    LaunchedEffect(step) { onSpotlightChange(step == GuideStep.BUDDY) }

    // Window → local: the overlay's own origin, so the spotlight lines up under system bars.
    var origin by remember { mutableStateOf(Offset.Zero) }
    val spotlight = if (step == GuideStep.BUDDY) buddyCardBounds?.translate(-origin) else null
    val pad = with(LocalDensity.current) { 10.dp.toPx() }
    val radius = with(LocalDensity.current) { 24.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.boundsInWindow().topLeft }
            // Swallow taps so nothing on Home underneath reacts.
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            )
            // pageSwipe's pointerInput outlives recompositions, so read the step state live
            // here rather than the `step` captured on first composition.
            .pageSwipe(
                onSwipeLeft = { if (GuideStep.entries[stepIndex] == GuideStep.SWIPE_LEFT) next() },
                onSwipeRight = { if (GuideStep.entries[stepIndex] == GuideStep.SWIPE_RIGHT) next() }
            )
    ) {
        // Scrim with a rounded hole around the buddy card on the last step.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(if (spotlight != null) SpotlightScrim else GuideScrim)
            spotlight?.let { r ->
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(r.left - pad, r.top - pad),
                    size = Size(r.width + pad * 2, r.height + pad * 2),
                    cornerRadius = CornerRadius(radius),
                    blendMode = BlendMode.Clear
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = Offset(r.left - pad, r.top - pad),
                    size = Size(r.width + pad * 2, r.height + pad * 2),
                    cornerRadius = CornerRadius(radius),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Tapping the lit card does what the card does.
        if (spotlight != null) {
            with(LocalDensity.current) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = spotlight.left - pad
                            translationY = spotlight.top - pad
                        }
                        .size((spotlight.width + pad * 2).toDp(), (spotlight.height + pad * 2).toDp())
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClickLabel = buddyLabel) {
                            onFinished()
                            onBuddyAction()
                        }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 30.rdp, vertical = 20.rdp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                GuideTextButton(text = "Skip", onClick = onFinished)
            }

            // Swipe steps sit mid-screen; the buddy step sits at the bottom, clear of the card.
            Spacer(modifier = Modifier.weight(if (step == GuideStep.BUDDY) 2f else 1f))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
                label = "home-guide-step",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            ) { shown ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (shown) {
                        GuideStep.SWIPE_RIGHT -> {
                            SwipeHint(toRight = true)
                            GuideCopy(
                                title = "Swipe right",
                                body = "Your Zen Score lives one swipe to the right. See how mindful today has been."
                            )
                        }
                        GuideStep.SWIPE_LEFT -> {
                            SwipeHint(toRight = false)
                            GuideCopy(
                                title = "Swipe left",
                                body = "Zen Gold is one swipe to the left. Keep your promise and watch your gold grow."
                            )
                        }
                        GuideStep.BUDDY -> GuideCopy(
                            title = if (hasBuddies) "Your circle" else "Bring a buddy",
                            body = if (hasBuddies) {
                                "Swipe this card to flip through your circle. Tap it to see how everyone's doing."
                            } else {
                                "Quitting doomscrolling is easier together. Add a friend and keep each other honest."
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            GuideDots(current = stepIndex, count = GuideStep.entries.size)
            Spacer(modifier = Modifier.height(20.rdp))
            if (step == GuideStep.BUDDY) {
                V3PrimaryPillButton(
                    text = buddyLabel,
                    onClick = {
                        onFinished()
                        onBuddyAction()
                    }
                )
                Spacer(modifier = Modifier.height(8.rdp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GuideTextButton(text = "Maybe later", onClick = onFinished)
                }
            } else {
                V3PrimaryPillButton(text = "Next", onClick = ::next)
            }
        }
    }
}

@Composable
private fun GuideCopy(title: String, body: String) {
    Text(
        text = title,
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.rsp,
        color = Color.White,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(10.rdp))
    Text(
        text = body,
        fontFamily = Geist,
        fontSize = 16.rsp,
        lineHeight = 22.rsp,
        color = Color.White.copy(alpha = 0.8f),
        textAlign = TextAlign.Center
    )
}

/** A fingertip gliding along a track in the swipe's direction, looping. */
@Composable
private fun SwipeHint(toRight: Boolean) {
    val accent = colorResource(R.color.score_grad_start)
    val progress by rememberInfiniteTransition(label = "swipe-hint").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "swipe-hint-progress"
    )
    Canvas(modifier = Modifier.width(200.rdp).height(72.rdp)) {
        val y = size.height / 2
        val start = if (toRight) size.width * 0.15f else size.width * 0.85f
        val end = if (toRight) size.width * 0.85f else size.width * 0.15f
        // Glide over the first 70%, then fade out before looping.
        val t = (progress / 0.7f).coerceAtMost(1f)
        val eased = 1f - (1f - t) * (1f - t)
        val x = start + (end - start) * eased
        val alpha = if (progress < 0.7f) 1f else 1f - (progress - 0.7f) / 0.3f

        drawLine(Color.White.copy(alpha = 0.18f), Offset(start, y), Offset(end, y), 4.dp.toPx(), StrokeCap.Round)
        drawLine(accent.copy(alpha = alpha), Offset(start, y), Offset(x, y), 4.dp.toPx(), StrokeCap.Round)
        // Arrow head at the destination.
        val head = 10.dp.toPx() * (if (toRight) 1f else -1f)
        drawLine(Color.White.copy(alpha = 0.6f), Offset(end, y), Offset(end - head, y - head * 0.8f * (if (toRight) 1f else -1f)), 3.dp.toPx(), StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.6f), Offset(end, y), Offset(end - head, y + head * 0.8f * (if (toRight) 1f else -1f)), 3.dp.toPx(), StrokeCap.Round)
        // Fingertip.
        drawCircle(Color.White.copy(alpha = 0.25f * alpha), 22.dp.toPx(), Offset(x, y))
        drawCircle(Color.White.copy(alpha = alpha), 12.dp.toPx(), Offset(x, y))
    }
    Spacer(modifier = Modifier.height(16.rdp))
}

@Composable
private fun GuideDots(current: Int, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.rdp, Alignment.CenterHorizontally)
    ) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .size(width = if (i == current) 20.rdp else 7.rdp, height = 7.rdp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (i == current) 1f else 0.35f))
            )
        }
    }
}

@Composable
private fun GuideTextButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 15.rsp,
        color = Color.White.copy(alpha = 0.85f),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .pressScale(onClick = onClick)
            .padding(horizontal = 14.rdp, vertical = 8.rdp)
    )
}
