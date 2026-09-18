package com.zenlauncher.zenmode.onboarding

import android.content.Context
import org.json.JSONArray

/**
 * Choices made mid-onboarding, on disk. Saved-instance state isn't enough: granting the
 * home role makes Android open a new home task, where a fresh OnboardingActivity starts
 * and the original instance — with its ViewModel — is destroyed. Cleared on completion.
 */
internal object OnboardingDraft {
    private const val PREFS_NAME = "zenmode_onboarding_draft"
    private const val KEY_SELECTED_APPS = "selected_apps"
    private const val KEY_PROMISE_HOURS = "promise_hours"
    private const val KEY_AWAITING_DEFAULT = "awaiting_default_launcher"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedApps(context: Context): List<String>? =
        prefs(context).getString(KEY_SELECTED_APPS, null)?.let { json ->
            runCatching { JSONArray(json).let { array -> List(array.length()) { array.getString(it) } } }.getOrNull()
        }

    fun setSelectedApps(context: Context, packages: List<String>) {
        prefs(context).edit().putString(KEY_SELECTED_APPS, JSONArray(packages).toString()).apply()
    }

    fun getPromiseHours(context: Context): Int? =
        prefs(context).getInt(KEY_PROMISE_HOURS, -1).takeIf { it > 0 }

    fun setPromiseHours(context: Context, hours: Int) {
        prefs(context).edit().putInt(KEY_PROMISE_HOURS, hours).apply()
    }

    /** The user asked to make ZenMode the home app and we're waiting to see it happen. */
    fun isAwaitingDefaultLauncher(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AWAITING_DEFAULT, false)

    fun setAwaitingDefaultLauncher(context: Context, awaiting: Boolean) {
        prefs(context).edit().putBoolean(KEY_AWAITING_DEFAULT, awaiting).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
