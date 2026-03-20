package com.zenlauncher.zenmode.coreapi.services

/**
 * Contract for analytics event tracking.
 * Mirrors the public API of the old AnalyticsTracker class.
 */
interface AnalyticsTrackerContract {
    fun trackAppInstalled()
    fun trackAppOpened()
    fun trackOnboardingStarted()
    fun trackOnboardingCompleted()
    fun trackPermissionsGranted(permissionType: String)
    fun trackDailySummaryViewed(dayStreak: Int, totalScreenTimeMin: Int, mindfulUnlockRate: Float)
    fun trackLauncherSetAsDefault(isDefault: Boolean)
    fun trackScreenUnlockStarted(trigger: String)
    fun trackMindfulUnlockCompleted(pauseDurationSec: Int)
    fun trackMindfulUnlockSkipped(timeWaitedSec: Long, skipMethod: String)
    fun trackMindfulUnlockDismissed(timeWaitedSec: Long)
    fun trackBuddyCodeGenerated(shareChannel: String)
    fun trackBuddyLinkAccepted(role: String)
    fun trackBuddyConnectionActive(buddyCount: Int)
    fun trackMindlessScrollDetected(durationSec: Long, appName: String, appCategory: String, nudgeShown: Boolean)
    fun trackMindfulScrollPromptShown(trigger: String)
    fun trackMindfulScrollPromptResponse(response: String, appCategory: String)
}
