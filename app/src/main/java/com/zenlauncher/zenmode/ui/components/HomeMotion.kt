package com.zenlauncher.zenmode.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zenlauncher.zenmode.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ── Home reveal ───────────────────────────────────────────────────
// Home "loads" as the phone is unlocked: the four numbers that matter play a short,
// staggered reveal so the eye lands on them in order, Zen Score → streak flame → my screen
// time → my Zen Bro / Zen Circle screen time, and then the crown is handed to whoever
// owns it today. It plays once per unlock only; coming back from an app or a page leaves
// Home as it was. MainActivity drives the [HomeRevealCue]: it hides Home while the screen is
// off (so the settled state never flashes), plays when Home is what the unlock lands on,
// and settles quietly otherwise. "Remove animations" skips straight to the settled state.

/** Stagger, in ms, of each piece of the reveal. */
object HomeReveal {
    const val SCORE = 0
    const val STREAK = 90
    const val GOLD = 160
    const val MY_CARD = 240
    const val BOLT = 330
    const val BUDDY_CARD = 360
    const val CROWN = 560
}

/** What Home's reveal should be doing; a new [id] with [Phase.Play] starts a fresh reveal. */
@Immutable
data class HomeRevealCue(val id: Int = 0, val phase: Phase = Phase.Settled) {
    enum class Phase { Settled, Hidden, Play }

    fun hidden() = copy(phase = Phase.Hidden)
    fun play() = HomeRevealCue(id + 1, Phase.Play)
    fun settled() = copy(phase = Phase.Settled)
}

/** True when the system "Remove animations" setting is on. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Increments every time the host resumes; key a refresh off it to re-read on each visit. */
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

/** 0 → 1 progress: hidden at 0 while [cue] is hidden, played after [delayMillis], else 1. */
@Composable
private fun rememberReveal(cue: HomeRevealCue, delayMillis: Int, spec: AnimationSpec<Float>): Animatable<Float, *> {
    val settled = LocalInspectionMode.current || rememberReduceMotion()
    val progress = remember { Animatable(if (!settled && cue.phase == HomeRevealCue.Phase.Hidden) 0f else 1f) }
    LaunchedEffect(cue, settled) {
        when {
            settled || cue.phase == HomeRevealCue.Phase.Settled -> progress.snapTo(1f)
            cue.phase == HomeRevealCue.Phase.Hidden -> progress.snapTo(0f)
            else -> {
                progress.snapTo(0f)
                delay(delayMillis.toLong())
                progress.animateTo(1f, spec)
            }
        }
    }
    return progress
}

/**
 * Card pop: rises [rise], scales up from 92% with a soft overshoot and fades in. Used on the two
 * screen-time cards so they land one after the other.
 */
@Composable
fun Modifier.revealPop(cue: HomeRevealCue, delayMillis: Int, rise: Dp = 22.dp): Modifier {
    val p = rememberReveal(cue, delayMillis, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
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
fun Modifier.revealRise(cue: HomeRevealCue, delayMillis: Int, rise: Dp = 10.dp): Modifier {
    val p = rememberReveal(cue, delayMillis, tween(420, easing = FastOutSlowInEasing))
    val risePx = with(LocalDensity.current) { rise.toPx() }
    return graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * risePx
    }
}

/** Zen mark spin-in: a quarter turn and a scale pop that settles on the score. */
@Composable
fun Modifier.revealSpin(cue: HomeRevealCue, delayMillis: Int): Modifier {
    val p = rememberReveal(cue, delayMillis, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
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
fun Modifier.revealStrike(cue: HomeRevealCue, delayMillis: Int): Modifier {
    val p = rememberReveal(cue, delayMillis, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
    return graphicsLayer {
        val v = p.value
        alpha = v.coerceIn(0f, 1f)
        val scale = 1.6f - 0.6f * v
        scaleX = scale
        scaleY = scale
        rotationZ = (1f - v) * 25f
    }
}

/** Counts from 0 up to [target] when [cue] plays, finishing in [durationMillis]. */
@Composable
fun rememberCountUp(target: Int, cue: HomeRevealCue, delayMillis: Int, durationMillis: Int = 700): Int {
    val settled = LocalInspectionMode.current || rememberReduceMotion()
    val value = remember { Animatable(target.toFloat()) }
    var countedId by remember { mutableIntStateOf(cue.id) }
    LaunchedEffect(cue, target, settled) {
        when {
            settled -> value.snapTo(target.toFloat())
            cue.phase == HomeRevealCue.Phase.Hidden -> value.snapTo(0f)
            // A fresh unlock counts up from zero; a live update mid-visit just glides there.
            cue.phase == HomeRevealCue.Phase.Play && cue.id != countedId -> {
                countedId = cue.id
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
 * The streak flame, alive: on each unlock it ignites (grows from its base with a flare of glow),
 * then keeps a slow, uneven flicker — a sway and a breathing stretch at slightly different
 * rhythms so it never looks looped — under a warm halo. At a zero streak it stays fully
 * visible but still, waiting to be lit.
 */
@Composable
fun BlazingFlame(
    cue: HomeRevealCue,
    lit: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceMotion()
    val animate = lit && !reduceMotion && !LocalInspectionMode.current
    val glowColor = colorResource(R.color.score_orange)

    val ignite = remember { Animatable(1f) }
    val flare = remember { Animatable(0f) }
    LaunchedEffect(cue, animate) {
        if (!animate || cue.phase == HomeRevealCue.Phase.Settled) {
            ignite.snapTo(1f)
            flare.snapTo(0f)
            return@LaunchedEffect
        }
        ignite.snapTo(0f)
        flare.snapTo(0f)
        if (cue.phase == HomeRevealCue.Phase.Hidden) return@LaunchedEffect
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

/**
 * Who owns the crown, told as a small story on each unlock. Once both cards have landed the
 * crown tumbles in above the bolt, leans toward one card and then the other as if weighing
 * them, and arcs over to today's winner, landing with a squash and a burst of gold. With no
 * rival it simply drops onto my card. Outside a reveal it just sits at [rest].
 *
 * [rest] and [stage] are top-left positions inside the parent; [towardRight] is which way the
 * winner lies from [stage]. Size the crown through [modifier].
 */
@Composable
fun CrownHandoff(
    cue: HomeRevealCue,
    rest: DpOffset,
    stage: DpOffset,
    contested: Boolean,
    towardRight: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val settled = LocalInspectionMode.current || rememberReduceMotion()
    val hiddenAtStart = !settled && cue.phase == HomeRevealCue.Phase.Hidden
    val drop = remember { Animatable(if (hiddenAtStart) 0f else 1f) }
    val flight = remember { Animatable(if (hiddenAtStart) 0f else 1f) }
    val sway = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    val burst = remember { Animatable(1f) }
    val toward = if (towardRight) 1f else -1f

    LaunchedEffect(cue, settled) {
        sway.snapTo(0f)
        squash.snapTo(0f)
        burst.snapTo(1f)
        when {
            settled || cue.phase == HomeRevealCue.Phase.Settled -> {
                drop.snapTo(1f)
                flight.snapTo(1f)
            }
            cue.phase == HomeRevealCue.Phase.Hidden -> {
                drop.snapTo(0f)
                flight.snapTo(0f)
            }
            else -> {
                drop.snapTo(0f)
                flight.snapTo(if (contested) 0f else 1f)
                delay(HomeReveal.CROWN.toLong())
                drop.animateTo(1f, spring(dampingRatio = if (contested) 0.6f else 0.45f, stiffness = 320f))
                if (contested) {
                    // "Hmm…": a lean toward the loser, a longer one toward the winner, then off.
                    sway.animateTo(-toward, tween(240, easing = FastOutSlowInEasing))
                    sway.animateTo(toward, tween(300, easing = FastOutSlowInEasing))
                    launch { sway.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
                    flight.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
                }
                squash.snapTo(1f)
                burst.snapTo(0f)
                launch { burst.animateTo(1f, tween(700, easing = LinearOutSlowInEasing)) }
                squash.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
            }
        }
    }

    val density = LocalDensity.current
    val dropPx = with(density) { (if (contested) 34.dp else 48.dp).toPx() }
    val arcPx = with(density) { 30.dp.toPx() }
    val swayPx = with(density) { 9.dp.toPx() }
    val rayPx = with(density) { 2.dp.toPx() }
    val gold = colorResource(R.color.score_grad_mid)
    val amber = colorResource(R.color.gold_amount)

    Image(
        painter = painterResource(R.drawable.ic_crown_v3),
        contentDescription = contentDescription,
        modifier = modifier
            .offset {
                val t = flight.value
                val x = with(density) { stage.x.toPx() + (rest.x.toPx() - stage.x.toPx()) * t } + sway.value * swayPx
                val y = with(density) { stage.y.toPx() + (rest.y.toPx() - stage.y.toPx()) * t } -
                    sin(PI * t).toFloat() * arcPx - (1f - drop.value) * dropPx
                IntOffset(x.roundToInt(), y.roundToInt())
            }
            // Outside the layer below, so the burst isn't squashed along with the crown.
            .drawBehind {
                val b = burst.value
                if (b >= 1f) return@drawBehind
                val c = Offset(size.width / 2f, size.height * 0.6f)
                val r = size.minDimension
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(gold.copy(alpha = 0.55f * (1f - b)), Color.Transparent),
                        center = c,
                        radius = r * (0.6f + 0.6f * b)
                    ),
                    radius = r * (0.6f + 0.6f * b),
                    center = c
                )
                for (i in 0 until 8) {
                    val a = (i * PI / 4 - PI / 2).toFloat()
                    val dir = Offset(cos(a), sin(a))
                    val inner = r * (0.55f + 0.45f * b)
                    val outer = inner + r * 0.32f * (1f - b)
                    drawLine(
                        color = (if (i % 2 == 0) gold else amber).copy(alpha = 1f - b),
                        start = c + dir * inner,
                        end = c + dir * outer,
                        strokeWidth = rayPx,
                        cap = StrokeCap.Round
                    )
                }
            }
            .graphicsLayer {
                val t = flight.value
                val d = drop.value
                val lift = sin(PI * t).toFloat()
                alpha = (d * 2f).coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(0.5f, 0.85f)
                rotationZ = (1f - d) * -200f + sway.value * 14f + lift * 16f * toward
                val grow = (0.4f + 0.6f * d) * (1f + 0.18f * lift)
                scaleX = grow * (1f + 0.2f * squash.value)
                scaleY = grow * (1f - 0.24f * squash.value)
            }
    )
}
