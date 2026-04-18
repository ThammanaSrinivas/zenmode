package com.zenlauncher.zenmode.coreapi.services

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Central service registry for the Open Core architecture.
 *
 * Populated at app startup by the private module's [AppInitializer].
 * Consumers access services through this singleton rather than
 * instantiating backend-specific classes directly.
 */
object ServiceLocator {
    lateinit var analyticsManager: com.zenlauncher.zenmode.coreapi.analytics.AnalyticsManager
    lateinit var analyticsTracker: AnalyticsTrackerContract
    lateinit var authProvider: AuthProvider
    lateinit var firestoreDataSource: FirestoreDataSource
    lateinit var remoteConfigProvider: RemoteConfigProvider

    /** Emitted when FCM delivers a buddy-reaction push while app is running. */
    val buddyReactedEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 4)

    /**
     * Returns true if the ServiceLocator has been fully initialized.
     */
    val isInitialized: Boolean
        get() = ::analyticsManager.isInitialized &&
                ::analyticsTracker.isInitialized &&
                ::authProvider.isInitialized &&
                ::firestoreDataSource.isInitialized &&
                ::remoteConfigProvider.isInitialized
}
