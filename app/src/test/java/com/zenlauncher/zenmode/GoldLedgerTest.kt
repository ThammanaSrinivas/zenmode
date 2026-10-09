package com.zenlauncher.zenmode

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GoldLedgerTest {

    private val monday = LocalDate.of(2026, 10, 5)

    @Test
    fun `an order in a new week adds to the balance`() {
        val orders = GoldLedger.withOrder(mapOf(monday to 37_146L), monday.plusWeeks(1), 12_382L)
        assertEquals(49_528L, orders.values.sum())
    }

    @Test
    fun `a second order in the same week replaces the first, so a retry can't count twice`() {
        val orders = GoldLedger.withOrder(mapOf(monday to 37_146L), monday, 12_382L)
        assertEquals(mapOf(monday to 12_382L), orders)
    }

    @Test
    fun `stored orders round-trip, and anything unreadable is skipped`() {
        val orders = mapOf(monday to 37_146L, monday.plusWeeks(1) to 12_382L)
        assertEquals(orders, GoldLedger.parse(GoldLedger.format(orders)))
        assertEquals(mapOf(monday to 37_146L), GoldLedger.parse("2026-10-05=37146;garbage;2026-13-99=5;x=y"))
        assertEquals(emptyMap<LocalDate, Long>(), GoldLedger.parse(null))
    }

    @Test
    fun `the balance prints as whole rupees with Indian grouping`() {
        assertEquals("0", GoldLedger.rupeesLabel(0))
        assertEquals("371", GoldLedger.rupeesLabel(37_146))
        assertEquals("1,23,457", GoldLedger.rupeesLabel(12_345_678))
    }
}
