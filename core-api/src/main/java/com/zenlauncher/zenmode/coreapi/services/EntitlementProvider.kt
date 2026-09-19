package com.zenlauncher.zenmode.coreapi.services

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ZenMode Pro entitlements.
 *
 * The whole loop — launcher, intent, declared sessions, Zen Score, daily report, one partner,
 * 7 days of history — is free forever. Pro only adds range. Anything that isn't listed in
 * [ProFeature] must never be gated.
 *
 * Real billing (Play Billing) lives in core-private; core-mock simulates it locally.
 */
interface EntitlementProvider {
    /**
     * False until a backend can actually sell Pro. When false the app shows no plan card,
     * no PRO labels and no gates — every user just sees the free app, unchanged.
     */
    val isAvailable: Boolean

    val entitlement: StateFlow<Entitlement>

    /** Localized offers from the store. Never hardcode prices in UI — read them from here. */
    suspend fun offers(): List<PlanOffer>

    suspend fun purchase(activity: Activity, period: BillingPeriod): PurchaseResult

    /** Stops renewal. Pro stays active until [Entitlement.endsOn]; nothing is deleted. */
    suspend fun cancel(activity: Activity): Boolean

    /** Undoes a pending cancellation before the period ends. */
    suspend fun resume(activity: Activity): Boolean
}

enum class BillingPeriod { ANNUAL, MONTHLY }

enum class ProFeature {
    FULL_HISTORY,
    PERIOD_REPORTS,
    EXTRA_PARTNERS,
    CUSTOM_SESSION_LENGTHS,
    HOME_THEMES,
    DATA_EXPORT,
    RANDOM_CONNECT,
    SUPPORTERS_LIST
}

enum class ProStatus {
    FREE,
    /** Annual plan inside its free first month. */
    TRIAL,
    ACTIVE,
    /** Cancelled, still Pro until [Entitlement.endsOn]. */
    ENDING
}

/** Dates are epoch millis at local midnight; null when not applicable. */
data class Entitlement(
    val status: ProStatus = ProStatus.FREE,
    val period: BillingPeriod? = null,
    val since: Long? = null,
    val trialEndsOn: Long? = null,
    val renewsOn: Long? = null,
    val endsOn: Long? = null
) {
    val isPro: Boolean get() = status != ProStatus.FREE

    fun has(feature: ProFeature): Boolean = isPro

    companion object {
        const val FREE_PARTNER_LIMIT = 1
        const val PRO_PARTNER_LIMIT = 3
        const val FREE_HISTORY_DAYS = 7

        val Free = Entitlement()
    }
}

data class PlanOffer(
    val period: BillingPeriod,
    /** Store-formatted price, e.g. "₹699". */
    val formattedPrice: String,
    /** Store-formatted total over a year, e.g. "₹899" for monthly. */
    val formattedYearTotal: String,
    /** Store-formatted per-month equivalent, e.g. "₹58". */
    val formattedPerMonth: String,
    val freeTrialDays: Int
)

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Failed(val message: String) : PurchaseResult
}

/**
 * Default until a backend registers a real provider: everyone is Free and nothing can be bought.
 * Keeps core-private compiling before it implements billing.
 */
object FreeEntitlementProvider : EntitlementProvider {
    private val state = MutableStateFlow(Entitlement.Free)
    override val isAvailable: Boolean = false
    override val entitlement: StateFlow<Entitlement> = state.asStateFlow()
    override suspend fun offers(): List<PlanOffer> = emptyList()
    override suspend fun purchase(activity: Activity, period: BillingPeriod): PurchaseResult =
        PurchaseResult.Failed("Purchases aren't available in this build.")
    override suspend fun cancel(activity: Activity): Boolean = false
    override suspend fun resume(activity: Activity): Boolean = false
}
