package com.zenlauncher.zenmode.week

import com.zenlauncher.zenmode.coreapi.week.DayMark
import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.coreapi.week.MomentAccent
import com.zenlauncher.zenmode.coreapi.week.MomentCopy
import com.zenlauncher.zenmode.coreapi.week.MomentFeeling
import com.zenlauncher.zenmode.coreapi.week.MomentKind
import com.zenlauncher.zenmode.coreapi.week.ScoredWeek
import com.zenlauncher.zenmode.coreapi.week.WeekMoment
import com.zenlauncher.zenmode.coreapi.week.WeeklyStreak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The weekly streak and the week's moment, case for case with the iPhone app's tests, so both
 * platforms read the same week the same way. iPhone scores are 0–1000; Android's are tenths, so 700 there is 70 here.
 */
class WeekMomentTest {

    private fun moment(scores: List<Int?>, today: Int, streakBefore: Int = 9, lastWeekWon: Boolean? = true, hour: Int = 10): WeekMoment {
        val week = ScoredWeek(scores, today)
        val streak = WeeklyStreak(streakBefore + if (week.isWon) 1 else 0, week.goodDays)
        return WeekMoment.read(week, streak, scores.getOrNull(today), lastWeekWon, DayPart.at(hour, 0))
    }

    // ── The streak ──────────────────────────────────────────────────

    @Test
    fun `a good day is above 6_0, and no data is never good`() {
        assertFalse(WeeklyStreak.isGood(60))
        assertTrue(WeeklyStreak.isGood(61))
        assertFalse(WeeklyStreak.isGood(null))
    }

    @Test
    fun `the week in progress never breaks the streak`() {
        val lost = ScoredWeek(List(7) { 50 })
        val won = ScoredWeek(listOf(70, 70, 70, 70, null, null, null))
        val partial = ScoredWeek(listOf(70, null, null, null, null, null, null), todayIndex = 0)
        assertEquals(2, WeeklyStreak.compute(listOf(won, won), partial).weeks)
        assertEquals(1, WeeklyStreak.compute(listOf(won, lost, won), partial).weeks)
        assertEquals(2, WeeklyStreak.compute(listOf(won), won).weeks)
    }

    @Test
    fun `three good days do not win a week, four do`() {
        assertFalse(ScoredWeek(listOf(70, 70, 70, 50, 50, 50, 50)).isWon)
        assertTrue(ScoredWeek(listOf(70, 70, 70, 61, 50, 50, 50)).isWon)
    }

    // ── The moment ──────────────────────────────────────────────────

    @Test
    fun `monday with a streak is a fresh week`() {
        val m = moment(emptyList(), today = 0)
        assertEquals(MomentKind.FRESH_WEEK, m.kind)
        assertEquals(4, m.needed)
        assertEquals(7, m.openDays)
    }

    @Test
    fun `monday after a lost week is a new run`() {
        assertEquals(MomentKind.NEW_RUN, moment(emptyList(), today = 0, streakBefore = 0, lastWeekWon = false).kind)
    }

    @Test
    fun `the fourth good day today is the high moment`() {
        val m = moment(listOf(70, 65, 61, 72), today = 3)
        assertEquals(MomentKind.JUST_WON, m.kind)
        assertEquals(MomentFeeling.PROUD, m.feeling)
        assertEquals(MomentAccent.AMBER, m.accent)
        assertEquals(10, m.streakWeeks)
        assertEquals(9, m.streakAtStake)
        assertEquals("WON", m.compactValue)
    }

    @Test
    fun `a week won earlier is won, not just won`() {
        assertEquals(MomentKind.WON, moment(listOf(70, 65, 61, 72, 50), today = 4).kind)
        assertEquals(MomentKind.WON, moment(listOf(70, 65, 61, 72, 70), today = 4).kind)
    }

    @Test
    fun `seven of seven is perfect`() {
        assertEquals(MomentKind.PERFECT_WEEK, moment(List(7) { 70 }, today = 6).kind)
    }

    @Test
    fun `exactly six is not a good day`() {
        assertEquals(0, moment(listOf(60, 60, 60), today = 3).goodDays)
    }

    @Test
    fun `three good days with room is one away`() {
        val m = moment(listOf(70, 70, 70), today = 3)
        assertEquals(MomentKind.ONE_AWAY, m.kind)
        assertEquals("3/4", m.compactValue)
    }

    @Test
    fun `saturday needing two is on the line`() {
        val m = moment(listOf(70, 50, 70, 50, 50), today = 5)
        assertEquals(MomentKind.ON_THE_LINE, m.kind)
        assertEquals(2, m.openDays)
        assertEquals(MomentFeeling.ALERT, m.feeling)
        assertEquals("STREAK ON THE LINE", MomentCopy.eyebrow(m))
    }

    @Test
    fun `sunday needing two is slipping`() {
        val m = moment(listOf(70, 50, 70, 50, 50, 50), today = 6)
        assertEquals(MomentKind.SLIPPING, m.kind)
        assertEquals(MomentFeeling.SAD, m.feeling)
        assertTrue(m.kind.isHard)
    }

    @Test
    fun `night makes Zeno sleepy, but not on a celebration`() {
        assertEquals(MomentFeeling.SLEEPY, moment(listOf(70, 70), today = 2, hour = 23).feeling)
        assertEquals(MomentFeeling.PROUD, moment(listOf(70, 65, 61, 72), today = 3, hour = 23).feeling)
    }

    @Test
    fun `day parts and their boundaries`() {
        assertEquals(DayPart.MORNING, DayPart.at(6, 0))
        assertEquals(DayPart.AFTERNOON, DayPart.at(13, 0))
        assertEquals(DayPart.EVENING, DayPart.at(19, 0))
        assertEquals(DayPart.NIGHT, DayPart.at(22, 0))
        assertEquals(DayPart.NIGHT, DayPart.at(3, 0))
        assertEquals(17 * 60, DayPart.nextBoundaryAfter(12 * 60))
        assertEquals(21 * 60 + 30, DayPart.nextBoundaryAfter(17 * 60))
        assertEquals(1440 + 5 * 60, DayPart.nextBoundaryAfter(22 * 60))
    }

    @Test
    fun `the marks read the week, no data is not a zero`() {
        val m = moment(listOf(70, null, 50, 61), today = 3)
        assertEquals(
            listOf(DayMark.GOOD, DayMark.NO_DATA, DayMark.QUIET, DayMark.GOOD, DayMark.TO_COME, DayMark.TO_COME, DayMark.TO_COME),
            m.marks
        )
        assertEquals(DayMark.TODAY, moment(listOf(70, 70, 55), today = 2).marks[2])
    }

    // ── The copy ────────────────────────────────────────────────────

    private val everyMoment: List<WeekMoment> by lazy {
        val cases = listOf(
            emptyList<Int?>() to 0, listOf(70, 70, 70) to 3, listOf(70, 50, 70, 50, 50) to 5,
            listOf(70, 50, 70, 50, 50, 50) to 6, listOf(70, 65, 61, 72) to 3,
            listOf(70, 65, 61, 72, 50) to 4, List(7) { 70 } to 6, listOf<Int?>(70) to 1
        )
        cases.flatMap { (scores, today) ->
            listOf(0, 9).flatMap { streak -> listOf(7, 13, 19, 23).map { moment(scores, today, streak, hour = it) } }
        }
    }

    @Test
    fun `lines never shame or rush, and never ask anything`() {
        val banned = listOf("!!", "don't lose", "hurry", "fail", "lazy", "log ", "open the app", "streak of days", "daily streak")
        // What Zeno says unprompted never asks; a reply to a poke may ("I'm okay. Are you?", as on iPhone).
        val unprompted = everyMoment.flatMap { m -> MomentCopy.lines(m) + MomentCopy.headline(m) }
        val replies = everyMoment.flatMap { m -> (0 until 4).map { MomentCopy.pokeReply(m, it) } }
        (unprompted + replies).forEach { line ->
            assertTrue("empty line", line.isNotBlank())
            banned.forEach { word -> assertFalse("\"$line\" contains \"$word\"", line.lowercase().contains(word)) }
        }
        unprompted.forEach { line -> assertFalse("\"$line\" asks", line.contains("?")) }
    }

    @Test
    fun `the streak is always weekly`() {
        assertEquals("10-week streak", MomentCopy.streak(10))
        assertEquals("No streak yet", MomentCopy.streak(0))
    }

    @Test
    fun `on the line names the streak`() {
        val m = moment(listOf(70, 50, 70, 50, 50), today = 5)
        assertTrue(MomentCopy.lines(m).contains("2 good days keep your 9-week streak."))
    }

    @Test
    fun `lines change day to day but hold within a day`() {
        val wednesday = LocalDate.of(2026, 11, 4)
        assertEquals(MomentCopy.daySeed(wednesday) + 1, MomentCopy.daySeed(wednesday.plusDays(1)))
        val m = moment(listOf(70, 70, 70), today = 3)
        val lines = (0 until 3).map { MomentCopy.line(m, MomentCopy.daySeed(wednesday.plusDays(it.toLong()))) }
        assertEquals(3, lines.toSet().size)
    }

    @Test
    fun `headlines fit one line at four columns`() {
        everyMoment.forEach { assertTrue(MomentCopy.headline(it), MomentCopy.headline(it).length <= 24) }
    }

    @Test
    fun `the spoken sentence carries the line, the progress and the score`() {
        val spoken = MomentCopy.spoken(moment(listOf(70, 70, 70, 50), today = 3), seed = 0)
        assertTrue(spoken, spoken.contains("3 of 4 good days this week."))
        assertTrue(spoken, spoken.contains("9-week streak."))
        assertTrue(spoken, spoken.contains("Today's Zen Score is 5.0."))
    }
}
