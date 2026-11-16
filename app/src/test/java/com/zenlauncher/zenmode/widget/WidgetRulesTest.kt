package com.zenlauncher.zenmode.widget

import com.zenlauncher.zenmode.coreapi.CircleMember
import com.zenlauncher.zenmode.coreapi.CircleRole
import com.zenlauncher.zenmode.coreapi.week.MomentCopy
import com.zenlauncher.zenmode.widget.WidgetFixtures.Moment
import com.zenlauncher.zenmode.widget.art.Surface
import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.widget.art.WidgetLight
import com.zenlauncher.zenmode.widget.faces.BloomCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** The rules every widget shares: one surface rule, one hill, one circle ranking, one bloom. */
class WidgetRulesTest {

    @Test
    fun `a won week turns every widget gold, a perfect one night, a quiet session night`() {
        assertEquals(Surface.GOLD, WidgetFixtures.data(Moment.WON).surfaceFor(dark = false))
        assertEquals(Surface.GOLD, WidgetFixtures.data(Moment.WON).surfaceFor(dark = true))
        assertEquals(Surface.NIGHT, WidgetFixtures.data(Moment.PERFECT).surfaceFor(dark = false))
        assertEquals(Surface.NIGHT, WidgetFixtures.data(quiet = WidgetFixtures.running).surfaceFor(dark = false))
        assertEquals(Surface.PAPER, WidgetFixtures.data(quiet = WidgetFixtures.running).surfaceFor(dark = false, followsQuiet = false))
        assertEquals(Surface.INK, WidgetFixtures.data(Moment.GOING).surfaceFor(dark = true))
        assertEquals(Surface.PAPER, WidgetFixtures.data(Moment.GOING).surfaceFor(dark = false))
    }

    @Test
    fun `after 21_30 the moment widgets take the night stage, the others keep the theme`() {
        val night = WidgetFixtures.data(Moment.GOING, part = DayPart.NIGHT)
        assertEquals(Surface.NIGHT, night.surfaceFor(dark = false, followsNight = true))
        assertEquals(Surface.PAPER, night.surfaceFor(dark = false))
        assertEquals("a won week at night is night, its glow gold", Surface.NIGHT, WidgetFixtures.data(Moment.WON, part = DayPart.NIGHT).surfaceFor(dark = false, followsNight = true))
    }

    @Test
    fun `the hill is today - calm, drifting, over, or nothing measured`() {
        assertEquals(WidgetLight.Mood.CALM, WidgetFixtures.data(Moment.CALM).mood)
        assertEquals(WidgetLight.Mood.DRIFT, WidgetFixtures.data(Moment.ONE).mood)
        assertEquals(WidgetLight.Mood.OVER, WidgetFixtures.data(Moment.ONE, minutesToday = 250).mood)
        assertEquals(WidgetLight.Mood.NONE, WidgetFixtures.data(usageAccess = false).mood)
    }

    @Test
    fun `usage access off asks once, and never for manual entry`() {
        val d = WidgetFixtures.data(usageAccess = false)
        assertEquals(MomentCopy.USAGE_ACCESS_OFF, d.zenoLine)
        assertFalse(d.zenoLine.lowercase().contains("log"))
    }

    @Test
    fun `the last hour of the promise is said, and only inside it`() {
        assertTrue(WidgetFixtures.data(Moment.ONE, minutesToday = 205).lastHour)
        assertEquals("35 minutes of your promise left.", WidgetFixtures.data(Moment.ONE, minutesToday = 205).zenoLine)
        assertFalse(WidgetFixtures.data(Moment.ONE, minutesToday = 120).lastHour)
        assertFalse(WidgetFixtures.data(Moment.ONE, minutesToday = 260).lastHour)
        assertFalse("a won week needs no countdown", WidgetFixtures.data(Moment.WON, minutesToday = 205).lastHour)
    }

    @Test
    fun `a poke gets an answer and a brighter Zeno`() {
        val d = WidgetFixtures.data(Moment.STAKE, pokes = 1, answering = true)
        assertEquals(MomentCopy.pokeReply(d.moment, 1), d.zenoLine)
        assertEquals(d.moment.feeling.lifted, d.zenoFeeling)
    }

    @Test
    fun `day scores read the week, today marked while measured`() {
        val d = WidgetFixtures.data(Moment.GOING)
        assertEquals(listOf("7.8", "6.9", "5.5", "", "", "", ""), d.dayScores)
    }

    @Test
    fun `the circle ranks by today's score, a stale score sits last`() {
        val today = LocalDate.of(2026, 11, 12)
        val noonToday = today.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val yesterday = noonToday - 86_400_000L
        fun m(uid: String, name: String, score: Int, at: Long) = CircleMember(uid, name, score, 0, at, CircleRole.MEMBER, 0)
        val c = CircleState.rank(
            "Morning people",
            listOf(m("a", "Asha Rao", 86, noonToday), m("me", "Kamal", 74, noonToday), m("r", "Ravi", 95, yesterday)),
            myUid = "me", today = today, zone = ZoneOffset.UTC
        ) as CircleState.Ranked
        assertEquals(listOf("Asha", "Kamal", "Ravi"), c.seats.map { it.name })
        assertEquals(2, c.myRank)
        assertEquals(12, c.behindBy)
        assertNull(c.seats.last().scoreTenths)
        assertEquals(CircleState.Alone("Solo"), CircleState.rank("Solo", listOf(m("me", "Kamal", 74, noonToday)), "me", today, ZoneOffset.UTC))
    }

    @Test
    fun `gold still needs the days the app's own gate needs`() {
        val g = GoldState("2,724", invested = true, open = false, kept = 3, outOfReach = false)
        assertEquals(2, g.stillNeeded)
        assertEquals(0, g.copy(kept = 6).stillNeeded)
    }

    @Test
    fun `the bloom reads each hour, and never past now`() {
        assertEquals(BloomCell.AWAY, BloomCell.classify(10, 0.4f, 0f))
        assertEquals(BloomCell.REST, BloomCell.classify(3, 0f, 0f))
        assertEquals(BloomCell.UNPLANNED, BloomCell.classify(22, 30f, 20f))
        assertEquals(BloomCell.ON_PURPOSE, BloomCell.classify(14, 30f, 4f))
        assertEquals(BloomCell.LIGHT, BloomCell.classify(14, 6f, 0f))
        val monday = LocalDate.of(2026, 11, 9)
        val cells = WidgetBloomLoader.cells(FloatArray(168), FloatArray(168), monday, monday.plusDays(2), nowHour = 15)
        assertEquals(168, cells.size)
        assertEquals(BloomCell.NOW, cells[2 * 24 + 15])
        assertEquals(BloomCell.FUTURE, cells[2 * 24 + 16])
        assertEquals(BloomCell.FUTURE, cells[6 * 24])
        assertEquals(BloomCell.AWAY, cells[24 + 10])
    }
}
