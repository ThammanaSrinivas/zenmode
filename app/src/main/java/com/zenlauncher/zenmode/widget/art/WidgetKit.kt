package com.zenlauncher.zenmode.widget.art

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.PathParser
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.week.DayMark
import com.zenlauncher.zenmode.share.withAlpha
import com.zenlauncher.zenmode.ui.theme.WidgetPalette
import com.zenlauncher.zenmode.ui.theme.WidgetTokens

/**
 * Brand type and the small pieces every widget face is built from, drawn in dp on a canvas
 * already scaled to the device's density.
 *
 * Why bitmaps at all: a widget is inflated by another launcher in a restricted context, where
 * neither Glance nor a classic layout can load bundled fonts (the platform's RemoteViews rules). Our
 * own process can, so the whole face is drawn here, in Departure Mono, Clash Display and Geist,
 * and handed over as one image with a spoken description. The Quiet countdown is the one
 * exception: it ticks inside the host as a system Chronometer.
 *
 * The type styles of the widget design map onto [Style].
 */
class WidgetKit(context: Context) {

    val clash: Typeface = font(context, R.font.clash_display_medium, Typeface.DEFAULT_BOLD)
    val geist: Typeface = font(context, R.font.geist_variable, Typeface.DEFAULT)
    val mono: Typeface = font(context, R.font.departure_mono_regular, Typeface.MONOSPACE)
    private val mark: Bitmap? = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.zen_mark) }.getOrNull()

    /** One type style: face, size and line height in dp, tracking in em, Geist weight, caps. */
    data class Style(val face: Face, val size: Float, val line: Float, val tracking: Float = 0f, val weight: Int = 400, val caps: Boolean = false)

    enum class Face { CLASH, GEIST, MONO }

    companion object {
        val metricXXL = Style(Face.MONO, 64f, 64f, -0.02f)
        val metricXL = Style(Face.MONO, 48f, 52f, -0.01f)
        val metricL = Style(Face.MONO, 32f, 36f, -0.01f)
        val metric = Style(Face.MONO, 20f, 24f, 0.02f)
        val metricS = Style(Face.MONO, 14f, 20f, 0.02f)
        val monoLabel = Style(Face.MONO, 11f, 16f, 0.1f, caps = true)
        val monoS = Style(Face.MONO, 10f, 14f, 0.1f, caps = true)
        val headline = Style(Face.CLASH, 24f, 29f, -0.01f)
        val title22 = Style(Face.CLASH, 22f, 27f, -0.01f)
        val callout = Style(Face.GEIST, 14f, 20f)
        val calloutBold = Style(Face.GEIST, 14f, 20f, weight = 600)
        val caption = Style(Face.GEIST, 13f, 18f)
        val label = Style(Face.GEIST, 12f, 16f, weight = 500)
        val says = Style(Face.GEIST, 16f, 20f, weight = 600)
        val button = Style(Face.GEIST, 14f, 20f, weight = 600)

        private fun font(context: Context, id: Int, fallback: Typeface): Typeface =
            runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: fallback

        // The brand flame, in a 2 0 29 33 viewBox.
        private const val FLAME_PATH = "M16.4 32.35C7.14 32.35 4.14 24.97 4.14 21.4C4.14 18.93 4.93 17.32 5.47 16.5C5.47 17.78 5.8 18.9 6.38 19.55" +
            "C6.54 19.73 6.73 19.88 6.94 19.98C7.16 20.09 7.39 20.14 7.63 20.15C8.66 20.15 9.36 19.39 9.36 18.24C9.36 17.53 8.92 17.05 8.46 16.54" +
            "C7.9 15.92 7.32 15.28 7.32 14.1C7.32 12.67 8.19 11.31 8.77 10.56C8.77 13.01 10.42 14.04 11.38 14.25L11.76 14.33L11.7 13.95" +
            "C11.7 13.93 11.38 11.55 13.26 9.21C13.49 8.92 13.74 8.62 13.99 8.33C15.3 6.77 16.66 5.15 16.66 3.44C16.65 2.47 16.45 1.51 16.06 0.62" +
            "C20.12 2.85 20.06 5.95 19.63 7.65C19.53 8.09 19.02 10.32 19.86 11.39C20.01 11.58 20.2 11.73 20.41 11.83C20.63 11.93 20.86 11.98 21.1 11.97" +
            "C21.98 11.97 22.69 11.43 23.14 10.42C23.27 10.12 23.37 9.81 23.44 9.5C25.42 11.73 24.92 13.77 24.13 15.11C23.39 16.34 23.19 17.6 23.6 18.33" +
            "C23.71 18.53 23.89 18.7 24.1 18.81C24.31 18.92 24.54 18.97 24.78 18.96C25 18.96 25.22 18.92 25.42 18.83C25.63 18.74 25.81 18.61 25.95 18.44" +
            "C26.59 17.73 26.71 16.44 26.71 15.57C27.51 16.68 28.49 18.39 28.49 20.8C28.49 22.7 27.79 25.61 25.85 28.08C24.31 30.03 21.42 32.35 16.4 32.35Z"
        private const val FLAME_CORE = "M16.4 30.2c-4.2 0-6.2-3.2-6.2-5.6 0-2.4 1.6-3.6 2.8-5.2.5 1.9 1.7 2.6 2.8 2.7-.4-2.3.4-4.3 2.1-5.9" +
            ".3 2.9 4.5 4.3 4.5 8.4 0 2.6-2 5.6-6 5.6z"
        private val flame: Path = PathParser.createPathFromPathData(FLAME_PATH)
        private val flameCore: Path = PathParser.createPathFromPathData(FLAME_CORE)
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun paint(style: Style, color: Int, size: Float = style.size): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = when (style.face) {
                Face.CLASH -> clash
                Face.GEIST -> geist
                Face.MONO -> mono
            }
            textSize = size
            this.color = color
            letterSpacing = style.tracking
            // Geist ships as one variable file; its weight is an axis, not a separate face.
            if (style.face == Face.GEIST) fontVariationSettings = "'wght' ${style.weight}"
        }

    private fun shown(text: String, style: Style) = if (style.caps) text.uppercase() else text

    fun width(text: String, style: Style, size: Float = style.size): Float = paint(style, 0, size).measureText(shown(text, style))

    /**
     * One line of text whose CSS line box starts at [top], the way the boards position text.
     * Shrinks to fit [maxWidth] down to 80% of its size before it would clip, then ellipsises.
     * Returns the width drawn.
     */
    fun text(
        canvas: Canvas, text: String, x: Float, top: Float, style: Style, color: Int,
        maxWidth: Float = Float.MAX_VALUE, align: Paint.Align = Paint.Align.LEFT
    ): Float {
        var s = shown(text, style)
        var size = style.size
        var p = paint(style, color, size)
        while (p.measureText(s) > maxWidth && size > style.size * 0.8f) {
            size -= 0.5f
            p = paint(style, color, size)
        }
        if (p.measureText(s) > maxWidth) s = TextUtils.ellipsize(s, p, maxWidth, TextUtils.TruncateAt.END).toString()
        p.textAlign = align
        val fm = p.fontMetrics
        val lineHeight = style.line * size / style.size
        val baseline = top + (lineHeight - (fm.descent - fm.ascent)) / 2 - fm.ascent
        canvas.drawText(s, x, baseline, p)
        return p.measureText(s)
    }

    /** Wrapped text at most [maxLines] lines; returns the height used. */
    fun paragraph(
        canvas: Canvas, text: String, x: Float, top: Float, width: Float, style: Style, color: Int,
        maxLines: Int = 2, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Float {
        val layout = layout(text, style, color, width, maxLines, align)
        canvas.save()
        canvas.translate(x, top)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    /** One sentence whose first words are set in 600 and [leadColor]: "**2 kept days** to open gold pay". */
    fun leadParagraph(
        canvas: Canvas, lead: String, rest: String, x: Float, top: Float, width: Float, style: Style,
        leadColor: Int, color: Int, maxLines: Int = 2
    ): Float {
        val text = android.text.SpannableStringBuilder(lead).append(rest)
        text.setSpan(LeadSpan(leadColor), 0, lead.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val p = paint(style, color)
        val fm = p.fontMetrics
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, p, width.toInt().coerceAtLeast(1))
            .setBreakStrategy(LineBreaker.BREAK_STRATEGY_BALANCED)
            .setLineSpacing(style.line - (fm.descent - fm.ascent), 1f)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(x, top)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    /** Geist at 600 in its own colour, for a sentence's lead. */
    private class LeadSpan(private val color: Int) : android.text.style.MetricAffectingSpan() {
        override fun updateDrawState(tp: TextPaint) {
            updateMeasureState(tp)
            tp.color = color
        }

        override fun updateMeasureState(tp: TextPaint) {
            tp.fontVariationSettings = "'wght' 600"
        }
    }

    fun layout(text: String, style: Style, color: Int, width: Float, maxLines: Int, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout {
        val p = paint(style, color)
        val s = shown(text, style)
        val fm = p.fontMetrics
        val extra = style.line - (fm.descent - fm.ascent)
        return StaticLayout.Builder.obtain(s, 0, s.length, p, width.toInt().coerceAtLeast(1))
            .setAlignment(align)
            .setBreakStrategy(LineBreaker.BREAK_STRATEGY_BALANCED)
            .setLineSpacing(extra, 1f)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
    }

    // ── Brand ───────────────────────────────────────────────────────

    /** The app mark, as every widget's header opens with it (widget quality bar WL-3). */
    fun mark(canvas: Canvas, x: Float, y: Float, size: Float = 13f) {
        val m = mark ?: return
        canvas.drawBitmap(m, null, RectF(x, y, x + size, y + size), fill)
    }

    /** The mark and a short mono label. Returns where the header ends. */
    fun header(canvas: Canvas, x: Float, y: Float, label: String, color: Int, maxWidth: Float = Float.MAX_VALUE): Float {
        mark(canvas, x, y + 0.5f, 13f)
        return x + 19f + text(canvas, label, x + 19f, y, monoS, color, maxWidth - 19f)
    }

    // ── Week cells ──────────────────────────────────────────────────

    /** The seven-cell strip (zds.py `week_cells`). Returns its width. */
    fun cells(canvas: Canvas, marks: List<DayMark>, x: Float, y: Float, size: Float, gap: Float, t: WidgetTokens): Float {
        val r = maxOf(1.5f, size / 6)
        marks.forEachIndexed { i, m ->
            val left = x + i * (size + gap)
            val rect = RectF(left, y, left + size, y + size)
            when (m) {
                DayMark.GOOD -> solid(canvas, rect, r, t.amber)
                DayMark.QUIET -> solid(canvas, rect, r, t.outline)
                DayMark.TODAY -> {
                    solid(canvas, rect, r, t.amberTint)
                    outline(canvas, rect, r, t.amberText, 1.5f)
                }
                DayMark.NO_DATA -> outline(canvas, rect, r, t.hair, 1f)
                DayMark.TO_COME -> outline(canvas, rect, r, t.outline, 1f)
            }
        }
        return marks.size * size + (marks.size - 1) * gap
    }

    fun cellsWidth(count: Int, size: Float, gap: Float) = count * size + (count - 1) * gap

    /** Day cells narrower than this print their score underneath instead of inside. */
    private val SCORE_INSIDE_MIN = 30f

    /**
     * The week's days with their letters and scores (screens_c.py `day_cells`), spread across
     * [width]. [scores] are display strings ("7.4", "" for none).
     */
    fun dayCells(
        canvas: Canvas, marks: List<DayMark>, scores: List<String>, x: Float, y: Float, width: Float,
        cw: Float, ch: Float, t: WidgetTokens, onGold: Boolean
    ) {
        val step = if (marks.size > 1) (width - cw) / (marks.size - 1) else 0f
        marks.forEachIndexed { i, m ->
            val left = x + i * step
            text(canvas, "MTWTFSS"[i].toString(), left + cw / 2, y, monoS, if (m == DayMark.TODAY) t.text else t.mono, align = Paint.Align.CENTER)
            val rect = RectF(left, y + 18f, left + cw, y + 18f + ch)
            val r = if (cw > 36) 10f else if (cw >= SCORE_INSIDE_MIN) 8f else cw / 5
            val numberColor = when (m) {
                DayMark.GOOD -> {
                    solid(canvas, rect, r, t.amber)
                    if (onGold) WidgetPalette.goldSky[1] else WidgetPalette.onAmber
                }
                DayMark.QUIET -> {
                    solid(canvas, rect, r, t.sunk)
                    t.muted
                }
                DayMark.TODAY -> {
                    solid(canvas, rect, r, t.amberTint)
                    outline(canvas, rect, r, t.amberText, 1.5f)
                    t.text
                }
                DayMark.NO_DATA -> {
                    outline(canvas, rect, r, t.hair, 1f)
                    t.muted
                }
                DayMark.TO_COME -> {
                    outline(canvas, rect, r, t.outline, 1f)
                    t.muted
                }
            }
            val score = scores.getOrElse(i) { "" }
            if (score.isNotEmpty()) {
                if (cw < SCORE_INSIDE_MIN) {
                    // Too small to hold a number: it sits underneath, as on the week strip.
                    val below = if (m == DayMark.GOOD || m == DayMark.TODAY) t.brand else t.mono
                    text(canvas, score, rect.centerX(), rect.bottom + 3f, Style(Face.MONO, 10f, 14f), below, align = Paint.Align.CENTER)
                } else {
                    val size = if (cw < 40) 11f else 12f
                    text(canvas, score, rect.centerX(), rect.centerY() - 8f, Style(Face.MONO, size, 16f), numberColor, cw - 4f, Paint.Align.CENTER)
                }
            }
        }
    }

    // ── Score bar ───────────────────────────────────────────────────

    /** The score bar (screens_a.py `score_bar`): ember to green, a tick at 6.0, a marker at the score. */
    fun scoreBar(canvas: Canvas, x: Float, y: Float, w: Float, scoreTenths: Int?, t: WidgetTokens, label: Boolean = false) {
        fill.color = android.graphics.Color.BLACK
        fill.shader = LinearGradient(x, 0f, x + w, 0f, WidgetPalette.scoreTrack, WidgetPalette.scoreTrackStops, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(x, y + 4, x + w, y + 12), 4f, 4f, fill)
        fill.shader = null
        fill.color = t.bg
        val tick = x + w * 0.6f
        canvas.drawRoundRect(RectF(tick - 1, y, tick + 1, y + 16), 1f, 1f, fill)
        if (scoreTenths != null) {
            val mx = x + w * (scoreTenths.coerceIn(0, 100) / 100f)
            val r = RectF(mx - 3, y - 1, mx + 3, y + 17)
            fill.color = t.text
            canvas.drawRoundRect(r, 3f, 3f, fill)
            outline(canvas, r, 3f, t.surface, 1.5f)
        }
        if (label) text(canvas, "6.0 · good day", tick, y + 20, monoS, t.mono, align = Paint.Align.CENTER)
    }

    // ── Glyphs ──────────────────────────────────────────────────────

    fun flame(canvas: Canvas, x: Float, y: Float, size: Float, color: Int = WidgetPalette.flame, core: Int = WidgetPalette.flameCore) {
        canvas.save()
        canvas.translate(x, y)
        val s = size / 33f
        canvas.scale(s, s)
        canvas.translate(-2f, 0f)
        fill.color = color
        canvas.drawPath(flame, fill)
        fill.color = core
        canvas.drawPath(flameCore, fill)
        canvas.restore()
    }

    /** The gold coin (zds.py `coin`). */
    fun coin(canvas: Canvas, x: Float, y: Float, size: Float) {
        val s = size / 28f
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(s, s)
        fill.color = android.graphics.Color.BLACK
        fill.shader = LinearGradient(6f, 3f, 22f, 26f, WidgetPalette.coin, WidgetPalette.coinStops, Shader.TileMode.CLAMP)
        canvas.drawCircle(14f, 14f, 12.5f, fill)
        fill.shader = null
        line.color = withAlpha(WidgetPalette.coinInk, 0.45f)
        line.strokeWidth = 1.2f
        canvas.drawCircle(14f, 14f, 9f, line)
        solid(canvas, RectF(10.5f, 10.5f, 17.5f, 17.5f), 7f / 6, WidgetPalette.coinInk)
        canvas.restore()
    }

    enum class Icon { TIMER, LOCK, PLUS, PHONE }

    /** The line icons the widgets use, in a 24-unit box. */
    fun icon(canvas: Canvas, icon: Icon, x: Float, y: Float, size: Float, color: Int, stroke: Float = 1.75f) {
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(size / 24f, size / 24f)
        line.color = color
        line.strokeWidth = stroke
        when (icon) {
            Icon.TIMER -> {
                canvas.drawCircle(12f, 13f, 7.5f, line)
                canvas.drawLine(12f, 13f, 12f, 9f, line)
                canvas.drawLine(9.5f, 3f, 14.5f, 3f, line)
            }
            Icon.LOCK -> {
                canvas.drawRoundRect(RectF(6f, 11f, 18f, 20f), 2f, 2f, line)
                canvas.drawPath(Path().apply { moveTo(9f, 11f); lineTo(9f, 8f); arcTo(RectF(9f, 5f, 15f, 11f), 180f, 180f); lineTo(15f, 11f) }, line)
            }
            Icon.PHONE -> {
                canvas.drawRoundRect(RectF(7f, 3f, 17f, 21f), 2.5f, 2.5f, line)
                canvas.drawLine(11f, 17.8f, 13f, 17.8f, line)
            }
            Icon.PLUS -> {
                canvas.drawLine(12f, 5f, 12f, 19f, line)
                canvas.drawLine(5f, 12f, 19f, 12f, line)
            }
        }
        canvas.restore()
    }

    // ── Controls ────────────────────────────────────────────────────

    /** A pill button. [border] draws an outline in that colour. Returns its rect. */
    fun pill(
        canvas: Canvas, rect: RectF, label: String, bg: Int, fg: Int,
        border: Int? = null, icon: Icon? = null, style: Style = button
    ): RectF {
        val r = rect.height() / 2
        if (bg != 0) solid(canvas, rect, r, bg)
        if (border != null) outline(canvas, RectF(rect.left + 0.5f, rect.top + 0.5f, rect.right - 0.5f, rect.bottom - 0.5f), r, border, 1f)
        val iconSize = 18f
        val textW = width(label, style)
        val total = textW + if (icon != null) iconSize + 6f else 0f
        var left = rect.centerX() - total / 2
        if (icon != null) {
            icon(canvas, icon, left, rect.centerY() - iconSize / 2, iconSize, fg)
            left += iconSize + 6f
        }
        text(canvas, label, left, rect.centerY() - style.line / 2, style, fg)
        return rect
    }

    /** A smooth capsule filled to [fraction], its fill lit a little brighter at the leading end. */
    fun progressBar(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, fraction: Float, from: Int, to: Int, track: Int) {
        solid(canvas, RectF(x, y, x + w, y + h), h / 2, track)
        val f = fraction.coerceIn(0f, 1f)
        if (f <= 0f) return
        val end = x + maxOf(h, w * f)
        fill.color = android.graphics.Color.BLACK
        fill.shader = LinearGradient(x, 0f, end, 0f, from, to, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(x, y, end, y + h), h / 2, h / 2, fill)
        fill.shader = null
    }

    /** A soft contact shadow under something that stands on the widget's floor. */
    fun softShadow(canvas: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, color: Int, alpha: Float) {
        canvas.save()
        canvas.scale(1f, ry / rx, cx, cy)
        fill.color = android.graphics.Color.BLACK
        fill.shader = android.graphics.RadialGradient(cx, cy, rx, withAlpha(color, alpha), withAlpha(color, 0f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, rx, fill)
        fill.shader = null
        canvas.restore()
    }

    /** A row of [total] cells with [lit] of them on: a session's progress, the gold gate. */
    fun progressCells(canvas: Canvas, x: Float, y: Float, w: Float, total: Int, lit: Int, h: Float, on: Int, off: Int, gap: Float = 3f) {
        val cw = (w - gap * (total - 1)) / total
        for (i in 0 until total) {
            val left = x + i * (cw + gap)
            solid(canvas, RectF(left, y, left + cw, y + h), minOf(2f, h / 3), if (i < lit) on else off)
        }
    }

    /** A round avatar with one letter. */
    fun avatar(canvas: Canvas, cx: Float, cy: Float, d: Float, letter: String, bg: Int, fg: Int) {
        fill.color = bg
        canvas.drawCircle(cx, cy, d / 2, fill)
        text(canvas, letter, cx, cy - 8f, Style(Face.GEIST, 12f, 16f, weight = 600), fg, align = Paint.Align.CENTER)
    }

    /** A small tag ("▲ 0.3"). Returns its width. */
    fun tag(canvas: Canvas, label: String, right: Float, y: Float, bg: Int, fg: Int): Float {
        val style = Style(Face.MONO, 10f, 14f, 0.04f)
        val w = width(label, style) + 14f
        solid(canvas, RectF(right - w, y, right, y + 20f), 6f, bg)
        text(canvas, label, right - w + 7f, y + 3f, style, fg)
        return w
    }

    fun solid(canvas: Canvas, rect: RectF, r: Float, color: Int) {
        fill.shader = null
        fill.color = color
        canvas.drawRoundRect(rect, r, r, fill)
    }

    fun outline(canvas: Canvas, rect: RectF, r: Float, color: Int, width: Float) {
        line.color = color
        line.strokeWidth = width
        val inset = width / 2
        canvas.drawRoundRect(RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset), r, r, line)
    }

    /** A soft gradient fill, for the gold gate's lit cells. */
    fun gradientBar(canvas: Canvas, rect: RectF, r: Float, from: Int, to: Int) {
        fill.color = android.graphics.Color.BLACK
        fill.shader = LinearGradient(rect.left, 0f, rect.right, 0f, from, to, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, r, r, fill)
        fill.shader = null
    }
}
