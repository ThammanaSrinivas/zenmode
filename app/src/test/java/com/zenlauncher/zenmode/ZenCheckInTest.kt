package com.zenlauncher.zenmode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ZenCheckInTest {

    private val today = LocalDate.of(2026, 9, 9)
    private val eveningAt = LocalTime.of(20, 0)

    private val streak = PromiseStreak.State(
        days = 6,
        weeksKept = 1,
        startDate = today.minusDays(8),
        daysKeptThisWeek = 2,
        daysLeftThisWeek = 4,
        weekAtRisk = false
    )

    private fun due(
        at: LocalTime,
        promiseHours: Int = 4,
        todayMinutes: Long,
        streak: PromiseStreak.State = this.streak,
        shown: Map<ZenCheckInSlot, LocalDate> = emptyMap()
    ) = ZenCheckIn.due(
        now = LocalDateTime.of(today, at),
        eveningAt = eveningAt,
        promiseHours = promiseHours,
        todayMinutes = todayMinutes,
        streak = streak,
        lastShown = { slot -> shown[slot] }
    )

    // ── Last hour ─────────────────────────────────────────────────

    @Test
    fun `the last hour fires with an hour or less of promise left`() {
        // 4-hour promise, 3h10m used: 50 minutes left.
        val card = due(at = LocalTime.of(14, 0), todayMinutes = 190)

        assertEquals(ZenCheckInKind.LAST_HOUR, card?.kind)
        assertEquals(50L, card?.minutesLeft)
        assertEquals(ZenCheckInSlot.LAST_HOUR, card?.kind?.slot)
    }

    @Test
    fun `the last hour does not fire while more than an hour is left`() {
        // 4-hour promise, 2h used: 120 minutes left.
        assertNull(due(at = LocalTime.of(12, 0), todayMinutes = 120))
    }

    @Test
    fun `the last hour does not fire on a promise of two hours or less`() {
        // 50 minutes left of a 2-hour promise would be a warning you can't act on.
        assertNull(due(at = LocalTime.of(12, 0), promiseHours = 2, todayMinutes = 70))
        // One hour more and it's worth saying.
        assertEquals(
            ZenCheckInKind.LAST_HOUR,
            due(at = LocalTime.of(12, 0), promiseHours = 3, todayMinutes = 130)?.kind
        )
    }

    @Test
    fun `the last hour does not fire once the promise is already spent`() {
        // Nothing left to protect — the evening card's framing takes over instead.
        assertNull(due(at = LocalTime.of(12, 0), todayMinutes = 240))
        assertNull(due(at = LocalTime.of(12, 0), todayMinutes = 400))
    }

    @Test
    fun `the last hour fires only once a day`() {
        val shown = mapOf(ZenCheckInSlot.LAST_HOUR to today)
        assertNull(due(at = LocalTime.of(14, 0), todayMinutes = 190, shown = shown))
        // Yesterday's showing doesn't count against today.
        assertNotNull(
            due(
                at = LocalTime.of(14, 0),
                todayMinutes = 190,
                shown = mapOf(ZenCheckInSlot.LAST_HOUR to today.minusDays(1))
            )
        )
    }

    // ── Evening ───────────────────────────────────────────────────

    @Test
    fun `the evening card celebrates a day still under the promise`() {
        val card = due(at = LocalTime.of(20, 0), todayMinutes = 120)

        assertEquals(ZenCheckInKind.CELEBRATION, card?.kind)
        assertTrue(card!!.kind.confetti)
        // The requested line, said plainly.
        assertTrue(card.body.contains("Your screen limit is under promise"))
    }

    @Test
    fun `the evening card encourages a day already over, without confetti`() {
        val card = due(at = LocalTime.of(21, 30), todayMinutes = 300)

        assertEquals(ZenCheckInKind.ENCOURAGEMENT, card?.kind)
        assertEquals(false, card?.kind?.confetti)
        // The whole point of the weekly rule, stated where it matters most.
        assertTrue(card!!.body.contains("the week does"))
        assertTrue(card.headline.contains("still stands"))
    }

    @Test
    fun `nothing fires before the chosen evening time`() {
        assertNull(due(at = LocalTime.of(19, 59), todayMinutes = 120))
        assertNotNull(due(at = LocalTime.of(20, 0), todayMinutes = 120))
    }

    @Test
    fun `the evening card fires only once a day, whatever the day then does`() {
        val shown = mapOf(ZenCheckInSlot.EVENING to today)
        // Under the promise at 20:00 and over it by 21:00 is still one card, not two.
        assertNull(due(at = LocalTime.of(21, 0), todayMinutes = 300, shown = shown))
    }

    @Test
    fun `the last hour wins a tie and does not consume the evening slot`() {
        // 20:30, 50 minutes of promise left: the time-critical card goes first.
        val first = due(at = LocalTime.of(20, 30), todayMinutes = 190)
        assertEquals(ZenCheckInKind.LAST_HOUR, first?.kind)

        // Having shown it, the evening card is still due in the same evening.
        val second = due(
            at = LocalTime.of(20, 35),
            todayMinutes = 190,
            shown = mapOf(ZenCheckInSlot.LAST_HOUR to today)
        )
        assertEquals(ZenCheckInKind.CELEBRATION, second?.kind)
    }

    // ── Copy ──────────────────────────────────────────────────────

    @Test
    fun `copy is tailored to the streak length`() {
        fun headlineFor(days: Int) = ZenCheckIn.headline(
            ZenCheckInKind.CELEBRATION,
            streak.copy(days = days)
        )

        // Distinct tiers, not one line with a number swapped in.
        val tiers = listOf(1, 3, 6, 12, 25, 45, 120).map(::headlineFor)
        assertEquals(tiers.size, tiers.distinct().size)
        assertTrue(headlineFor(1).contains("One day"))
        assertTrue(headlineFor(25).contains("25"))
    }

    @Test
    fun `an at-risk week is told plainly rather than cheered`() {
        val atRisk = streak.copy(daysKeptThisWeek = 1, daysLeftThisWeek = 1, weekAtRisk = true)
        val card = ZenCheckIn.card(ZenCheckInKind.ENCOURAGEMENT, atRisk, promiseHours = 4, todayMinutes = 300)

        assertTrue(card.headline.contains("so has the week"))
        assertTrue(card.body.contains("restarts Monday"))
    }

    @Test
    fun `a won week is not asked for more days`() {
        val won = streak.copy(daysKeptThisWeek = 5, daysLeftThisWeek = 2)
        val card = ZenCheckIn.card(ZenCheckInKind.CELEBRATION, won, promiseHours = 4, todayMinutes = 60)

        assertTrue(card.body.contains("already won"))
    }

    @Test
    fun `duration phrases read naturally`() {
        assertEquals("45 minutes", ZenCheckIn.durationPhrase(45))
        assertEquals("1 minute", ZenCheckIn.durationPhrase(1))
        assertEquals("2 hours", ZenCheckIn.durationPhrase(120))
        assertEquals("1 hour", ZenCheckIn.durationPhrase(60))
        assertEquals("1h 5m", ZenCheckIn.durationPhrase(65))
        assertEquals("no time", ZenCheckIn.durationPhrase(0))
    }

    // ── Settings time picker ──────────────────────────────────────

    @Test
    fun `the picker window is a finite set of half-hour slots`() {
        // Regression: the slots used to be built by stepping a LocalTime until it passed the
        // 23:30 bound. LocalTime.plusMinutes wraps round midnight, so 23:30 + 30 = 00:00, which
        // is never "after" 23:30 -- the loop never ended and the app died with an OOM the
        // moment the sheet opened. Minute-of-day arithmetic can't wrap.
        val first = ZenCheckInPreferences.earliest
        val last = ZenCheckInPreferences.latest
        val firstMinute = first.hour * 60 + first.minute
        val lastMinute = last.hour * 60 + last.minute
        val slots = (firstMinute..lastMinute step 30).map { LocalTime.of(it / 60, it % 60) }

        assertEquals(LocalTime.of(16, 0), slots.first())
        assertEquals(LocalTime.of(23, 30), slots.last())
        assertEquals(16, slots.size)
        assertEquals(slots.size, slots.distinct().size)
        assertTrue(slots.zipWithNext().all { (a, b) -> a.isBefore(b) })
    }
    // ── Last-hour card: the win leads, the hour follows ───────────

    @Test
    fun `the last hour leads with the streak, not the clock`() {
        val card = ZenCheckIn.card(ZenCheckInKind.LAST_HOUR, streak, promiseHours = 4, todayMinutes = 190)

        // The loudest line on the card is the win itself.
        assertEquals("6-day streak", card.headline)
        // ...and it carries no number other than the streak.
        assertFalse(card.headline.contains("50"))
        assertFalse(card.headline.contains("hour"))
    }

    @Test
    fun `the last hour asks for one achievable thing and never threatens`() {
        val card = ZenCheckIn.card(ZenCheckInKind.LAST_HOUR, streak, promiseHours = 4, todayMinutes = 190)

        assertEquals("Stay under it and you bank day 6.", card.body)
        for (threat in listOf("undo", "never", "lose", "breaks", "Don't")) {
            assertFalse("body should not threaten: ${card.body}", card.body.contains(threat))
        }
    }

    @Test
    fun `a first-time user is asked for day one rather than day zero`() {
        val fresh = streak.copy(days = 0, startDate = null)
        val card = ZenCheckIn.card(ZenCheckInKind.LAST_HOUR, fresh, promiseHours = 4, todayMinutes = 190)

        assertEquals("Today can be day one", card.headline)
        assertEquals("Stay under it and you bank your first day.", card.body)
    }

    @Test
    fun `the primary action has a real job`() {
        val card = ZenCheckIn.card(ZenCheckInKind.LAST_HOUR, streak, promiseHours = 4, todayMinutes = 190)
        assertEquals("Lock in today", card.actionLabel)
    }

    @Test
    fun `the buffer is spelled in the largest unit that stays whole`() {
        assertEquals("50 min", ZenCheckIn.bufferCompact(50))
        assertEquals("1 min", ZenCheckIn.bufferCompact(1))
        assertEquals("1 hr", ZenCheckIn.bufferCompact(60))
        assertEquals("2 hr", ZenCheckIn.bufferCompact(120))
        assertEquals("1h 20m", ZenCheckIn.bufferCompact(80))
        assertEquals("0 min", ZenCheckIn.bufferCompact(0))
    }

    @Test
    fun `the week is demoted to one line with the arithmetic already done`() {
        val line = ZenCheckIn.weekQuotaLine(streak.copy(daysKeptThisWeek = 2))
        assertEquals("This week: 2 of 5 banked \u00B7 3 to go", line)

        // Quota met reads as met, never as "0 to go".
        val met = ZenCheckIn.weekQuotaLine(streak.copy(daysKeptThisWeek = 5))
        assertEquals("This week: 5 of 5 banked \u00B7 quota met", met)
        // ...and an over-achieving week never reports more than the quota.
        val over = ZenCheckIn.weekQuotaLine(streak.copy(daysKeptThisWeek = 7))
        assertEquals("This week: 5 of 5 banked \u00B7 quota met", over)
    }

    @Test
    fun `the coin row banks every day but today`() {
        assertEquals("12 days banked, today almost banked", ZenCheckIn.coinsSpoken(13))
        assertEquals("0 days banked, today almost banked", ZenCheckIn.coinsSpoken(0))
    }
}
