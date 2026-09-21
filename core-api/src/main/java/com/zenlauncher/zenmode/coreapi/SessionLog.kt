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
 */
data class PhoneSession(
    val startMillis: Long,
    val endMillis: Long,
    val category: AppCategory,
    val dominantPackage: String?,
    val dominantLabel: String,
    val eventType: SessionEventType
) {
    val durationMillis: Long get() = (endMillis - startMillis).coerceAtLeast(0L)
}
