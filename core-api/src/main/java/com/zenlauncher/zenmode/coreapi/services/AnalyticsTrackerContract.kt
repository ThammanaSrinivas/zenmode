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
    fun trackBuddyShareStarted(mode: String)
    fun trackBuddyCodeCopied(mode: String)
    fun trackBuddyCodePasted(mode: String)
    fun trackBuddyConnected(mode: String)
    fun trackDailyScreenTime(minutes: Long)
    fun trackWeeklyScreenTime(minutes: Long)
    fun trackCircleCreated()
    fun trackCircleJoined(via: String)
    fun trackCircleMemberRemoved(byLeader: Boolean)
    fun trackCircleLeft()
    fun trackBuddyToCircleSwitch()
    fun trackCircleLeadershipTransferred(reason: String)
    fun trackCircleReactionSent(type: String)
}
