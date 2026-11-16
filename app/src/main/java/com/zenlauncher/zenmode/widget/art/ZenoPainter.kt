package com.zenlauncher.zenmode.widget.art

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.zenlauncher.zenmode.coreapi.week.MomentFeeling
import com.zenlauncher.zenmode.share.withAlpha
import com.zenlauncher.zenmode.ui.theme.WidgetPalette
import com.zenlauncher.zenmode.ui.theme.ZenoSkinColors
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/** Zeno's five skins. Colour follows the moment: green does the work, gold is earned. */
enum class ZenoSkin { ZEN, GOLD, EMBER, DUSK }

/**
 * Zeno, drawn with android.graphics for the widgets. Geometry and the eight feelings are one for
 * one with the iPhone app's Zeno, which is itself the brand book's: the mark's two corner triangles are the ears, the
 * band is the face, the cut turned 45° is the nose. A widget is a still frame, so there is no
 * breath or blink here; the feeling is the whole expression.
 *
 * Design units: the head lives in a 150 × 100 box, drawn in the widget boards' viewBox
 * (-4 -4 158 108), so [draw] takes the width that box should fill.
 */
object ZenoPainter {

    /** Height over width of the box Zeno is drawn in. */
    const val ASPECT = 108f / 158f
    private const val VIEW_X = -4f
    private const val VIEW_Y = -4f
    private const val VIEW_W = 158f

    /** The skin a feeling wears by default (ZenoFeeling.skin). */
    fun skinFor(feeling: MomentFeeling): ZenoSkin = when (feeling) {
        MomentFeeling.PROUD -> ZenoSkin.GOLD
        MomentFeeling.ALERT -> ZenoSkin.EMBER
        MomentFeeling.SAD, MomentFeeling.SLEEPY -> ZenoSkin.DUSK
        else -> ZenoSkin.ZEN
    }

    private fun colors(skin: ZenoSkin): ZenoSkinColors = when (skin) {
        ZenoSkin.ZEN -> WidgetPalette.zenoZen
        ZenoSkin.GOLD -> WidgetPalette.zenoGold
        ZenoSkin.EMBER -> WidgetPalette.zenoEmber
        ZenoSkin.DUSK -> WidgetPalette.zenoDusk
    }

    /** Every value a face takes (ZenoPose), at rest for one feeling. */
    private class Pose(
        val shut: Float = 0f, val arc: Float = 0f, val lidU: Float = 0f, val lidTilt: Float = 0f,
        val brow: Float = 0f, val browTilt: Float = 0f, val browY: Float = 0f, val eyeS: Float = 1f,
        val mouth: Float = 0.32f, val open: Float = 0f, val blush: Float = 0.25f, val droop: Float = 2f,
        val tilt: Float = 0f, val wet: Float = 0f, val earLo: Float = 0f, val earRo: Float = 0f
    )

    private fun pose(f: MomentFeeling): Pose = when (f) {
        MomentFeeling.CALM -> Pose()
        MomentFeeling.HAPPY -> Pose(shut = 1f, arc = 1f, brow = 0.55f, browTilt = -6f, browY = -3f, mouth = 1f, open = 0.62f, blush = 1f, droop = -10f)
        MomentFeeling.PLEASANT -> Pose(shut = 1f, arc = -1f, mouth = 0.78f, blush = 0.75f, droop = 9f, tilt = 3f)
        MomentFeeling.SAD -> Pose(lidU = 0.34f, lidTilt = 17f, brow = 0.95f, browTilt = 22f, browY = 1f, eyeS = 1.06f, mouth = -0.92f, blush = 0f, droop = 36f, tilt = -2f, wet = 1f)
        MomentFeeling.CURIOUS -> Pose(brow = 0.8f, browTilt = -4f, browY = -2f, eyeS = 1.1f, mouth = 0f, open = 0.26f, blush = 0.2f, droop = 4f, tilt = 4f, earLo = 17f, earRo = -4f)
        MomentFeeling.SLEEPY -> Pose(shut = 0.86f, arc = -0.3f, mouth = 0.15f, blush = 0.35f, droop = 24f, tilt = -3f)
        MomentFeeling.ALERT -> Pose(brow = 0.9f, browTilt = -3f, browY = -4.5f, eyeS = 1.18f, mouth = -0.12f, open = 0.36f, blush = 0f, droop = -16f)
        MomentFeeling.PROUD -> Pose(shut = 1f, arc = 1f, brow = 0.5f, browTilt = -6f, browY = -3f, mouth = 1f, open = 0.36f, blush = 0.9f, droop = -12f)
    }

    // ── Geometry ────────────────────────────────────────────────────

    private val head = roundPoly(
        floatArrayOf(50f, 0f, 100f, 0f, 100f, 10.7f, 150f, 60.7f, 150f, 100f, 111.3f, 100f, 100f, 88.7f, 100f, 100f, 50f, 100f, 50f, 88.7f, 38.7f, 100f, 0f, 100f, 0f, 60.7f, 50f, 10.7f),
        floatArrayOf(8f, 8f, 4f, 9f, 8f, 2.5f, 2f, 2f, 2f, 2f, 2.5f, 8f, 9f, 4f)
    )
    private val earL = roundPoly(floatArrayOf(0f, 0f, 38f, 0f, 0f, 38f), floatArrayOf(8f, 7f, 7f))
    private val earR = roundPoly(floatArrayOf(150f, 0f, 150f, 38f, 112f, 0f), floatArrayOf(8f, 7f, 7f))
    private val nose = roundPoly(floatArrayOf(75f, 65.4f, 80f, 70.4f, 75f, 75.4f, 70f, 70.4f), floatArrayOf(1.5f, 1.5f, 1.5f, 1.5f))
    private val noseShine = Path().apply { moveTo(73.5f, 68f); lineTo(75f, 66.5f); lineTo(76.5f, 68f) }
    private const val EYE_L_X = 48f
    private const val EYE_R_X = 102f
    private const val EYE_Y = 51f
    private const val EYE_R = 7.2f

    /** Rounds every corner of a polygon with a quadratic, exactly like `roundPoly` in zeno.js. */
    private fun roundPoly(xy: FloatArray, radii: FloatArray): Path {
        val n = radii.size
        val p = Path()
        for (i in 0 until n) {
            val p0x = xy[((i - 1 + n) % n) * 2]; val p0y = xy[((i - 1 + n) % n) * 2 + 1]
            val p1x = xy[i * 2]; val p1y = xy[i * 2 + 1]
            val p2x = xy[((i + 1) % n) * 2]; val p2y = xy[((i + 1) % n) * 2 + 1]
            val ax = p0x - p1x; val ay = p0y - p1y; val bx = p2x - p1x; val by = p2y - p1y
            val la = hypot(ax, ay); val lb = hypot(bx, by)
            val ux = ax / la; val uy = ay / la; val vx = bx / lb; val vy = by / lb
            val ang = acos((ux * vx + uy * vy).coerceIn(-1f, 1f))
            val t = min(min(radii[i] / tan(ang / 2), la / 2), lb / 2)
            val axp = p1x + ux * t; val ayp = p1y + uy * t
            if (i == 0) p.moveTo(axp, ayp) else p.lineTo(axp, ayp)
            p.quadTo(p1x, p1y, p1x + vx * t, p1y + vy * t)
        }
        p.close()
        return p
    }

    private fun circle(cx: Float, cy: Float, r: Float) = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }

    private fun eyeArc(cx: Float, a: Float) = Path().apply {
        moveTo(cx - 6.6f, EYE_Y + 1.6f * a)
        quadTo(cx, EYE_Y - 6.4f * a, cx + 6.6f, EYE_Y + 1.6f * a)
    }

    private fun lidUpper(cx: Float, u: Float): Path {
        val yb = EYE_Y - 8 + u * 16.4f
        val bow = 2.8f * (1 - u * 0.6f)
        return Path().apply {
            moveTo(cx - 11, EYE_Y - 18); lineTo(cx + 11, EYE_Y - 18); lineTo(cx + 11, yb)
            quadTo(cx, yb + bow, cx - 11, yb); close()
        }
    }

    private fun mouthClosed(m: Float) = Path().apply {
        if (m >= 0) {
            val e = 79 - 1.8f * m; val c = 79 + 2.8f * m; val w = 1.4f * m
            moveTo(69 - w, e); quadTo(72f, c, 75f, 78.7f + 0.2f * m); quadTo(78f, c, 81 + w, e)
        } else {
            val k = -m
            moveTo(69.6f + 0.4f * k, 79 + 0.9f * k); quadTo(75f, 79 - 5.2f * k, 80.4f - 0.4f * k, 79 + 0.9f * k)
        }
    }

    private fun mouthOpen(o: Float, m: Float): Path {
        val s = max(0f, m); val w = 5.4f + 1.8f * s; val top = 78 - 0.8f * s; val depth = 2 + 7.8f * o
        return Path().apply {
            moveTo(75 - w, top)
            quadTo(75f, top + 1.2f + 1.8f * s, 75 + w, top)
            cubicTo(75 + w * 0.95f, top + depth * 0.92f, 75 + w * 0.42f, top + depth, 75f, top + depth)
            cubicTo(75 - w * 0.42f, top + depth, 75 - w * 0.95f, top + depth * 0.92f, 75 - w, top)
            close()
        }
    }

    private fun tongue(o: Float, m: Float): Path {
        val s = max(0f, m); val top = 78 - 0.8f * s; val dep = 2 + 7.8f * o
        val rx = (5.4f + 1.8f * s) * 0.62f; val ry = max(0.1f, dep * 0.36f)
        return Path().apply { addOval(RectF(75 - rx, top + dep - 1.3f - ry, 75 + rx, top + dep - 1.3f + ry), Path.Direction.CW) }
    }

    private val brow = Path().apply { moveTo(-6.5f, 1.2f); quadTo(0f, -2f, 6.5f, 1.2f) }

    private fun smooth(a: Float, b: Float, x: Float): Float {
        val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }

    // ── Painting ────────────────────────────────────────────────────

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /**
     * Draws Zeno with the top-left of his box at ([x], [y]) and the box [width] wide (the box is
     * [ASPECT] × width high). [mono], when set, draws him flat in that one colour.
     */
    fun draw(canvas: Canvas, x: Float, y: Float, width: Float, feeling: MomentFeeling, skin: ZenoSkin = skinFor(feeling), mono: Int? = null) {
        val k = colors(skin)
        val p = pose(feeling)
        val scale = width / VIEW_W
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale, scale)
        canvas.translate(-VIEW_X, -VIEW_Y)
        // The whole body tilts about the middle of its feet.
        canvas.rotate(p.tilt, 75f, 100f)

        val headFill = LinearGradient(20f, 0f, 34f, 100f, intArrayOf(k.a, k.b, k.c), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        val earFill = LinearGradient(0f, 0f, 10f, 44f, k.a, k.b, Shader.TileMode.CLAMP)

        ear(canvas, earL, -p.droop + p.earLo, 22f, earFill, k, mono)
        ear(canvas, earR, p.droop + p.earRo, 128f, earFill, k, mono)
        part(canvas, head, headFill, k, mono, rimWidth = 2.2f, shade = true)

        if (mono == null) {
            blush(canvas, 33f, k, p.blush)
            blush(canvas, 117f, k, p.blush)
        }
        val ink = if (mono != null) inkOn(mono) else k.ink
        eye(canvas, EYE_L_X, -1f, p, ink, headFill, mono)
        eye(canvas, EYE_R_X, 1f, p, ink, headFill, mono)
        browOf(canvas, EYE_L_X, -1f, p, ink)
        browOf(canvas, EYE_R_X, 1f, p, ink)

        fill.shader = null
        fill.color = ink
        canvas.drawPath(nose, fill)
        if (mono == null) {
            stroke.shader = null
            stroke.color = withAlpha(WidgetPalette.white100, 0.4f)
            stroke.strokeWidth = 0.8f
            canvas.drawPath(noseShine, stroke)
        }

        val op = p.open.coerceIn(0f, 1f)
        val oa = (op * 5).coerceIn(0f, 1f)
        if (oa < 1f) {
            stroke.color = withAlpha(ink, 1 - oa)
            stroke.strokeWidth = 1.9f
            canvas.drawPath(mouthClosed(p.mouth.coerceIn(-1f, 1f)), stroke)
        }
        if (oa > 0f) {
            val mo = mouthOpen(op, p.mouth)
            fill.color = withAlpha(ink, oa)
            canvas.drawPath(mo, fill)
            if (mono == null) {
                canvas.save()
                canvas.clipPath(mo)
                fill.color = withAlpha(WidgetPalette.zenoTongue, oa)
                canvas.drawPath(tongue(op, p.mouth), fill)
                canvas.restore()
            }
        }
        canvas.restore()
    }

    private fun inkOn(mono: Int): Int {
        val lum = (0.299 * android.graphics.Color.red(mono) + 0.587 * android.graphics.Color.green(mono) + 0.114 * android.graphics.Color.blue(mono)) / 255
        return if (lum > 0.6) WidgetPalette.black100 else WidgetPalette.white100
    }

    private fun ear(canvas: Canvas, shape: Path, degrees: Float, pivotX: Float, earFill: Shader, k: ZenoSkinColors, mono: Int?) {
        canvas.save()
        canvas.rotate(degrees, pivotX, 24f)
        part(canvas, shape, earFill, k, mono, rimWidth = 2f, shade = false)
        canvas.restore()
    }

    /** One body part: its fill, the light from the upper left, a shade at the feet, and the rim. */
    private fun part(canvas: Canvas, shape: Path, shader: Shader, k: ZenoSkinColors, mono: Int?, rimWidth: Float, shade: Boolean) {
        if (mono != null) {
            fill.shader = null
            fill.color = mono
            canvas.drawPath(shape, fill)
            return
        }
        fill.color = android.graphics.Color.BLACK
        fill.shader = shader
        canvas.drawPath(shape, fill)
        val white = WidgetPalette.white100
        fill.color = android.graphics.Color.BLACK
        fill.shader = RadialGradient(
            60f, 16f, 70f,
            intArrayOf(withAlpha(white, 0.36f), withAlpha(white, 0.06f), withAlpha(white, 0f)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawPath(shape, fill)
        if (shade) {
            val black = WidgetPalette.black100
            fill.color = android.graphics.Color.BLACK
            fill.shader = LinearGradient(0f, 58f, 0f, 100f, withAlpha(black, 0f), withAlpha(black, 0.16f), Shader.TileMode.CLAMP)
            canvas.drawPath(shape, fill)
        }
        fill.shader = null
        canvas.save()
        canvas.clipPath(shape)
        stroke.color = android.graphics.Color.BLACK
        stroke.shader = LinearGradient(0f, 0f, 0f, 30f, withAlpha(k.rim, 0.75f), withAlpha(k.rim, 0f), Shader.TileMode.CLAMP)
        stroke.strokeWidth = rimWidth
        canvas.drawPath(shape, stroke)
        stroke.shader = null
        canvas.restore()
    }

    private fun blush(canvas: Canvas, cx: Float, k: ZenoSkinColors, amount: Float) {
        if (amount <= 0f) return
        val a = amount.coerceIn(0f, 1f)
        fill.color = android.graphics.Color.BLACK
        fill.shader = RadialGradient(cx, 66f, 9f, withAlpha(k.blush, 0.78f * a), withAlpha(k.blush, 0f), Shader.TileMode.CLAMP)
        canvas.save()
        canvas.scale(1f, 5f / 9f, cx, 66f)
        canvas.drawCircle(cx, 66f, 9f, fill)
        canvas.restore()
        fill.shader = null
    }

    private fun eye(canvas: Canvas, cx: Float, sign: Float, p: Pose, ink: Int, headFill: Shader, mono: Int?) {
        val shut = p.shut.coerceIn(0f, 1f)
        val squash = max(0.06f, 1 - shut * 1.05f)
        val ballA = 1 - smooth(0.72f, 0.92f, shut)
        val arcA = smooth(0.55f, 0.9f, shut)
        canvas.save()
        canvas.scale(p.eyeS, p.eyeS, cx, EYE_Y)
        if (ballA > 0.01f) {
            canvas.save()
            canvas.scale(1f, squash, cx, EYE_Y)
            val layer = canvas.saveLayerAlpha(cx - 12, EYE_Y - 20, cx + 12, EYE_Y + 12, (ballA * 255).toInt())
            if (mono != null) {
                fill.shader = null
                fill.color = ink
            } else {
                fill.color = android.graphics.Color.BLACK
                fill.shader = RadialGradient(
                    cx - 0.2f * EYE_R, EYE_Y - 0.28f * EYE_R, 1.44f * EYE_R,
                    WidgetPalette.zenoEye, WidgetPalette.zenoEyeStops, Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, EYE_Y, EYE_R, fill)
            fill.shader = null
            if (mono == null) {
                fill.color = WidgetPalette.white100
                canvas.drawCircle(cx - 2.3f, EYE_Y - 2.6f, 2.3f + 0.6f * p.wet, fill)
                fill.color = withAlpha(WidgetPalette.white100, 0.85f)
                canvas.drawCircle(cx + 2.6f, EYE_Y + 2.5f, 1.05f, fill)
            }
            if (p.lidU > 0f) {
                canvas.save()
                canvas.clipPath(circle(cx, EYE_Y, EYE_R + 0.45f))
                val lid = lidUpper(cx, p.lidU.coerceIn(0f, 1f))
                lid.transform(Matrix().apply { setRotate(sign * p.lidTilt, cx, EYE_Y) })
                if (mono != null) {
                    fill.color = mono
                } else {
                    fill.color = android.graphics.Color.BLACK
                    fill.shader = headFill
                }
                canvas.drawPath(lid, fill)
                fill.shader = null
                canvas.restore()
            }
            canvas.restoreToCount(layer)
            canvas.restore()
        }
        if (arcA > 0.01f) {
            stroke.shader = null
            stroke.color = withAlpha(ink, arcA)
            stroke.strokeWidth = 2.7f
            canvas.drawPath(eyeArc(cx, p.arc.coerceIn(-1f, 1f)), stroke)
        }
        canvas.restore()
    }

    private fun browOf(canvas: Canvas, x: Float, sign: Float, p: Pose, ink: Int) {
        if (p.brow <= 0.01f) return
        val y = 38.5f + p.browY - (p.eyeS - 1) * 12
        val path = Path(brow)
        path.transform(Matrix().apply { setRotate(sign * p.browTilt); postTranslate(x, y) })
        stroke.shader = null
        stroke.color = withAlpha(ink, p.brow.coerceIn(0f, 1f))
        stroke.strokeWidth = 2.4f
        canvas.drawPath(path, stroke)
    }
}
