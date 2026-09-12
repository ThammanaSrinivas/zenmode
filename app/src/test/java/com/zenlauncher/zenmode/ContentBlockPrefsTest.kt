package com.zenlauncher.zenmode

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ContentBlockPrefsTest {

    private fun createMocks(): Triple<Context, SharedPreferences, SharedPreferences.Editor> {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        val editor = mock<SharedPreferences.Editor>()

        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)

        return Triple(context, prefs, editor)
    }

    private val yt = "com.google.android.youtube"

    @Test
    fun `surface key is namespaced by package and surface`() {
        assertEquals("block__${yt}__shorts", ContentBlockPrefs.surfaceKey(yt, "shorts"))
    }

    @Test
    fun `surface blocked defaults to false`() {
        val (context, prefs, _) = createMocks()
        whenever(prefs.getBoolean("block__${yt}__shorts", false)).thenReturn(false)

        assertFalse(ContentBlockPrefs.isSurfaceBlocked(context, yt, "shorts"))
    }

    @Test
    fun `setSurfaceBlocked persists under the namespaced key`() {
        val (context, _, editor) = createMocks()

        ContentBlockPrefs.setSurfaceBlocked(context, yt, "home", true)

        verify(editor).putBoolean("block__${yt}__home", true)
        verify(editor).apply()
    }

    @Test
    fun `isAnyBlockEnabled is true only when a block key is true`() {
        val (context, prefs, _) = createMocks()

        whenever(prefs.all).thenReturn(mapOf("debug_dump" to true, "block__${yt}__shorts" to false))
        assertFalse(ContentBlockPrefs.isAnyBlockEnabled(context))

        whenever(prefs.all).thenReturn(mapOf("block__${yt}__shorts" to true))
        assertTrue(ContentBlockPrefs.isAnyBlockEnabled(context))
    }

    @Test
    fun `debug dump toggle round-trips`() {
        val (context, prefs, editor) = createMocks()
        whenever(prefs.getBoolean("debug_dump", false)).thenReturn(true)

        assertTrue(ContentBlockPrefs.isDebugDumpEnabled(context))

        ContentBlockPrefs.setDebugDumpEnabled(context, false)
        verify(editor).putBoolean("debug_dump", false)
        verify(editor).apply()
    }
}
