package com.zenlauncher.zenmode.coreapi

/**
 * Tuning constants shared by [ZenScore] / [SessionLogBuilder] / [PromisePreferences] that
 * `core-private`'s StatSyncWorker also needs, so they can't live in the app module's
 * `AppConstants` (core-private cannot depend on `app` — see the composite build's open-core
 * split). Mirrors app-module constants where noted; keep both sides in sync if they change.
 */
object CoreConstants {

    // My Promise (mirrors app module's AppConstants.PROMISE_* — the promise stepper there
    // clamps into this same range).
    const val PROMISE_MIN_DAILY_HOURS = 1
    const val PROMISE_MAX_DAILY_HOURS = 12
    const val PLACEHOLDER_PROMISE_HOURS = 4

    // Session log segmentation (Zen Score screen's "Today's Session Log").
    /** Pickups shorter than this are glances, not sessions — dropped from the log entirely. */
    const val MIN_SESSION_DURATION_MS = 10_000L
    /** Sessions past this are chunked into same-length blocks for display only. */
    const val SESSION_MAX_DURATION_MS = 60 * 60_000L
    /** A same-category reopen within this long of the last session's end reads as a relapse
     *  (DISRUPTED) rather than a fresh, intentional check (ENTERTAINING). */
    const val RAPID_REOPEN_GAP_MS = 5 * 60_000L

    // Zen Score formula: screen time vs. the user's own promise carries most of the weight;
    // today's session quality (see SessionLogBuilder) carries the rest.
    const val SCORE_ADHERENCE_WEIGHT = 0.75f
    const val SCORE_SESSION_QUALITY_WEIGHT = 0.25f
}
