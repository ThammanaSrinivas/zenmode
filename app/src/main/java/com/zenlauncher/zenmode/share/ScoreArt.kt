package com.zenlauncher.zenmode.share

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.Sfx
import com.zenlauncher.zenmode.coreapi.ZenScore
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

// ── Zen Score cards: the day as water ─────────────────────────────
// The score sits in a dial at the centre of a pond, and the pond is the day: rings spread out
// from the dial for as long as the card is open, perfectly round when the day was calm and
// more ragged the noisier it got. A near-perfect day adds a few glints of light on the water;
// a choppy one, wind; a stormy one, rain. The bowl that plays as it lands is pitched with the
// score — higher for calm, lower for loud — and always in the app's key.

class ScoreArt(private val kit: ShareKit, private val share: ScoreShare) : ShareArt {
    override val format = ShareFormat.POST
    override val introMs = 2_000
    override val description = share.description

    override val cues = listOf(
        ShareCue(
            atMs = 280,
            sfx = Sfx.SCORE_BOWL,
            rate = when (share.band) {
                ScoreBand.STILL -> Pentatonic.E5
                ScoreBand.CALM -> Pentatonic.D5
                ScoreBand.STEADY -> Pentatonic.C5
                ScoreBand.CHOPPY -> Pentatonic.A4
                ScoreBand.STORMY -> Pentatonic.G4
            },
            haptic = CueHaptic.LAND
        )
    )

    private val white = kit.color(R.color.white)
    private val ink = kit.color(R.color.ink_base)
    private val inkSurface = kit.color(R.color.ink_surface)
    private val inkGain = kit.color(R.color.ink_gain)
    private val zen300 = kit.color(R.color.zen_300)
    private val zen700 = kit.color(R.color.zen_700)
    private val zen900 = kit.color(R.color.zen_900)
    private val amber = kit.color(R.color.amber_500)
    private val amberOn = kit.color(R.color.amber_on)
    private val amberDeep = kit.color(R.color.amber_800)
    private val ember = kit.color(R.color.ember_500)
    private val dusk = kit.color(R.color.recap_dusk)
    private val duskGlow = kit.color(R.color.recap_dusk_glow)
    private val paperRaised = kit.color(R.color.paper_raised)
    private val paperSunk = kit.color(R.color.paper_sunk)
    private val paperWhite = kit.color(R.color.paper_white)
    private val stone500 = kit.color(R.color.stone_500)
    private val stone600 = kit.color(R.color.stone_600)
    private val cream = kit.color(R.color.forest_sun_core)

    private val light = share.band == ScoreBand.STEADY || share.band == ScoreBand.CHOPPY
    private val type = if (light) inkSurface else white

    /** The water's character for this band: ring count, how ragged, the ink it's drawn in. */
    private val water = when (share.band) {
        ScoreBand.STILL -> Water(rings = 4, wobble = 0f, jag = 0f, color = zen300, alpha = 0.3f, period = 5_600f)
        ScoreBand.CALM -> Water(rings = 5, wobble = 3f, jag = 0.2f, color = zen300, alpha = 0.26f, period = 5_000f)
        ScoreBand.STEADY -> Water(rings = 6, wobble = 6f, jag = 0.35f, color = zen700, alpha = 0.3f, period = 4_400f)
        ScoreBand.CHOPPY -> Water(rings = 7, wobble = 11f, jag = 0.6f, color = amberDeep, alpha = 0.3f, period = 3_600f)
        ScoreBand.STORMY -> Water(rings = 9, wobble = 18f, jag = 1f, color = duskGlow, alpha = 0.32f, period = 2_800f)
    }

    private val chrome = PosterChrome(
        kit = kit,
        ink = if (light) {
            PosterInk(inkSurface, stone600, withAlpha(stone500, 0.85f), if (share.band == ScoreBand.STEADY) zen700 else amberDeep)
        } else {
            PosterInk(white, withAlpha(white, 0.72f), withAlpha(white, 0.42f), if (share.band == ScoreBand.STORMY) duskGlow else zen300)
        },
        stamp = share.stamp,
        title = share.title,
        caption = share.caption,
        stats = share.stats,
        footer = share.footer
    )

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val ripple = Path()
    private val numberPaint = kit.paint(kit.mono, 168f, type, tracking = -0.05f, align = Paint.Align.CENTER)
    private val labelPaint = kit.paint(kit.mono, 22f, withAlpha(type, 0.55f), tracking = 0.16f, align = Paint.Align.CENTER)
    private val drops = Random(share.tenths + 11).let { rnd -> List(70) { floatArrayOf(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 0.8f) } }
    private val glints = Random(share.tenths).let { rnd -> List(6) { floatArrayOf(rnd.nextFloat() * TAU, rnd.nextFloat()) } }

    override fun draw(canvas: Canvas, timeMs: Float) {
        val art = chrome.artRect
        val cx = art.centerX()
        val cy = art.centerY() + 6f
        ground(canvas, cx, cy)

        // The water fades out above the title rather than stopping at an edge.
        val fadeTop = art.bottom - 230f
        val fadeBottom = art.bottom + 10f
        ripples(canvas, cx, cy, timeMs, fadeTop, fadeBottom)
        weather(canvas, art, cx, cy, timeMs, fadeTop, fadeBottom)

        dial(canvas, cx, cy, timeMs)
        kit.grain(canvas, format, if (light) 0.05f else 0.07f)
        chrome.drawHeader(canvas, timeMs)
        chrome.drawText(canvas, timeMs, 900)
    }

    private fun ground(canvas: Canvas, cx: Float, cy: Float) {
        when (share.band) {
            ScoreBand.STILL -> kit.wash(canvas, format, 0f to zen900, 0.5f to inkGain, 1f to ink)
            ScoreBand.CALM -> kit.wash(canvas, format, 0f to inkGain, 0.8f to ink, 1f to ink)
            ScoreBand.STEADY -> kit.wash(canvas, format, 0f to paperRaised, 1f to paperSunk)
            ScoreBand.CHOPPY -> kit.wash(canvas, format, 0f to amberOn, 0.6f to paperRaised, 1f to paperSunk)
            ScoreBand.STORMY -> kit.wash(canvas, format, 0f to dusk, 1f to ink)
        }
        val glow = when (share.band) {
            ScoreBand.STILL, ScoreBand.CALM, ScoreBand.STEADY -> zen300
            ScoreBand.CHOPPY -> amber
            ScoreBand.STORMY -> duskGlow
        }
        kit.glow(canvas, cx, cy, 720f, glow, if (light) 0.16f else 0.12f)
    }

    /** Rings spreading out from the dial, round or ragged by band, for as long as the card moves. */
    private fun ripples(canvas: Canvas, cx: Float, cy: Float, t: Float, fadeTop: Float, fadeBottom: Float) {
        val inner = 262f
        val outer = 700f
        stroke.strokeWidth = 3f
        for (k in 0 until water.rings) {
            val intro = ShareMotion.beat(t, 200 + k * 70, 800)
            if (intro <= 0f) continue
            val u = ((k + t / water.period) / water.rings) % 1f
            val r = inner + u * (outer - inner)
            val amplitude = water.wobble * (0.5f + u)
            val phase = k * 1.7f
            ripple.reset()
            val steps = 128
            for (s in 0..steps) {
                val a = s / steps.toFloat() * TAU
                val rr = r + amplitude * (0.65f * sin(5f * a + phase + t / 900f) + 0.35f * water.jag * sin(11f * a - phase * 1.7f - t / 600f))
                val x = cx + cos(a) * rr
                val y = cy + sin(a) * rr
                if (s == 0) ripple.moveTo(x, y) else ripple.lineTo(x, y)
            }
            ripple.close()
            val a = water.alpha * (1f - u).pow(1.3f) * (u * 8f).coerceAtMost(1f) * intro
            stroke.color = white
            stroke.shader = LinearGradient(0f, fadeTop, 0f, fadeBottom, withAlpha(water.color, a), withAlpha(water.color, 0f), Shader.TileMode.CLAMP)
            canvas.drawPath(ripple, stroke)
            stroke.shader = null
        }
    }

    /** Glints on still water; wind over choppy; rain over a storm. */
    private fun weather(canvas: Canvas, art: RectF, cx: Float, cy: Float, t: Float, fadeTop: Float, fadeBottom: Float) {
        val on = ShareMotion.beat(t, 700, 900)
        when (share.band) {
            ScoreBand.STILL, ScoreBand.CALM -> {
                val count = if (share.band == ScoreBand.STILL) glints.size else 3
                glints.take(count).forEach { (angle, spot) ->
                    val r = 300f + spot * 300f
                    val tw = 0.5f + 0.5f * sin(t / 420f + angle * 3f)
                    val x = cx + cos(angle) * r
                    val y = cy + sin(angle) * r * 0.9f
                    kit.glow(canvas, x, y, 26f * tw, amber, 0.6f * on)
                    stroke.strokeWidth = 2.4f
                    stroke.color = withAlpha(cream, 0.85f * tw * on)
                    canvas.drawLine(x - 13f * tw, y, x + 13f * tw, y, stroke)
                    canvas.drawLine(x, y - 13f * tw, x, y + 13f * tw, stroke)
                }
            }
            // Choppy needs nothing extra: its rings are already ragged.
            ScoreBand.STEADY, ScoreBand.CHOPPY -> Unit
            ScoreBand.STORMY -> {
                stroke.strokeWidth = 2.2f
                drops.forEach { (x0, y0, speed) ->
                    val y = (y0 + t / 900f * speed) % 1f
                    val x = x0 * 1180f - 60f - y * 120f
                    val top = art.top - 60f + y * (fadeBottom - art.top + 60f)
                    val fade = ((fadeBottom - top) / (fadeBottom - fadeTop)).coerceIn(0f, 1f)
                    stroke.color = withAlpha(white, 0.16f * on * fade)
                    canvas.drawLine(x, top, x - 16f, top + 46f, stroke)
                }
            }
        }
    }

    /** Track, the score's arc in the score ramp (ember → amber → zen), ticks, and the number. */
    private fun dial(canvas: Canvas, cx: Float, cy: Float, t: Float) {
        val r = 208f
        val land = ShareMotion.pop(t, 120, 700, overshoot = 1.2f)
        if (land <= 0f) return
        canvas.save()
        canvas.scale(land, land, cx, cy)

        fill.color = if (light) withAlpha(paperWhite, 0.7f) else withAlpha(ink, 0.45f)
        canvas.drawCircle(cx, cy, r - 14f, fill)

        stroke.strokeWidth = 24f
        stroke.color = withAlpha(type, 0.09f)
        canvas.drawCircle(cx, cy, r, stroke)

        val sweep = 360f * share.tenths.coerceIn(0, ZenScore.MAX_TENTHS) / ZenScore.MAX_TENTHS * ShareMotion.beat(t, 280, 1_200)
        if (sweep > 0f) {
            // Opaque: a shader still takes the paint's alpha, and the track just left it at 9%.
            stroke.color = white
            stroke.shader = SweepGradient(cx, cy, intArrayOf(ember, ember, amber, zen300), floatArrayOf(0f, 0.02f, 0.46f, 1f)).apply {
                setLocalMatrix(Matrix().apply { setRotate(-96f, cx, cy) })
            }
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, sweep, false, stroke)
            stroke.shader = null
        }

        stroke.strokeWidth = 3f
        for (i in 0 until ZenScore.MAX_DISPLAY) {
            val a = -PI.toFloat() / 2f + i * TAU / ZenScore.MAX_DISPLAY
            val lit = i * 10 < share.tenths
            stroke.color = withAlpha(type, if (lit) 0.55f else 0.2f)
            canvas.drawLine(cx + cos(a) * 240f, cy + sin(a) * 240f, cx + cos(a) * 252f, cy + sin(a) * 252f, stroke)
        }
        canvas.restore()

        val roll = ShareMotion.beat(t, 300, 1_100)
        val shown = ZenScore.format((share.tenths * roll).toInt())
        numberPaint.color = withAlpha(type, ShareMotion.beat(t, 300, 240))
        canvas.drawText(shown, cx, cy + 46f, numberPaint)
        labelPaint.color = withAlpha(type, 0.55f * ShareMotion.beat(t, 500, 420))
        canvas.drawText("OUT OF 10", cx, cy + 100f, labelPaint)
    }

    private class Water(val rings: Int, val wobble: Float, val jag: Float, val color: Int, val alpha: Float, val period: Float)

    private companion object {
        val TAU = (2 * PI).toFloat()
    }
}
