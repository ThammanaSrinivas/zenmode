package com.zenlauncher.zenmode.coreapi

/**
 * Zen score (0-100) — lives in core-api, not app's AppLogic, because StatSyncWorker
 * (core-private) needs it too and core-private cannot depend on the app module.
 *
 * [DEFAULT_MAX_MINUTES] intentionally mirrors AppConstants.THRESHOLD_NEUTRAL_MINUTES (app
 * module, value 210) — the same module boundary that forces this function to live here
 * means that constant can't be shared directly either. Keep both in sync if it ever changes.
 *
 * v1 placeholder formula (screen-time only, will be revisited): 100 at zero minutes, 0 at
 * [maxMinutes], linear in between.
 */
object ZenScoreCalculator {
    const val DEFAULT_MAX_MINUTES = 210

    fun calculateZenScore(minutes: Long, maxMinutes: Int = DEFAULT_MAX_MINUTES): Int {
        val percentage = ((maxMinutes - minutes).toFloat() / maxMinutes * 100).toInt()
        return percentage.coerceIn(0, 100)
    }
}
