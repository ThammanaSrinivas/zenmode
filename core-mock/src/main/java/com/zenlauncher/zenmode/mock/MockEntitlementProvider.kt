package com.zenlauncher.zenmode.mock

import android.app.Activity
import android.content.Context
import com.zenlauncher.zenmode.coreapi.services.BillingPeriod
import com.zenlauncher.zenmode.coreapi.services.Entitlement
import com.zenlauncher.zenmode.coreapi.services.EntitlementProvider
import com.zenlauncher.zenmode.coreapi.services.PlanOffer
import com.zenlauncher.zenmode.coreapi.services.ProStatus
import com.zenlauncher.zenmode.coreapi.services.PurchaseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Local stand-in for Play Billing so open-source builds can walk the whole Pro flow:
 * purchase (with the annual free month), cancel, resume. Persisted in SharedPreferences so
 * the state survives process death like a real subscription would.
 */
class MockEntitlementProvider(context: Context) : EntitlementProvider {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override val isAvailable: Boolean = true

    private val state = MutableStateFlow(read())
    override val entitlement: StateFlow<Entitlement> = state.asStateFlow()

    override suspend fun offers(): List<PlanOffer> = listOf(
        PlanOffer(BillingPeriod.ANNUAL, "₹699", "₹699", "₹58", freeTrialDays = 30),
        PlanOffer(BillingPeriod.MONTHLY, "₹75", "₹899", "₹75", freeTrialDays = 0)
    )

    override suspend fun purchase(activity: Activity, period: BillingPeriod): PurchaseResult {
        val today = startOfToday()
        // Annual: the free month ends and the first charge lands together. Monthly: next bill.
        val next = plusMonths(today, 1)
        write(
            Entitlement(
                status = if (period == BillingPeriod.ANNUAL) ProStatus.TRIAL else ProStatus.ACTIVE,
                period = period,
                since = today,
                trialEndsOn = if (period == BillingPeriod.ANNUAL) next else null,
                renewsOn = next
            )
        )
        return PurchaseResult.Success
    }

    override suspend fun cancel(activity: Activity): Boolean {
        val current = state.value
        if (!current.isPro || current.status == ProStatus.ENDING) return false
        // Pro runs to the end of what was paid for: the free month, or the current period.
        val endsOn = if (current.status == ProStatus.TRIAL) current.trialEndsOn else current.renewsOn
        write(current.copy(status = ProStatus.ENDING, endsOn = endsOn, renewsOn = null))
        return true
    }

    override suspend fun resume(activity: Activity): Boolean {
        val current = state.value
        if (current.status != ProStatus.ENDING) return false
        write(current.copy(status = ProStatus.ACTIVE, renewsOn = current.endsOn, endsOn = null))
        return true
    }

    private fun write(value: Entitlement) {
        prefs.edit()
            .putString(KEY_STATUS, value.status.name)
            .putString(KEY_PERIOD, value.period?.name)
            .putLong(KEY_SINCE, value.since ?: 0L)
            .putLong(KEY_TRIAL_ENDS, value.trialEndsOn ?: 0L)
            .putLong(KEY_RENEWS, value.renewsOn ?: 0L)
            .putLong(KEY_ENDS, value.endsOn ?: 0L)
            .apply()
        state.value = value
    }

    private fun read(): Entitlement {
        val status = prefs.getString(KEY_STATUS, null)
            ?.let { runCatching { ProStatus.valueOf(it) }.getOrNull() }
            ?: return Entitlement.Free
        val endsOn = prefs.getLong(KEY_ENDS, 0L).takeIf { it > 0 }
        // An ended subscription reads back as Free, exactly like Play would report it.
        if (status == ProStatus.ENDING && endsOn != null && endsOn <= startOfToday()) {
            return Entitlement.Free
        }
        val trialEndsOn = prefs.getLong(KEY_TRIAL_ENDS, 0L).takeIf { it > 0 }
        if (status == ProStatus.TRIAL && trialEndsOn != null && trialEndsOn <= startOfToday()) {
            // The free month converted: first annual charge taken, next renewal a year on.
            return Entitlement(
                status = ProStatus.ACTIVE,
                period = BillingPeriod.ANNUAL,
                since = prefs.getLong(KEY_SINCE, 0L).takeIf { it > 0 },
                renewsOn = plusMonths(trialEndsOn, 12)
            )
        }
        return Entitlement(
            status = status,
            period = prefs.getString(KEY_PERIOD, null)
                ?.let { runCatching { BillingPeriod.valueOf(it) }.getOrNull() },
            since = prefs.getLong(KEY_SINCE, 0L).takeIf { it > 0 },
            trialEndsOn = trialEndsOn,
            renewsOn = prefs.getLong(KEY_RENEWS, 0L).takeIf { it > 0 },
            endsOn = endsOn
        )
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun plusMonths(millis: Long, months: Int): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.MONTH, months)
    }.timeInMillis

    private companion object {
        const val PREFS_NAME = "zenmode_mock_entitlement"
        const val KEY_STATUS = "status"
        const val KEY_PERIOD = "period"
        const val KEY_SINCE = "since"
        const val KEY_TRIAL_ENDS = "trial_ends_on"
        const val KEY_RENEWS = "renews_on"
        const val KEY_ENDS = "ends_on"
    }
}
