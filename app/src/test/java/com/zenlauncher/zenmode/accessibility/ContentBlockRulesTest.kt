package com.zenlauncher.zenmode.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentBlockRulesTest {

    @Test
    fun `default ruleset blocks the short-form feed of each tracked app`() {
        val rules = ContentBlockRules.default()

        val yt = rules.appRule(ContentBlockRules.YOUTUBE)!!
        assertEquals(listOf("shorts"), yt.surfaces.map { it.id })
        assertTrue(yt.surfaces.single().anyViewId.contains("${ContentBlockRules.YOUTUBE}:id/reel_recycler"))

        val ig = rules.appRule(ContentBlockRules.INSTAGRAM)!!
        assertEquals(listOf("reels"), ig.surfaces.map { it.id })
        assertTrue(ig.surfaces.single().anyViewId.contains("${ContentBlockRules.INSTAGRAM}:id/clips_viewer_view_pager"))

        val snap = rules.appRule(ContentBlockRules.SNAPCHAT)!!
        assertEquals(listOf("spotlight"), snap.surfaces.map { it.id })
        assertTrue(snap.surfaces.single().anyViewId.contains("${ContentBlockRules.SNAPCHAT}:id/spotlight_container"))
    }

    @Test
    fun `tracked packages list matches the ruleset apps`() {
        assertEquals(
            ContentBlockRules.TRACKED_PACKAGES.toSet(),
            ContentBlockRules.default().apps.keys
        )
    }

    @Test
    fun `default has no rule for an untracked package`() {
        assertNull(ContentBlockRules.default().appRule("com.whatsapp"))
    }

    @Test
    fun `parse reads every constraint list`() {
        val json = """
            {
              "version": 5,
              "apps": {
                "com.google.android.youtube": {
                  "surfaces": [
                    {
                      "id": "home", "label": "Home",
                      "allViewId": ["a", "b"],
                      "anyViewId": ["c"],
                      "selectedViewId": ["d"],
                      "selectedText": ["Home"]
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val rules = ContentBlockRules.parse(json)
        assertEquals(5, rules.version)
        val s = rules.appRule("com.google.android.youtube")!!.surfaces.single()
        assertEquals(listOf("a", "b"), s.allViewId)
        assertEquals(listOf("c"), s.anyViewId)
        assertEquals(listOf("d"), s.selectedViewId)
        assertEquals(listOf("Home"), s.selectedText)
    }

    @Test
    fun `parse falls back label to id and tolerates missing lists`() {
        val json = """
            { "version": 1, "apps": { "com.x": { "surfaces": [ { "id": "feed", "anyViewId": ["a"] } ] } } }
        """.trimIndent()
        val s = ContentBlockRules.parse(json).appRule("com.x")!!.surfaces.single()
        assertEquals("feed", s.label)
        assertEquals(emptyList<String>(), s.allViewId)
    }

    @Test
    fun `parse returns EMPTY on malformed json`() {
        assertEquals(ContentBlockRules.EMPTY, ContentBlockRules.parse("not json {"))
        assertEquals(0, ContentBlockRules.parse("").version)
    }

    @Test
    fun `parse skips id-less surfaces and empty apps`() {
        val json = """
            {
              "apps": {
                "com.x": { "surfaces": [ { "anyViewId": ["a"] } ] },
                "com.y": { "surfaces": [] }
              }
            }
        """.trimIndent()
        val rules = ContentBlockRules.parse(json)
        assertNull(rules.appRule("com.x"))
        assertNull(rules.appRule("com.y"))
    }
}
