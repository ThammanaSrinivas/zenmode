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
    /** App use outside any unlock window (no lock screen, Smart Lock) with gaps shorter than
     *  this reads as one pickup in the log. */
    const val UNTRACKED_JOIN_GAP_MS = 60_000L

    // Zen Score formula (see ZenScore): kept promise holds 8, going over costs a saturating
    // penalty, distracted sessions are a light texture, and only time left unused reaches 9–10.
    /** What a kept promise is worth, in whole tenths. */
    const val SCORE_KEPT_TENTHS = 80
    /** Nothing reads lower than this — "you've drifted, come back", never "you failed". */
    const val SCORE_FLOOR_TENTHS = 25
    /** Ceiling of the over-promise penalty, in points, and how fast it gets there. */
    const val SCORE_OVER_PENALTY_MAX = 2.8
    const val SCORE_OVER_PENALTY_RATE = 1.5
    /** Cost of the first distracted session, in points; each later one costs less (÷ i^decay),
     *  so the whole sum stays near one point however many there are. */
    const val SCORE_DISTRACTION_COST = 0.3
    const val SCORE_DISTRACTION_DECAY = 1.3
    /** The most the unused part of the promise adds, in points; reached at half the promise. */
    const val SCORE_UNUSED_BONUS_MAX = 2.0
}
