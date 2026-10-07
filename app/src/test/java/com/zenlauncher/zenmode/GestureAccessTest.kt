package com.zenlauncher.zenmode

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The free/Pro split is the whole point of [GesturePreferences.active], so that's what's
 * tested: which gestures are in force, not how they're stored.
 */
class GestureAccessTest {

    @Test
    fun `search is the free gesture and the others are not`() {
        assertEquals(
            setOf(HomeGesture.SWIPE_UP_SEARCH),
            HomeGesture.entries.filterNot { it.isPro }.toSet()
        )
    }

    @Test
    fun `free keeps the search gesture and loses the pro ones`() {
        assertEquals(
            setOf(HomeGesture.SWIPE_UP_SEARCH),
            GesturePreferences.active(HomeGesture.entries.toSet(), isPro = false)
        )
    }

    @Test
    fun `pro keeps every gesture it switched on`() {
        val all = HomeGesture.entries.toSet()
        assertEquals(all, GesturePreferences.active(all, isPro = true))
    }

    @Test
    fun `a pro gesture is held, not erased, while pro is away`() {
        // Settings stores the choice either way; only `active` drops it. Resubscribing has to
        // bring back the same set rather than an empty one.
        val stored = setOf(HomeGesture.DOUBLE_TAP_LOCK)
        assertEquals(emptySet<HomeGesture>(), GesturePreferences.active(stored, isPro = false))
        assertEquals(stored, GesturePreferences.active(stored, isPro = true))
    }

    @Test
    fun `nothing switched on means nothing in force`() {
        assertEquals(emptySet<HomeGesture>(), GesturePreferences.active(emptySet(), isPro = true))
    }
}
