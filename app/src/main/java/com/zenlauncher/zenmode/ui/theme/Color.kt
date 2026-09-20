package com.zenlauncher.zenmode.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.colorResource
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.MoodState

// ── Primitives ────────────────────────────────────────────────────

val ZenOn: Color @Composable get() = colorResource(R.color.zen_on)
val AmberGraphic: Color @Composable get() = colorResource(R.color.amber_500)

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
    /** Deep brand green for headings and titles: zen_900 on paper, zen_300 on ink. */
    val textBrandStrong: Color,
    val actionPrimary: Color,
    val actionPrimaryText: Color,
    val accentReward: Color,
    val accentRewardGraphic: Color,
    val accentScore: Color,
    val washHappy: List<Color>,
    val washNeutral: List<Color>,
    val washAnnoyed: List<Color>,
    val cardHappy: List<Color>,
    val cardNeutral: List<Color>,
    val cardAnnoyed: List<Color>,
    val moodHappy: Color,
    val moodNeutral: Color,
    val moodAnnoyed: Color,
    val innerShadow: Color,
    val bgMoodHappy: Color,
    val bgMoodNeutral: Color,
    val bgMoodAnnoyed: Color,
    val statsCardFillHappy: Color,
    val statsCardFillNeutral: Color,
    val statsCardFillAnnoyed: Color,
    val strokeHappy: Color,
    val strokeNeutral: Color,
    val strokeAnnoyed: Color,
    val notificationBadgeStroke: Color,
    // Settings / Pro surfaces (v3). Pairing budget: green + amber on the settings screen;
    // ember only inside the destructive account sheet.
    val surfaceSunk: Color,
    val surfaceTint: Color,
    val surfaceTintLine: Color,
    val textOnTint: Color,
    val textMuted: Color,
    /** Between secondary and muted: captions and eyebrows that still need to read (stone_500 / stone_400). */
    val textTertiary: Color,
    /** Text and icons sitting on a [textBrand] fill: white on paper, ink on the brighter ink-theme green. */
    val textOnBrand: Color,
    val borderOutline: Color,
    val borderHairlineSoft: Color,
    val rewardSurface: Color,
    val rewardSurfaceLine: Color,
    val accentDeduct: Color,
    val textOnDeduct: Color,
    val switchThumb: Color
)

/** Every token of [a] blended toward [b] by [t] — drives the paper ↔ ink crossfade in ZenTheme. */
fun lerp(a: ZenColors, b: ZenColors, t: Float): ZenColors =
    ZenColors(
        bgPrimary = lerp(a.bgPrimary, b.bgPrimary, t),
        bgSecondary = lerp(a.bgSecondary, b.bgSecondary, t),
        surfaceElevated = lerp(a.surfaceElevated, b.surfaceElevated, t),
        borderSubtle = lerp(a.borderSubtle, b.borderSubtle, t),
        borderFocus = lerp(a.borderFocus, b.borderFocus, t),
        textPrimary = lerp(a.textPrimary, b.textPrimary, t),
        textSecondary = lerp(a.textSecondary, b.textSecondary, t),
        textBrand = lerp(a.textBrand, b.textBrand, t),
        textBrandStrong = lerp(a.textBrandStrong, b.textBrandStrong, t),
        actionPrimary = lerp(a.actionPrimary, b.actionPrimary, t),
        actionPrimaryText = lerp(a.actionPrimaryText, b.actionPrimaryText, t),
        accentReward = lerp(a.accentReward, b.accentReward, t),
        accentRewardGraphic = lerp(a.accentRewardGraphic, b.accentRewardGraphic, t),
        accentScore = lerp(a.accentScore, b.accentScore, t),
        washHappy = a.washHappy.zip(b.washHappy) { x, y -> lerp(x, y, t) },
        washNeutral = a.washNeutral.zip(b.washNeutral) { x, y -> lerp(x, y, t) },
        washAnnoyed = a.washAnnoyed.zip(b.washAnnoyed) { x, y -> lerp(x, y, t) },
        cardHappy = a.cardHappy.zip(b.cardHappy) { x, y -> lerp(x, y, t) },
        cardNeutral = a.cardNeutral.zip(b.cardNeutral) { x, y -> lerp(x, y, t) },
        cardAnnoyed = a.cardAnnoyed.zip(b.cardAnnoyed) { x, y -> lerp(x, y, t) },
        moodHappy = lerp(a.moodHappy, b.moodHappy, t),
        moodNeutral = lerp(a.moodNeutral, b.moodNeutral, t),
        moodAnnoyed = lerp(a.moodAnnoyed, b.moodAnnoyed, t),
        innerShadow = lerp(a.innerShadow, b.innerShadow, t),
        bgMoodHappy = lerp(a.bgMoodHappy, b.bgMoodHappy, t),
        bgMoodNeutral = lerp(a.bgMoodNeutral, b.bgMoodNeutral, t),
        bgMoodAnnoyed = lerp(a.bgMoodAnnoyed, b.bgMoodAnnoyed, t),
        statsCardFillHappy = lerp(a.statsCardFillHappy, b.statsCardFillHappy, t),
        statsCardFillNeutral = lerp(a.statsCardFillNeutral, b.statsCardFillNeutral, t),
        statsCardFillAnnoyed = lerp(a.statsCardFillAnnoyed, b.statsCardFillAnnoyed, t),
        strokeHappy = lerp(a.strokeHappy, b.strokeHappy, t),
        strokeNeutral = lerp(a.strokeNeutral, b.strokeNeutral, t),
        strokeAnnoyed = lerp(a.strokeAnnoyed, b.strokeAnnoyed, t),
        notificationBadgeStroke = lerp(a.notificationBadgeStroke, b.notificationBadgeStroke, t),
        surfaceSunk = lerp(a.surfaceSunk, b.surfaceSunk, t),
        surfaceTint = lerp(a.surfaceTint, b.surfaceTint, t),
        surfaceTintLine = lerp(a.surfaceTintLine, b.surfaceTintLine, t),
        textOnTint = lerp(a.textOnTint, b.textOnTint, t),
        textMuted = lerp(a.textMuted, b.textMuted, t),
        textTertiary = lerp(a.textTertiary, b.textTertiary, t),
        textOnBrand = lerp(a.textOnBrand, b.textOnBrand, t),
        borderOutline = lerp(a.borderOutline, b.borderOutline, t),
        borderHairlineSoft = lerp(a.borderHairlineSoft, b.borderHairlineSoft, t),
        rewardSurface = lerp(a.rewardSurface, b.rewardSurface, t),
        rewardSurfaceLine = lerp(a.rewardSurfaceLine, b.rewardSurfaceLine, t),
        accentDeduct = lerp(a.accentDeduct, b.accentDeduct, t),
        textOnDeduct = lerp(a.textOnDeduct, b.textOnDeduct, t),
        switchThumb = lerp(a.switchThumb, b.switchThumb, t)
    )

/** True on the ink (dark) palette, and past the midpoint of a crossfade toward it. */
val ZenColors.isInk: Boolean get() = bgPrimary.luminance() < 0.5f

fun ZenColors.statsCardFill(mood: MoodState): Color =
    when (mood) {
        MoodState.HAPPY -> statsCardFillHappy
        MoodState.NEUTRAL -> statsCardFillNeutral
        MoodState.ANNOYED -> statsCardFillAnnoyed
    }

fun ZenColors.statsCardStroke(mood: MoodState): Color =
    when (mood) {
        MoodState.HAPPY -> strokeHappy
        MoodState.NEUTRAL -> strokeNeutral
        MoodState.ANNOYED -> strokeAnnoyed
    }

/**
 * Whole-screen wash for the home screen. One of three, picked by today's screen time.
 * Three stops, not two — the mood colour pools in the middle of the screen and fades
 * back out at both edges, which is what the v3 render does.
 */
fun ZenColors.moodWash(mood: MoodState): List<Color> =
    when (mood) {
        MoodState.HAPPY -> washHappy
        MoodState.NEUTRAL -> washNeutral
        MoodState.ANNOYED -> washAnnoyed
    }

/** Stat card fill: face colour on top, body colour beneath. */
fun ZenColors.cardGradient(mood: MoodState): List<Color> =
    when (mood) {
        MoodState.HAPPY -> cardHappy
        MoodState.NEUTRAL -> cardNeutral
        MoodState.ANNOYED -> cardAnnoyed
    }

fun ZenColors.percentageChangeColor(percent: Int): Color =
    if (percent < 0) moodHappy else moodAnnoyed

val LightZenColors: ZenColors
    @Composable
    get() = ZenColors(
        bgPrimary = colorResource(R.color.paper_base),
        bgSecondary = colorResource(R.color.paper_raised),
        surfaceElevated = colorResource(R.color.paper_white),
        borderSubtle = colorResource(R.color.paper_hairline),
        borderFocus = colorResource(R.color.zen_700),
        textPrimary = colorResource(R.color.ink_surface),
        textSecondary = colorResource(R.color.stone_600),
        textBrand = colorResource(R.color.zen_700),
        textBrandStrong = colorResource(R.color.zen_900),
        actionPrimary = colorResource(R.color.ink_surface),
        actionPrimaryText = colorResource(R.color.paper_ink),
        accentReward = colorResource(R.color.amber_800),
        accentRewardGraphic = colorResource(R.color.amber_500),
        accentScore = colorResource(R.color.score_orange),
        washHappy = listOf(
            colorResource(R.color.wash_happy_edge),
            colorResource(R.color.wash_happy_core),
            colorResource(R.color.wash_happy_edge)
        ),
        washNeutral = listOf(
            colorResource(R.color.wash_neutral_edge),
            colorResource(R.color.wash_neutral_core),
            colorResource(R.color.wash_neutral_edge)
        ),
        washAnnoyed = listOf(
            colorResource(R.color.wash_annoyed_edge),
            colorResource(R.color.wash_annoyed_core),
            colorResource(R.color.wash_annoyed_edge)
        ),
        cardHappy = listOf(
            colorResource(R.color.card_happy_face),
            colorResource(R.color.card_happy_body)
        ),
        cardNeutral = listOf(
            colorResource(R.color.card_neutral_face),
            colorResource(R.color.card_neutral_body)
        ),
        cardAnnoyed = listOf(
            colorResource(R.color.card_annoyed_face),
            colorResource(R.color.card_annoyed_body)
        ),
        moodHappy = colorResource(R.color.zen_700),
        moodNeutral = colorResource(R.color.amber_800),
        moodAnnoyed = colorResource(R.color.ember_700),
        innerShadow = Color(0x40000000),
        bgMoodHappy = colorResource(R.color.zen_050),
        bgMoodNeutral = colorResource(R.color.amber_on),
        bgMoodAnnoyed = colorResource(R.color.ember_on),
        statsCardFillHappy = colorResource(R.color.zen_050),
        statsCardFillNeutral = colorResource(R.color.amber_on),
        statsCardFillAnnoyed = colorResource(R.color.ember_on),
        strokeHappy = colorResource(R.color.zen_700),
        strokeNeutral = colorResource(R.color.amber_500),
        strokeAnnoyed = colorResource(R.color.ember_500),
        notificationBadgeStroke = colorResource(R.color.ink_surface),
        surfaceSunk = colorResource(R.color.paper_sunk),
        surfaceTint = colorResource(R.color.zen_050),
        surfaceTintLine = colorResource(R.color.zen_050_line),
        textOnTint = colorResource(R.color.zen_ink),
        textMuted = colorResource(R.color.stone_400),
        textTertiary = colorResource(R.color.stone_500),
        textOnBrand = colorResource(R.color.paper_white),
        borderOutline = colorResource(R.color.paper_outline),
        borderHairlineSoft = colorResource(R.color.paper_hairline_soft),
        rewardSurface = colorResource(R.color.amber_on),
        rewardSurfaceLine = colorResource(R.color.amber_on_line),
        accentDeduct = colorResource(R.color.ember_700),
        textOnDeduct = colorResource(R.color.ember_on),
        switchThumb = colorResource(R.color.paper_white)
    )

val DarkZenColors: ZenColors
    @Composable
    get() = ZenColors(
        bgPrimary = colorResource(R.color.ink_base),
        bgSecondary = colorResource(R.color.ink_surface),
        surfaceElevated = colorResource(R.color.ink_raised),
        borderSubtle = colorResource(R.color.ink_line),
        borderFocus = colorResource(R.color.zen_300),
        textPrimary = colorResource(R.color.paper_ink),
        textSecondary = colorResource(R.color.stone_300),
        textBrand = colorResource(R.color.zen_300),
        textBrandStrong = colorResource(R.color.zen_300),
        actionPrimary = colorResource(R.color.zen_300),
        actionPrimaryText = colorResource(R.color.ink_base),
        accentReward = colorResource(R.color.amber_500),
        accentRewardGraphic = colorResource(R.color.amber_500),
        accentScore = colorResource(R.color.ember_500),
        washHappy = listOf(
            colorResource(R.color.wash_happy_edge_dark),
            colorResource(R.color.wash_happy_core_dark),
            colorResource(R.color.wash_happy_edge_dark)
        ),
        washNeutral = listOf(
            colorResource(R.color.wash_neutral_edge_dark),
            colorResource(R.color.wash_neutral_core_dark),
            colorResource(R.color.wash_neutral_edge_dark)
        ),
        washAnnoyed = listOf(
            colorResource(R.color.wash_annoyed_edge_dark),
            colorResource(R.color.wash_annoyed_core_dark),
            colorResource(R.color.wash_annoyed_edge_dark)
        ),
        cardHappy = listOf(
            colorResource(R.color.card_happy_face),
            colorResource(R.color.card_happy_body)
        ),
        cardNeutral = listOf(
            colorResource(R.color.card_neutral_face),
            colorResource(R.color.card_neutral_body)
        ),
        cardAnnoyed = listOf(
            colorResource(R.color.card_annoyed_face),
            colorResource(R.color.card_annoyed_body)
        ),
        moodHappy = colorResource(R.color.zen_300),
        moodNeutral = colorResource(R.color.amber_500),
        moodAnnoyed = colorResource(R.color.ember_300),
        innerShadow = Color(0x40000000),
        bgMoodHappy = colorResource(R.color.ink_gain),
        bgMoodNeutral = colorResource(R.color.ink_reward),
        bgMoodAnnoyed = colorResource(R.color.ink_deduct),
        statsCardFillHappy = colorResource(R.color.ink_gain),
        statsCardFillNeutral = colorResource(R.color.ink_reward),
        statsCardFillAnnoyed = colorResource(R.color.ink_deduct),
        strokeHappy = colorResource(R.color.zen_300),
        strokeNeutral = colorResource(R.color.amber_500),
        strokeAnnoyed = colorResource(R.color.ember_300),
        notificationBadgeStroke = colorResource(R.color.paper_ink),
        surfaceSunk = colorResource(R.color.ink_raised),
        surfaceTint = colorResource(R.color.ink_gain),
        surfaceTintLine = colorResource(R.color.ink_line),
        textOnTint = colorResource(R.color.zen_300),
        textMuted = colorResource(R.color.stone_250),
        textTertiary = colorResource(R.color.stone_400),
        textOnBrand = colorResource(R.color.ink_base),
        borderOutline = colorResource(R.color.ink_line),
        borderHairlineSoft = colorResource(R.color.ink_line),
        rewardSurface = colorResource(R.color.ink_reward),
        rewardSurfaceLine = colorResource(R.color.ink_line),
        accentDeduct = colorResource(R.color.ember_300),
        textOnDeduct = colorResource(R.color.ink_base),
        switchThumb = colorResource(R.color.paper_ink)
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
        textBrandStrong = Color.Unspecified,
        actionPrimary = Color.Unspecified,
        actionPrimaryText = Color.Unspecified,
        accentReward = Color.Unspecified,
        accentRewardGraphic = Color.Unspecified,
        accentScore = Color.Unspecified,
        washHappy = emptyList(),
        washNeutral = emptyList(),
        washAnnoyed = emptyList(),
        cardHappy = emptyList(),
        cardNeutral = emptyList(),
        cardAnnoyed = emptyList(),
        moodHappy = Color.Unspecified,
        moodNeutral = Color.Unspecified,
        moodAnnoyed = Color.Unspecified,
        innerShadow = Color.Unspecified,
        bgMoodHappy = Color.Unspecified,
        bgMoodNeutral = Color.Unspecified,
        bgMoodAnnoyed = Color.Unspecified,
        statsCardFillHappy = Color.Unspecified,
        statsCardFillNeutral = Color.Unspecified,
        statsCardFillAnnoyed = Color.Unspecified,
        strokeHappy = Color.Unspecified,
        strokeNeutral = Color.Unspecified,
        strokeAnnoyed = Color.Unspecified,
        notificationBadgeStroke = Color.Unspecified,
        surfaceSunk = Color.Unspecified,
        surfaceTint = Color.Unspecified,
        surfaceTintLine = Color.Unspecified,
        textOnTint = Color.Unspecified,
        textMuted = Color.Unspecified,
        textTertiary = Color.Unspecified,
        textOnBrand = Color.Unspecified,
        borderOutline = Color.Unspecified,
        borderHairlineSoft = Color.Unspecified,
        rewardSurface = Color.Unspecified,
        rewardSurfaceLine = Color.Unspecified,
        accentDeduct = Color.Unspecified,
        textOnDeduct = Color.Unspecified,
        switchThumb = Color.Unspecified
    )
}
