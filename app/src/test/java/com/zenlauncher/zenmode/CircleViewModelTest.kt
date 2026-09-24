package com.zenlauncher.zenmode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.zenlauncher.zenmode.coreapi.Circle
import com.zenlauncher.zenmode.coreapi.CircleJoinResult
import com.zenlauncher.zenmode.coreapi.CircleMember
import com.zenlauncher.zenmode.coreapi.CircleRole
import com.zenlauncher.zenmode.coreapi.ReactionType
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.AuthProvider
import com.zenlauncher.zenmode.coreapi.services.FirestoreDataSource
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CircleViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var firestoreDataSource: FirestoreDataSource
    private lateinit var authProvider: AuthProvider

    private fun aCircle(
        id: String = "circle-1",
        name: String = "Zen Squad",
        leaderUid: String = "leader-uid",
        members: List<CircleMember> = listOf(
            CircleMember(
                uid = "leader-uid",
                displayName = "Leader",
                zenScore = 80,
                lastUpdatedEpochMs = 0L,
                role = CircleRole.LEADER,
                joinedAtEpochMs = 0L
            )
        )
    ) = Circle(id = id, name = name, leaderUid = leaderUid, members = members, createdAtEpochMs = 0L)

    @Before
    fun setup() {
        // viewModelScope needs a Main dispatcher in unit tests. Unconfined (unlike
        // StandardTestDispatcher) runs a launched coroutine's body immediately in the calling
        // frame as long as it never hits a real suspension point -- which is the case here
        // since every suspend call goes to a Mockito stub that returns synchronously. That lets
        // methods like createCircle()/leaveCircle() finish updating uiState before the call
        // returns, so assertions right after don't need to advance a scheduler.
        Dispatchers.setMain(UnconfinedTestDispatcher())

        firestoreDataSource = mock()
        authProvider = mock()
        whenever(authProvider.getCurrentUserId()).thenReturn("my-uid")
        whenever(authProvider.getDisplayName()).thenReturn("Me")

        ServiceLocator.firestoreDataSource = firestoreDataSource
        ServiceLocator.authProvider = authProvider
        ServiceLocator.analyticsManager = mock()
        ServiceLocator.analyticsTracker = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repoWithNoCachedCircle(): UsageRepository {
        val repository = mock<UsageRepository>()
        whenever(repository.getUserUid()).thenReturn("my-uid")
        whenever(repository.getCachedCircle()).thenReturn(null)
        whenever(repository.getCachedCircleId()).thenReturn(null)
        return repository
    }

    @Test
    fun `init resolves the cached circleId against the server and refreshes the cache`() = runTest {
        // Unconfined makes the reload launched in init() run to completion synchronously (every
        // suspend call below is a non-suspending mock), so what we can observe is the *final*,
        // server-verified state -- not the transient cache-only value. That's the offline-first
        // contract loadCircle() actually implements: trust the server over a stale cache.
        val stale = aCircle(id = "circle-1", name = "Stale Name")
        val fresh = aCircle(id = "circle-1", name = "Fresh Name")
        val repository = mock<UsageRepository>()
        whenever(repository.getUserUid()).thenReturn("my-uid")
        whenever(repository.getCachedCircle()).thenReturn(stale)
        whenever(repository.getCachedCircleId()).thenReturn("circle-1")
        whenever(firestoreDataSource.getCircle("circle-1")).thenReturn(fresh)
        whenever(firestoreDataSource.getTodayCircleReactions("circle-1", "my-uid")).thenReturn(3L to 5L)

        val viewModel = CircleViewModel(repository)

        assertEquals(fresh, viewModel.uiState.value?.circle)
        assertEquals(3L to 5L, viewModel.uiState.value?.reactions)
        verify(repository).cacheCircle(fresh)
    }

    @Test
    fun `init self-heals when the cached circleId no longer resolves on the server`() = runTest {
        val repository = mock<UsageRepository>()
        whenever(repository.getUserUid()).thenReturn("my-uid")
        whenever(repository.getCachedCircle()).thenReturn(null)
        whenever(repository.getCachedCircleId()).thenReturn("stale-id")
        whenever(firestoreDataSource.getCircle("stale-id")).thenReturn(null)
        val recovered = aCircle(id = "circle-recovered")
        whenever(firestoreDataSource.getMyCircleId("my-uid")).thenReturn("circle-recovered")
        whenever(firestoreDataSource.getCircle("circle-recovered")).thenReturn(recovered)
        whenever(firestoreDataSource.getTodayCircleReactions("circle-recovered", "my-uid")).thenReturn(0L to 0L)

        val viewModel = CircleViewModel(repository)

        assertEquals(recovered, viewModel.uiState.value?.circle)
        verify(repository).saveCircleId("circle-recovered")
        verify(repository).cacheCircle(recovered)
    }

    @Test
    fun `init clears the cache when neither the cached id nor the server has a circle`() = runTest {
        val repository = repoWithNoCachedCircle()
        whenever(firestoreDataSource.getMyCircleId("my-uid")).thenReturn(null)

        val viewModel = CircleViewModel(repository)

        assertNull(viewModel.uiState.value?.circle)
        verify(repository).clearCachedCircle()
    }

    @Test
    fun `createCircle success caches the circle and fires justEnteredCircle`() = runTest {
        val repository = repoWithNoCachedCircle()
        val created = aCircle(id = "new-circle")
        whenever(firestoreDataSource.createCircle("my-uid", "Me", "Zen Squad")).thenReturn(created)

        val viewModel = CircleViewModel(repository)
        viewModel.createCircle("Zen Squad")

        assertEquals(created, viewModel.uiState.value?.circle)
        assertEquals(true, viewModel.uiState.value?.justEnteredCircle)
        assertNull(viewModel.uiState.value?.errorMessage)
        verify(repository).saveCircleId("new-circle")
        verify(repository).cacheCircle(created)
        verify(ServiceLocator.analyticsTracker).trackCircleCreated()
    }

    @Test
    fun `createCircle failure surfaces an error and does not cache`() = runTest {
        val repository = repoWithNoCachedCircle()
        whenever(firestoreDataSource.createCircle(any(), any(), any())).thenReturn(null)

        val viewModel = CircleViewModel(repository)
        viewModel.createCircle("Zen Squad")

        assertNull(viewModel.uiState.value?.circle)
        assertEquals(false, viewModel.uiState.value?.justEnteredCircle)
        assertEquals("Couldn't create circle", viewModel.uiState.value?.errorMessage)
        verify(repository, never()).saveCircleId(any())
        verify(repository, never()).cacheCircle(any())
    }

    @Test
    fun `joinCircle success caches circle and fires justEnteredCircle`() = runTest {
        val repository = repoWithNoCachedCircle()
        whenever(repository.hasCachedBuddy()).thenReturn(false)
        val joined = aCircle(id = "circle-42")
        whenever(firestoreDataSource.joinCircle("circle-42", "my-uid", "Me", confirmedSwitchFromBuddy = false))
            .thenReturn(CircleJoinResult.Success)
        whenever(firestoreDataSource.getCircle("circle-42")).thenReturn(joined)

        val viewModel = CircleViewModel(repository)
        viewModel.joinCircleAwait("circle-42", "code")

        assertEquals(joined, viewModel.uiState.value?.circle)
        assertEquals(true, viewModel.uiState.value?.justEnteredCircle)
        verify(repository).saveCircleId("circle-42")
        verify(repository).cacheCircle(joined)
    }

    @Test
    fun `joinCircle when circle is full surfaces an error`() = runTest {
        val repository = repoWithNoCachedCircle()
        whenever(repository.hasCachedBuddy()).thenReturn(false)
        whenever(firestoreDataSource.joinCircle(any(), any(), any(), any()))
            .thenReturn(CircleJoinResult.CircleFull)

        val viewModel = CircleViewModel(repository)
        viewModel.joinCircleAwait("circle-42", "code")

        assertEquals("This circle is full", viewModel.uiState.value?.errorMessage)
        assertEquals(false, viewModel.uiState.value?.justEnteredCircle)
    }

    @Test
    fun `joinCircle already in circle with a buddy offers the switch prompt instead of an error`() = runTest {
        val repository = repoWithNoCachedCircle()
        whenever(repository.hasCachedBuddy()).thenReturn(true)
        whenever(firestoreDataSource.joinCircle(any(), any(), any(), any()))
            .thenReturn(CircleJoinResult.AlreadyInCircle)

        val viewModel = CircleViewModel(repository)
        viewModel.joinCircleAwait("circle-42", "code")

        assertEquals("circle-42", viewModel.uiState.value?.pendingBuddySwitchCircleId)
        assertNull(viewModel.uiState.value?.errorMessage)
    }

    @Test
    fun `joinCircle already in circle without a buddy surfaces a plain error`() = runTest {
        val repository = repoWithNoCachedCircle()
        whenever(repository.hasCachedBuddy()).thenReturn(false)
        whenever(firestoreDataSource.joinCircle(any(), any(), any(), any()))
            .thenReturn(CircleJoinResult.AlreadyInCircle)

        val viewModel = CircleViewModel(repository)
        viewModel.joinCircleAwait("circle-42", "code")

        assertNull(viewModel.uiState.value?.pendingBuddySwitchCircleId)
        assertEquals("You're already in a circle", viewModel.uiState.value?.errorMessage)
    }

    @Test
    fun `confirmSwitchFromBuddyAndJoin disconnects the buddy then joins on success`() = runTest {
        val repository = repoWithNoCachedCircle()
        whenever(repository.getBuddyUid()).thenReturn("buddy-uid")
        val joined = aCircle(id = "circle-7")
        whenever(firestoreDataSource.joinCircle("circle-7", "my-uid", "Me", confirmedSwitchFromBuddy = true))
            .thenReturn(CircleJoinResult.Success)
        whenever(firestoreDataSource.getCircle("circle-7")).thenReturn(joined)

        val viewModel = CircleViewModel(repository)
        viewModel.confirmSwitchFromBuddyAndJoin("circle-7")

        assertEquals(joined, viewModel.uiState.value?.circle)
        assertEquals(true, viewModel.uiState.value?.justEnteredCircle)
        assertNull(viewModel.uiState.value?.pendingBuddySwitchCircleId)
        verify(firestoreDataSource).disconnectBuddy("my-uid", "buddy-uid")
        verify(repository).clearCachedBuddy()
        verify(repository).saveCircleId("circle-7")
    }

    @Test
    fun `confirmSwitchFromBuddyAndJoin surfaces a distinct error when disconnect succeeded but join failed`() = runTest {
        // Deliberately not the codebase's usual silent-safe-default -- the disconnect already
        // happened, so silence would leave the user thinking they're still paired. See the
        // comment in CircleViewModel.confirmSwitchFromBuddyAndJoin.
        val repository = repoWithNoCachedCircle()
        whenever(repository.getBuddyUid()).thenReturn("buddy-uid")
        whenever(firestoreDataSource.joinCircle("circle-7", "my-uid", "Me", confirmedSwitchFromBuddy = true))
            .thenReturn(CircleJoinResult.CircleFull)

        val viewModel = CircleViewModel(repository)
        viewModel.confirmSwitchFromBuddyAndJoin("circle-7")

        verify(firestoreDataSource).disconnectBuddy("my-uid", "buddy-uid")
        verify(repository).clearCachedBuddy()
        assertEquals(
            "Disconnected your buddy, but couldn't join the circle -- please try the invite link again.",
            viewModel.uiState.value?.errorMessage
        )
        assertEquals(false, viewModel.uiState.value?.justEnteredCircle)
    }

    @Test
    fun `confirmSwitchFromBuddyAndJoin surfaces an error when the datasource throws`() = runTest {
        val repository = repoWithNoCachedCircle()
        whenever(repository.getBuddyUid()).thenReturn("buddy-uid")
        whenever(firestoreDataSource.disconnectBuddy(any(), any())).thenThrow(RuntimeException("network"))

        val viewModel = CircleViewModel(repository)
        viewModel.confirmSwitchFromBuddyAndJoin("circle-7")

        assertEquals(
            "Something went wrong switching to Circle -- please try again.",
            viewModel.uiState.value?.errorMessage
        )
        assertEquals(false, viewModel.uiState.value?.justEnteredCircle)
    }

    @Test
    fun `confirmSwitchFromBuddyAndJoin with no cached buddy still joins`() = runTest {
        val repository = repoWithNoCachedCircle()
        whenever(repository.getBuddyUid()).thenReturn(null)
        val joined = aCircle(id = "circle-9")
        whenever(firestoreDataSource.joinCircle("circle-9", "my-uid", "Me", confirmedSwitchFromBuddy = true))
            .thenReturn(CircleJoinResult.Success)
        whenever(firestoreDataSource.getCircle("circle-9")).thenReturn(joined)

        val viewModel = CircleViewModel(repository)
        viewModel.confirmSwitchFromBuddyAndJoin("circle-9")

        verify(firestoreDataSource, never()).disconnectBuddy(any(), any())
        assertEquals(joined, viewModel.uiState.value?.circle)
    }

    /** Stubs the init-time [CircleViewModel.loadCircle] reload to resolve to [current] so the
     * ViewModel's state matches "already in this circle" before the action under test runs. */
    private suspend fun stubExistingCircle(repository: UsageRepository, current: Circle) {
        whenever(repository.getUserUid()).thenReturn("my-uid")
        whenever(repository.getCachedCircle()).thenReturn(current)
        whenever(repository.getCachedCircleId()).thenReturn(current.id)
        whenever(firestoreDataSource.getCircle(current.id)).thenReturn(current)
        whenever(firestoreDataSource.getTodayCircleReactions(current.id, "my-uid")).thenReturn(0L to 0L)
    }

    @Test
    fun `leaveCircle success clears cache and emits a one-shot justLeftCircle state`() = runTest {
        val repository = mock<UsageRepository>()
        stubExistingCircle(repository, aCircle(id = "circle-1"))
        whenever(firestoreDataSource.leaveCircle("circle-1", "my-uid")).thenReturn(true)

        val viewModel = CircleViewModel(repository)
        viewModel.leaveCircle()

        assertEquals(true, viewModel.uiState.value?.justLeftCircle)
        assertNull(viewModel.uiState.value?.circle)
        verify(repository).clearCachedCircle()
        verify(ServiceLocator.analyticsTracker).trackCircleLeft()
    }

    @Test
    fun `leaveCircle failure keeps the circle and surfaces an error`() = runTest {
        val repository = mock<UsageRepository>()
        stubExistingCircle(repository, aCircle(id = "circle-1"))
        whenever(firestoreDataSource.leaveCircle("circle-1", "my-uid")).thenReturn(false)

        val viewModel = CircleViewModel(repository)
        viewModel.leaveCircle()

        assertEquals(false, viewModel.uiState.value?.justLeftCircle)
        assertEquals("Couldn't leave circle", viewModel.uiState.value?.errorMessage)
        assertEquals("circle-1", viewModel.uiState.value?.circle?.id)
        verify(repository, never()).clearCachedCircle()
    }

    @Test
    fun `consumeLeftCircleEvent resets the one-shot flag`() = runTest {
        val repository = mock<UsageRepository>()
        stubExistingCircle(repository, aCircle(id = "circle-1"))
        whenever(firestoreDataSource.leaveCircle("circle-1", "my-uid")).thenReturn(true)

        val viewModel = CircleViewModel(repository)
        viewModel.leaveCircle()
        assertEquals(true, viewModel.uiState.value?.justLeftCircle)

        viewModel.consumeLeftCircleEvent()
        assertEquals(false, viewModel.uiState.value?.justLeftCircle)
    }

    @Test
    fun `consumeEnteredCircleEvent resets the one-shot flag`() = runTest {
        val repository = repoWithNoCachedCircle()
        val created = aCircle()
        whenever(firestoreDataSource.createCircle(any(), any(), any())).thenReturn(created)

        val viewModel = CircleViewModel(repository)
        viewModel.createCircle("Zen Squad")
        assertEquals(true, viewModel.uiState.value?.justEnteredCircle)

        viewModel.consumeEnteredCircleEvent()
        assertEquals(false, viewModel.uiState.value?.justEnteredCircle)
    }

    @Test
    fun `removeMember reloads the circle on success`() = runTest {
        val repository = mock<UsageRepository>()
        val current = aCircle(id = "circle-1")
        stubExistingCircle(repository, current)
        whenever(firestoreDataSource.removeCircleMember("circle-1", "my-uid", "target-uid")).thenReturn(true)

        val viewModel = CircleViewModel(repository)
        viewModel.removeMember("target-uid")

        assertEquals(false, viewModel.uiState.value?.removing)
        verify(ServiceLocator.analyticsTracker).trackCircleMemberRemoved(byLeader = true)
        // Once from init's loadCircle(), once more from removeMember()'s post-success reload.
        verify(firestoreDataSource, org.mockito.kotlin.times(2)).getCircle("circle-1")
    }

    @Test
    fun `removeMember failure does not reload the circle`() = runTest {
        val repository = mock<UsageRepository>()
        val current = aCircle(id = "circle-1")
        stubExistingCircle(repository, current)
        whenever(firestoreDataSource.removeCircleMember("circle-1", "my-uid", "target-uid")).thenReturn(false)

        val viewModel = CircleViewModel(repository)
        viewModel.removeMember("target-uid")

        assertEquals(false, viewModel.uiState.value?.removing)
        // Only the one call from init's loadCircle() -- no reload on a failed removal.
        verify(firestoreDataSource, org.mockito.kotlin.times(1)).getCircle(any())
    }

    @Test
    fun `transferLeadership reloads circle only on success`() = runTest {
        val repository = mock<UsageRepository>()
        val current = aCircle(id = "circle-1")
        stubExistingCircle(repository, current)
        whenever(firestoreDataSource.transferLeadership("circle-1", "my-uid", "new-leader")).thenReturn(true)

        val viewModel = CircleViewModel(repository)
        viewModel.transferLeadership("new-leader")

        verify(ServiceLocator.analyticsTracker).trackCircleLeadershipTransferred("voluntary")
        // Once from init's loadCircle(), once more from the post-success reload.
        verify(firestoreDataSource, org.mockito.kotlin.times(2)).getCircle("circle-1")
    }

    @Test
    fun `transferLeadership does not track analytics when it fails`() = runTest {
        val repository = mock<UsageRepository>()
        val current = aCircle(id = "circle-1")
        stubExistingCircle(repository, current)
        whenever(firestoreDataSource.transferLeadership("circle-1", "my-uid", "new-leader")).thenReturn(false)

        val viewModel = CircleViewModel(repository)
        viewModel.transferLeadership("new-leader")

        verify(ServiceLocator.analyticsTracker, never()).trackCircleLeadershipTransferred(any())
    }

    @Test
    fun `sendReaction under the rate limit sends and tracks analytics`() = runTest {
        val repository = mock<UsageRepository>()
        val current = aCircle(id = "circle-1")
        stubExistingCircle(repository, current)
        whenever(repository.getRecentReactionTimestamps("target-uid")).thenReturn(emptyList())
        whenever(firestoreDataSource.sendCircleReaction("circle-1", "my-uid", "target-uid", ReactionType.LOVE))
            .thenReturn(true)

        val viewModel = CircleViewModel(repository)
        viewModel.sendReaction("target-uid", ReactionType.LOVE)

        verify(repository).recordReactionSent("target-uid")
        verify(ServiceLocator.analyticsTracker).trackCircleReactionSent("love")
        assertNull(viewModel.uiState.value?.errorMessage)
    }

    @Test
    fun `sendReaction at the rate limit is blocked client-side without calling the datasource`() = runTest {
        val repository = mock<UsageRepository>()
        val current = aCircle(id = "circle-1")
        stubExistingCircle(repository, current)
        val now = System.currentTimeMillis()
        val maxed = List(UsageRepository.CIRCLE_REACTION_MAX_COUNT) { now - it * 1000L }
        whenever(repository.getRecentReactionTimestamps("target-uid")).thenReturn(maxed)

        val viewModel = CircleViewModel(repository)
        viewModel.sendReaction("target-uid", ReactionType.LOVE)

        verify(repository, never()).recordReactionSent(any())
        verify(firestoreDataSource, never()).sendCircleReaction(any(), any(), any(), any())
        assertTrue(viewModel.uiState.value?.errorMessage?.contains("Wait") == true)
    }

    @Test
    fun `sendReaction rolls back the rate-limit timestamp when the send fails`() = runTest {
        val repository = mock<UsageRepository>()
        val current = aCircle(id = "circle-1")
        stubExistingCircle(repository, current)
        whenever(repository.getRecentReactionTimestamps("target-uid")).thenReturn(emptyList())
        whenever(firestoreDataSource.sendCircleReaction("circle-1", "my-uid", "target-uid", ReactionType.MELT))
            .thenReturn(false)

        val viewModel = CircleViewModel(repository)
        viewModel.sendReaction("target-uid", ReactionType.MELT)

        verify(repository).recordReactionSent("target-uid")
        verify(repository).removeLastReactionTimestamp("target-uid")
        assertEquals("Failed to react", viewModel.uiState.value?.errorMessage)
    }

    @Test
    fun `clearError resets the error message`() = runTest {
        val repository = repoWithNoCachedCircle()
        whenever(firestoreDataSource.createCircle(any(), any(), any())).thenReturn(null)

        val viewModel = CircleViewModel(repository)
        viewModel.createCircle("Zen Squad")
        assertEquals("Couldn't create circle", viewModel.uiState.value?.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value?.errorMessage)
    }

    @Test
    fun `dismissBuddySwitchPrompt clears the pending switch id`() = runTest {
        val repository = repoWithNoCachedCircle()
        whenever(repository.hasCachedBuddy()).thenReturn(true)
        whenever(firestoreDataSource.joinCircle(any(), any(), any(), any()))
            .thenReturn(CircleJoinResult.AlreadyInCircle)

        val viewModel = CircleViewModel(repository)
        viewModel.joinCircleAwait("circle-42", "code")
        assertEquals("circle-42", viewModel.uiState.value?.pendingBuddySwitchCircleId)

        viewModel.dismissBuddySwitchPrompt()
        assertNull(viewModel.uiState.value?.pendingBuddySwitchCircleId)
    }

    @Test
    fun `CircleViewModelFactory creates ViewModel`() {
        val repository = repoWithNoCachedCircle()
        val factory = CircleViewModelFactory(repository)
        val viewModel = factory.create(CircleViewModel::class.java)

        assertTrue(viewModel is CircleViewModel)
    }
}
