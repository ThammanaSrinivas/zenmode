package com.zenlauncher.zenmode

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ThemePreferencesTest {

    private fun prefs(mode: String? = null, legacyDark: Boolean? = null): Context {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.getString("theme_mode", null)).thenReturn(mode)
        whenever(prefs.contains("dark_mode_enabled")).thenReturn(legacyDark != null)
        whenever(prefs.getBoolean("dark_mode_enabled", false)).thenReturn(legacyDark ?: false)
        return context
    }

    @Test
    fun `new installs follow the system`() {
        assertEquals(ThemeMode.SYSTEM, ThemePreferences.mode(prefs()))
    }

    @Test
    fun `a stored mode wins`() {
        assertEquals(ThemeMode.DARK, ThemePreferences.mode(prefs(mode = "DARK")))
        assertEquals(ThemeMode.LIGHT, ThemePreferences.mode(prefs(mode = "LIGHT", legacyDark = true)))
    }

    @Test
    fun `the old dark-mode switch carries over on update`() {
        assertEquals(ThemeMode.DARK, ThemePreferences.mode(prefs(legacyDark = true)))
        assertEquals(ThemeMode.LIGHT, ThemePreferences.mode(prefs(legacyDark = false)))
    }

    @Test
    fun `an unknown stored value falls back to the legacy switch`() {
        assertEquals(ThemeMode.DARK, ThemePreferences.mode(prefs(mode = "SEPIA", legacyDark = true)))
    }
}
