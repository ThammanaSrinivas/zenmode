package com.zenlauncher.zenmode.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.AppLogic
import com.zenlauncher.zenmode.HomeTheme
import com.zenlauncher.zenmode.HomeThemePreferences
import com.zenlauncher.zenmode.ProAccess
import com.zenlauncher.zenmode.ui.theme.isInk
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.zenlauncher.zenmode.MoodState
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.moodWash
import com.zenlauncher.zenmode.ui.theme.statsCardStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Mood backdrop ─────────────────────────────────────────────────
// Home's three washes — green, yellow or red by today's screen time — carried across the
// product as one background layer: the vertical wash plus a few large, heavily blurred colour
// pools that drift very slowly, so every page quietly says how the day is going.

/** Last mood any screen worked out, so a page opened from Home paints the right wash at once. */
object MoodSource {
    @Volatile
    var lastKnown: MoodState? = null
}

/** Today's mood from screen time. Re-read every time the page resumes. */
@Composable
fun rememberTodayMood(): MoodState {
    val context = LocalContext.current.applicationContext
    val resumes = rememberResumeCount()
    val mood by produceState(initialValue = MoodSource.lastKnown ?: MoodState.HAPPY, resumes) {
        val millis = withContext(Dispatchers.IO) {
            runCatching {
                UsageRepository(context, ServiceLocator.analyticsManager).getTodayUsage().screenTimeInMillis
            }.getOrNull()
        } ?: return@produceState
        val today = AppLogic.getMoodState(millis / 60_000)
        MoodSource.lastKnown = today
        value = today
    }
    return mood
}

/**
 * The home theme actually in force: the user's pick while they're Pro, otherwise [HomeTheme.MOOD].
 * Lapsing back to free quietly returns the mood wash; the pick is kept for if they come back.
 */
@Composable
fun rememberActiveHomeTheme(): HomeTheme {
    if (LocalInspectionMode.current || !ServiceLocator.isInitialized) return HomeTheme.MOOD
    val context = LocalContext.current.applicationContext
    val picked by remember(context) { HomeThemePreferences.state(context) }.collectAsState()
    return if (ProAccess.isProState(context)) picked else HomeTheme.MOOD
}

/** Edge, core, edge — the same three-stop shape as the mood washes. */
fun HomeTheme.wash(ink: Boolean): List<Color> {
    val (edge, core) = when (this) {
        HomeTheme.MOOD -> error("Mood wash comes from ZenColors.moodWash")
        HomeTheme.MOSS -> if (ink) 0xFF0F140C to 0xFF1C2B15 else 0xFFEEF2E6 to 0xFFD8E7C6
        HomeTheme.TIDE -> if (ink) 0xFF0B1316 to 0xFF11272F else 0xFFE8F0F2 to 0xFFCBE1E9
        HomeTheme.DUSK -> if (ink) 0xFF120F16 to 0xFF241A30 else 0xFFF1ECF2 to 0xFFE1D3EA
        HomeTheme.SAND -> if (ink) 0xFF15120C to 0xFF2A2216 else 0xFFF4EFE6 to 0xFFEAD9BE
        HomeTheme.STONE -> if (ink) 0xFF111111 to 0xFF1D1D1C else 0xFFF2F1ED to 0xFFE0DFDA
    }
    return listOf(Color(edge), Color(core), Color(edge))
}

/** The colour the drifting pools lean toward. */
fun HomeTheme.accent(ink: Boolean): Color = Color(
    when (this) {
        HomeTheme.MOOD -> error("Mood accent comes from ZenColors.statsCardStroke")
        HomeTheme.MOSS -> if (ink) 0xFF4E7A35 else 0xFF8DBA68
        HomeTheme.TIDE -> if (ink) 0xFF2F6F84 else 0xFF76B0C4
        HomeTheme.DUSK -> if (ink) 0xFF6A4A8A else 0xFFB095CC
        HomeTheme.SAND -> if (ink) 0xFF8A6A36 else 0xFFD8B278
        HomeTheme.STONE -> if (ink) 0xFF3A3A38 else 0xFFB5B4AE
    }
)

/** Where the drifting pools are after a given time. Pure, so the motion can be pinned in a test. */
object BackdropDrift {
    /**
     * Redraw interval while drifting. The pools move a few pixels a second under a 64dp blur,
     * so ten redraws a second look the same as one every vsync; each redraw repaints the whole
     * window and re-blurs the canvas.
     */
    const val TICK_MILLIS = 100L
    private const val LAP_SECONDS = 28f
    private const val SWELL_SECONDS = 9f

    /** 0 → 2π once every [LAP_SECONDS], then round again. */
    fun phase(seconds: Float): Float = (seconds % LAP_SECONDS) / LAP_SECONDS * (2 * PI).toFloat()

    /** 0.92 → 1.08 and back, [SWELL_SECONDS] each way, eased at both ends. */
    fun swell(seconds: Float): Float {
        val lap = (seconds / SWELL_SECONDS) % 2f
        val fraction = if (lap <= 1f) lap else 2f - lap
        return 0.92f + 0.16f * FastOutSlowInEasing.transform(fraction)
    }
}

/**
 * Seconds of drift so far: advances every [BackdropDrift.TICK_MILLIS] while [running] and holds
 * its value otherwise, so the pools stop where they are and carry on from there.
 */
@Composable
private fun rememberDriftSeconds(running: Boolean): FloatState {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val base = seconds.floatValue
        val start = withFrameNanos { it }
        while (true) {
            delay(BackdropDrift.TICK_MILLIS)
            seconds.floatValue = base + (withFrameNanos { it } - start) / 1_000_000_000f
        }
    }
    return seconds
}

/** The wash colours for [mood] — [first] is the top edge, [last] the bottom edge. */
@Composable
fun moodWashColors(mood: MoodState): List<Color> = ZenTheme.colors.moodWash(mood)

/**
 * Full-bleed mood background. Put it first in a Box, with `Modifier.matchParentSize()` or
 * `fillMaxSize()`; content drawn after it sits on top.
 */
@Composable
fun MoodBackdrop(mood: MoodState = rememberTodayMood(), modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    val theme = rememberActiveHomeTheme()
    val wash = if (theme == HomeTheme.MOOD) colors.moodWash(mood) else theme.wash(colors.isInk)
    val pull = if (theme == HomeTheme.MOOD) colors.statsCardStroke(mood) else theme.accent(colors.isInk)
    // Cross-fade when the mood (or the picked theme) changes.
    val top by animateColorAsState(wash.first(), tween(700), label = "wash-top")
    val core by animateColorAsState(wash[1], tween(700), label = "wash-core")
    val bottom by animateColorAsState(wash.last(), tween(700), label = "wash-bottom")
    val accent by animateColorAsState(lerp(wash[1], pull, 0.28f), tween(700), label = "wash-accent")

    val still = rememberReduceMotion() || LocalInspectionMode.current

    // Read only inside the Canvas, so each drift tick redraws without recomposing. The drift
    // runs on its own slow clock rather than an InfiniteTransition, which asks the frame clock
    // for a new frame every vsync for as long as it exists; and only while the page is freshly
    // arrived at, after which the pools rest where they are and nothing is redrawn.
    val drift = rememberDriftSeconds(running = rememberAmbientAlive())
    // Real blur exists from API 31; below that the radial falloff alone keeps the pools soft.
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier = modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, core, bottom)))) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(if (canBlur) Modifier.blur(64.dp) else Modifier)
        ) {
            val w = size.width
            val h = size.height
            val phase = BackdropDrift.phase(drift.floatValue)
            val scale = if (still) 1f else BackdropDrift.swell(drift.floatValue)
            fun pool(color: Color, x: Float, y: Float, radius: Float, alpha: Float) {
                val c = Offset(x, y)
                drawCircle(
                    brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), c, radius),
                    radius = radius,
                    center = c
                )
            }
            pool(core, w * (0.25f + 0.08f * cos(phase)), h * (0.34f + 0.05f * sin(phase)), w * 0.75f * scale, 0.9f)
            pool(accent, w * (0.8f + 0.07f * sin(phase * 1.3f)), h * (0.55f + 0.06f * cos(phase)), w * 0.6f * (2f - scale), 0.55f)
            pool(core, w * (0.45f + 0.1f * sin(phase * 0.7f)), h * (0.8f + 0.04f * cos(phase * 1.1f)), w * 0.7f, 0.6f)
        }
    }
}
