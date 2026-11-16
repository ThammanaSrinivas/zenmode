package com.zenlauncher.zenmode

import android.content.Context
import com.zenlauncher.zenmode.coreapi.zenPrefs
import com.zenlauncher.zenmode.onboarding.HomeAppSuggestions

/**
 * The apps opened from ZenMode OS lately, newest first, so search can offer a few before the
 * first keystroke. Only launches made through the launcher count (home tiles and search);
 * nothing reads usage stats for this, so it works without usage access.
 *
 * The suggestions stay calm on purpose: the search spec says the empty state must not be a
 * menu of temptations, so distracting and quieted apps never appear here, however recent.
 */
object RecentApps {

    private const val KEY_RECENT = "recent_app_keys"
    private const val SEPARATOR = "\n"

    /** How many launches are remembered: enough to still fill four slots after filtering. */
    const val HISTORY_LIMIT = 12

    /** How many suggestions search shows before typing (spec: "four most-recent apps"). */
    const val SUGGESTION_LIMIT = 4

    fun record(context: Context, key: String) {
        val prefs = context.zenPrefs()
        val updated = push(read(prefs.getString(KEY_RECENT, null)), key)
        prefs.edit().putString(KEY_RECENT, updated.joinToString(SEPARATOR)).apply()
    }

    fun keys(context: Context): List<String> = read(context.zenPrefs().getString(KEY_RECENT, null))

    private fun read(raw: String?): List<String> =
        raw?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    /** [key] moves to the front; older entries keep their order; the list stays bounded. */
    fun push(recent: List<String>, key: String, limit: Int = HISTORY_LIMIT): List<String> =
        (listOf(key) + recent.filter { it != key }).take(limit)

    /**
     * Up to [limit] apps for the empty search: recent calm apps first, newest first. A new
     * install has no history yet, so the gap is filled with the calm essentials (phone,
     * messages, camera…) rather than leaving the space blank. Apps that are no longer
     * installed simply drop out.
     */
    fun <T> suggest(
        recentKeys: List<String>,
        apps: List<T>,
        key: (T) -> String,
        packageName: (T) -> String,
        isCalm: (T) -> Boolean,
        limit: Int = SUGGESTION_LIMIT
    ): List<T> {
        val calm = apps.filter(isCalm)
        val byKey = calm.associateBy(key)
        val picked = LinkedHashMap<String, T>()

        for (k in recentKeys) {
            if (picked.size >= limit) break
            byKey[k]?.let { picked[k] = it }
        }
        for (prefix in HomeAppSuggestions.ESSENTIAL_PREFIXES) {
            if (picked.size >= limit) break
            calm.firstOrNull { packageName(it).startsWith(prefix) && key(it) !in picked }
                ?.let { picked[key(it)] = it }
        }
        return picked.values.toList()
    }
}
