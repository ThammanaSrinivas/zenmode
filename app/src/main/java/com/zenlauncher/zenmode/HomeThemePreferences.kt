package com.zenlauncher.zenmode

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Pro "Home-screen themes": the wash behind Home and the pages that share its backdrop.
 * [MOOD] is the free default — green, amber or ember by today's screen time. The others are
 * fixed, calm palettes for people who'd rather the background didn't grade them.
 * Palettes live with the backdrop in ui/components/MoodBackdrop.kt.
 */
enum class HomeTheme(val label: String, val caption: String) {
    MOOD("Mood", "Shifts with your day"),
    MOSS("Moss", "Forest floor"),
    TIDE("Tide", "Cool water"),
    DUSK("Dusk", "Last light"),
    SAND("Sand", "Warm and dry"),
    STONE("Stone", "Almost nothing")
}

object HomeThemePreferences {
    private const val PREFS_NAME = "zenmode_prefs"
    private const val KEY_THEME = "home_theme"

    @Volatile
    private var flow: MutableStateFlow<HomeTheme>? = null

    fun state(context: Context): StateFlow<HomeTheme> = flow(context)

    fun set(context: Context, theme: HomeTheme) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme.name).apply()
        flow(context).value = theme
    }

    private fun flow(context: Context): MutableStateFlow<HomeTheme> =
        flow ?: synchronized(this) {
            flow ?: MutableStateFlow(read(context)).also { flow = it }
        }

    /** Re-reads the stored pick; called when the prefs file is wiped out from under the cache. */
    internal fun reload(context: Context) {
        flow?.value = read(context)
    }

    private fun read(context: Context): HomeTheme =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_THEME, null)
            ?.let { name -> HomeTheme.entries.firstOrNull { it.name == name } }
            ?: HomeTheme.MOOD
}
