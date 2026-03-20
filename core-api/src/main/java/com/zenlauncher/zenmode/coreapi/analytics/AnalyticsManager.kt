package com.zenlauncher.zenmode.coreapi.analytics

/**
 * Interface for analytics event tracking and user identification.
 * The implementation lives in the core-private module.
 */
interface AnalyticsManager {
    fun trackEvent(eventName: String, properties: Map<String, Any>? = null)
    fun identifyUser(userId: String)
    fun reset()
}
