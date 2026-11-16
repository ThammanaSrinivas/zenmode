package com.zenlauncher.zenmode.ui.theme

import androidx.compose.ui.graphics.Color
import com.zenlauncher.zenmode.coreapi.week.MomentAccent

// SOURCE OF TRUTH: design tokens (Zen Island). The island is black in light and dark alike, so it
// meets the camera without a seam, and everything on it is drawn in the night stage's tokens
// (WidgetPalette.night), so the island, the Quiet widget and the night widgets agree.

object IslandPalette {

    /** The island itself: albeit's island black, a hair off pure black so it never bands. */
    val island = Color(0xFF050605)
    /** The open card's hairline, which keeps its edge on a dark app. */
    val edge = Color(0x17FFFFFF)
    /** Secondary pill buttons and tracks on the island. */
    val raised = Color(WidgetPalette.night.surface2)
    val track = Color(WidgetPalette.night.outline)

    val text = Color(WidgetPalette.night.text)
    val text2 = Color(WidgetPalette.night.text2)
    val muted = Color(WidgetPalette.night.muted)
    val mono = Color(WidgetPalette.night.mono)

    val zen = Color(WidgetPalette.night.brand)
    val zenFill = Color(WidgetPalette.night.brandFill)
    val zenOn = Color(WidgetPalette.night.brandOn)
    val amber = Color(WidgetPalette.night.amber)
    val amberText = Color(WidgetPalette.night.amberText)
    val ember = Color(WidgetPalette.night.emberText)
    val dusk = Color(WidgetPalette.glowDuskDark)
    val moon = Color(WidgetPalette.moon)

    /** The zen hue as an ARGB int, for code that draws outside Compose. */
    val zenInt: Int = WidgetPalette.night.brand

    /** The moment's hue: what glows behind Zeno and colours the compact number. */
    fun hue(accent: MomentAccent): Color = when (accent) {
        MomentAccent.ZEN -> zen
        MomentAccent.AMBER -> amber
        MomentAccent.EMBER -> ember
        MomentAccent.DUSK -> dusk
    }

    /** The same, for words: amber is softened so it reads as text on black. */
    fun accentText(accent: MomentAccent): Color = when (accent) {
        MomentAccent.AMBER -> amberText
        else -> hue(accent)
    }
}
