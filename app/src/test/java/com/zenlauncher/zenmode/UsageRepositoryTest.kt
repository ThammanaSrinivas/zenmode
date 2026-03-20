package com.zenlauncher.zenmode

import android.content.Context
import android.content.SharedPreferences
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.analytics.AnalyticsManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UsageRepositoryTest {

    @Test
    fun `incrementUnlockCount increments count when already exists for today`() {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        val editor = mock<SharedPreferences.Editor>()

        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        
        // Mock getting "today" is tricky without dependency injection for DateProvider in Repository
        // But we can check behavior based on mocked existing data.
        
        // Mock current date as already stored
        whenever(prefs.getString(eq("last_date_unlocks"), any())).thenReturn(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()))
        whenever(prefs.getInt("unlock_count", 0)).thenReturn(5)
        
        // Chain for Editor
        whenever(editor.putInt(any(), any())).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        
        val analyticsManager = mock<AnalyticsManager>()
        val repository = UsageRepository(context, analyticsManager)
        repository.incrementUnlockCount()

        verify(editor).putInt("unlock_count", 6)
        verify(editor).apply()
    }
    
    @Test
    fun `updateScreenTime increases total time`() {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        val editor = mock<SharedPreferences.Editor>()

        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        whenever(prefs.getString(eq("last_date_screentime"), any())).thenReturn(today)
        whenever(prefs.getLong("daily_screen_time", 0)).thenReturn(1000L)
        
        whenever(editor.putLong(any(), any())).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        
        val analyticsManager = mock<AnalyticsManager>()
        val repository = UsageRepository(context, analyticsManager)
        repository.updateScreenTime(5000L) // 5 seconds
        
        verify(editor).putLong("daily_screen_time", 6000L)
        verify(editor).apply()
    }

    @Test
    fun `onboarding status is retrieved correctly`() {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        
        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.getBoolean("is_onboarding_complete", false)).thenReturn(true)
        
        val analyticsManager = mock<AnalyticsManager>()
        val repository = UsageRepository(context, analyticsManager)
        val result = repository.isOnboardingComplete()
        
        assertEquals(true, result)
    }

    @Test
    fun `zen unlock flag operations work correctly`() {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        val editor = mock<SharedPreferences.Editor>()
        
        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        
        val analyticsManager = mock<AnalyticsManager>()
        val repository = UsageRepository(context, analyticsManager)
        
        // Test Set
        repository.setZenUnlockFlag(true)
        verify(editor).putBoolean("is_zen_unlocked", true)
        verify(editor).apply()
        
        // Test Reset
        repository.resetZenUnlockFlag()
        verify(editor).putBoolean("is_zen_unlocked", false)
        
        // Test Get
        whenever(prefs.getBoolean("is_zen_unlocked", true)).thenReturn(true)
        assertEquals(true, repository.isZenUnlocked())
    }
}
