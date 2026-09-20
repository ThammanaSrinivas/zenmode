package com.zenlauncher.zenmode.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import com.zenlauncher.zenmode.Sfx
import com.zenlauncher.zenmode.ZenSound

// ── Motion tokens ─────────────────────────────────────────────────
// One vocabulary for every animated thing, so the app moves like one object.
//  · press:   things under a finger sink fast and settle back with a little give
//  · settle:  cards, rows and indicators landing in a new place
//  · emphasis: the few moments worth a flourish (Pro unlocked, onboarding done)
// Durations stay under 400ms for anything a user waits on. "Remove animations" is honoured
// by the system animator scale, which every spec here goes through.

object ZenMotion {
    /** Material "emphasized decelerate": arrivals. */
    val EaseOut = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    /** Material "emphasized accelerate": departures. */
    val EaseIn = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val FAST = 160
    const val MEDIUM = 280
    const val SLOW = 420

    fun <T> settle() = spring<T>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    fun <T> bouncy() = spring<T>(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow)
    fun <T> snappy() = spring<T>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
    fun <T> arrive(durationMillis: Int = MEDIUM) = tween<T>(durationMillis, easing = EaseOut)
    fun <T> leave(durationMillis: Int = FAST) = tween<T>(durationMillis, easing = EaseIn)
}

// ── Feedback: sound + haptic, paired ──────────────────────────────
// Call sites say *what happened* (a toggle, a success), never which buzz or which file.
// Keeping the pairing here means a switch always sounds and feels like a switch.

@Stable
class ZenFeedback internal constructor(private val haptics: HapticFeedback?) {
    private fun fire(sfx: Sfx?, haptic: HapticFeedbackType?) {
        if (haptic != null) haptics?.performHapticFeedback(haptic)
        if (sfx != null) ZenSound.play(sfx)
    }

    /** Buttons, rows, chips. */
    fun tap() = fire(Sfx.TAP, HapticFeedbackType.VirtualKey)
    /** A radio, segment or list item became the chosen one. */
    fun select() = fire(Sfx.SELECT, HapticFeedbackType.SegmentTick)
    fun toggle(on: Boolean) = fire(
        if (on) Sfx.TOGGLE_ON else Sfx.TOGGLE_OFF,
        if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
    )
    fun sheetOpen() = fire(Sfx.SHEET_OPEN, null)
    fun sheetClose() = fire(Sfx.SHEET_CLOSE, null)
    /** Forward one page in a flow. */
    fun step() = fire(Sfx.STEP, HapticFeedbackType.ContextClick)
    /** Back one page: haptic only, going back shouldn't announce itself. */
    fun back() = fire(null, HapticFeedbackType.ContextClick)
    fun success() = fire(Sfx.SUCCESS, HapticFeedbackType.Confirm)
    fun proUnlocked() = fire(Sfx.PRO_UNLOCK, HapticFeedbackType.Confirm)
    fun error() = fire(Sfx.ERROR, HapticFeedbackType.Reject)
    fun theme(dark: Boolean) = fire(if (dark) Sfx.THEME_DARK else Sfx.THEME_LIGHT, HapticFeedbackType.GestureThresholdActivate)
    /** The last beat of onboarding. */
    fun enter() = fire(Sfx.ENTER, HapticFeedbackType.LongPress)
    /** Dragging past a detent (steppers, wheels). Haptic only — sound here would machine-gun. */
    fun detent() = fire(null, HapticFeedbackType.SegmentFrequentTick)
}

@Composable
fun rememberZenFeedback(): ZenFeedback {
    // Previews and screenshot tests get a silent instance.
    val haptics = if (LocalInspectionMode.current) null else LocalHapticFeedback.current
    return remember(haptics) { ZenFeedback(haptics) }
}

/** [toggleable] that sounds and feels like a switch. Use for every on/off row. */
@Composable
fun Modifier.zenToggleable(value: Boolean, onValueChange: (Boolean) -> Unit, role: Role = Role.Switch): Modifier {
    val feedback = rememberZenFeedback()
    return toggleable(value = value, role = role) { on ->
        if (role == Role.Switch) feedback.toggle(on) else feedback.select()
        onValueChange(on)
    }
}
