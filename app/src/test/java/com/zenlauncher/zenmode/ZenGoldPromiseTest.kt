package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.recap.AppMinutes
import com.zenlauncher.zenmode.recap.DayRecord
import com.zenlauncher.zenmode.recap.WeeklyRecap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ZenGoldPromiseTest {

    // Monday.
    private val monday = LocalDate.of(2026, 9, 7)

    private fun day(date: LocalDate, minutes: Long, promiseHours: Int = 4) =
        DayRecord(date, minutes, promiseHours, emptyList<AppMinutes>(), 0, 30)

    @Test
    fun `weekly marks finished days kept or broken and leaves future days undecided`() {
        // Wed: Mon+Tue kept (under 4h), today (Wed) live at 100min (kept), Thu-Sun haven't happened.
        val today = monday.plusDays(2)
        val days = mapOf(
            monday to day(monday, 100),
            monday.plusDays(1) to day(monday.plusDays(1), 500) // broken
        )
        val state = ZenGoldPromise.weekly(days, todayMinutes = 100, promiseHours = 4, today = today)

        assertEquals(listOf(true, false, true, null, null, null, null), state.units.map { it.kept })
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), state.units.map { it.label })
        assertEquals(2, state.unitsKept)
        assertEquals(1, state.unitsMissed)
        assertEquals(4, state.unitsRemaining)
        assertFalse(state.unlocked)
    }

    @Test
    fun `weekly unlocks once five days are kept and stays unlocked the rest of the week`() {
        val today = monday.plusDays(4) // Friday
        val days = (0..3).associate { i -> monday.plusDays(i.toLong()) to day(monday.plusDays(i.toLong()), 100) }
        val state = ZenGoldPromise.weekly(days, todayMinutes = 50, promiseHours = 4, today = today)

        assertEquals(5, state.unitsKept)
        assertTrue(state.unlocked)
    }

    @Test
    fun `weekly daily average only counts days that have actually happened`() {
        val today = monday.plusDays(1) // Tuesday
        val days = mapOf(monday to day(monday, 120))
        val state = ZenGoldPromise.weekly(days, todayMinutes = 60, promiseHours = 4, today = today)

        // (120 + 60) / 2 elapsed days, not / 7.
        assertEquals(90, state.dailyAverageMinutes)
    }

    @Test
    fun `weekly missing history for a past day is left undecided, not marked broken`() {
        val today = monday.plusDays(3)
        val state = ZenGoldPromise.weekly(emptyMap(), todayMinutes = 500, promiseHours = 4, today = today)

        // Mon-Wed have no record (app not installed then); today (Thu) is broken live.
        assertEquals(listOf(null, null, null, false, null, null, null), state.units.map { it.kept })
    }

    @Test
    fun `monthly week containing today reads live once the threshold is locked in either way`() {
        val monthStart = LocalDate.of(2026, 9, 1) // a Tuesday
        val weekStart = LocalDate.of(2026, 8, 31) // Monday covering Sep 1

        // Already 5 kept days this week by Friday -> locked in green even though Sat/Sun are open.
        val today = LocalDate.of(2026, 9, 4) // Friday
        val days = (0..3).associate { i ->
            weekStart.plusDays(i.toLong()) to day(weekStart.plusDays(i.toLong()), 100)
        }
        val state = ZenGoldPromise.monthly(days, todayMinutes = 50, promiseHours = 4, recapFor = { null }, today = today)

        assertEquals(true, state.units.first().kept)
    }

    @Test
    fun `monthly week containing today reads broken once it can no longer reach the threshold`() {
        val weekStart = LocalDate.of(2026, 8, 31)
        val today = LocalDate.of(2026, 9, 4) // Friday: Mon-Thu all broken already.
        val days = (0..3).associate { i ->
            weekStart.plusDays(i.toLong()) to day(weekStart.plusDays(i.toLong()), 500)
        }
        val state = ZenGoldPromise.monthly(days, todayMinutes = 500, promiseHours = 4, recapFor = { null }, today = today)

        // Best case left: Fri, Sat, Sun kept = 3 < 5, so it's already lost.
        assertEquals(false, state.units.first().kept)
    }

    @Test
    fun `monthly reuses a finished week's real recap outcome`() {
        // Aug 20 2026 is a Thursday, so the month's first bar (the week starting Jul 27) is
        // fully in the past by then — decided purely from recapFor, never live.
        val today = LocalDate.of(2026, 8, 20)
        val keptRecapFor = { start: LocalDate ->
            WeeklyRecap(start, (0..6).map { i -> day(start.plusDays(i.toLong()), if (i < 5) 100 else 500) }, null)
        }
        val state = ZenGoldPromise.monthly(emptyMap(), todayMinutes = 0, promiseHours = 4, recapFor = keptRecapFor, today = today)

        assertEquals(true, state.units.first().kept)
    }

    @Test
    fun `monthly is unlocked only when every decided week was kept`() {
        // Sep 21 2026 is a Monday: three full prior weeks this month (Aug 31, Sep 7, Sep 14)
        // are judged purely from recapFor; the current week (today itself) is still undecided.
        val today = LocalDate.of(2026, 9, 21)
        val missedWeek = LocalDate.of(2026, 9, 7)

        val clean = ZenGoldPromise.monthly(
            days = emptyMap(),
            todayMinutes = 50,
            promiseHours = 4,
            recapFor = { start -> WeeklyRecap(start, (0..6).map { i -> day(start.plusDays(i.toLong()), 100) }, null) },
            today = today
        )
        assertTrue(clean.unlocked)

        val withAMiss = ZenGoldPromise.monthly(
            days = emptyMap(),
            todayMinutes = 50,
            promiseHours = 4,
            recapFor = { start ->
                val minutes = if (start == missedWeek) 500L else 100L
                WeeklyRecap(start, (0..6).map { i -> day(start.plusDays(i.toLong()), minutes) }, null)
            },
            today = today
        )
        assertFalse(withAMiss.unlocked)
    }

    @Test
    fun `monthly week after today is undecided`() {
        val today = LocalDate.of(2026, 9, 4)
        val state = ZenGoldPromise.monthly(emptyMap(), todayMinutes = 0, promiseHours = 4, recapFor = { null }, today = today)
        assertNull(state.units.last().kept)
    }
}
