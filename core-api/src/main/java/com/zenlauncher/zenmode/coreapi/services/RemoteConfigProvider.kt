package com.zenlauncher.zenmode.coreapi.services

import kotlinx.coroutines.flow.StateFlow

/**
 * Service to fetch and provide remote configuration data.
 */
interface RemoteConfigProvider {
    /**
     * A real-time stream representing the minimum version code required.
     */
    val minVersionCode: StateFlow<Long>

    /**
     * WOWO (Wire On / Wire Off) switch for real Google Play Billing: true routes
     * [ServiceLocator.entitlementProvider] to real money via `PlayBillingEntitlementProvider`
     * (core-private); false (the default) keeps it on [LocalEntitlementProvider]'s simulated
     * checkout. Lets a production rollout be gated -- or an emergency reverted -- from the
     * Firebase console alone, no app release needed either way. See
     * [SwitchableEntitlementProvider], which reacts to this flow live (no restart required).
     */
    val playBillingEnabled: StateFlow<Boolean>

    /**
     * Initializes the provider (fetches data and starts listeners if applicable).
     */
    suspend fun initialize()
}
