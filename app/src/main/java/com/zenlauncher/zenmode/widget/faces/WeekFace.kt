package com.zenlauncher.zenmode.widget.faces

import android.graphics.Paint
import android.graphics.RectF
import com.zenlauncher.zenmode.coreapi.week.MomentAccent
import com.zenlauncher.zenmode.coreapi.week.MomentCopy
import com.zenlauncher.zenmode.coreapi.week.MomentKind
import com.zenlauncher.zenmode.ui.theme.WidgetPalette
import com.zenlauncher.zenmode.widget.art.Surface
import com.zenlauncher.zenmode.widget.art.WidgetKit
import com.zenlauncher.zenmode.widget.art.WidgetLight
import com.zenlauncher.zenmode.widget.art.ZenoPainter
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

/**
 * **This week**: the one streak, and the week it's made of. One widget for every size, each
 * step up adding detail:
 *
 *  · 2 × 1 and 2 × 2: the **weekly streak**, a star in the sky for every week won;
 *  · 4 × 1: the seven days with their scores;
 *  · 4 × 2: Zeno, the week in a few words, the seven days, good days and the streak;
 *  · 4 × 3 and up: the **week in bloom**, all 168 hours, a seed each, always on night.
 *
 * A won week turns it gold with every other widget; a perfect week turns it night.
 */
object WeekFace : Face {

    override fun draw(scope: FaceScope): FaceResult = with(scope) {
        when {
            wide && rows >= 3 -> bloom(this)
            wide && rows == 2 -> week(this)
            wide -> days(this)
            rows == 1 -> streakStrip(this)
            else -> streak(this)
        }
        result(MomentCopy.spoken(data.moment, MomentCopy.daySeed(data.today)), WidgetAction.OPEN_WEEK)
    }

    private fun accent(s: FaceScope): Int = with(s) {
        if (data.moment.accent == MomentAccent.EMBER) t.emberText else t.amberText
    }

    private fun weekLabel(s: FaceScope): String = "Week ${s.data.today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"

    /** The streak lives on a dark sky so its stars can show; won and perfect weeks follow every widget. */
    private fun streakSurface(s: FaceScope): Surface {
        val follow = s.data.surfaceFor(s.input.dark, followsQuiet = false, followsNight = true)
        return when {
            follow == Surface.GOLD || follow == Surface.NIGHT -> follow
            s.data.moment.streakWeeks == 0 -> follow
            else -> Surface.INK
        }
    }

    private fun mood(s: FaceScope): WidgetLight.Mood =
        if (s.data.moment.kind == MomentKind.ON_THE_LINE) WidgetLight.Mood.OVER else WidgetLight.Mood.CALM

    /** The flame's warm light, ember when the streak is on the line. */
    private fun flameHue(s: FaceScope): WidgetLight.Hue =
        if (s.data.moment.kind == MomentKind.ON_THE_LINE) WidgetLight.Hue.EMBER else WidgetLight.Hue.AMBER

    /** 2 × 2: the weekly streak. */
    private fun streak(s: FaceScope) = with(s) {
        val m = data.moment
        val surface = streakSurface(this)
        ground(surface, lit(w - 25f, 21f, w * 0.55f, hue = flameHue(this), celebration = false, mood = mood(this),
            starBox = WidgetLight.Box(w * 0.5f, 40f, w - 12f, h - 56f)))
        val stake = m.kind == MomentKind.ON_THE_LINE && m.streakAtStake > 0
        val acc = if (stake) t.emberText else t.amberText
        val cell = minOf(16f, (w - 28f - 36f) / 7)
        if (m.streakWeeks == 0 && !stake) {
            kit.header(canvas, 14f, 14f, "Streak", t.amberText, w - 60f)
            kit.flame(canvas, w - 14f - 22f, 10f, 22f, t.outline, t.surface)
            kit.text(canvas, "First week.", 14f, 56f, WidgetKit.headline, t.text, w - 28f)
            kit.paragraph(canvas, MomentCopy.headline(m), 14f, 92f, w - 28f, WidgetKit.caption, t.muted, maxLines = 2)
        } else {
            kit.header(canvas, 14f, 14f, if (stake) "At stake" else if (m.isWon) "Week won" else "Streak", acc, w - 60f)
            kit.flame(canvas, w - 14f - 22f, 10f, 22f)
            val weeks = if (stake) m.streakAtStake else m.streakWeeks
            val numW = kit.text(canvas, weeks.toString(), 14f, 42f, WidgetKit.metricXXL, t.text, w * 0.6f)
            kit.text(canvas, if (weeks == 1) "week" else "weeks", 14f + numW + 6f, 42f + 40f, WidgetKit.calloutBold, acc)
            val caption = when {
                stake -> "Every day left counts."
                m.isWon -> MomentCopy.foot(m) + " this week."
                else -> "A star for every week won."
            }
            kit.paragraph(canvas, caption, 14f, 120f, w - 28f, WidgetKit.caption, t.text3, maxLines = 2)
        }
        kit.cells(canvas, m.marks, 14f, h - 16f - cell, cell, (cell * 0.375f).coerceAtLeast(3f), t)
    }

    /** 2 × 1: the streak, folded to one row. */
    private fun streakStrip(s: FaceScope) = with(s) {
        val m = data.moment
        val surface = streakSurface(this)
        ground(surface, lit(w - 40f, 22f, w * 0.55f, hue = flameHue(this), celebration = false, mood = mood(this),
            starBox = WidgetLight.Box(18f, 36f, w - 18f, 62f)))
        kit.header(canvas, 14f, 13f, "Streak", t.amberText, w - 100f)
        val unit = "wk"
        val unitW = kit.width(unit, WidgetKit.label)
        val numW = kit.width(m.streakWeeks.toString(), WidgetKit.metric.copy(size = 24f, line = 28f))
        kit.text(canvas, unit, w - 14f, 18f, WidgetKit.label, t.amberText, align = Paint.Align.RIGHT)
        kit.text(canvas, m.streakWeeks.toString(), w - 14f - unitW - 4f, 9f, WidgetKit.metric.copy(size = 24f, line = 28f), t.text, align = Paint.Align.RIGHT)
        kit.flame(canvas, w - 14f - unitW - 4f - numW - 26f, 12f, 20f)
        val cell = minOf(16f, (w - 28f - 36f) / 7)
        kit.cells(canvas, m.marks, 14f, h - 14f - cell, cell, (cell * 0.375f).coerceAtLeast(3f), t)
    }

    /** 4 × 1: the seven days with their scores. */
    private fun days(s: FaceScope) = with(s) {
        val m = data.moment
        val surface = data.surfaceFor(input.dark, followsQuiet = false, followsNight = true)
        ground(surface, lit(w * 0.5f, h * 0.7f, w * 0.45f))
        val acc = accent(this)
        val chip = MomentCopy.streakChip(m)
        val chipW = kit.width(chip, WidgetKit.label) + if (m.streakWeeks > 0) 19f else 0f
        val label = if (w < 330f) MomentCopy.foot(m) else "This week · ${MomentCopy.foot(m)}"
        kit.header(canvas, 16f, 12f, label, acc, w - 32f - chipW - 10f)
        kit.text(canvas, chip, w - 16f, 10f, WidgetKit.label, acc, align = Paint.Align.RIGHT)
        if (m.streakWeeks > 0) kit.flame(canvas, w - 16f - chipW, 10f, 15f)
        val cw = minOf(40f, (w - 32f - 36f) / 7)
        kit.dayCells(canvas, m.marks, data.dayScores, 16f, 34f, w - 32f, cw, minOf(cw, h - 34f - 18f - 10f), t, surface == Surface.GOLD)
    }

    /** 4 × 2: the board's This week. */
    private fun week(s: FaceScope) = with(s) {
        val m = data.moment
        val surface = data.surfaceFor(input.dark, followsQuiet = false, followsNight = true)
        val narrowWeek = w < 330f
        val zw = if (narrowWeek) 60f else 84f
        val zh = zw * ZenoPainter.ASPECT
        ground(surface, lit(w - 16f - zw / 2, 14f + zh * 0.55f, zw * 1.5f, starBox = WidgetLight.Box(w * 0.45f, 30f, w - zw - 24f, 70f)))
        val acc = accent(this)
        val label = if (narrowWeek) MomentCopy.eyebrow(m) else "${weekLabel(this)} · ${MomentCopy.eyebrow(m)}"
        kit.header(canvas, 16f, 16f, label, acc, w - 32f - zw - 8f)
        kit.text(canvas, MomentCopy.headline(m), 16f, 36f, if (narrowWeek) WidgetKit.headline.copy(size = 20f, line = 25f) else WidgetKit.headline, t.text, w - 32f - zw - 8f)
        val feeling = data.zenoFeeling
        ZenoPainter.draw(canvas, w - 16f - zw, 14f, zw, feeling)
        hit(RectF(w - 16f - zw, 14f, w - 16f, 14f + zw * ZenoPainter.ASPECT), WidgetAction.POKE)
        val cw = if (narrowWeek) 32f else 44f
        kit.dayCells(canvas, m.marks, data.dayScores, 16f, 84f, w - 32f, cw, 44f, t, surface == Surface.GOLD)
        kit.text(canvas, MomentCopy.foot(m), 16f, h - 13f - 20f, WidgetKit.calloutBold, t.text, w * 0.5f)
        val chip = MomentCopy.streakChip(m)
        val chipW = kit.text(canvas, chip, w - 16f, h - 13f - 18f, WidgetKit.label, acc, w * 0.4f, Paint.Align.RIGHT)
        if (m.streakWeeks > 0 && m.kind != MomentKind.SLIPPING) kit.flame(canvas, w - 16f - chipW - 20f, h - 13f - 18f, 15f)
    }

    /** 4 × 3 and up: the week in bloom, on night. */
    private fun bloom(s: FaceScope) = with(s) {
        val m = data.moment
        val stats = 64f
        val size = minOf(w - 60f, h - 86f - stats - 14f)
        val bloomTop = 86f + (h - 86f - stats - 6f - size) / 2
        ground(Surface.NIGHT, lit(w / 2, bloomTop + size / 2, size * 0.75f, hue = WidgetLight.Hue.ZEN, celebration = false, mood = WidgetLight.Mood.NONE))
        val day = data.today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        kit.header(canvas, 18f, 18f, "${weekLabel(this)} · $day", t.brand, w * 0.55f)
        kit.text(canvas, "Week in bloom", 18f, 36f, WidgetKit.headline, t.text, w * 0.6f)
        legend(this, w - 18f, 18f)
        BloomPainter.draw(canvas, (w - size) / 2, bloomTop, size, input.bloom ?: placeholder(this))
        // Three raised tiles, as on iPhone: what the week has earned so far.
        val gap = 8f
        val tileW = (w - 32f - gap * 2) / 3
        val y = h - 16f - stats
        val won = m.goodDays >= 4
        stat(this, "Good days", if (won) "${m.goodDays}" else "${m.goodDays}/4", if (won) t.amber else t.text, 16f, y, tileW, stats)
        stat(this, "Week streak", if (m.streakWeeks > 0) m.streakWeeks.toString() else "First", if (m.streakWeeks > 0) t.amber else t.text2, 16f + tileW + gap, y, tileW, stats)
        stat(this, "Zen today", data.todayScore?.let(MomentCopy::formatTenths) ?: "Soon", if (m.todayIsGood) t.brand else t.text, 16f + (tileW + gap) * 2, y, tileW, stats)
    }

    private fun legend(s: FaceScope, right: Float, top: Float) = with(s) {
        listOf(
            WidgetPalette.bloomAway to "Phone down",
            WidgetPalette.bloomOnPurpose to "On purpose",
            WidgetPalette.bloomUnplanned to "Unplanned"
        ).forEachIndexed { i, (color, name) ->
            val y = top + i * 14f
            val tw = kit.text(canvas, name, right, y, WidgetKit.monoS.copy(caps = false, tracking = 0.04f), t.mono, align = Paint.Align.RIGHT)
            kit.solid(canvas, RectF(right - tw - 12f, y + 3.5f, right - tw - 5f, y + 10.5f), 2f, color)
        }
    }

    private fun stat(s: FaceScope, label: String, value: String, color: Int, x: Float, y: Float, tileW: Float, tileH: Float) = with(s) {
        kit.solid(canvas, RectF(x, y, x + tileW, y + tileH), 14f, com.zenlauncher.zenmode.share.withAlpha(t.surface2, 0.85f))
        kit.outline(canvas, RectF(x, y, x + tileW, y + tileH), 14f, t.hair, 1f)
        kit.text(canvas, value, x + 12f, y + 10f, WidgetKit.metric, color, tileW - 20f)
        kit.text(canvas, label, x + 12f, y + tileH - 22f, WidgetKit.monoS.copy(size = 9f), t.mono, tileW - 20f)
    }

    /** Before the hours are read: the week so far as unknown seeds, the rest still to come. */
    private fun placeholder(s: FaceScope): List<BloomCell> {
        val todayIndex = s.data.week.week.today
        return List(7 * 24) { i -> if (i / 24 <= todayIndex) BloomCell.UNKNOWN else BloomCell.FUTURE }
    }
}
