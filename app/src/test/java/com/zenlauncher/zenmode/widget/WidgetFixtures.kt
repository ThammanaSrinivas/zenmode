package com.zenlauncher.zenmode.widget

import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.quiet.QuietSession
import com.zenlauncher.zenmode.week.WeekReader
import java.time.LocalDate

/**
 * The week's moments as the widget design draws them, built through
 * the real [WeekReader] so a fixture can't describe a week the rules wouldn't produce.
 *
 * Week 46 of 2026: Monday 9 November to Sunday 15 November. Nine won weeks come before it.
 */
object WidgetFixtures {

    val monday: LocalDate = LocalDate.of(2026, 11, 9)

    /** Nine won weeks (four good days each) after one lost week, ending the Sunday before [monday]. */
    private fun history(wonWeeks: Int = 9): Map<LocalDate, Int> {
        val out = mutableMapOf<LocalDate, Int>()
        val first = monday.minusWeeks(wonWeeks.toLong() + 1)
        for (w in 0..wonWeeks) {
            val start = first.plusWeeks(w.toLong())
            val good = if (w == 0) 2 else 4
            for (d in 0 until 7) out[start.plusDays(d.toLong())] = if (d < good) 72 else 52
        }
        return out
    }

    enum class Moment(val scores: List<Int?>, val label: String) {
        NEW(listOf(55), "Fresh week"),
        GOING(listOf(78, 69, 55), "Building"),
        ONE(listOf(78, 69, 52, 71, 58), "One to go"),
        CALM(listOf(78, 69, 52, 74), "A calm day, one to go"),
        WON(listOf(78, 69, 52, 71, 80), "Just won"),
        PERFECT(listOf(78, 69, 64, 71, 80, 86, 73), "Perfect week"),
        STAKE(listOf(51, 68, 49, 56, 55), "On the line"),
        OUT(listOf(51, 44, 68, 56, 50, 55), "Slipping"),
        FIRST(listOf(null), "First day")
    }

    fun data(
        moment: Moment = Moment.GOING,
        part: DayPart = DayPart.AFTERNOON,
        minutesToday: Long = 112,
        usageAccess: Boolean = true,
        quiet: QuietState = QuietState(available = true, running = null, ended = null),
        gold: GoldState = GoldState("2,724", invested = true, open = false, kept = 3, outOfReach = false),
        circle: CircleState = chasing,
        yesterday: Int? = 71,
        pokes: Int = 0,
        answering: Boolean = false
    ): WidgetData {
        val today = monday.plusDays((moment.scores.size - 1).toLong())
        val measured = (if (moment == Moment.FIRST) emptyMap() else history()) +
            moment.scores.dropLast(1).mapIndexedNotNull { i, s -> s?.let { monday.plusDays(i.toLong()) to it } }
        val todayScore = moment.scores.last()
        val week = WeekReader.read(measured, emptyMap(), today, todayScore, part)
        return WidgetData(
            now = NOW,
            today = today,
            part = part,
            usageAccess = usageAccess,
            week = week,
            todayScore = if (usageAccess) todayScore else null,
            yesterdayScore = yesterday,
            minutesToday = minutesToday,
            promiseHours = 4,
            quiet = quiet,
            gold = gold,
            circle = circle,
            pokes = pokes,
            answering = answering
        )
    }

    const val NOW = 1_763_000_000_000L

    val running = QuietState(available = true, running = QuietSession.Running(NOW - 15 * 60_000L, NOW + 10 * 60_000L), ended = null)
    val done = QuietState(available = true, running = null, ended = QuietSession.Ended(NOW - 60_000L, 25, 2))

    val chasing = CircleState.Ranked(
        "Morning people",
        listOf(CircleSeat("Asha", 86, false), CircleSeat("Kamal", 74, true), CircleSeat("Ravi", 68, false), CircleSeat("Meera", 61, false), CircleSeat("Arun", null, false)),
        myRank = 2
    )
    val leading = CircleState.Ranked(
        "Morning people",
        listOf(CircleSeat("Kamal", 88, true), CircleSeat("Asha", 86, false), CircleSeat("Ravi", 68, false)),
        myRank = 1
    )
}
