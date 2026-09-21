package com.zenlauncher.zenmode.coreapi

import java.util.Locale
import kotlin.math.roundToInt

/**
 * The Zen Score: 0.0–10.0, stored and passed around as whole tenths (0–100) so it stays an
 * Int everywhere and only [format] knows about the decimal point.
 *
 * Lives in core-api, not the app module, because `core-private`'s StatSyncWorker computes and
 * syncs this same score to `users/{uid}.zen_score` (mirrored to the Zen Circle leaderboard by
 * a Cloud Function) — the two must stay identical, or a user's own Home screen and what their
 * buddy/circle sees would silently disagree. Same reasoning as [PromisePreferences].
 *
 * 75% screen time against the user's own promise; 25% today's session quality (see
 * [SessionLogBuilder]/[SessionEventType]) — a day of short, fragmented, relapse-y sessions
 * scores worse than the same total minutes spent in one calm sitting, even at the same
 * screen-time total.
 */
object ZenScore {
    const val MAX_TENTHS = 100
    const val MAX_DISPLAY = 10

    /** Up to half the promise costs nothing; the score's adherence half reaches zero at twice the promise. */
    private const val SCREEN_FREE_RATIO = 0.5f
    private const val SCREEN_ZERO_RATIO = 2f

    /**
     * @param sessionQualityPercent 0–100, from [SessionLogRepository.getSessionQualityPercent] —
     *   defaults to a neutral 100 upstream when there's no usage yet today, so a fresh day never
     *   scores low for lack of data.
     */
    fun compute(screenTimeMillis: Long, sessionQualityPercent: Int, promiseHours: Int): Int {
        val promiseMillis = promiseHours.coerceAtLeast(1) * 3_600_000f
        val adherence = falloff(screenTimeMillis.coerceAtLeast(0) / promiseMillis, SCREEN_FREE_RATIO, SCREEN_ZERO_RATIO)
        val sessionQuality = sessionQualityPercent.coerceIn(0, 100) / 100f
        return (
            (CoreConstants.SCORE_ADHERENCE_WEIGHT * adherence + CoreConstants.SCORE_SESSION_QUALITY_WEIGHT * sessionQuality) *
                MAX_TENTHS
            ).roundToInt().coerceIn(0, MAX_TENTHS)
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
