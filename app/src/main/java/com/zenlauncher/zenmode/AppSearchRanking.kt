package com.zenlauncher.zenmode

/**
 * Orders app search results by how well they match, so the obvious app comes first.
 * Only the top few results show before "+N more", so a plain alphabetical "contains"
 * could bury "Phone" under "Apple Music" and "Google Play Store" for the query "p".
 */
object AppSearchRanking {

    /** Lower is better; null means no match. */
    fun rank(label: String, packageName: String, query: String): Int? {
        val q = query.trim()
        if (q.isEmpty()) return null
        return when {
            label.equals(q, ignoreCase = true) -> 0
            label.startsWith(q, ignoreCase = true) -> 1
            label.split(' ', '-', '_', '.').any { it.startsWith(q, ignoreCase = true) } -> 2
            label.contains(q, ignoreCase = true) -> 3
            // The package's own name catches what people call an app, e.g. "dialer" for Phone.
            packageName.substringAfterLast('.').contains(q, ignoreCase = true) -> 4
            else -> null
        }
    }

    /** Matches for [query], best first; ties keep [apps]' own order. */
    fun <T> filter(apps: List<T>, query: String, label: (T) -> String, packageName: (T) -> String): List<T> =
        apps.mapNotNull { app -> rank(label(app), packageName(app), query)?.let { app to it } }
            .sortedBy { it.second }
            .map { it.first }
}
