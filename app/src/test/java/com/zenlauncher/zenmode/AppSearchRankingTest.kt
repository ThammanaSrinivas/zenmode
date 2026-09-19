package com.zenlauncher.zenmode

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSearchRankingTest {

    private data class App(val label: String, val pkg: String)

    private val apps = listOf(
        App("Apple Music", "com.apple.android.music"),
        App("Google Play Store", "com.android.vending"),
        App("Maps", "com.google.android.apps.maps"),
        App("Phone", "com.google.android.dialer"),
        App("Photos", "com.google.android.apps.photos")
    )

    private fun search(q: String) = AppSearchRanking.filter(apps, q, { it.label }, { it.pkg }).map { it.label }

    @Test
    fun `prefix matches beat letters buried in the middle`() {
        assertEquals(listOf("Phone", "Photos", "Google Play Store", "Apple Music", "Maps"), search("p"))
    }

    @Test
    fun `exact match comes first`() {
        assertEquals("Phone", search("phone").first())
    }

    @Test
    fun `package name finds the dialer`() {
        assertEquals(listOf("Phone"), search("dialer"))
    }

    @Test
    fun `blank query matches nothing`() {
        assertEquals(emptyList<String>(), search("  "))
    }
}
