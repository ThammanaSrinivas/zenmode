package com.zenlauncher.zenmode.recap

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.zenlauncher.zenmode.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The downloadable weekly report (PRO): one A4 page with the week's numbers, the promise
 * verdict, a daily chart, top apps and the recap's takeaway. Drawn with [PdfDocument] so
 * it needs no extra dependency and renders identically on every device.
 */
object RecapReport {

    fun fileName(recap: WeeklyRecap) = "ZenMode-OS-week-${recap.weekStart}.pdf"

    /**
     * Saves straight to Downloads (Android 10+). Returns the file's Uri, or null when the
     * platform needs the caller to pick a location instead (Android 9).
     */
    fun saveToDownloads(context: Context, recap: WeeklyRecap): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return saveViaMediaStore(context, recap)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(context: Context, recap: WeeklyRecap): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName(recap))
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) {
            "Couldn't create the report in Downloads"
        }
        try {
            resolver.openOutputStream(uri).use { out -> write(context, recap, requireNotNull(out)) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }

    /**
     * Opens the system share sheet for the week. With [attachPdf] (PRO) the report PDF is
     * rendered into the share cache and attached; otherwise a short text summary goes out.
     * Throws if the PDF can't be written; the caller decides how to tell the user.
     */
    suspend fun share(context: Context, recap: WeeklyRecap, attachPdf: Boolean) {
        val summary = "My week in Zen (${recap.rangeLabel()}): ${formatMinutes(recap.totalMinutes)} on my phone, " +
            "promise kept ${recap.daysKept} of 7 days. Tracked with ZenMode OS: zenmodeos.com"
        val send = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, "My week in Zen · ${recap.rangeLabel()}")
            putExtra(Intent.EXTRA_TEXT, summary)
        }
        if (attachPdf) {
            val file = withContext(Dispatchers.IO) {
                val folder = File(context.cacheDir, SHARE_FOLDER).apply { mkdirs() }
                File(folder, fileName(recap)).also { f -> f.outputStream().use { write(context, recap, it) } }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            send.type = "application/pdf"
            send.putExtra(Intent.EXTRA_STREAM, uri)
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            send.type = "text/plain"
        }
        context.startActivity(Intent.createChooser(send, "Share weekly report"))
    }

    /** Matches the cache-path in res/xml/file_paths.xml. */
    private const val SHARE_FOLDER = "shared_reports"

    /** Writes the PDF to [out] (e.g. a document the user picked). */
    fun write(context: Context, recap: WeeklyRecap, out: OutputStream) {
        val document = PdfDocument()
        try {
            val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
            Painter(context, page.canvas, recap).draw()
            document.finishPage(page)
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    private const val PAGE_W = 595 // A4 at 72 dpi
    private const val PAGE_H = 842

    private class Painter(private val context: Context, private val c: Canvas, private val recap: WeeklyRecap) {
        private val clash = font(R.font.clash_display_medium, Typeface.DEFAULT_BOLD)
        private val geist = font(R.font.geist_variable, Typeface.DEFAULT)
        private val mono = font(R.font.departure_mono_regular, Typeface.MONOSPACE)
        private val margin = 40f
        private val width = PAGE_W - margin * 2
        private val kept = recap.outcome == RecapOutcome.KEPT

        fun draw() {
            c.drawColor(color(R.color.paper_base))
            header()
            var y = 170f
            y = stats(y)
            y = verdict(y + 18f)
            y = chart(y + 26f)
            y = topApps(y + 26f)
            takeaway(y + 22f)
            footer()
        }

        private fun header() {
            c.drawRect(0f, 0f, PAGE_W.toFloat(), 130f, fill(R.color.zen_900))
            val zen = text(clash, 26f, android.graphics.Color.WHITE)
            c.drawText("ZenMode ", margin, 62f, zen)
            val osX = margin + zen.measureText("ZenMode ")
            val os = text(clash, 26f, android.graphics.Color.WHITE).apply {
                shader = LinearGradient(
                    osX, 62f, osX + measureText("OS"), 40f,
                    intArrayOf(color(R.color.score_grad_start), color(R.color.score_grad_mid), color(R.color.score_orange)),
                    floatArrayOf(0.1f, 0.5f, 0.9f), Shader.TileMode.CLAMP
                )
            }
            c.drawText("OS", osX, 62f, os)
            c.drawText(
                "WEEKLY REPORT  ·  ${recap.rangeLabel().uppercase()}, ${recap.weekEnd.year}",
                margin, 92f, text(mono, 10f, color(R.color.zen_100)).apply { letterSpacing = 0.12f }
            )
            val verdict = if (kept) "PROMISE KEPT" else "PROMISE MISSED"
            val chip = text(mono, 10f, color(if (kept) R.color.zen_900 else R.color.ember_700))
            val chipW = chip.measureText(verdict) + 20f
            val chipRect = RectF(PAGE_W - margin - chipW, 46f, PAGE_W - margin, 68f)
            c.drawRoundRect(chipRect, 11f, 11f, fill(if (kept) R.color.zen_300 else R.color.ember_on))
            c.drawText(verdict, chipRect.left + 10f, 61f, chip)
        }

        private fun stats(top: Float): Float {
            val change = recap.previousWeekTotalMinutes?.let { recap.totalMinutes - it }
            val items = listOf(
                "SCREEN TIME" to formatMinutes(recap.totalMinutes),
                "DAILY AVERAGE" to formatMinutes(recap.dailyAverageMinutes),
                "DAYS KEPT" to "${recap.daysKept}/7",
                "VS LAST WEEK" to when {
                    change == null -> "-"
                    change <= 0 -> "−${formatMinutes(-change)}"
                    else -> "+${formatMinutes(change)}"
                }
            )
            val cellW = (width - 3 * 10f) / 4
            items.forEachIndexed { i, (label, value) ->
                val x = margin + i * (cellW + 10f)
                c.drawRoundRect(RectF(x, top, x + cellW, top + 74f), 12f, 12f, fill(R.color.paper_white))
                c.drawText(label, x + 12f, top + 22f, text(mono, 8.5f, color(R.color.stone_500)).apply { letterSpacing = 0.1f })
                c.drawText(value, x + 12f, top + 56f, text(mono, 20f, color(R.color.ink_surface)))
            }
            return top + 74f
        }

        private fun verdict(top: Float): Float {
            val rect = RectF(margin, top, margin + width, top + 64f)
            c.drawRoundRect(rect, 12f, 12f, fill(if (kept) R.color.zen_050 else R.color.ember_on))
            val title = if (kept) "Invest unlocked for the week" else "Invest stays locked this week"
            val body = if (kept) {
                "You stayed under ${recap.promiseHours}h on ${recap.daysKept} of 7 days (${recap.daysToUnlock} needed)."
            } else {
                "${recap.daysKept} of 7 days under ${recap.promiseHours}h, ${recap.daysToUnlock} needed. " +
                    "Missed days ran ${formatMinutes(recap.minutesOverPromise)} over. Nothing is taken away."
            }
            c.drawText(title, margin + 16f, top + 26f, text(clash, 15f, color(if (kept) R.color.zen_900 else R.color.ember_700)))
            c.drawText(body, margin + 16f, top + 47f, text(geist, 10.5f, color(R.color.stone_600)))
            return top + 64f
        }

        private fun chart(top: Float): Float {
            c.drawText("DAILY SCREEN TIME", margin, top, text(mono, 9f, color(R.color.stone_500)).apply { letterSpacing = 0.1f })
            val chartTop = top + 14f
            val chartH = 150f
            val base = chartTop + chartH
            val maxMinutes = maxOf(recap.days.maxOf { it.screenTimeMinutes }, recap.promiseHours * 60L, 1L).toFloat()
            val slot = width / 7
            val barW = slot * 0.46f
            recap.days.forEachIndexed { i, day ->
                val h = chartH * day.screenTimeMinutes / maxMinutes
                val x = margin + i * slot + (slot - barW) / 2
                c.drawRoundRect(RectF(x, base - h, x + barW, base), 6f, 6f, fill(if (day.keptPromise) R.color.zen_500 else R.color.ember_500))
                val value = text(mono, 8.5f, color(R.color.ink_surface)).apply { textAlign = Paint.Align.CENTER }
                c.drawText(formatMinutes(day.screenTimeMinutes), x + barW / 2, base - h - 5f, value)
                val label = text(mono, 9f, color(R.color.stone_500)).apply { textAlign = Paint.Align.CENTER }
                c.drawText(day.shortDayName().uppercase(), x + barW / 2, base + 15f, label)
            }
            val promiseY = base - chartH * recap.promiseHours * 60f / maxMinutes
            c.drawLine(margin, promiseY, margin + width, promiseY, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = color(R.color.stone_400)
                strokeWidth = 1f
                pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
            })
            c.drawText("promise ${recap.promiseHours}h", margin + width, promiseY - 4f,
                text(geist, 8.5f, color(R.color.stone_500)).apply { textAlign = Paint.Align.RIGHT })
            return base + 20f
        }

        private fun topApps(top: Float): Float {
            c.drawText("WHERE THE TIME WENT", margin, top, text(mono, 9f, color(R.color.stone_500)).apply { letterSpacing = 0.1f })
            var y = top + 20f
            val apps = recap.topApps.take(5)
            if (apps.isEmpty()) {
                c.drawText("App breakdown wasn't available on this device.", margin, y, text(geist, 10.5f, color(R.color.stone_600)))
                return y + 6f
            }
            val total = recap.totalMinutes.coerceAtLeast(1)
            apps.forEach { app ->
                val share = app.minutes.toFloat() / total
                c.drawText(app.label, margin, y + 10f, text(geist, 11f, color(R.color.ink_surface)))
                val barX = margin + 150f
                val barMax = width - 150f - 80f
                c.drawRoundRect(RectF(barX, y + 2f, barX + barMax, y + 12f), 5f, 5f, fill(R.color.paper_sunk))
                c.drawRoundRect(RectF(barX, y + 2f, barX + barMax * share.coerceIn(0.02f, 1f), y + 12f), 5f, 5f, fill(R.color.zen_700))
                c.drawText(formatMinutes(app.minutes), margin + width, y + 10f,
                    text(mono, 10f, color(R.color.ink_surface)).apply { textAlign = Paint.Align.RIGHT })
                y += 22f
            }
            return y
        }

        private fun takeaway(top: Float) {
            val story = RecapStory.cards(recap)
            val (title, body) = when (val card = story[3]) {
                is RecapCard.Obstacle -> "What got in the way" to "${card.detail} ${card.tip}"
                is RecapCard.Highlight -> card.headline to "${card.value}. ${card.caption}"
                else -> return
            }
            val extras = buildList {
                if (recap.lateNightMinutes > 0) add("After 10 pm: ${formatMinutes(recap.lateNightMinutes)}")
                recap.pickups?.let { add("Pickups: $it") }
                recap.calmestDay?.let { add("Calmest day: ${it.fullDayName()}") }
            }.joinToString("   ·   ")
            val rect = RectF(margin, top, margin + width, top + 96f)
            c.drawRoundRect(rect, 12f, 12f, fill(R.color.paper_white))
            c.drawText(title, margin + 16f, top + 24f, text(clash, 14f, color(R.color.ink_surface)))
            var y = top + 44f
            wrap(body, text(geist, 10.5f, color(R.color.stone_600)), width - 32f).take(3).forEach { line ->
                c.drawText(line, margin + 16f, y, text(geist, 10.5f, color(R.color.stone_600)))
                y += 14f
            }
            if (extras.isNotEmpty()) c.drawText(extras, margin + 16f, top + 86f, text(mono, 8.5f, color(R.color.stone_500)))
        }

        private fun footer() {
            val generated = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
            val small = text(geist, 8f, color(R.color.stone_500))
            c.drawText("Generated on this phone by ZenMode OS on $generated. Screen time comes from Android's usage data.",
                margin, PAGE_H - 44f, small)
            c.drawText("ZenMode never holds or receives your money. Investing happens in your own broker's app. Not investment advice.",
                margin, PAGE_H - 30f, small)
        }

        // ── helpers ──
        private fun wrap(textValue: String, paint: Paint, maxWidth: Float): List<String> {
            val lines = mutableListOf<String>()
            var line = ""
            for (word in textValue.split(" ")) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                    lines += line
                    line = word
                } else {
                    line = candidate
                }
            }
            if (line.isNotEmpty()) lines += line
            return lines
        }

        private fun font(res: Int, fallback: Typeface): Typeface =
            runCatching { ResourcesCompat.getFont(context, res) }.getOrNull() ?: fallback

        private fun color(@ColorRes res: Int) = ContextCompat.getColor(context, res)

        private fun fill(@ColorRes res: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = color(res) }

        private fun text(face: Typeface, size: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = face
            textSize = size
            this.color = color
        }
    }
}
