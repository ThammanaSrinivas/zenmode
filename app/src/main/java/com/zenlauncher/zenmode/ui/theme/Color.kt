package com.zenlauncher.zenmode.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Base Primitives
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val Grey100 = Color(0xFFF5F5F5)
val Grey200 = Color(0xFFE5E5E5)
val Grey400 = Color(0xFFA3A3A3)
val Grey600 = Color(0xFF525252)
val Grey800 = Color(0xFF1A1A1A)

val ZenBase = Color(0xFF00C700)
val ZenGlow = Color(0xFF24FF24)
val ZenDark = Color(0xFF007700)

@Immutable
data class ZenColors(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textBrand: Color,
    val actionPrimary: Color,
    val actionPrimaryText: Color
)

val LightZenColors = ZenColors(
    bgPrimary = White,
    bgSecondary = Grey100,
    textPrimary = Black,
    textSecondary = Grey600,
    textBrand = ZenBase,
    actionPrimary = ZenDark,
    actionPrimaryText = White
)

val DarkZenColors = ZenColors(
    bgPrimary = Black,
    bgSecondary = Grey800,
    textPrimary = White,
    textSecondary = Grey400,
    textBrand = ZenGlow,
    actionPrimary = ZenBase,
    actionPrimaryText = Black
)

val LocalZenColors = staticCompositionLocalOf { LightZenColors }
