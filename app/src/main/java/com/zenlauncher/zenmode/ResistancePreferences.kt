package com.zenlauncher.zenmode

import android.content.Context
import com.zenlauncher.zenmode.coreapi.zenPrefs

object ResistancePreferences {
    private const val KEY_RESISTANCE = "resistance_enabled"

    fun isEnabled(context: Context): Boolean {
        return context.zenPrefs()
            .getBoolean(KEY_RESISTANCE, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.zenPrefs()
            .edit().putBoolean(KEY_RESISTANCE, enabled).apply()
    }
}
