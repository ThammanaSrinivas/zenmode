package com.zenlauncher.zenmode.coreapi.services

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/**
 * Pro subscriptions kept on this device, with no store behind them — the whole Pro loop
 * (trial, renewal, cancel, resume, lapse) without Play Billing. No money ever moves, so
 * [isSimulated] is true and the app says so at checkout.
 *
 * Used by open-source builds (core-mock) and by core-private until Play Billing ships; the
 * billing provider replaces it behind [EntitlementProvider] with no app changes.
 *
 * The subscription lives in SharedPreferences, so it survives process death like a real
 * one would, and time-based transitions (trial converting, a cancelled plan lapsing) are
 * derived from the stored dates on every [refresh] rather than scheduled.
 */
class LocalEntitlementProvider(
    private val prefs: SharedPreferences,
    private val now: () -> Long = System::currentTimeMillis,
    /** Stand-in for the store's round trip, so the checkout's working state is real. */
    private val checkoutDelayMs: Long = CHECKOUT_DELAY_MS
) : EntitlementProvider {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    override val isAvailable: Boolean = true
    override val isSimulated: Boolean = true

    private val state = MutableStateFlow(read())
    override val entitlement: StateFlow<Entitlement> = state.asStateFlow()

    /** One store transaction at a time: a double tap can't start two subscriptions. */
    private val lock = Mutex()

    override suspend fun offers(): List<PlanOffer> = Offers

    override suspend fun refresh() {
        state.value = read()
    }

    override suspend fun purchase(activity: Activity, period: BillingPeriod): PurchaseResult = lock.withLock {
        refresh()
        if (state.value.isPro) return PurchaseResult.Failed("You already have Pro. Manage it from Settings.")
        val offer = Offers.firstOrNull { it.period == period }
            ?: return PurchaseResult.Failed("That plan isn't available right now.")
        delay(checkoutDelayMs)
        val today = startOfDay(now())
        val trialEndsOn = offer.freeTrialDays.takeIf { it > 0 }?.let { plusDays(today, it) }
        write(
            Entitlement(
                status = if (trialEndsOn != null) ProStatus.TRIAL else ProStatus.ACTIVE,
                period = period,
                since = today,
                trialEndsOn = trialEndsOn,
                // A trial's first charge lands the day it ends; otherwise one period on.
                renewsOn = trialEndsOn ?: plusPeriod(today, period)
            )
        )
        PurchaseResult.Success
    }

    override suspend fun cancel(activity: Activity): Boolean = lock.withLock {
        refresh()
        val current = state.value
        if (!current.isPro || current.status == ProStatus.ENDING) return false
        delay(checkoutDelayMs / 2)
        // Pro runs to the end of what was paid for: the trial, or the current period.
        // trialEndsOn is kept so a resume can pick the trial back up.
        write(current.copy(status = ProStatus.ENDING, endsOn = current.renewsOn, renewsOn = null))
        true
    }

    override suspend fun resume(activity: Activity): Boolean = lock.withLock {
        refresh()
        val current = state.value
        if (current.status != ProStatus.ENDING) return false
        delay(checkoutDelayMs / 2)
        val inTrial = current.trialEndsOn?.let { it > startOfDay(now()) } == true
        write(
            current.copy(
                status = if (inTrial) ProStatus.TRIAL else ProStatus.ACTIVE,
                renewsOn = current.endsOn,
                endsOn = null
            )
        )
        true
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

    /** The stored subscription as the store would report it today. */
    private fun read(): Entitlement {
        val status = prefs.getString(KEY_STATUS, null)
            ?.let { runCatching { ProStatus.valueOf(it) }.getOrNull() }
            ?.takeIf { it != ProStatus.FREE }
            ?: return Entitlement.Free
        val period = prefs.getString(KEY_PERIOD, null)
            ?.let { runCatching { BillingPeriod.valueOf(it) }.getOrNull() }
            ?: return Entitlement.Free
        val stored = Entitlement(
            status = status,
            period = period,
            since = prefs.long(KEY_SINCE),
            trialEndsOn = prefs.long(KEY_TRIAL_ENDS),
            renewsOn = prefs.long(KEY_RENEWS),
            endsOn = prefs.long(KEY_ENDS)
        )
        return advance(stored, startOfDay(now()))
    }

    /** Rolls [e] forward through every renewal and lapse that has happened by [today]. */
    private fun advance(e: Entitlement, today: Long): Entitlement = when (e.status) {
        ProStatus.ENDING ->
            if (e.endsOn != null && e.endsOn <= today) Entitlement.Free else e
        ProStatus.TRIAL, ProStatus.ACTIVE -> {
            var renewsOn = e.renewsOn
            if (renewsOn == null || renewsOn > today) {
                e
            } else {
                // Charged on each renewal date that has passed; a trial converts on its first.
                while (renewsOn!! <= today) renewsOn = plusPeriod(renewsOn, e.period!!)
                e.copy(status = ProStatus.ACTIVE, trialEndsOn = null, renewsOn = renewsOn)
            }
        }
        ProStatus.FREE -> Entitlement.Free
    }

    private fun SharedPreferences.long(key: String): Long? = getLong(key, 0L).takeIf { it > 0 }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun plusDays(millis: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    private fun plusPeriod(millis: Long, period: BillingPeriod): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.MONTH, if (period == BillingPeriod.ANNUAL) 12 else 1)
    }.timeInMillis

    companion object {
        /** Kept from the core-mock provider this replaced, so existing mock subscriptions carry over. */
        const val PREFS_NAME = "zenmode_mock_entitlement"
        private const val CHECKOUT_DELAY_MS = 900L
        private const val KEY_STATUS = "status"
        private const val KEY_PERIOD = "period"
        private const val KEY_SINCE = "since"
        private const val KEY_TRIAL_ENDS = "trial_ends_on"
        private const val KEY_RENEWS = "renews_on"
        private const val KEY_ENDS = "ends_on"

        private val Offers = listOf(
            PlanOffer(BillingPeriod.ANNUAL, "₹699", "₹699", "₹58", freeTrialDays = 30, originalPrice = "₹999"),
            PlanOffer(BillingPeriod.MONTHLY, "₹208", "₹2,499", "₹208", freeTrialDays = 7)
        )
    }
}
