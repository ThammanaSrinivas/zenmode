package com.zenlauncher.zenmode.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R

val CabinetGrotesque = FontFamily(
    Font(R.font.cabinet_grotesk_thin, FontWeight.Thin),
    Font(R.font.cabinet_grotesk_extralight, FontWeight.ExtraLight),
    Font(R.font.cabinet_grotesk_light, FontWeight.Light),
    Font(R.font.cabinet_grotesk_regular, FontWeight.Normal),
    Font(R.font.cabinet_grotesk_medium, FontWeight.Medium),
    Font(R.font.cabinet_grotesk_bold, FontWeight.Bold),
    Font(R.font.cabinet_grotesk_extrabold, FontWeight.ExtraBold),
    Font(R.font.cabinet_grotesk_black, FontWeight.Black)
)

val RedditMono = FontFamily(
    Font(R.font.reddit_mono_200, FontWeight.ExtraLight),
    Font(R.font.reddit_mono_300, FontWeight.Light),
    Font(R.font.reddit_mono_400, FontWeight.Normal),
    Font(R.font.reddit_mono_500, FontWeight.Medium),
    Font(R.font.reddit_mono_600, FontWeight.SemiBold),
    Font(R.font.reddit_mono_700, FontWeight.Bold),
    Font(R.font.reddit_mono_800, FontWeight.ExtraBold),
    Font(R.font.reddit_mono_900, FontWeight.Black)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = CabinetGrotesque,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = CabinetGrotesque,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    titleLarge = TextStyle(
        fontFamily = CabinetGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = CabinetGrotesque,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

object ZenTypography {
    val numericLarge = TextStyle(
        fontFamily = RedditMono,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    )
    val numericMedium = TextStyle(
        fontFamily = RedditMono,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp
    )
}
