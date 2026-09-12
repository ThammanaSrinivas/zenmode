package com.zenlauncher.zenmode

import android.content.Context

/**
 * Persists which in-app surfaces the user has chosen to block, plus the
 * developer view-id dump toggle used to tune detection rules on a real device.
 *
 * Keys are `block__<package>__<surfaceId>` so adding an app/surface needs no
 * schema change. Mirrors [DistractingAppsRepository]'s SharedPreferences style.
 */
object ContentBlockPrefs {
    private const val PREFS = "zen_content_block"
    private const val SURFACE_PREFIX = "block__"
    private const val KEY_DEBUG_DUMP = "debug_dump"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun surfaceKey(packageName: String, surfaceId: String) =
        "$SURFACE_PREFIX${packageName}__$surfaceId"

    fun isSurfaceBlocked(ctx: Context, packageName: String, surfaceId: String): Boolean =
        prefs(ctx).getBoolean(surfaceKey(packageName, surfaceId), false)

    fun setSurfaceBlocked(ctx: Context, packageName: String, surfaceId: String, blocked: Boolean) {
        prefs(ctx).edit().putBoolean(surfaceKey(packageName, surfaceId), blocked).apply()
    }

    /** Fast pre-check so the accessibility service can bail before touching the node tree. */
    fun isAnyBlockEnabled(ctx: Context): Boolean =
        prefs(ctx).all.any { (k, v) -> k.startsWith(SURFACE_PREFIX) && v == true }

    fun isDebugDumpEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_DEBUG_DUMP, false)

    fun setDebugDumpEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_DEBUG_DUMP, enabled).apply()
    }
}
