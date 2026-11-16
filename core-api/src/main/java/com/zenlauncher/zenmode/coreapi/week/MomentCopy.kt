package com.zenlauncher.zenmode.coreapi.week

import java.time.LocalDate

/**
 * What Zeno says for a moment, on every surface that reads [WeekMoment]. Ported from the
 * iPhone app's copy so both platforms
 * speak the same week.
 *
 * Voice: no shame, no urgency, no ranking. Stakes are said as facts ("Sunday decides your 9-week
 * streak"), never as pressure. Nothing here asks the person to log anything or open the app.
 *
 * Lines vary from day to day (seeded by the date, so a widget says the same thing all day and
 * something new tomorrow) and with each poke (tap Zeno and he answers).
 */
object MomentCopy {

    /** Zeno's line for the moment. [seed] picks among the variants (the day, plus any pokes). */
    fun line(m: WeekMoment, seed: Int = 0): String = pick(lines(m), seed)

    /** Every line Zeno can say for this moment (tests read them all). */
    fun lines(m: WeekMoment): List<String> {
        val streak = m.streakAtStake
        val streakPhrase = "your $streak-week streak"
        val needed = m.needed
        val goodDays = "good day${plural(needed)}"
        return when (m.kind) {
            MomentKind.PERFECT_WEEK -> listOf(
                "Seven of seven. A perfect week.",
                "Every day, a good day. That's rare.",
                "A perfect week. Zeno is glowing."
            )
            MomentKind.JUST_WON -> listOf(
                if (m.streakWeeks > 1) "Week won. ${m.streakWeeks}-week streak." else "Week won. Your first.",
                "Four good days. The week is yours.",
                "You kept your promise. Week won."
            )
            MomentKind.WON -> if (m.part == DayPart.NIGHT) {
                listOf("Week won. Rest easy.", "A won week, and a quiet night.")
            } else {
                listOf("This week is yours.", "Week won. The rest is a bonus.", "${m.goodDays} good days and counting.")
            }
            MomentKind.ONE_AWAY -> listOf(
                "One good day from winning the week.",
                "So close. One more good day.",
                "${m.goodDays} of 4. One to go."
            )
            MomentKind.ON_THE_LINE -> {
                val today = m.openDays == 1
                val subject = if (today) "Today decides" else "The next ${m.openDays} days decide"
                if (streak > 0) {
                    listOf(
                        "$subject $streakPhrase.",
                        "$needed $goodDays keep${if (needed == 1) "s" else ""} $streakPhrase.",
                        "Every day left counts. $streak weeks ride on it."
                    )
                } else {
                    listOf("Every day left counts this week.", "$needed $goodDays still wins the week.")
                }
            }
            MomentKind.SLIPPING -> if (streak > 0) {
                listOf(
                    "This week got away. Your $streak weeks still happened.",
                    "Not this week. Monday starts a new run.",
                    "Zeno misses the streak too. Monday is new."
                )
            } else {
                listOf("Not this week. Monday is a fresh start.", "This one got away. The next one is new.")
            }
            MomentKind.FRESH_WEEK -> if (m.streakWeeks > 0) {
                listOf(
                    "Week ${m.streakWeeks + 1} of your streak starts today.",
                    "A new week. Your ${m.streakWeeks}-week streak comes with you.",
                    "Fresh week. 4 good days wins it."
                )
            } else {
                listOf("A new week. 4 good days wins it.", "Fresh start. Zeno's ready.")
            }
            MomentKind.NEW_RUN -> listOf(
                "New week, new run.",
                "Last week got away. This one is brand new.",
                "A fresh start. 4 good days wins it."
            )
            MomentKind.BUILDING -> when {
                m.part == DayPart.NIGHT -> listOf(WINDING_DOWN, "Phone down soon. Tomorrow counts too.")
                m.todayIsGood -> listOf("Today's a good day.", progressLine(m), "Another good day in the bloom.")
                else -> listOf(
                    progressLine(m),
                    if (m.part == DayPart.MORNING) "Today starts clean." else "The day is still yours.",
                    "Steady. $needed good days to win the week."
                )
            }
            MomentKind.WAITING -> if (m.part == DayPart.NIGHT) {
                listOf(WINDING_DOWN)
            } else {
                listOf(COUNTS_ITSELF, "Zeno is here when you are.")
            }
        }
    }

    /**
     * The week in a few words, for a headline set large ("Two more win it."): the This week
     * widget's Clash Display line. Always short enough for one line at four columns.
     */
    fun headline(m: WeekMoment): String = when (m.kind) {
        MomentKind.PERFECT_WEEK -> "Perfect week."
        MomentKind.JUST_WON, MomentKind.WON -> "Week won."
        MomentKind.ONE_AWAY -> "One more wins it."
        MomentKind.ON_THE_LINE -> "Every day counts now."
        MomentKind.SLIPPING -> "Monday starts fresh."
        MomentKind.FRESH_WEEK, MomentKind.NEW_RUN -> "Four good days win it."
        MomentKind.BUILDING -> when (m.needed) {
            2 -> "Two more win it."
            3 -> "Three more win it."
            else -> "Four good days win it."
        }
        MomentKind.WAITING -> COUNTS_ITSELF
    }

    /** The eyebrow above Zeno's line ("THIS WEEK", "WEEK WON"). */
    fun eyebrow(m: WeekMoment): String = when (m.kind) {
        MomentKind.PERFECT_WEEK -> "PERFECT WEEK"
        MomentKind.JUST_WON, MomentKind.WON -> "WEEK WON"
        MomentKind.ON_THE_LINE -> if (m.streakAtStake > 0) "STREAK ON THE LINE" else "EVERY DAY COUNTS"
        MomentKind.ONE_AWAY -> "ONE TO GO"
        MomentKind.SLIPPING -> "NEXT WEEK"
        MomentKind.FRESH_WEEK, MomentKind.NEW_RUN -> "NEW WEEK"
        MomentKind.BUILDING, MomentKind.WAITING -> "THIS WEEK"
    }

    /** The streak as said everywhere: "9-week streak" (never a daily streak). */
    fun streak(weeks: Int): String = if (weeks == 0) "No streak yet" else "$weeks-week streak"

    /** "3 of 4 good days this week" or "Week won · 5 good days". */
    fun progress(m: WeekMoment): String =
        if (m.goodDays >= WeeklyStreak.GOOD_DAYS_TO_WIN) "Week won · ${m.goodDays} good days" else progressLine(m)

    /** The short foot a widget prints under the week: "2 of 4 good days", "4 good days". */
    fun foot(m: WeekMoment): String = when {
        m.kind == MomentKind.PERFECT_WEEK -> "7 of 7 good days"
        m.goodDays >= WeeklyStreak.GOOD_DAYS_TO_WIN -> "${m.goodDays} good days"
        else -> "${m.goodDays} of ${WeeklyStreak.GOOD_DAYS_TO_WIN} good days"
    }

    /** What the streak chip says beside the week: "9-week streak", "9 weeks at stake", "First week". */
    fun streakChip(m: WeekMoment): String = when {
        m.kind == MomentKind.ON_THE_LINE && m.streakAtStake > 0 -> "${m.streakAtStake} weeks at stake"
        m.kind == MomentKind.SLIPPING -> "Fresh start Monday"
        m.streakWeeks == 0 -> "First week"
        else -> streak(m.streakWeeks)
    }

    /** The sentence TalkBack reads for a widget. */
    fun spoken(m: WeekMoment, seed: Int = 0): String {
        val parts = mutableListOf(line(m, seed), progress(m) + ".")
        if (m.streakWeeks > 0) parts += streak(m.streakWeeks) + "."
        m.todayScoreTenths?.let { parts += "Today's Zen Score is ${formatTenths(it)}." }
        return parts.joinToString(" ")
    }

    /**
     * What Zeno says right after a poke, before going back to the moment's line. Short, warm,
     * never a nudge.
     */
    fun pokeReply(m: WeekMoment, poke: Int): String {
        val replies = when (m.kind) {
            MomentKind.PERFECT_WEEK, MomentKind.JUST_WON, MomentKind.WON ->
                listOf("Hehe. We did it.", "Gold looks good on us.", "High five!")
            MomentKind.SLIPPING, MomentKind.NEW_RUN -> listOf("I'm okay. Are you?", "Thanks. That helped.", "Monday, together.")
            MomentKind.ON_THE_LINE, MomentKind.ONE_AWAY -> listOf("I believe in this week.", "Eyes up, phone down.", "We've got this.")
            else -> if (m.part == DayPart.NIGHT) {
                listOf("Zzz… oh, hi.", "Five more minutes…", "Sleep well.")
            } else {
                listOf("Hi!", "That tickles.", "Hello again.", "Still here.")
            }
        }
        return pick(replies, poke)
    }

    // ── Lines the widgets need that iPhone's moments don't carry ───────────────

    /** Usage access is off: the one card that asks, and never for manual entry. */
    const val USAGE_ACCESS_OFF = "Tap to let your day count itself."

    /** Under an hour of the promise left. */
    fun lastHour(minutesLeft: Long): String =
        if (minutesLeft <= 1) "A minute of your promise left." else "$minutesLeft minutes of your promise left."

    /** A stable seed for a date: the same all day, different tomorrow. */
    fun daySeed(date: LocalDate): Int = (date.toEpochDay() % Int.MAX_VALUE).toInt()

    /** "7.4" for 74. Kept here (not in ZenScore) so this file has no other core-api dependency. */
    fun formatTenths(tenths: Int): String = "${tenths / 10}.${tenths % 10}"

    private fun progressLine(m: WeekMoment) = "${m.goodDays} of ${WeeklyStreak.GOOD_DAYS_TO_WIN} good days this week"

    private fun plural(n: Int) = if (n == 1) "" else "s"

    private fun pick(lines: List<String>, seed: Int): String {
        if (lines.isEmpty()) return ""
        val i = ((seed % lines.size) + lines.size) % lines.size
        return lines[i]
    }

    private const val WINDING_DOWN = "Zeno is winding down."
    private const val COUNTS_ITSELF = "Your day counts itself."
}
