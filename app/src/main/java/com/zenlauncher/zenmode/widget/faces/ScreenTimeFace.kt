package com.zenlauncher.zenmode.widget.faces

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.zenlauncher.zenmode.coreapi.week.MomentCopy
import com.zenlauncher.zenmode.recap.formatMinutes
import com.zenlauncher.zenmode.widget.art.Surface
import com.zenlauncher.zenmode.widget.art.WidgetKit
import com.zenlauncher.zenmode.widget.art.WidgetLight
import android.graphics.LinearGradient
import android.graphics.Shader
import com.zenlauncher.zenmode.share.withAlpha
import java.time.format.TextStyle
import java.util.Locale

/**
 * **Screen time**: today's time on this phone against the promise, measured by the phone itself.
 * 2 × 1 to 4 × 4, each step adding detail:
 *
 *  · 2 × 1: the total, and the day on one bar whose track is as long as the promise;
 *  · 2 × 2: the total, and the day as ground rising toward the promise line;
 *  · 4 × 1: the total, the bar, and the time left;
 *  · 4 × 2: the promise ring, the day in a few words, the Zen Score;
 *  · 4 × 3 and up: the last seven days, a column each, the promise as a line.
 *
 * Colour says one thing: green inside the promise, ember for whatever is past it. Every stretch
 * of time is drawn in a [Texture], solid for this phone, so a second screen can join the same bar,
 * ring and columns without a new picture. On the lock screen the minutes stay hidden.
 */
object ScreenTimeFace : Face {

    /** How one device's time is laid down: solid, dashes, or dots. Never told apart by colour. */
    enum class Texture { SOLID, DASH, DOT }

    override fun draw(scope: FaceScope): FaceResult = with(scope) {
        val surface = data.surfaceFor(input.dark, followsQuiet = false)
        when {
            input.private -> hidden(this, surface)
            !data.usageAccess -> off(this, surface)
            rows == 1 && !wide -> small(this, surface)
            rows == 1 -> oneRow(this, surface)
            !wide -> square(this, surface)
            else -> wideFace(this, surface)
        }
        result(spoken(this), if (data.usageAccess) WidgetAction.OPEN_TODAY else WidgetAction.OPEN_USAGE_ACCESS)
    }

    private val label = "Screen time"

    private fun used(s: FaceScope) = s.data.minutesToday
    private fun promise(s: FaceScope) = s.data.promiseMinutes
    private fun over(s: FaceScope) = used(s) > promise(s)
    private fun totalColor(s: FaceScope) = if (over(s)) s.t.emberText else s.t.text
    private fun green(s: FaceScope) = if (s.surface == Surface.PAPER || s.surface == Surface.GOLD) s.t.brandFill else s.t.brand
    private fun track(s: FaceScope) = if (s.surface == Surface.PAPER) s.t.sunk else s.t.outline
    private fun ember(s: FaceScope) = com.zenlauncher.zenmode.ui.theme.WidgetPalette.flame

    /** "On track.", "20 min left.", "38 min over." */
    private fun headline(s: FaceScope): String {
        val left = s.data.minutesLeft
        return when {
            left < 0 -> "${formatMinutes(-left)} over."
            left < 60 -> "${formatMinutes(left)} left."
            else -> "On track."
        }
    }

    private fun note(s: FaceScope): String {
        val left = s.data.minutesLeft
        return when {
            left < 0 -> "Tomorrow starts at zero"
            left < 60 -> "Close to your promise"
            else -> "${formatMinutes(left)} to spare"
        }
    }

    private fun spoken(s: FaceScope): String = with(s) {
        when {
            input.private -> "Screen time. Unlock to see today's time."
            !data.usageAccess -> "Screen time. Usage access is off; tap to turn it on."
            else -> "Screen time today, ${formatMinutes(used(this))} of your ${data.promiseHours}-hour promise. ${note(this)}."
        }
    }

    // ── Sizes ───────────────────────────────────────────────────────

    private fun small(s: FaceScope, surface: Surface) = with(s) {
        ground(surface, lit(14f + 40f, 52f, 120f, hue = hue(this), celebration = false))
        kit.header(canvas, 14f, 12f, label, t.brand, w - 50f)
        kit.icon(canvas, WidgetKit.Icon.PHONE, w - 14f - 14f, 10f, 14f, t.text2, 1.9f)
        kit.text(canvas, formatMinutes(used(this)), 14f, 32f, WidgetKit.metricL, totalColor(this), w - 28f)
        bar(this, 14f, h - 10f - 9f, w - 28f, 6f)
    }

    private fun oneRow(s: FaceScope, surface: Surface) = with(s) {
        ground(surface, lit(16f + 50f, h / 2, 140f, hue = hue(this), celebration = false))
        val colW = 128f
        kit.header(canvas, 16f, h / 2 - 24f, label, t.brand, colW)
        kit.text(canvas, formatMinutes(used(this)), 16f, h / 2 - 8f, WidgetKit.metricL, totalColor(this), colW)
        val left = 16f + colW + 16f
        val barW = w - 16f - left
        bar(this, left, h / 2 - 12f, barW, 6f)
        if (w >= 330f) {
            kit.icon(canvas, WidgetKit.Icon.PHONE, left, h / 2 + 4f, 13f, t.text2, 1.9f)
            kit.text(canvas, formatMinutes(used(this)), left + 17f, h / 2 + 3f, WidgetKit.Style(WidgetKit.Face.MONO, 11f, 14f), t.text)
        }
        val short = if (over(this)) "${formatMinutes(-data.minutesLeft)} over" else "${formatMinutes(data.minutesLeft)} left"
        kit.text(canvas, short, w - 16f, h / 2 + 3f, WidgetKit.monoS.copy(caps = false, tracking = 0.02f), if (over(this)) t.emberText else t.mono, barW, Paint.Align.RIGHT)
    }

    private fun square(s: FaceScope, surface: Surface) = with(s) {
        ground(surface, lit(w * 0.3f, 54f, w * 0.8f, hue = hue(this), celebration = false, mood = WidgetLight.Mood.NONE))
        level(this, h * 0.55f)
        kit.header(canvas, 14f, 14f, label, t.brand, w - 70f)
        scoreChip(this, w - 14f, 11f)
        kit.text(canvas, formatMinutes(used(this)), 14f, 36f, WidgetKit.metricL, totalColor(this), w - 28f)
        kit.text(canvas, note(this), 14f, 72f, WidgetKit.caption, t.muted, w - 28f)
    }

    private fun wideFace(s: FaceScope, surface: Surface) = with(s) {
        val tall = rows >= 3
        val block = if (tall) 216f else h
        val ring = minOf(148f, block - 32f, w * 0.4f)
        ground(surface, lit(16f + ring / 2, block / 2, ring * 1.1f, hue = hue(this), celebration = false))
        ring(this, 16f, (block - ring) / 2, ring, 12f)
        val left = 16f + ring + 16f
        val colW = w - 16f - left
        var y = (block - 128f) / 2
        kit.header(canvas, left, y, "$label · today", t.brand, colW)
        y += 18f
        val score = data.todayScore?.let(MomentCopy::formatTenths)
        val scoreW = if (score != null) kit.text(canvas, score, w - 16f, y + 2f, WidgetKit.metric, if (data.mood == WidgetLight.Mood.CALM) t.brand else t.emberText, align = Paint.Align.RIGHT) else 0f
        kit.text(canvas, headline(this), left, y, WidgetKit.title22, t.text, colW - scoreW - 8f)
        y += 38f
        kit.icon(canvas, WidgetKit.Icon.PHONE, left, y - 1f, 16f, t.text2, 1.8f)
        val rowBarW = colW - 22f - 54f
        stretch(canvas, Texture.SOLID, left + 22f, y + 5f, rowBarW, 6f, track(this), green(this), ember(this), used(this), promise(this), showsOver = false)
        kit.text(canvas, formatMinutes(used(this)), w - 16f, y, WidgetKit.Style(WidgetKit.Face.MONO, 11f, 14f), t.text, align = Paint.Align.RIGHT)
        y += 26f
        kit.paragraph(canvas, note(this), left, y, colW, WidgetKit.caption, t.muted, maxLines = 2)
        if (tall) lastSeven(this, block)
    }

    // ── Pieces ──────────────────────────────────────────────────────

    private fun scoreChip(s: FaceScope, right: Float, y: Float) = with(s) {
        val score = data.todayScore ?: return@with
        val calm = data.mood == WidgetLight.Mood.CALM
        kit.tag(canvas, MomentCopy.formatTenths(score), right, y, if (calm) t.brandTint else t.emberTint, if (calm) t.brand else t.emberText)
    }

    /** The whole day on one bar: the track as long as the promise, past it in ember. */
    private fun bar(s: FaceScope, x: Float, y: Float, w: Float, h: Float) = with(s) {
        stretch(canvas, Texture.SOLID, x, y, w, h, track(this), green(this), ember(this), used(this), promise(this), showsOver = true, mark = t.text)
    }

    /**
     * One stretch of time in its [texture], on a track whose span is the promise or the day,
     * whichever is longer. Inside the promise it is green; past it, ember.
     */
    private fun stretch(
        canvas: Canvas, texture: Texture, x: Float, y: Float, w: Float, h: Float, trackColor: Int,
        greenColor: Int, emberColor: Int, minutes: Long, promise: Long, showsOver: Boolean, mark: Int = 0
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = trackColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), h / 2, h / 2, p)
        val span = maxOf(promise, if (showsOver) minutes else promise).toFloat()
        val lim = x + w * promise / span
        val end = x + w * minOf(minutes.toFloat(), span) / span
        lay(canvas, texture, x, minOf(end, lim) - if (end > lim) 1.5f else 0f, y, h, greenColor, p)
        if (end > lim) {
            lay(canvas, texture, lim + 1.5f, end, y, h, emberColor, p)
            p.color = mark
            canvas.drawRoundRect(RectF(lim - 1, y - 3, lim + 1, y + h + 3), 1f, 1f, p)
        }
    }

    private fun lay(canvas: Canvas, texture: Texture, a: Float, b: Float, y: Float, h: Float, color: Int, p: Paint) {
        if (b - a < 1.2f) return
        p.color = color
        when (texture) {
            Texture.SOLID -> canvas.drawRoundRect(RectF(a, y, b, y + h), minOf(h / 2, (b - a) / 2), minOf(h / 2, (b - a) / 2), p)
            Texture.DASH -> {
                var x = a
                while (x < b - 1.5f) {
                    canvas.drawRoundRect(RectF(x, y, minOf(x + 5f, b), y + h), 1.5f, 1.5f, p)
                    x += 8f
                }
            }
            Texture.DOT -> {
                var x = a + 1.7f
                while (x < b - 0.5f) {
                    canvas.drawCircle(x, y + h / 2, 1.7f, p)
                    x += 5.6f
                }
            }
        }
    }

    /** The day's light: green when calm, amber when it drifts, ember once over the promise. */
    private fun hue(s: FaceScope): WidgetLight.Hue = when (s.data.mood) {
        WidgetLight.Mood.CALM -> WidgetLight.Hue.ZEN
        WidgetLight.Mood.DRIFT -> WidgetLight.Hue.AMBER
        WidgetLight.Mood.OVER -> WidgetLight.Hue.EMBER
        WidgetLight.Mood.NONE -> WidgetLight.Hue.DUSK
    }

    /**
     * The day as a level: soft green light rising from the floor toward the promise line, its
     * surface a bright edge; whatever rises past the line glows ember.
     */
    private fun level(s: FaceScope, height: Float) = with(s) {
        val span = maxOf(promise(this), used(this)).toFloat()
        val lim = height * promise(this) / span
        val top = height * used(this) / span
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val inside = minOf(top, lim)
        if (inside > 0f) {
            val y = h - inside
            p.shader = LinearGradient(0f, y, 0f, h, withAlpha(green(this), 0.42f), withAlpha(green(this), 0.12f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, y, w, h, p)
            p.shader = null
            p.color = withAlpha(green(this), 0.9f)
            canvas.drawRect(0f, y, w, y + 1.5f, p)
        }
        if (top > lim) {
            p.shader = LinearGradient(0f, h - top, 0f, h - lim, withAlpha(ember(this), 0.5f), withAlpha(ember(this), 0.2f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, h - top, w, h - lim, p)
            p.shader = null
            p.color = withAlpha(ember(this), 0.95f)
            canvas.drawRect(0f, h - top, w, h - top + 1.5f, p)
        }
        val ly = h - lim
        p.color = t.outline
        var x = 0f
        while (x < w) {
            canvas.drawRoundRect(RectF(x, ly - 0.75f, x + 5f, ly + 0.75f), 0.75f, 0.75f, p)
            x += 9f
        }
        val tag = "${data.promiseHours}h promise"
        if (!over(this)) {
            kit.text(canvas, tag, w - 12f, ly + 5f, WidgetKit.monoS, t.mono, align = Paint.Align.RIGHT)
        } else {
            val tw = kit.width("${data.promiseHours}h", WidgetKit.monoS) + 14f
            kit.solid(canvas, RectF(w - 12f - tw, ly - 9f, w - 12f, ly + 9f), 6f, t.surface)
            kit.text(canvas, "${data.promiseHours}h", w - 19f, ly - 7f, WidgetKit.monoS, t.emberText, align = Paint.Align.RIGHT)
        }
        if (top >= 22f) {
            val chipY = h - inside / 2 - 9f
            val chip = formatMinutes(used(this))
            val cw = kit.width(chip, WidgetKit.Style(WidgetKit.Face.MONO, 11f, 14f)) + 26f
            kit.solid(canvas, RectF(12f, chipY, 12f + cw, chipY + 18f), 6f, t.surface)
            kit.icon(canvas, WidgetKit.Icon.PHONE, 16f, chipY + 3f, 12f, t.text, 2f)
            kit.text(canvas, chip, 31f, chipY + 2f, WidgetKit.Style(WidgetKit.Face.MONO, 11f, 14f), t.text)
        }
    }

    /** The promise ring: a track and one arc from the top; past the promise the arc turns ember. */
    private fun ring(s: FaceScope, x: Float, y: Float, size: Float, sw: Float) = with(s) {
        val r = size / 2 - sw / 2 - 1
        val cx = x + size / 2
        val cy = y + size / 2
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = sw }
        p.color = track(this)
        canvas.drawCircle(cx, cy, r, p)
        val span = maxOf(promise(this), used(this)).toFloat()
        val lim = 359f * promise(this) / span
        val end = 359f * used(this) / span
        p.strokeCap = Paint.Cap.ROUND
        val cap = Math.toDegrees((sw / 2 / r).toDouble()).toFloat()
        if (end > 1f) {
            p.color = green(this)
            val e = minOf(end, lim) - (if (end > lim) 2f else 0f)
            canvas.drawArc(oval, -90f + cap, maxOf(0.5f, e - 2 * cap), false, p)
        }
        if (end > lim) {
            p.color = ember(this)
            canvas.drawArc(oval, -90f + lim + 2f + cap, maxOf(0.5f, end - lim - 3f - 2 * cap), false, p)
            p.color = t.text
            p.strokeWidth = 2f
            val a = Math.toRadians((lim - 90f).toDouble())
            val c = kotlin.math.cos(a).toFloat()
            val sn = kotlin.math.sin(a).toFloat()
            canvas.drawLine(cx + (r - sw / 2 - 2) * c, cy + (r - sw / 2 - 2) * sn, cx + (r + sw / 2 + 2) * c, cy + (r + sw / 2 + 2) * sn, p)
        }
        val big = size >= 110f
        val total = WidgetKit.Style(WidgetKit.Face.MONO, if (big) 16f else 13f, if (big) 22f else 18f)
        kit.text(canvas, formatMinutes(used(this)), cx, cy - (total.line + 14f) / 2, total, t.text, align = Paint.Align.CENTER)
        kit.text(canvas, "of ${data.promiseHours}h", cx, cy - (total.line + 14f) / 2 + total.line, WidgetKit.monoS.copy(caps = false), t.mono, align = Paint.Align.CENTER)
    }

    /** The last seven days, a column each, the promise as a line, today last. */
    private fun lastSeven(s: FaceScope, top: Float) = with(s) {
        val p = Paint()
        p.color = t.hair
        canvas.drawRect(16f, top + 6f, w - 16f, top + 7f, p)
        kit.text(canvas, "Last 7 days", 16f, top + 20f, WidgetKit.monoS, t.mono)
        val days = data.lastSevenMinutes
        val known = days.filterNotNull()
        val inside = days.count { it != null && it <= promise(this) }
        val chartTop = top + 46f
        val chartH = h - chartTop - 13f - 20f - 30f
        val span = maxOf(promise(this) * 1.25f, (known.maxOrNull() ?: 0L).toFloat())
        val cw = minOf(30f, (w - 32f) / 7 - 8f)
        val step = (w - 32f - cw) / 6
        val ly = chartTop + chartH - chartH * promise(this) / span
        days.forEachIndexed { i, m ->
            val x = 16f + i * step
            val date = data.today.minusDays((6 - i).toLong())
            val letter = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            kit.text(canvas, letter, x + cw / 2, chartTop + chartH + 6f, WidgetKit.monoS, if (i == 6) t.text else t.mono, align = Paint.Align.CENTER)
            if (m == null) {
                kit.outline(canvas, RectF(x, chartTop + chartH - 6f, x + cw, chartTop + chartH), 3f, t.hair, 1f)
                return@forEachIndexed
            }
            val hIn = chartH * minOf(m.toFloat(), promise(this).toFloat()) / span
            column(canvas, RectF(x, chartTop + chartH - hIn, x + cw, chartTop + chartH), green(this), i == 6)
            if (m > promise(this)) {
                val hOut = chartH * (m - promise(this)) / span
                column(canvas, RectF(x, chartTop + chartH - hIn - 2f - hOut, x + cw, chartTop + chartH - hIn - 2f), ember(this), true)
            }
        }
        p.color = t.outline
        var x = 16f
        while (x < w - 16f) {
            canvas.drawRoundRect(RectF(x, ly - 0.75f, x + 5f, ly + 0.75f), 0.75f, 0.75f, p)
            x += 9f
        }
        kit.text(canvas, "${data.promiseHours}h", w - 16f, ly - 16f, WidgetKit.monoS, t.mono, align = Paint.Align.RIGHT)
        val lead = "$inside of ${days.count { it != null }} days"
        kit.leadParagraph(canvas, lead, " inside your promise", 16f, h - 13f - 20f, w * 0.65f, WidgetKit.callout, t.text, t.text2, maxLines = 1)
        if (known.isNotEmpty()) {
            kit.text(canvas, "avg ${formatMinutes(known.sum() / known.size)}", w - 16f, h - 13f - 17f, WidgetKit.monoS.copy(caps = false, tracking = 0.02f), t.mono, align = Paint.Align.RIGHT)
        }
    }

    private fun hidden(s: FaceScope, surface: Surface) = with(s) {
        ground(surface, lit(w / 2, h, maxOf(w, h) * 0.6f, hue = WidgetLight.Hue.DUSK, celebration = false, mood = WidgetLight.Mood.NONE))
        kit.header(canvas, 14f, 14f, label, t.brand, w - 28f)
        kit.paragraph(canvas, "Unlock to see today's time.", 14f, 38f, w - 28f, WidgetKit.says, t.text, maxLines = 2)
    }

    private fun off(s: FaceScope, surface: Surface) = with(s) {
        ground(surface, lit(w / 2, h, maxOf(w, h) * 0.6f, hue = WidgetLight.Hue.DUSK, celebration = false, mood = WidgetLight.Mood.NONE))
        kit.header(canvas, 14f, 14f, label, t.brand, w - 50f)
        kit.icon(canvas, WidgetKit.Icon.LOCK, w - 28f, 13f, 14f, t.mono, 2f)
        kit.paragraph(canvas, MomentCopy.USAGE_ACCESS_OFF, 14f, 38f, w - 28f, WidgetKit.says, t.text, maxLines = 3)
    }

    /** One day's column: light at the top, settling toward the floor; today's the strongest. */
    private fun column(canvas: Canvas, r: RectF, color: Int, strong: Boolean) {
        if (r.height() <= 0f) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val top = if (strong) 1f else 0.85f
        p.shader = LinearGradient(0f, r.top, 0f, r.bottom, withAlpha(color, top), withAlpha(color, top * 0.55f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(r, minOf(6f, r.width() / 2), minOf(6f, r.width() / 2), p)
    }
}
