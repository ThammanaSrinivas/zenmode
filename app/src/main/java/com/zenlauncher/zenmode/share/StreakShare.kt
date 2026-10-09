package com.zenlauncher.zenmode.share

import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.PromiseStreak
import com.zenlauncher.zenmode.PromiseUnit
import com.zenlauncher.zenmode.ui.components.PRODUCT_NAME
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What a streak card celebrates, by days kept. Each tier is its own picture, and the set
 * reads as one story — from a spark to an orbit: a flame, a seven-petal bloom, growth rings,
 * a full moon (a lunar cycle is 30 days), moon and tide, a season, then the trip round the sun.
 */
enum class StreakTier(val minDays: Int, val label: String) {
    RELIGHT(0, "a fresh start"),
    SPARK(1, "the spark"),
    WEEK(7, "one week"),
    ROOTED(14, "two weeks"),
    MOON(30, "one full moon"),
    TIDE(60, "two moons"),
    SEASON(90, "a whole season"),
    HALF_ORBIT(180, "half an orbit"),
    ORBIT(365, "a full orbit");

    companion object {
        fun of(days: Int): StreakTier = entries.last { days >= it.minDays }
    }
}

/**
 * Everything a streak card says, from the promise streak Home's flame shows ([PromiseStreak]):
 * **days kept, judged by the week**. One day over the promise holds the streak rather than
 * breaking it — only a lost week does — so the kept days aren't consecutive, and nothing here
 * says "in a row". Pure, so every line is unit-tested.
 *
 * @param days days kept in the current run: the number next to the flame.
 * @param weeksWon finished Monday–Sunday weeks won back to back inside the run.
 * @param keptThisWeek this week's kept days, toward the
 *   [AppConstants.PROMISE_DAYS_TO_UNLOCK]-of-7 verdict that decides whether the run goes on.
 * @param week this week, Monday first: true kept, false over, null still open.
 */
data class StreakShare(
    val days: Int,
    val weeksWon: Int,
    val keptThisWeek: Int,
    val weekAtRisk: Boolean,
    val week: List<Boolean?>,
    val today: LocalDate
) {
    val tier: StreakTier = StreakTier.of(days.coerceAtLeast(0))

    /** Full orbits completed, for the 365+ tier. */
    val orbits: Int = days.coerceAtLeast(0) / 365

    val title: String = when (tier) {
        StreakTier.RELIGHT -> "Day one starts now."
        StreakTier.SPARK -> if (days == 1) "Day one. Kept." else "The spark is lit."
        StreakTier.WEEK -> if (days == 7) "One whole week." else "A week, and then some."
        StreakTier.ROOTED -> "Taking root."
        StreakTier.MOON -> "One full moon."
        StreakTier.TIDE -> "Moon and tide."
        StreakTier.SEASON -> "A whole season."
        StreakTier.HALF_ORBIT -> "Half an orbit."
        StreakTier.ORBIT -> if (orbits == 1) "One full orbit." else "$orbits full orbits."
    }

    val caption: String = when (tier) {
        StreakTier.RELIGHT -> "Every streak starts with one day kept under the promise. Mine starts today."
        StreakTier.SPARK -> if (days == 1) {
            "One day kept under my screen-time promise. Six more to a full week."
        } else {
            "$days days kept under my screen-time promise. ${7 - days} more to a full week."
        }
        StreakTier.WEEK -> "$days days kept under my promise. Next stop: two weeks."
        StreakTier.ROOTED -> "$days days kept. This is where a habit starts to hold."
        StreakTier.MOON -> "$days days kept: a whole lunar cycle of calmer screen time."
        StreakTier.TIDE -> "$days days kept. Two full moons of choosing what my time is for."
        StreakTier.SEASON -> "$days days kept. A quarter of a year, lived on purpose."
        StreakTier.HALF_ORBIT -> "$days days kept: halfway round the sun, week after week."
        StreakTier.ORBIT -> "$days days kept. A whole trip round the sun, calmer every week."
    }

    /** Top-right of the card. */
    val stamp: String = if (days <= 0) "FRESH START" else "$days DAY STREAK"

    val stats: List<PosterStat> = listOf(
        PosterStat("DAYS KEPT", "${days.coerceAtLeast(0)}"),
        PosterStat("WEEKS WON", "$weeksWon"),
        PosterStat("THIS WEEK", "$keptThisWeek/${AppConstants.PROMISE_DAYS_PER_WEEK}")
    )

    val footer: String = today.format(FooterDate).uppercase(Locale.US)

    /**
     * The line under the card in the sheet: what's next, and how far — or, when this week can
     * no longer be won, that plainly. Null past three orbits, with nothing left to point at.
     */
    val nextMilestone: String? = run {
        if (weekAtRisk && days > 0) {
            return@run "This week can't reach ${AppConstants.PROMISE_DAYS_TO_UNLOCK} of 7 any more, so the streak restarts Monday."
        }
        if (days <= 0) return@run "One day under the promise starts it."
        val (name, at) = when (tier) {
            StreakTier.ORBIT -> {
                if (orbits >= 3) return@run null
                "${orbits + 1} full orbits" to (orbits + 1) * 365
            }
            else -> StreakTier.entries[tier.ordinal + 1].let { it.label to it.minDays }
        }
        "Next: $name, ${plural(at - days, "kept day")} to go."
    }

    /** What analytics calls this card. */
    val analyticsKey: String = "streak_${tier.name.lowercase(Locale.US)}"

    /** What goes in the share message alongside the image. */
    val shareText: String = if (days <= 0) {
        "Starting a screen-time streak with $PRODUCT_NAME. ${AppConstants.PLAY_STORE_URL}"
    } else {
        "${plural(days, "day")} kept under my screen-time promise with $PRODUCT_NAME — " +
            "${tier.label}. Start your streak: ${AppConstants.PLAY_STORE_URL}"
    }

    /** For TalkBack: the card in one breath. */
    val description: String = "$title $caption ${plural(weeksWon, "week")} won, " +
        "$keptThisWeek of ${AppConstants.PROMISE_DAYS_PER_WEEK} days kept this week."

    companion object {
        private val FooterDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

        /** From Home's promise streak and this week's days (ZenGoldPromise.weekly's units). */
        fun of(streak: PromiseStreak.State, week: List<PromiseUnit>, today: LocalDate) = StreakShare(
            days = streak.days,
            weeksWon = streak.weeksKept,
            keptThisWeek = streak.daysKeptThisWeek,
            weekAtRisk = streak.weekAtRisk,
            week = week.map { it.kept },
            today = today
        )
    }
}

/** "1 day", "4 days". */
internal fun plural(count: Int, noun: String): String = if (count == 1) "1 $noun" else "$count ${noun}s"
