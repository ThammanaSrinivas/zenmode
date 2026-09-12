package com.zenlauncher.zenmode.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R

// ── Families ──────────────────────────────────────────────────────
// ZenMode OS v3 type system. Three layers, each with one job:
//   ClashDisplay  — brand voice. Medium 500 only, never a paragraph, never below 20sp.
//   Geist         — everything readable. 400 body, 600 emphasis.
//   DepartureMono — every number. Tabular figures, prevents layout shift.

val ClashDisplay = FontFamily(
    Font(R.font.clash_display_extralight, FontWeight.ExtraLight),
    Font(R.font.clash_display_light, FontWeight.Light),
    Font(R.font.clash_display_regular, FontWeight.Normal),
    Font(R.font.clash_display_medium, FontWeight.Medium),
    Font(R.font.clash_display_semibold, FontWeight.SemiBold),
    Font(R.font.clash_display_bold, FontWeight.Bold)
)

// Geist ships as a single variable file; each weight is an instance of it.
@OptIn(ExperimentalTextApi::class)
private fun geist(
    weight: FontWeight,
    axis: Int,
    style: FontStyle = FontStyle.Normal,
    resId: Int = R.font.geist_variable
) = Font(
    resId,
    weight,
    style,
    variationSettings = FontVariation.Settings(FontVariation.weight(axis))
)

val Geist = FontFamily(
    geist(FontWeight.Light, 300),
    geist(FontWeight.Normal, 400),
    geist(FontWeight.Medium, 500),
    geist(FontWeight.SemiBold, 600),
    geist(FontWeight.Bold, 700),
    geist(FontWeight.ExtraBold, 800),
    geist(FontWeight.Black, 900),
    geist(FontWeight.Normal, 400, FontStyle.Italic, R.font.geist_italic_variable)
)

val DepartureMono = FontFamily(
    Font(R.font.departure_mono_regular, FontWeight.Normal)
)

val Silkscreen = FontFamily(
    Font(R.font.silkscreen_regular, FontWeight.Normal)
)

// ── Scale ─────────────────────────────────────────────────────────
// Sizes, line heights and tracking come from the v3 token set
// (zenmode.figmatokens.json → `type`). Tracking is em-relative there,
// converted here against each style's own size.

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.08.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.14.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.24).sp
    ),
    labelSmall = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    ),
    displaySmall = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.48).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.24).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    )
)

object ZenTypography {
    // metric-xl — Zen Score, session timer
    val numericLarge = TextStyle(
        fontFamily = DepartureMono,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.48).sp
    )

    // metric — deltas, minutes, counts
    val numericMedium = TextStyle(
        fontFamily = DepartureMono,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.4.sp
    )

    // mono-label — section eyebrows only, uppercase
    val monoLabel = TextStyle(
        fontFamily = DepartureMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.1.sp
    )
}
