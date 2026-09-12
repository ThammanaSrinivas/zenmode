package com.zenlauncher.zenmode.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * App-wide layout tokens — the single source for numbers that were otherwise
 * retyped as bare dp literals on every screen. [screenMargin] alone was copied as
 * `20.dp` or `20.rdp` in over a dozen files before this existed.
 *
 * Each value scales with [rdp], so screens keep their existing responsive sizing;
 * this only removes the duplication, not the behaviour.
 */
object Spacing {
    /** Standard side margin for a full-bleed screen. */
    val screenMargin: Dp @Composable get() = 20.rdp

    /** Inner padding for a card or sheet. */
    val cardPadding: Dp @Composable get() = 16.rdp

    /** Minimum touch target per the design system's target-min token. */
    val touchTarget: Dp @Composable get() = 48.rdp
}
