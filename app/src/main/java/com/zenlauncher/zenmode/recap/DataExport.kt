package com.zenlauncher.zenmode.recap

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.zenlauncher.zenmode.coreapi.ZenScore
import java.io.File
import java.time.LocalDate

/**
 * Pro "Export data": every day ZenMode still remembers (see [RecapStore.RETENTION_DAYS]) as one
 * CSV row, handed to the share sheet. Built on-device from the same [DayRecord]s the recaps
 * read — nothing is fetched and nothing leaves the phone unless the user shares it.
 */
object DataExport {

    private const val SHARE_FOLDER = "shared_reports"

    val HEADER = listOf(
        "date",
        "screen_time_minutes",
        "promise_hours",
        "kept_promise",
        "zen_score",
        "pickups",
        "late_night_minutes",
        "top_app",
        "top_app_minutes"
    )

    /** Oldest day first, so the file reads like a diary. Pure, for tests. */
    fun csv(days: Collection<DayRecord>): String = buildString {
        appendLine(HEADER.joinToString(","))
        days.sortedBy { it.date }.forEach { day ->
            val top = day.appMinutes.firstOrNull()
            // The score needs pickups; without them a blank beats a flattering guess.
            val score = day.pickups?.let {
                ZenScore.format(ZenScore.compute(day.screenTimeMinutes * 60_000, it, day.promiseHours))
            }
            appendLine(
                listOf(
                    day.date.toString(),
                    day.screenTimeMinutes.toString(),
                    day.promiseHours.toString(),
                    if (day.keptPromise) "yes" else "no",
                    score.orEmpty(),
                    day.pickups?.toString().orEmpty(),
                    day.lateNightMinutes.toString(),
                    escape(top?.label.orEmpty()),
                    top?.minutes?.toString().orEmpty()
                ).joinToString(",")
            )
        }
    }

    /** RFC 4180: quote a field that holds a comma, quote or newline; double inner quotes. */
    internal fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${field.replace("\"", "\"\"")}\""
        else field

    /**
     * Writes the CSV and opens the share sheet. Returns the number of days exported, 0 when
     * there's nothing yet (the caller says so instead of sharing an empty file).
     */
    fun share(context: Context, today: LocalDate = LocalDate.now()): Int {
        val days = RecapStore(context).days().values
        if (days.isEmpty()) return 0
        val folder = File(context.cacheDir, SHARE_FOLDER).apply { mkdirs() }
        val file = File(folder, "zenmode-export-$today.csv").apply { writeText(csv(days)) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "My ZenMode data")
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Export your ZenMode data"))
        return days.size
    }
}
