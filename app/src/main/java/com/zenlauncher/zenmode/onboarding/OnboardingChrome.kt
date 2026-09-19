package com.zenlauncher.zenmode.onboarding

import com.zenlauncher.zenmode.ui.components.BrandedText
import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.FullLineBox
import com.zenlauncher.zenmode.ui.components.GeistLineHeight
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// ── ZenMode OS onboarding chrome ────────────────────────────────────
// Light-only like the other v3 sub-screens, so colors come straight from colors.xml.
// The stories step is the one dark screen and passes `dark = true` where it matters.

internal val OnboardingMargin: Dp @Composable get() = 24.rdp

/** Paper page with the status/nav bar insets applied, a top bar and a sticky bottom slot. */
@Composable
internal fun OnboardingPage(
    modifier: Modifier = Modifier,
    background: Color = colorResource(R.color.paper_base),
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        topBar?.invoke()
        Column(modifier = Modifier.weight(1f).fillMaxWidth(), content = content)
        if (bottomBar != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = OnboardingMargin, end = OnboardingMargin, top = 12.rdp, bottom = 16.rdp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.rdp),
                content = bottomBar
            )
        }
    }
}

/**
 * Revolut-style segmented progress: one thin bar per step, completed ones full, the
 * current one filled to [currentFraction]. Stories drive the fraction frame by frame.
 */
@Composable
internal fun SegmentedProgress(
    segments: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    currentFraction: Float = 1f,
    dark: Boolean = false
) {
    val track = if (dark) Color.White.copy(alpha = 0.22f) else colorResource(R.color.paper_hairline)
    val fill = if (dark) Color.White else colorResource(R.color.ink_surface)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = (currentIndex + currentFraction).coerceAtLeast(0f),
                    range = 0f..segments.toFloat()
                )
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(segments) { index ->
            val target = when {
                index < currentIndex -> 1f
                index == currentIndex -> currentFraction
                else -> 0f
            }
            // Stories feed a per-frame fraction already; steps snap between 0 and 1, so ease those.
            val animated by animateFloatAsState(target, spring(stiffness = 300f), label = "segment")
            val shown = if (dark) target else animated
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(track)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shown.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(fill)
                )
            }
        }
    }
}

/** Where a step sits in the flow, so each step can draw the shared top bar without knowing the rest. */
@Immutable
data class StepProgress(val segments: Int, val currentIndex: Int) {
    /** 0–100 through the whole flow; [fraction] is how far into this step the user is. */
    fun percent(fraction: Float = 0f): Int = OnboardingFlow.percentComplete(currentIndex, segments, fraction)

    @Composable
    fun TopBar(
        onBack: (() -> Unit)?,
        eyebrow: String = "Welcome to ZenMode OS",
        trailing: (@Composable () -> Unit)? = null
    ) = OnboardingTopBar(
        segments = segments,
        currentIndex = currentIndex,
        onBack = onBack,
        percent = percent(),
        eyebrow = eyebrow,
        trailing = trailing
    )
}

/**
 * Progress bar with the flow's percentage beside it, then back arrow + "ZenMode OS"
 * eyebrow and an optional trailing slot.
 */
@Composable
internal fun OnboardingTopBar(
    segments: Int,
    currentIndex: Int,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    currentFraction: Float = 1f,
    dark: Boolean = false,
    percent: Int? = null,
    eyebrow: String = "Welcome to ZenMode OS",
    trailing: (@Composable () -> Unit)? = null
) {
    val content = if (dark) Color.White else colorResource(R.color.ink_surface)
    Column(modifier = modifier.padding(horizontal = 16.rdp).padding(top = 10.rdp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SegmentedProgress(
                segments = segments,
                currentIndex = currentIndex,
                currentFraction = currentFraction,
                dark = dark,
                modifier = Modifier.weight(1f)
            )
            if (percent != null) {
                Spacer(Modifier.width(10.rdp))
                Text(
                    text = "$percent%",
                    fontFamily = DepartureMono,
                    fontSize = 12.rsp,
                    lineHeight = 14.rsp,
                    color = if (dark) Color.White.copy(alpha = 0.72f) else colorResource(R.color.stone_600),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    // Wide enough for "100%" so the bar doesn't twitch as the number grows.
                    modifier = Modifier
                        .width(36.rdp)
                        .semantics { contentDescription = "$percent percent complete" }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .requiredSize(44.dp)
                        .pressScale(onClick = onBack, onClickLabel = "Back"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = content,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Image(
                painter = painterResource(R.drawable.ic_zen_mark_gradient),
                contentDescription = null,
                modifier = Modifier.size(16.rdp).alpha(if (dark) 0.9f else 1f)
            )
            Spacer(Modifier.width(8.rdp))
            BrandedText(
                text = eyebrow,
                style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.rsp, color = content),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
    }
}

// ── Type ──────────────────────────────────────────────────────────

/** Display headline — Clash Display 500, the brand voice. */
@Composable
internal fun OnboardingHeadline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = colorResource(R.color.ink_surface),
    size: Float = 34f,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = size.rsp,
        lineHeight = (size * 1.08f).rsp,
        letterSpacing = (-size * 0.02f).sp,
        color = color,
        textAlign = textAlign,
        style = FullLineBox,
        modifier = modifier.semantics { heading() }
    )
}

@Composable
internal fun OnboardingBody(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = colorResource(R.color.stone_600),
    size: Float = 16f,
    textAlign: TextAlign = TextAlign.Start
) {
    BrandedText(
        text = text,
        style = TextStyle(
            fontFamily = Geist,
            fontSize = size.rsp,
            lineHeight = (size * 1.45f).rsp,
            letterSpacing = (-0.01f * size).sp,
            color = color,
            textAlign = textAlign
        ),
        modifier = modifier
    )
}

/** DepartureMono eyebrow, the v3 section label. Onboarding keeps it in sentence case so it reads easily. */
@Composable
internal fun OnboardingEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = colorResource(R.color.zen_700)
) {
    Text(
        text = text,
        fontFamily = DepartureMono,
        fontSize = 12.rsp,
        lineHeight = 16.rsp,
        letterSpacing = 1.2.sp,
        color = color,
        maxLines = 1,
        modifier = modifier
    )
}

// ── Buttons ───────────────────────────────────────────────────────

enum class OnboardingButtonStyle { Ink, Brand, Light, Outline }

@Composable
internal fun OnboardingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: OnboardingButtonStyle = OnboardingButtonStyle.Ink,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    val (container, content, border) = when (style) {
        OnboardingButtonStyle.Ink -> Triple(colorResource(R.color.ink_surface), colorResource(R.color.paper_ink), null)
        OnboardingButtonStyle.Brand -> Triple(colorResource(R.color.zen_700), Color.White, null)
        OnboardingButtonStyle.Light -> Triple(Color.White, colorResource(R.color.ink_surface), null)
        OnboardingButtonStyle.Outline -> Triple(
            Color.Transparent,
            colorResource(R.color.ink_surface),
            BorderStroke(1.dp, colorResource(R.color.paper_hairline))
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.rdp)
            .pressScale(onClick = onClick, enabled = enabled, pressedScale = 0.97f)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(percent = 50)) else Modifier),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.rdp))
        }
        Text(
            text = text,
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.rsp,
            lineHeight = (17 * GeistLineHeight).rsp,
            letterSpacing = (-0.3).sp,
            color = content,
            maxLines = 1,
            style = FullLineBox
        )
    }
}

/** Quiet text action under the main CTA ("Skip for now", "Maybe later"). */
@Composable
internal fun OnboardingTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = colorResource(R.color.stone_600)
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .pressScale(onClick = onClick, pressedScale = 0.95f)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 15.rsp,
            color = color,
            maxLines = 1
        )
    }
}

/** Small rounded tag, e.g. "Open source" or "Halfway there". */
@Composable
internal fun OnboardingChip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = colorResource(R.color.zen_050),
    content: Color = colorResource(R.color.zen_700),
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .padding(horizontal = 10.rdp, vertical = 5.rdp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.rdp)
    ) {
        leading?.invoke()
        Text(
            text = text,
            style = TextStyle(
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.rsp,
                letterSpacing = 0.1.sp,
                color = content
            ),
            maxLines = 1
        )
    }
}

/**
 * Light status/nav icons while a dark screen is up; restores whatever was there after.
 * Re-asserted after every composition because ZenTheme sets them in its own SideEffect.
 */
@Composable
internal fun DarkSystemBars() {
    val view = LocalView.current
    if (view.isInEditMode) return
    val controller = remember(view) {
        (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
    } ?: return
    // Read during composition, before the SideEffect below flips them.
    val previous = remember(controller) {
        controller.isAppearanceLightStatusBars to controller.isAppearanceLightNavigationBars
    }
    SideEffect {
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }
    DisposableEffect(controller) {
        onDispose {
            controller.isAppearanceLightStatusBars = previous.first
            controller.isAppearanceLightNavigationBars = previous.second
        }
    }
}
