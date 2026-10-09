package com.zenlauncher.zenmode.share

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** One figure under a poster's divider: a small mono label over a value. */
data class PosterStat(val label: String, val value: String)

/** A poster's text colours, picked for its ground: [primary] for type, [muted] for the caption,
 * [faint] for labels and rules, [accent] for the one stat worth a second look. */
class PosterInk(val primary: Int, val muted: Int, val faint: Int, val accent: Int)

/**
 * Everything a post-shaped card shares, top to bottom: the wordmark and a stamp; the card's own
 * picture in [artRect]; title; caption; a rule; up to three stats; the footer. Text is laid out
 * once, from the bottom of the card upward, so a two-line title or a three-line caption takes
 * its room from the picture and never runs into the stats.
 *
 * Arrival: the header drops in first, then — from the `start` a card passes to [drawText] —
 * the title rises, the caption follows, the rule draws across and the stats land one by one.
 */
class PosterChrome(
    private val kit: ShareKit,
    private val ink: PosterInk,
    private val stamp: String,
    title: String,
    caption: String,
    private val stats: List<PosterStat>,
    private val footer: String,
    format: ShareFormat = ShareFormat.POST
) {
    private val w = format.width.toFloat()
    private val h = format.height.toFloat()

    private val titlePaint = kit.paint(kit.clash, 92f, ink.primary, tracking = -0.02f)
    private val captionPaint = kit.paint(kit.geist, 37f, ink.muted, weight = 400)
    private val titleLayout = kit.layout(title, titlePaint, (w - MARGIN * 2).toInt(), lineSpacing = 0.98f, maxLines = 2)
    private val captionLayout = kit.layout(caption, captionPaint, (w - MARGIN * 2).toInt(), lineSpacing = 1.3f, maxLines = 3)

    private val stampPaint = kit.paint(kit.mono, 24f, ink.muted, tracking = 0.12f, align = Paint.Align.RIGHT)
    private val labelPaint = kit.paint(kit.mono, 22f, ink.faint, tracking = 0.14f)
    private val valuePaint = kit.paint(kit.mono, VALUE_SIZE, ink.primary, tracking = -0.02f)
    private val footerPaint = kit.paint(kit.mono, 23f, ink.faint, tracking = 0.14f)
    private val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2f }

    private val footerBaseline = h - 62f
    private val valueBaseline = footerBaseline - 88f
    private val labelBaseline = valueBaseline - 64f
    private val ruleY = labelBaseline - 46f
    private val captionTop = ruleY - 46f - captionLayout.height
    private val titleTop = captionTop - 20f - titleLayout.height

    /** Where the card's own picture goes: under the wordmark, above the title. */
    val artRect = RectF(0f, 150f, w, titleTop - 24f)

    fun drawHeader(canvas: Canvas, t: Float) {
        val p = ShareMotion.beat(t, 60, 520)
        canvas.save()
        canvas.translate(0f, -20f * (1f - p))
        kit.drawWordmark(canvas, MARGIN, 114f, 38f, ink.primary, alpha = p)
        stampPaint.color = withAlpha(ink.muted, p)
        canvas.drawText(stamp, w - MARGIN, 111f, stampPaint)
        canvas.restore()
    }

    /** Title, caption, rule, stats and footer, arriving from [start] ms. */
    fun drawText(canvas: Canvas, t: Float, start: Int) {
        rise(canvas, t, start, 34f) { p ->
            titlePaint.color = withAlpha(ink.primary, p)
            canvas.translate(MARGIN, titleTop)
            titleLayout.draw(canvas)
        }
        rise(canvas, t, start + 110, 24f) { p ->
            captionPaint.color = withAlpha(ink.muted, p)
            canvas.translate(MARGIN, captionTop)
            captionLayout.draw(canvas)
        }

        val drawn = ShareMotion.beat(t, start + 200, 520)
        rule.color = withAlpha(ink.faint, 0.6f)
        canvas.drawLine(MARGIN, ruleY, MARGIN + (w - MARGIN * 2) * drawn, ruleY, rule)

        if (stats.isNotEmpty()) {
            val column = (w - MARGIN * 2) / stats.size
            stats.forEachIndexed { i, stat ->
                rise(canvas, t, start + 280 + i * 80, 18f) { p ->
                    val x = MARGIN + i * column
                    labelPaint.color = withAlpha(ink.faint, p)
                    canvas.drawText(stat.label, x, labelBaseline, labelPaint)
                    valuePaint.color = withAlpha(if (i == 0) ink.accent else ink.primary, p)
                    // A long value (₹1,25,000) shrinks to its column rather than running into the next.
                    valuePaint.textSize = VALUE_SIZE
                    val room = column - 28f
                    val width = valuePaint.measureText(stat.value)
                    if (width > room) valuePaint.textSize = (VALUE_SIZE * room / width).coerceAtLeast(30f)
                    canvas.drawText(stat.value, x, valueBaseline, valuePaint)
                }
            }
        }

        val f = ShareMotion.beat(t, start + 420, 420)
        footerPaint.color = withAlpha(ink.faint, f)
        footerPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(SITE, MARGIN, footerBaseline, footerPaint)
        footerPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(footer, w - MARGIN, footerBaseline, footerPaint)
    }

    /** Runs [block] faded in and risen [distance] px into place over one beat from [at]. */
    private inline fun rise(canvas: Canvas, t: Float, at: Int, distance: Float, block: (Float) -> Unit) {
        val p = ShareMotion.beat(t, at, 560)
        if (p <= 0f) return
        canvas.save()
        canvas.translate(0f, distance * (1f - p))
        block(p)
        canvas.restore()
    }

    companion object {
        const val MARGIN = 76f
        const val VALUE_SIZE = 54f
        const val SITE = "ZENMODEOS.COM"
    }
}
