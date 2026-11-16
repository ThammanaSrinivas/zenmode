package com.zenlauncher.zenmode.widget.art

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.share.withAlpha
import com.zenlauncher.zenmode.ui.theme.WidgetPalette
import com.zenlauncher.zenmode.ui.theme.WidgetTokens
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * The surface a widget sits on. Every widget asks [com.zenlauncher.zenmode.widget.WidgetData.surfaceFor],
 * so a won week turns them all gold together and a Quiet session turns them night together.
 */
enum class Surface {
    /** The default by day. */
    PAPER,
    /** The phone is in dark theme. */
    INK,
    /** The night stage: a Quiet session, a perfect week, the bloom, and the moment widgets after 21:30. */
    NIGHT,
    /** The week is won, by day. */
    GOLD;

    val tokens: WidgetTokens
        get() = when (this) {
            PAPER -> WidgetPalette.paper
            INK -> WidgetPalette.ink
            NIGHT -> WidgetPalette.night
            GOLD -> WidgetPalette.gold
        }

    /** Stars show on dark skies only. */
    val showsStars: Boolean get() = this == INK || this == NIGHT
    val isDark: Boolean get() = this == INK || this == NIGHT
}

/** The one choice a widget offers, behind the system's reconfigure button. */
enum class Backdrop {
    /** The default: albeit's light, which follows the day. */
    LIGHT,
    /** The surface at about 60%, so the wallpaper comes through. */
    GLASS,
    /** The surface alone, paper and ink everywhere. */
    PLAIN;

    companion object {
        fun of(name: String?): Backdrop = entries.firstOrNull { it.name == name } ?: LIGHT
    }
}

/**
 * Lays down a widget's ground in dp (the canvas is already scaled): the surface, then the
 * [WidgetLight] on it, a fine paper grain, and a glassy edge. Smooth light only: no shapes, no
 * pictures, nothing that reads as pixels.
 */
internal object SurfacePainter {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun ground(canvas: Canvas, w: Float, h: Float, radius: Float, surface: Surface, backdrop: Backdrop, light: WidgetLight.Spec?, pxPerDp: Float) {
        val shape = Path().apply { addRoundRect(RectF(0f, 0f, w, h), radius, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(shape)
        when (backdrop) {
            Backdrop.GLASS -> solid(canvas, w, h, if (surface.isDark) WidgetPalette.glassDark else WidgetPalette.glassLight)
            Backdrop.PLAIN -> sky(canvas, w, h, surface)
            Backdrop.LIGHT -> {
                sky(canvas, w, h, surface)
                if (light != null) light(canvas, light, surface)
                grain(canvas, w, h, surface, pxPerDp)
            }
        }
        canvas.restore()
        rim(canvas, w, h, radius, surface, backdrop)
    }

    private fun solid(canvas: Canvas, w: Float, h: Float, color: Int) {
        fill.shader = null
        fill.color = color
        canvas.drawRect(0f, 0f, w, h, fill)
    }

    private fun shade(shader: Shader) {
        fill.color = Color.BLACK
        fill.shader = shader
    }

    /** The surface itself. Paper is flat paper; the light does the rest. */
    private fun sky(canvas: Canvas, w: Float, h: Float, surface: Surface) {
        when (surface) {
            Surface.PAPER -> solid(canvas, w, h, WidgetPalette.paper.surface)
            Surface.INK -> {
                shade(LinearGradient(0f, 0f, w * 0.4f, h, WidgetPalette.inkSky[0], WidgetPalette.inkSky[1], Shader.TileMode.CLAMP))
                canvas.drawRect(0f, 0f, w, h, fill)
            }
            Surface.NIGHT -> {
                shade(LinearGradient(0f, 0f, 0f, h, WidgetPalette.nightSky, WidgetPalette.nightSkyStops, Shader.TileMode.CLAMP))
                canvas.drawRect(0f, 0f, w, h, fill)
            }
            Surface.GOLD -> {
                shade(LinearGradient(0f, 0f, w, h, WidgetPalette.goldSky, WidgetPalette.goldSkyStops, Shader.TileMode.CLAMP))
                canvas.drawRect(0f, 0f, w, h, fill)
                // The key light, upper left: gold reads as lit, not painted.
                radial(canvas, w * 0.22f, h * 0.05f, max(w, h) * 0.85f, 1f, 1f, WidgetPalette.goldKey, 0.55f)
            }
        }
        fill.shader = null
    }

    private fun light(canvas: Canvas, s: WidgetLight.Spec, surface: Surface) {
        val r = max(s.w, s.h)
        daylight(canvas, s, surface, r)
        wash(canvas, s, surface)
        s.glow?.let { glow(canvas, it, surface) }
        if (surface.showsStars) stars(canvas, s)
        s.moon?.let { moon(canvas, it) }
        fill.shader = null
    }

    /** The light of the time of day (iPhone's MomentGround), in the surface's own tints. */
    private fun daylight(canvas: Canvas, s: WidgetLight.Spec, surface: Surface, r: Float) {
        val t = surface.tokens
        when {
            surface == Surface.GOLD -> Unit
            surface == Surface.NIGHT -> radial(canvas, s.w * 0.5f, 0f, r * 0.95f, 1f, 1f, WidgetPalette.night.brandTint, 0.9f)
            s.part == DayPart.MORNING -> {
                // Dawn: a clean green light from the top, a warm line low.
                radial(canvas, s.w * 0.5f, 0f, r * 0.9f, 1f, 1f, t.brandTint, 1f)
                linear(canvas, s.w, s.h, 0.5f, t.amberTint, 0f, 0.55f)
            }
            s.part == DayPart.AFTERNOON -> radial(canvas, 0f, 0f, r * 1.1f, 1f, 1f, t.brandTint, 0.9f)
            s.part == DayPart.EVENING -> {
                // How the day went: the light goes low and warm.
                linear(canvas, s.w, s.h, 0f, t.sunk, 0.5f, 0f)
                linear(canvas, s.w, s.h, 0f, t.amberTint, 0f, 0.85f)
                radial(canvas, s.w, s.h, r * 0.8f, 1f, 1f, t.emberTint, 1f)
            }
            else -> radial(canvas, s.w * 0.5f, 0f, r * 0.95f, 1f, 1f, t.brandTint, 0.8f)
        }
    }

    /** Today, rising from the bottom: albeit's happy, neutral or annoyed wash (130% × 90% at 50% 100%). */
    private fun wash(canvas: Canvas, s: WidgetLight.Spec, surface: Surface) {
        if (s.mood == WidgetLight.Mood.NONE || surface == Surface.GOLD) return
        val rx = s.w * 1.3f
        val ry = s.h * 0.9f
        canvas.save()
        canvas.scale(1f, ry / rx, s.w / 2, s.h)
        if (surface.isDark) {
            val (color, a) = when (s.mood) {
                WidgetLight.Mood.CALM -> WidgetPalette.washCalmDark to 0.16f
                WidgetLight.Mood.DRIFT -> WidgetPalette.washDriftDark to 0.10f
                else -> WidgetPalette.washOverDark to 0.17f
            }
            shade(RadialGradient(s.w / 2, s.h, rx, intArrayOf(withAlpha(color, a), withAlpha(color, a * 0.45f), withAlpha(color, 0f)), floatArrayOf(0f, 0.38f, 0.72f), Shader.TileMode.CLAMP))
        } else {
            val (core, mid) = when (s.mood) {
                WidgetLight.Mood.CALM -> WidgetPalette.washCalm to WidgetPalette.washCalmMid
                WidgetLight.Mood.DRIFT -> WidgetPalette.washDrift to WidgetPalette.washDrift
                else -> WidgetPalette.washOver to WidgetPalette.washOver
            }
            shade(RadialGradient(s.w / 2, s.h, rx, intArrayOf(withAlpha(core, 0.9f), withAlpha(mid, 0.5f), withAlpha(mid, 0f)), floatArrayOf(0f, 0.38f, 0.72f), Shader.TileMode.CLAMP))
        }
        canvas.drawCircle(s.w / 2, s.h, rx, fill)
        canvas.restore()
    }

    /** The moment's hue behind the hero, at iPhone's strengths. */
    private fun glow(canvas: Canvas, g: WidgetLight.Glow, surface: Surface) {
        val dark = surface.isDark
        val (color, strength) = when (g.hue) {
            WidgetLight.Hue.AMBER -> WidgetPalette.glowAmber to when {
                dark -> if (g.celebration) 0.3f else 0.16f
                surface == Surface.GOLD -> if (g.celebration) 0.7f else 0.5f
                else -> if (g.celebration) 0.55f else 0.32f
            }
            WidgetLight.Hue.EMBER -> WidgetPalette.glowEmber to if (dark) 0.2f else 0.18f
            WidgetLight.Hue.ZEN -> WidgetPalette.glowZen to if (dark) 0.14f else 0.22f
            WidgetLight.Hue.DUSK -> (if (dark) WidgetPalette.glowDuskDark else WidgetPalette.glowDusk) to if (dark) 0.14f else 0.45f
        }
        radial(canvas, g.x, g.y, if (dark && g.celebration) g.radius * 0.75f else g.radius, 1f, 1f, color, strength)
    }

    /** Soft points of light, each with its own faint halo. */
    private fun stars(canvas: Canvas, s: WidgetLight.Spec) {
        val color = if (s.amberStars) WidgetPalette.streakStar else WidgetPalette.starPale
        for (star in WidgetLight.stars(s)) {
            val a = if (star.bright) 0.95f else 0.55f
            radial(canvas, star.x, star.y, star.r * 4.5f, 1f, 1f, color, a * 0.28f)
            fill.shader = null
            fill.color = withAlpha(color, a)
            canvas.drawCircle(star.x, star.y, star.r, fill)
        }
    }

    /** A crescent with a pale halo: a Quiet session is on. */
    private fun moon(canvas: Canvas, m: WidgetLight.Moon) {
        radial(canvas, m.x, m.y, m.r * 3.4f, 1f, 1f, WidgetPalette.moon, 0.16f)
        val layer = canvas.saveLayer(m.x - m.r * 2, m.y - m.r * 2, m.x + m.r * 2, m.y + m.r * 2, null)
        shade(RadialGradient(m.x - m.r * 0.35f, m.y - m.r * 0.4f, m.r * 1.4f, WidgetPalette.moon, WidgetPalette.moonShade, Shader.TileMode.CLAMP))
        canvas.drawCircle(m.x, m.y, m.r, fill)
        fill.shader = null
        fill.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        val a = Math.toRadians(-38.0)
        canvas.drawCircle(m.x + m.r * 0.62f * cos(a).toFloat(), m.y + m.r * 0.62f * sin(a).toFloat(), m.r * 0.86f, fill)
        fill.xfermode = null
        canvas.restoreToCount(layer)
    }

    /** A fine paper grain, one speck per device pixel, so the light never bands. */
    private fun grain(canvas: Canvas, w: Float, h: Float, surface: Surface, pxPerDp: Float) {
        val tile = grainTile
        shade(BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
            setLocalMatrix(Matrix().apply { setScale(1f / pxPerDp, 1f / pxPerDp) })
        })
        fill.alpha = if (surface.isDark) 150 else 255
        canvas.drawRect(0f, 0f, w, h, fill)
        fill.alpha = 255
        fill.shader = null
    }

    /** The glassy edge: a hairline, and a highlight along the top that fades as it goes down. */
    private fun rim(canvas: Canvas, w: Float, h: Float, radius: Float, surface: Surface, backdrop: Backdrop) {
        val rect = RectF(0.5f, 0.5f, w - 0.5f, h - 0.5f)
        edge.shader = null
        edge.strokeWidth = 1f
        edge.color = when {
            backdrop == Backdrop.GLASS -> if (surface.isDark) WidgetPalette.glassDarkEdge else WidgetPalette.glassLightEdge
            surface == Surface.PAPER -> withAlpha(WidgetPalette.paperEdge, 0.8f)
            surface == Surface.INK -> WidgetPalette.inkEdge
            surface == Surface.NIGHT -> WidgetPalette.nightEdge
            else -> withAlpha(WidgetPalette.gold.text, 0.08f)
        }
        canvas.drawRoundRect(rect, radius - 0.5f, radius - 0.5f, edge)
        val top = if (surface.isDark) 0.12f else 0.85f
        edge.color = Color.BLACK
        edge.shader = LinearGradient(0f, 0f, 0f, minOf(h * 0.35f, 60f),
            withAlpha(WidgetPalette.highlight, top), withAlpha(WidgetPalette.highlight, 0f), Shader.TileMode.CLAMP)
        edge.strokeWidth = 1.2f
        canvas.drawRoundRect(RectF(1.1f, 1.1f, w - 1.1f, h - 1.1f), radius - 1.1f, radius - 1.1f, edge)
        edge.shader = null
    }

    private fun radial(canvas: Canvas, x: Float, y: Float, r: Float, sx: Float, sy: Float, color: Int, alpha: Float) {
        if (r <= 0f || alpha <= 0f) return
        canvas.save()
        canvas.scale(sx, sy, x, y)
        shade(RadialGradient(x, y, r, intArrayOf(withAlpha(color, alpha), withAlpha(color, alpha * 0.55f), withAlpha(color, 0f)), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP))
        canvas.drawCircle(x, y, r, fill)
        canvas.restore()
        fill.shader = null
    }

    /** A vertical wash: [from] at the top fraction [start], to [to] alpha at the bottom. */
    private fun linear(canvas: Canvas, w: Float, h: Float, start: Float, color: Int, from: Float, to: Float) {
        shade(LinearGradient(0f, h * start, 0f, h, withAlpha(color, from), withAlpha(color, to), Shader.TileMode.CLAMP))
        canvas.drawRect(0f, 0f, w, h, fill)
        fill.shader = null
    }

    /** 128 × 128 specks, dark and light, a few percent each; seeded, so every widget shares one. */
    private val grainTile: Bitmap by lazy {
        val side = 128
        val rnd = Random(0x2E7)
        val px = IntArray(side * side) {
            val v = rnd.nextFloat()
            when {
                v < 0.18f -> withAlpha(WidgetPalette.grainDark, 0.025f + rnd.nextFloat() * 0.03f)
                v < 0.30f -> withAlpha(WidgetPalette.grainLight, 0.04f + rnd.nextFloat() * 0.05f)
                else -> 0
            }
        }
        Bitmap.createBitmap(px, side, side, Bitmap.Config.ARGB_8888)
    }
}
