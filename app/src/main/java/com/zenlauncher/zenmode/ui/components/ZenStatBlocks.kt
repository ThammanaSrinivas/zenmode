package com.zenlauncher.zenmode.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// ── v3 header stat blocks ─────────────────────────────────────────
// SOURCE OF TRUTH for the v3 "icon + label over value" header stat (Figma node 2001:1504).
// Home draws Zen Score and Streaks with it; the resistance screen draws the same Streaks
// block so the two screens can never drift apart. Nothing here owns state — the caller
// passes the number and, where the block is tappable, what the tap should do.

/** The small caption above each header number, shared so every block lines up exactly. */
@Composable
fun ZenStatLabel(text: String) {
    Text(
        text = text,
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = ZenStatLabelSize,
        lineHeight = ZenStatLabelSize,
        color = ZenTheme.colors.textPrimary,
        style = ZenStatTextStyle
    )
}

// One type scale for every header stat, so their blocks — and the icons sized to them —
// come out the same height and sit on the same lines.
val ZenStatLabelSize: TextUnit @Composable get() = 12.rsp
val ZenStatValueSize: TextUnit @Composable get() = 18.rsp
val ZenStatUnitSize: TextUnit @Composable get() = 12.rsp
val ZenStatLineGap: Dp @Composable get() = 3.rdp
val ZenStatIconGap: Dp @Composable get() = 6.rdp

/** Tight line boxes, so a two-line block's height is its type and nothing else. */
val ZenStatTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
)

/**
 * [icon] beside [label], with the icon sized to a square exactly as tall as the label block —
 * so the mark always matches its two lines of text, whatever the font scale.
 */
@Composable
fun ZenIconMatchedLabel(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit
) {
    val gap = ZenStatIconGap
    Layout(contents = listOf(icon, label), modifier = modifier) { (iconMeasurables, labelMeasurables), constraints ->
        val gapPx = gap.roundToPx()
        val labelPlaceable = labelMeasurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val side = labelPlaceable.height
        val iconPlaceable = iconMeasurables.first().measure(Constraints.fixed(side, side))
        layout(side + gapPx + labelPlaceable.width, side) {
            iconPlaceable.place(0, 0)
            labelPlaceable.place(side + gapPx, 0)
        }
    }
}

/**
 * The v3 Streaks block: a flame that burns as tall as the "13 days / Streaks" text beside it.
 * [onClick] is null where the block is read-only (the resistance screen is a wait, not a place
 * to go), and the flame only burns once there is a streak to burn for.
 */
@Composable
fun StreakStat(
    streaks: Int,
    modifier: Modifier = Modifier,
    reveal: HomeRevealCue = HomeRevealCue(),
    onClick: (() -> Unit)? = null
) {
    val colors = ZenTheme.colors
    val shownStreak = rememberCountUp(streaks, reveal, HomeReveal.STREAK, durationMillis = 600)

    ZenIconMatchedLabel(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier
                    .clip(RoundedCornerShape(8.rdp))
                    .clickable(onClickLabel = "Open streaks", onClick = onClick)
                    .systemGestureExclusion()
            } else Modifier
        ),
        icon = {
            BlazingFlame(
                cue = reveal,
                lit = streaks > 0,
                contentDescription = null
            )
        }
    ) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
            ZenStatLabel("Streaks")
            Spacer(modifier = Modifier.height(ZenStatLineGap))
            Row(verticalAlignment = Alignment.Bottom) {
                // Solid ink, not the brand gradient: the gradient's yellow-green washed out
                // on the green home wash. textPrimary flips dark/light with the theme.
                Text(
                    text = shownStreak.toString(),
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = ZenStatValueSize,
                    lineHeight = ZenStatValueSize,
                    color = colors.textPrimary,
                    style = ZenStatTextStyle
                )
                Spacer(modifier = Modifier.width(3.rdp))
                Text(
                    text = if (streaks == 1) "day" else "days",
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = ZenStatUnitSize,
                    lineHeight = ZenStatUnitSize,
                    color = colors.textSecondary,
                    style = ZenStatTextStyle
                )
            }
        }
    }
}
