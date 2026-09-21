package com.zenlauncher.zenmode.ui.screens

import com.zenlauncher.zenmode.coreapi.services.BillingPeriod
import com.zenlauncher.zenmode.coreapi.services.Entitlement
import com.zenlauncher.zenmode.coreapi.services.PlanOffer
import com.zenlauncher.zenmode.coreapi.services.ProFeature
import com.zenlauncher.zenmode.coreapi.services.ProStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** The sales rules live in copy, so the copy is what gets tested. */
class ZenProCopyTest {

    private val offers = listOf(
        PlanOffer(BillingPeriod.ANNUAL, "₹699", "₹699", "₹58", freeTrialDays = 30),
        PlanOffer(BillingPeriod.MONTHLY, "₹75", "₹899", "₹75", freeTrialDays = 0)
    )

    private fun date(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `price summary puts both plans in one line`() {
        assertEquals("₹699/year, or ₹75/month", offers.priceSummary())
    }

    @Test
    fun `price summary is null until the store answers`() {
        assertNull(emptyList<PlanOffer>().priceSummary())
    }

    @Test
    fun `price summary never invents a monthly price`() {
        assertEquals("₹699/year", offers.take(1).priceSummary())
    }

    @Test
    fun `free has no feature and pro has all of them`() {
        ProFeature.entries.forEach { feature ->
            assertFalse(Entitlement.Free.has(feature))
            assertTrue(Entitlement(status = ProStatus.ENDING).has(feature))
        }
    }

    @Test
    fun `trial line names the first charge before it happens`() {
        val e = Entitlement(status = ProStatus.TRIAL, period = BillingPeriod.ANNUAL, trialEndsOn = date(2026, 10, 17))
        assertEquals("Free trial ends 17 Oct 2026, then ₹699/year.", e.statusLine(offers))
    }

    @Test
    fun `active monthly line shows renewal and price`() {
        val e = Entitlement(status = ProStatus.ACTIVE, period = BillingPeriod.MONTHLY, renewsOn = date(2026, 10, 17))
        assertEquals("Renews 17 Oct 2026 · ₹75/month.", e.statusLine(offers))
    }

    @Test
    fun `ending line says when pro stops and that it will not renew`() {
        val e = Entitlement(status = ProStatus.ENDING, period = BillingPeriod.ANNUAL, endsOn = date(2027, 10, 17))
        assertEquals("Pro until 17 Oct 2027. It won't renew.", e.statusLine(offers))
    }

    @Test
    fun `early access never quotes an upcoming charge`() {
        val trial = Entitlement(status = ProStatus.TRIAL, period = BillingPeriod.ANNUAL, trialEndsOn = date(2026, 10, 17))
        assertEquals("Free trial ends 17 Oct 2026.", trial.statusLine(offers, isSimulated = true))
        val active = Entitlement(status = ProStatus.ACTIVE, period = BillingPeriod.MONTHLY, renewsOn = date(2026, 10, 17))
        assertEquals("Early access. Nothing is charged.", active.statusLine(offers, isSimulated = true))
    }

    @Test
    fun `server granted pro still gets a status line`() {
        assertEquals("Early access, nothing to manage.", Entitlement.Free.statusLine(offers))
    }
}
