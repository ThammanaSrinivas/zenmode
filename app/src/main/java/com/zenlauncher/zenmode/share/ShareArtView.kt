package com.zenlauncher.zenmode.share

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.zenlauncher.zenmode.ZenSound
import com.zenlauncher.zenmode.ui.components.rememberReduceMotion

/** The fonts and mark every card draws with, loaded once for this screen. */
@Composable
fun rememberShareKit(): ShareKit {
    val context = LocalContext.current
    return remember(context) { ShareKit(context) }
}

/**
 * Plays [art] at whatever size this takes (keep it at the art's aspect ratio): its arrival from
 * the first frame, with each [ShareCue] sounded as the clock passes it, then — for ambient
 * art — the settled card gently alive for as long as it's on screen. Change [playKey] to replay.
 *
 * The clock is read only while drawing, so a running card redraws without recomposing.
 * "Remove animations", previews and screenshot tests get the settled frame and no sound.
 */
@Composable
fun ShareArtView(
    art: ShareArt,
    modifier: Modifier = Modifier,
    playKey: Int = 0,
    sounds: Boolean = true,
    onSettled: () -> Unit = {}
) {
    val still = rememberReduceMotion() || LocalInspectionMode.current
    val clock = remember(art) { mutableFloatStateOf(if (still) art.introMs.toFloat() else 0f) }
    val haptics = if (LocalInspectionMode.current) null else LocalHapticFeedback.current
    val settled by rememberUpdatedState(onSettled)

    LaunchedEffect(art, playKey, still) {
        if (still) {
            clock.floatValue = art.introMs.toFloat()
            settled()
            return@LaunchedEffect
        }
        val cues = art.cues.sortedBy { it.atMs }
        var next = 0
        var announced = false
        val start = withFrameMillis { it }
        while (true) {
            val elapsed = withFrameMillis { it } - start
            clock.floatValue = elapsed.toFloat()
            while (next < cues.size && elapsed >= cues[next].atMs) {
                val cue = cues[next++]
                if (sounds) ZenSound.play(cue.sfx, cue.rate)
                when (cue.haptic) {
                    CueHaptic.NONE -> Unit
                    CueHaptic.TICK -> haptics?.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    CueHaptic.LAND -> haptics?.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            }
            if (!announced && elapsed >= art.introMs) {
                announced = true
                settled()
                if (!art.ambient) {
                    clock.floatValue = art.introMs.toFloat()
                    break
                }
            }
            // A calm app shouldn't animate forever: let the card breathe a while, then hold still.
            if (elapsed >= art.introMs + AMBIENT_MS) break
        }
    }

    Canvas(modifier = modifier.semantics { contentDescription = art.description }) {
        val t = clock.floatValue
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val scale = size.width / art.format.width
            native.save()
            native.scale(scale, scale)
            // An exported bitmap is clipped by its own edges; on screen, nothing else would be.
            native.clipRect(0f, 0f, art.format.width.toFloat(), art.format.height.toFloat())
            art.draw(native, t)
            native.restore()
        }
    }
}

/** How long a settled card keeps its ambient motion on screen before it rests. */
private const val AMBIENT_MS = 20_000
