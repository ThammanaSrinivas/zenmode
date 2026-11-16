package com.zenlauncher.zenmode.ui.theme

// SOURCE OF TRUTH: design tokens (widgets). albeit v5, the design system's current values,
// and the widget light's washes and glows. Widgets are drawn with android.graphics into bitmaps, so these
// are ARGB ints, not Compose colours. The app's ZenColors still carry the previous values;
// the widgets carry v5 themselves until the two meet.

/** One surface's tokens: what text, cells and buttons are drawn in on it. */
class WidgetTokens(
    val text: Int,
    val text2: Int,
    val text3: Int,
    val muted: Int,
    val mono: Int,
    val hair: Int,
    val outline: Int,
    val sunk: Int,
    val surface: Int,
    val surface2: Int,
    val brand: Int,
    val brandFill: Int,
    val brandSoft: Int,
    val brandTint: Int,
    val brandTintLine: Int,
    val brandOn: Int,
    val amber: Int,
    val amberText: Int,
    val amberTint: Int,
    val emberText: Int,
    val emberTint: Int,
    val bg: Int
)

/** A Zeno skin: the head's light-to-shadow ramp, the rim, the blush and the ink of his face. */
class ZenoSkinColors(val a: Int, val b: Int, val c: Int, val rim: Int, val blush: Int, val ink: Int)

private fun c(argb: Long): Int = argb.toInt()

object WidgetPalette {

    val paper = WidgetTokens(
        text = c(0xFF12160F), text2 = c(0xFF4A4F46), text3 = c(0xFF5C6158), muted = c(0xFF5F645B),
        mono = c(0xFF666B62), hair = c(0xFFDCDAD1), outline = c(0xFFC9C7BD), sunk = c(0xFFEAE8E2),
        surface = c(0xFFFAF9F5), surface2 = c(0xFFFFFFFF), brand = c(0xFF0F7A18), brandFill = c(0xFF2AA136),
        brandSoft = c(0xFF5BDF62), brandTint = c(0xFFE6F6E7), brandTintLine = c(0xFFC8E7CA), brandOn = c(0xFFF2FFF2),
        amber = c(0xFFFFC800), amberText = c(0xFF7A5A00), amberTint = c(0xFFFFF3CC), emberText = c(0xFFB34700),
        emberTint = c(0xFFFFF3EA), bg = c(0xFFF2F1EC)
    )

    val ink = WidgetTokens(
        text = c(0xFFF2F3EE), text2 = c(0xFFC9CCC4), text3 = c(0xFFA4A9A0), muted = c(0xFF8E948B),
        mono = c(0xFF8A9087), hair = c(0xFF262B26), outline = c(0xFF343A34), sunk = c(0xFF090B09),
        surface = c(0xFF161A16), surface2 = c(0xFF1C211C), brand = c(0xFF5BDF62), brandFill = c(0xFF2AA136),
        brandSoft = c(0xFF5BDF62), brandTint = c(0xFF142516), brandTintLine = c(0xFF1F3A22), brandOn = c(0xFF0A140B),
        amber = c(0xFFFFC800), amberText = c(0xFFFFD86B), amberTint = c(0xFF2A2208), emberText = c(0xFFFF8A3D),
        emberTint = c(0xFF2A1508), bg = c(0xFF0E110E)
    )

    /** Night: Quiet sessions, a perfect week, the bloom. Ink's values on a deeper ground. */
    val night = WidgetTokens(
        text = ink.text, text2 = ink.text2, text3 = ink.text3, muted = ink.muted, mono = ink.mono,
        hair = c(0xFF1E241E), outline = c(0xFF262B26), sunk = ink.sunk, surface = c(0xFF121612),
        surface2 = c(0xFF1A1F16), brand = ink.brand, brandFill = ink.brandFill, brandSoft = ink.brandSoft,
        brandTint = ink.brandTint, brandTintLine = ink.brandTintLine, brandOn = ink.brandOn, amber = ink.amber,
        amberText = ink.amberText, amberTint = ink.amberTint, emberText = ink.emberText, emberTint = ink.emberTint,
        bg = c(0xFF070907)
    )

    /** Gold: the week is won. Ink-brown on amber; the amber itself becomes the ink. */
    val gold = WidgetTokens(
        text = c(0xFF1A1003), text2 = c(0xFF4A3500), text3 = c(0xFF5E4500), muted = c(0xFF5E4500),
        mono = c(0xFF5E4500), hair = c(0x291A1003), outline = c(0x571A1003), sunk = c(0x1F1A1003),
        surface = c(0xFFFFE59A), surface2 = c(0x66FFFFFF), brand = c(0xFF1A1003), brandFill = c(0xFF1A1003),
        brandSoft = c(0xFF1A1003), brandTint = c(0x73FFFFFF), brandTintLine = c(0x571A1003), brandOn = c(0xFFFFC800),
        amber = c(0xFF1A1003), amberText = c(0xFF1A1003), amberTint = c(0x73FFFFFF), emberText = c(0xFF1A1003),
        emberTint = c(0x73FFFFFF), bg = c(0xFFFFD75A)
    )

    // ── Surfaces: the light each widget sits in (albeit's lit surface) ──

    private val green = c(0xFF2AA136)
    private val greenSoft = c(0xFF5BDF62)
    private val emberHue = c(0xFFFF6600)
    private val amberHue = c(0xFFFFC800)
    private val white = c(0xFFFFFFFF)
    private val pale = c(0xFFF2F3EE)

    /** Ink lifts a little toward the upper left, where the key light falls. */
    val inkSky = intArrayOf(c(0xFF1C211C), c(0xFF121612))
    /** albeit's night tone: #060D08 at the top to #122219 low. */
    val nightSky = intArrayOf(c(0xFF060D08), c(0xFF0D1A11), c(0xFF122219))
    val nightSkyStops = floatArrayOf(0f, 0.55f, 1f)
    /** A won week: warm, bright gold, lit from the upper left. */
    val goldSky = intArrayOf(c(0xFFFFF4D2), c(0xFFFFE08A), c(0xFFFFCF4A))
    val goldSkyStops = floatArrayOf(0f, 0.55f, 1f)
    val paperEdge = c(0xFFDCDAD1)
    val inkEdge = c(0xFF262B26)
    val nightEdge = c(0xFF1E241E)
    val glassLight = c(0x9EFAF9F5)
    val glassLightEdge = c(0xA8FFFFFF)
    val glassDark = c(0x8F0E110E)
    val glassDarkEdge = c(0x24FFFFFF)

    /** albeit's washes (`--wash-happy`, `-neutral`, `-annoyed`): today, rising from the bottom. */
    val washCalm = c(0xFFD6E99F)
    val washCalmMid = c(0xFFE4EDC4)
    val washDrift = c(0xFFF6E9A8)
    val washOver = c(0xFFF7C9B4)
    /** The same three on a dark sky, as light rather than paint. */
    val washCalmDark = greenSoft
    val washDriftDark = amberHue
    val washOverDark = emberHue

    /** The moment's hue behind the hero (iPhone's MomentGround glow). */
    val glowZen = greenSoft
    val glowAmber = amberHue
    val glowEmber = emberHue
    val glowDusk = c(0xFFC9C7BD)
    val glowDuskDark = c(0xFF9DA298)
    /** The white key light on gold. */
    val goldKey = white

    /** Night's points of light: pale for the sky, amber for the streak. */
    val starPale = c(0xFFEAF7E6)
    val streakStar = amberHue
    val moon = c(0xFFE6F6E7)
    val moonShade = c(0xFF9FB9A3)

    /** The glassy top edge, and the grain's two specks. */
    val highlight = white
    val grainDark = c(0xFF000000)
    val grainLight = white

    // ── Pieces ──────────────────────────────────────────────────────

    /** The score bar's track: ember, amber, green. */
    val scoreTrack = intArrayOf(emberHue, amberHue, greenSoft)
    val scoreTrackStops = floatArrayOf(0f, 0.46f, 1f)
    val flame = emberHue
    val flameCore = amberHue
    val coin = intArrayOf(c(0xFFFFE59A), amberHue, c(0xFFC98A00))
    val coinStops = floatArrayOf(0f, 0.55f, 1f)
    val coinInk = c(0xFF7A5A00)
    /** A good-day cell's number on amber. */
    val onAmber = c(0xFF1A1003)
    val goldBarFrom = c(0xFFFFE59A)
    val hidden = 0

    // The bloom: 168 hours, a seed each.
    val bloomRest = c(0xFF17301C)
    val bloomIdle = c(0xFF22382A)
    val bloomFuture = c(0xFF263326)
    val bloomNow = pale
    val bloomAway = amberHue
    val bloomOnPurpose = greenSoft
    val bloomLight = green
    val bloomUnplanned = emberHue

    // ── Zeno (ZenKit's ZenoSkin palettes, one for one) ─────────────

    val zenoZen = ZenoSkinColors(c(0xFF86E486), c(0xFF34AE41), c(0xFF0F5F1A), c(0xFFEDFFEA), c(0xFFFF8F6B), c(0xFF0A140B))
    val zenoGold = ZenoSkinColors(c(0xFFFFE59A), c(0xFFFFBE2E), c(0xFFC96E00), c(0xFFFFFAE6), c(0xFFFF6F45), c(0xFF1A1003))
    val zenoEmber = ZenoSkinColors(c(0xFFFFC197), c(0xFFFF7C30), c(0xFFB43F00), c(0xFFFFF1E6), c(0xFFFF4D4D), c(0xFF1C0A02))
    val zenoDusk = ZenoSkinColors(c(0xFFD3DDCF), c(0xFF93A88F), c(0xFF4D654A), c(0xFFF7FAF5), c(0xFFE59F96), c(0xFF0F160F))
    val zenoEye = intArrayOf(c(0xFF3A4A3D), c(0xFF0C140D), c(0xFF030503))
    val zenoEyeStops = floatArrayOf(0f, 0.62f, 1f)
    val zenoTongue = c(0xFFFF8C78)
    val zenoTear = c(0xCCCDE9FF)
    val white100 = white
    val black100 = c(0xFF000000)
}

/** The stand-in wallpaper behind the backdrop picker's previews, so Glass has something to show through. */
object WidgetStyleColors {
    val wallpaper = listOf(androidx.compose.ui.graphics.Color(0xFF7FA58C), androidx.compose.ui.graphics.Color(0xFF2F4A3A))
}
