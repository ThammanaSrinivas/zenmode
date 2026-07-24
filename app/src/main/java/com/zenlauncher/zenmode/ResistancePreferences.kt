package com.zenlauncher.zenmode

import android.content.Context

object ResistancePreferences {
    private const val PREFS_NAME = "zenmode_prefs"
    private const val KEY_RESISTANCE = "resistance_enabled"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_RESISTANCE, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_RESISTANCE, enabled).apply()
    }
}
