package com.zenlauncher.zenmode

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.User
import com.zenlauncher.zenmode.coreapi.services.AnalyticsTrackerContract
import com.zenlauncher.zenmode.coreapi.services.AuthProvider
import com.zenlauncher.zenmode.coreapi.services.Entitlement
import com.zenlauncher.zenmode.coreapi.services.FirestoreDataSource
import com.zenlauncher.zenmode.coreapi.services.FreeEntitlementProvider
import com.zenlauncher.zenmode.coreapi.services.ProEntitlementProvider
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.BuddyAddResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.stubbing.Answer
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * [BuddyConnector.addBuddy] and [BuddyConnector.randomConnect]. Toast is static, so it is
 * stubbed with Mockito's [mockStatic] (inline mock maker, the Mockito 5 default) — that lets the
 * toast paths run and lets each test check what the user is told.
 */
class BuddyConnectorTest {

    private lateinit var activity: Activity
    private lateinit var repository: UsageRepository
    private lateinit var firestoreDataSource: FirestoreDataSource
    private lateinit var authProvider: AuthProvider
    private lateinit var analyticsTracker: AnalyticsTrackerContract
    private lateinit var capabilities: NetworkCapabilities
    private lateinit var toastStatic: MockedStatic<Toast>
    private var buddyChangedCalls = 0

    private lateinit var connector: BuddyConnector

    @Before
    fun setup() {
        firestoreDataSource = mock()
        authProvider = mock()
        analyticsTracker = mock()
        whenever(authProvider.getCurrentUserId()).thenReturn("my-uid")

        ServiceLocator.firestoreDataSource = firestoreDataSource
        ServiceLocator.authProvider = authProvider
        ServiceLocator.analyticsManager = mock()
        ServiceLocator.analyticsTracker = analyticsTracker
        // randomConnect asks ProAccess.isPro(activity), which reads both entitlement sources.
        ServiceLocator.proEntitlementProvider = mock<ProEntitlementProvider>().also {
            whenever(it.isPro).thenReturn(MutableStateFlow(false))
        }
        ServiceLocator.entitlementProvider = FreeEntitlementProvider

        activity = mock()
        whenever(activity.getSharedPreferences(any(), any())).thenReturn(mock<SharedPreferences>())
        // android.net.Network can't be mocked on the JVM (it implements a hidden okhttp interface
        // missing from android.jar), so activeNetwork stays null and the capabilities lookup
        // is stubbed for that null.
        capabilities = mockNetworkCapabilities()
        val connectivityManager = mock<ConnectivityManager>()
        whenever(connectivityManager.getNetworkCapabilities(null)).thenReturn(capabilities)
        whenever(activity.getSystemService(ConnectivityManager::class.java)).thenReturn(connectivityManager)
        setOnline(true)

        toastStatic = mockStatic(Toast::class.java)
        toastStatic.`when`<Toast> { Toast.makeText(any<Context>(), any<CharSequence>(), any<Int>()) }
            .thenReturn(mock<Toast>())

        repository = mock()
        buddyChangedCalls = 0
        connector = BuddyConnector(activity, repository) { buddyChangedCalls++ }
    }

    @After
    fun tearDown() {
        if (::toastStatic.isInitialized) toastStatic.close()
    }

    /**
     * Paparazzi puts layoutlib's real framework on the unit-test classpath, and
     * NetworkCapabilities' static initializer reads Build via the native SystemProperties.get,
     * which isn't linked in a plain JVM test. Stubbing SystemProperties while the class
     * initializes lets Mockito mock it; afterwards the class stays initialized for the JVM.
     * String properties come back as "0", not "": Build indexes into split lists and parses
     * some of them as numbers.
     */
    private fun mockNetworkCapabilities(): NetworkCapabilities {
        val properties = Answer { call ->
            if (call.method.returnType == String::class.java) "0" else Answers.RETURNS_DEFAULTS.answer(call)
        }
        return mockStatic(Class.forName("android.os.SystemProperties"), properties).use { mock() }
    }

    private fun setOnline(online: Boolean) {
        whenever(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(online)
    }

    private fun verifyToast(message: String, length: Int = Toast.LENGTH_SHORT) {
        toastStatic.verify { Toast.makeText(activity, message, length) }
    }

    private fun verifyNoToast() {
        toastStatic.verify({ Toast.makeText(any<Context>(), any<CharSequence>(), any<Int>()) }, never())
    }

    /** TimeoutCancellationException has no public constructor; get a real one from withTimeout. */
    private suspend fun timeout(): TimeoutCancellationException =
        try {
            withTimeout(1) { awaitCancellation() }
        } catch (e: TimeoutCancellationException) {
            e
        }

    /** Lets RandomConnect.attempt through its cooldown and weekly-quota checks. */
    private suspend fun allowRandomConnect() {
        whenever(repository.getLastRandomConnectAttemptTime()).thenReturn(0L)
        whenever(firestoreDataSource.hasRandomConnectQuota(any(), any())).thenReturn(true)
    }

    // --- addBuddy ---

    @Test
    fun `addBuddy with your own code is SelfAdd and touches nothing`() = runTest {
        val result = connector.addBuddy("my-uid")

        assertEquals(BuddyAddResult.SelfAdd, result)
        verify(analyticsTracker, never()).trackBuddyCodePasted(any())
        verify(firestoreDataSource, never()).getUser(any())
    }

    @Test
    fun `addBuddy offline reports no internet without hitting Firestore`() = runTest {
        setOnline(false)

        val result = connector.addBuddy("bro-uid")

        assertEquals(
            BuddyAddResult.Error("No internet connection. Please check your network and try again."),
            result
        )
        verify(analyticsTracker).trackBuddyCodePasted("manual")
        verify(firestoreDataSource, never()).getUser(any())
    }

    @Test
    fun `addBuddy treats a missing ConnectivityManager as offline`() = runTest {
        whenever(activity.getSystemService(ConnectivityManager::class.java)).thenReturn(null)

        val result = connector.addBuddy("bro-uid")

        assertEquals(
            BuddyAddResult.Error("No internet connection. Please check your network and try again."),
            result
        )
    }

    @Test
    fun `addBuddy with an unknown code says the user wasn't found`() = runTest {
        whenever(firestoreDataSource.getUser("bro-uid")).thenReturn(null)

        val result = connector.addBuddy("bro-uid")

        assertEquals(BuddyAddResult.Error("User ID not found. Please check the ID and try again."), result)
        verify(firestoreDataSource, never()).sendBuddyInvite(any(), any())
    }

    @Test
    fun `addBuddy while signed out says not signed in`() = runTest {
        whenever(authProvider.getCurrentUserId()).thenReturn(null)
        whenever(firestoreDataSource.getUser("bro-uid")).thenReturn(User("bro-uid", "Sam"))

        val result = connector.addBuddy("bro-uid")

        assertEquals(BuddyAddResult.Error("Not signed in."), result)
        verify(firestoreDataSource, never()).sendBuddyInvite(any(), any())
    }

    @Test
    fun `addBuddy with an existing buddy is AlreadyBuddies and sends no invite`() = runTest {
        whenever(firestoreDataSource.getUser("bro-uid")).thenReturn(User("bro-uid", "Sam"))
        whenever(firestoreDataSource.checkRelationshipExists("my-uid", "bro-uid")).thenReturn(true)

        val result = connector.addBuddy("bro-uid")

        assertEquals(BuddyAddResult.AlreadyBuddies("Sam"), result)
        verify(firestoreDataSource, never()).sendBuddyInvite(any(), any())
        verify(repository, never()).clearCachedBuddy()
        assertEquals(0, buddyChangedCalls)
    }

    @Test
    fun `addBuddy success sends the invite, clears the cached buddy and notifies the host`() = runTest {
        whenever(firestoreDataSource.getUser("bro-uid")).thenReturn(User("bro-uid", "Sam"))
        whenever(firestoreDataSource.checkRelationshipExists("my-uid", "bro-uid")).thenReturn(false)

        val result = connector.addBuddy("bro-uid")

        assertEquals(BuddyAddResult.Success("Sam"), result)
        verify(firestoreDataSource).sendBuddyInvite("my-uid", "bro-uid")
        verify(repository).clearCachedBuddy()
        assertEquals(1, buddyChangedCalls)
        verify(analyticsTracker).trackBuddyConnected("manual")
    }

    @Test
    fun `addBuddy timing out says the connection timed out`() = runTest {
        val timeout = timeout()
        whenever(firestoreDataSource.getUser("bro-uid")).thenThrow(timeout)

        val result = connector.addBuddy("bro-uid")

        assertEquals(
            BuddyAddResult.Error("Connection timed out. Please check your network and try again."),
            result
        )
    }

    @Test
    fun `addBuddy maps an offline Firestore error to a friendly message`() = runTest {
        whenever(firestoreDataSource.getUser("bro-uid"))
            .thenThrow(IllegalStateException("Failed to get document because the client is OFFLINE"))

        val result = connector.addBuddy("bro-uid")

        assertEquals(BuddyAddResult.Error("Unable to connect. Please check your internet and try again."), result)
    }

    @Test
    fun `addBuddy passes any other error message through`() = runTest {
        whenever(firestoreDataSource.getUser("bro-uid")).thenReturn(User("bro-uid", "Sam"))
        whenever(firestoreDataSource.checkRelationshipExists("my-uid", "bro-uid")).thenReturn(false)
        whenever(firestoreDataSource.sendBuddyInvite("my-uid", "bro-uid"))
            .thenThrow(IllegalStateException("permission denied"))

        val result = connector.addBuddy("bro-uid")

        assertEquals(BuddyAddResult.Error("Failed: permission denied"), result)
        verify(repository, never()).clearCachedBuddy()
        assertEquals(0, buddyChangedCalls)
    }

    // --- randomConnect ---

    @Test
    fun `randomConnect while signed out toasts and looks for no one`() = runTest {
        whenever(authProvider.getCurrentUserId()).thenReturn(null)

        assertNull(connector.randomConnect())

        verifyToast("Not signed in.")
        verify(firestoreDataSource, never()).findRandomBuddy(any())
    }

    @Test
    fun `randomConnect offline toasts no internet`() = runTest {
        setOnline(false)

        assertNull(connector.randomConnect())

        verifyToast("No internet connection.")
        verify(firestoreDataSource, never()).findRandomBuddy(any())
    }

    @Test
    fun `randomConnect finding no one toasts the cooldown message and keeps the old buddy`() = runTest {
        allowRandomConnect()
        whenever(firestoreDataSource.findRandomBuddy("my-uid")).thenReturn(null)

        assertNull(connector.randomConnect())

        verifyToast(RandomConnectOutcome.NoneAvailable.message!!, Toast.LENGTH_LONG)
        verify(repository).saveLastRandomConnectAttemptTime(any())
        verify(repository, never()).clearCachedBuddy()
        verify(repository, never()).saveHasBuddy(any())
        assertEquals(0, buddyChangedCalls)
    }

    @Test
    fun `randomConnect out of weekly quota toasts the upsell`() = runTest {
        whenever(repository.getLastRandomConnectAttemptTime()).thenReturn(0L)
        whenever(firestoreDataSource.hasRandomConnectQuota(any(), any())).thenReturn(false)

        assertNull(connector.randomConnect())

        val expected = RandomConnectOutcome.OutOfQuota(Entitlement.RANDOM_CONNECT_FREE_WEEKLY_LIMIT, isPro = false).message!!
        verifyToast(expected, Toast.LENGTH_LONG)
        verify(firestoreDataSource, never()).findRandomBuddy(any())
    }

    @Test
    fun `randomConnect match saves the new buddy and returns their name without a toast`() = runTest {
        allowRandomConnect()
        whenever(firestoreDataSource.findRandomBuddy("my-uid")).thenReturn("bro-uid")
        whenever(firestoreDataSource.getUser("bro-uid")).thenReturn(User("bro-uid", "Sam"))

        assertEquals("Sam", connector.randomConnect())

        verify(repository).clearCachedBuddy()
        verify(repository).saveHasBuddy(true)
        assertEquals(1, buddyChangedCalls)
        verify(analyticsTracker).trackBuddyConnected("random")
        verify(firestoreDataSource).recordRandomConnectUsed("my-uid")
        verifyNoToast()
    }

    @Test
    fun `randomConnect match with a blank name falls back to the unnamed bro`() = runTest {
        allowRandomConnect()
        whenever(firestoreDataSource.findRandomBuddy("my-uid")).thenReturn("bro-uid")
        whenever(firestoreDataSource.getUser("bro-uid")).thenReturn(User("bro-uid", "  "))

        assertEquals(RandomConnect.UNNAMED_BRO, connector.randomConnect())
        verify(repository).saveHasBuddy(true)
    }

    @Test
    fun `randomConnect match whose user doc is missing still connects as the unnamed bro`() = runTest {
        allowRandomConnect()
        whenever(firestoreDataSource.findRandomBuddy("my-uid")).thenReturn("bro-uid")
        whenever(firestoreDataSource.getUser("bro-uid")).thenReturn(null)

        assertEquals(RandomConnect.UNNAMED_BRO, connector.randomConnect())
        verify(repository).clearCachedBuddy()
        verify(repository).saveHasBuddy(true)
    }

    @Test
    fun `randomConnect timing out toasts a retry hint`() = runTest {
        allowRandomConnect()
        val timeout = timeout()
        whenever(firestoreDataSource.findRandomBuddy("my-uid")).thenThrow(timeout)

        assertNull(connector.randomConnect())

        verifyToast("Connection timed out. Please try again.")
        verify(repository, never()).saveHasBuddy(any())
    }

    @Test
    fun `randomConnect failing toasts something went wrong`() = runTest {
        allowRandomConnect()
        whenever(firestoreDataSource.findRandomBuddy("my-uid")).thenThrow(IllegalStateException("boom"))

        assertNull(connector.randomConnect())

        verifyToast("Something went wrong. Please try again.")
        verify(repository, never()).clearCachedBuddy()
        verify(repository, never()).saveHasBuddy(any())
        assertEquals(0, buddyChangedCalls)
    }
}
