package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.Entitlement
import com.zenlauncher.zenmode.coreapi.services.FirestoreDataSource
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Random Connect's shared rules: cooldown, weekly allowance, and what a match costs. */
class RandomConnectTest {

    private lateinit var firestore: FirestoreDataSource
    private lateinit var repository: UsageRepository
    private var matchCalls = 0

    @Before
    fun setup() {
        firestore = mock()
        repository = mock()
        ServiceLocator.firestoreDataSource = firestore
        matchCalls = 0
    }

    private suspend fun attempt(isPro: Boolean = false, result: String? = "bro") =
        RandomConnect.attempt(repository, "me", isPro) { matchCalls++; result }

    @Test
    fun `a match uses up one of this week's connects`() = runTest {
        whenever(firestore.hasRandomConnectQuota("me", Entitlement.RANDOM_CONNECT_FREE_WEEKLY_LIMIT)).thenReturn(true)

        val outcome = attempt()

        assertEquals(RandomConnectOutcome.Matched("bro"), outcome)
        assertNull(outcome.message)
        verify(firestore).recordRandomConnectUsed("me")
        verify(repository, never()).saveLastRandomConnectAttemptTime(any())
    }

    @Test
    fun `finding no one costs nothing but starts the cooldown`() = runTest {
        whenever(firestore.hasRandomConnectQuota(any(), any())).thenReturn(true)

        val outcome = attempt(result = null)

        assertEquals(RandomConnectOutcome.NoneAvailable, outcome)
        assertTrue(outcome.message!!.contains("30 seconds"))
        verify(firestore, never()).recordRandomConnectUsed(any())
        verify(repository).saveLastRandomConnectAttemptTime(any())
    }

    @Test
    fun `retrying inside the cooldown doesn't look for anyone`() = runTest {
        whenever(repository.getLastRandomConnectAttemptTime()).thenReturn(System.currentTimeMillis() - 10_000)

        val outcome = attempt()

        assertTrue(outcome is RandomConnectOutcome.CoolingDown)
        assertTrue((outcome as RandomConnectOutcome.CoolingDown).secondsLeft in 19..20)
        assertEquals(0, matchCalls)
        verify(firestore, never()).hasRandomConnectQuota(any(), any())
    }

    @Test
    fun `out of free connects points at Pro`() = runTest {
        whenever(firestore.hasRandomConnectQuota(any(), any())).thenReturn(false)

        val outcome = attempt(isPro = false)

        assertEquals(RandomConnectOutcome.OutOfQuota(Entitlement.RANDOM_CONNECT_FREE_WEEKLY_LIMIT, isPro = false), outcome)
        assertTrue(outcome.message!!.contains("Upgrade to Pro"))
        assertEquals(0, matchCalls)
    }

    @Test
    fun `Pro is checked against the Pro allowance and isn't upsold`() = runTest {
        whenever(firestore.hasRandomConnectQuota(anyOrNull(), any())).thenReturn(false)

        val outcome = attempt(isPro = true)

        verify(firestore).hasRandomConnectQuota("me", Entitlement.RANDOM_CONNECT_PRO_WEEKLY_LIMIT)
        assertFalse(outcome.message!!.contains("Upgrade"))
    }
}
