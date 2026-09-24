package com.zenlauncher.zenmode.mock

import android.app.Application
import android.content.Context
import android.util.Log
import com.zenlauncher.zenmode.coreapi.analytics.AnalyticsManager
import com.zenlauncher.zenmode.coreapi.Circle
import com.zenlauncher.zenmode.coreapi.CircleJoinResult
import com.zenlauncher.zenmode.coreapi.ReactionType
import com.zenlauncher.zenmode.coreapi.SignInResult
import com.zenlauncher.zenmode.coreapi.User
import com.zenlauncher.zenmode.coreapi.UserStats
import com.zenlauncher.zenmode.coreapi.services.AppInitializer
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.coreapi.services.AuthProvider
import com.zenlauncher.zenmode.coreapi.services.FirestoreDataSource
import com.zenlauncher.zenmode.coreapi.services.AnalyticsTrackerContract
import com.zenlauncher.zenmode.coreapi.services.CrashReporter
import com.zenlauncher.zenmode.coreapi.services.LocalEntitlementProvider
import com.zenlauncher.zenmode.coreapi.services.ProEntitlementProvider

import com.zenlauncher.zenmode.coreapi.services.RemoteConfigProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockAppInitializer : AppInitializer {
    override fun initialize(application: Application) {
        Log.i("MockAppInitializer", "Initializing MOCK Core Services...")
        
        ServiceLocator.authProvider = MockAuthProvider()
        ServiceLocator.firestoreDataSource = MockFirestoreDataSource()
        ServiceLocator.analyticsTracker = MockAnalyticsTracker()
        ServiceLocator.analyticsManager = MockAnalyticsManager()
        ServiceLocator.remoteConfigProvider = MockRemoteConfigProvider()
        ServiceLocator.proEntitlementProvider = MockProEntitlementProvider()
        ServiceLocator.entitlementProvider = LocalEntitlementProvider(application)
        ServiceLocator.crashReporter = MockCrashReporter()
    }
}

/**
 * No server grants in open-source builds: Pro comes from [LocalEntitlementProvider]'s simulated
 * purchase (free and instant), so Settings' plan card and every Pro gate agree.
 */
class MockProEntitlementProvider : ProEntitlementProvider {
    override val isPro: StateFlow<Boolean> = MutableStateFlow(false)
    override suspend fun refresh() {}
}

class MockRemoteConfigProvider : RemoteConfigProvider {
    override val minVersionCode: StateFlow<Long> = MutableStateFlow(0L)
    // Deliberately NOT the usual "mock defaults to feature-ON" convention: turning this on
    // would try to route open-source builds through real Play Billing, which they structurally
    // can't do (no Play Console tie, no service account) -- LocalEntitlementProvider (simulated,
    // always available) is already core-mock's permanent answer for Pro, regardless of this flag.
    override val playBillingEnabled: StateFlow<Boolean> = MutableStateFlow(false)
    override suspend fun initialize() {
        Log.i("MockRemoteConfig", "MOCK Remote Config initialized. Defaulting to 0L.")
    }
}


class MockAuthProvider : AuthProvider {
    private var signedIn = false
    private val fakeUserId = "mock_user_123"

    override fun isSignedIn(): Boolean = signedIn
    override fun getCurrentUserId(): String? = if (signedIn) fakeUserId else null
    override fun getPhotoUrl(): String? = null
    override fun getEmail(): String? = null
    override fun getDisplayName(): String? = if (signedIn) "Mock User" else null

    override suspend fun signInWithGoogleToken(idToken: String): SignInResult {
        signedIn = true
        return SignInResult(
            userId = fakeUserId,
            displayName = "Mock User",
            isNewUser = false,
            isSuccess = true
        )
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): SignInResult {
        signedIn = true
        return SignInResult(
            userId = fakeUserId,
            displayName = "Mock User",
            isNewUser = false,
            isSuccess = true,
            email = email
        )
    }

    override fun signOut() {
        signedIn = false
    }

    override suspend fun deleteAccount() {
        signedIn = false
    }
}

class MockFirestoreDataSource : FirestoreDataSource {
    override suspend fun getBuddyUid(myUid: String): String? = null
    override suspend fun getBuddyStats(buddyUid: String): UserStats? = null
    override suspend fun getUser(uid: String): User? = User(uid, "Mock User")
    override suspend fun checkRelationshipExists(myUid: String, otherUid: String): Boolean = false
    override suspend fun sendBuddyInvite(myUid: String, targetUid: String) {}
    override suspend fun disconnectBuddy(myUid: String, buddyUid: String) {}
    override suspend fun findRandomBuddy(myUid: String): String? = null
    override suspend fun initializeUser(uid: String, displayName: String?) {}
    override suspend fun deleteUser(uid: String) {}
    override suspend fun getRelationshipCreatedAt(myUid: String): Long? = null
    override fun getRelationshipId(user1: String, user2: String): String =
        if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"
    override suspend fun sendLike(relationshipId: String, senderUid: String): Boolean = true
    override suspend fun getTodayLikes(relationshipId: String, myUid: String, buddyUid: String): Pair<Long, Long> = 0L to 0L
    override suspend fun saveFcmToken(uid: String, token: String) {}

    override suspend fun getMyCircleId(myUid: String): String? = null
    override suspend fun getCircle(circleId: String): Circle? = null
    override suspend fun createCircle(leaderUid: String, leaderDisplayName: String?, circleName: String): Circle? = null
    override suspend fun joinCircle(circleId: String, myUid: String, myDisplayName: String?, confirmedSwitchFromBuddy: Boolean): CircleJoinResult =
        CircleJoinResult.Success
    override suspend fun leaveCircle(circleId: String, myUid: String): Boolean = true
    override suspend fun removeCircleMember(circleId: String, leaderUid: String, targetUid: String): Boolean = true
    override suspend fun transferLeadership(circleId: String, currentLeaderUid: String, newLeaderUid: String): Boolean = true
    override suspend fun sendCircleReaction(circleId: String, fromUid: String, toUid: String, type: ReactionType): Boolean = true
    override suspend fun getTodayCircleReactions(circleId: String, myUid: String): Pair<Long, Long> = 0L to 0L
    override suspend fun findRandomCircleUser(myUid: String, myDisplayName: String?): Circle? = null
    // Open-source builds never run out — mock defaults to the feature-ON value, same convention
    // as MockRemoteConfigProvider.
    override suspend fun hasRandomConnectQuota(myUid: String, limit: Int): Boolean = true
    override suspend fun recordRandomConnectUsed(myUid: String) {}
}

class MockAnalyticsTracker : AnalyticsTrackerContract {
    override fun trackAppFirstOpen(source: String, device: String) {}
    override fun trackOnboardingStarted() {}
    override fun trackPermissionScreenViewed(permissionType: String) {}
    override fun trackPermissionGranted(permissionType: String) {}
    override fun trackSetupCompleted(timeTakenSec: Int, permissionsGrantedCount: Int) {}
    override fun trackBuddyShareStarted(mode: String) {}
    override fun trackBuddyCodeCopied(mode: String) {}
    override fun trackBuddyCodePasted(mode: String) {}
    override fun trackBuddyConnected(mode: String) {}
    override fun trackDailyScreenTime(minutes: Long) {}
    override fun trackWeeklyScreenTime(minutes: Long) {}
    override fun trackRecapReady(weekStart: String, outcome: String) {}
    override fun trackRecapOpened(weekStart: String, outcome: String, source: String) {}
    override fun trackRecapCardViewed(weekStart: String, outcome: String, card: String, position: Int) {}
    override fun trackRecapCompleted(weekStart: String, outcome: String) {}
    override fun trackRecapCtaClicked(weekStart: String, outcome: String, cta: String) {}
    override fun trackReportDownloaded(weekStart: String) {}
    override fun trackProUpsellViewed(surface: String) {}
    override fun trackCircleCreated() {}
    override fun trackCircleJoined(via: String) {}
    override fun trackCircleMemberRemoved(byLeader: Boolean) {}
    override fun trackCircleLeft() {}
    override fun trackBuddyToCircleSwitch() {}
    override fun trackCircleLeadershipTransferred(reason: String) {}
    override fun trackCircleReactionSent(type: String) {}

override fun trackScreentimePermissionGranted(permissionType: String) {}
    override fun trackScreentimePermissionDenied(permissionType: String) {}
    override fun trackDailyScreentimeRecorded(date: String, totalMinutes: Long, blockedMinutes: Long, unlockCount: Int, topApps: List<String>) {}
    override fun trackDailyScreentimeViewed(totalMinutes: Long, vsYesterdayDelta: Int) {}
    override fun trackWeeklyScreentimeRecorded(weekStartDate: String, totalMinutes: Long, avgDailyMinutes: Long, vsLastWeekDeltaPct: Int) {}
    override fun trackWeeklyScreentimeViewed(totalMinutes: Long, vsLastWeekDelta: Int) {}
    override fun trackLifetimeScreentimeUpdated(lifetimeTotalMinutes: Long, daysTracked: Long) {}
    override fun trackLifetimeMilestoneReached(milestoneValue: Int) {}
    override fun trackSessionStart(sessionNumber: Int) {}
    override fun trackDay1CheckinCompleted(actionType: String) {}
    override fun trackDay7MilestoneShown() {}
    override fun trackDay7MilestoneEngaged() {}
    override fun trackDay30MilestoneShown() {}
    override fun trackDay30MilestoneEngaged(cumulativeTimeSaved: Long) {}
    override fun trackReengagementPushSent(pushType: String, daysInactive: Long) {}
    override fun trackReengagementPushOpened(pushType: String) {}
    override fun trackGoldTabViewed(entryPoint: String) {}
    override fun trackGoldEarned(goldAmount: Int, triggerReason: String) {}
    override fun trackGoldPurchaseInitiated(amountLocalCurrency: Int, grams: Int) {}
    override fun trackGoldPurchaseCompleted(amount: Int, currency: String, grams: Int, paymentMethod: String) {}
    override fun trackGoldPurchaseFailed(reason: String) {}
    override fun trackGoldPortfolioViewed(portfolioValue: Int) {}
    override fun trackGoldRedeemed(amount: Int, redemptionType: String) {}
    override fun trackZencircleViewed() {}
    override fun trackZencircleJoinedV3(circleSize: Int, circleType: String) {}
    override fun trackZencircleCreatedV3(circleType: String) {}
    override fun trackZencircleInviteSent(channel: String) {}
    override fun trackZencirclePostCreated(postType: String) {}
    override fun trackZencircleReactionAdded(postType: String) {}
    override fun trackZencircleLeaderboardViewed(rank: Int) {}
    override fun trackZencircleChallengeJoined(challengeType: String, durationDays: Long) {}
    override fun trackZencircleChallengeCompleted(challengeType: String) {}
    override fun trackZencircleLeftV3(tenureDays: Long) {}
    override fun trackReferralShareInitiated(channel: String, shareContext: String) {}
    override fun trackReferralLinkShared(channel: String) {}
    override fun trackReferralSignupCompleted(referredByUserId: String) {}
    override fun trackReferralPointsEarned(amount: Int, reason: String) {}
    override fun trackPointsRedeemed(rewardType: String, amount: Int) {}
    override fun trackOnboardingStartedV3(acquisitionSource: String) {}
    override fun trackOnboardingStepViewed(stepName: String, stepNumber: Int) {}
    override fun trackOnboardingPermissionRequested(permissionType: String) {}
    override fun trackOnboardingPermissionGrantedV3(permissionType: String) {}
    override fun trackOnboardingPermissionDenied(permissionType: String) {}
    override fun trackOnboardingGoalSelected(goalType: String) {}
    override fun trackOnboardingCompleted(totalTimeSeconds: Long) {}
    override fun trackOnboardingAbandoned(lastStep: String) {}
    override fun trackHomeSearchOpened(entryPoint: String) {}
    override fun trackHomeSearchQuerySubmitted(queryLength: Int, resultCount: Int) {}
    override fun trackHomeSearchResultClicked(resultType: String, position: Int) {}
    override fun trackHomeSearchNoResults() {}
    override fun trackHomeSearchAbandoned() {}
    override fun trackBlockerEnabled(blockerMode: String) {}
    override fun trackBlockerDisabled(blockerMode: String) {}
    override fun trackBlockerSessionStarted(mode: String, plannedDuration: Long, blockedApps: List<String>) {}
    override fun trackBlockerSessionCompleted(actualDuration: Long) {}
    override fun trackBlockerSessionInterrupted(elapsedDuration: Long, reason: String) {}
    override fun trackBlockedAppAttempt(appName: String) {}
    override fun trackBlockerBypassUsed(justification: String) {}
    override fun trackBlockerStreakAchieved(streakDays: Long) {}
    override fun trackA11yPermissionGranted() {}
    override fun trackA11yPermissionLost(
        manufacturer: String,
        model: String,
        sdk: Int,
        serviceRunning: Boolean,
        hoursSinceGranted: Long
    ) {}
}

/** No crash backend in open-source builds: non-fatals go to logcat. */
class MockCrashReporter : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, keys: Map<String, String>) {
        Log.w("MockCrashReporter", "Non-fatal $keys", throwable)
    }
}

class MockAnalyticsManager : AnalyticsManager {
    override fun trackEvent(eventName: String, properties: Map<String, Any>?) {}
    override fun identifyUser(userId: String, properties: Map<String, Any>?) {}
    override fun reset() {}
}
