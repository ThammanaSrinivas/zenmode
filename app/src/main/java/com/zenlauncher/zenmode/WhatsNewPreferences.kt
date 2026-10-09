package com.zenlauncher.zenmode

import android.content.Context
import com.zenlauncher.zenmode.coreapi.zenPrefs

/** Which release notes this install has already seen. */
object WhatsNewPreferences {
    private const val KEY_SEEN_VERSION = "whats_new_seen_version"

    fun shouldShow(context: Context): Boolean {
        val prefs = context.zenPrefs()
        val seen = if (prefs.contains(KEY_SEEN_VERSION)) prefs.getInt(KEY_SEEN_VERSION, 0) else null
        val show = WhatsNew.shouldShow(seen, isFreshInstall(context))
        if (!show && seen == null) markSeen(context)
        return show
    }

    fun markSeen(context: Context) {
        context.zenPrefs()
            .edit()
            .putInt(KEY_SEEN_VERSION, WhatsNew.NOTES_VERSION_CODE)
            .apply()
    }

    /** Never updated since install: the install and last-update times are the same moment. */
    private fun isFreshInstall(context: Context): Boolean = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.firstInstallTime == info.lastUpdateTime
    }.getOrDefault(false)
}
