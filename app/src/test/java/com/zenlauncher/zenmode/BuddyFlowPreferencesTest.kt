package com.zenlauncher.zenmode

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BuddyFlowPreferencesTest {

    private fun createMocks(): Triple<Context, SharedPreferences, SharedPreferences.Editor> {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        val editor = mock<SharedPreferences.Editor>()

        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)

        return Triple(context, prefs, editor)
    }

    @Test
    fun `no stored decision is null`() {
        val (context, prefs, _) = createMocks()
        whenever(prefs.getString("buddy_flow_decision", null)).thenReturn(null)

        assertNull(BuddyFlowPreferences.decision(context))
    }

    @Test
    fun `an unrecognized stored value is treated as undecided`() {
        val (context, prefs, _) = createMocks()
        whenever(prefs.getString("buddy_flow_decision", null)).thenReturn("something_stale")

        assertNull(BuddyFlowPreferences.decision(context))
    }

    @Test
    fun `reads back a stored decision`() {
        val (context, prefs, _) = createMocks()
        whenever(prefs.getString("buddy_flow_decision", null)).thenReturn("ZEN_BUDDY_CLASSIC")

        assertEquals(BuddyFlow.ZEN_BUDDY_CLASSIC, BuddyFlowPreferences.decision(context))
    }

    @Test
    fun `setDecision persists the choice`() {
        val (context, _, editor) = createMocks()

        BuddyFlowPreferences.setDecision(context, BuddyFlow.ZEN_CIRCLE)

        verify(editor).putString("buddy_flow_decision", "ZEN_CIRCLE")
        verify(editor).apply()
    }
}
