package com.zenlauncher.zenmode.coreapi.services

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Central service registry for the Open Core architecture.
 *
 * [WHAT] The only surface app code uses to reach auth, analytics, Firestore, or remote config.
 * [WHY] Keeps app/ ignorant of whether core-mock or core-private backs it, so neither Firebase
 * nor PostHog SDK types ever need to be imported outside those two modules.
 * [HOW] Populated once at startup by whichever [AppInitializer] the ServiceLoader SPI discovers;
 * consumers must check [isInitialized] before reading the lateinit properties.
 * [WHERE] Sits between app/ (ViewModels, Activities, Services) and the AppInitializer
 * implementations in core-mock/core-private.
 */
object ServiceLocator {
    lateinit var analyticsManager: com.zenlauncher.zenmode.coreapi.analytics.AnalyticsManager
    lateinit var analyticsTracker: AnalyticsTrackerContract
    lateinit var authProvider: AuthProvider
    lateinit var firestoreDataSource: FirestoreDataSource
    lateinit var remoteConfigProvider: RemoteConfigProvider
    lateinit var proEntitlementProvider: ProEntitlementProvider

    /**
     * Not lateinit on purpose: backends that don't sell Pro yet (core-private today) keep
     * compiling and every user is simply Free. Not part of [isInitialized] for the same reason.
     */
    var entitlementProvider: EntitlementProvider = FreeEntitlementProvider

    /** Emitted when FCM delivers a buddy-reaction push while app is running. */
    val buddyReactedEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 4)

    /** Emitted when FCM delivers a circle-reaction push while app is running. */
    val circleReactedEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 4)

    /**
     * Returns true if the ServiceLocator has been fully initialized.
     */
    val isInitialized: Boolean
        get() = ::analyticsManager.isInitialized &&
                ::analyticsTracker.isInitialized &&
                ::authProvider.isInitialized &&
                ::firestoreDataSource.isInitialized &&
                ::remoteConfigProvider.isInitialized &&
                ::proEntitlementProvider.isInitialized
}
