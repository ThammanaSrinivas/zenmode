package com.zenlauncher.zenmode.coreapi

import java.util.Locale
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The Zen Score: 2.5–10.0, stored and passed around as whole tenths (25–100) so it stays an
 * Int everywhere and only [format] knows about the decimal point.
 *
 * Lives in core-api, not the app module, because `core-private`'s StatSyncWorker computes and
 * syncs this same score to `users/{uid}.zen_score` (mirrored to the Zen Circle leaderboard by
 * a Cloud Function) — the two must stay identical, or a user's own Home screen and what their
 * buddy/circle sees would silently disagree. Same reasoning as [PromisePreferences].
 *
 * Rewarding, not punishing: keeping the promise is worth 8.0 and nothing on a kept day takes
 * that away. Only time left unused lifts it toward 10, and distracted sessions (relapses, see
 * [SessionEventType.DISRUPTED]) only eat into that lift. Going over the promise costs a
 * penalty that saturates (2.8 at most), so even a bad day reads ~5, and nothing reads below 2.5.
 */
object ZenScore {
    const val MAX_TENTHS = 100
    const val MAX_DISPLAY = 10

    /** Share of the promise left unused at which the unused-time bonus is full. */
    private const val BONUS_FULL_AT_UNUSED = 0.5

    /** Past this many, a further distracted session costs effectively nothing (the sum has converged). */
    private const val MAX_COUNTED_DISTRACTIONS = 1_000

    /**
     * @param distractedSessions today's [SessionEventType.DISRUPTED] sessions, from
     *   [SessionLogRepository.getDistractedSessionCount].
     */
    fun compute(screenTimeMillis: Long, distractedSessions: Int, promiseHours: Int): Int {
        val promiseMillis = promiseHours.coerceAtLeast(1) * 3_600_000.0
        val used = screenTimeMillis.coerceAtLeast(0) / promiseMillis
        val kept = CoreConstants.SCORE_KEPT_TENTHS / 10.0
        val distraction = distractionPenalty(distractedSessions)
        val points = if (used <= 1.0) {
            val unusedBonus = CoreConstants.SCORE_UNUSED_BONUS_MAX *
                ((1.0 - used) / BONUS_FULL_AT_UNUSED).coerceIn(0.0, 1.0)
            kept + (unusedBonus - distraction).coerceAtLeast(0.0)
        } else {
            val overPenalty = CoreConstants.SCORE_OVER_PENALTY_MAX *
                (1.0 - exp(-CoreConstants.SCORE_OVER_PENALTY_RATE * (used - 1.0)))
            kept - overPenalty - distraction
        }
        return (points * 10).roundToInt().coerceIn(CoreConstants.SCORE_FLOOR_TENTHS, MAX_TENTHS)
    }

    /** "9.3" for 93. */
    fun format(tenths: Int): String =
        String.format(Locale.US, "%.1f", tenths.coerceIn(0, MAX_TENTHS) / 10f)

    /** Size of a change in points, unsigned, for "▲ 0.4 vs yesterday" style labels. */
    fun formatDelta(tenths: Int): String =
        String.format(Locale.US, "%.1f", kotlin.math.abs(tenths) / 10f)

    /** 0.3 for the first, then 0.3 / i^1.3 for the i-th: a texture that stays under ~1.2 points. */
    private fun distractionPenalty(count: Int): Double =
        (1..count.coerceIn(0, MAX_COUNTED_DISTRACTIONS)).sumOf { i ->
            CoreConstants.SCORE_DISTRACTION_COST / i.toDouble().pow(CoreConstants.SCORE_DISTRACTION_DECAY)
        }
}
