package com.zenlauncher.zenmode

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray

/**
 * Stores the user-selected list of "distracting" apps and exposes a single
 * [isDistracting] check used by [DoomScrollingMonitorService].
 *
 * Two buckets:
 *  - Forced: hardcoded popular apps + anything whose [ApplicationInfo.category]
 *    is SOCIAL/VIDEO/GAME. These are always treated as distracting and cannot
 *    be unselected by the user. Newly installed SOCIAL/VIDEO/GAME apps are
 *    covered automatically because the category is read live.
 *  - User-selected: persisted in SharedPreferences as a JSON array.
 */
object DistractingAppsRepository {
    private const val PREFS = "zen_mode_stats"
    private const val KEY_USER_SELECTED = "distracting_apps_user"

    val FORCED_PACKAGES: Set<String> = setOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.google.android.youtube",
        "com.netflix.mediaclient"
    )

    fun isForcedCategory(pm: PackageManager, pkg: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return try {
            val info = pm.getApplicationInfo(pkg, 0)
            when (info.category) {
                ApplicationInfo.CATEGORY_SOCIAL,
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_GAME -> true
                else -> false
            }
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isForced(pm: PackageManager, pkg: String): Boolean =
        pkg in FORCED_PACKAGES || isForcedCategory(pm, pkg)

    fun getUserSelected(ctx: Context): Set<String> {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_USER_SELECTED, null) ?: return emptySet()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun saveUserSelected(ctx: Context, packages: Set<String>) {
        val array = JSONArray(packages.toList())
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_SELECTED, array.toString())
            .apply()
    }

    fun toggleUserSelected(ctx: Context, pkg: String): Boolean {
        val current = getUserSelected(ctx).toMutableSet()
        val nowSelected = if (current.contains(pkg)) {
            current.remove(pkg); false
        } else {
            current.add(pkg); true
        }
        saveUserSelected(ctx, current)
        return nowSelected
    }

    fun isDistracting(ctx: Context, pm: PackageManager, pkg: String): Boolean =
        isForced(pm, pkg) || pkg in getUserSelected(ctx)
}
