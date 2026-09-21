package com.zenlauncher.zenmode

import android.app.Activity
import android.content.SharedPreferences
import com.zenlauncher.zenmode.coreapi.services.BillingPeriod
import com.zenlauncher.zenmode.coreapi.services.LocalEntitlementProvider
import com.zenlauncher.zenmode.coreapi.services.ProStatus
import com.zenlauncher.zenmode.coreapi.services.PurchaseResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import java.time.LocalDate
import java.time.ZoneId

class LocalEntitlementProviderTest {

    private val activity = mock<Activity>()
    private val prefs = InMemoryPrefs()
    private var today = LocalDate.of(2026, 9, 21)

    private fun millis(date: LocalDate): Long = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // Noon, so start-of-day rounding is exercised.
    private fun provider() = LocalEntitlementProvider(prefs, now = { millis(today) + 12 * 3_600_000L }, checkoutDelayMs = 0)

    @Test
    fun `starts free and is always simulated`() {
        val p = provider()
        assertFalse(p.entitlement.value.isPro)
        assertTrue(p.isAvailable)
        assertTrue(p.isSimulated)
    }

    @Test
    fun `annual purchase starts the 30 day trial and first renewal lands on its last day`() = runTest {
        val p = provider()
        assertEquals(PurchaseResult.Success, p.purchase(activity, BillingPeriod.ANNUAL))
        val e = p.entitlement.value
        assertEquals(ProStatus.TRIAL, e.status)
        assertEquals(millis(today), e.since)
        assertEquals(millis(today.plusDays(30)), e.trialEndsOn)
        assertEquals(e.trialEndsOn, e.renewsOn)
    }

    @Test
    fun `monthly purchase honours its own 7 day trial`() = runTest {
        val p = provider()
        p.purchase(activity, BillingPeriod.MONTHLY)
        assertEquals(millis(today.plusDays(7)), p.entitlement.value.trialEndsOn)
    }

    @Test
    fun `a second purchase while pro fails instead of stacking`() = runTest {
        val p = provider()
        p.purchase(activity, BillingPeriod.ANNUAL)
        assertTrue(p.purchase(activity, BillingPeriod.MONTHLY) is PurchaseResult.Failed)
        assertEquals(BillingPeriod.ANNUAL, p.entitlement.value.period)
    }

    @Test
    fun `trial converts to active and renews a year after it ends`() = runTest {
        provider().purchase(activity, BillingPeriod.ANNUAL)
        today = today.plusDays(31)
        val e = provider().entitlement.value
        assertEquals(ProStatus.ACTIVE, e.status)
        assertNull(e.trialEndsOn)
        assertEquals(millis(LocalDate.of(2026, 9, 21).plusDays(30).plusYears(1)), e.renewsOn)
    }

    @Test
    fun `monthly keeps renewing through every missed month`() = runTest {
        provider().purchase(activity, BillingPeriod.MONTHLY)
        val trialEnd = LocalDate.of(2026, 9, 28)
        today = trialEnd.plusMonths(2).plusDays(3)
        assertEquals(millis(trialEnd.plusMonths(3)), provider().entitlement.value.renewsOn)
    }

    @Test
    fun `cancel keeps pro until the paid period ends then drops to free`() = runTest {
        val p = provider()
        p.purchase(activity, BillingPeriod.ANNUAL)
        assertTrue(p.cancel(activity))
        val e = p.entitlement.value
        assertEquals(ProStatus.ENDING, e.status)
        assertEquals(millis(today.plusDays(30)), e.endsOn)
        assertNull(e.renewsOn)
        assertFalse(p.cancel(activity))

        today = today.plusDays(29)
        assertTrue(provider().entitlement.value.isPro)
        today = today.plusDays(1)
        assertFalse(provider().entitlement.value.isPro)
    }

    @Test
    fun `resume inside the trial picks the trial back up`() = runTest {
        val p = provider()
        p.purchase(activity, BillingPeriod.ANNUAL)
        p.cancel(activity)
        assertTrue(p.resume(activity))
        val e = p.entitlement.value
        assertEquals(ProStatus.TRIAL, e.status)
        assertEquals(e.trialEndsOn, e.renewsOn)
        assertNull(e.endsOn)
    }

    @Test
    fun `refresh picks up a lapse without a restart`() = runTest {
        val p = provider()
        p.purchase(activity, BillingPeriod.MONTHLY)
        p.cancel(activity)
        today = today.plusDays(8)
        assertTrue(p.entitlement.value.isPro)
        p.refresh()
        assertFalse(p.entitlement.value.isPro)
    }

    @Test
    fun `corrupt storage reads as free`() {
        prefs.edit().putString("status", "GOLD").apply()
        assertFalse(provider().entitlement.value.isPro)
        prefs.edit().putString("status", "ACTIVE").putString("period", null).apply()
        assertFalse(provider().entitlement.value.isPro)
    }

    /** Enough of SharedPreferences for the provider: strings and longs, applied immediately. */
    private class InMemoryPrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values
        override fun getString(key: String, defValue: String?): String? = values[key] as String? ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int): Int = values[key] as Int? ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key] as Long? ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = values[key] as Float? ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as Boolean? ?: defValue
        override fun contains(key: String): Boolean = key in values
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(key: String, value: String?) = apply { values[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?) = this
            override fun putInt(key: String, value: Int) = apply { values[key] = value }
            override fun putLong(key: String, value: Long) = apply { values[key] = value }
            override fun putFloat(key: String, value: Float) = apply { values[key] = value }
            override fun putBoolean(key: String, value: Boolean) = apply { values[key] = value }
            override fun remove(key: String) = apply { values.remove(key) }
            override fun clear() = apply { values.clear() }
            override fun commit(): Boolean = true
            override fun apply() {}
        }
    }
}
