package com.zenlauncher.zenmode.widget.faces

import android.graphics.Paint
import android.graphics.RectF
import com.zenlauncher.zenmode.coreapi.ZEN_CIRCLE_MAX_MEMBERS
import com.zenlauncher.zenmode.coreapi.week.MomentCopy
import com.zenlauncher.zenmode.coreapi.week.MomentFeeling
import com.zenlauncher.zenmode.ui.theme.WidgetPalette
import com.zenlauncher.zenmode.widget.CircleSeat
import com.zenlauncher.zenmode.widget.CircleState
import com.zenlauncher.zenmode.widget.art.Surface
import com.zenlauncher.zenmode.widget.art.WidgetKit
import com.zenlauncher.zenmode.widget.art.WidgetLight
import com.zenlauncher.zenmode.widget.art.ZenoPainter

/**
 * **Zen Circle**: your rank and the top three, today. 2 × 2 and 4 × 2.
 *
 * The sky holds the circle as orbits, a seat a member. Ranks follow today's Zen Score, the
 * number every member already sees (on iPhone the only one that leaves the phone). A circle of
 * one, no circle, or signed out each get one calm invitation. On the lock screen the circle
 * stays hidden.
 */
object CircleFace : Face {

    override fun draw(scope: FaceScope): FaceResult = with(scope) {
        val surface = if (input.dark) Surface.INK else Surface.PAPER
        if (input.private) return@with hidden(this, surface)
        when (val c = data.circle) {
            is CircleState.Ranked -> ranked(this, surface, c)
            is CircleState.Alone -> invite(this, surface, c.name, "Bring your people.", "Up to seven of you, side by side.", "Invite", WidgetAction.OPEN_CIRCLE)
            CircleState.NoCircle -> invite(this, surface, "Zen Circle", "Bring your people.", "Up to seven of you, side by side.", "Start", WidgetAction.OPEN_CIRCLE)
            CircleState.SignedOut -> invite(this, surface, "Zen Circle", "Sign in to bring your people.", null, "Sign in", WidgetAction.OPEN_SIGN_IN)
        }
    }

    private fun score(seat: CircleSeat) = seat.scoreTenths?.let(MomentCopy::formatTenths) ?: "–"

    private fun ranked(s: FaceScope, surface: Surface, c: CircleState.Ranked): FaceResult = with(s) {
        val ox = if (wide) 70f else w - 38f
        val oy = if (wide) h - 46f else 44f
        ground(surface, lit(ox, oy, 110f, hue = if (c.leading) WidgetLight.Hue.AMBER else WidgetLight.Hue.ZEN, celebration = false, mood = WidgetLight.Mood.NONE))
        orbits(this, ox, oy, if (wide) listOf(15f, 28f) else listOf(14f, 26f), c.seats.take(ZEN_CIRCLE_MAX_MEMBERS))
        val acc = if (c.leading) t.amberText else t.brand
        val rank = "#${c.myRank}"
        val sub = when {
            c.leading -> "Calmest of ${c.size} today"
            c.behindBy != null -> "of ${c.size} today · ${MomentCopy.formatTenths(c.behindBy!!)} behind"
            else -> "of ${c.size} today"
        }
        if (wide) {
            val colW = 132f
            kit.header(canvas, 16f, 16f, c.name, acc, colW)
            kit.text(canvas, rank, 16f, 38f, WidgetKit.metricXL, if (c.leading) t.amberText else t.text)
            kit.paragraph(canvas, sub, 16f, 92f, colW, WidgetKit.caption, t.muted, maxLines = 2)
            val rows = topRows(c)
            val left = 16f + colW + 12f
            val rowH = 44f
            val top = (h - (rows.size * rowH + (rows.size - 1) * 6f)) / 2
            rows.forEachIndexed { i, (place, seat) -> row(this, place, seat, left, top + i * (rowH + 6f), w - 12f - left, rowH) }
        } else {
            kit.header(canvas, 14f, 14f, c.name, acc, w - 84f)
            kit.text(canvas, rank, 14f, 32f, WidgetKit.metricXL, if (c.leading) t.amberText else t.text)
            kit.paragraph(canvas, sub, 14f, 86f, w - 28f, WidgetKit.caption, t.muted, maxLines = 2)
            val rows = topRows(c).take(2)
            rows.forEachIndexed { i, (place, seat) -> row(this, place, seat, 10f, h - 12f - (rows.size - i) * 40f + 4f, w - 20f, 36f) }
        }
        val me = c.me
        result(
            "Zen Circle, ${c.name}. You are number ${c.myRank} of ${c.size} today" +
                (me?.scoreTenths?.let { ", with a Zen Score of ${MomentCopy.formatTenths(it)}." } ?: "."),
            WidgetAction.OPEN_CIRCLE
        )
    }

    /** The top three; when you're further down, the top two and you. */
    private fun topRows(c: CircleState.Ranked): List<Pair<Int, CircleSeat>> {
        val top = c.seats.take(3).mapIndexed { i, s -> i + 1 to s }
        if (c.myRank <= 3) return top
        val me = c.me ?: return top
        return top.take(2) + (c.myRank to me)
    }

    private fun row(s: FaceScope, place: Int, seat: CircleSeat, x: Float, y: Float, width: Float, height: Float) = with(s) {
        val rect = RectF(x, y, x + width, y + height)
        if (seat.isYou) {
            kit.solid(canvas, rect, 14f, t.brandTint)
            kit.outline(canvas, rect, 14f, t.brandTintLine, 1f)
        } else {
            kit.solid(canvas, rect, 14f, t.surface)
        }
        val mid = y + height / 2
        kit.text(canvas, "#%02d".format(place), x + 10f, mid - 7f, WidgetKit.monoS, t.mono)
        val (bg, fg) = when {
            seat.isYou -> t.brandFill to t.brandOn
            place == 1 -> t.amberTint to t.amberText
            else -> t.sunk to t.text2
        }
        kit.avatar(canvas, x + 46f, mid, if (height < 40f) 24f else 28f, seat.initial, bg, fg)
        val name = if (seat.isYou) "You" else seat.name
        kit.text(canvas, score(seat), x + width - 12f, mid - 10f, WidgetKit.metricS, t.brand, align = Paint.Align.RIGHT)
        kit.text(canvas, name, x + 66f, mid - 10f, WidgetKit.calloutBold, t.text, width - 66f - 52f)
    }

    private fun invite(s: FaceScope, surface: Surface, name: String, headline: String, sub: String?, button: String, action: WidgetAction): FaceResult = with(s) {
        val hue = if (action == WidgetAction.OPEN_SIGN_IN) WidgetLight.Hue.DUSK else WidgetLight.Hue.ZEN
        ground(surface, lit(if (wide) 72f else w / 2, if (wide) h / 2 else h - 40f, 120f, hue = hue, celebration = false, mood = WidgetLight.Mood.NONE))
        if (wide) orbits(this, 72f, h / 2, listOf(66f, 86f), emptyList())
        if (wide) {
            val zw = 112f
            ZenoPainter.draw(canvas, 16f, (h - zw * ZenoPainter.ASPECT) / 2, zw, MomentFeeling.CALM)
            val left = 16f + zw + 16f
            val colW = w - 16f - left
            kit.header(canvas, left, 30f, name, t.brand, colW)
            val hh = kit.paragraph(canvas, headline, left, 48f, colW, WidgetKit.title22, t.text, maxLines = 2)
            var y = 48f + hh + 4f
            if (sub != null) {
                y += kit.paragraph(canvas, sub, left, y, colW, WidgetKit.caption, t.muted, maxLines = 2) + 8f
                seatsRow(this, left, y)
                y += 30f
            } else {
                y += 8f
            }
            val pill = RectF(left, y, left + kit.width(button, WidgetKit.button) + 52f, y + 40f)
            kit.pill(canvas, pill, button, t.text, t.bg, icon = if (action == WidgetAction.OPEN_CIRCLE) WidgetKit.Icon.PLUS else null)
            hit(pill, action)
        } else {
            kit.header(canvas, 14f, 14f, name, t.brand, w - 28f)
            kit.paragraph(canvas, headline, 14f, 38f, w - 28f, WidgetKit.says, t.text, maxLines = 3)
            if (rows >= 2) seatsRow(this, 14f, h - 14f - 40f - 34f, size = minOf(18f, (w - 28f - 30f) / 7))
            val pill = RectF(14f, h - 14f - 40f, w - 14f, h - 14f)
            kit.pill(canvas, pill, button, t.text, t.bg)
            hit(pill, action)
        }
        result("Zen Circle. $headline ${sub ?: ""}".trim(), action)
    }

    /** Seven seats, yours lit. */
    private fun seatsRow(s: FaceScope, x: Float, y: Float, size: Float = 22f) = with(s) {
        for (i in 0 until ZEN_CIRCLE_MAX_MEMBERS) {
            val r = RectF(x + i * (size + 5f), y, x + i * (size + 5f) + size, y + size)
            if (i == 0) kit.solid(canvas, r, 6f, t.brandFill)
            else {
                kit.solid(canvas, r, 6f, t.surface)
                kit.outline(canvas, r, 6f, t.outline, 1.5f)
            }
        }
    }

    private fun hidden(s: FaceScope, surface: Surface): FaceResult = with(s) {
        ground(surface, lit(w / 2, h / 2, maxOf(w, h) * 0.5f, hue = WidgetLight.Hue.DUSK, celebration = false, mood = WidgetLight.Mood.NONE))
        orbits(this, w / 2, h * 0.62f, listOf(24f, 42f), emptyList())
        kit.header(canvas, 14f, 14f, "Zen Circle", t.brand, w - 28f)
        kit.paragraph(canvas, "Unlock to see your circle.", 14f, 40f, w - 28f, WidgetKit.says, t.text, maxLines = 2)
        result("Zen Circle. Unlock to see your circle.", WidgetAction.OPEN_CIRCLE)
    }

    /**
     * The circle as orbits: fine rings of dots, and one soft light a member on them, you the
     * brightest, the leader in amber.
     */
    private fun orbits(s: FaceScope, cx: Float, cy: Float, radii: List<Float>, seats: List<CircleSeat>) = with(s) {
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            strokeCap = Paint.Cap.ROUND
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(0.1f, 5f), 0f)
            color = com.zenlauncher.zenmode.share.withAlpha(t.brandFill, if (surface.isDark) 0.45f else 0.35f)
        }
        radii.forEach { canvas.drawCircle(cx, cy, it, ring) }
        if (seats.isEmpty()) return@with
        val dot = Paint(Paint.ANTI_ALIAS_FLAG)
        seats.forEachIndexed { i, seat ->
            val a = Math.toRadians(-90.0 + i * 360.0 / seats.size)
            val r = radii[i % radii.size]
            val x = cx + r * kotlin.math.cos(a).toFloat()
            val y = cy + r * kotlin.math.sin(a).toFloat()
            val color = if (i == 0) WidgetPalette.flameCore else t.brandFill
            val size = if (seat.isYou) 4.2f else 3f
            kit.softShadow(canvas, x, y, size * 3f, size * 3f, color, if (seat.isYou) 0.45f else 0.25f)
            dot.color = com.zenlauncher.zenmode.share.withAlpha(color, if (seat.isYou || i == 0) 1f else 0.6f)
            canvas.drawCircle(x, y, size, dot)
        }
    }
}
