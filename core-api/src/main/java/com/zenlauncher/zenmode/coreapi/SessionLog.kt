package com.zenlauncher.zenmode.coreapi

/**
 * How a [PhoneSession] reads, for the Zen Score screen's session log. True intent isn't
 * observable — this is a heuristic derived from category and reopen pattern, not a claim
 * about what the user actually meant. See [SessionLogBuilder].
 */
enum class SessionEventType { INTENTIONAL, ENTERTAINING, DISRUPTED }

/**
 * One phone pickup-to-putdown span (unlock to lock), the atomic row in the session log.
 * May cover several apps; [category]/[dominantPackage] reflect whichever had the most time.
 *
 * [durationMillis] is the real time spent in apps during the span — every app, not just the
 * dominant one, and not the idle stretches on home between them — so the log's rows add up
 * to the same screen time Home shows. It's usually shorter than `endMillis - startMillis`.
 */
data class PhoneSession(
    val startMillis: Long,
    val endMillis: Long,
    val durationMillis: Long,
    /** How many different apps were used in the span, the dominant one included. */
    val appCount: Int,
    val category: AppCategory,
    val dominantPackage: String?,
    val dominantLabel: String,
    val eventType: SessionEventType
)
