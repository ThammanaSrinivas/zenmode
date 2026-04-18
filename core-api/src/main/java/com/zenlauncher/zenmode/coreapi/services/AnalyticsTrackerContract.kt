package com.zenlauncher.zenmode.coreapi.services

/**
 * Contract for analytics event tracking.
 * Mirrors the public API of the old AnalyticsTracker class.
 */
interface AnalyticsTrackerContract {
    fun trackAppFirstOpen(source: String, device: String)
    fun trackOnboardingStarted()
    fun trackPermissionScreenViewed(permissionType: String)
    fun trackPermissionGranted(permissionType: String)
    fun trackSetupCompleted(timeTakenSec: Int, permissionsGrantedCount: Int)
    fun trackDoomScrollThresholdReached(appName: String)
    fun trackOverlayDismissed(type: String)
    fun trackRememberMeSelected(duration: String)
    fun trackOverlayActionTaken(action: String)
    fun trackBuddyShareStarted(mode: String)
    fun trackBuddyCodeCopied(mode: String)
    fun trackBuddyCodePasted(mode: String)
    fun trackBuddyConnected(mode: String)
}
