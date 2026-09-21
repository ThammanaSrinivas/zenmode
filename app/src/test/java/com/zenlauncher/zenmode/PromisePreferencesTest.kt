package com.zenlauncher.zenmode

import android.content.Context
import android.content.SharedPreferences
import com.zenlauncher.zenmode.coreapi.PromisePreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PromisePreferencesTest {

    private fun createMocks(): Triple<Context, SharedPreferences, SharedPreferences.Editor> {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        val editor = mock<SharedPreferences.Editor>()

        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putInt(any(), any())).thenReturn(editor)

        return Triple(context, prefs, editor)
    }

    @Test
    fun `defaults to the placeholder promise`() {
        val (context, prefs, _) = createMocks()
        val default = AppConstants.PLACEHOLDER_PROMISE_HOURS
        whenever(prefs.getInt("promise_daily_hours", default)).thenReturn(default)

        assertEquals(default, PromisePreferences.getDailyHours(context))
    }

    @Test
    fun `clamps an out-of-range stored value`() {
        val (context, prefs, _) = createMocks()
        whenever(prefs.getInt("promise_daily_hours", AppConstants.PLACEHOLDER_PROMISE_HOURS)).thenReturn(99)

        assertEquals(AppConstants.PROMISE_MAX_DAILY_HOURS, PromisePreferences.getDailyHours(context))
    }

    @Test
    fun `setDailyHours persists the clamped value`() {
        val (context, _, editor) = createMocks()

        PromisePreferences.setDailyHours(context, 0)

        verify(editor).putInt("promise_daily_hours", AppConstants.PROMISE_MIN_DAILY_HOURS)
        verify(editor).apply()
    }
}
