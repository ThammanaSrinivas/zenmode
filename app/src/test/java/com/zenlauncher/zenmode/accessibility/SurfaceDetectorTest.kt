package com.zenlauncher.zenmode.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceDetectorTest {

    private class FakeQuery(
        private val present: Set<String> = emptySet(),
        private val selected: Set<String> = emptySet(),
        private val selectedTexts: Set<String> = emptySet()
    ) : NodeQuery {
        override fun hasViewId(viewId: String) = viewId in present
        override fun isViewIdSelected(viewId: String) = viewId in selected
        override fun hasSelectedText(text: String) = selectedTexts.any { it.equals(text, true) }
    }

    @Test
    fun `empty rule never matches`() {
        assertFalse(SurfaceDetector.matches(SurfaceRule("x", "X"), FakeQuery(setOf("a"))))
    }

    @Test
    fun `anyViewId matches on a single present id`() {
        val rule = SurfaceRule("shorts", "Shorts", anyViewId = listOf("yt:id/reel", "yt:id/shorts"))
        assertTrue(SurfaceDetector.matches(rule, FakeQuery(present = setOf("yt:id/shorts"))))
        assertFalse(SurfaceDetector.matches(rule, FakeQuery(present = setOf("yt:id/watch"))))
    }

    @Test
    fun `allViewId requires every id present`() {
        val rule = SurfaceRule("home", "Home", allViewId = listOf("yt:id/browse", "yt:id/chips"))
        assertTrue(SurfaceDetector.matches(rule, FakeQuery(present = setOf("yt:id/browse", "yt:id/chips"))))
        assertFalse(SurfaceDetector.matches(rule, FakeQuery(present = setOf("yt:id/browse"))))
    }

    @Test
    fun `selectedViewId requires present and selected`() {
        val rule = SurfaceRule("home", "Home", selectedViewId = listOf("yt:id/pivot_home"))
        assertFalse(SurfaceDetector.matches(rule, FakeQuery(present = setOf("yt:id/pivot_home"))))
        assertTrue(
            SurfaceDetector.matches(
                rule, FakeQuery(present = setOf("yt:id/pivot_home"), selected = setOf("yt:id/pivot_home"))
            )
        )
    }

    @Test
    fun `selectedText matches case-insensitively`() {
        val rule = SurfaceRule("shorts", "Shorts (tab)", selectedText = listOf("Shorts"))
        assertTrue(SurfaceDetector.matches(rule, FakeQuery(selectedTexts = setOf("shorts"))))
        assertFalse(SurfaceDetector.matches(rule, FakeQuery(selectedTexts = setOf("Home"))))
    }

    @Test
    fun `constraints are conjunctive`() {
        val rule = SurfaceRule(
            "home", "Home (tab)",
            anyViewId = listOf("yt:id/browse"),
            selectedText = listOf("Home")
        )
        // browse present but Home not the selected tab -> no match
        assertFalse(SurfaceDetector.matches(rule, FakeQuery(present = setOf("yt:id/browse"))))
        assertTrue(
            SurfaceDetector.matches(
                rule, FakeQuery(present = setOf("yt:id/browse"), selectedTexts = setOf("Home"))
            )
        )
    }

    @Test
    fun `detect returns first matching surface and keeps its id`() {
        val app = AppRule(
            "com.google.android.youtube",
            listOf(
                SurfaceRule("shorts", "Shorts", anyViewId = listOf("yt:id/reel_recycler")),
                SurfaceRule("shorts", "Shorts (tab)", selectedText = listOf("Shorts")),
                SurfaceRule("home", "Home", allViewId = listOf("yt:id/browse", "yt:id/chips"))
            )
        )
        assertEquals("shorts", SurfaceDetector.detect(app, FakeQuery(selectedTexts = setOf("Shorts")))?.id)
        assertEquals(
            "home",
            SurfaceDetector.detect(app, FakeQuery(present = setOf("yt:id/browse", "yt:id/chips")))?.id
        )
        assertNull(SurfaceDetector.detect(app, FakeQuery(present = setOf("yt:id/settings"))))
    }

    @Test
    fun `detect returns null for null app rule`() {
        assertNull(SurfaceDetector.detect(null, FakeQuery(present = setOf("yt:id/reel_recycler"))))
    }
}
