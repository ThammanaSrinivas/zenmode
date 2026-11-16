package com.zenlauncher.zenmode.week

import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.coreapi.week.MomentKind
import com.zenlauncher.zenmode.recap.DayRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeekReaderTest {

    private val monday = LocalDate.of(2026, 11, 9)

    private fun record(date: LocalDate, minutes: Long, promiseHours: Int = 4) =
        DayRecord(date, minutes, promiseHours, emptyList(), 0, null)

    @Test
    fun `a finished day takes the lower of measured and estimated`() {
        val day = monday.minusDays(1)
        // 3h of a 4h promise estimates high; a measured score with distractions is lower.
        val recorded = mapOf(day to record(day, 180))
        val estimate = WeekReader.estimate(recorded.getValue(day))
        assertEquals(minOf(70, estimate), WeekReader.finishedDayScore(day, mapOf(day to 70), recorded))
        // A late, heavy evening the app never saw: the sealed minutes win.
        val heavy = mapOf(day to record(day, 400))
        assertEquals(WeekReader.estimate(heavy.getValue(day)), WeekReader.finishedDayScore(day, mapOf(day to 85), heavy))
        assertNull(WeekReader.finishedDayScore(day, emptyMap(), emptyMap()))
    }

    @Test
    fun `the streak runs over the weeks on record and not before`() {
        val measured = buildMap {
            // Three won weeks, ending the Sunday before this Monday.
            for (w in 1..3) for (d in 0 until 7) put(monday.minusWeeks(w.toLong()).plusDays(d.toLong()), if (d < 4) 72 else 50)
        }
        val state = WeekReader.read(measured, emptyMap(), monday.plusDays(1), 66, DayPart.MORNING)
        assertEquals(3, state.streak.weeks)
        assertEquals(1, state.week.goodDays)
        assertEquals(true, state.lastWeek?.isWon)
        assertEquals(MomentKind.BUILDING, state.moment.kind)
    }

    @Test
    fun `a lost week in the middle restarts the run`() {
        val measured = buildMap {
            for (w in 1..4) for (d in 0 until 7) {
                val good = if (w == 2) 2 else 4
                put(monday.minusWeeks(w.toLong()).plusDays(d.toLong()), if (d < good) 72 else 50)
            }
        }
        assertEquals(1, WeekReader.read(measured, emptyMap(), monday, null, DayPart.MORNING).streak.weeks)
    }

    @Test
    fun `nothing on record is a first day, not a lost week`() {
        val state = WeekReader.read(emptyMap(), emptyMap(), monday.plusDays(2), null, DayPart.AFTERNOON)
        assertEquals(0, state.streak.weeks)
        assertNull(state.lastWeek)
        assertEquals(MomentKind.WAITING, state.moment.kind)
    }

    @Test
    fun `today's live score counts as soon as it's good`() {
        val measured = mapOf(monday to 70, monday.plusDays(1) to 70, monday.plusDays(2) to 70)
        val state = WeekReader.read(measured, emptyMap(), monday.plusDays(3), 71, DayPart.EVENING)
        assertEquals(MomentKind.JUST_WON, state.moment.kind)
        assertEquals(1, state.streak.weeks)
    }
}
