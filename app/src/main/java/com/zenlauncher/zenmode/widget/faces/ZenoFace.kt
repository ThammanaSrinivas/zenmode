package com.zenlauncher.zenmode.widget.faces

import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import com.zenlauncher.zenmode.coreapi.week.DayMark
import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.coreapi.week.MomentCopy
import com.zenlauncher.zenmode.coreapi.week.MomentFeeling
import com.zenlauncher.zenmode.widget.art.Surface
import com.zenlauncher.zenmode.widget.art.WidgetKit
import com.zenlauncher.zenmode.widget.art.WidgetLight
import com.zenlauncher.zenmode.widget.art.ZenoPainter
import com.zenlauncher.zenmode.widget.art.ZenoSkin

/**
 * **Zeno**: one kind word at the right moment. The widget that is never the same twice: it
 * shows whatever matters now, in this order:
 *
 *  1. a Quiet session, counting down, on night;
 *  2. usage access off: the one card that asks;
 *  3. the last hour of the promise;
 *  4. the week's moment, in a line that changes every day and with every poke.
 *
 * Composed as on iPhone: the week on top, Zeno in the middle in the moment's light, his line
 * beneath him. After 21:30 he moves to the night stage. Tap Zeno and he answers, in place.
 */
object ZenoFace : Face {

    override fun draw(scope: FaceScope): FaceResult = with(scope) {
        val d = data
        when {
            d.quiet.running != null -> quiet(this)
            !d.usageAccess -> locked(this)
            rows == 1 -> oneRow(this)
            wide -> wideFace(this)
            else -> square(this)
        }
    }

    private fun spoken(scope: FaceScope): String = with(scope) {
        if (data.answering || data.lastHour) data.zenoLine + " " + MomentCopy.progress(data.moment) + "."
        else MomentCopy.spoken(data.moment, MomentCopy.daySeed(data.today) + data.pokes)
    }

    private fun skinOf(scope: FaceScope, feeling: MomentFeeling): ZenoSkin =
        if (scope.data.lastHour && !scope.data.answering) ZenoSkin.EMBER else ZenoPainter.skinFor(feeling)

    private fun hueOf(scope: FaceScope): WidgetLight.Hue =
        if (scope.data.lastHour && !scope.data.answering) WidgetLight.Hue.EMBER else scope.momentHue

    /** Zeno standing in his light, with a soft shadow at his feet. Returns his box. */
    private fun zeno(s: FaceScope, x: Float, y: Float, zw: Float): RectF = with(s) {
        val zh = zw * ZenoPainter.ASPECT
        val feeling = data.zenoFeeling
        kit.softShadow(canvas, x + zw / 2, y + zh * 0.95f, zw * 0.42f, zw * 0.07f, t.text, if (surface.isDark) 0.35f else 0.14f)
        ZenoPainter.draw(canvas, x, y, zw, feeling, skinOf(this, feeling))
        val box = RectF(x, y, x + zw, y + zh)
        hit(box, WidgetAction.POKE)
        box
    }

    /** Two rows and up, two columns: the week, Zeno, his line. */
    private fun square(s: FaceScope): FaceResult = with(s) {
        val m = data.moment
        val surface = data.surfaceFor(input.dark, followsNight = true)
        val pad = if (narrow) 12f else 14f
        val style = WidgetKit.says.copy(size = if (narrow) 14f else 15f, line = 19f)
        val lineH = kit.layout(data.zenoLine, style, surface.tokens.text, w - pad * 2, 3, Layout.Alignment.ALIGN_CENTER).height.toFloat()
        val top = 34f
        val room = h - pad - lineH - 10f - top
        val zw = minOf(w - pad * 2 - 12f, room / ZenoPainter.ASPECT, 210f)
        val zh = zw * ZenoPainter.ASPECT
        val zx = (w - zw) / 2
        val zy = top + (room - zh) / 2
        ground(surface, lit(w / 2, zy + zh * 0.55f, zw * 1.05f, hue = hueOf(this), starBox = WidgetLight.Box(pad, top, w - pad, zy + zh * 0.4f)))

        val cell = if (narrow) 6f else 8f
        kit.cells(canvas, m.marks, pad, 15f, cell, if (narrow) 2f else 3f, t)
        kit.text(canvas, m.compactValue, w - pad, 11f, WidgetKit.metricS, accentText(this), align = Paint.Align.RIGHT)
        zeno(this, zx, zy, zw)
        kit.paragraph(canvas, data.zenoLine, pad, h - pad - lineH, w - pad * 2, style, t.text, maxLines = 3, align = Layout.Alignment.ALIGN_CENTER)
        result(spoken(this), WidgetAction.OPEN_TODAY)
    }

    /** Two rows, four columns: Zeno at the left in his light, his line set large beside him. */
    private fun wideFace(s: FaceScope): FaceResult = with(s) {
        val m = data.moment
        val surface = data.surfaceFor(input.dark, followsNight = true)
        val zw = minOf(h - 52f, w * 0.34f, 150f)
        val zh = zw * ZenoPainter.ASPECT
        val zx = 18f
        val zy = (h - zh) / 2 + 4f
        ground(surface, lit(zx + zw / 2, zy + zh * 0.55f, zw * 1.15f, hue = hueOf(this), starBox = WidgetLight.Box(zx + zw + 10f, 14f, w - 14f, 60f)))
        zeno(this, zx, zy, zw)

        val left = zx + zw + 18f
        val colW = w - 16f - left
        kit.text(canvas, MomentCopy.eyebrow(m), left, 18f, WidgetKit.monoS, accentText(this), colW - 50f)
        kit.text(canvas, m.compactValue, w - 16f, 15f, WidgetKit.metricS, accentText(this), align = Paint.Align.RIGHT)
        val headline = if (data.zenoLine.length > 34) WidgetKit.title22.copy(size = 21f, line = 25f) else WidgetKit.headline
        kit.paragraph(canvas, data.zenoLine, left, 40f, colW, headline, t.text, maxLines = 3)
        val cells = kit.cells(canvas, m.marks, left, h - 18f - 11f, 11f, 6f, t)
        if (m.streakWeeks > 0) {
            val n = m.streakWeeks.toString()
            val nw = kit.width(n, WidgetKit.metricS)
            kit.text(canvas, n, w - 16f, h - 18f - 15f, WidgetKit.metricS, t.text, align = Paint.Align.RIGHT)
            if (left + cells + 12f < w - 16f - nw - 22f) kit.flame(canvas, w - 16f - nw - 19f, h - 18f - 14f, 16f)
        }
        result(spoken(this), WidgetAction.OPEN_TODAY)
    }

    /** One row. At its smallest, Zeno and the count; wider, his line; widest, the week too. */
    private fun oneRow(s: FaceScope): FaceResult = with(s) {
        val m = data.moment
        val surface = data.surfaceFor(input.dark, followsNight = true)
        if (w < 120f) {
            val zw = minOf(w - 14f, (h - 30f) / ZenoPainter.ASPECT)
            val zx = (w - zw) / 2
            ground(surface, lit(w / 2, 10f + zw * ZenoPainter.ASPECT * 0.55f, zw * 1.2f, hue = hueOf(this)))
            zeno(this, zx, 10f, zw)
            kit.text(canvas, m.compactValue, w / 2, h - 26f, WidgetKit.metricS, accentText(this), align = Paint.Align.CENTER)
            hit(RectF(0f, 0f, w, h), WidgetAction.POKE)
            return@with result(spoken(this), WidgetAction.POKE)
        }
        val zw = if (wide) 62f else 50f
        val zh = zw * ZenoPainter.ASPECT
        val zx = if (wide) 16f else 12f
        val zy = (h - zh) / 2
        ground(surface, lit(zx + zw / 2, zy + zh * 0.5f, zw * 1.3f, hue = hueOf(this)))
        zeno(this, zx, zy, zw)
        val left = zx + zw + (if (wide) 16f else 12f)
        when {
            wide && w >= 330f -> {
                val right = w - 16f
                val cellsW = kit.cellsWidth(7, 13f, 4f)
                val colW = right - cellsW - 16f - left
                val lineH = kit.layout(data.zenoLine, WidgetKit.says, t.text, colW, 2).height.toFloat()
                val top = (h - lineH - 18f) / 2
                kit.paragraph(canvas, data.zenoLine, left, top, colW, WidgetKit.says, t.text, maxLines = 2)
                kit.text(canvas, MomentCopy.foot(m), left, top + lineH + 2f, WidgetKit.caption, t.muted, colW)
                kit.cells(canvas, m.marks, right - cellsW, h / 2 - 16f, 13f, 4f, t)
                kit.text(canvas, MomentCopy.streakChip(m), right, h / 2 + 4f, WidgetKit.label, accentText(this), cellsW, Paint.Align.RIGHT)
            }
            wide -> {
                val colW = w - left - 16f
                val style = WidgetKit.says.copy(size = 15f)
                val lineH = kit.layout(data.zenoLine, style, t.text, colW, 2).height.toFloat()
                val top = (h - lineH - 22f) / 2
                kit.paragraph(canvas, data.zenoLine, left, top, colW, style, t.text, maxLines = 2)
                val cellsW = kit.cells(canvas, m.marks, left, top + lineH + 8f, 10f, 3f, t)
                kit.text(canvas, m.compactValue, left + cellsW + 8f, top + lineH + 5f, WidgetKit.metricS.copy(size = 12f, line = 16f), accentText(this))
            }
            w < 160f -> {
                kit.text(canvas, m.compactValue, left, h / 2 - 22f, WidgetKit.metric, accentText(this))
                kit.cells(canvas, m.marks, left, h / 2 + 8f, 6f, 2f, t)
            }
            else -> {
                val colW = w - left - 12f
                kit.text(canvas, m.compactValue, left, 14f, WidgetKit.metricS, accentText(this))
                kit.paragraph(canvas, data.zenoLine, left, 36f, colW, WidgetKit.says.copy(size = 13f, line = 17f), t.text, maxLines = 3)
            }
        }
        result(spoken(this), WidgetAction.OPEN_TODAY)
    }

    /** A Quiet session runs: night, a crescent moon, Zeno asleep, the countdown. */
    private fun quiet(s: FaceScope): FaceResult = with(s) {
        val run = data.quiet.running!!
        val progress = run.progress(data.now)
        val spoken = "Quiet session. Reels and Shorts are quiet until it ends."
        if (rows == 1) {
            val zw = if (w < 200) 40f else 52f
            val zy = (h - zw * ZenoPainter.ASPECT) / 2
            ground(Surface.NIGHT, quietLight(this, 14f + zw / 2, h / 2, zw * 1.4f, WidgetLight.Moon(w - 22f, 22f, 8f), WidgetLight.Box(w * 0.35f, 8f, w - 40f, 30f)))
            ZenoPainter.draw(canvas, 14f, zy, zw, MomentFeeling.SLEEPY, ZenoSkin.ZEN)
            val left = 14f + zw + 12f
            kit.text(canvas, "Quiet", left, h / 2 - 26f, WidgetKit.monoS, t.brand)
            clock(Clock(left, h / 2 - 10f, w - left - 14f, 34f, run.endsAt, 26f, t.brand))
            return@with result(spoken, WidgetAction.OPEN_TODAY)
        }
        val zw = minOf(w - 48f, 170f)
        val zh = zw * ZenoPainter.ASPECT
        val zy = h - zh - 14f
        ground(Surface.NIGHT, quietLight(this, w / 2, zy + zh * 0.55f, zw * 1.1f, WidgetLight.Moon(w - 30f, 34f, 11f), WidgetLight.Box(14f, 84f, w - 50f, zy + 10f)))
        kit.header(canvas, 14f, 14f, "Quiet session", t.brand, w - 60f)
        clock(Clock(14f, 32f, w - 70f, 38f, run.endsAt, 30f, t.brand))
        kit.progressBar(canvas, 14f, 78f, w - 28f, 6f, progress, t.brandFill, t.brandSoft, t.outline)
        kit.softShadow(canvas, w / 2, zy + zh * 0.95f, zw * 0.42f, zw * 0.07f, t.text, 0.35f)
        ZenoPainter.draw(canvas, (w - zw) / 2, zy, zw, MomentFeeling.SLEEPY, ZenoSkin.ZEN)
        result(spoken, WidgetAction.OPEN_TODAY)
    }

    private fun quietLight(s: FaceScope, gx: Float, gy: Float, gr: Float, moon: WidgetLight.Moon, stars: WidgetLight.Box) =
        WidgetLight.Spec(
            s.w, s.h, part = DayPart.NIGHT,
            glow = WidgetLight.Glow(WidgetLight.Hue.ZEN, gx, gy, gr),
            stars = 6, starBox = stars, amberStars = false, moon = moon
        )

    /** Usage access is off: the one card that asks. Never manual entry. */
    private fun locked(s: FaceScope): FaceResult = with(s) {
        val surface = if (input.dark) Surface.INK else Surface.PAPER
        val empty = List(7) { DayMark.NO_DATA }
        if (rows == 1) {
            val zw = 48f
            val zy = (h - zw * ZenoPainter.ASPECT) / 2
            ground(surface, lit(12f + zw / 2, h / 2, zw * 1.3f, hue = WidgetLight.Hue.DUSK, mood = WidgetLight.Mood.NONE))
            ZenoPainter.draw(canvas, 12f, zy, zw, MomentFeeling.CALM, ZenoSkin.DUSK)
            kit.paragraph(canvas, MomentCopy.USAGE_ACCESS_OFF, 72f, 20f, w - 84f, WidgetKit.says.copy(size = 14f, line = 18f), t.text, maxLines = 3)
        } else {
            val zw = minOf(w - 60f, 120f)
            val zh = zw * ZenoPainter.ASPECT
            val zy = (h - zh) / 2 - 6f
            ground(surface, lit(w / 2, zy + zh * 0.55f, zw, hue = WidgetLight.Hue.DUSK, mood = WidgetLight.Mood.NONE))
            kit.cells(canvas, empty, 14f, 15f, 8f, 3f, t)
            kit.icon(canvas, WidgetKit.Icon.LOCK, w - 28f, 12f, 14f, t.mono, 2f)
            ZenoPainter.draw(canvas, (w - zw) / 2, zy, zw, MomentFeeling.CALM, ZenoSkin.DUSK)
            val style = WidgetKit.says.copy(size = 14f, line = 19f)
            val lineH = kit.layout(MomentCopy.USAGE_ACCESS_OFF, style, t.text, w - 28f, 2, Layout.Alignment.ALIGN_CENTER).height.toFloat()
            kit.paragraph(canvas, MomentCopy.USAGE_ACCESS_OFF, 14f, h - 14f - lineH, w - 28f, style, t.text, maxLines = 2, align = Layout.Alignment.ALIGN_CENTER)
        }
        result("Zeno. Usage access is off. Tap to turn it on, so your day counts itself.", WidgetAction.OPEN_USAGE_ACCESS)
    }
}
