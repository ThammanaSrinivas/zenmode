package com.zenlauncher.zenmode.island

import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.widget.WidgetFixtures
import com.zenlauncher.zenmode.widget.WidgetFixtures.Moment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** The Zen Island's rules: the phone's tier, where it sits, what gets in, when the week shows, what takes the pill. */
class IslandRulesTest {

    // ── Which phones start with it on ─────────────────────────────────

    private fun facts(sdk: Int = 34, lowRam: Boolean = false, ramMb: Long = 7_600, pc: Int = 0, cores: Int = 8) =
        IslandDevice.Facts(sdk, lowRam, ramMb, pc, cores)

    @Test
    fun `confident phones start on, low-memory and older phones start off`() {
        assertEquals(IslandDevice.Tier.FULL, IslandDevice.tier(facts()))
        assertEquals(IslandDevice.Tier.FULL, IslandDevice.tier(facts(ramMb = 3_700, pc = 31, cores = 4)))
        assertEquals(IslandDevice.Tier.FULL, IslandDevice.tier(facts(ramMb = 3_700, sdk = 31, cores = 8)))
        assertEquals(IslandDevice.Tier.LITE, IslandDevice.tier(facts(lowRam = true)))
        assertEquals(IslandDevice.Tier.LITE, IslandDevice.tier(facts(sdk = 28)))
        assertEquals(IslandDevice.Tier.LITE, IslandDevice.tier(facts(ramMb = 2_600)))
        // A 4 GB phone with four cores (the emulator we test on) is not one we are sure of.
        assertEquals(IslandDevice.Tier.LITE, IslandDevice.tier(facts(ramMb = 3_960, cores = 4)))
        assertEquals(IslandDevice.Tier.LITE, IslandDevice.tier(facts(ramMb = 3_700, sdk = 30, cores = 8)))
    }

    // ── Where it sits ─────────────────────────────────────────────────

    private val d = 2.75f
    private val screen = 1080f
    private val bar = 136f

    @Test
    fun `a centred punch hole is wrapped by the pill`() {
        val hole = Cutout(500f, 30f, 580f, 110f)
        val g = IslandGeometry.of(screen, bar, listOf(hole), d, Placement.CAMERA)
        assertTrue(g.onCamera)
        assertEquals(540f, g.centerX, 0.1f)
        assertEquals(hole.centerY, g.top + g.pillHeight / 2, 0.5f)
        assertTrue(g.cameraGap > hole.width)
        assertEquals(0f, g.avoidStart, 0f)
    }

    @Test
    fun `a corner camera leaves the pill centred and the card's top row clear of it`() {
        val corner = Cutout(41f, 37f, 121f, 117f)
        val g = IslandGeometry.of(screen, bar, listOf(corner), d, Placement.CAMERA)
        assertFalse(g.onCamera)
        assertEquals(540f, g.centerX, 0.1f)
        assertTrue("the band starts after the camera", g.avoidStart > 0f)
        assertEquals(0f, g.avoidEnd, 0f)
    }

    @Test
    fun `a wide notch is sat under, no camera at all sits in the status bar`() {
        val notch = Cutout(262f, 0f, 818f, 120f)
        val under = IslandGeometry.of(screen, bar, listOf(notch), d, Placement.CAMERA)
        assertFalse(under.onCamera)
        assertTrue(under.top >= notch.bottom)
        val none = IslandGeometry.of(screen, bar, emptyList(), d, Placement.CAMERA)
        assertTrue(none.top + none.pillHeight <= bar)
        assertEquals(540f, none.centerX, 0.1f)
    }

    @Test
    fun `without the accessibility service it docks just under the status bar`() {
        val g = IslandGeometry.of(screen, bar, listOf(Cutout(500f, 30f, 580f, 110f)), d, Placement.DOCKED)
        assertFalse(g.onCamera)
        assertTrue(g.top > bar)
    }

    @Test
    fun `the open card is the screen less a margin, never wider than a phone`() {
        val phone = IslandGeometry.of(screen, bar, emptyList(), d, Placement.CAMERA)
        assertEquals(screen - 2 * IslandGeometry.CARD_MARGIN_DP * d, phone.cardWidth, 0.5f)
        val tablet = IslandGeometry.of(2560f, bar, emptyList(), 2f, Placement.CAMERA)
        assertEquals(IslandGeometry.CARD_MAX_DP * 2f, tablet.cardWidth, 0.5f)
    }

    // ── Which notifications get in ────────────────────────────────────

    private fun note(
        pkg: String = "com.whatsapp", own: Boolean = false, category: String? = "msg", ongoing: Boolean = false,
        importance: Int = 4, dnd: Boolean = true, chronometer: Boolean = false, promoted: Boolean = false,
        progress: Boolean = false, media: Boolean = false, summary: Boolean = false, quietUpdate: Boolean = false, words: Boolean = true
    ) = NoteRules.Facts(pkg, own, category, ongoing, summary, importance, dnd, chronometer, promoted, progress, media, false, quietUpdate, words)

    private val ctx = NoteRules.Context(peeksOn = true, liveOn = true, quiet = false, foregroundPkg = "com.zenlauncher.zenmode", quietedApps = setOf("com.instagram.android"))

    @Test
    fun `a message peeks, a call and a timer stay, music comes from its session`() {
        assertEquals(NoteRules.Route.PEEK, NoteRules.route(note(), ctx))
        assertEquals(NoteRules.Route.CALL, NoteRules.route(note(category = "call", ongoing = true), ctx))
        assertEquals(NoteRules.Route.LIVE, NoteRules.route(note(category = null, ongoing = true, chronometer = true), ctx))
        assertEquals(NoteRules.Route.LIVE, NoteRules.route(note(category = null, ongoing = true, promoted = true), ctx))
        assertEquals(NoteRules.Route.LIVE, NoteRules.route(note(category = "progress", ongoing = true, progress = true), ctx))
        assertEquals(NoteRules.Route.IGNORE, NoteRules.route(note(category = "transport", media = true), ctx))
    }

    @Test
    fun `nothing quiet, silent, summarised or already on screen peeks`() {
        assertEquals(NoteRules.Route.IGNORE, NoteRules.route(note(importance = 2), ctx))
        assertEquals(NoteRules.Route.IGNORE, NoteRules.route(note(dnd = false), ctx))
        assertEquals(NoteRules.Route.IGNORE, NoteRules.route(note(summary = true), ctx))
        assertEquals(NoteRules.Route.IGNORE, NoteRules.route(note(quietUpdate = true), ctx))
        assertEquals(NoteRules.Route.IGNORE, NoteRules.route(note(words = false), ctx))
        assertEquals(NoteRules.Route.IGNORE, NoteRules.route(note(category = "alarm"), ctx))
        assertEquals(NoteRules.Route.IGNORE, NoteRules.route(note(ongoing = true, category = null), ctx))
        assertEquals(NoteRules.Route.IGNORE, NoteRules.route(note(pkg = "com.zenlauncher.zenmode"), ctx))
    }

    @Test
    fun `a Quiet session or a quieted app holds messages back, but never a call`() {
        val quiet = ctx.copy(quiet = true)
        assertEquals(NoteRules.Route.HOLD, NoteRules.route(note(), quiet))
        assertEquals(NoteRules.Route.HOLD, NoteRules.route(note(pkg = "com.instagram.android"), ctx))
        assertEquals(NoteRules.Route.CALL, NoteRules.route(note(category = "call", ongoing = true), quiet))
    }

    @Test
    fun `the switches in Settings are obeyed`() {
        assertEquals(NoteRules.Route.IGNORE, NoteRules.route(note(), ctx.copy(peeksOn = false)))
        assertEquals(NoteRules.Route.IGNORE, NoteRules.route(note(category = "call", ongoing = true), ctx.copy(liveOn = false)))
        assertEquals(NoteRules.Route.IGNORE, NoteRules.route(note(own = true, ongoing = true, chronometer = true, category = null), ctx))
    }

    @Test
    fun `the island opens only for what the system would not show as a banner`() {
        assertTrue(NoteRules.opens(3))
        assertFalse(NoteRules.opens(4))
    }

    // ── When the week is in it ────────────────────────────────────────

    private val today: LocalDate = WidgetFixtures.monday.plusDays(3)
    private val nine = 9 * HOUR
    private val night = 21 * HOUR + 30 * MINUTE

    private fun decide(moment: Moment, now: Long = nine, part: DayPart = DayPart.MORNING, log: MomentPlan.Log = MomentPlan.Log()) =
        MomentPlan.decide(WidgetFixtures.data(moment).moment, part, now, today, night, log)

    @Test
    fun `a won week arrives open, for an hour, and keeps its own hour`() {
        val first = decide(Moment.WON)
        assertNotNull(first)
        assertEquals(MomentPlan.Reason.CELEBRATION, first!!.reason)
        assertTrue(first.arrives)
        assertEquals(nine + HOUR, first.endsAt)
        val log = MomentPlan.Log(today, first.startedAt, first.reason, first.endsAt)
        val later = decide(Moment.WON, now = nine + 30 * MINUTE, log = log)
        assertEquals(first.endsAt, later!!.endsAt)
        assertFalse(later.arrives)
        assertNull("gone once its hour is up", decide(Moment.WON, now = nine + 2 * HOUR, log = log))
    }

    @Test
    fun `a decisive day stays until the night, eight hours at most`() {
        val show = decide(Moment.STAKE, now = 15 * HOUR, part = DayPart.AFTERNOON)
        assertEquals(MomentPlan.Reason.DECISIVE_DAY, show!!.reason)
        assertEquals(night, show.endsAt)
        val early = decide(Moment.ONE, now = 6 * HOUR)
        assertEquals(6 * HOUR + MomentPlan.DECISIVE_MAX_MILLIS, early!!.endsAt)
    }

    @Test
    fun `nothing at night, nothing on an ordinary or hard day, nothing once swiped away`() {
        assertNull(decide(Moment.ONE, part = DayPart.NIGHT))
        assertNull(decide(Moment.GOING))
        assertNull(decide(Moment.OUT))
        assertNull(decide(Moment.ONE, log = MomentPlan.Log(dismissedDay = today)))
    }

    @Test
    fun `one a day - a celebration may replace a decisive day, not the other way round`() {
        val decisive = MomentPlan.Log(today, nine, MomentPlan.Reason.DECISIVE_DAY, night)
        assertEquals(MomentPlan.Reason.CELEBRATION, decide(Moment.WON, log = decisive)!!.reason)
        val celebrated = MomentPlan.Log(today, nine, MomentPlan.Reason.CELEBRATION, nine + HOUR)
        assertNull(decide(Moment.ONE, now = nine + 2 * HOUR, log = celebrated))
    }

    // ── What takes the pill ───────────────────────────────────────────

    private val quiet = QuietItem(0L, 25 * MINUTE)
    private val media = MediaItem("p", "Music", "Song", "Artist", null, 0, true, 0L, 1L, 0L, 1f)
    private val call = CallItem("k", "Asha", 0L, null, emptyList())
    private val peek = ChargingPeek(54)

    @Test
    fun `the most important live thing takes the pill and the next becomes the bubble`() {
        val s = IslandStage.scene(listOf(media, quiet), null, null, hidden = false)
        assertSame(quiet, s.main)
        assertSame(media, s.bubble)
        assertFalse(s.open)
    }

    @Test
    fun `opening the bubble swaps it into the card`() {
        val s = IslandStage.scene(listOf(media, quiet), null, MediaItem.ID, hidden = false)
        assertSame(media, s.main)
        assertSame(quiet, s.bubble)
        assertTrue(s.open)
    }

    @Test
    fun `a peek takes the island while it passes, except over a call`() {
        val s = IslandStage.scene(listOf(quiet, media), peek, null, hidden = false)
        assertSame(peek, s.peek)
        assertNull(s.bubble)
        assertEquals(peek.id, s.shownId)
        val withCall = IslandStage.scene(listOf(quiet, call), peek, null, hidden = false)
        assertNull(withCall.peek)
        assertSame(call, withCall.main)
    }

    @Test
    fun `hidden or with nothing in it, the island is empty`() {
        assertTrue(IslandStage.scene(emptyList(), null, null, hidden = false).isEmpty)
        assertTrue(IslandStage.scene(listOf(quiet), null, null, hidden = true).isEmpty)
    }

    @Test
    fun `music keeps its place between updates`() {
        val m = media.copy(positionMs = 10_000L, positionAt = 1_000L, durationMs = 60_000L)
        assertEquals(15_000L, m.positionNow(6_000L))
        assertEquals(10_000L, m.copy(playing = false).positionNow(6_000L))
        assertEquals(60_000L, m.positionNow(100_000L))
    }

    private companion object {
        val HOUR = TimeUnit.HOURS.toMillis(1)
        val MINUTE = TimeUnit.MINUTES.toMillis(1)
    }
}
