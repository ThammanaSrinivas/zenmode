package com.zenlauncher.zenmode.share

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.Sfx
import com.zenlauncher.zenmode.recap.formatMinutes
import com.zenlauncher.zenmode.ui.components.ZenMotion
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

// ── The weekly story ──────────────────────────────────────────────
// "My week in Zen" as a 9:16 story that plays: header, then the headline word by word, the
// week's two numbers rolling up, then the chart — a promise line drawn across and seven bars
// rising one after another, each landing with its own note (up the pentatonic scale for a kept
// day, a muted knock for a missed one), so a perfect week plays a whole rising scale and a missed
// day breaks the tune. The verdict lands on its own sound; the takeaways slide in; it settles.
//
// Every beat runs on the app's motion tokens: arrivals decelerate on ZenMotion.EaseOut
// (emphasized decelerate), pops overshoot and settle, and beats are staggered so the eye lands
// on one thing at a time. The same timeline drives the in-app preview and the exported clip.

class WeeklyArt(private val kit: ShareKit, private val share: WeeklyShare) : ShareArt {
    override val format = ShareFormat.STORY
    override val introMs = INTRO_MS
    override val description = share.description

    private val days = share.recap.days

    override val cues: List<ShareCue> = buildList {
        days.forEachIndexed { i, day ->
            val at = barStart(i) + BAR_LAND
            if (day.keptPromise) add(ShareCue(at, Sfx.WEEK_TICK, Pentatonic.rising[i % 7], CueHaptic.TICK))
            else add(ShareCue(at, Sfx.WEEK_MISS, haptic = CueHaptic.TICK))
        }
        add(
            when (share.tier) {
                WeekTier.PERFECT -> ShareCue(VERDICT, Sfx.STREAK_LEGEND, haptic = CueHaptic.LAND)
                WeekTier.KEPT -> ShareCue(VERDICT, Sfx.SUCCESS, haptic = CueHaptic.LAND)
                WeekTier.MISSED -> ShareCue(VERDICT, Sfx.WEEK_RECOMMIT, haptic = CueHaptic.LAND)
            }
        )
    }

    // ── Palette ───────────────────────────────────────────────────

    private val white = kit.color(R.color.white)
    private val ink = kit.color(R.color.ink_base)
    private val inkSurface = kit.color(R.color.ink_surface)
    private val zen300 = kit.color(R.color.zen_300)
    private val zen500 = kit.color(R.color.zen_500)
    private val zen700 = kit.color(R.color.zen_700)
    private val ember300 = kit.color(R.color.ember_300)
    private val ember500 = kit.color(R.color.ember_500)
    private val amber = kit.color(R.color.amber_500)
    private val amberDeep = kit.color(R.color.amber_800)

    private val light = share.tier == WeekTier.PERFECT
    private val primary = if (light) inkSurface else white
    private val muted = if (light) kit.color(R.color.stone_600) else withAlpha(white, 0.7f)
    private val faint = if (light) withAlpha(kit.color(R.color.stone_500), 0.85f) else withAlpha(white, 0.42f)
    private val accent = when (share.tier) {
        WeekTier.PERFECT -> amberDeep
        WeekTier.KEPT -> zen300
        WeekTier.MISSED -> kit.color(R.color.recap_dusk_glow)
    }
    private val keptTop = if (light) zen500 else zen300
    private val keptBottom = if (light) zen700 else zen500

    // ── Type ──────────────────────────────────────────────────────

    private val stampPaint = kit.paint(kit.mono, 26f, muted, tracking = 0.14f, align = Paint.Align.RIGHT)
    private val eyebrowPaint = kit.paint(kit.mono, 28f, accent, tracking = 0.14f)
    private val headlinePaint = kit.paint(kit.clash, 128f, primary, tracking = -0.025f)
    private val headline = kit.layout(share.headline, headlinePaint, (W - M * 2).toInt(), lineSpacing = 0.96f, maxLines = 2)
    private val bigPaint = kit.paint(kit.mono, 108f, primary, tracking = -0.04f)
    private val bigRight = kit.paint(kit.mono, 108f, accent, tracking = -0.04f, align = Paint.Align.RIGHT)
    private val labelPaint = kit.paint(kit.mono, 24f, faint, tracking = 0.14f)
    private val labelRight = kit.paint(kit.mono, 24f, faint, tracking = 0.14f, align = Paint.Align.RIGHT)
    private val pillPaint = kit.paint(kit.geist, 34f, white, weight = 600)
    private val dayPaint = kit.paint(kit.mono, 28f, faint, tracking = 0.08f, align = Paint.Align.CENTER)
    private val promisePaint = kit.paint(kit.mono, 22f, faint, tracking = 0.12f, align = Paint.Align.RIGHT)
    private val valuePaint = kit.paint(kit.geist, 42f, primary, weight = 600)
    private val footerPaint = kit.paint(kit.mono, 24f, faint, tracking = 0.14f)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val dash = DashPathEffect(floatArrayOf(16f, 14f), 0f)

    /** The headline split into words with their laid-out positions, so each can rise on its own. */
    private val words: List<Word> = buildList {
        val text = share.headline
        for (line in 0 until headline.lineCount) {
            var i = headline.getLineStart(line)
            val end = headline.getLineEnd(line)
            while (i < end) {
                while (i < end && text[i] == ' ') i++
                if (i >= end) break
                var j = i
                while (j < end && text[j] != ' ') j++
                add(Word(text.substring(i, j), headline.getPrimaryHorizontal(i), headline.getLineBaseline(line).toFloat()))
                i = j
            }
        }
    }

    private val maxMinutes = max(days.maxOfOrNull { it.screenTimeMinutes } ?: 0L, share.recap.promiseHours * 60L).coerceAtLeast(1L).toFloat()
    private val letters = days.map { it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.US) }
    private val motes = Random(share.recap.weekStart.toEpochDay().toInt()).let { rnd ->
        List(18) { floatArrayOf(rnd.nextFloat(), rnd.nextFloat(), 0.5f + rnd.nextFloat(), rnd.nextFloat() * TAU) }
    }

    override fun draw(canvas: Canvas, timeMs: Float) {
        val t = timeMs
        ground(canvas, t)
        header(canvas, t)
        title(canvas, t)
        numbers(canvas, t)
        chart(canvas, t)
        highlights(canvas, t)
        if (share.tier == WeekTier.PERFECT) celebration(canvas, t)
        kit.grain(canvas, format, if (light) 0.05f else 0.07f)
        val f = ShareMotion.beat(t, FOOTER, 480)
        footerPaint.color = withAlpha(faint, f)
        footerPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(PosterChrome.SITE, M, 1846f, footerPaint)
        footerPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("TRACKED ON THIS PHONE", W - M, 1846f, footerPaint)
    }

    private fun ground(canvas: Canvas, t: Float) {
        when (share.tier) {
            WeekTier.PERFECT -> kit.wash(
                canvas, format,
                0f to kit.color(R.color.recap_gold_light),
                0.55f to kit.color(R.color.amber_on),
                1f to kit.color(R.color.paper_raised)
            )
            WeekTier.KEPT -> kit.wash(canvas, format, 0f to kit.color(R.color.onboarding_night), 0.6f to kit.color(R.color.ink_gain), 1f to ink)
            WeekTier.MISSED -> kit.wash(canvas, format, 0f to kit.color(R.color.recap_dusk), 1f to ink)
        }
        val open = ShareMotion.beat(t, 0, 1_200)
        val drift = sin(t / 2_400f) * 40f
        val glow = when (share.tier) {
            WeekTier.PERFECT -> kit.color(R.color.recap_gold_deep)
            WeekTier.KEPT -> kit.color(R.color.os_grad_glow)
            WeekTier.MISSED -> kit.color(R.color.recap_dusk_glow)
        }
        kit.glow(canvas, W * 0.82f + drift, 420f, 760f * (0.6f + 0.4f * open), glow, 0.22f * open)
        kit.glow(canvas, W * 0.2f - drift, 1500f, 700f, glow, 0.1f * open)
    }

    private fun header(canvas: Canvas, t: Float) {
        val p = ShareMotion.beat(t, 120, 560)
        canvas.save()
        canvas.translate(0f, -28f * (1f - p))
        kit.drawWordmark(canvas, M, 156f, 44f, primary, alpha = p)
        stampPaint.color = withAlpha(muted, p)
        canvas.drawText("WEEKLY REPORT", W - M, 152f, stampPaint)
        canvas.restore()
    }

    private fun title(canvas: Canvas, t: Float) {
        val e = ShareMotion.beat(t, 300, 480)
        canvas.save()
        canvas.translate(-24f * (1f - e), 0f)
        eyebrowPaint.color = withAlpha(accent, e)
        canvas.drawText(share.eyebrow, M, 318f, eyebrowPaint)
        canvas.restore()

        words.forEachIndexed { i, word ->
            val p = ShareMotion.beat(t, 460 + i * 90, 620)
            if (p <= 0f) return@forEachIndexed
            canvas.save()
            canvas.translate(M + word.x, 352f + 60f * (1f - p))
            headlinePaint.color = withAlpha(primary, p)
            canvas.drawText(word.text, 0f, word.baseline, headlinePaint)
            canvas.restore()
        }
    }

    /** The week's total on the left, days kept on the right, both rolling up; the change pill under. */
    private fun numbers(canvas: Canvas, t: Float) {
        val baseline = 790f
        val left = ShareMotion.beat(t, NUMBERS, 1_200)
        if (left > 0f) {
            bigPaint.color = withAlpha(primary, ShareMotion.beat(t, NUMBERS, 260))
            canvas.drawText(formatMinutes((share.recap.totalMinutes * left).toLong()), M, baseline, bigPaint)
            labelPaint.color = withAlpha(faint, ShareMotion.beat(t, NUMBERS + 160, 420))
            canvas.drawText("ON MY PHONE THIS WEEK", M, baseline + 50f, labelPaint)
        }
        val right = ShareMotion.beat(t, NUMBERS + 160, 1_000)
        if (right > 0f) {
            bigRight.color = withAlpha(accent, ShareMotion.beat(t, NUMBERS + 160, 260))
            val pulse = 1f + 0.12f * (ShareMotion.pop(t, VERDICT, 520) - ShareMotion.beat(t, VERDICT + 200, 600)).coerceAtLeast(0f)
            canvas.save()
            canvas.scale(pulse, pulse, W - M, baseline - 40f)
            canvas.drawText("${(share.recap.daysKept * right).toInt()}/${days.size}", W - M, baseline, bigRight)
            canvas.restore()
            labelRight.color = withAlpha(faint, ShareMotion.beat(t, NUMBERS + 300, 420))
            canvas.drawText(share.keptLabel, W - M, baseline + 50f, labelRight)
        }

        val label = share.changeLabel ?: return
        val pill = ShareMotion.pop(t, PILL, 520)
        if (pill <= 0f) return
        val tint = if (share.changeBetter) (if (light) zen700 else zen300) else (if (light) ember500 else ember300)
        val width = pillPaint.measureText(label) + 56f
        val top = 876f
        canvas.save()
        canvas.scale(pill, pill, M, top + 34f)
        fill.color = withAlpha(tint, if (light) 0.14f else 0.18f)
        canvas.drawRoundRect(M, top, M + width, top + 68f, 34f, 34f, fill)
        pillPaint.color = tint
        canvas.drawText(label, M + 28f, top + 46f, pillPaint)
        canvas.restore()
    }

    /** Seven bars against a dashed promise line, rising one after another, each crowned on landing. */
    private fun chart(canvas: Canvas, t: Float) {
        val base = 1430f
        val height = 400f
        val slot = (W - M * 2) / days.size
        val barW = slot * 0.62f

        val axis = ShareMotion.beat(t, CHART, 420)
        stroke.pathEffect = null
        stroke.strokeWidth = 2f
        stroke.color = withAlpha(faint, 0.7f * axis)
        canvas.drawLine(M, base, M + (W - M * 2) * axis, base, stroke)

        days.forEachIndexed { i, day ->
            val cx = M + slot * (i + 0.5f)
            val rise = ShareMotion.beat(t, barStart(i), BAR_RISE)
            if (rise > 0f) {
                val h = max(12f, height * day.screenTimeMinutes / maxMinutes) * rise
                val top = base - h
                shade.shader = LinearGradient(
                    0f, top, 0f, base,
                    if (day.keptPromise) intArrayOf(keptTop, keptBottom) else intArrayOf(ember300, ember500),
                    null, Shader.TileMode.CLAMP
                )
                path.reset()
                path.addRoundRect(RectF(cx - barW / 2, top, cx + barW / 2, base), floatArrayOf(20f, 20f, 20f, 20f, 4f, 4f, 4f, 4f), Path.Direction.CW)
                canvas.drawPath(path, shade)
                shade.shader = null
                dayPaint.color = withAlpha(if (i == days.lastIndex) accent else faint, rise)
                canvas.drawText(letters[i], cx, base + 52f, dayPaint)

                val crown = ShareMotion.pop(t, barStart(i) + BAR_LAND, 400)
                if (crown > 0f) mark(canvas, cx, top - 40f, 22f * crown, day.keptPromise)
            }
        }

        // The promise, drawn across left to right as the bars begin.
        val line = ShareMotion.beat(t, CHART + 150, 900, ZenMotion.EaseOut)
        if (line > 0f) {
            val y = base - height * share.recap.promiseHours * 60f / maxMinutes
            stroke.pathEffect = dash
            stroke.strokeWidth = 3f
            stroke.color = withAlpha(primary, 0.55f)
            canvas.drawLine(M, y, M + (W - M * 2) * line, y, stroke)
            stroke.pathEffect = null
        }
        // Its legend sits above the chart, clear of every bar's crown wherever the line falls.
        val legend = ShareMotion.beat(t, CHART + 600, 480)
        if (legend > 0f) {
            val y = base - height - 84f
            val label = "PROMISE ${share.recap.promiseHours}H"
            val textX = W - M
            val lineEnd = textX - promisePaint.measureText(label) - 18f
            stroke.pathEffect = dash
            stroke.strokeWidth = 3f
            stroke.color = withAlpha(primary, 0.55f * legend)
            canvas.drawLine(lineEnd - 64f, y - 8f, lineEnd, y - 8f, stroke)
            stroke.pathEffect = null
            promisePaint.color = withAlpha(faint, legend)
            canvas.drawText(label, textX, y, promisePaint)
        }
    }

    /** A kept day's tick or a missed day's cross, in a little disc above its bar. */
    private fun mark(canvas: Canvas, cx: Float, cy: Float, r: Float, kept: Boolean) {
        if (kept) {
            fill.color = keptTop
            canvas.drawCircle(cx, cy, r, fill)
            stroke.strokeWidth = r * 0.2f
            stroke.color = if (light) white else ink
            path.reset()
            path.moveTo(cx - r * 0.42f, cy + r * 0.02f)
            path.lineTo(cx - r * 0.1f, cy + r * 0.34f)
            path.lineTo(cx + r * 0.46f, cy - r * 0.3f)
            canvas.drawPath(path, stroke)
        } else {
            stroke.strokeWidth = r * 0.16f
            stroke.color = ember300
            canvas.drawCircle(cx, cy, r * 0.9f, stroke)
            val k = r * 0.34f
            canvas.drawLine(cx - k, cy - k, cx + k, cy + k, stroke)
            canvas.drawLine(cx - k, cy + k, cx + k, cy - k, stroke)
        }
    }

    private fun highlights(canvas: Canvas, t: Float) {
        share.highlights.forEachIndexed { i, item ->
            val p = ShareMotion.beat(t, HIGHLIGHTS + i * 170, 620)
            if (p <= 0f) return@forEachIndexed
            val top = 1560f + i * 130f
            canvas.save()
            canvas.translate(-40f * (1f - p), 0f)
            labelPaint.color = withAlpha(faint, p)
            canvas.drawText(item.label, M, top, labelPaint)
            valuePaint.color = withAlpha(primary, p)
            canvas.drawText(item.value, M, top + 54f, valuePaint)
            canvas.restore()
        }
    }

    /** A perfect week ends in a slow lift of gold motes. */
    private fun celebration(canvas: Canvas, t: Float) {
        val on = ShareMotion.beat(t, VERDICT, 900)
        if (on <= 0f) return
        motes.forEach { (x0, y0, speed, phase) ->
            val u = (y0 + (t - VERDICT) / 7_000f * speed) % 1f
            val x = M + x0 * (W - M * 2) + sin(t / 700f + phase) * 18f
            val y = 1500f - u * 1300f
            val a = (1f - u) * on
            kit.glow(canvas, x, y, 26f, amber, 0.55f * a)
            fill.color = withAlpha(amber, 0.9f * a)
            canvas.drawCircle(x, y, 4f, fill)
        }
    }

    private class Word(val text: String, val x: Float, val baseline: Float)

    companion object {
        private const val W = 1080f
        private const val M = 84f
        private val TAU = (2 * PI).toFloat()

        // Timeline, in ms.
        const val NUMBERS = 1_050
        const val PILL = 1_900
        const val CHART = 2_200
        const val BAR_RISE = 520
        const val BAR_LAND = 400
        const val BAR_STAGGER = 230
        const val VERDICT = 4_300
        const val HIGHLIGHTS = 4_700
        const val FOOTER = 5_300
        const val INTRO_MS = 6_000

        fun barStart(i: Int) = CHART + 260 + i * BAR_STAGGER
    }
}
