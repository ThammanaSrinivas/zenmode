package com.zenlauncher.zenmode.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zenlauncher.zenmode.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ── Home reveal ───────────────────────────────────────────────────
// Every time Home comes into view (unlock, back from an app, back from a page) the four
// numbers that matter play a short, staggered reveal so the eye lands on them in order:
// Zen Score → streak flame → my screen time → my Zen Bro / Zen Circle screen time.
// Home is a singleTask launcher that is rarely recreated, so the reveal is keyed to
// ON_RESUME rather than to first composition. Everything settles in under a second, and
// "Remove animations" skips straight to the settled state.

/** Stagger, in ms, of each piece of the reveal. */
object HomeReveal {
    const val SCORE = 0
    const val STREAK = 90
    const val GOLD = 160
    const val MY_CARD = 240
    const val BOLT = 330
    const val BUDDY_CARD = 360
}

/** True when the system "Remove animations" setting is on. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Increments every time the host resumes; key a reveal off it to replay on each visit. */
@Composable
fun rememberResumeCount(): Int {
    val owner = LocalLifecycleOwner.current
    var count by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) count++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return count
}

/** 0 → 1 progress that restarts whenever [key] changes, after [delayMillis]. */
@Composable
private fun rememberReveal(key: Int, delayMillis: Int, spec: androidx.compose.animation.core.AnimationSpec<Float>): Animatable<Float, *> {
    val settled = LocalInspectionMode.current || rememberReduceMotion()
    // Starts hidden so the first frame doesn't flash the settled state before the reveal.
    val progress = remember { Animatable(if (settled) 1f else 0f) }
    LaunchedEffect(key) {
        if (settled || key == 0) return@LaunchedEffect
        progress.snapTo(0f)
        delay(delayMillis.toLong())
        progress.animateTo(1f, spec)
    }
    return progress
}

/**
 * Card pop: rises [rise], scales up from 92% with a soft overshoot and fades in. Used on the two
 * screen-time cards so they land one after the other.
 */
@Composable
fun Modifier.revealPop(key: Int, delayMillis: Int, rise: Dp = 22.dp): Modifier {
    val p = rememberReveal(key, delayMillis, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
    val risePx = with(LocalDensity.current) { rise.toPx() }
    return graphicsLayer {
        val v = p.value
        alpha = (v * 1.6f).coerceIn(0f, 1f)
        translationY = (1f - v) * risePx
        val scale = 0.92f + 0.08f * v
        scaleX = scale
        scaleY = scale
    }
}

/** Gentle fade-and-rise for supporting elements. */
@Composable
fun Modifier.revealRise(key: Int, delayMillis: Int, rise: Dp = 10.dp): Modifier {
    val p = rememberReveal(key, delayMillis, tween(420, easing = FastOutSlowInEasing))
    val risePx = with(LocalDensity.current) { rise.toPx() }
    return graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * risePx
    }
}

/** Zen mark spin-in: a quarter turn and a scale pop that settles on the score. */
@Composable
fun Modifier.revealSpin(key: Int, delayMillis: Int): Modifier {
    val p = rememberReveal(key, delayMillis, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
    return graphicsLayer {
        rotationZ = (1f - p.value) * -90f
        val scale = 0.6f + 0.4f * p.value
        scaleX = scale
        scaleY = scale
        alpha = p.value.coerceIn(0f, 1f)
    }
}

/** The bolt between the cards strikes: flashes in oversized, then snaps to size. */
@Composable
fun Modifier.revealStrike(key: Int, delayMillis: Int): Modifier {
    val p = rememberReveal(key, delayMillis, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
    return graphicsLayer {
        val v = p.value
        alpha = v.coerceIn(0f, 1f)
        val scale = 1.6f - 0.6f * v
        scaleX = scale
        scaleY = scale
        rotationZ = (1f - v) * 25f
    }
}

/** Counts from 0 up to [target] each time [key] changes, finishing in [durationMillis]. */
@Composable
fun rememberCountUp(target: Int, key: Int, delayMillis: Int, durationMillis: Int = 700): Int {
    val settled = LocalInspectionMode.current || rememberReduceMotion()
    val value = remember { Animatable(if (settled) target.toFloat() else 0f) }
    var countedKey by remember { mutableIntStateOf(-1) }
    LaunchedEffect(key, target) {
        when {
            settled -> value.snapTo(target.toFloat())
            key == 0 -> Unit
            // A fresh visit counts up from zero; a live update mid-visit just glides there.
            key != countedKey -> {
                countedKey = key
                value.snapTo(0f)
                delay(delayMillis.toLong())
                value.animateTo(target.toFloat(), tween(durationMillis, easing = FastOutSlowInEasing))
            }
            else -> value.animateTo(target.toFloat(), tween(400, easing = FastOutSlowInEasing))
        }
    }
    return value.value.roundToInt()
}

/**
 * The streak flame, alive: on each reveal it ignites (grows from its base with a flare of glow),
 * then keeps a slow, uneven flicker — a sway and a breathing stretch at slightly different
 * rhythms so it never looks looped — under a warm halo. At a zero streak it stays fully
 * visible but still, waiting to be lit.
 */
@Composable
fun BlazingFlame(
    revealKey: Int,
    lit: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceMotion()
    val animate = lit && !reduceMotion && !LocalInspectionMode.current
    val glowColor = colorResource(R.color.score_orange)

    val ignite = remember { Animatable(if (animate) 0f else 1f) }
    val flare = remember { Animatable(0f) }
    LaunchedEffect(revealKey, animate) {
        if (!animate) {
            ignite.snapTo(1f)
            return@LaunchedEffect
        }
        if (revealKey == 0) return@LaunchedEffect
        ignite.snapTo(0f)
        flare.snapTo(0f)
        delay(HomeReveal.STREAK.toLong())
        launch { ignite.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow)) }
        flare.animateTo(1f, tween(260))
        flare.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
    }

    val flicker = rememberInfiniteTransition(label = "flame")
    val sway by flicker.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1_150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flame-sway"
    )
    val breathe by flicker.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(730, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flame-breathe"
    )
    val halo by flicker.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flame-halo"
    )

    Box(
        modifier = modifier.drawBehind {
            if (!lit) return@drawBehind
            val strength = (if (animate) halo else 0.22f) + flare.value * 0.45f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = strength.coerceIn(0f, 1f)), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.62f),
                    radius = size.minDimension * (0.75f + 0.25f * flare.value)
                ),
                radius = size.minDimension,
                center = Offset(size.width / 2f, size.height * 0.62f)
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_streak_fire),
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize().graphicsLayer {
                // Flames grow from the base, so every transform pivots at the bottom centre.
                transformOrigin = TransformOrigin(0.5f, 1f)
                val grow = ignite.value
                scaleX = (0.5f + 0.5f * grow) * (if (animate) 2f - breathe else 1f)
                scaleY = (0.3f + 0.7f * grow) * (if (animate) breathe else 1f)
                rotationZ = if (animate) sway else 0f
            }
        )
    }
}
