package com.zenlauncher.zenmode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.FirestoreDataSource
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MainViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // MainViewModel accesses ServiceLocator.firestoreDataSource in init
        if (!ServiceLocator.isInitialized) {
            ServiceLocator.firestoreDataSource = mock<FirestoreDataSource>()
            ServiceLocator.analyticsManager = mock()
            ServiceLocator.analyticsTracker = mock()
            ServiceLocator.authProvider = mock()
        }
    }

    @Test
    fun `onScreenUnlocked triggers navigation`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0L))

        val viewModel = MainViewModel(repository)

        viewModel.onScreenUnlocked()

        assertEquals(true, viewModel.navigateToDelayedUnlock.value)
    }

    @Test
    fun `onScreenLocked updates screen time if session started`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0L))

        val viewModel = MainViewModel(repository)

        viewModel.onScreenUnlocked()
        viewModel.onScreenLocked()

        verify(repository).setZenUnlockFlag(false)
        verify(repository).updateScreenTime(any())
    }

    @Test
    fun `refreshStats updates stats LiveData`() {
        val repository = mock<UsageRepository>()
        val expectedUsage = DailyUsage(5000L)
        whenever(repository.getTodayUsage()).thenReturn(expectedUsage)

        val viewModel = MainViewModel(repository)
        viewModel.refreshStats()

        assertEquals(expectedUsage, viewModel.stats.value)
    }

    @Test
    fun `onResumeCheck triggers navigation if not unlocked`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0L))
        whenever(repository.isZenUnlocked()).thenReturn(false)

        val viewModel = MainViewModel(repository)
        viewModel.onResumeCheck()

        assertEquals(true, viewModel.navigateToDelayedUnlock.value)
    }

    @Test
    fun `onDelayedUnlockNavigated resets event`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0L))

        val viewModel = MainViewModel(repository)
        viewModel.onDelayedUnlockNavigated()

        assertEquals(false, viewModel.navigateToDelayedUnlock.value)
    }

    @Test
    fun `onResumeCheck does not double trigger after onScreenUnlocked`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0L))
        whenever(repository.isZenUnlocked()).thenReturn(false)

        val viewModel = MainViewModel(repository)

        viewModel.onScreenUnlocked()
        assertEquals(true, viewModel.navigateToDelayedUnlock.value)

        viewModel.onDelayedUnlockNavigated()
        assertEquals(false, viewModel.navigateToDelayedUnlock.value)

        // onResume fires — should NOT re-trigger
        viewModel.onResumeCheck()
        assertEquals(false, viewModel.navigateToDelayedUnlock.value)
    }

    @Test
    fun `onScreenUnlocked does not double trigger after onResumeCheck`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0L))
        whenever(repository.isZenUnlocked()).thenReturn(false)

        val viewModel = MainViewModel(repository)

        viewModel.onResumeCheck()
        assertEquals(true, viewModel.navigateToDelayedUnlock.value)

        viewModel.onDelayedUnlockNavigated()
        assertEquals(false, viewModel.navigateToDelayedUnlock.value)

        // Unlock broadcast fires — should NOT re-trigger
        viewModel.onScreenUnlocked()
        assertEquals(false, viewModel.navigateToDelayedUnlock.value)
    }

    @Test
    fun `onScreenLocked resets hasTriggeredDelayedUnlock allowing next unlock to trigger`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0L))

        val viewModel = MainViewModel(repository)

        // First unlock cycle
        viewModel.onScreenUnlocked()
        assertEquals(true, viewModel.navigateToDelayedUnlock.value)
        viewModel.onDelayedUnlockNavigated()

        // Lock resets the hasTriggeredDelayedUnlock flag
        viewModel.onScreenLocked()

        // Reset the debounce timestamp so the second unlock is not suppressed
        val field = MainViewModel::class.java.getDeclaredField("lastUnlockTimestamp")
        field.isAccessible = true
        field.setLong(viewModel, 0L)

        // Second unlock should trigger again
        viewModel.onScreenUnlocked()
        assertEquals(true, viewModel.navigateToDelayedUnlock.value)
    }

    @Test
    fun `refreshBuddyStatsFromCache updates buddy LiveData when cached`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0L))
        whenever(repository.hasCachedBuddy()).thenReturn(true)
        whenever(repository.getBuddyScreenTime()).thenReturn(45L)

        val viewModel = MainViewModel(repository)
        viewModel.refreshBuddyStatsFromCache()

        assertEquals(true, viewModel.hasBuddies.value)
        assertEquals(BuddyStats(45L), viewModel.buddyStats.value)
    }

    @Test
    fun `refreshBuddyStatsFromCache does nothing when no cached buddy`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0L))
        whenever(repository.hasCachedBuddy()).thenReturn(false)

        val viewModel = MainViewModel(repository)
        viewModel.refreshBuddyStatsFromCache()

        assertEquals(false, viewModel.hasBuddies.value)
        assertEquals(null, viewModel.buddyStats.value)
    }

    @Test
    fun `MainViewModelFactory creates ViewModel`() {
        val repository = mock<UsageRepository>()
        val factory = MainViewModelFactory(repository)
        val viewModel = factory.create(MainViewModel::class.java)

        assert(viewModel is MainViewModel)
    }
}
