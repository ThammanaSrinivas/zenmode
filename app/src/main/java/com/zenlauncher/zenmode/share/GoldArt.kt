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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── Zen Gold cards ────────────────────────────────────────────────
// While the week is being earned, the picture is an empty coin slowly filling with liquid gold —
// one fifth per kept day — over a row of the week's seven days. Gold pay unlocked mints the coin.
// With a real balance the picture grows with it: one coin with the amount struck on it, then
// stacks, then bars, then a whole vault of them under a fan of light. Everything is drawn in the
// coin_gold_* ramp the onboarding coin already uses, on warm ink (or cream, for the first coin).

class GoldArt(private val kit: ShareKit, private val share: GoldShare) : ShareArt {
    override val format = ShareFormat.POST
    override val introMs = 2_000
    override val description = share.description

    override val cues: List<ShareCue> = when (share.tier) {
        GoldTier.EARNING -> listOf(ShareCue(520, Sfx.COIN, haptic = CueHaptic.TICK))
        GoldTier.FIRST -> listOf(ShareCue(320, Sfx.COIN, haptic = CueHaptic.LAND))
        else -> listOf(ShareCue(300, Sfx.GOLD_CASCADE, haptic = CueHaptic.LAND))
    }

    private val white = kit.color(R.color.white)
    private val ink = kit.color(R.color.ink_base)
    private val inkSurface = kit.color(R.color.ink_surface)
    private val inkReward = kit.color(R.color.ink_reward)
    private val amber = kit.color(R.color.amber_500)
    private val amberOn = kit.color(R.color.amber_on)
    private val amberDeep = kit.color(R.color.amber_800)
    private val cream = kit.color(R.color.forest_sun_core)
    private val paperRaised = kit.color(R.color.paper_raised)
    private val paperSunk = kit.color(R.color.paper_sunk)
    private val stone500 = kit.color(R.color.stone_500)
    private val stone600 = kit.color(R.color.stone_600)
    private val hi = kit.color(R.color.coin_gold_highlight)
    private val light = kit.color(R.color.coin_gold_light)
    private val face = kit.color(R.color.coin_gold_face)
    private val deep = kit.color(R.color.coin_gold_deep)
    private val edge = kit.color(R.color.coin_gold_edge)
    private val shadow = kit.color(R.color.coin_gold_shadow)

    private val paper = share.tier == GoldTier.FIRST

    private val chrome = PosterChrome(
        kit = kit,
        ink = if (paper) {
            PosterInk(inkSurface, stone600, withAlpha(stone500, 0.85f), amberDeep)
        } else {
            PosterInk(white, withAlpha(white, 0.72f), withAlpha(white, 0.42f), amber)
        },
        stamp = share.stamp,
        title = share.title,
        caption = share.caption,
        stats = share.stats,
        footer = share.footer
    )

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Gradient fills only — see [StreakScene.shade] for why it's its own paint. */
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val path = Path()
    private val dash = DashPathEffect(floatArrayOf(14f, 14f), 0f)
    private val dayPaint = kit.paint(kit.mono, 22f, white, tracking = 0.1f, align = Paint.Align.CENTER)
    private val bigNumber = kit.paint(kit.mono, 150f, white, tracking = -0.05f, align = Paint.Align.CENTER)
    private val amountPaint = kit.paint(kit.mono, 132f, cream, tracking = -0.04f, align = Paint.Align.CENTER)
    private val engrave = kit.paint(kit.mono, 92f, edge, tracking = -0.04f, align = Paint.Align.CENTER)
    private val sparkles = Random(share.investedRupees.toInt() + share.kept * 7).let { rnd ->
        List(9) { floatArrayOf(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 0.8f, rnd.nextFloat() * TAU) }
    }
    private val todayIndex = share.today.dayOfWeek.value - 1

    override fun draw(canvas: Canvas, timeMs: Float) {
        val art = chrome.artRect
        val cx = art.centerX()
        val cy = art.centerY()
        if (paper) {
            kit.wash(canvas, format, 0f to amberOn, 0.7f to paperRaised, 1f to paperSunk)
            kit.glow(canvas, cx, cy, 560f, amber, 0.22f)
        } else {
            kit.wash(canvas, format, 0f to inkReward, 0.75f to ink, 1f to ink)
            kit.glow(canvas, cx, cy, 640f, amber, 0.14f)
        }
        when (share.tier) {
            GoldTier.EARNING -> {
                slot(canvas, cx, cy - 40f, 186f, timeMs)
                week(canvas, cx, cy + 236f, timeMs)
            }
            GoldTier.UNLOCKED -> {
                rays(canvas, cx, cy - 40f, timeMs, 0.12f)
                coin(canvas, cx, cy - 40f, 192f * ShareMotion.pop(timeMs, 150, 760, overshoot = 1.3f), null)
                sparkle(canvas, art, timeMs, 6)
                week(canvas, cx, cy + 236f, timeMs)
            }
            GoldTier.FIRST -> {
                fill.color = withAlpha(shadow, 0.16f)
                canvas.drawOval(RectF(cx - 170f, cy + 196f, cx + 170f, cy + 236f), fill)
                coin(canvas, cx, cy, 206f * ShareMotion.pop(timeMs, 150, 760, overshoot = 1.3f), share.amount)
                sparkle(canvas, art, timeMs, 5)
            }
            GoldTier.STACK -> {
                amount(canvas, cx, art.top + 170f, timeMs)
                stacks(canvas, cx, cy + 200f, timeMs)
                sparkle(canvas, art, timeMs, 5)
            }
            GoldTier.BARS -> {
                rays(canvas, cx, cy + 40f, timeMs, 0.08f)
                pyramid(canvas, cx, cy + 210f, rows = 2, timeMs, engraved = share.amount)
                sparkle(canvas, art, timeMs, 6)
            }
            GoldTier.VAULT -> {
                rays(canvas, cx, cy + 80f, timeMs, 0.12f)
                amount(canvas, cx, art.top + 150f, timeMs)
                pyramid(canvas, cx, cy + 230f, rows = 3, timeMs, engraved = null)
                sparkle(canvas, art, timeMs, 9)
            }
        }
        kit.grain(canvas, format, if (paper) 0.05f else 0.07f)
        chrome.drawHeader(canvas, timeMs)
        chrome.drawText(canvas, timeMs, 900)
    }

    // ── Pieces ────────────────────────────────────────────────────

    /**
     * An empty coin filling with liquid gold, a fifth per kept day, the "kept/needed" count in
     * it — white over the empty part, ink where the gold has risen past it.
     */
    private fun slot(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        val inner = r - 10f
        fill.color = withAlpha(ink, 0.5f)
        canvas.drawCircle(cx, cy, r, fill)
        stroke.strokeWidth = 4f
        stroke.pathEffect = dash
        stroke.color = withAlpha(amber, 0.6f * ShareMotion.beat(t, 80, 500))
        canvas.drawCircle(cx, cy, r, stroke)
        stroke.pathEffect = null

        val level = (share.kept.toFloat() / share.needed).coerceIn(0f, 1f) * ShareMotion.beat(t, 300, 1_300)
        val surface = cy + inner - 2f * inner * level
        path.reset()
        if (level > 0f) {
            path.moveTo(cx - inner, cy + inner + 10f)
            var x = cx - inner
            while (x <= cx + inner) {
                path.lineTo(x, surface + 9f * sin(x / 52f + t / 460f) + 4f * sin(x / 23f - t / 300f))
                x += 8f
            }
            path.lineTo(cx + inner, cy + inner + 10f)
            path.close()
            canvas.save()
            canvas.clipPath(Path().apply { addCircle(cx, cy, inner, Path.Direction.CW) })
            shade.shader = LinearGradient(0f, surface - 20f, 0f, cy + inner, intArrayOf(hi, light, face, deep), floatArrayOf(0f, 0.2f, 0.7f, 1f), Shader.TileMode.CLAMP)
            canvas.drawPath(path, shade)
            shade.shader = null
            // The meniscus: a bright line riding the surface.
            stroke.strokeWidth = 4f
            stroke.color = withAlpha(hi, 0.9f)
            canvas.drawPath(path, stroke)
            canvas.restore()
        }

        val label = "${share.kept}/${share.needed}"
        val bounds = Rect().also { bigNumber.getTextBounds("0", 0, 1, it) }
        val baseline = cy + bounds.height() / 2f
        bigNumber.color = withAlpha(white, ShareMotion.beat(t, 260, 400))
        canvas.drawText(label, cx, baseline, bigNumber)
        if (level > 0f) {
            canvas.save()
            canvas.clipPath(path)
            bigNumber.color = withAlpha(inkSurface, ShareMotion.beat(t, 260, 400))
            canvas.drawText(label, cx, baseline, bigNumber)
            canvas.restore()
        }
    }

    /** The week as seven small coins: struck for a kept day, struck out for a missed one,
     * dashed while still open. Today's initial is lit. */
    private fun week(canvas: Canvas, cx: Float, y: Float, t: Float) {
        val spacing = 82f
        share.week.units.take(7).forEachIndexed { i, unit ->
            val x = cx + (i - 3) * spacing
            val p = ShareMotion.pop(t, 700 + i * 70, 420)
            if (p <= 0f) return@forEachIndexed
            when (unit.kept) {
                true -> coin(canvas, x, y, 25f * p, null, small = true)
                false -> {
                    stroke.strokeWidth = 3f
                    stroke.color = withAlpha(stone500, 0.7f * p)
                    canvas.drawCircle(x, y, 22f, stroke)
                    canvas.drawLine(x - 12f, y + 12f, x + 12f, y - 12f, stroke)
                }
                null -> {
                    stroke.strokeWidth = 3f
                    stroke.pathEffect = DashPathEffect(floatArrayOf(6f, 7f), 0f)
                    stroke.color = withAlpha(amber, 0.5f * p)
                    canvas.drawCircle(x, y, 22f, stroke)
                    stroke.pathEffect = null
                }
            }
            dayPaint.color = withAlpha(if (i == todayIndex) amber else white, (if (i == todayIndex) 0.95f else 0.5f) * p)
            canvas.drawText(unit.label, x, y + 66f, dayPaint)
        }
    }

    /** A struck coin, face on: reeded rim, a raised inner face, the mark (or [struck] text) embossed. */
    private fun coin(canvas: Canvas, cx: Float, cy: Float, r: Float, struck: String?, small: Boolean = false) {
        if (r <= 0f) return
        shade.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, intArrayOf(hi, face, edge), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, shade)
        shade.shader = RadialGradient(cx - r * 0.3f, cy - r * 0.35f, r * 1.2f, intArrayOf(hi, light, face, deep), floatArrayOf(0f, 0.3f, 0.7f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r * 0.84f, shade)
        shade.shader = null
        stroke.strokeWidth = r * 0.02f
        stroke.color = withAlpha(deep, 0.7f)
        canvas.drawCircle(cx, cy, r * 0.84f, stroke)
        if (small) return

        // Reeding round the rim.
        stroke.strokeWidth = r * 0.012f
        stroke.color = withAlpha(edge, 0.4f)
        for (i in 0 until 96) {
            val a = i * TAU / 96
            canvas.drawLine(cx + cos(a) * r * 0.89f, cy + sin(a) * r * 0.89f, cx + cos(a) * r * 0.97f, cy + sin(a) * r * 0.97f, stroke)
        }
        if (struck != null) {
            engrave.textSize = r * 0.46f
            val bounds = Rect().also { engrave.getTextBounds("0", 0, 1, it) }
            val baseline = cy + bounds.height() / 2f
            engrave.color = withAlpha(hi, 0.8f)
            canvas.drawText(struck, cx - 2f, baseline - 2f, engrave)
            engrave.color = withAlpha(edge, 0.85f)
            canvas.drawText(struck, cx, baseline, engrave)
        } else {
            val side = r * 0.9f
            kit.drawMark(canvas, RectF(cx - side / 2 - 3f, cy - side / 2 - 3f, cx + side / 2 - 3f, cy + side / 2 - 3f), hi, 0.7f)
            kit.drawMark(canvas, RectF(cx - side / 2, cy - side / 2, cx + side / 2, cy + side / 2), deep, 0.75f)
        }
        // A soft sheen across the face.
        canvas.save()
        canvas.clipPath(Path().apply { addCircle(cx, cy, r, Path.Direction.CW) })
        shade.shader = LinearGradient(cx - r, cy - r, cx + r * 0.2f, cy + r * 0.2f, intArrayOf(withAlpha(white, 0f), withAlpha(white, 0.28f), withAlpha(white, 0f)), floatArrayOf(0.35f, 0.5f, 0.65f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, shade)
        shade.shader = null
        canvas.restore()
    }

    /** One coin seen from the side, top face up, for building stacks. */
    private fun edgeCoin(canvas: Canvas, cx: Float, y: Float, rx: Float, thick: Float) {
        val ry = rx * 0.3f
        path.reset()
        path.moveTo(cx - rx, y)
        path.lineTo(cx - rx, y + thick)
        path.arcTo(RectF(cx - rx, y + thick - ry, cx + rx, y + thick + ry), 180f, -180f, false)
        path.lineTo(cx + rx, y)
        path.close()
        shade.shader = LinearGradient(cx - rx, 0f, cx + rx, 0f, intArrayOf(edge, face, light, deep, edge), floatArrayOf(0f, 0.3f, 0.45f, 0.8f, 1f), Shader.TileMode.CLAMP)
        canvas.drawPath(path, shade)
        shade.shader = RadialGradient(cx - rx * 0.2f, y - ry * 0.3f, rx, intArrayOf(hi, light, face), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(cx - rx, y - ry, cx + rx, y + ry), shade)
        shade.shader = null
        stroke.strokeWidth = 2f
        stroke.color = withAlpha(deep, 0.6f)
        canvas.drawOval(RectF(cx - rx * 0.86f, y - ry * 0.86f, cx + rx * 0.86f, y + ry * 0.86f), stroke)
    }

    /** Three stacks, the middle one tallest and nearest, landing one coin at a time. */
    private fun stacks(canvas: Canvas, cx: Float, base: Float, t: Float) {
        val plan = listOf(Triple(cx - 210f, base - 26f, 6), Triple(cx + 210f, base - 26f, 8), Triple(cx, base, 11))
        val rx = 96f
        val thick = 17f
        plan.forEachIndexed { s, (x, y, count) ->
            fill.color = withAlpha(ink, 0.35f)
            canvas.drawOval(RectF(x - rx * 1.1f, y + thick - 6f, x + rx * 1.1f, y + thick + 34f), fill)
            for (i in 0 until count) {
                val p = ShareMotion.beat(t, 260 + s * 120 + i * 45, 320)
                if (p <= 0f) break
                val jitter = ((i * 37 + s * 11) % 7 - 3).toFloat()
                edgeCoin(canvas, x + jitter, y - i * thick - 60f * (1f - p), rx, thick)
            }
        }
    }

    /** Gold bars stacked in a pyramid of [rows]; the top bar can carry [engraved]. */
    private fun pyramid(canvas: Canvas, cx: Float, base: Float, rows: Int, t: Float, engraved: String?) {
        val w = if (rows == 3) 230f else 270f
        val h = if (rows == 3) 82f else 96f
        var order = 0
        for (row in 0 until rows) {
            val count = rows - row
            val y = base - row * (h + h * 0.36f)
            for (i in 0 until count) {
                val x = cx + (i - (count - 1) / 2f) * (w + 14f)
                val p = ShareMotion.pop(t, 240 + order * 110, 520, overshoot = 1.2f)
                order++
                if (p <= 0f) continue
                bar(canvas, x, y - 40f * (1f - p), w, h, if (row == rows - 1) engraved else null, t)
            }
        }
    }

    private fun bar(canvas: Canvas, cx: Float, bottom: Float, w: Float, h: Float, engraved: String?, t: Float) {
        val topW = w * 0.84f
        val lidW = w * 0.68f
        val lid = h * 0.36f
        fill.color = withAlpha(ink, 0.35f)
        canvas.drawOval(RectF(cx - w * 0.56f, bottom - 10f, cx + w * 0.56f, bottom + 22f), fill)

        path.reset()
        path.moveTo(cx - w / 2, bottom)
        path.lineTo(cx + w / 2, bottom)
        path.lineTo(cx + topW / 2, bottom - h)
        path.lineTo(cx - topW / 2, bottom - h)
        path.close()
        shade.shader = LinearGradient(0f, bottom - h, 0f, bottom, intArrayOf(light, face, deep), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawPath(path, shade)

        val front = Path(path)
        path.reset()
        path.moveTo(cx - topW / 2, bottom - h)
        path.lineTo(cx + topW / 2, bottom - h)
        path.lineTo(cx + lidW / 2, bottom - h - lid)
        path.lineTo(cx - lidW / 2, bottom - h - lid)
        path.close()
        shade.shader = LinearGradient(0f, bottom - h - lid, 0f, bottom - h, intArrayOf(hi, light), null, Shader.TileMode.CLAMP)
        canvas.drawPath(path, shade)
        shade.shader = null

        stroke.strokeWidth = 2f
        stroke.color = withAlpha(edge, 0.55f)
        canvas.drawPath(front, stroke)
        canvas.drawPath(path, stroke)

        if (engraved != null) {
            engrave.textSize = h * 0.5f
            val bounds = Rect().also { engrave.getTextBounds("0", 0, 1, it) }
            val baseline = bottom - h / 2f + bounds.height() / 2f
            engrave.color = withAlpha(hi, 0.7f)
            canvas.drawText(engraved, cx - 1.5f, baseline - 1.5f, engrave)
            engrave.color = withAlpha(edge, 0.85f)
            canvas.drawText(engraved, cx, baseline, engrave)
        }

        // A sheen sweeping across, now and then.
        val sweep = ((t / 2_600f) % 1f) * 2.4f - 0.7f
        canvas.save()
        canvas.clipPath(front)
        val sx = cx - w / 2 + sweep * w
        shade.shader = LinearGradient(sx - 60f, bottom, sx + 60f, bottom - h, intArrayOf(withAlpha(white, 0f), withAlpha(white, 0.35f), withAlpha(white, 0f)), null, Shader.TileMode.CLAMP)
        canvas.drawRect(cx - w, bottom - h, cx + w, bottom, shade)
        shade.shader = null
        canvas.restore()
    }

    /** The balance itself, rolling up in mono over a warm halo. */
    private fun amount(canvas: Canvas, cx: Float, baseline: Float, t: Float) {
        val roll = ShareMotion.beat(t, 300, 1_100)
        val shown = GoldShare.rupeesLabel((share.investedRupees * roll).toLong())
        kit.glow(canvas, cx, baseline - 46f, 300f, amber, 0.18f * roll)
        amountPaint.color = withAlpha(cream, ShareMotion.beat(t, 300, 300))
        canvas.drawText(shown, cx, baseline, amountPaint)
    }

    /** A slow fan of light behind the gold, fading out well before the text. */
    private fun rays(canvas: Canvas, cx: Float, cy: Float, t: Float, strength: Float) {
        val open = ShareMotion.beat(t, 200, 1_000)
        if (open <= 0f) return
        shade.shader = RadialGradient(cx, cy, 560f, intArrayOf(withAlpha(amber, strength * open), withAlpha(amber, 0f)), null, Shader.TileMode.CLAMP)
        for (i in 0 until 14) {
            val a = i * TAU / 14 + t / 12_000f
            path.reset()
            path.moveTo(cx, cy)
            path.lineTo(cx + cos(a - 0.07f) * 620f, cy + sin(a - 0.07f) * 620f)
            path.lineTo(cx + cos(a + 0.07f) * 620f, cy + sin(a + 0.07f) * 620f)
            path.close()
            canvas.drawPath(path, shade)
        }
        shade.shader = null
    }

    private fun sparkle(canvas: Canvas, art: RectF, t: Float, count: Int) {
        val on = ShareMotion.beat(t, 900, 600)
        stroke.strokeWidth = 2.6f
        sparkles.take(count).forEach { (fx, fy, size, phase) ->
            val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(t / 380f + phase))
            val x = 120f + fx * 840f
            val y = art.top + 20f + fy * (art.height() - 40f)
            val r = 15f * size * tw
            kit.glow(canvas, x, y, r * 2.4f, amber, 0.4f * on)
            stroke.color = withAlpha(if (paper) amberDeep else cream, 0.9f * tw * on)
            canvas.drawLine(x - r, y, x + r, y, stroke)
            canvas.drawLine(x, y - r, x, y + r, stroke)
        }
    }

    private companion object {
        val TAU = (2 * PI).toFloat()
    }
}
