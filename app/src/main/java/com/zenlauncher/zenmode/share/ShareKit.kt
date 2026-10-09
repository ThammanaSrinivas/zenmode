package com.zenlauncher.zenmode.share

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.ColorRes
import androidx.compose.animation.core.Easing
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.Sfx
import com.zenlauncher.zenmode.ui.components.ZenMotion
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// ── Share kit ─────────────────────────────────────────────────────
// Every share card is drawn with plain android.graphics calls onto a fixed, social-sized canvas,
// so one drawing serves the in-app preview, the exported PNG and the exported clip, the same on
// every phone. Cards change with what the user actually reached (a 30-day streak is a moon, a
// 365-day one an orbit), so each card is a [ShareArt]: a picture as a function of time.
//
// This file is the vocabulary they share: the formats, fonts and palette lookup, text, the
// ZenMode OS wordmark, and the few textures (glow, grain, stars) every card is built from.
// Colours always come from colors.xml through [ShareKit.color] — never a literal here.

/** The two shapes a card is exported in. */
enum class ShareFormat(val width: Int, val height: Int) {
    /** 4:5, the post shape every feed shows whole; 1080×1350 is Instagram's own post size. */
    POST(1080, 1350),

    /** 9:16, for stories, status and reels. */
    STORY(1080, 1920)
}

/**
 * A sound timed to a card's arrival: [sfx] at [atMs] into the intro, replayed at [rate] (see
 * [Pentatonic]), with [haptic] under it.
 */
data class ShareCue(val atMs: Int, val sfx: Sfx, val rate: Float = 1f, val haptic: CueHaptic = CueHaptic.NONE)

/** How a cue feels: nothing, a light tick (a bar landing), or a firm landing (the milestone itself). */
enum class CueHaptic { NONE, TICK, LAND }

/**
 * A share card. [draw] paints the whole card for [timeMs] since it started arriving: from
 * [introMs] on it is settled — the frame exported as an image — and only ambient motion
 * (a flicker, a twinkle) carries on, which [ambient] says whether to keep drawing.
 */
interface ShareArt {
    val format: ShareFormat
    val introMs: Int
    val cues: List<ShareCue>

    /** What the card says, for TalkBack. */
    val description: String
    val ambient: Boolean get() = true
    fun draw(canvas: Canvas, timeMs: Float)
}

/** Fonts, palette and the brand mark, loaded once per sheet. Always light resources: a card
 * leaves the app as an image, so it never follows the phone's dark mode. */
class ShareKit(context: Context) {
    private val res: Context = context.lightOnly()

    val clash: Typeface = font(R.font.clash_display_medium, Typeface.DEFAULT_BOLD)
    val geist: Typeface = font(R.font.geist_variable, Typeface.DEFAULT)
    val mono: Typeface = font(R.font.departure_mono_regular, Typeface.MONOSPACE)
    private val mark: Bitmap? = runCatching {
        BitmapFactory.decodeResource(res.resources, R.drawable.ic_zen_mark_gradient)
    }.getOrNull()
    private val grain: Bitmap = grainTile()

    fun color(@ColorRes id: Int): Int = ContextCompat.getColor(res, id)

    // ── Text ──────────────────────────────────────────────────────

    fun paint(
        face: Typeface,
        size: Float,
        color: Int,
        tracking: Float = 0f,
        align: Paint.Align = Paint.Align.LEFT,
        weight: Int? = null
    ): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = face
        textSize = size
        this.color = color
        letterSpacing = tracking
        textAlign = align
        // Geist ships as one variable file; its weight is an axis, not a separate face.
        if (weight != null) fontVariationSettings = "'wght' $weight"
    }

    /** Wrapped text, at most [maxLines] lines, ellipsised past that. */
    fun layout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        lineSpacing: Float = 1f,
        maxLines: Int = 3,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(align)
        // Balanced, so a caption never ends on one stranded word. (A compile-time constant, so
        // the API 29 LineBreaker class itself is never touched on older phones.)
        .setBreakStrategy(LineBreaker.BREAK_STRATEGY_BALANCED)
        .setLineSpacing(0f, lineSpacing)
        .setIncludePad(false)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()

    // ── Brand ─────────────────────────────────────────────────────

    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val wordPaint = paint(clash, 38f, android.graphics.Color.WHITE, tracking = -0.01f)
    private val osColors = intArrayOf(
        color(R.color.score_grad_start),
        color(R.color.score_grad_mid),
        color(R.color.score_orange)
    )

    /**
     * Mark + "ZenMode " + gradient "OS" with its baseline at [baseline], starting at [x]. The
     * "OS" runs the same -67.9° brand ramp as [com.zenlauncher.zenmode.ui.components.ZenModeOsWordmark].
     * Returns the drawn width.
     */
    fun drawWordmark(canvas: Canvas, x: Float, baseline: Float, size: Float, ink: Int, alpha: Float = 1f): Float {
        val a = (alpha.coerceIn(0f, 1f) * 255).roundToInt()
        var cursor = x
        mark?.let {
            val side = size * 1.12f
            val top = baseline - size * 0.84f
            markPaint.alpha = a
            canvas.drawBitmap(it, null, RectF(cursor, top, cursor + side, top + side), markPaint)
            cursor += side + size * 0.34f
        }
        val word = wordPaint.apply {
            shader = null
            textSize = size
            color = ColorUtils.setAlphaComponent(ink, a)
        }
        canvas.drawText("ZenMode ", cursor, baseline, word)
        cursor += word.measureText("ZenMode ")
        val osWidth = word.measureText("OS")
        word.shader = cssGradient(RectF(cursor, baseline - size * 0.75f, cursor + osWidth, baseline), -67.92f, osColors, OsStops)
        canvas.drawText("OS", cursor, baseline, word)
        word.shader = null
        return cursor + osWidth - x
    }

    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** The bare mark in [box], in its own colours or flattened to [tint] (an emboss on a coin). */
    fun drawMark(canvas: Canvas, box: RectF, tint: Int? = null, alpha: Float = 1f) {
        val bitmap = mark ?: return
        tintPaint.colorFilter = tint?.let { PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN) }
        tintPaint.alpha = (alpha.coerceIn(0f, 1f) * 255).roundToInt()
        canvas.drawBitmap(bitmap, null, box, tintPaint)
    }

    // ── Textures ──────────────────────────────────────────────────

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)

    /** A soft round light: [color] at [alpha] in the middle, nothing at [radius]. */
    fun glow(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, alpha: Float) {
        if (radius <= 0f || alpha <= 0f) return
        fill.shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(withAlpha(color, alpha), withAlpha(color, alpha * 0.45f), withAlpha(color, 0f)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, fill)
        fill.shader = null
    }

    /** Top-to-bottom wash across the whole card. */
    fun wash(canvas: Canvas, format: ShareFormat, vararg stops: Pair<Float, Int>) {
        fill.shader = LinearGradient(
            0f, 0f, 0f, format.height.toFloat(),
            stops.map { it.second }.toIntArray(),
            stops.map { it.first }.toFloatArray(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, format.width.toFloat(), format.height.toFloat(), fill)
        fill.shader = null
    }

    private val grainPaint = Paint().apply {
        shader = BitmapShader(grain, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    /** Fine print grain over the whole card, so flat colour reads as paper or film, not a screen. */
    fun grain(canvas: Canvas, format: ShareFormat, alpha: Float) {
        grainPaint.alpha = (alpha.coerceIn(0f, 1f) * 255).roundToInt()
        canvas.drawRect(0f, 0f, format.width.toFloat(), format.height.toFloat(), grainPaint)
    }

    private fun grainTile(): Bitmap {
        val side = 192
        val rnd = Random(1337)
        val pixels = IntArray(side * side) {
            // Mostly nothing; a sprinkle of light and dark specks at low, varied strength.
            val roll = rnd.nextFloat()
            when {
                roll < 0.18f -> withAlpha(android.graphics.Color.WHITE, 0.05f + rnd.nextFloat() * 0.35f)
                roll < 0.36f -> withAlpha(android.graphics.Color.BLACK, 0.05f + rnd.nextFloat() * 0.35f)
                else -> 0
            }
        }
        return Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888)
    }

    private fun font(id: Int, fallback: Typeface): Typeface =
        runCatching { ResourcesCompat.getFont(res, id) }.getOrNull() ?: fallback

    private companion object {
        val OsStops = floatArrayOf(0.23558f, 0.50636f, 0.71412f)
    }
}

/** A night sky: seeded so the same card always gets the same stars. */
class StarField(seed: Int, count: Int, private val area: RectF, private val minR: Float = 1.4f, private val maxR: Float = 4.2f) {
    private val rnd = Random(seed)
    private val x = FloatArray(count) { area.left + rnd.nextFloat() * area.width() }
    private val y = FloatArray(count) { area.top + rnd.nextFloat() * rnd.nextFloat() * area.height() }
    private val r = FloatArray(count) { minR + rnd.nextFloat() * rnd.nextFloat() * (maxR - minR) }
    private val phase = FloatArray(count) { rnd.nextFloat() * 6.283f }
    private val base = FloatArray(count) { 0.25f + rnd.nextFloat() * 0.7f }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Stars appear in a quick scatter over [appear] (0→1); after that they twinkle on [timeMs]. */
    fun draw(canvas: Canvas, color: Int, appear: Float, timeMs: Float, twinkle: Boolean) {
        for (i in x.indices) {
            val shown = ((appear * 1.4f - i / x.size.toFloat() * 0.4f)).coerceIn(0f, 1f)
            if (shown <= 0f) continue
            val tw = if (twinkle) 0.55f + 0.45f * sin(timeMs / 640f + phase[i]) else 1f
            paint.color = withAlpha(color, base[i] * tw * shown)
            canvas.drawCircle(x[i], y[i], r[i], paint)
        }
    }
}

// ── Motion ────────────────────────────────────────────────────────

/** Timing for card arrivals, on the app's own curves ([ZenMotion]): arrivals decelerate. */
internal object ShareMotion {
    /** 0→1 for a beat starting [start] ms in and lasting [duration] ms, through [easing]. */
    fun beat(t: Float, start: Int, duration: Int, easing: Easing = ZenMotion.EaseOut): Float {
        if (duration <= 0) return if (t >= start) 1f else 0f
        return easing.transform(((t - start) / duration).coerceIn(0f, 1f))
    }

    /** A landing that overshoots and settles — for things that "pop" into place. */
    fun pop(t: Float, start: Int, duration: Int, overshoot: Float = 1.6f): Float {
        val p = ((t - start) / duration).coerceIn(0f, 1f)
        val q = p - 1f
        return q * q * ((overshoot + 1f) * q + overshoot) + 1f
    }
}

/**
 * Playback rates that keep a replayed sound in C-major pentatonic, the one key every sound in
 * the app is written in (see scripts/generate_sfx.py). Seven notes rising: one per weekday.
 */
internal object Pentatonic {
    const val G4 = 0.75f
    const val A4 = 0.8333f
    const val C5 = 1f
    const val D5 = 1.125f
    const val E5 = 1.25f
    const val G5 = 1.5f
    const val A5 = 1.6667f
    val rising = floatArrayOf(G4, A4, C5, D5, E5, G5, A5)
}

// ── Small helpers ─────────────────────────────────────────────────

internal fun withAlpha(color: Int, alpha: Float): Int =
    ColorUtils.setAlphaComponent(color, (alpha.coerceIn(0f, 1f) * android.graphics.Color.alpha(color)).roundToInt())

internal fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f

internal fun blend(a: Int, b: Int, f: Float): Int = ColorUtils.blendARGB(a, b, f.coerceIn(0f, 1f))

/** A CSS-style angled linear gradient across [box] — the same maths as Compose's cssLinearGradient. */
internal fun cssGradient(box: RectF, angleDegrees: Float, colors: IntArray, stops: FloatArray): LinearGradient {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val dx = sin(radians).toFloat()
    val dy = -cos(radians).toFloat()
    val half = (abs(box.width() * dx) + abs(box.height() * dy)) / 2f
    return LinearGradient(
        box.centerX() - dx * half, box.centerY() - dy * half,
        box.centerX() + dx * half, box.centerY() + dy * half,
        colors, stops, Shader.TileMode.CLAMP
    )
}

/** Same context, light resources: values-night never reaches a card. */
private fun Context.lightOnly(): Context {
    val config = Configuration(resources.configuration).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
    }
    return createConfigurationContext(config)
}
