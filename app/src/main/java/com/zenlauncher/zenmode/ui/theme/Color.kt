package com.zenlauncher.zenmode.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.MoodState


// Base Primitives
val White: Color @Composable get() = colorResource(R.color.white)
val Black: Color @Composable get() = colorResource(R.color.black)
val Grey100: Color @Composable get() = colorResource(R.color.grey_100)
val Grey200: Color @Composable get() = colorResource(R.color.grey_200)
val Grey400: Color @Composable get() = colorResource(R.color.grey_400)
val Grey600: Color @Composable get() = colorResource(R.color.grey_600)
val Grey800: Color @Composable get() = colorResource(R.color.grey_800)

val ZenBase: Color @Composable get() = colorResource(R.color.zen_base)
val ZenGlow: Color @Composable get() = colorResource(R.color.zen_glow)
val ZenDark: Color @Composable get() = colorResource(R.color.zen_dark)

@Immutable
data class ZenColors(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val surfaceElevated: Color,
    val borderSubtle: Color,
    val borderFocus: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textBrand: Color,
    val actionPrimary: Color,
    val actionPrimaryText: Color,
    val moodHappy: Color,
    val moodNeutral: Color,
    val moodAnnoyed: Color,
    val innerShadow: Color,
    val statsCardFillHappy: Color,
    val statsCardFillNeutral: Color,
    val statsCardFillAnnoyed: Color,
    val strokeHappy: Color,
    val strokeNeutral: Color,
    val strokeAnnoyed: Color
)

fun ZenColors.statsCardFill(mood: MoodState): Color =
    when (mood) {
        MoodState.HAPPY -> statsCardFillHappy
        MoodState.NEUTRAL -> statsCardFillNeutral
        MoodState.ANNOYED -> statsCardFillAnnoyed
    }


fun ZenColors.percentageChangeColor(percent: Int): Color =
    if (percent < 0) moodHappy else moodAnnoyed

val LightZenColors: ZenColors
    @Composable
    get() = ZenColors(
        bgPrimary = White,
        bgSecondary = Grey100,
        surfaceElevated = White,
        borderSubtle = Grey200,
        borderFocus = ZenBase,
        textPrimary = Black,
        textSecondary = Grey600,
        textBrand = ZenBase,
        actionPrimary = ZenDark,
        actionPrimaryText = White,
        moodHappy = Color(0xFF00C700),
        moodNeutral = Color(0xFFEBDE27),
        moodAnnoyed = Color(0xFFF1634F),
        statsCardFillHappy = Color(0x1AB9E234),
        statsCardFillNeutral = Color(0x1AEBDE28),
        statsCardFillAnnoyed = Color(0x1AF1634F),
        innerShadow = Color(0x40000000),
        strokeHappy = Color(0xFF006703),
        strokeNeutral = Color(0xFFEBDE28),
        strokeAnnoyed = Color(0xFFFF7C69)

    )

val DarkZenColors: ZenColors
    @Composable
    get() = ZenColors(
        bgPrimary = Black,
        bgSecondary = Grey800,
        surfaceElevated = Grey600,
        borderSubtle = Grey800,
        borderFocus = ZenGlow,
        textPrimary = White,
        textSecondary = Grey400,
        textBrand = ZenGlow,
        actionPrimary = ZenBase,
        actionPrimaryText = Black,
        moodHappy = Color(0xFF00C700),
        moodNeutral = Color(0xFFEBDE27),
        moodAnnoyed = Color(0xFFF1634F),
        statsCardFillHappy = Color(0x1AB9E234),
        statsCardFillNeutral = Color(0x1AEBDE28),
        statsCardFillAnnoyed = Color(0x1AF1634F),
        innerShadow = Color(0x40000000),
        strokeHappy = Color(0xFF006703),
        strokeNeutral = Color(0xFFEBDE28),
        strokeAnnoyed = Color(0xFFFF7C69)

    )

val LocalZenColors = staticCompositionLocalOf {
    ZenColors(
        bgPrimary = Color.Unspecified,
        bgSecondary = Color.Unspecified,
        surfaceElevated = Color.Unspecified,
        borderSubtle = Color.Unspecified,
        borderFocus = Color.Unspecified,
        textPrimary = Color.Unspecified,
        textSecondary = Color.Unspecified,
        textBrand = Color.Unspecified,
        actionPrimary = Color.Unspecified,
        actionPrimaryText = Color.Unspecified,
        moodHappy = Color.Unspecified,
        moodNeutral = Color.Unspecified,
        moodAnnoyed = Color.Unspecified,
        statsCardFillHappy = Color.Unspecified,
        statsCardFillNeutral = Color.Unspecified,
        statsCardFillAnnoyed = Color.Unspecified,
        innerShadow = Color.Unspecified,
        strokeHappy = Color.Unspecified,
        strokeNeutral = Color.Unspecified,
        strokeAnnoyed = Color.Unspecified

    )
}
