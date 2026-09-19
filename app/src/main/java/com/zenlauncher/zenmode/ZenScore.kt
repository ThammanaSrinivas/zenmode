package com.zenlauncher.zenmode

import android.content.Context
import androidx.core.content.edit
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.coreapi.UsageRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The Zen Score: 0.0–10.0, stored and passed around as whole tenths (0–100) so it stays
 * an Int everywhere and only [format] knows about the decimal point.
 *
 * Screen time against the user's own promise carries most of the weight; pickups
 * (unlocks) carry the rest, since a day of many short checks isn't a calm day either.
 */
object ZenScore {
    const val MAX_TENTHS = 100
    const val MAX_DISPLAY = 10

    private const val SCREEN_WEIGHT = 0.7f
    private const val PICKUP_WEIGHT = 0.3f

    /** Up to half the promise costs nothing; the score reaches zero at twice the promise. */
    private const val SCREEN_FREE_RATIO = 0.5f
    private const val SCREEN_ZERO_RATIO = 2f

    /** Pickups cost nothing up to a quarter of the daily goal and zero out at twice it. */
    private const val PICKUP_FREE_RATIO = 0.25f
    private const val PICKUP_ZERO_RATIO = 2f

    fun compute(screenTimeMillis: Long, pickups: Int, promiseHours: Int): Int {
        val promiseMillis = promiseHours.coerceAtLeast(1) * 3_600_000f
        val screenPart = falloff(screenTimeMillis.coerceAtLeast(0) / promiseMillis, SCREEN_FREE_RATIO, SCREEN_ZERO_RATIO)
        val pickupPart = falloff(
            pickups.coerceAtLeast(0) / AppConstants.GOAL_UNLOCKS_COUNT.toFloat(),
            PICKUP_FREE_RATIO,
            PICKUP_ZERO_RATIO
        )
        return ((SCREEN_WEIGHT * screenPart + PICKUP_WEIGHT * pickupPart) * MAX_TENTHS)
            .roundToInt()
            .coerceIn(0, MAX_TENTHS)
    }

    /** "9.3" for 93. */
    fun format(tenths: Int): String =
        String.format(Locale.US, "%.1f", tenths.coerceIn(0, MAX_TENTHS) / 10f)

    /** Size of a change in points, unsigned, for "▲ 0.4 vs yesterday" style labels. */
    fun formatDelta(tenths: Int): String =
        String.format(Locale.US, "%.1f", kotlin.math.abs(tenths) / 10f)

    private fun falloff(ratio: Float, free: Float, zero: Float): Float =
        (1f - (ratio - free) / (zero - free)).coerceIn(0f, 1f)
}

/**
 * Today's Zen Score, recomputed from live usage and kept in prefs per day so the Zen
 * Score screen, the circle and tomorrow's "vs yesterday" all read the same number.
 */
class ZenScoreStore(private val context: Context, private val repository: UsageRepository) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Recomputes today's score from [usage] (or a fresh read), saves it and returns it. */
    fun refresh(usage: DailyUsage = repository.getTodayUsage()): Int {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val score = ZenScore.compute(
            screenTimeMillis = usage.screenTimeInMillis,
            pickups = repository.getPickupCount(startOfDay, System.currentTimeMillis()),
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
