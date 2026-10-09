package com.zenlauncher.zenmode.coreapi

import android.content.Context
import java.util.Calendar

/**
 * Today's session log for the Zen Score screen — cache-only by design (the raw log never
 * leaves the device; only the [ZenScore] number it feeds into gets synced). Recomputed on
 * demand straight from Android's own usage history (retained ~7-10 days) rather than
 * persisted anywhere: it's cheap (same event-query shape [UsageRepository.getForegroundSessions]
 * already does), and the screen only ever shows *today*, so there's nothing to gain from a
 * separate SharedPreferences cache with its own day-rollover/invalidation logic to get wrong.
 */
class SessionLogRepository(private val context: Context, private val usageRepository: UsageRepository) {

    /** Every session so far today, oldest first. */
    fun getTodaySessions(): List<PhoneSession> {
        if (!UsageAccess.isGranted(context)) return emptyList()
        val start = startOfTodayMillis()
        val end = System.currentTimeMillis()
        val windows = usageRepository.getUnlockWindows(start, end)
        val appSessions = usageRepository.getForegroundSessions(start, end)
        val pm = context.packageManager
        val excluded = usageRepository.excludedPackages()
        return SessionLogBuilder.build(
            unlockWindows = windows,
            appSessions = appSessions,
            excludedPackages = excluded,
            classify = { pkg -> AppCategoryClassifier.classify(pm, pkg) },
            labelOf = { pkg -> AppCategoryClassifier.labelOf(pm, pkg) }
        )
    }

    /** Today's relapses ([SessionEventType.DISRUPTED]) — the Zen Score's light distraction texture. */
    fun getDistractedSessionCount(sessions: List<PhoneSession> = getTodaySessions()): Int =
        sessions.count { it.eventType == SessionEventType.DISRUPTED }

    /** Share of today's tracked foreground time per category, summing to ~100. Empty on a usage-free day. */
    fun getCategoryBreakdownPercent(): List<Pair<AppCategory, Int>> {
        if (!UsageAccess.isGranted(context)) return emptyList()
        val start = startOfTodayMillis()
        val end = System.currentTimeMillis()
        val pm = context.packageManager
        val excluded = usageRepository.excludedPackages()
        val perCategory = HashMap<AppCategory, Long>()
        var total = 0L
        for (session in usageRepository.getForegroundSessions(start, end)) {
            if (session.packageName in excluded) continue
            val duration = session.endMillis - session.startMillis
            if (duration <= 0L) continue
            val category = AppCategoryClassifier.classify(pm, session.packageName)
            perCategory[category] = (perCategory[category] ?: 0L) + duration
            total += duration
        }
        if (total <= 0L) return emptyList()
        return AppCategory.entries.mapNotNull { category ->
            val millis = perCategory[category] ?: return@mapNotNull null
            category to ((millis * 100) / total).toInt()
        }.sortedByDescending { it.second }
    }

    private fun startOfTodayMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
