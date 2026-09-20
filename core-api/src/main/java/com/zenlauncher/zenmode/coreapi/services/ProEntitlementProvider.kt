package com.zenlauncher.zenmode.coreapi.services

import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the signed-in user has ZenMode PRO. PRO unlocks the weekly report history
 * and report downloads in Settings.
 *
 * The source of truth is server-side; implementations cache the last known value so
 * the answer is available offline and at cold start.
 */
interface ProEntitlementProvider {
    /** Last known entitlement; updated by [refresh]. */
    val isPro: StateFlow<Boolean>

    /** Re-reads the entitlement from the server. Safe to call often; never throws. */
    suspend fun refresh()
}
