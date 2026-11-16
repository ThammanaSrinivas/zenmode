package com.zenlauncher.zenmode.widget.faces

import android.graphics.RectF
import com.zenlauncher.zenmode.share.withAlpha
import com.zenlauncher.zenmode.coreapi.week.MomentFeeling
import com.zenlauncher.zenmode.widget.art.Surface
import com.zenlauncher.zenmode.widget.art.WidgetKit
import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.widget.art.WidgetLight
import com.zenlauncher.zenmode.widget.art.ZenoPainter
import com.zenlauncher.zenmode.widget.art.ZenoSkin

/**
 * **Quiet**: the one widget you press. Start puts Reels & Shorts to sleep for 25 minutes without
 * opening the app; the countdown ticks in place (a system Chronometer, so it keeps time with
 * our process asleep); End stops it early. Then Zeno says how it went.
 *
 * Without the accessibility service a session would quiet nothing, so the widget says that,
 * and its button opens the one screen that turns quieting on.
 */
object QuietFace : Face {

    private const val PRESET = "Reels & Shorts"

    override fun draw(scope: FaceScope): FaceResult = with(scope) {
        val q = data.quiet
        when {
            q.running != null -> running(this)
            q.ended != null -> done(this)
            else -> idle(this)
        }
    }

    private fun today(n: Int) = when (n) {
        1 -> "Your first today."
        2 -> "That's two today."
        else -> "That's $n today."
    }

    private fun idle(s: FaceScope): FaceResult = with(s) {
        val available = data.quiet.available
        val minutes = data.quiet.minutes
        val action = if (available) WidgetAction.QUIET_START else WidgetAction.OPEN_QUIET_SETUP
        val label = if (available) "Start" else "Set up"
        val surface = if (input.dark) Surface.INK else Surface.PAPER
        if (rows == 1) {
            val small = w < 330f
            val zw = if (small) 44f else 56f
            ground(surface, lit(16f + zw / 2, h / 2, zw * 1.4f, hue = WidgetLight.Hue.ZEN, celebration = false, mood = WidgetLight.Mood.NONE))
            ZenoPainter.draw(canvas, 16f, (h - zw * ZenoPainter.ASPECT) / 2, zw, MomentFeeling.CALM)
            val pillW = if (w < 200f) 76f else if (small) 88f else 108f
            val pill = RectF(w - 14f - pillW, (h - 48f) / 2, w - 14f, (h + 48f) / 2)
            kit.pill(canvas, pill, label, t.brandFill, t.brandOn, icon = if (small || !available) null else WidgetKit.Icon.TIMER)
            hit(pill, action)
            val col = 16f + zw + 12f
            val colW = pill.left - 10f - col
            if (w >= 200f) {
                kit.text(canvas, "Quiet the pull.", col, h / 2 - 22f, WidgetKit.says.copy(size = if (small) 14f else 16f, line = 22f), t.text, colW)
                val sub = if (!available) "Turn on quieting first." else if (small) "$minutes min" else "$PRESET · $minutes min"
                kit.text(canvas, sub, col, h / 2 + 1f, WidgetKit.caption, t.muted, colW)
            } else {
                kit.text(canvas, "Quiet", col, h / 2 - 10f, WidgetKit.says.copy(size = 14f), t.text, colW)
            }
            return@with result(spokenIdle(this), action)
        }
        ground(surface, lit(if (wide) w * 0.8f else w * 0.5f, h - 40f, maxOf(w, h) * 0.55f, hue = WidgetLight.Hue.ZEN, celebration = false, mood = WidgetLight.Mood.NONE))
        val colW = if (wide) w * 0.56f else w - 28f
        kit.header(canvas, 14f, 14f, "Quiet", t.brand, colW)
        kit.paragraph(canvas, "Quiet the pull.", 14f, 40f, colW, WidgetKit.says, t.text, maxLines = 1)
        val chip = "$PRESET · $minutes min"
        val chipW = minOf(kit.width(chip, WidgetKit.label) + 20f, colW)
        kit.solid(canvas, RectF(14f, 70f, 14f + chipW, 96f), 8f, t.sunk)
        kit.outline(canvas, RectF(14f, 70f, 14f + chipW, 96f), 8f, t.hair, 1f)
        kit.text(canvas, chip, 24f, 75f, WidgetKit.label, t.text2, chipW - 20f)
        val note = if (available) "Starts here. The app stays closed." else "Quieting needs one switch on first."
        kit.paragraph(canvas, note, 14f, 106f, colW, WidgetKit.caption, t.muted, maxLines = 2)
        val pill = RectF(14f, h - 14f - 48f, 14f + colW, h - 14f)
        kit.pill(canvas, pill, label, t.brandFill, t.brandOn, icon = if (available) WidgetKit.Icon.TIMER else null)
        hit(pill, action)
        if (wide) {
            val zw = minOf(w - colW - 40f, 150f)
            ZenoPainter.draw(canvas, w - 14f - zw, h - zw * 0.64f, zw, MomentFeeling.CALM)
        }
        result(spokenIdle(this), action)
    }

    private fun spokenIdle(s: FaceScope) =
        if (s.data.quiet.available) "Quiet. Start ${s.data.quiet.minutes} minutes of quiet for $PRESET. The app stays closed."
        else "Quiet. Quieting needs the accessibility switch on first. Tap to set it up."

    private fun running(s: FaceScope): FaceResult = with(s) {
        val run = data.quiet.running!!
        val progress = run.progress(data.now)
        val spoken = "Quiet session. $PRESET are quiet. Tap End to stop early."
        if (rows == 1) {
            val small = w < 330f
            val zw = if (w < 200f) 36f else if (small) 44f else 56f
            ground(Surface.NIGHT, night(this, 16f + zw / 2, h / 2, zw * 1.4f, WidgetLight.Moon(w * 0.62f, 20f, 7f), WidgetLight.Box(w * 0.25f, 8f, w * 0.55f, 30f)))
            ZenoPainter.draw(canvas, 16f, (h - zw * ZenoPainter.ASPECT) / 2, zw, MomentFeeling.SLEEPY, ZenoSkin.ZEN)
            val pill = RectF(w - 14f - 64f, (h - 44f) / 2, w - 14f, (h + 44f) / 2)
            kit.pill(canvas, pill, "End", 0, t.text, border = t.outline)
            hit(pill, WidgetAction.QUIET_END)
            val col = 16f + zw + 12f
            val clockW = if (small) 82f else 96f
            if (!small) {
                val colW = pill.left - 12f - clockW - col
                kit.text(canvas, PRESET, col, h / 2 - 16f, WidgetKit.monoS, t.brand, colW)
                kit.progressBar(canvas, col, h / 2 + 6f, colW, 5f, progress, t.brandFill, t.brandSoft, t.outline)
                clock(Clock(pill.left - 10f - clockW, h / 2 - 18f, clockW, 36f, run.endsAt, 30f, t.brand))
            } else {
                clock(Clock(col, h / 2 - 16f, pill.left - 8f - col, 32f, run.endsAt, 26f, t.brand))
            }
            return@with result(spoken, WidgetAction.OPEN_TODAY)
        }
        ground(Surface.NIGHT, night(this, if (wide) w * 0.8f else w * 0.5f, h * 0.7f, maxOf(w, h) * 0.5f, WidgetLight.Moon(w - 30f, 32f, 11f), WidgetLight.Box(w * 0.55f, 56f, w - 10f, 92f)))
        val colW = if (wide) w * 0.56f else w - 28f
        kit.header(canvas, 14f, 14f, "Quiet for", t.brand, colW - 30f)
        clock(Clock(14f, 34f, colW, 48f, run.endsAt, 40f, t.brand))
        kit.progressBar(canvas, 14f, 94f, colW, 6f, progress, t.brandFill, t.brandSoft, t.outline)
        kit.paragraph(canvas, "$PRESET are quiet.", 14f, 110f, colW, WidgetKit.caption, t.text3, maxLines = 2)
        val pill = RectF(14f, h - 14f - 48f, 14f + colW, h - 14f)
        kit.pill(canvas, pill, "End session", withAlpha(t.bg, 0.5f), t.text, border = t.outline)
        hit(pill, WidgetAction.QUIET_END)
        if (wide) {
            val zw = minOf(w - colW - 40f, 150f)
            ZenoPainter.draw(canvas, w - 14f - zw, h - zw * 0.64f, zw, MomentFeeling.SLEEPY, ZenoSkin.ZEN)
        }
        result(spoken, WidgetAction.OPEN_TODAY)
    }

    private fun done(s: FaceScope): FaceResult = with(s) {
        val ended = data.quiet.ended!!
        val headline = "${ended.minutes} minutes of quiet."
        val sub = "Lovely. ${today(ended.sessionsToday)}"
        val surface = if (input.dark) Surface.INK else Surface.PAPER
        val spoken = "$headline $sub Tap Again for another ${data.quiet.minutes} minutes."
        if (rows == 1) {
            val zw = if (w < 330f) 44f else 56f
            ground(surface, lit(16f + zw / 2, h / 2, zw * 1.5f, hue = WidgetLight.Hue.ZEN, celebration = true, mood = WidgetLight.Mood.CALM))
            ZenoPainter.draw(canvas, 16f, (h - zw * ZenoPainter.ASPECT) / 2, zw, MomentFeeling.HAPPY)
            val pill = RectF(w - 14f - 80f, (h - 44f) / 2, w - 14f, (h + 44f) / 2)
            kit.pill(canvas, pill, "Again", t.surface2, t.text, border = t.outline)
            hit(pill, WidgetAction.QUIET_START)
            val col = 16f + zw + 12f
            val colW = pill.left - 10f - col
            kit.text(canvas, headline, col, h / 2 - 22f, WidgetKit.says.copy(line = 22f), t.text, colW)
            kit.text(canvas, sub, col, h / 2 + 1f, WidgetKit.caption, t.muted, colW)
            return@with result(spoken, WidgetAction.OPEN_TODAY)
        }
        ground(surface, lit(if (wide) w * 0.8f else w - 31f, if (wide) h - 40f else 26f, maxOf(w, h) * 0.5f, hue = WidgetLight.Hue.ZEN, celebration = true, mood = WidgetLight.Mood.CALM))
        val colW = if (wide) w * 0.56f else w - 28f
        kit.header(canvas, 14f, 14f, "Quiet", t.brand, colW)
        kit.paragraph(canvas, headline, 14f, 40f, colW, WidgetKit.says, t.text, maxLines = 2)
        kit.paragraph(canvas, sub, 14f, 84f, colW, WidgetKit.caption, t.muted, maxLines = 2)
        val pill = RectF(14f, h - 14f - 48f, 14f + colW, h - 14f)
        kit.pill(canvas, pill, "Again", t.surface2, t.text, border = t.outline, icon = WidgetKit.Icon.TIMER)
        hit(pill, WidgetAction.QUIET_START)
        if (wide) {
            val zw = minOf(w - colW - 40f, 150f)
            ZenoPainter.draw(canvas, w - 14f - zw, h - zw * 0.64f, zw, MomentFeeling.HAPPY)
        } else {
            // Two columns: Zeno beside the header, small and pleased.
            ZenoPainter.draw(canvas, w - 14f - 34f, 9f, 34f, MomentFeeling.HAPPY)
        }
        result(spoken, WidgetAction.OPEN_TODAY)
    }

    /** The night a session runs in: a crescent moon, a few pale stars, Zeno's green glow. */
    private fun night(s: FaceScope, gx: Float, gy: Float, gr: Float, moon: WidgetLight.Moon, stars: WidgetLight.Box) =
        WidgetLight.Spec(
            s.w, s.h, part = DayPart.NIGHT,
            glow = WidgetLight.Glow(WidgetLight.Hue.ZEN, gx, gy, gr),
            stars = 5, starBox = stars, amberStars = false, moon = moon
        )
}
