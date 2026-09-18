package com.zenlauncher.zenmode.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import kotlin.math.roundToInt

/**
 * Instagram/Revolut-style story playback: each story runs for [storyMillis], then
 * advances; the last one stays put. Shared by onboarding's intro and the weekly recap.
 * Pair with [storyGestures] for tap-to-skip and hold-to-pause.
 */
@Stable
class StoryState internal constructor(val count: Int, private val storyMillis: Int, initialIndex: Int) {
    var index by mutableIntStateOf(initialIndex)
        private set
    internal var paused by mutableStateOf(false)
    internal val progress = Animatable(0f)

    /** How far through the current story, 0..1 — feed this to the segmented bar. */
    val fraction: Float get() = progress.value
    val isLast: Boolean get() = index == count - 1

    fun goTo(target: Int) {
        index = target.coerceIn(0, count - 1)
    }

    fun next() = goTo(index + 1)
    fun previous() = goTo(index - 1)

    internal suspend fun play(restart: Boolean) {
        if (restart) progress.snapTo(0f)
        if (paused) return
        val remaining = ((1f - progress.value) * storyMillis).roundToInt()
        progress.animateTo(1f, tween(remaining, easing = LinearEasing))
        if (!isLast) index++
    }
}

@Composable
fun rememberStoryState(count: Int, storyMillis: Int = 5_500): StoryState {
    val state = rememberSaveable(
        count,
        saver = Saver<StoryState, Int>(save = { it.index }, restore = { StoryState(count, storyMillis, it) })
    ) { StoryState(count, storyMillis, 0) }

    val inspection = LocalInspectionMode.current
    // One effect owns the bar: a new story restarts it, a pause/resume continues it.
    val startedFor = remember(state) { intArrayOf(-1) }
    LaunchedEffect(state, state.index, state.paused) {
        if (inspection) {
            state.progress.snapTo(1f)
            return@LaunchedEffect
        }
        val restart = startedFor[0] != state.index
        startedFor[0] = state.index
        state.play(restart)
    }
    return state
}

/** Tap the left third to go back, anywhere else to go forward; press and hold to pause. */
fun Modifier.storyGestures(state: StoryState): Modifier = pointerInput(state) {
    detectTapGestures(
        onPress = {
            state.paused = true
            tryAwaitRelease()
            state.paused = false
        },
        onTap = { offset ->
            if (offset.x < size.width / 3f) state.previous() else state.next()
        }
    )
}
