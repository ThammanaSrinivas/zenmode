package com.zenlauncher.zenmode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.PromiseUnit
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// The three states a promise unit can be in, shared by every surface that draws the strip —
// the Zen Gold screen's "My Screen time" card and the daily check-in overlay. One definition,
// so a kept day is never green in one place and grey in the other.

/** Kept: the day (or week) came in under the promise. */
val PromiseKeptGreen: Color @Composable get() = colorResource(R.color.gold_delta_text)

/** Broken: decided, and it went over. */
val PromiseBrokenGray: Color @Composable get() = colorResource(R.color.stone_500)

/** Not decided yet — still to come, or never recorded. */
val PromiseUndecidedGray: Color @Composable get() = colorResource(R.color.stone_200)

@Composable
fun promiseUnitColor(kept: Boolean?): Color = when (kept) {
    true -> PromiseKeptGreen
    false -> PromiseBrokenGray
    null -> PromiseUndecidedGray
}

/**
 * Renders [units] as equal columns — one bar + label per day (Weekly) or week (Monthly),
 * each column centred under its own bar. Green = kept, grey = broken, light = not decided yet.
 *
 * Defaults are the Zen Gold card's own metrics (Figma node 2026:1449); the check-in overlay
 * passes larger ones, since the strip is the main event there rather than a footnote.
 */
@Composable
fun PromiseUnitBars(
    units: List<PromiseUnit>,
    modifier: Modifier = Modifier,
    barHeight: Dp = 5.5.rdp,
    barSpacing: Dp = 7.6.rdp,
    labelGap: Dp = 10.rdp,
    labelSize: TextUnit = 12.rsp,
    labelSpacing: TextUnit = (-0.24).sp,
    labelColor: Color = ZenTheme.colors.textPrimary,
    labelWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(barSpacing)
    ) {
        units.forEach { unit ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(promiseUnitColor(unit.kept))
                )
                Spacer(modifier = Modifier.height(labelGap))
                Text(
                    text = unit.label,
                    fontFamily = Geist,
                    fontWeight = labelWeight,
                    fontSize = labelSize,
                    letterSpacing = labelSpacing,
                    color = labelColor
                )
            }
        }
    }
}
