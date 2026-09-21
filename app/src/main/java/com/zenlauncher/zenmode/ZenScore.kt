package com.zenlauncher.zenmode

import android.content.Context
import androidx.core.content.edit
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.coreapi.PromisePreferences
import com.zenlauncher.zenmode.coreapi.SessionLogRepository
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.ZenScore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Today's Zen Score, recomputed from live usage and kept in prefs per day so the Zen
 * Score screen, the circle and tomorrow's "vs yesterday" all read the same number.
 *
 * The formula itself ([ZenScore.compute]) lives in core-api, shared with `core-private`'s
 * StatSyncWorker, so this app-side live view and the number synced to Firestore never disagree.
 */
class ZenScoreStore(
    private val context: Context,
    private val repository: UsageRepository,
    private val sessionLogRepository: SessionLogRepository = SessionLogRepository(context, repository)
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Recomputes today's score from [usage] (or a fresh read), saves it and returns it. */
    fun refresh(usage: DailyUsage = repository.getTodayUsage()): Int {
        val score = ZenScore.compute(
            screenTimeMillis = usage.screenTimeInMillis,
            sessionQualityPercent = sessionLogRepository.getSessionQualityPercent(),
            promiseHours = PromisePreferences.getDailyHours(context)
        )
        // Only today and yesterday are ever read, so drop the day before as we go.
        prefs.edit {
            putInt(key(dayOffset = 0), score)
            remove(key(dayOffset = -2))
        }
        return score
    }

    /** The last saved score for today, or a fresh one if nothing is saved yet. */
    fun today(): Int = prefs.getInt(key(dayOffset = 0), -1).takeIf { it >= 0 } ?: refresh()

    /** Yesterday's saved score, if the app computed one then. */
    fun yesterday(): Int? = prefs.getInt(key(dayOffset = -1), -1).takeIf { it >= 0 }

    private fun key(dayOffset: Int): String {
        val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }.time
        return KEY_PREFIX + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day)
    }

    private companion object {
        const val PREFS_NAME = "zenmode_prefs"
        const val KEY_PREFIX = "zen_score_"
    }
}
