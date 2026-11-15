package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.recap.AppMinutes
import com.zenlauncher.zenmode.recap.DayRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The rule under test: a day over the promise **holds** the streak, and only a lost
 * Monday–Sunday week resets it.
 */
class PromiseStreakTest {

    // Monday.
    private val monday = LocalDate.of(2026, 9, 7)

    private val underPromise = 100L
    private val overPromise = 500L

    private fun day(date: LocalDate, minutes: Long, promiseHours: Int = 4) =
        DayRecord(date, minutes, promiseHours, emptyList<AppMinutes>(), 0, 30)

    /** A full week starting [weekStart] with its first [kept] days under the promise. */
    private fun week(weekStart: LocalDate, kept: Int): Map<LocalDate, DayRecord> =
        (0..6).associate { i ->
            val date = weekStart.plusDays(i.toLong())
            date to day(date, if (i < kept) underPromise else overPromise)
        }

    @Test
    fun `a day over the promise does not break the streak, it just does not add a day`() {
        // Wednesday: Mon kept, Tue over, today under.
        val today = monday.plusDays(2)
        val days = mapOf(
            monday to day(monday, underPromise),
            monday.plusDays(1) to day(monday.plusDays(1), overPromise)
        )

        val state = PromiseStreak.of(days, todayKept = true, today = today)

        // Mon + today. Tuesday is simply absent from the count — not a reset.
        assertEquals(2, state.days)
        assertEquals(monday, state.startDate)
        assertFalse(state.weekAtRisk)
    }

    @Test
    fun `a kept finished week carries the run back into it`() {
        val lastWeek = monday.minusWeeks(1)
        // 5 of 7 last week is a win; the week before it isn't on record, so the walk stops.
        val days = week(lastWeek, kept = 5) + mapOf(monday to day(monday, underPromise))
        val today = monday.plusDays(1)

        val state = PromiseStreak.of(days, todayKept = true, today = today)

        assertEquals(1, state.weeksKept)
        assertEquals(lastWeek, state.startDate)
        // 5 kept last week + Monday + today.
        assertEquals(7, state.days)
    }

    @Test
    fun `a lost finished week resets the run to this week`() {
        val lastWeek = monday.minusWeeks(1)
        // Only 2 of 7 — short of PROMISE_DAYS_TO_UNLOCK, so the week was lost.
        val days = week(lastWeek, kept = 2) + mapOf(monday to day(monday, underPromise))
        val today = monday.plusDays(1)

        val state = PromiseStreak.of(days, todayKept = true, today = today)

        assertEquals(0, state.weeksKept)
        assertEquals(monday, state.startDate)
        assertEquals(2, state.days) // Monday + today only
    }

    @Test
    fun `several kept weeks in a row all count`() {
        val days = week(monday.minusWeeks(3), kept = 7) +
            week(monday.minusWeeks(2), kept = 5) +
            week(monday.minusWeeks(1), kept = 6)

        val state = PromiseStreak.of(days, todayKept = true, today = monday)

        assertEquals(3, state.weeksKept)
        assertEquals(monday.minusWeeks(3), state.startDate)
        assertEquals(7 + 5 + 6 + 1, state.days) // + today
    }

    @Test
    fun `a finished week that is not fully on record stops the run without breaking it`() {
        val lastWeek = monday.minusWeeks(1)
        // Six days recorded, all kept — but a week we can't fully prove is never claimed.
        val partial = (0..5).associate { i ->
            val date = lastWeek.plusDays(i.toLong())
            date to day(date, underPromise)
        }

        val state = PromiseStreak.of(partial, todayKept = true, today = monday)

        assertEquals(0, state.weeksKept)
        assertEquals(monday, state.startDate)
        assertEquals(1, state.days) // today only
    }

    @Test
    fun `the week is at risk once five of seven can no longer be reached`() {
        // Sunday, with Mon-Sat all over the promise and today over too: nothing left to win.
        val today = monday.plusDays(6)
        val days = (0..5).associate { i ->
            val date = monday.plusDays(i.toLong())
            date to day(date, overPromise)
        }

        val state = PromiseStreak.of(days, todayKept = false, today = today)

        assertTrue(state.weekAtRisk)
        assertEquals(0, state.daysKeptThisWeek)
        assertEquals(0, state.daysLeftThisWeek)
        // Nothing kept yet, so there's no run to date.
        assertEquals(0, state.days)
        assertNull(state.startDate)
        assertTrue(state.isEmpty)
    }

    @Test
    fun `today counts while it is still under the promise, and not once it is over`() {
        val today = monday.plusDays(1)
        val days = mapOf(monday to day(monday, underPromise))

        assertEquals(2, PromiseStreak.of(days, todayKept = true, today = today).days)
        assertEquals(1, PromiseStreak.of(days, todayKept = false, today = today).days)
    }

    @Test
    fun `this week's progress counts a kept today once, as kept, not as open too`() {
        // Tuesday, Monday kept, today under: Mon and today kept, Wed-Sun still to come.
        val today = monday.plusDays(1)
        val days = mapOf(monday to day(monday, underPromise))

        val state = PromiseStreak.of(days, todayKept = true, today = today)

        assertEquals(2, state.daysKeptThisWeek) // Mon + today
        assertEquals(5, state.daysLeftThisWeek) // Wed..Sun
        assertFalse(state.weekAtRisk)
    }

    @Test
    fun `a week that can only reach four is at risk even while today is being kept`() {
        // Thursday: Mon-Wed over, today under so far. Thu-Sun is four days, one short of five.
        // Counting today as kept and as open too used to make this 1 + 4 = 5, "safe".
        val today = monday.plusDays(3)
        val days = (0..2).associate { i -> monday.plusDays(i.toLong()).let { it to day(it, overPromise) } }

        val state = PromiseStreak.of(days, todayKept = true, today = today)

        assertEquals(1, state.daysKeptThisWeek) // today
        assertEquals(3, state.daysLeftThisWeek) // Fri..Sun
        assertTrue(state.weekAtRisk)
    }

    @Test
    fun `a week that needs every remaining day is not yet at risk`() {
        // Wednesday: Mon-Tue over, today under. Wed-Sun is exactly five.
        val today = monday.plusDays(2)
        val days = (0..1).associate { i -> monday.plusDays(i.toLong()).let { it to day(it, overPromise) } }

        val state = PromiseStreak.of(days, todayKept = true, today = today)

        assertEquals(1, state.daysKeptThisWeek)
        assertEquals(4, state.daysLeftThisWeek)
        assertFalse(state.weekAtRisk)
    }

    @Test
    fun `a day is judged against the promise in force that day, not today's`() {
        // The promise was 8 hrs on Monday; 300 minutes was under it then.
        val days = mapOf(monday to day(monday, minutes = 300, promiseHours = 8))

        val state = PromiseStreak.of(days, todayKept = false, today = monday.plusDays(1))

        assertEquals(1, state.days)
    }
}
