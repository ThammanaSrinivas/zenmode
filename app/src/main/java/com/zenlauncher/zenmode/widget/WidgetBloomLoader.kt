package com.zenlauncher.zenmode.widget

import android.content.Context
import com.zenlauncher.zenmode.ContentBlockPrefs
import com.zenlauncher.zenmode.DistractingApps
import com.zenlauncher.zenmode.coreapi.UsageAccess
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.recap.weekStartOf
import com.zenlauncher.zenmode.widget.faces.BloomCell
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The week's 168 hours for the bloom, from Android's own app history: for each hour, how many
 * minutes the phone was used and how many of them went to the apps that pull (social, video and
 * games, plus anything the person quieted). Android keeps that history for about a week, which
 * is exactly the bloom's span.
 *
 * Read once an hour at most; the hours already past don't change.
 */
object WidgetBloomLoader {

    private class Cached(val hourKey: Long, val cells: List<BloomCell>)

    @Volatile private var cached: Cached? = null

    fun load(context: Context, now: Long = System.currentTimeMillis()): List<BloomCell>? {
        if (!UsageAccess.isGranted(context)) return null
        val zone = ZoneId.systemDefault()
        val hourKey = now / HOUR
        cached?.takeIf { it.hourKey == hourKey }?.let { return it.cells }
        val cells = runCatching { read(context.applicationContext, now, zone) }.getOrNull() ?: return null
        cached = Cached(hourKey, cells)
        return cells
    }

    private fun read(ctx: Context, now: Long, zone: ZoneId): List<BloomCell> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val monday = weekStartOf(today)
        val start = monday.atStartOfDay(zone).toInstant().toEpochMilli()
        val repository = UsageRepository(ctx, ServiceLocator.analyticsManager)
        val excluded = repository.excludedPackages()
        val pm = ctx.packageManager
        val quieted = ContentBlockPrefs.quietedApps(ctx)
        val pulls = HashMap<String, Boolean>()
        val used = FloatArray(7 * 24)
        val pull = FloatArray(7 * 24)
        for (s in repository.getForegroundSessions(start, now)) {
            if (s.packageName in excluded) continue
            val isPull = pulls.getOrPut(s.packageName) { s.packageName in quieted || DistractingApps.isDistracting(pm, s.packageName) }
            var t = maxOf(s.startMillis, start)
            val end = minOf(s.endMillis, now)
            while (t < end) {
                val slot = ((t - start) / HOUR).toInt()
                val slotEnd = start + (slot + 1) * HOUR
                val minutes = (minOf(end, slotEnd) - t) / 60_000f
                if (slot in used.indices) {
                    used[slot] += minutes
                    if (isPull) pull[slot] += minutes
                }
                t = slotEnd
            }
        }
        return cells(used, pull, monday, today, Instant.ofEpochMilli(now).atZone(zone).hour)
    }

    /** Pure: the 168 cells for a week whose current hour is [nowHour] on [today]. Day-major, Monday first. */
    fun cells(used: FloatArray, pull: FloatArray, monday: LocalDate, today: LocalDate, nowHour: Int): List<BloomCell> {
        val todayIndex = (today.toEpochDay() - monday.toEpochDay()).toInt()
        return List(7 * 24) { i ->
            val day = i / 24
            val hour = i % 24
            when {
                day > todayIndex || (day == todayIndex && hour > nowHour) -> BloomCell.FUTURE
                day == todayIndex && hour == nowHour -> BloomCell.NOW
                else -> BloomCell.classify(hour, used[i], pull[i])
            }
        }
    }

    private const val HOUR = 3_600_000L
}
