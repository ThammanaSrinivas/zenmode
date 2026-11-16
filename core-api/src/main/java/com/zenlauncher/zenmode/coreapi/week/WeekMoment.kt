package com.zenlauncher.zenmode.coreapi.week

/**
 * The week's moment: how the week feels right now, in one value. Every surface that speaks for
 * the week reads the same moment, so Zeno never says one thing on one surface and something
 * else on another.
 *
 * Ported one for one from the iPhone app, so a person with both
 * devices hears the same week from both. It follows the one streak ([WeeklyStreak]).
 *
 * Pure Kotlin, no `android.*`.
 */
data class WeekMoment(
    val kind: MomentKind,
    val part: DayPart,
    /** Good days so far this week. */
    val goodDays: Int,
    /** Good days still needed to win (0 once won). */
    val needed: Int,
    /** Days from today to Sunday that can still become good (today included unless it already is). */
    val openDays: Int,
    /** Index of today, 0 = Monday. */
    val todayIndex: Int,
    /** Won weeks in a row (the current week counted once it's won). */
    val streakWeeks: Int,
    /** The streak at stake: the run of finished won weeks before this one. */
    val streakAtStake: Int,
    /** Today is already a good day. */
    val todayIsGood: Boolean,
    /** Today's Zen Score in whole tenths, or null before any data. */
    val todayScoreTenths: Int?,
    /** The week as seven marks, Monday first. */
    val marks: List<DayMark>
) {

    /** Zeno's mood. Night softens everything except a celebration. */
    val feeling: MomentFeeling
        get() {
            val night = part == DayPart.NIGHT
            return when (kind) {
                MomentKind.PERFECT_WEEK, MomentKind.JUST_WON -> MomentFeeling.PROUD
                MomentKind.WON -> if (night) MomentFeeling.PLEASANT else MomentFeeling.HAPPY
                MomentKind.ONE_AWAY -> if (night) MomentFeeling.SLEEPY else MomentFeeling.CURIOUS
                MomentKind.ON_THE_LINE -> if (night) MomentFeeling.SLEEPY else MomentFeeling.ALERT
                MomentKind.SLIPPING -> MomentFeeling.SAD
                MomentKind.FRESH_WEEK -> if (night) MomentFeeling.SLEEPY else MomentFeeling.PLEASANT
                MomentKind.NEW_RUN -> MomentFeeling.CALM
                MomentKind.BUILDING -> when {
                    night -> MomentFeeling.SLEEPY
                    todayIsGood -> MomentFeeling.HAPPY
                    else -> MomentFeeling.PLEASANT
                }
                MomentKind.WAITING -> if (night) MomentFeeling.SLEEPY else MomentFeeling.CALM
            }
        }

    /** The hue the moment wears: green does the work, amber is earned, ember is something to notice. */
    val accent: MomentAccent
        get() = when (kind) {
            MomentKind.PERFECT_WEEK, MomentKind.JUST_WON, MomentKind.WON -> MomentAccent.AMBER
            MomentKind.ON_THE_LINE -> MomentAccent.EMBER
            MomentKind.SLIPPING, MomentKind.NEW_RUN -> MomentAccent.DUSK
            MomentKind.ONE_AWAY, MomentKind.FRESH_WEEK, MomentKind.BUILDING, MomentKind.WAITING -> MomentAccent.ZEN
        }

    /** The week is won (the widgets turn gold together). */
    val isWon: Boolean get() = goodDays >= WeeklyStreak.GOOD_DAYS_TO_WIN

    /** The compact value: "3/4" while the week is open, "WON" once it's won, "7/7" for a perfect week. */
    val compactValue: String
        get() = when (kind) {
            MomentKind.PERFECT_WEEK -> "7/7"
            MomentKind.JUST_WON, MomentKind.WON -> "WON"
            else -> "$goodDays/${WeeklyStreak.GOOD_DAYS_TO_WIN}"
        }

    companion object {
        /**
         * Reads the week.
         *
         * @param week the week in progress, its [ScoredWeek.todayIndex] set to today.
         * @param todayScoreTenths today's live score, when it isn't in [week] yet.
         * @param lastWeekWon null when there's no history (a first week).
         */
        fun read(
            week: ScoredWeek,
            streak: WeeklyStreak,
            todayScoreTenths: Int?,
            lastWeekWon: Boolean?,
            part: DayPart
        ): WeekMoment {
            val good = week.goodDays
            val todayIndex = week.today
            val todayIsGood = week.todayIsGood
            val needed = (WeeklyStreak.GOOD_DAYS_TO_WIN - good).coerceAtLeast(0)
            val open = ((ScoredWeek.DAYS - todayIndex) - (if (todayIsGood) 1 else 0)).coerceAtLeast(0)
            val atStake = if (week.isWon) (streak.weeks - 1).coerceAtLeast(0) else streak.weeks
            val anyData = week.anyData || todayScoreTenths != null

            val kind = when {
                good >= ScoredWeek.DAYS -> MomentKind.PERFECT_WEEK
                // The 4th good day landed today exactly when today is good and it made four.
                week.isWon -> if (todayIsGood && good == WeeklyStreak.GOOD_DAYS_TO_WIN) MomentKind.JUST_WON else MomentKind.WON
                needed > open -> MomentKind.SLIPPING
                todayIndex == 0 && good == 0 -> if (lastWeekWon == false) MomentKind.NEW_RUN else MomentKind.FRESH_WEEK
                !anyData -> MomentKind.WAITING
                needed == open -> MomentKind.ON_THE_LINE
                needed == 1 -> MomentKind.ONE_AWAY
                else -> MomentKind.BUILDING
            }
            return WeekMoment(
                kind = kind,
                part = part,
                goodDays = good,
                needed = needed,
                openDays = open,
                todayIndex = todayIndex,
                streakWeeks = streak.weeks,
                streakAtStake = atStake,
                todayIsGood = todayIsGood,
                todayScoreTenths = todayScoreTenths ?: week.days[todayIndex],
                marks = DayMark.marks(week)
            )
        }
    }
}

/**
 * The four parts of a day. Widgets change their light and Zeno's mood with them, so the same
 * week reads differently at 07:00 and at 23:00.
 */
enum class DayPart(
    /** When the part begins, in minutes after midnight (widgets refresh at these). */
    val startMinute: Int
) {
    /** 05:00–11:00. The day starts clean. */
    MORNING(300),
    /** 11:00–17:00. The day in progress. */
    AFTERNOON(660),
    /** 17:00–21:30. How the day went. */
    EVENING(1020),
    /** 21:30–05:00. Zeno winds down. */
    NIGHT(1290);

    companion object {
        fun at(hour: Int, minute: Int): DayPart = when (hour * 60 + minute) {
            in MORNING.startMinute until AFTERNOON.startMinute -> MORNING
            in AFTERNOON.startMinute until EVENING.startMinute -> AFTERNOON
            in EVENING.startMinute until NIGHT.startMinute -> EVENING
            else -> NIGHT
        }

        /** Minutes after midnight of the next part boundary after [minuteOfDay]; may be ≥ 1440 (tomorrow). */
        fun nextBoundaryAfter(minuteOfDay: Int): Int =
            entries.map { it.startMinute }.firstOrNull { it > minuteOfDay } ?: (MINUTES_PER_DAY + MORNING.startMinute)

        const val MINUTES_PER_DAY = 1440
    }
}

/** How one day of the week reads in a 7-cell strip. */
enum class DayMark {
    /** A Zen Score above 6.0. */
    GOOD,
    /** A day gone by that wasn't a good day. Quiet, never red. */
    QUIET,
    /** A day gone by with no data. Not a zero. */
    NO_DATA,
    /** Today, not a good day yet. */
    TODAY,
    /** Still to come. */
    TO_COME;

    companion object {
        fun marks(week: ScoredWeek): List<DayMark> = week.days.mapIndexed { i, score ->
            when {
                WeeklyStreak.isGood(score) -> GOOD
                i < week.today -> if (score == null) NO_DATA else QUIET
                i == week.today -> TODAY
                else -> TO_COME
            }
        }
    }
}

/** What the week is doing now, ordered from the brightest moment to the hardest. */
enum class MomentKind {
    /** All seven days good. */
    PERFECT_WEEK,
    /** The 4th good day landed today: the high moment. */
    JUST_WON,
    /** The week was won earlier; the rest is a bonus. */
    WON,
    /** One good day from winning, with room to spare. */
    ONE_AWAY,
    /** Every day left has to be good to win. */
    ON_THE_LINE,
    /** The week can no longer be won. The streak resets when it ends. */
    SLIPPING,
    /** Monday (or a week with nothing yet): a clean start, the streak carried in. */
    FRESH_WEEK,
    /** Monday after a lost week: a new run starts. */
    NEW_RUN,
    /** Good days are coming in; nothing decisive yet. */
    BUILDING,
    /** No data at all (usage access off, or the very first day). */
    WAITING;

    /** Celebrations: the gold moments. */
    val isCelebration: Boolean get() = this == JUST_WON || this == PERFECT_WEEK

    /** Hard moments: nothing that asks anything of the person follows one. */
    val isHard: Boolean get() = this == SLIPPING
}

enum class MomentAccent { ZEN, AMBER, EMBER, DUSK }

/** Zeno's eight feelings (the brand book's expressions). */
enum class MomentFeeling {
    CALM, HAPPY, PLEASANT, SAD, CURIOUS, SLEEPY, ALERT, PROUD;

    /** Zeno a moment after a poke: a sad Zeno smiles, a sleepy one wakes, the rest brighten. */
    val lifted: MomentFeeling
        get() = when (this) {
            SAD -> CALM
            SLEEPY -> CURIOUS
            ALERT, CURIOUS, CALM -> HAPPY
            HAPPY, PLEASANT, PROUD -> this
        }
}
