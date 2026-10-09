package com.zenlauncher.zenmode.share

import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.PromiseStreak
import com.zenlauncher.zenmode.PromiseUnit
import com.zenlauncher.zenmode.ZenGoldPromiseState
import com.zenlauncher.zenmode.recap.DayRecord
import com.zenlauncher.zenmode.recap.WeeklyRecap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** What every share card says, tier by tier — the words are as much the design as the art. */
class ShareCopyTest {

    private val today = LocalDate.of(2026, 9, 13)

    // ── Streak ────────────────────────────────────────────────────
    // The promise streak (PromiseStreak): days kept, judged by the week — not consecutive days.

    private val thisWeek = listOf(true, true, true, null, null, null, null)

    private fun streak(days: Int, weeksWon: Int = days / 7, keptThisWeek: Int = 3, atRisk: Boolean = false) =
        StreakShare(days, weeksWon, keptThisWeek, atRisk, thisWeek, today)

    @Test
    fun `streak tiers change exactly at their milestones`() {
        val expected = mapOf(
            0 to StreakTier.RELIGHT, 1 to StreakTier.SPARK, 6 to StreakTier.SPARK,
            7 to StreakTier.WEEK, 13 to StreakTier.WEEK, 14 to StreakTier.ROOTED, 29 to StreakTier.ROOTED,
            30 to StreakTier.MOON, 59 to StreakTier.MOON, 60 to StreakTier.TIDE, 89 to StreakTier.TIDE,
            90 to StreakTier.SEASON, 179 to StreakTier.SEASON, 180 to StreakTier.HALF_ORBIT,
            364 to StreakTier.HALF_ORBIT, 365 to StreakTier.ORBIT, 1_000 to StreakTier.ORBIT
        )
        expected.forEach { (days, tier) -> assertEquals("day $days", tier, StreakTier.of(days)) }
    }

    @Test
    fun `a negative count reads as a fresh start, not a crash`() {
        assertEquals(StreakTier.RELIGHT, streak(-3).tier)
        assertEquals("0", streak(-3).stats.first().value)
    }

    @Test
    fun `thirty kept days is a full moon, counted as kept days and weeks won`() {
        val share = streak(30, weeksWon = 4, keptThisWeek = 2)
        assertEquals("One full moon.", share.title)
        assertTrue(share.caption.startsWith("30 days kept"))
        assertEquals("30 DAY STREAK", share.stamp)
        assertEquals(listOf("DAYS KEPT", "WEEKS WON", "THIS WEEK"), share.stats.map { it.label })
        assertEquals(listOf("30", "4", "2/7"), share.stats.map { it.value })
    }

    @Test
    fun `kept days aren't consecutive, so no line ever says in a row`() {
        listOf(0, 1, 3, 7, 10, 20, 30, 64, 120, 200, 365, 800).forEach { days ->
            val share = streak(days)
            listOf(share.title, share.caption, share.shareText, share.description).forEach {
                assertFalse("day $days: $it", it.contains("in a row"))
            }
        }
    }

    @Test
    fun `ninety days is a season and a year is an orbit`() {
        assertEquals("A whole season.", streak(90).title)
        assertEquals("One full orbit.", streak(365).title)
        assertEquals("2 full orbits.", streak(800).title)
    }

    @Test
    fun `day one and a fresh start speak for themselves`() {
        assertEquals("Day one. Kept.", streak(1).title)
        assertEquals("One day kept under my screen-time promise. Six more to a full week.", streak(1).caption)
        assertEquals("Day one starts now.", streak(0).title)
        assertEquals("FRESH START", streak(0).stamp)
    }

    @Test
    fun `the next milestone counts kept days to the next tier`() {
        assertEquals("Next: one week, 4 kept days to go.", streak(3).nextMilestone)
        assertEquals("Next: one full moon, 1 kept day to go.", streak(29).nextMilestone)
        assertEquals("Next: a full orbit, 165 kept days to go.", streak(200).nextMilestone)
        assertEquals("Next: 2 full orbits, 365 kept days to go.", streak(365).nextMilestone)
        assertEquals("One day under the promise starts it.", streak(0).nextMilestone)
        assertNull(streak(365 * 3).nextMilestone)
    }

    @Test
    fun `a week that can no longer be won says the streak restarts Monday`() {
        assertEquals(
            "This week can't reach 5 of 7 any more, so the streak restarts Monday.",
            streak(40, atRisk = true).nextMilestone
        )
    }

    @Test
    fun `it reads straight off the promise streak Home's flame shows`() {
        val state = PromiseStreak.State(days = 12, weeksKept = 1, daysKeptThisWeek = 4, daysLeftThisWeek = 2)
        val units = thisWeek.mapIndexed { i, kept -> PromiseUnit("MTWTFSS"[i].toString(), kept) }
        val share = StreakShare.of(state, units, today)
        assertEquals(12, share.days)
        assertEquals(1, share.weeksWon)
        assertEquals(4, share.keptThisWeek)
        assertEquals(thisWeek, share.week)
    }

    @Test
    fun `every streak share carries the store link`() {
        listOf(0, 3, 30, 400).forEach { assertTrue(streak(it).shareText.endsWith(AppConstants.PLAY_STORE_URL)) }
    }

    // ── Zen Score ─────────────────────────────────────────────────

    @Test
    fun `score bands split the scale into five states of water`() {
        assertEquals(ScoreBand.STORMY, ScoreBand.of(0))
        assertEquals(ScoreBand.STORMY, ScoreBand.of(39))
        assertEquals(ScoreBand.CHOPPY, ScoreBand.of(40))
        assertEquals(ScoreBand.STEADY, ScoreBand.of(60))
        assertEquals(ScoreBand.CALM, ScoreBand.of(80))
        assertEquals(ScoreBand.STILL, ScoreBand.of(90))
        assertEquals(ScoreBand.STILL, ScoreBand.of(140))
    }

    @Test
    fun `score cards compare with yesterday when there is one`() {
        assertEquals("+0.6", ScoreShare(84, 78, 30, today).delta)
        assertEquals("−0.4", ScoreShare(74, 78, 30, today).delta)
        assertEquals("LEVEL", ScoreShare(74, 74, 30, today).delta)
        assertEquals("—", ScoreShare(74, null, 30, today).delta)
        assertEquals("Down 0.4 on yesterday. There's still time today.", ScoreShare(74, 78, 30, today).note)
    }

    @Test
    fun `won-back time is today's, formatted like the rest of the app`() {
        val share = ScoreShare(84, 78, 75, today)
        assertEquals("1h 15m", share.stats.last().value)
        assertEquals("0m", ScoreShare(84, 78, -20, today).stats.last().value)
    }

    @Test
    fun `score titles follow the water`() {
        assertEquals("Still water.", ScoreShare(95, null, 0, today).title)
        assertEquals("A stormy one.", ScoreShare(20, null, 0, today).title)
        assertTrue(ScoreShare(84, null, 0, today).caption.startsWith("8.4 out of 10"))
    }

    // ── Zen Gold ──────────────────────────────────────────────────

    private fun week(vararg kept: Boolean?) = ZenGoldPromiseState(
        promiseHours = 4,
        units = listOf("M", "T", "W", "T", "F", "S", "S").mapIndexed { i, l -> PromiseUnit(l, kept.getOrNull(i)) },
        unitsKept = kept.count { it == true },
        unitsMissed = kept.count { it == false },
        unitsRemaining = kept.count { it == null },
        goalMet = kept.count { it == true } >= AppConstants.PROMISE_DAYS_TO_UNLOCK
    )

    @Test
    fun `gold tiers grow with the balance, and with the promise before there is one`() {
        val earning = week(true, true, null, null, null, null, null)
        val open = week(true, true, true, true, true, null, null)
        assertEquals(GoldTier.EARNING, GoldShare(0, 0, earning, today).tier)
        assertEquals(GoldTier.UNLOCKED, GoldShare(0, 0, open, today).tier)
        assertEquals(GoldTier.FIRST, GoldShare(1, 0, earning, today).tier)
        assertEquals(GoldTier.STACK, GoldShare(1_000, 0, earning, today).tier)
        assertEquals(GoldTier.BARS, GoldShare(10_000, 0, earning, today).tier)
        assertEquals(GoldTier.VAULT, GoldShare(1_00_000, 0, earning, today).tier)
    }

    @Test
    fun `gold amounts use whole rupees in Indian grouping`() {
        assertEquals("₹2,350", GoldShare.rupeesLabel(2_350))
        assertEquals("₹1,25,000", GoldShare.rupeesLabel(1_25_000))
        assertEquals(2_350L, GoldShare.rupees("2,350"))
        assertEquals(0L, GoldShare.rupees("—"))
    }

    @Test
    fun `an earning week counts down to gold pay`() {
        val share = GoldShare(0, 0, week(true, true, false, true, null, null, null), today)
        assertEquals("3 of 5 promise days kept this week. 2 more and my calm turns into gold.", share.caption)
        assertEquals("2 more kept days and gold pay opens.", share.nextStep)
        assertEquals(listOf("3/7", "4H/DAY", "AT 5"), share.stats.map { it.value })
    }

    @Test
    fun `a week that can no longer unlock says so kindly`() {
        val share = GoldShare(0, 0, week(false, false, false, true, false, false, null), today)
        assertTrue(share.week.goalOutOfReach)
        assertEquals("A fresh week starts Monday.", share.nextStep)
        assertFalse(share.caption.contains("more and my calm"))
    }

    // ── Weekly ────────────────────────────────────────────────────

    private fun recap(vararg hours: Double, previous: Long? = 2_000) = WeeklyRecap(
        weekStart = LocalDate.of(2026, 9, 7),
        days = hours.mapIndexed { i, h -> DayRecord(LocalDate.of(2026, 9, 7).plusDays(i.toLong()), (h * 60).toLong(), 4, emptyList(), 0, null) },
        previousWeekTotalMinutes = previous
    )

    @Test
    fun `a week is perfect, kept or honest`() {
        assertEquals(WeekTier.PERFECT, WeeklyShare(recap(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)).tier)
        assertEquals(WeekTier.KEPT, WeeklyShare(recap(1.0, 5.0, 1.0, 1.0, 1.0, 5.0, 1.0)).tier)
        assertEquals(WeekTier.MISSED, WeeklyShare(recap(5.0, 5.0, 5.0, 1.0, 1.0, 5.0, 1.0)).tier)
    }

    @Test
    fun `a kept week names its calmest day and what the saved time adds up to`() {
        val share = WeeklyShare(recap(2.0, 1.0, 3.0, 2.0, 2.0, 3.0, 2.0, previous = 1_500))
        assertEquals("MY WEEK IN ZEN · SEP 7 – 13", share.eyebrow)
        assertEquals("CALMEST DAY", share.highlights[0].label)
        assertEquals("Tuesday · 1h 00m", share.highlights[0].value)
        assertEquals("WON BACK", share.highlights[1].label)
        assertEquals("▼ 10h 00m vs last week", share.changeLabel)
        assertTrue(share.changeBetter)
    }

    @Test
    fun `a missed week names the toughest day and one thing to try`() {
        val share = WeeklyShare(recap(5.0, 6.0, 5.0, 1.0, 1.0, 7.0, 1.0))
        assertEquals("An honest week.", share.headline)
        assertEquals("TOUGHEST DAY", share.highlights[0].label)
        assertEquals("Saturday · 7h 00m", share.highlights[0].value)
        assertEquals("NEXT WEEK", share.highlights[1].label)
    }

    @Test
    fun `no week before means no comparison pill`() {
        assertNull(WeeklyShare(recap(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, previous = null)).changeLabel)
    }

    @Test
    fun `the story's notes climb the scale in the app's key`() {
        val rates = Pentatonic.rising.toList()
        assertEquals(7, rates.size)
        assertEquals(rates.sorted(), rates)
        assertTrue(rates.all { it in 0.5f..2f })
    }

    @Test
    fun `the clip keeps the art's shape when the encoder takes it and falls back to 720 wide`() {
        assertEquals(1080 to 1920, ShareClip.encoderSize(ShareFormat.STORY) { _, _ -> true })
        assertEquals(720 to 1280, ShareClip.encoderSize(ShareFormat.STORY) { _, _ -> false })
        assertEquals(720 to 900, ShareClip.encoderSize(ShareFormat.POST) { _, _ -> false })
    }
}
