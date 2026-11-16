package com.zenlauncher.zenmode.widget.faces

import android.graphics.RectF
import com.zenlauncher.zenmode.ui.theme.WidgetPalette
import com.zenlauncher.zenmode.widget.art.Surface
import com.zenlauncher.zenmode.widget.art.WidgetKit
import com.zenlauncher.zenmode.widget.art.WidgetLight

/**
 * **Zen Gold**: what is set aside, and the gate to gold pay. 2 × 1 to 4 × 2.
 *
 * The gate is the Zen Gold screen's own ([com.zenlauncher.zenmode.widget.GoldState]), drawn as
 * one bar a day it needs; when it opens the widget turns gold and offers Invest. On the lock
 * screen the amount stays hidden.
 */
object GoldFace : Face {

    override fun draw(scope: FaceScope): FaceResult = with(scope) {
        val g = data.gold
        val surface = if (g.open) Surface.GOLD else if (input.dark) Surface.INK else Surface.PAPER
        // The coin's warm light: brighter the closer gold pay is, a celebration once it opens.
        val near = g.kept.toFloat() / g.daysToOpen
        ground(surface, lit(w - 25f, 20f, maxOf(w, h) * (0.55f + 0.35f * near), hue = WidgetLight.Hue.AMBER,
            celebration = g.open, mood = WidgetLight.Mood.NONE))
        val amount = if (input.private) "₹ ···" else "₹" + g.balanceLabel
        val label = if (g.open) "Gold pay · open" else "Gold set aside"
        val labelColor = if (g.open) t.text else t.amberText
        kit.header(canvas, 14f, if (rows == 1) 12f else 13f, label, labelColor, w - 60f)
        kit.coin(canvas, w - 14f - 22f, if (rows == 1) 8f else 9f, 22f)
        if (rows == 1) {
            kit.text(canvas, amount, 14f, 30f, WidgetKit.metricL, t.text, if (wide) w * 0.45f else w - 28f)
            if (wide) {
                val left = w * 0.5f
                gate(this, left, 40f, w - 14f - left)
                kit.text(canvas, line(this), left, 56f, WidgetKit.caption, t.text2, w - 14f - left)
            } else {
                gate(this, 14f, h - 14f - 10f, w - 28f)
            }
        } else {
            kit.text(canvas, amount, 14f, 50f, WidgetKit.metricL, t.text, w - 28f)
            gate(this, 14f, 104f, if (wide) w * 0.55f else w - 28f)
            val colW = (if (wide) w * 0.55f else w - 28f) - (if (g.open) 0f else 24f)
            if (!g.open && !g.outOfReach) {
                kit.leadParagraph(canvas, needed(this), " to open gold pay", 14f, 120f, colW, WidgetKit.callout, t.text, t.text2)
            } else {
                kit.paragraph(canvas, line(this), 14f, 120f, colW, WidgetKit.callout, t.text2, maxLines = 2)
            }
            if (g.open) {
                val pill = RectF(14f, h - 14f - 44f, if (wide) w * 0.55f else w - 14f, h - 14f)
                kit.pill(canvas, pill, "Invest gold", t.text, WidgetPalette.goldSky[1])
                hit(pill, WidgetAction.OPEN_GOLD)
            } else {
                kit.text(canvas, "Kept ${g.kept} of 7 this week", 14f, h - 13f - 14f, WidgetKit.monoS.copy(caps = false, tracking = 0.02f), t.mono, w - 28f)
            }
        }
        result(spoken(this), WidgetAction.OPEN_GOLD)
    }

    private fun needed(s: FaceScope): String {
        val n = s.data.gold.stillNeeded
        return "$n kept day${if (n == 1) "" else "s"}"
    }

    private fun line(s: FaceScope): String {
        val g = s.data.gold
        return when {
            g.open -> "Promise kept this week."
            g.outOfReach -> "Gold pay opens again Monday."
            else -> "${needed(s)} to open gold pay"
        }
    }

    /** One bar a day the gate needs: lit for each kept day. */
    private fun gate(s: FaceScope, x: Float, y: Float, width: Float) = with(s) {
        val g = data.gold
        val n = g.daysToOpen
        val gap = 5f
        val bw = (width - gap * (n - 1)) / n
        for (i in 0 until n) {
            val r = RectF(x + i * (bw + gap), y, x + i * (bw + gap) + bw, y + 10f)
            when {
                g.open -> kit.solid(canvas, r, 3f, t.text)
                i < g.kept -> kit.gradientBar(canvas, r, 3f, WidgetPalette.goldBarFrom, WidgetPalette.goldSky[1])
                else -> {
                    kit.solid(canvas, r, 3f, t.sunk)
                    kit.outline(canvas, r, 3f, t.hair, 1f)
                }
            }
        }
    }

    private fun spoken(s: FaceScope): String = with(s) {
        val g = data.gold
        val amount = if (input.private) "" else "₹${g.balanceLabel} set aside in gold. "
        "Zen Gold. $amount${line(this)}."
    }
}
