package com.zenlauncher.zenmode.coreapi

import kotlin.math.ceil

/**
 * Turns raw unlock windows + per-app foreground intervals into the [PhoneSession] rows the
 * Zen Score screen shows. Pure maths, no Android calls, so it's unit-testable the same way
 * [com.zenlauncher.zenmode.coreapi.ForegroundSession] aggregation already is elsewhere —
 * category/label lookups are passed in as functions rather than done here.
 */
object SessionLogBuilder {

    /**
     * @param unlockWindows [start, end) spans from unlock to the next lock (or "now", if still
     *   unlocked), as [com.zenlauncher.zenmode.coreapi.UsageRepository.getUnlockWindows] returns
     *   them. Assumed sorted and non-overlapping.
     * @param appSessions every foreground app interval in the same overall range.
     * @param excludedPackages ZenMode itself and any launcher/systemui packages — not "apps you used".
     */
    fun build(
        unlockWindows: List<Pair<Long, Long>>,
        appSessions: List<ForegroundSession>,
        excludedPackages: Set<String>,
        classify: (String) -> AppCategory,
        labelOf: (String) -> String
    ): List<PhoneSession> {
        val sessions = mutableListOf<PhoneSession>()
        for ((windowStart, windowEnd) in unlockWindows.sortedBy { it.first }) {
            if (windowEnd - windowStart < CoreConstants.MIN_SESSION_DURATION_MS) continue
            for ((chunkStart, chunkEnd) in chunk(windowStart, windowEnd)) {
                val perApp = appOverlapIn(chunkStart, chunkEnd, appSessions, excludedPackages)
                // No trackable app opened during this stretch (e.g. glanced at the lock screen
                // and put the phone back down) — nothing to log as a "session".
                val dominant = perApp.maxByOrNull { it.value.totalMillis }?.key ?: continue
                val overlap = perApp.getValue(dominant)
                val category = classify(dominant)
                val label = labelOf(dominant)
                val previous = sessions.lastOrNull()
                val eventType = eventTypeFor(category, overlap.firstStart, previous)
                // The session's own span is the dominant app's actual first-to-last overlap in
                // this chunk, not the chunk's own boundaries — otherwise a few seconds spent in
                // an app during an otherwise-idle-on-home-screen hour would misreport as an
                // hour-long session for that app.
                sessions += PhoneSession(overlap.firstStart, overlap.lastEnd, category, dominant, label, eventType)
            }
        }
        return sessions
    }

    /** Sessions past the max length become same-length display chunks; the last one absorbs the remainder. */
    private fun chunk(start: Long, end: Long): List<Pair<Long, Long>> {
        val total = end - start
        if (total <= CoreConstants.SESSION_MAX_DURATION_MS) return listOf(start to end)
        val count = ceil(total / CoreConstants.SESSION_MAX_DURATION_MS.toDouble()).toInt()
        val size = total / count
        return (0 until count).map { i ->
            val chunkStart = start + i * size
            val chunkEnd = if (i == count - 1) end else chunkStart + size
            chunkStart to chunkEnd
        }
    }

    /** A package's total real foreground time in the chunk, plus the earliest/latest instant of it. */
    private data class AppOverlap(val totalMillis: Long, val firstStart: Long, val lastEnd: Long)

    private fun appOverlapIn(
        start: Long,
        end: Long,
        appSessions: List<ForegroundSession>,
        excludedPackages: Set<String>
    ): Map<String, AppOverlap> {
        val perApp = HashMap<String, AppOverlap>()
        for (s in appSessions) {
            if (s.packageName in excludedPackages) continue
            val overlapStart = maxOf(s.startMillis, start)
            val overlapEnd = minOf(s.endMillis, end)
            if (overlapEnd <= overlapStart) continue
            val existing = perApp[s.packageName]
            perApp[s.packageName] = if (existing == null) {
                AppOverlap(overlapEnd - overlapStart, overlapStart, overlapEnd)
            } else {
                AppOverlap(
                    existing.totalMillis + (overlapEnd - overlapStart),
                    minOf(existing.firstStart, overlapStart),
                    maxOf(existing.lastEnd, overlapEnd)
                )
            }
        }
        return perApp
    }

    /**
     * FOCUS reads as a deliberate check-in. OTHER (no declared Play Store category — weather,
     * most utility apps) reads the same way: a quick, purposeful glance rather than doomscrolling,
     * so it gets the same credit rather than being lumped in with ENTERTAINMENT/SOCIAL/COMMUNICATION.
     * Anything else reopening the *same* category within [CoreConstants.RAPID_REOPEN_GAP_MS] of the
     * last session reads as a relapse rather than a fresh, intentional look — everything else is a
     * plain entertaining/communication session.
     */
    private fun eventTypeFor(category: AppCategory, startMillis: Long, previous: PhoneSession?): SessionEventType {
        if (category == AppCategory.FOCUS || category == AppCategory.OTHER) return SessionEventType.INTENTIONAL
        val isRapidReopen = previous != null &&
            previous.category == category &&
            (startMillis - previous.endMillis) < CoreConstants.RAPID_REOPEN_GAP_MS
        return if (isRapidReopen) SessionEventType.DISRUPTED else SessionEventType.ENTERTAINING
    }
}
