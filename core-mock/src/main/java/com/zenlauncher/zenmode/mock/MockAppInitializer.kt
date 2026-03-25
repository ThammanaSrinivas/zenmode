package com.zenlauncher.zenmode.mock

import android.app.Application
import android.content.Context
import android.util.Log
import com.zenlauncher.zenmode.coreapi.analytics.AnalyticsManager
import com.zenlauncher.zenmode.coreapi.SignInResult
import com.zenlauncher.zenmode.coreapi.User
import com.zenlauncher.zenmode.coreapi.UserStats
import com.zenlauncher.zenmode.coreapi.services.AppInitializer
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.coreapi.services.AuthProvider
import com.zenlauncher.zenmode.coreapi.services.FirestoreDataSource
import com.zenlauncher.zenmode.coreapi.services.AnalyticsTrackerContract

class MockAppInitializer : AppInitializer {
    override fun initialize(application: Application) {
        Log.i("MockAppInitializer", "Initializing MOCK Core Services...")
        
        ServiceLocator.authProvider = MockAuthProvider()
        ServiceLocator.firestoreDataSource = MockFirestoreDataSource()
        ServiceLocator.analyticsTracker = MockAnalyticsTracker()
        ServiceLocator.analyticsManager = MockAnalyticsManager()
    }
}

class MockAuthProvider : AuthProvider {
    private var signedIn = false
    private val fakeUserId = "mock_user_123"

    override fun isSignedIn(): Boolean = signedIn
    override fun getCurrentUserId(): String? = if (signedIn) fakeUserId else null
    override fun getPhotoUrl(): String? = null

    override suspend fun signInWithGoogleToken(idToken: String): SignInResult {
        signedIn = true
        return SignInResult(
            userId = fakeUserId,
            displayName = "Mock User",
            isNewUser = false,
            isSuccess = true
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
    override suspend fun initializeUser(uid: String, displayName: String?) {}
    override suspend fun deleteUser(uid: String) {}
}

class MockAnalyticsTracker : AnalyticsTrackerContract {
    override fun trackAppOpened() {}
    override fun trackAppInstalled() {}
    override fun trackSignInStatus(isSignedIn: Boolean) {}
    override fun trackScreenUnlock() {}
    override fun trackDelayStarted(delayDurationSec: Int) {}
    override fun trackDelayCompleted() {}
    override fun trackUnlockSuccessful(totalDelaySec: Int) {}
    override fun trackDailyStatsSynced(screenTimeMin: Int, unlockCount: Int) {}
    override fun trackServiceStatus(serviceName: String, isEnabled: Boolean) {}
    override fun trackPermissionsGranted(permissionName: String) {}
    override fun trackPermissionsDenied(permissionName: String) {}
    override fun trackWidgetAdded() {}
    override fun trackMindlessScrollDetected(durationSec: Long, appName: String, appCategory: String, nudgeShown: Boolean) {}
    override fun trackMindfulScrollPromptShown(promptType: String) {}
    override fun trackMindfulScrollPromptResponse(response: String, sessionCategory: String) {}
    override fun trackFeatureEngagement(featureName: String, actionName: String) {}
    override fun trackError(errorType: String, errorMessage: String, contextStr: String?) {}
    override fun trackBuddyInviteSent() {}
    override fun trackBuddyInviteAccepted() {}
    override fun trackBuddyStatsViewed() {}
    override fun trackDailySummaryViewed(dayStreak: Int, totalScreenTimeMin: Int, mindfulUnlockRate: Float) {}
    override fun trackGoalSet(goalType: String, targetValue: Int) {}
    override fun trackGoalAchieved(goalType: String) {}
}

class MockAnalyticsManager : AnalyticsManager {
    override fun trackEvent(eventName: String, properties: Map<String, Any>?) {}
    override fun identifyUser(userId: String, properties: Map<String, Any>?) {}
}
