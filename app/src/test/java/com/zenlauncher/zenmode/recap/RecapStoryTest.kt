package com.zenlauncher.zenmode.recap

import com.zenlauncher.zenmode.coreapi.ForegroundSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class RecapStoryTest {

    private val monday = LocalDate.of(2026, 9, 7)

    private fun week(
        minutes: List<Long>,
        promiseHours: Int = 4,
        previous: Long? = null,
        apps: List<AppMinutes> = emptyList(),
        lateNight: Long = 0
    ) = WeeklyRecap(
        weekStart = monday,
        days = minutes.mapIndexed { i, m ->
            DayRecord(monday.plusDays(i.toLong()), m, promiseHours, if (i == 0) apps else emptyList(), if (i == 0) lateNight else 0, 30)
        },
        previousWeekTotalMinutes = previous
    )

    @Test
    fun `five of seven kept days is a kept week that ends on Invest`() {
        val recap = week(listOf(100, 120, 200, 230, 300, 310, 90)) // 5 under 240
        assertEquals(RecapOutcome.KEPT, recap.outcome)
        val cards = RecapStory.cards(recap)
        assertEquals(5, cards.size)
        assertEquals(listOf("opener", "total", "promise", "highlight", "invest"), cards.map { it.key })
    }

    @Test
    fun `four of seven is a missed week that ends on recommit`() {
        val recap = week(listOf(100, 120, 200, 230, 300, 310, 400))
        assertEquals(RecapOutcome.MISSED, recap.outcome)
        val cards = RecapStory.cards(recap)
        assertEquals(listOf("opener", "total", "promise", "obstacle", "recommit"), cards.map { it.key })
        val promise = cards[2] as RecapCard.Promise
        assertTrue(promise.caption.startsWith("One day short"))
    }

    @Test
    fun `missed days report how far over the promise they ran`() {
        val recap = week(listOf(100, 120, 200, 230, 300, 310, 400))
        assertEquals((300 - 240) + (310 - 240) + (400 - 240).toLong(), recap.minutesOverPromise)
    }

    @Test
    fun `suggested promise is the smallest that would have kept five days`() {
        val recap = week(listOf(100, 120, 200, 230, 300, 310, 400))
        // 5th smallest day is 300 min -> 5 hours.
        assertEquals(5, RecapStory.suggestedPromiseHours(recap))
        val recommit = RecapStory.cards(recap).last() as RecapCard.Recommit
        assertEquals(5, recommit.suggestedPromiseHours)
    }

    @Test
    fun `late-night heavy weeks get the bedroom tip`() {
        val recap = week(
            listOf(300, 300, 300, 300, 300, 300, 300),
            apps = listOf(AppMinutes("com.instagram.android", "Instagram", 400)),
            lateNight = 700
        )
        val obstacle = RecapStory.cards(recap)[3] as RecapCard.Obstacle
        assertEquals("Instagram", obstacle.culpritLabel)
        assertTrue(obstacle.tip.contains("outside the bedroom"))
    }

    @Test
    fun `one dominant app gets the blocker tip`() {
        val recap = week(
            listOf(300, 300, 300, 300, 300, 300, 300),
            apps = listOf(AppMinutes("com.google.android.youtube", "YouTube", 900))
        )
        val obstacle = RecapStory.cards(recap)[3] as RecapCard.Obstacle
        assertTrue(obstacle.tip.contains("YouTube"))
    }

    @Test
    fun `kept week with real time won back leads with it`() {
        val recap = week(listOf(100, 100, 100, 100, 100, 100, 100), previous = 1000)
        val highlight = RecapStory.cards(recap)[3] as RecapCard.Highlight
        assertEquals("Time you won back", highlight.headline)
        assertEquals("5h 00m", highlight.value)
    }

    @Test
    fun `first week has no comparison`() {
        val recap = week(listOf(100, 100, 100, 100, 100, 100, 100))
        val total = RecapStory.cards(recap)[1] as RecapCard.Total
        assertNull(total.changeVsLastWeekMinutes)
    }

    @Test
    fun `weeks start on Monday and ranges read naturally`() {
        assertEquals(monday, weekStartOf(LocalDate.of(2026, 9, 13)))
        assertEquals(monday, weekStartOf(monday))
        assertEquals("Sep 7 – 13", week(List(7) { 0L }).rangeLabel(java.util.Locale.US))
    }

    @Test
    fun `minutes format`() {
        assertEquals("45m", formatMinutes(45))
        assertEquals("2h 05m", formatMinutes(125))
    }

    @Test
    fun `announcement waits for Monday morning`() {
        assertEquals(false, WeeklyRecapWorker.isAnnounceTime(LocalDateTime.of(2026, 9, 14, 7, 59)))
        assertEquals(true, WeeklyRecapWorker.isAnnounceTime(LocalDateTime.of(2026, 9, 14, 8, 0)))
        assertEquals(true, WeeklyRecapWorker.isAnnounceTime(LocalDateTime.of(2026, 9, 15, 1, 0)))
    }

    @Test
    fun `day aggregation splits apps and counts only the late-night window`() {
        val hour = TimeUnit.HOURS.toMillis(1)
        val start = 0L
        val end = 24 * hour
        val sessions = listOf(
            ForegroundSession("app.a", 21 * hour, 23 * hour),       // 1h late
            ForegroundSession("app.b", 10 * hour, 11 * hour),       // 0 late
            ForegroundSession("launcher", 1 * hour, 2 * hour),      // excluded
            ForegroundSession("app.a", -1 * hour, 1 * hour)         // clipped to 1h, all late
        )
        val result = DayAggregator.aggregate(sessions, start, end, excluded = setOf("launcher"))
        assertEquals(listOf("app.a" to 3 * hour, "app.b" to hour), result.appMillis)
        assertEquals(2 * hour, result.lateNightMillis)
    }
}
