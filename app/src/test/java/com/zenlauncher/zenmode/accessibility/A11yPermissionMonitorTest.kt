package com.zenlauncher.zenmode.accessibility

import com.zenlauncher.zenmode.accessibility.A11yPermissionMonitor.Transition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class A11yPermissionMonitorTest {

    @Test
    fun `newly enabled is a grant`() {
        assertEquals(Transition.GRANTED, A11yPermissionMonitor.transition(enabled = true, wasGranted = false))
    }

    @Test
    fun `granted then missing is a loss`() {
        assertEquals(Transition.LOST, A11yPermissionMonitor.transition(enabled = false, wasGranted = true))
    }

    @Test
    fun `steady states send nothing`() {
        assertNull(A11yPermissionMonitor.transition(enabled = true, wasGranted = true))
        assertNull(A11yPermissionMonitor.transition(enabled = false, wasGranted = false))
    }
}
