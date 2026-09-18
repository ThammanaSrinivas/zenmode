package com.zenlauncher.zenmode.recap

import com.zenlauncher.zenmode.AppConstants
import kotlin.math.ceil

/**
 * The five cards of a week's recap. Two flows share the same beats but not the same
 * voice: a kept week celebrates and ends on Invest; a missed week names what got in the
 * way, suggests one concrete change, and ends on recommitting (Invest stays locked —
 * nothing is taken away).
 */
sealed interface RecapCard {
    /** Stable id for analytics. */
    val key: String

    data class Opener(val headline: String, val subline: String) : RecapCard {
        override val key = "opener"
    }

    data class Total(
        val totalMinutes: Long,
        val dailyAverageMinutes: Long,
        val changeVsLastWeekMinutes: Long?,
        val caption: String
    ) : RecapCard {
        override val key = "total"
    }

    data class Promise(
        val days: List<DayRecord>,
        val daysKept: Int,
        val daysToUnlock: Int,
        val headline: String,
        val caption: String
    ) : RecapCard {
        override val key = "promise"
    }

    /** Kept flow: a moment worth remembering. */
    data class Highlight(val headline: String, val value: String, val caption: String) : RecapCard {
        override val key = "highlight"
    }

    /** Missed flow: what got in the way, and one thing to try. */
    data class Obstacle(
        val headline: String,
        val culpritLabel: String?,
        val culpritMinutes: Long?,
        val detail: String,
        val tip: String
    ) : RecapCard {
        override val key = "obstacle"
    }

    /** Kept flow ending: Invest is unlocked. */
    data class InvestUnlocked(val headline: String, val caption: String) : RecapCard {
        override val key = "invest"
    }

    /** Missed flow ending: recommit; Invest shows what unlocks it. */
    data class Recommit(
        val headline: String,
        val caption: String,
        val suggestedPromiseHours: Int?
    ) : RecapCard {
        override val key = "recommit"
    }
}

object RecapStory {

    fun cards(recap: WeeklyRecap): List<RecapCard> = when (recap.outcome) {
        RecapOutcome.KEPT -> listOf(opener(recap), total(recap), promise(recap), highlight(recap), investUnlocked(recap))
        RecapOutcome.MISSED -> listOf(opener(recap), total(recap), promise(recap), obstacle(recap), recommit(recap))
    }

    private fun opener(recap: WeeklyRecap) = when (recap.outcome) {
        RecapOutcome.KEPT -> RecapCard.Opener(
            headline = "Your week in Zen",
            subline = "You kept your promise. Let's look back at a calmer week."
        )
        RecapOutcome.MISSED -> RecapCard.Opener(
            headline = "Your week in Zen",
            subline = "Not every week is a calm one. Let's look at this one honestly."
        )
    }

    private fun total(recap: WeeklyRecap): RecapCard.Total {
        val change = recap.previousWeekTotalMinutes?.let { recap.totalMinutes - it }
        val caption = when {
            change == null -> "That's ${formatMinutes(recap.dailyAverageMinutes)} a day on average."
            change < 0 -> "${formatMinutes(-change)} less than last week. That's real time back."
            change > 0 -> "${formatMinutes(change)} more than last week."
            else -> "Exactly the same as last week."
        }
        return RecapCard.Total(recap.totalMinutes, recap.dailyAverageMinutes, change, caption)
    }

    private fun promise(recap: WeeklyRecap): RecapCard.Promise {
        val hours = recap.promiseHours
        val unit = if (hours == 1) "hour" else "hours"
        val (headline, caption) = when (recap.outcome) {
            RecapOutcome.KEPT -> "${recap.daysKept} of ${recap.days.size} days under $hours $unit" to
                if (recap.daysKept == recap.days.size) "A perfect week. Every single day." else "Promise kept. That unlocks Invest for the week."
            RecapOutcome.MISSED -> {
                val short = recap.daysToUnlock - recap.daysKept
                "${recap.daysKept} of ${recap.days.size} days under $hours $unit" to
                    "${if (short == 1) "One day" else "$short days"} short of unlocking Invest. " +
                    "Missed days ran ${formatMinutes(recap.minutesOverPromise)} over in total."
            }
        }
        return RecapCard.Promise(recap.days, recap.daysKept, recap.daysToUnlock, headline, caption)
    }

    private fun highlight(recap: WeeklyRecap): RecapCard.Highlight {
        val reclaimed = recap.minutesReclaimed
        if (reclaimed != null && reclaimed >= 30) {
            return RecapCard.Highlight(
                headline = "Time you won back",
                value = formatMinutes(reclaimed),
                caption = "That's ${equivalentOf(reclaimed)} you got back from your phone."
            )
        }
        val calmest = recap.calmestDay
        return RecapCard.Highlight(
            headline = "Your calmest day",
            value = calmest?.fullDayName() ?: "—",
            caption = calmest?.let { "Just ${formatMinutes(it.screenTimeMinutes)} on screen. More days like this one." }
                ?: "More days like this one."
        )
    }

    private fun obstacle(recap: WeeklyRecap): RecapCard.Obstacle {
        val top = recap.topApps.firstOrNull()
        val lateShare = if (recap.totalMinutes > 0) recap.lateNightMinutes.toFloat() / recap.totalMinutes else 0f
        val loudest = recap.loudestDay
        val detail = buildString {
            if (top != null) append("${top.label} took ${formatMinutes(top.minutes)} of your week.")
            if (lateShare >= LATE_NIGHT_SHARE) {
                if (isNotEmpty()) append(" ")
                append("${formatMinutes(recap.lateNightMinutes)} of your screen time came after 10 pm.")
            } else if (loudest != null) {
                if (isNotEmpty()) append(" ")
                append("${loudest.fullDayName()} was the heaviest day at ${formatMinutes(loudest.screenTimeMinutes)}.")
            }
        }
        val topShare = if (top != null && recap.totalMinutes > 0) top.minutes.toFloat() / recap.totalMinutes else 0f
        val tip = when {
            lateShare >= LATE_NIGHT_SHARE -> "Try this: charge your phone outside the bedroom after 10 pm."
            top != null && topShare >= TOP_APP_SHARE ->
                "Try this: switch on the Reels & Shorts blocker, or keep ${top.label} off your home screen."
            else -> "Try this: pick one screen-free hour each day and protect it."
        }
        return RecapCard.Obstacle(
            headline = "What got in the way",
            culpritLabel = top?.label,
            culpritMinutes = top?.minutes,
            detail = detail.ifEmpty { "Your screen time crept up across the week." },
            tip = tip
        )
    }

    private fun investUnlocked(recap: WeeklyRecap) = RecapCard.InvestUnlocked(
        headline = "Invest is unlocked",
        caption = "You kept your promise ${recap.daysKept} of ${recap.days.size} days. " +
            "Put the discipline to work: invest in gold, on your own terms."
    )

    private fun recommit(recap: WeeklyRecap): RecapCard.Recommit {
        val suggestion = suggestedPromiseHours(recap)?.takeIf { it != recap.promiseHours }
        val caption = buildString {
            append("Invest stays locked this week. Nothing is taken away, and this week is a fresh start.")
            if (suggestion != null) {
                append(" A promise of $suggestion ${if (suggestion == 1) "hour" else "hours"} a day ")
                append("would have kept ${recap.daysToUnlock} of your 7 days.")
            }
        }
        return RecapCard.Recommit(headline = "Let's win the next one", caption = caption, suggestedPromiseHours = suggestion)
    }

    /**
     * The smallest whole-hour promise that would have been kept on [WeeklyRecap.daysToUnlock]
     * of this week's days — realistic enough to succeed, tight enough to mean something.
     */
    fun suggestedPromiseHours(recap: WeeklyRecap): Int? {
        if (recap.days.size < recap.daysToUnlock) return null
        val nth = recap.days.map { it.screenTimeMinutes }.sorted()[recap.daysToUnlock - 1]
        return ceil(nth / 60.0).toInt()
            .coerceIn(AppConstants.PROMISE_MIN_DAILY_HOURS, AppConstants.PROMISE_MAX_DAILY_HOURS)
    }

    /** Warm, human-sized equivalents for won-back time. */
    internal fun equivalentOf(minutes: Long): String = when {
        minutes >= 240 -> "${minutes / 120} full movies"
        minutes >= 120 -> "a whole movie night"
        minutes >= 60 -> "a long dinner with people you love"
        else -> "an unhurried evening walk"
    }

    private const val LATE_NIGHT_SHARE = 0.25f
    private const val TOP_APP_SHARE = 0.3f
}
