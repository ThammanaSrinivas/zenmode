package com.zenlauncher.zenmode

import android.content.Context
import androidx.core.content.edit
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.coreapi.PromisePreferences
import com.zenlauncher.zenmode.coreapi.SessionLogRepository
import com.zenlauncher.zenmode.coreapi.UsageAccess
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.ZenScore
import com.zenlauncher.zenmode.coreapi.zenPrefs
import java.time.LocalDate

/**
 * Today's Zen Score, recomputed from live usage and kept in prefs per day so the Zen Score
 * screen, the circle, the widgets and the weekly streak all read the same number.
 *
 * Each day's last computed score stays on record for [RETENTION_DAYS], the same window as the
 * recap history: the weekly streak needs every day of every week in its run, and a good day
 * is a score above 6.0, not a promise kept. A score is only recorded with usage access on —
 * without it today's usage reads 0, which would score a perfect 10 for a day nobody measured.
 * Keys are `zen_score_yyyy-MM-dd`, the format this store has always written.
 *
 * The formula itself ([ZenScore.compute]) lives in core-api, shared with `core-private`'s
 * StatSyncWorker, so this app-side live view and the number synced to Firestore never disagree.
 */
class ZenScoreStore(
    private val context: Context,
    private val repository: UsageRepository,
    private val sessionLogRepository: SessionLogRepository = SessionLogRepository(context, repository)
) {

    private val prefs = context.zenPrefs()

    /** Recomputes today's score from [usage] (or a fresh read), saves it and returns it. */
    fun refresh(usage: DailyUsage = repository.getTodayUsage(), today: LocalDate = LocalDate.now()): Int {
        val score = ZenScore.compute(
            screenTimeMillis = usage.screenTimeInMillis,
            distractedSessions = sessionLogRepository.getDistractedSessionCount(),
            promiseHours = PromisePreferences.getDailyHours(context)
        )
        if (UsageAccess.isGranted(context)) {
            val firstToday = !prefs.contains(key(today))
            prefs.edit {
                putInt(key(today), score)
                // Once a day is enough to let the oldest days go.
                if (firstToday) prune(today).forEach(::remove)
            }
        }
        return score
    }

    /** The last saved score for today, or a fresh one if nothing is saved yet. */
    fun today(): Int = scoreOn(LocalDate.now()) ?: refresh()

    /** Yesterday's saved score, if the app computed one then. */
    fun yesterday(): Int? = scoreOn(LocalDate.now().minusDays(1))

    /** The last score the app computed on [date], or null when it computed none that day. */
    fun scoreOn(date: LocalDate): Int? = prefs.getInt(key(date), -1).takeIf { it >= 0 }

    /** Every recorded day's score, for the weekly streak. */
    fun history(): Map<LocalDate, Int> = prefs.all.mapNotNull { (k, v) ->
        if (!k.startsWith(KEY_PREFIX) || v !is Int) return@mapNotNull null
        runCatching { LocalDate.parse(k.removePrefix(KEY_PREFIX)) }.getOrNull()?.let { it to v }
    }.toMap()

    private fun prune(today: LocalDate): List<String> {
        val cutoff = today.minusDays(RETENTION_DAYS)
        return history().keys.filter { it < cutoff }.map(::key)
    }

    private fun key(date: LocalDate): String = KEY_PREFIX + date.toString()

    private companion object {
        const val KEY_PREFIX = "zen_score_"

        /** Sixteen weeks, the recap history's window (RecapStore.RETENTION_DAYS). */
        const val RETENTION_DAYS = 16L * 7
    }
}
