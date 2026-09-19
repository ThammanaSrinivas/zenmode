package com.zenlauncher.zenmode.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── ZM_OS v3 shared building blocks ─────────────────────────────────
// Pieces the Zen Gold sub-screens (My Promise 2026:1793, Invest Gold 2026:1250)
// share: the same frame header, gradient stepper card, rolling numbers and pill CTA.
// Light-only like those screens, so colors come straight from colors.xml tokens.

// Figma's "normal" line height is each font's typo metrics (ascender - descender + gap);
// Android lays these fonts out taller, so every text pins it and keeps the full line box.
internal const val ClashLineHeight = 1.23f
internal const val GeistLineHeight = 1.3f
internal const val DepartureMonoLineHeight = 1.2727f
internal val FullLineBox = TextStyle(
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Proportional, LineHeightStyle.Trim.None)
)

internal val V3BrandGreen: Color @Composable get() = colorResource(R.color.gold_delta_text)

// ── Header ────────────────────────────────────────────────────────

/**
 * Back arrow, dotted title, optional trailing action. The title's dot sits [dotGap]
 * before it; with [centerOnTitle] only the word is centred (dot hangs to its left),
 * otherwise dot + word are centred as a group.
 */
@Composable
internal fun V3ScreenHeader(
    title: String,
    backLabel: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotGap: Dp = 10.rdp,
    centerOnTitle: Boolean = false,
    dotPulse: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val margin = 33.rdp
    val arrowWidth = 29.3673.rdp
    val touchTarget = 48.rdp
    val dotSize = 7.rdp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.rdp)
    ) {
        // 48dp hit area centred on the 29dp glyph without pushing the header's layout.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = margin - (touchTarget - arrowWidth) / 2)
                .requiredSize(touchTarget)
                .pressScale(onClick = onBackClick, onClickLabel = backLabel),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_my_promise_back),
                contentDescription = "Back",
                modifier = Modifier
                    .width(arrowWidth)
                    .height(20.1378.rdp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = if (centerOnTitle) -(dotSize + dotGap) / 2 else 0.dp)
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dotGap)
        ) {
            val pulse = remember { Animatable(1f) }
            if (dotPulse) {
                LaunchedEffect(Unit) {
                    while (true) {
                        pulse.animateTo(1.45f, tween(durationMillis = 700))
                        pulse.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
                        delay(1_400)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        scaleX = pulse.value
                        scaleY = pulse.value
                    }
                    .clip(CircleShape)
                    .background(colorResource(R.color.my_promise_title_dot))
            )
            Text(
                text = title,
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 24.rsp,
                lineHeight = (24 * ClashLineHeight).rsp,
                letterSpacing = (-0.48).sp,
                color = colorResource(R.color.zen_900),
                maxLines = 1,
                style = FullLineBox
            )
        }

        if (trailing != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 32.rdp)
            ) {
                trailing()
            }
        }
    }
}

// ── Card ──────────────────────────────────────────────────────────

/**
 * The v3 stepper card surface: a vertical gradient with a CSS-style top-only border on
 * a rounded box. The border is the outer rounded rect minus the inner padding box, so it
 * tapers into the corners exactly like the web render.
 */
@Composable
internal fun Modifier.v3GradientCard(): Modifier {
    val topColor = colorResource(R.color.my_promise_card_top)
    val bottomColor = colorResource(R.color.my_promise_card_bottom)
    val borderColor = V3BrandGreen
    val radius = 24.rdp
    val borderWidth = 3.rdp

    return drawWithCache {
        val r = radius.toPx()
        val b = borderWidth.toPx()
        val outer = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r)))
        }
        val innerTopCorner = CornerRadius(r, (r - b).coerceAtLeast(0f))
        val inner = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = b,
                    right = size.width,
                    bottom = size.height,
                    topLeftCornerRadius = innerTopCorner,
                    topRightCornerRadius = innerTopCorner,
                    bottomRightCornerRadius = CornerRadius(r),
                    bottomLeftCornerRadius = CornerRadius(r)
                )
            )
        }
        val border = Path.combine(PathOperation.Difference, outer, inner)
        val fill = Brush.verticalGradient(listOf(topColor, bottomColor), startY = 0f, endY = size.height)
        onDrawBehind {
            drawPath(outer, fill)
            drawPath(border, borderColor)
        }
    }
}

@Composable
internal fun V3CardDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 28.rdp)
            .fillMaxWidth()
            .height(1.dp)
            .background(colorResource(R.color.my_promise_divider))
    )
}

/** A list-style bullet centred in a fixed-width slot, as Figma renders "A • B" labels. */
@Composable
internal fun V3BulletDot(slotWidth: Dp, dotSize: Dp, color: Color) {
    Box(modifier = Modifier.width(slotWidth), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(color)
        )
    }
}

// ── Stepper ───────────────────────────────────────────────────────

/**
 * Minus button, big rolling number with a caption under it, plus button — laid out at
 * the offsets both v3 stepper cards share. [rollUp] is set by the caller from the tap
 * itself so every rolling number on the screen agrees on direction.
 */
@Composable
internal fun V3ValueStepper(
    value: Int,
    maxValue: Int,
    rollUp: Boolean,
    caption: String,
    captionLetterSpacing: TextUnit,
    captionGap: Dp,
    valueDescription: String,
    decreaseLabel: String,
    increaseLabel: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onStep: (Int) -> Unit,
    minDigits: Int = 1
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        V3StepperButton(
            enabled = canDecrease,
            label = decreaseLabel,
            onClick = { onStep(-1) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 28.rdp, top = 22.rdp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_my_promise_minus_button),
                contentDescription = null,
                modifier = Modifier
                    .width(42.7886.rdp)
                    .height(44.7886.rdp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-1).rdp)
                .clearAndSetSemantics {
                    contentDescription = valueDescription
                    liveRegion = LiveRegionMode.Polite
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(captionGap)
        ) {
            // Figma fixes this text box at 74 tall while the font's line is ~85, so the
            // caption sits relative to the box, not below the glyph line.
            Box(modifier = Modifier.height(74.rdp)) {
                V3RollingNumber(
                    value = value,
                    maxValue = maxValue,
                    rollUp = rollUp,
                    minDigits = minDigits,
                    style = FullLineBox.copy(
                        fontFamily = DepartureMono,
                        fontSize = 67.019.rsp,
                        lineHeight = (67.019 * DepartureMonoLineHeight).rsp,
                        letterSpacing = (-2.0106).sp,
                        color = Color.Black
                    ),
                    modifier = Modifier.wrapContentHeight(Alignment.Top, unbounded = true)
                )
            }
            Text(
                text = caption,
                fontFamily = DepartureMono,
                fontSize = 13.404.rsp,
                lineHeight = (13.404 * DepartureMonoLineHeight).rsp,
                letterSpacing = captionLetterSpacing,
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = FullLineBox
            )
        }

        V3StepperButton(
            enabled = canIncrease,
            label = increaseLabel,
            onClick = { onStep(1) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 31.2.rdp, top = 22.rdp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.8.rdp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, colorResource(R.color.my_promise_stepper_border), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_my_promise_plus),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = 0.45.rdp, y = 0.45.rdp)
                        .size(11.7.rdp)
                )
            }
        }
    }
}

@Composable
private fun V3StepperButton(
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                role = Role.Button,
                onClickLabel = label,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    scope.launch {
                        scale.animateTo(0.88f, tween(durationMillis = 70))
                        scale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
                    }
                    onClick()
                }
            )
            .semantics { contentDescription = label }
    ) {
        content()
    }
}

// ── Rolling number ────────────────────────────────────────────────

/**
 * Odometer-style number: each digit slot rolls independently, so 21 -> 28 only
 * rolls the ones digit. Slots are keyed from the right and sized to [maxValue] (or
 * [minDigits] when zero-padded), so a digit appearing (7 -> 14) grows its slot from
 * zero width and the number re-centres smoothly instead of jumping.
 */
@Composable
internal fun V3RollingNumber(
    value: Int,
    maxValue: Int,
    rollUp: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    minDigits: Int = 1
) {
    val digits = value.toString().padStart(minDigits, '0')
    val slotCount = maxOf(maxValue.toString().length, digits.length)

    Row(modifier = modifier) {
        for (slot in slotCount - 1 downTo 0) {
            key(slot) {
                AnimatedContent(
                    targetState = digits.getOrNull(digits.length - 1 - slot),
                    transitionSpec = { digitRoll(rollUp) },
                    label = "RollingDigit"
                ) { digit ->
                    if (digit != null) {
                        Text(text = digit.toString(), style = style, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<Char?>.digitRoll(rollUp: Boolean): ContentTransform {
    val direction = if (rollUp) 1 else -1
    val slide = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)
    return (slideInVertically(slide) { height -> direction * height } + fadeIn(tween(220, delayMillis = 40)))
        .togetherWith(slideOutVertically(slide) { height -> -direction * height } + fadeOut(tween(160)))
        .using(SizeTransform(clip = true) { _, _ -> spring(stiffness = Spring.StiffnessMediumLow) })
}

// ── Buttons & motion helpers ──────────────────────────────────────

@Composable
internal fun V3PrimaryPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.rdp)
            .pressScale(onClick = onClick, pressedScale = 0.97f)
            .clip(RoundedCornerShape(percent = 50))
            .background(colorResource(R.color.zen_700)),
        contentAlignment = Alignment.Center
    ) {
        V3PillButtonText(text = text, color = Color.White)
    }
}

@Composable
internal fun V3PillButtonText(text: String, color: Color) {
    Text(
        text = text,
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 17.575.rsp,
        lineHeight = (17.575 * GeistLineHeight).rsp,
        letterSpacing = (-0.3515).sp,
        color = color,
        maxLines = 1,
        style = FullLineBox
    )
}

/** Clickable without a ripple that springs down while held and back on release. */
@Composable
internal fun Modifier.pressScale(
    onClick: () -> Unit,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    pressedScale: Float = 0.92f
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        scale.animateTo(
            if (pressed) pressedScale else 1f,
            spring(dampingRatio = if (pressed) 1f else 0.45f, stiffness = Spring.StiffnessMedium)
        )
    }
    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .clickable(
            enabled = enabled,
            indication = null,
            interactionSource = interaction,
            role = Role.Button,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
}

/**
 * Entrance for a screen section: fades in and rises [rise] into place, starting
 * [index] × [stepMillis] after first composition so sections cascade top-down.
 * Runs once per composition entry; system animator scale (incl. "remove animations")
 * applies automatically.
 */
@Composable
internal fun Modifier.staggeredEntrance(index: Int, stepMillis: Int = 55, rise: Dp = 18.dp): Modifier {
    val inspection = LocalInspectionMode.current
    // Previews and screenshot tests render a single frame, so they get the settled state.
    val progress = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(index * stepMillis.toLong())
        progress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow))
    }
    val risePx = with(LocalDensity.current) { rise.toPx() }
    return graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * risePx
    }
}
