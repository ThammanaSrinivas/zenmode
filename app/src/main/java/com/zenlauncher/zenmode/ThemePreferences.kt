package com.zenlauncher.zenmode

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
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

    // Built on first use, not at class-init: unit tests touch [mode] without a Looper.
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var pendingApply: Runnable? = null

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
     * Saves [mode] and applies it, optionally [applyAfterMillis] later so a screen that is
     * crossfading can finish before AppCompat recreates every open activity onto the matching
     * colours. The delayed apply is posted to the main thread rather than run in a composition
     * scope: leaving Settings mid-crossfade must not strand the night-qualified resources on the
     * old theme until the next launch.
     */
    fun setMode(context: Context, mode: ThemeMode, applyAfterMillis: Long = 0L) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .remove(KEY_LEGACY_DARK)
            .apply()
        flow(app).value = mode
        pendingApply?.let(mainHandler::removeCallbacks)
        pendingApply = null
        if (applyAfterMillis <= 0L) {
            applyStoredTheme(app)
        } else {
            val task = Runnable {
                pendingApply = null
                applyStoredTheme(app)
            }
            pendingApply = task
            mainHandler.postDelayed(task, applyAfterMillis)
        }
    }

    fun applyStoredTheme(context: Context) = mode(context).apply()

    /**
     * Wipes the whole `zenmode_prefs` file on account delete. [HomeThemePreferences] and
     * [ZenSound] cache their own keys from this same file, so they are re-read here too —
     * otherwise a deleted account keeps its home theme and sound setting until the next launch.
     */
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
        modeFlow?.value = ThemeMode.SYSTEM
        // Reapply to AppCompat too: otherwise a prior explicit Dark/Light choice stays forced on
        // the process, so the next Activity's Configuration (and everything reading it via
        // colorResource, e.g. the onboarding promise card) disagrees with ZenTheme's now-SYSTEM
        // colors, which resolve straight from the real system setting instead.
        applyStoredTheme(context)
        HomeThemePreferences.reload(context)
        ZenSound.reload(context)
    }
}
