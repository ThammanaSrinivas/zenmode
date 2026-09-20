package com.zenlauncher.zenmode

import android.content.Context
import android.content.SharedPreferences
import com.zenlauncher.zenmode.accessibility.ContentBlockRules
import java.util.concurrent.TimeUnit

/**
 * Everything the Distraction Blocker remembers:
 *  - which in-app surfaces are blocked (`block__<package>__<surfaceId>`, so adding an
 *    app/surface needs no schema change) — "Quiet reels & shorts";
 *  - which whole apps are quieted (sent straight back home when opened);
 *  - a PRO pause that lets everything through until a set time;
 *  - how many times the blocker has stepped in, for the "scrolls stopped" stat;
 *  - the developer view-id dump toggle used to tune detection rules on a real device.
 */
object ContentBlockPrefs {
    private const val PREFS = "zen_content_block"
    private const val SURFACE_PREFIX = "block__"
    private const val KEY_DEBUG_DUMP = "debug_dump"
    private const val KEY_QUIETED_APPS = "quieted_apps"
    private const val KEY_PAUSED_UNTIL = "paused_until"
    private const val KEY_STOPS = "stops_count"

    /** Rough minutes of feed each intervention saves — a short-form session runs ~5–8 min. */
    const val MINUTES_SAVED_PER_STOP = 6

    /** Length of the PRO "Pause" in the blocker's header. */
    const val PAUSE_MINUTES = 30

    fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── In-app surfaces (reels & shorts) ──────────────────────────

    fun surfaceKey(packageName: String, surfaceId: String) =
        "$SURFACE_PREFIX${packageName}__$surfaceId"

    fun isSurfaceBlocked(ctx: Context, packageName: String, surfaceId: String): Boolean =
        prefs(ctx).getBoolean(surfaceKey(packageName, surfaceId), false)

    fun setSurfaceBlocked(ctx: Context, packageName: String, surfaceId: String, blocked: Boolean) {
        prefs(ctx).edit().putBoolean(surfaceKey(packageName, surfaceId), blocked).apply()
    }

    /** "Quiet reels & shorts" is on when every known short-form surface is blocked. */
    fun isReelsQuieted(ctx: Context): Boolean {
        val surfaces = ContentBlockRules.default().apps.values.flatMap { app -> app.surfaces.map { app.packageName to it.id } }
        return surfaces.isNotEmpty() && surfaces.all { (pkg, id) -> isSurfaceBlocked(ctx, pkg, id) }
    }

    fun setReelsQuieted(ctx: Context, quieted: Boolean) {
        val edit = prefs(ctx).edit()
        ContentBlockRules.default().apps.values.forEach { app ->
            app.surfaces.forEach { edit.putBoolean(surfaceKey(app.packageName, it.id), quieted) }
        }
        edit.apply()
    }

    // ── Quieted apps ──────────────────────────────────────────────

    fun quietedApps(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_QUIETED_APPS, emptySet()).orEmpty().toSet()

    fun isAppQuieted(ctx: Context, packageName: String): Boolean = packageName in quietedApps(ctx)

    fun setAppQuieted(ctx: Context, packageName: String, quieted: Boolean) {
        val next = quietedApps(ctx).let { if (quieted) it + packageName else it - packageName }
        prefs(ctx).edit().putStringSet(KEY_QUIETED_APPS, next).apply()
    }

    /** True when [packageName] is quieted right now (quieted and the blocker isn't paused). */
    fun shouldQuietApp(ctx: Context, packageName: String): Boolean =
        !isPaused(ctx) && isAppQuieted(ctx, packageName)

    // ── Pause (PRO) ───────────────────────────────────────────────

    fun pausedUntil(ctx: Context): Long = prefs(ctx).getLong(KEY_PAUSED_UNTIL, 0L)

    fun isPaused(ctx: Context, now: Long = System.currentTimeMillis()): Boolean = pausedUntil(ctx) > now

    fun pause(ctx: Context, minutes: Int = PAUSE_MINUTES, now: Long = System.currentTimeMillis()) {
        prefs(ctx).edit().putLong(KEY_PAUSED_UNTIL, now + TimeUnit.MINUTES.toMillis(minutes.toLong())).apply()
    }

    fun resume(ctx: Context) {
        prefs(ctx).edit().remove(KEY_PAUSED_UNTIL).apply()
    }

    // ── Stats ─────────────────────────────────────────────────────

    fun stopsCount(ctx: Context): Int = prefs(ctx).getInt(KEY_STOPS, 0)

    fun recordStop(ctx: Context) {
        val p = prefs(ctx)
        p.edit().putInt(KEY_STOPS, p.getInt(KEY_STOPS, 0) + 1).apply()
    }

    fun minutesSaved(ctx: Context): Int = stopsCount(ctx) * MINUTES_SAVED_PER_STOP

    // ── Service gates ─────────────────────────────────────────────

    /** Fast pre-check so the accessibility service can bail before touching the node tree. */
    fun isAnyBlockEnabled(ctx: Context): Boolean = snapshot(ctx).isAnyBlockEnabled

    /**
     * Everything the accessibility service checks per event, read in one pass. The service
     * keeps one and re-reads it from its change listener instead of hitting prefs per event.
     */
    data class Snapshot(
        val pausedUntil: Long,
        val quietedApps: Set<String>,
        val blockedSurfaceKeys: Set<String>,
        val isDebugDumpEnabled: Boolean
    ) {
        val isAnyBlockEnabled: Boolean get() = blockedSurfaceKeys.isNotEmpty() || quietedApps.isNotEmpty()
        fun isPaused(now: Long): Boolean = pausedUntil > now
        fun isAppQuieted(packageName: String): Boolean = packageName in quietedApps
        fun isSurfaceBlocked(packageName: String, surfaceId: String): Boolean =
            surfaceKey(packageName, surfaceId) in blockedSurfaceKeys
    }

    fun snapshot(ctx: Context): Snapshot {
        val all = prefs(ctx).all
        return Snapshot(
            pausedUntil = all[KEY_PAUSED_UNTIL] as? Long ?: 0L,
            quietedApps = (all[KEY_QUIETED_APPS] as? Set<*>).orEmpty().filterIsInstance<String>().toSet(),
            blockedSurfaceKeys = all.filter { (k, v) -> k.startsWith(SURFACE_PREFIX) && v == true }.keys,
            isDebugDumpEnabled = all[KEY_DEBUG_DUMP] as? Boolean ?: false
        )
    }

    fun isDebugDumpEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_DEBUG_DUMP, false)

    fun setDebugDumpEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_DEBUG_DUMP, enabled).apply()
    }

    /** True for keys whose change alters which packages the service must watch. */
    fun affectsWatchedPackages(key: String?): Boolean = key == KEY_QUIETED_APPS
}
