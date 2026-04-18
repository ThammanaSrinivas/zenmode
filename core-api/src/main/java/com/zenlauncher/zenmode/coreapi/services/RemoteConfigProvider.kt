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
     * Initializes the provider (fetches data and starts listeners if applicable).
     */
    suspend fun initialize()
}
