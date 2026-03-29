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

    private fun createMockedRepository(): Triple<UsageRepository, SharedPreferences, SharedPreferences.Editor> {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        val editor = mock<SharedPreferences.Editor>()

        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putInt(any(), any())).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.putLong(any(), any())).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(editor.remove(any())).thenReturn(editor)

        val analyticsManager = mock<AnalyticsManager>()
        val repository = UsageRepository(context, analyticsManager)
        return Triple(repository, prefs, editor)
    }

    @Test
    fun `updateScreenTime increases total time`() {
        val (repository, prefs, editor) = createMockedRepository()

        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        whenever(prefs.getString(eq("last_date_screentime"), any())).thenReturn(today)
        whenever(prefs.getLong("daily_screen_time", 0)).thenReturn(1000L)

        repository.updateScreenTime(5000L)

        verify(editor).putLong("daily_screen_time", 6000L)
        verify(editor).apply()
    }

    @Test
    fun `onboarding status is retrieved correctly`() {
        val (repository, prefs, _) = createMockedRepository()

        whenever(prefs.getBoolean("is_onboarding_complete", false)).thenReturn(true)

        assertEquals(true, repository.isOnboardingComplete())
    }

    @Test
    fun `setOnboardingComplete persists value`() {
        val (repository, _, editor) = createMockedRepository()

        repository.setOnboardingComplete(true)

        verify(editor).putBoolean("is_onboarding_complete", true)
        verify(editor).commit()
    }

    @Test
    fun `onboarding current page is stored and retrieved`() {
        val (repository, prefs, editor) = createMockedRepository()

        repository.setOnboardingCurrentPage(3)
        verify(editor).putInt("onboarding_current_page", 3)
        verify(editor).apply()

        whenever(prefs.getInt("onboarding_current_page", 0)).thenReturn(3)
        assertEquals(3, repository.getOnboardingCurrentPage())
    }

    @Test
    fun `clearOnboardingCurrentPage removes key`() {
        val (repository, _, editor) = createMockedRepository()

        repository.clearOnboardingCurrentPage()

        verify(editor).remove("onboarding_current_page")
        verify(editor).apply()
    }

    @Test
    fun `zen unlock flag operations work correctly`() {
        val (repository, prefs, editor) = createMockedRepository()

        repository.setZenUnlockFlag(true)
        verify(editor).putBoolean("is_zen_unlocked", true)

        repository.resetZenUnlockFlag()
        verify(editor).putBoolean("is_zen_unlocked", false)

        whenever(prefs.getBoolean("is_zen_unlocked", true)).thenReturn(true)
        assertEquals(true, repository.isZenUnlocked())
    }

    @Test
    fun `first run flag operations work correctly`() {
        val (repository, prefs, editor) = createMockedRepository()

        whenever(prefs.getBoolean("is_first_run", true)).thenReturn(true)
        assertEquals(true, repository.isFirstRun())

        repository.setFirstRunComplete()
        verify(editor).putBoolean("is_first_run", false)
        verify(editor).apply()
    }

    @Test
    fun `user uid can be saved and retrieved`() {
        val (repository, prefs, editor) = createMockedRepository()

        repository.saveUserUid("test-uid-123")
        verify(editor).putString("user_uid", "test-uid-123")

        whenever(prefs.getString("user_uid", null)).thenReturn("test-uid-123")
        assertEquals("test-uid-123", repository.getUserUid())
    }

    @Test
    fun `buddy uid can be saved and retrieved`() {
        val (repository, prefs, editor) = createMockedRepository()

        repository.saveBuddyUid("buddy-uid-456")
        verify(editor).putString("buddy_uid", "buddy-uid-456")

        whenever(prefs.getString("buddy_uid", null)).thenReturn("buddy-uid-456")
        assertEquals("buddy-uid-456", repository.getBuddyUid())
    }

    @Test
    fun `hasCachedBuddy checks prefs contains key`() {
        val (repository, prefs, _) = createMockedRepository()

        whenever(prefs.contains("buddy_uid")).thenReturn(true)
        assertEquals(true, repository.hasCachedBuddy())

        whenever(prefs.contains("buddy_uid")).thenReturn(false)
        assertEquals(false, repository.hasCachedBuddy())
    }

    @Test
    fun `buddy screen time can be saved and retrieved`() {
        val (repository, prefs, editor) = createMockedRepository()

        repository.saveBuddyScreenTime(120L)
        verify(editor).putLong("buddy_screen_time", 120L)

        whenever(prefs.getLong("buddy_screen_time", 0)).thenReturn(120L)
        val time = repository.getBuddyScreenTime()
        assertEquals(120L, time)
    }

    @Test
    fun `clearCachedBuddy removes buddy keys`() {
        val (repository, _, editor) = createMockedRepository()

        repository.clearCachedBuddy()

        verify(editor).remove("buddy_uid")
        verify(editor).remove("buddy_screen_time")
        verify(editor).apply()
    }

    @Test
    fun `last stats processed time can be saved and retrieved`() {
        val (repository, prefs, editor) = createMockedRepository()

        repository.updateLastStatsProcessedTime(1234567890L)
        verify(editor).putLong("last_stats_processed_timestamp", 1234567890L)

        whenever(prefs.getLong("last_stats_processed_timestamp", 0L)).thenReturn(1234567890L)
        assertEquals(1234567890L, repository.getLastStatsProcessedTime())
    }
}
