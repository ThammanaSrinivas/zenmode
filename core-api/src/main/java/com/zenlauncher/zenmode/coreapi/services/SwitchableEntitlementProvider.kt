package com.zenlauncher.zenmode.coreapi.services

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * WOWO (Wire On / Wire Off) delegate between two [EntitlementProvider]s, picked live by
 * [enabledFlow] -- [real] when true, [simulated] when false. Built for exactly one case today:
 * core-private wiring real Google Play Billing behind a remote-config flag it can flip without
 * an app release, falling back to (or starting from) [LocalEntitlementProvider]'s simulated
 * checkout with zero risk. [ServiceLocator.entitlementProvider]'s reference is assigned once at
 * boot to an instance of this class, not reassigned when the flag flips -- [entitlement] is a
 * derived flow that reacts to [enabledFlow] itself, so already-collecting UI (e.g. Compose's
 * `collectAsState()`) updates live with no restart needed, unlike naively swapping which
 * concrete provider [ServiceLocator.entitlementProvider] points at.
 *
 * Every other member reads [enabledFlow]'s current value fresh on each call -- so a flag flip
 * takes effect on the very next [offers]/[purchase]/[refresh], not just for already-open UI.
 */
class SwitchableEntitlementProvider(
    private val real: EntitlementProvider,
    private val simulated: EntitlementProvider,
    private val enabledFlow: StateFlow<Boolean>,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EntitlementProvider {

    private fun active(): EntitlementProvider = if (enabledFlow.value) real else simulated

    override val isAvailable: Boolean get() = active().isAvailable
    override val isSimulated: Boolean get() = active().isSimulated

    override val entitlement: StateFlow<Entitlement> =
        combine(enabledFlow, real.entitlement, simulated.entitlement) { enabled, r, s ->
            if (enabled) r else s
        }.stateIn(
            scope,
            SharingStarted.Eagerly,
            if (enabledFlow.value) real.entitlement.value else simulated.entitlement.value
        )

    override suspend fun refresh() = active().refresh()
    override suspend fun offers(): List<PlanOffer> = active().offers()
    override suspend fun purchase(activity: Activity, period: BillingPeriod): PurchaseResult =
        active().purchase(activity, period)
    override suspend fun cancel(activity: Activity): Boolean = active().cancel(activity)
    override suspend fun resume(activity: Activity): Boolean = active().resume(activity)
}
