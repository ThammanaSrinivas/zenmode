package com.zenlauncher.zenmode.share

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.Sfx
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// ── Streak cards ──────────────────────────────────────────────────
// One card per StreakTier. Every tier is built the same way — a ground, one picture with the
// day count at its heart, then the shared poster text (PosterChrome) — but the picture is its
// own: a flame for the first days, a lotus for a week, growth rings for two, then the night
// sky (StreakSky.kt) from a full moon up to a whole orbit round the sun.
//
// Arrival, in ms: the ground and glow come up first, the picture builds from ~150, the count
// rolls up from ~350, the text follows from 900 and the card settles at [StreakArt.introMs].

class StreakArt(kit: ShareKit, private val share: StreakShare) : ShareArt {
    override val format = ShareFormat.POST
    override val introMs = 2_000
    override val description = share.description

    override val cues: List<ShareCue> = when (share.tier) {
        StreakTier.RELIGHT -> emptyList()
        StreakTier.SPARK -> listOf(ShareCue(300, Sfx.STREAK_SPARK, haptic = CueHaptic.LAND))
        StreakTier.WEEK, StreakTier.ROOTED, StreakTier.MOON, StreakTier.TIDE ->
            listOf(ShareCue(340, Sfx.STREAK_MILESTONE, haptic = CueHaptic.LAND))
        StreakTier.SEASON, StreakTier.HALF_ORBIT, StreakTier.ORBIT ->
            listOf(ShareCue(300, Sfx.STREAK_LEGEND, haptic = CueHaptic.LAND))
    }

    private val c = StreakColors(kit)
    private val light = share.tier == StreakTier.ROOTED

    private val chrome = PosterChrome(
        kit = kit,
        ink = if (light) {
            PosterInk(c.inkSurface, c.stone600, withAlpha(c.stone500, 0.85f), c.zen700)
        } else {
            PosterInk(c.white, withAlpha(c.white, 0.72f), withAlpha(c.white, 0.42f), c.amber)
        },
        stamp = share.stamp,
        title = share.title,
        caption = share.caption,
        stats = share.stats,
        footer = share.footer
    )

    private val scene: StreakScene = when (share.tier) {
        StreakTier.RELIGHT -> RelightScene(kit, c, share)
        StreakTier.SPARK -> SparkScene(kit, c, share)
        StreakTier.WEEK -> LotusScene(kit, c, share)
        StreakTier.ROOTED -> RingsScene(kit, c, share)
        StreakTier.MOON -> MoonScene(kit, c, share)
        StreakTier.TIDE -> TideScene(kit, c, share)
        StreakTier.SEASON -> SeasonScene(kit, c, share)
        StreakTier.HALF_ORBIT, StreakTier.ORBIT -> OrbitScene(kit, c, share)
    }

    override fun draw(canvas: Canvas, timeMs: Float) {
        scene.draw(canvas, timeMs, chrome.artRect)
        scene.kit.grain(canvas, format, if (light) 0.05f else 0.07f)
        chrome.drawHeader(canvas, timeMs)
        chrome.drawText(canvas, timeMs, TEXT_START)
    }

    private companion object {
        const val TEXT_START = 900
    }
}

/** The streak cards' palette, resolved once. All primitives from colors.xml. */
internal class StreakColors(kit: ShareKit) {
    val white = kit.color(R.color.white)
    val ink = kit.color(R.color.ink_base)
    val inkSurface = kit.color(R.color.ink_surface)
    val inkGain = kit.color(R.color.ink_gain)
    val inkReward = kit.color(R.color.ink_reward)
    val ember = kit.color(R.color.ember_500)
    val amber = kit.color(R.color.amber_500)
    val amberOn = kit.color(R.color.amber_on)
    val amberDeep = kit.color(R.color.amber_800)
    val cream = kit.color(R.color.forest_sun_core)
    val zen300 = kit.color(R.color.zen_300)
    val zen500 = kit.color(R.color.zen_500)
    val zen700 = kit.color(R.color.zen_700)
    val zen900 = kit.color(R.color.zen_900)
    val paperRaised = kit.color(R.color.paper_raised)
    val paperSunk = kit.color(R.color.paper_sunk)
    val stone300 = kit.color(R.color.stone_300)
    val stone400 = kit.color(R.color.stone_400)
    val stone500 = kit.color(R.color.stone_500)
    val stone600 = kit.color(R.color.stone_600)
    val coinLight = kit.color(R.color.coin_gold_light)
    val skyTop = kit.color(R.color.forest_sky_top)
    val skyMid = kit.color(R.color.forest_sky_mid)
    val skyGlow = kit.color(R.color.forest_sky_glow)
    val night = kit.color(R.color.onboarding_night)
    val rankMid = kit.color(R.color.forest_rank_mid)
    val rankNear = kit.color(R.color.forest_rank_near)
    val ridge = kit.color(R.color.forest_ridge)
    val mist = kit.color(R.color.forest_mist)
    val woodFaceLight = kit.color(R.color.share_wood_face_light)
    val woodFace = kit.color(R.color.share_wood_face)
    val woodRing = kit.color(R.color.share_wood_ring)
    val woodBark = kit.color(R.color.share_wood_bark)
    val woodSide = kit.color(R.color.share_wood_side)
}

/** One tier's ground and picture. [art] is where the picture goes (see [PosterChrome.artRect]). */
internal abstract class StreakScene(val kit: ShareKit, val c: StreakColors, val share: StreakShare) {
    abstract fun draw(canvas: Canvas, t: Float, art: RectF)

    val fill = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Gradient fills only. Kept apart from [fill] because a shader still takes its paint's
     * alpha, and a faded colour left on [fill] would quietly fade the next gradient with it. */
    val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    /** A slow 0→1→0 swell for things that breathe once the card has settled. */
    protected fun breath(t: Float, periodMs: Float = 2_600f): Float = 0.5f + 0.5f * sin(t / periodMs * 2f * PI.toFloat())
}

/**
 * The count at the heart of a picture, rolling up from zero as the card arrives, with a small
 * label under it. Digits shrink as the number grows so 365 sits where 7 does.
 */
internal class HeroNumber(
    kit: ShareKit,
    private val value: Int,
    size: Float,
    private val color: Int,
    private val label: String?,
    private val labelColor: Int
) {
    private val digits = value.toString().length
    private val numberPaint = kit.paint(
        kit.mono,
        size * when {
            digits <= 2 -> 1f
            digits == 3 -> 0.8f
            else -> 0.64f
        },
        color,
        tracking = -0.05f,
        align = Paint.Align.CENTER
    )
    private val labelPaint = kit.paint(kit.mono, 22f, labelColor, tracking = 0.16f, align = Paint.Align.CENTER)
    private val digitHeight = Rect().also { numberPaint.getTextBounds("0", 0, 1, it) }.height()

    fun draw(canvas: Canvas, cx: Float, cy: Float, t: Float, start: Int, alpha: Float = 1f) {
        val roll = ShareMotion.beat(t, start, 1_000)
        if (roll <= 0f) return
        val shown = (value * roll).roundToInt()
        val gap = if (label != null) 26f else 0f
        val block = digitHeight + gap + if (label != null) 18f else 0f
        val baseline = cy - block / 2f + digitHeight
        numberPaint.color = withAlpha(color, alpha * ShareMotion.beat(t, start, 240))
        canvas.drawText(shown.toString(), cx, baseline, numberPaint)
        if (label != null) {
            labelPaint.color = withAlpha(labelColor, alpha * ShareMotion.beat(t, start + 200, 420))
            canvas.drawText(label, cx, baseline + gap + 18f, labelPaint)
        }
    }
}

/**
 * The unit petal: a leaf one unit long along +x, used for the lotus and the sapling's leaves.
 * Drawn under a canvas transform, so one path serves every size and angle. A fresh one per
 * scene — an export draws on its own thread, and paths aren't shared across threads.
 */
internal fun unitPetal() = Path().apply {
    moveTo(0f, 0f)
    cubicTo(0.2f, -0.31f, 0.64f, -0.3f, 1f, 0f)
    cubicTo(0.64f, 0.3f, 0.2f, 0.31f, 0f, 0f)
    close()
}

// ── Day 0: ready to relight ───────────────────────────────────────

/** A cold ring with one ember waiting at the top of it, and this week's kept days inside. */
internal class RelightScene(kit: ShareKit, c: StreakColors, share: StreakShare) : StreakScene(kit, c, share) {
    private val hero = HeroNumber(kit, share.keptThisWeek, 210f, c.white, "KEPT THIS WEEK", withAlpha(c.white, 0.5f))
    private val dash = DashPathEffect(floatArrayOf(14f, 20f), 0f)

    override fun draw(canvas: Canvas, t: Float, art: RectF) {
        kit.wash(canvas, ShareFormat.POST, 0f to c.inkSurface, 1f to c.ink)
        val cx = art.centerX()
        val cy = art.centerY()
        val r = 228f
        val b = breath(t)
        kit.glow(canvas, cx, cy + 70f, 460f, c.ember, 0.08f + 0.04f * b)

        stroke.strokeWidth = 4f
        stroke.pathEffect = dash
        stroke.color = withAlpha(c.white, 0.22f)
        canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, 360f * ShareMotion.beat(t, 120, 1_100), false, stroke)
        stroke.pathEffect = null

        val e = ShareMotion.pop(t, 1_050, 520)
        if (e > 0f) {
            kit.glow(canvas, cx, cy - r, 90f * e, c.amber, 0.32f + 0.22f * b)
            fill.color = c.amber
            canvas.drawCircle(cx, cy - r, 12f * e, fill)
        }
        hero.draw(canvas, cx, cy, t, 320)
    }
}

// ── Days 1–6: the spark ───────────────────────────────────────────

/** A three-layer flame with the count in its white-hot core, sparks lifting off the tip, and
 * seven dots underneath counting down to the first full week. */
internal class SparkScene(kit: ShareKit, c: StreakColors, share: StreakShare) : StreakScene(kit, c, share) {
    private val hero = HeroNumber(kit, share.days, 150f, c.inkSurface, null, 0)
    private val path = Path()
    private val sparks = Random(share.days * 31 + 7).let { rnd ->
        List(18) { floatArrayOf(rnd.nextFloat(), 0.6f + rnd.nextFloat() * 0.8f, rnd.nextFloat() - 0.5f, rnd.nextFloat()) }
    }

    override fun draw(canvas: Canvas, t: Float, art: RectF) {
        kit.wash(canvas, ShareFormat.POST, 0f to c.ink, 0.5f to c.inkReward, 1f to c.ink)
        val cx = art.centerX()
        val base = art.centerY() + 196f
        val grow = ShareMotion.beat(t, 160, 800)
        val b = breath(t, 1_900f)

        kit.glow(canvas, cx, base - 110f, 600f * (0.55f + 0.45f * grow), c.ember, (0.2f + 0.05f * b) * grow)
        kit.glow(canvas, cx, base - 170f, 330f, c.amber, 0.18f * grow)

        val sway = sin(t / 170f) * 8f + sin(t / 97f) * 4f
        val lift = 1f + 0.035f * sin(t / 133f)
        val h = 450f * (0.88f + 0.02f * share.days) * grow * lift
        val r = h * 0.31f
        if (grow > 0f) {
            flame(canvas, cx, base, r, h, sway, c.ember, 0.95f)
            flame(canvas, cx, base - r * 0.08f, r * 0.74f, h * 0.72f, sway * 0.75f, c.amber, 1f)
            flame(canvas, cx, base - r * 0.14f, r * 0.52f, h * 0.48f, sway * 0.5f, c.cream, 1f)
        }

        sparks.forEachIndexed { i, (phase, speed, spread, size) ->
            val u = ((phase + t / 1_700f * speed) % 1f)
            val y = base - h * 0.8f - u * 300f
            val x = cx + spread * 120f * (0.35f + u) + sin(t / 260f + i) * 10f
            fill.color = withAlpha(if (i % 3 == 0) c.cream else c.amber, (1f - u) * 0.85f * grow)
            canvas.drawCircle(x, y, 2.5f + size * 4f * (1f - u), fill)
        }

        hero.draw(canvas, cx, base - r * 0.14f - r * 0.52f, t, 380)

        val spacing = 54f
        val y = base + 84f
        for (i in 0 until 7) {
            val x = cx + (i - 3) * spacing
            val p = ShareMotion.pop(t, 900 + i * 70, 420)
            if (p <= 0f) continue
            if (i < share.days) {
                kit.glow(canvas, x, y, 38f * p, c.amber, 0.35f)
                fill.color = c.amber
                canvas.drawCircle(x, y, 11f * p, fill)
            } else {
                stroke.strokeWidth = 3f
                stroke.color = withAlpha(c.white, 0.26f * p)
                canvas.drawCircle(x, y, 10f, stroke)
            }
        }
    }

    /** A teardrop flame standing on [base]: round bottom of radius [r], tip [h] up, leaning [sway]. */
    private fun flame(canvas: Canvas, cx: Float, base: Float, r: Float, h: Float, sway: Float, color: Int, alpha: Float) {
        val tipX = cx + sway
        val tipY = base - h
        path.reset()
        path.moveTo(tipX, tipY)
        path.cubicTo(tipX + r * 0.18f, tipY + h * 0.3f, cx + r * 1.1f, base - h * 0.42f, cx + r, base - r)
        path.arcTo(RectF(cx - r, base - 2 * r, cx + r, base), 0f, 180f, false)
        path.cubicTo(cx - r * 1.1f, base - h * 0.42f, tipX - r * 0.18f, tipY + h * 0.3f, tipX, tipY)
        path.close()
        fill.color = withAlpha(color, alpha)
        canvas.drawPath(path, fill)
    }
}

// ── Days 7–13: a week in bloom ────────────────────────────────────

/** Seven petals — a week's worth of kept days — opening around a green heart with the count in
 * it, ringed by this week's days, lit on the ones kept. Past seven, dots count on to two weeks. */
internal class LotusScene(kit: ShareKit, c: StreakColors, share: StreakShare) : StreakScene(kit, c, share) {
    private val hero = HeroNumber(kit, share.days, 150f, c.white, "DAYS", withAlpha(c.white, 0.6f))
    private val petalShape = unitPetal()
    private val dayPaint = kit.paint(kit.mono, 26f, c.white, tracking = 0.08f, align = Paint.Align.CENTER)
    private val outerShader = LinearGradient(0f, 0f, 1f, 0f, intArrayOf(c.amber, c.coinLight, c.cream), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
    private val innerShader = LinearGradient(0f, 0f, 1f, 0f, intArrayOf(c.amberDeep, c.amber), null, Shader.TileMode.CLAMP)

    /** This week, Monday first, as weekday initials — lit amber on the days actually kept. */
    private val letters = DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, Locale.US) }

    override fun draw(canvas: Canvas, t: Float, art: RectF) {
        kit.wash(canvas, ShareFormat.POST, 0f to c.inkGain, 0.7f to c.ink, 1f to c.ink)
        val cx = art.centerX()
        val cy = art.centerY() + 6f
        val b = breath(t, 3_000f)
        kit.glow(canvas, cx, cy, 470f, c.zen300, 0.13f + 0.04f * b)
        kit.glow(canvas, cx, cy, 250f, c.amber, 0.12f)

        val spin = 1.8f * sin(t / 1_500f)
        val step = 360f / 7f
        for (i in 0 until 7) {
            val p = ShareMotion.beat(t, 240 + i * 75, 600)
            if (p <= 0f) continue
            petal(canvas, cx, cy, -90f + i * step + spin - (1f - p) * 26f, 262f * (0.2f + 0.8f * p), outerShader, p)
            val lp = ShareMotion.beat(t, 760 + i * 60, 420)
            val a = Math.toRadians((-90f + i * step + spin).toDouble())
            val kept = share.week.getOrNull(i) == true
            dayPaint.color = withAlpha(if (kept) c.amber else c.white, (if (kept) 0.95f else 0.4f) * lp)
            canvas.drawText(letters[i], cx + cos(a).toFloat() * 304f, cy + sin(a).toFloat() * 304f + 9f, dayPaint)
        }
        for (i in 0 until 7) {
            val p = ShareMotion.beat(t, 520 + i * 60, 520)
            if (p > 0f) petal(canvas, cx, cy, -90f + (i + 0.5f) * step + spin, 178f * (0.3f + 0.7f * p), innerShader, p)
        }

        val heart = ShareMotion.pop(t, 300, 640)
        if (heart > 0f) {
            shade.shader = RadialGradient(cx, cy - 30f, 150f, intArrayOf(c.zen500, c.zen700, c.zen900), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, 116f * heart, shade)
            shade.shader = null
        }
        hero.draw(canvas, cx, cy, t, 420)

        // Days past the week, counting towards two. Nothing to count on day seven itself.
        val extra = (share.days - 7).coerceIn(0, 7)
        if (extra > 0) for (i in 0 until 7) {
            val p = ShareMotion.pop(t, 1_150 + i * 60, 380)
            if (p <= 0f) continue
            val a = Math.toRadians((-90f + (i + 0.5f) * step + spin).toDouble())
            val x = cx + cos(a).toFloat() * 352f
            val y = cy + sin(a).toFloat() * 352f
            if (i < extra) {
                fill.color = c.amber
                canvas.drawCircle(x, y, 7f * p, fill)
            } else {
                stroke.strokeWidth = 2.5f
                stroke.color = withAlpha(c.white, 0.18f * p)
                canvas.drawCircle(x, y, 6f, stroke)
            }
        }
    }

    private fun petal(canvas: Canvas, cx: Float, cy: Float, angle: Float, length: Float, shader: Shader, alpha: Float) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(angle)
        canvas.scale(length, length)
        shade.shader = shader
        shade.alpha = (alpha * 255).roundToInt()
        canvas.drawPath(petalShape, shade)
        shade.shader = null
        shade.alpha = 255
        canvas.restore()
    }
}

// ── Days 14–29: growth rings ──────────────────────────────────────

/** A cut log on paper: one growth ring per day, the count in the heartwood, and a sprout
 * pushing up from the bark — the habit taking root. The one light card in the family. */
internal class RingsScene(kit: ShareKit, c: StreakColors, share: StreakShare) : StreakScene(kit, c, share) {
    private val hero = HeroNumber(kit, share.days, 176f, c.inkSurface, "DAYS KEPT", withAlpha(c.inkSurface, 0.55f))
    private val radius = 232f
    private val petalShape = unitPetal()
    private val stem = Path()
    private val rings: List<Path> = buildRings()

    private fun buildRings(): List<Path> {
        val n = share.days
        return List(n) { k ->
            val f = ((k + 1f) / n).pow(0.82f)
            val r = 64f + (radius - 30f - 64f) * f
            Path().apply {
                val steps = 96
                for (s in 0..steps) {
                    val a = s / steps.toFloat() * 2f * PI.toFloat()
                    val wobble = 3.4f * sin(3f * a + k * 0.9f) + 2.1f * sin(5f * a - k * 0.6f) + 1.2f * sin(9f * a + k)
                    val x = cos(a) * (r + wobble * f)
                    val y = sin(a) * (r + wobble * f)
                    if (s == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        }
    }

    override fun draw(canvas: Canvas, t: Float, art: RectF) {
        kit.wash(canvas, ShareFormat.POST, 0f to c.paperRaised, 1f to c.paperSunk)
        val cx = art.centerX()
        val cy = art.centerY() + 34f
        kit.glow(canvas, cx, cy, 470f, c.amber, 0.16f)

        val slice = ShareMotion.pop(t, 120, 700, overshoot = 1.2f)
        if (slice > 0f) {
            canvas.save()
            canvas.scale(slice, slice, cx, cy)
            // Shadow, then the bark side showing under the face, then the face.
            fill.color = withAlpha(c.inkSurface, 0.12f)
            canvas.drawOval(RectF(cx - radius * 0.95f, cy + radius - 14f, cx + radius * 0.95f, cy + radius + 40f), fill)
            fill.color = c.woodSide
            canvas.drawCircle(cx, cy + 18f, radius, fill)
            fill.color = c.woodBark
            canvas.drawCircle(cx, cy, radius, fill)
            shade.shader = RadialGradient(cx - 40f, cy - 50f, radius * 1.05f, intArrayOf(c.woodFaceLight, c.woodFace), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, radius - 16f, shade)
            shade.shader = null
            canvas.restore()
        }

        canvas.save()
        canvas.translate(cx, cy)
        stroke.strokeWidth = 2.4f
        rings.forEachIndexed { k, ring ->
            val p = ShareMotion.beat(t, 360 + k * 26, 260)
            if (p <= 0f) return@forEachIndexed
            stroke.color = withAlpha(c.woodRing, (if (k % 2 == 0) 0.62f else 0.34f) * p)
            canvas.drawPath(ring, stroke)
        }
        canvas.restore()

        // A hairline crack from the bark in, like every real cut log has.
        val crack = ShareMotion.beat(t, 1_000, 500)
        stroke.strokeWidth = 2f
        stroke.color = withAlpha(c.woodBark, 0.5f * crack)
        canvas.drawLine(cx + radius * 0.72f, cy + radius * 0.5f, cx + radius * (0.72f - 0.3f * crack), cy + radius * (0.5f - 0.18f * crack), stroke)

        kit.glow(canvas, cx, cy, 160f, c.woodFaceLight, 0.85f * ShareMotion.beat(t, 380, 500))
        hero.draw(canvas, cx, cy, t, 420)

        sprout(canvas, cx + 6f, cy - radius + 4f, t)
    }

    private fun sprout(canvas: Canvas, x: Float, y: Float, t: Float) {
        val g = ShareMotion.beat(t, 1_000, 700)
        if (g <= 0f) return
        canvas.save()
        canvas.rotate(3f * sin(t / 900f), x, y)
        stroke.strokeWidth = 7f
        stroke.color = c.zen700
        stem.reset()
        stem.moveTo(x, y)
        stem.quadTo(x + 12f * g, y - 50f * g, x - 4f * g, y - 98f * g)
        canvas.drawPath(stem, stroke)
        val tipX = x - 4f * g
        val tipY = y - 98f * g
        leaf(canvas, tipX, tipY, -150f, 74f * ShareMotion.beat(t, 1_350, 420), c.zen500)
        leaf(canvas, tipX, tipY, -35f, 82f * ShareMotion.beat(t, 1_450, 420), c.zen700)
        canvas.restore()
    }

    private fun leaf(canvas: Canvas, x: Float, y: Float, angle: Float, length: Float, color: Int) {
        if (length <= 0f) return
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(angle)
        canvas.scale(length, length * 1.25f)
        fill.color = color
        canvas.drawPath(petalShape, fill)
        canvas.restore()
    }
}
