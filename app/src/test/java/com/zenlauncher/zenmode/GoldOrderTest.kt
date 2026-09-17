package com.zenlauncher.zenmode

import org.junit.Assert.assertEquals
import org.junit.Test

class GoldOrderTest {

    @Test
    fun `clamps quantity to the whole-unit weekly range`() {
        assertEquals(AppConstants.INVEST_GOLD_MIN_UNITS, GoldOrder.clampUnits(0))
        assertEquals(4, GoldOrder.clampUnits(4))
        assertEquals(AppConstants.INVEST_GOLD_MAX_UNITS_PER_WEEK, GoldOrder.clampUnits(99))
    }

    @Test
    fun `total is units times unit price, in paise, never above the cap`() {
        assertEquals(99_056L, GoldOrder.totalPaise(8, 12_382L))
        assertEquals(GoldOrder.weeklyCapPaise(12_382L), GoldOrder.totalPaise(50, 12_382L))
    }

    @Test
    fun `weekly cap is max units times unit price`() {
        assertEquals(AppConstants.INVEST_GOLD_MAX_UNITS_PER_WEEK * 12_382L, GoldOrder.weeklyCapPaise(12_382L))
    }

    @Test
    fun `formats rupees with Indian grouping and two decimals`() {
        assertEquals("₹0.05", GoldOrder.formatInr(5))
        assertEquals("₹123.82", GoldOrder.formatInr(12_382))
        assertEquals("₹990.56", GoldOrder.formatInr(99_056))
        assertEquals("₹1,238.20", GoldOrder.formatInr(123_820))
        assertEquals("₹12,34,567.00", GoldOrder.formatInr(123_456_700))
        assertEquals("₹1,00,00,000.10", GoldOrder.formatInr(1_000_000_010))
        assertEquals("-₹1,251.90", GoldOrder.formatInr(-125_190))
    }
}
