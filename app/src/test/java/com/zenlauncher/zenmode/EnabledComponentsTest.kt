package com.zenlauncher.zenmode

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnabledComponentsTest {

    private val pkg = "com.zenlauncher.zenmode"
    private val cls = "com.zenlauncher.zenmode.ZenAccessibilityService"

    @Test
    fun `matches full form written by the Settings app`() {
        assertTrue(EnabledComponents.contains("$pkg/$cls", pkg, cls))
    }

    @Test
    fun `matches short form written by system_server`() {
        assertTrue(EnabledComponents.contains("$pkg/.ZenAccessibilityService", pkg, cls))
    }

    @Test
    fun `matches among other services in either form`() {
        val setting = "com.other/.OtherService:$pkg/.ZenAccessibilityService:com.x/com.x.Y"
        assertTrue(EnabledComponents.contains(setting, pkg, cls))
    }

    @Test
    fun `missing or empty setting is not enabled`() {
        assertFalse(EnabledComponents.contains(null, pkg, cls))
        assertFalse(EnabledComponents.contains("", pkg, cls))
    }

    @Test
    fun `other class in same package does not match`() {
        assertFalse(EnabledComponents.contains("$pkg/.ZenNotificationListenerService", pkg, cls))
    }

    @Test
    fun `same class name under another package does not match`() {
        assertFalse(EnabledComponents.contains("com.evil/$cls", pkg, cls))
    }

    @Test
    fun `malformed entries are ignored`() {
        assertFalse(EnabledComponents.contains("garbage:/.ZenAccessibilityService:$pkg", pkg, cls))
    }
}
