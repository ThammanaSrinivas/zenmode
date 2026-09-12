package com.zenlauncher.zenmode.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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
    val notificationBadgeStroke: Color
)

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
        notificationBadgeStroke = colorResource(R.color.ink_surface)
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
        notificationBadgeStroke = colorResource(R.color.paper_ink)
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
        notificationBadgeStroke = Color.Unspecified
    )
}
