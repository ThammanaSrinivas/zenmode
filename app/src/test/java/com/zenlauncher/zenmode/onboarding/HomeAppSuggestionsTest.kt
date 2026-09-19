package com.zenlauncher.zenmode.onboarding

import com.zenlauncher.zenmode.onboarding.HomeAppSuggestions.Candidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeAppSuggestionsTest {

    private val installed = listOf(
        Candidate("com.instagram.android", "Instagram"),
        Candidate("com.google.android.apps.maps", "Maps"),
        Candidate("com.google.android.dialer", "Phone"),
        Candidate("org.notes", "Notes"),
        Candidate("com.spotify.music", "Spotify"),
        Candidate("com.google.android.apps.messaging", "Messages"),
        Candidate("com.bank", "Bank")
    )

    @Test
    fun `essentials come first, in essential order`() {
        val picks = HomeAppSuggestions.suggest(installed, distracting = emptySet(), limit = 3)
        assertEquals(
            listOf("com.google.android.dialer", "com.google.android.apps.messaging", "com.google.android.apps.maps"),
            picks
        )
    }

    @Test
    fun `most used calm apps come before the alphabetical fill`() {
        val picks = HomeAppSuggestions.suggest(
            installed,
            distracting = setOf("com.instagram.android"),
            usageMinutes = mapOf("com.spotify.music" to 90L, "com.bank" to 5L, "com.instagram.android" to 500L)
        )
        assertEquals(listOf("com.spotify.music", "com.bank", "org.notes"), picks.drop(3))
    }

    @Test
    fun `distracting apps are never suggested`() {
        val picks = HomeAppSuggestions.suggest(installed, distracting = setOf("com.instagram.android"))
        assertFalse("com.instagram.android" in picks)
        assertEquals(6, picks.size)
    }

    @Test
    fun `toggle adds until the limit, then ignores, and always removes`() {
        var selected = emptyList<String>()
        repeat(3) { selected = HomeAppSuggestions.toggle(selected, "app$it", limit = 2) }
        assertEquals(listOf("app0", "app1"), selected)
        selected = HomeAppSuggestions.toggle(selected, "app0", limit = 2)
        assertEquals(listOf("app1"), selected)
    }
}
