package com.zenlauncher.zenmode.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import com.zenlauncher.zenmode.MoodState
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.moodWash
import com.zenlauncher.zenmode.ui.theme.statsCardStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val wash = colors.moodWash(mood)
    // Cross-fade when the mood changes (e.g. crossing a threshold mid-day).
    val top by animateColorAsState(wash.first(), tween(700), label = "wash-top")
    val core by animateColorAsState(wash[1], tween(700), label = "wash-core")
    val bottom by animateColorAsState(wash.last(), tween(700), label = "wash-bottom")
    val accent by animateColorAsState(lerp(wash[1], colors.statsCardStroke(mood), 0.28f), tween(700), label = "wash-accent")

    val still = rememberReduceMotion() || LocalInspectionMode.current
    val drift = rememberInfiniteTransition(label = "wash-drift")
    val t by drift.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing)),
        label = "wash-drift-t"
    )
    val swell by drift.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wash-swell"
    )
    val phase = if (still) 0f else t
    val scale = if (still) 1f else swell
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
