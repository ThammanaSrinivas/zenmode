package com.zenlauncher.zenmode.recap

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.zenlauncher.zenmode.PromisePreferences
import com.zenlauncher.zenmode.coreapi.ForegroundSession
import com.zenlauncher.zenmode.coreapi.UsageAccess
import com.zenlauncher.zenmode.coreapi.UsageRepository
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Turns Android's usage history into [DayRecord]s while it still exists. Android keeps
 * raw events for roughly a week, so this runs regularly and backfills any finished day
 * from the last [LOOKBACK_DAYS] that isn't stored yet.
 */
class RecapCollector(
    private val context: Context,
    private val repository: UsageRepository,
    private val store: RecapStore
) {

    /** Records every missing finished day. Returns how many were added. */
    fun backfill(today: LocalDate = LocalDate.now()): Int {
        if (!UsageAccess.isGranted(context)) return 0
        val missing = (1..LOOKBACK_DAYS).map { today.minusDays(it.toLong()) }.filterNot(store::hasDay)
        if (missing.isEmpty()) return 0
        val excluded = excludedPackages()
        val records = missing.mapNotNull { date -> record(date, excluded) }
        store.putDays(records)
        return records.size
    }

    private fun record(date: LocalDate, excluded: Set<String>): DayRecord? {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val totalMillis = repository.getScreenTimeMillisForDate(date.toString())
        val sessions = repository.getForegroundSessions(start, end)
        // Nothing at all usually means the history already rolled off; don't store a false zero.
        if (totalMillis <= 0L && sessions.isEmpty()) return null

        val breakdown = DayAggregator.aggregate(sessions, start, end, excluded)
        return DayRecord(
            date = date,
            screenTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(totalMillis),
            promiseHours = PromisePreferences.getDailyHours(context),
            appMinutes = breakdown.appMillis.map { (pkg, millis) ->
                AppMinutes(pkg, labelOf(pkg), TimeUnit.MILLISECONDS.toMinutes(millis))
            }.filter { it.minutes > 0 }.take(MAX_APPS_PER_DAY),
            lateNightMinutes = TimeUnit.MILLISECONDS.toMinutes(breakdown.lateNightMillis),
            pickups = repository.getPickupCount(start, end)
        )
    }

    /** ZenMode itself and any home screen or system UI — not "apps you used". */
    private fun excludedPackages(): Set<String> {
        val pm = context.packageManager
        val homes = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        ).map { it.activityInfo.packageName }
        return (homes + context.packageName + "com.android.systemui").toSet()
    }

    private fun labelOf(pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    private companion object {
        const val LOOKBACK_DAYS = 7
        const val MAX_APPS_PER_DAY = 12
    }
}

/** Pure per-day maths over foreground sessions, kept apart from Android for tests. */
internal object DayAggregator {

    data class Breakdown(val appMillis: List<Pair<String, Long>>, val lateNightMillis: Long)

    /** 22:00 → 04:00: the late-night window the missed-week tip talks about. */
    private const val LATE_START_HOUR = 22L
    private const val LATE_END_HOUR = 4L

    fun aggregate(
        sessions: List<ForegroundSession>,
        dayStartMillis: Long,
        dayEndMillis: Long,
        excluded: Set<String>
    ): Breakdown {
        val hour = TimeUnit.HOURS.toMillis(1)
        val dayEnd = dayEndMillis
        // Windows inside this calendar day: [00:00, 04:00) and [22:00, midnight).
        val lateWindows = listOf(
            dayStartMillis to dayStartMillis + LATE_END_HOUR * hour,
            dayEnd - (24 - LATE_START_HOUR) * hour to dayEnd
        )
        val perApp = HashMap<String, Long>()
        var late = 0L
        for (s in sessions) {
            if (s.packageName in excluded) continue
            val start = s.startMillis.coerceAtLeast(dayStartMillis)
            val end = s.endMillis.coerceAtMost(dayEnd)
            if (end <= start) continue
            perApp[s.packageName] = (perApp[s.packageName] ?: 0L) + (end - start)
            for ((ws, we) in lateWindows) {
                val overlap = minOf(end, we) - maxOf(start, ws)
                if (overlap > 0) late += overlap
            }
        }
        return Breakdown(perApp.toList().sortedByDescending { it.second }, late)
    }
}
