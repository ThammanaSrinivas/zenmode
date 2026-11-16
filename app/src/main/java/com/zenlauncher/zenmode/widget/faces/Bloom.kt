package com.zenlauncher.zenmode.widget.faces

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.zenlauncher.zenmode.ui.theme.WidgetPalette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One hour of the week in the bloom. What the phone saw in that hour, never a guess:
 *
 *  · [AWAY]: a waking hour the phone stayed down (the bloom's amber, its brightest seed);
 *  · [ON_PURPOSE] and [LIGHT]: the phone was used, mostly not on the apps that pull;
 *  · [UNPLANNED]: most of the hour went to the apps that pull (social, video, games);
 *  · [REST]: a night hour the phone stayed down;
 *  · [NOW] and [FUTURE]: this hour, and the hours still to come.
 */
enum class BloomCell { REST, AWAY, LIGHT, ON_PURPOSE, UNPLANNED, NOW, FUTURE, UNKNOWN;

    companion object {
        /** Waking hours start at 07:00 and end at 23:00. */
        const val WAKE_FROM = 7
        const val WAKE_UNTIL = 23

        fun classify(hour: Int, usedMinutes: Float, pullMinutes: Float): BloomCell = when {
            usedMinutes < 1f -> if (hour in WAKE_FROM until WAKE_UNTIL) AWAY else REST
            pullMinutes >= 5f && pullMinutes >= usedMinutes / 2 -> UNPLANNED
            usedMinutes >= 15f -> ON_PURPOSE
            else -> LIGHT
        }
    }
}

/**
 * The week in bloom (screens_c.py `bloom`): 168 hours, a seed each, on a golden-angle spiral.
 * Midnight at the heart, the evening at the rim. [cells] is day-major (Monday 00:00 first).
 */
internal object BloomPainter {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.8f }

    fun draw(canvas: Canvas, x: Float, y: Float, size: Float, cells: List<BloomCell>) {
        val c = size / 2
        val k = (c - 9) / sqrt(168f)
        for (h in 0 until 24) {
            for (d in 0 until 7) {
                val n = h * 7 + d
                val a = Math.toRadians(n * 137.508).toFloat()
                val r = k * sqrt(n + 0.6f)
                val s = (7.0f + 3.2f * (n / 167f)) * size / 252f
                val left = x + c + r * cos(a) - s / 2
                val top = y + c + r * sin(a) - s / 2
                val rect = RectF(left, top, left + s, top + s)
                when (val cell = cells.getOrElse(d * 24 + h) { BloomCell.FUTURE }) {
                    BloomCell.FUTURE -> {
                        stroke.color = WidgetPalette.bloomFuture
                        canvas.drawRoundRect(rect, s / 6, s / 6, stroke)
                    }
                    else -> {
                        fill.color = colorOf(cell)
                        canvas.drawRoundRect(rect, s / 6, s / 6, fill)
                    }
                }
            }
        }
    }

    private fun colorOf(cell: BloomCell): Int = when (cell) {
        BloomCell.REST -> WidgetPalette.bloomRest
        BloomCell.AWAY -> WidgetPalette.bloomAway
        BloomCell.LIGHT -> WidgetPalette.bloomLight
        BloomCell.ON_PURPOSE -> WidgetPalette.bloomOnPurpose
        BloomCell.UNPLANNED -> WidgetPalette.bloomUnplanned
        BloomCell.NOW -> WidgetPalette.bloomNow
        BloomCell.UNKNOWN -> WidgetPalette.bloomIdle
        BloomCell.FUTURE -> WidgetPalette.bloomFuture
    }
}
