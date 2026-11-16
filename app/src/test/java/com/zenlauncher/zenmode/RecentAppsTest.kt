package com.zenlauncher.zenmode

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentAppsTest {

    private data class App(val pkg: String, val calm: Boolean = true)

    private val dialer = App("com.google.android.dialer")
    private val messages = App("com.google.android.apps.messaging")
    private val camera = App("com.google.android.GoogleCamera")
    private val maps = App("com.google.android.apps.maps")
    private val notes = App("com.example.notes")
    private val bank = App("com.example.bank")
    private val instagram = App("com.instagram.android", calm = false)

    private val installed = listOf(bank, camera, dialer, instagram, maps, messages, notes)

    private fun suggest(recent: List<String>, apps: List<App> = installed) =
        RecentApps.suggest(recent, apps, { it.pkg }, { it.pkg }, { it.calm }).map { it.pkg }

    @Test
    fun `push moves an app to the front without duplicating it`() {
        assertEquals(listOf("b", "a", "c"), RecentApps.push(listOf("a", "b", "c"), "b"))
    }

    @Test
    fun `push keeps the history bounded`() {
        val full = (1..RecentApps.HISTORY_LIMIT).map { "app$it" }
        val pushed = RecentApps.push(full, "new")
        assertEquals(RecentApps.HISTORY_LIMIT, pushed.size)
        assertEquals("new", pushed.first())
        assertEquals("app${RecentApps.HISTORY_LIMIT - 1}", pushed.last())
    }

    @Test
    fun `recent calm apps come first, newest first, four at most`() {
        assertEquals(
            listOf(notes.pkg, bank.pkg, maps.pkg, camera.pkg),
            suggest(listOf(notes.pkg, bank.pkg, maps.pkg, camera.pkg, dialer.pkg))
        )
    }

    @Test
    fun `distracting apps never appear, however recent`() {
        val picked = suggest(listOf(instagram.pkg, notes.pkg))
        assertEquals(notes.pkg, picked.first())
        assert(instagram.pkg !in picked)
    }

    @Test
    fun `a new install is filled with the calm essentials`() {
        assertEquals(listOf(dialer.pkg, messages.pkg, camera.pkg, maps.pkg), suggest(emptyList()))
    }

    @Test
    fun `a short history is topped up with essentials, skipping ones already picked`() {
        assertEquals(listOf(camera.pkg, notes.pkg, dialer.pkg, messages.pkg), suggest(listOf(camera.pkg, notes.pkg)))
    }

    @Test
    fun `uninstalled apps drop out of the history`() {
        assertEquals(listOf(notes.pkg), suggest(listOf("com.gone.app", notes.pkg), apps = listOf(notes)))
    }
}
