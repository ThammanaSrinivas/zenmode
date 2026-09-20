package com.zenlauncher.zenmode

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Appearance the user picked. [SYSTEM] follows the phone's own dark-theme setting. */
enum class ThemeMode(val label: String, private val nightMode: Int) {
    SYSTEM("System", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT("Light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("Dark", AppCompatDelegate.MODE_NIGHT_YES);

    internal fun apply() = AppCompatDelegate.setDefaultNightMode(nightMode)
}

object ThemePreferences {
    private const val PREFS_NAME = "zenmode_prefs"
    private const val KEY_MODE = "theme_mode"
    /** v3.0 stored a plain on/off. Read once as a fallback so nobody's choice flips on update. */
    private const val KEY_LEGACY_DARK = "dark_mode_enabled"

    @Volatile
    private var modeFlow: MutableStateFlow<ThemeMode>? = null

    /** Live [mode], so an open screen can crossfade the moment the choice changes. */
    fun modeState(context: Context): StateFlow<ThemeMode> = flow(context)

    private fun flow(context: Context): MutableStateFlow<ThemeMode> =
        modeFlow ?: synchronized(this) {
            modeFlow ?: MutableStateFlow(mode(context)).also { modeFlow = it }
        }

    fun mode(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_MODE, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?.let { return it }
        return when {
            !prefs.contains(KEY_LEGACY_DARK) -> ThemeMode.SYSTEM
            prefs.getBoolean(KEY_LEGACY_DARK, false) -> ThemeMode.DARK
            else -> ThemeMode.LIGHT
        }
    }

    /** Whether ink (dark) should render right now, resolving [ThemeMode.SYSTEM] against [context]. */
    fun isDarkMode(context: Context): Boolean = when (mode(context)) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark()
    }

    /**
     * The phone's own setting. Read from system resources, not [Context.getResources]: an
     * activity's configuration carries AppCompat's forced night mode until it's recreated.
     */
    fun isSystemDark(): Boolean =
        Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    /**
     * Saves [mode] without applying it. Settings calls [applyStoredTheme] once its crossfade has
     * played, so the activity recreation that AppCompat triggers lands on already-matching colours.
     */
    fun setMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .remove(KEY_LEGACY_DARK)
            .apply()
        flow(context).value = mode
    }

    fun applyStoredTheme(context: Context) = mode(context).apply()

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
        modeFlow?.value = ThemeMode.SYSTEM
    }
}
