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

    // ── Weekly recap ("Zen Week") ──
    /** A week's recap was built and the user notified. [outcome] is "kept" or "missed". */
    fun trackRecapReady(weekStart: String, outcome: String)
    /** [source]: "notification", "home", "settings". */
    fun trackRecapOpened(weekStart: String, outcome: String, source: String)
    fun trackRecapCardViewed(weekStart: String, outcome: String, card: String, position: Int)
    fun trackRecapCompleted(weekStart: String, outcome: String)
    /** [cta]: "invest", "recommit", "reports". */
    fun trackRecapCtaClicked(weekStart: String, outcome: String, cta: String)
    fun trackReportDownloaded(weekStart: String)
    /** [surface]: where the PRO upsell was shown, e.g. "settings_reports". */
    fun trackProUpsellViewed(surface: String)
    fun trackCircleCreated()
    fun trackCircleJoined(via: String)
    fun trackCircleMemberRemoved(byLeader: Boolean)
    fun trackCircleLeft()
    fun trackBuddyToCircleSwitch()
    fun trackCircleLeadershipTransferred(reason: String)
    fun trackCircleReactionSent(type: String)
}
