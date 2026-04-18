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
    }
}

class MockRemoteConfigProvider : RemoteConfigProvider {
    override val minVersionCode: StateFlow<Long> = MutableStateFlow(0L)
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
}

class MockAnalyticsTracker : AnalyticsTrackerContract {
    override fun trackAppFirstOpen(source: String, device: String) {}
    override fun trackOnboardingStarted() {}
    override fun trackPermissionScreenViewed(permissionType: String) {}
    override fun trackPermissionGranted(permissionType: String) {}
    override fun trackSetupCompleted(timeTakenSec: Int, permissionsGrantedCount: Int) {}
    override fun trackDoomScrollThresholdReached(appName: String) {}
    override fun trackOverlayDismissed(type: String) {}
    override fun trackRememberMeSelected(duration: String) {}
    override fun trackOverlayActionTaken(action: String) {}
    override fun trackBuddyShareStarted(mode: String) {}
    override fun trackBuddyCodeCopied(mode: String) {}
    override fun trackBuddyCodePasted(mode: String) {}
    override fun trackBuddyConnected(mode: String) {}
}

class MockAnalyticsManager : AnalyticsManager {
    override fun trackEvent(eventName: String, properties: Map<String, Any>?) {}
    override fun identifyUser(userId: String, properties: Map<String, Any>?) {}
}
