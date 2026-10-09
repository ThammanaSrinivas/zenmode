package com.zenlauncher.zenmode.ui

import com.zenlauncher.zenmode.ui.components.settingsMatch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {

    @Test
    fun `an empty or blank query matches everything`() {
        assertTrue(settingsMatch("", "Resistance screen"))
        assertTrue(settingsMatch("   ", "Resistance screen"))
    }

    @Test
    fun `matching ignores case and looks in the subtitle and group too`() {
        assertTrue(settingsMatch("RESIST", "Resistance screen", null, "Focus"))
        assertTrue(settingsMatch("reels", "Distraction Blocker", "Quiet reels, shorts and more.", "Focus"))
        assertTrue(settingsMatch("phone", "Battery", "Unrestricted keeps streaks counting.", "Phone"))
    }

    @Test
    fun `every word has to appear, in any order`() {
        assertTrue(settingsMatch("lock tap", "Double-tap to lock"))
        assertFalse(settingsMatch("lock swipe", "Double-tap to lock"))
    }
}
