package com.zenlauncher.zenmode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.coreapi.UsageRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MainViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun `onScreenUnlocked increments count and triggers navigation`() {
        val repository = mock<UsageRepository>()
        
        // Mock getTodayUsage to avoid NPE in refreshStats
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0, 0L))
        
        val viewModel = MainViewModel(repository)
        
        viewModel.onScreenUnlocked()
        
        verify(repository).incrementUnlockCount()
        assertEquals(true, viewModel.navigateToDelayedUnlock.value)
    }

    @Test
    fun `onScreenLocked updates screen time if session started`() {
        // This test is tricky because onScreenLocked relies on sessionStartTime set by onScreenUnlocked
        // and System.currentTimeMillis() which we can't easily mock effectively without a TimeProvider wrapper.
        // But we can verify the repository interaction sequence.
        
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0, 0L))
        
        val viewModel = MainViewModel(repository)
        
        // 1. Simulate Unlock
        viewModel.onScreenUnlocked()
        
        // 2. Simulate Lock (assuming some time passed in real world, or we accept 0 duration if fast enough)
        // If we want to test duration logic, we'd need to inject a Clock. 
        // For now, let's just verify repository methods are called.
        
        viewModel.onScreenLocked()
        
        verify(repository).setZenUnlockFlag(false)
        verify(repository).updateScreenTime(any())
    }
    
    @Test
    fun `refreshStats updates stats LiveData`() {
        val repository = mock<UsageRepository>()
        val expectedUsage = DailyUsage(10, 5000L)
        whenever(repository.getTodayUsage()).thenReturn(expectedUsage)
        
        val viewModel = MainViewModel(repository)
        viewModel.refreshStats()
        
        assertEquals(expectedUsage, viewModel.stats.value)
    }

    @Test
    fun `onResumeCheck triggers navigation if not unlocked`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0, 0L))
        whenever(repository.isZenUnlocked()).thenReturn(false)
        
        val viewModel = MainViewModel(repository)
        viewModel.onResumeCheck()
        
        assertEquals(true, viewModel.navigateToDelayedUnlock.value)
    }

    @Test
    fun `onDelayedUnlockNavigated resets event`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0, 0L))
        
        val viewModel = MainViewModel(repository)
        viewModel.onDelayedUnlockNavigated()
        
        assertEquals(false, viewModel.navigateToDelayedUnlock.value)
    }

    @Test
    fun `onResumeCheck does not double trigger after onScreenUnlocked`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0, 0L))
        whenever(repository.isZenUnlocked()).thenReturn(false)

        val viewModel = MainViewModel(repository)

        // Simulate unlock broadcast triggering first
        viewModel.onScreenUnlocked()
        assertEquals(true, viewModel.navigateToDelayedUnlock.value)

        // Reset as the observer would
        viewModel.onDelayedUnlockNavigated()
        assertEquals(false, viewModel.navigateToDelayedUnlock.value)

        // Now onResume fires — should NOT re-trigger
        viewModel.onResumeCheck()
        assertEquals(false, viewModel.navigateToDelayedUnlock.value)
    }

    @Test
    fun `onScreenUnlocked does not double trigger after onResumeCheck`() {
        val repository = mock<UsageRepository>()
        whenever(repository.getTodayUsage()).thenReturn(DailyUsage(0, 0L))
        whenever(repository.isZenUnlocked()).thenReturn(false)

        val viewModel = MainViewModel(repository)

        // Screen turns on under lock screen — onResume fires first
        viewModel.onResumeCheck()
        assertEquals(true, viewModel.navigateToDelayedUnlock.value)

        // Reset as the observer would
        viewModel.onDelayedUnlockNavigated()
        assertEquals(false, viewModel.navigateToDelayedUnlock.value)

        // 2s later user swipes to unlock — should NOT re-trigger
        viewModel.onScreenUnlocked()
        assertEquals(false, viewModel.navigateToDelayedUnlock.value)
    }
    
    @Test
    fun `MainViewModelFactory creates ViewModel`() {
        val repository = mock<UsageRepository>()
        val factory = MainViewModelFactory(repository)
        val viewModel = factory.create(MainViewModel::class.java)
        
        assert(viewModel is MainViewModel)
    }
}
