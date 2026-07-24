package com.zenlauncher.zenmode

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ResistancePreferencesTest {

    private fun createMocks(): Triple<Context, SharedPreferences, SharedPreferences.Editor> {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        val editor = mock<SharedPreferences.Editor>()

        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)

        return Triple(context, prefs, editor)
    }

    @Test
    fun `resistance is disabled by default`() {
        val (context, prefs, _) = createMocks()
        whenever(prefs.getBoolean("resistance_enabled", false)).thenReturn(false)

        assertEquals(false, ResistancePreferences.isEnabled(context))
    }

    @Test
    fun `isEnabled returns true when preference set`() {
        val (context, prefs, _) = createMocks()
        whenever(prefs.getBoolean("resistance_enabled", false)).thenReturn(true)

        assertEquals(true, ResistancePreferences.isEnabled(context))
    }

    @Test
    fun `setEnabled persists preference`() {
        val (context, _, editor) = createMocks()

        ResistancePreferences.setEnabled(context, true)

        verify(editor).putBoolean("resistance_enabled", true)
        verify(editor).apply()
    }
}
