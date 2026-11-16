package com.zenlauncher.zenmode.widget.faces

import android.graphics.Paint
import com.zenlauncher.zenmode.coreapi.week.MomentCopy
import com.zenlauncher.zenmode.coreapi.week.MomentFeeling
import com.zenlauncher.zenmode.recap.formatMinutes
import com.zenlauncher.zenmode.widget.art.Surface
import com.zenlauncher.zenmode.widget.art.WidgetKit
import com.zenlauncher.zenmode.widget.art.WidgetLight
import com.zenlauncher.zenmode.widget.art.ZenoPainter
import com.zenlauncher.zenmode.widget.art.ZenoSkin
import kotlin.math.abs

/**
 * **Zen Score**: today's score against the promise. 2 × 1 to 4 × 2, and taller.
 *
 * The number is green while the day is calm (6.0 and up, inside the promise) and ember once it
 * drifts under 6.0 or goes over. The bar marks 6.0, a good day. Wider sizes add Zeno, whose mood
 * follows the day, and time used; taller ones add the week's days with their scores. On the
 * lock screen the minutes stay hidden.
 */
object ScoreFace : Face {

    private class Day(val score: String, val color: Int, val spare: String, val spareBold: String, val spareRest: String, val feeling: MomentFeeling, val skin: ZenoSkin)

    override fun draw(scope: FaceScope): FaceResult = with(scope) {
        val surface = data.surfaceFor(input.dark)
        // The day's own hue glows behind its hero: the number, or Zeno when there's room for him.
        val hue = when (data.mood) {
            WidgetLight.Mood.CALM -> WidgetLight.Hue.ZEN
            WidgetLight.Mood.DRIFT -> WidgetLight.Hue.AMBER
            WidgetLight.Mood.OVER -> WidgetLight.Hue.EMBER
            WidgetLight.Mood.NONE -> WidgetLight.Hue.DUSK
        }
        val spec = when {
            rows == 1 && wide -> lit(14f + 26f, h / 2, 90f, hue = hue, celebration = false)
            rows == 1 -> lit(50f, 50f, 110f, hue = hue, celebration = false)
            wide -> lit(w - 16f - 56f, 84f, 150f, hue = hue, celebration = false)
            else -> lit(w * 0.36f, 74f, w * 0.85f, hue = hue, celebration = false)
        }
        ground(surface, spec)
        val day = day(this)
        when {
            rows == 1 && !wide -> small(this, day)
            rows == 1 -> oneRow(this, day)
            !wide -> square(this, day)
            else -> wideFace(this, day)
        }
        result(spoken(this), if (data.usageAccess) WidgetAction.OPEN_TODAY else WidgetAction.OPEN_USAGE_ACCESS)
    }

    private fun day(s: FaceScope): Day = with(s) {
        val score = data.todayScore
        val calm = data.mood == WidgetLight.Mood.CALM
        val color = if (score == null || calm || surface == Surface.GOLD) t.brand else t.emberText
        val (bold, rest) = when {
            !data.usageAccess -> "Not counting yet" to "· tap to turn on"
            input.private -> MomentCopy.foot(data.moment) to ""
            data.overPromise -> formatMinutes(-data.minutesLeft) to "over"
            else -> formatMinutes(data.minutesLeft) to "to spare"
        }
        val (feeling, skin) = when (data.mood) {
            WidgetLight.Mood.CALM -> MomentFeeling.PLEASANT to ZenoSkin.ZEN
            WidgetLight.Mood.DRIFT -> MomentFeeling.CALM to ZenoSkin.DUSK
            WidgetLight.Mood.OVER -> MomentFeeling.ALERT to ZenoSkin.EMBER
            WidgetLight.Mood.NONE -> MomentFeeling.CALM to ZenoSkin.DUSK
        }
        Day(score?.let(MomentCopy::formatTenths) ?: "–", color, "$bold $rest".trim(), bold, rest, feeling, skin)
    }

    private fun spoken(s: FaceScope): String = with(s) {
        val score = data.todayScore ?: return@with "Zen Score. Nothing measured yet today."
        val spare = when {
            input.private -> ""
            data.overPromise -> " ${formatMinutes(-data.minutesLeft)} over your promise."
            else -> " ${formatMinutes(data.minutesLeft)} to spare."
        }
        "Today's Zen Score is ${MomentCopy.formatTenths(score)} of 10.$spare ${MomentCopy.progress(data.moment)}."
    }

    /** "▲ 0.3" against yesterday, or nothing when there's no yesterday to compare with. */
    private fun delta(s: FaceScope, right: Float, y: Float) = with(s) {
        val today = data.todayScore ?: return@with
        val before = data.yesterdayScore ?: return@with
        val d = today - before
        if (d == 0) return@with
        val label = (if (d > 0) "▲ " else "▼ ") + MomentCopy.formatTenths(abs(d))
        if (d > 0) kit.tag(canvas, label, right, y, t.brandTint, t.brand) else kit.tag(canvas, label, right, y, t.emberTint, t.emberText)
    }

    /** 2 × 1: the number and the bar. */
    private fun small(s: FaceScope, day: Day) = with(s) {
        kit.header(canvas, 14f, 12f, "Zen Score", t.mono, w - 70f)
        delta(this, w - 14f, 9f)
        kit.text(canvas, day.score, 14f, 32f, WidgetKit.metricL, day.color)
        kit.scoreBar(canvas, 14f, h - 11f - 16f, w - 28f, data.todayScore, t)
    }

    /** 4 × 1: Zeno, the number, the bar, the week and the time to spare. */
    private fun oneRow(s: FaceScope, day: Day) = with(s) {
        val zw = if (w < 330f) 44f else 52f
        ZenoPainter.draw(canvas, 14f, (h - zw * ZenoPainter.ASPECT) / 2, zw, day.feeling, day.skin)
        val col = 14f + zw + 10f
        kit.header(canvas, col, 24f, "Zen Score", t.mono, 90f)
        kit.text(canvas, day.score, col, 42f, WidgetKit.metricL, day.color)
        val right = col + (if (w < 330f) 78f else 92f)
        val gw = w - 14f - right
        kit.scoreBar(canvas, right, 30f, gw, data.todayScore, t)
        val spareStyle = WidgetKit.monoS.copy(caps = false, tracking = 0.02f)
        if (w < 330f) {
            kit.text(canvas, day.spare, right, 54f, spareStyle, t.mono, gw)
        } else {
            val cellsW = kit.cells(canvas, data.moment.marks, right, 58f, 8f, 3f, t)
            kit.text(canvas, day.spare, w - 14f, 54f, spareStyle, t.mono, gw - cellsW - 8f, Paint.Align.RIGHT)
        }
    }

    /** 2 × 2 and taller: the number large, the bar, the time to spare, the week. */
    private fun square(s: FaceScope, day: Day) = with(s) {
        kit.header(canvas, 14f, 14f, "Zen Score", t.mono, if (narrow) w - 28f else w - 80f)
        if (!narrow) delta(this, w - 14f, 11f)
        val big = if (narrow) WidgetKit.metricXL else WidgetKit.metricXXL
        val numW = kit.text(canvas, day.score, 14f, 40f, big, day.color, w - 50f)
        if (!narrow) kit.text(canvas, "/10", 14f + numW + 4f, 40f + big.line - 22f, WidgetKit.metricS, t.mono)
        val barTop = if (narrow) 104f else 118f
        kit.scoreBar(canvas, 14f, barTop, w - 28f, data.todayScore, t)
        val boldW = kit.text(canvas, day.spareBold, 14f, barTop + 24f, WidgetKit.calloutBold, t.text, w - 28f)
        if (day.spareRest.isNotEmpty()) kit.text(canvas, " " + (if (narrow && day.spareRest == "to spare") "left" else day.spareRest), 14f + boldW, barTop + 24f, WidgetKit.callout, t.text2)
        if (rows >= 3) {
            val cw = minOf(24f, (w - 28f - 24f) / 7)
            kit.dayCells(canvas, data.moment.marks, data.dayScores, 14f, barTop + 56f, w - 28f, cw, cw, t, surface == Surface.GOLD)
        }
        val cell = if (narrow) 7f else 9f
        kit.cells(canvas, data.moment.marks, 14f, h - 13f - 12f, cell, if (narrow) 2f else 3f, t)
        kit.text(canvas, data.moment.compactValue, w - 14f, h - 13f - 16f, WidgetKit.metricS.copy(size = 12f, line = 16f), t.amberText, align = Paint.Align.RIGHT)
    }

    /** 4 × 2 and taller: the number, Zeno, the bar with its 6.0 mark, time used, the week. */
    private fun wideFace(s: FaceScope, day: Day) = with(s) {
        kit.header(canvas, 16f, 16f, "Zen Score · today", t.mono, w - 90f)
        delta(this, w - 16f, 13f)
        val numW = kit.text(canvas, day.score, 16f, 44f, WidgetKit.metricXXL, day.color)
        kit.text(canvas, "/10", 16f + numW + 6f, 44f + 34f, WidgetKit.metric, t.mono)
        val zw = if (w < 330f) 92f else 112f
        ZenoPainter.draw(canvas, w - 16f - zw, 44f, zw, day.feeling, day.skin)
        kit.scoreBar(canvas, 16f, 128f, w - 32f, data.todayScore, t, label = true)
        if (rows >= 3) {
            val cw = minOf(40f, (w - 32f - 36f) / 7)
            kit.dayCells(canvas, data.moment.marks, data.dayScores, 16f, 172f, w - 32f, cw, cw, t, surface == Surface.GOLD)
        }
        val cellsW = kit.cellsWidth(7, 10f, 4f)
        kit.cells(canvas, data.moment.marks, w - 16f - cellsW, h - 13f - 15f, 10f, 4f, t)
        val used = if (data.usageAccess && !input.private) "${formatMinutes(data.minutesToday)} today · " else ""
        val boldW = kit.text(canvas, used + day.spareBold, 16f, h - 13f - 20f, WidgetKit.calloutBold, t.text, w - 48f - cellsW)
        if (day.spareRest.isNotEmpty()) kit.text(canvas, " " + day.spareRest, 16f + boldW, h - 13f - 20f, WidgetKit.callout, t.text2, w - 48f - cellsW - boldW)
    }
}
