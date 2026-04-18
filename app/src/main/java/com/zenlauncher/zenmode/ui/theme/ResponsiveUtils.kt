package com.zenlauncher.zenmode.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screen-width-based scale factor for responsive UI.
 * Reference device: Pixel 6 (411dp width).
 * On a 360dp phone, scale ≈ 0.876 — fonts and spacing shrink proportionally.
 */

private const val REFERENCE_WIDTH_DP = 411f

val LocalScreenScale = compositionLocalOf { 1f }

@Composable
fun ProvideScreenScale(content: @Composable () -> Unit) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    val scale = (screenWidthDp / REFERENCE_WIDTH_DP).coerceIn(0.75f, 1.2f)
    CompositionLocalProvider(LocalScreenScale provides scale) {
        content()
    }
}

/** Responsive sp — scales font size with screen width */
val Int.rsp: TextUnit
    @Composable get() = (this * LocalScreenScale.current).sp

val Float.rsp: TextUnit
    @Composable get() = (this * LocalScreenScale.current).sp

/** Responsive dp — scales dimensions with screen width */
val Int.rdp: Dp
    @Composable get() = (this * LocalScreenScale.current).dp

val Float.rdp: Dp
    @Composable get() = (this * LocalScreenScale.current).dp
