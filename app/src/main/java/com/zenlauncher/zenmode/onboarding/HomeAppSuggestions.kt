package com.zenlauncher.zenmode.onboarding

/**
 * Picks the home screen's starting apps so the "Pick your 8" step arrives already
 * filled — most users should only need to tap Continue.
 *
 * Order: essentials (phone, messages, camera, …) first, then the user's most-used
 * non-distracting apps, then anything else alphabetically. Distracting apps are never
 * suggested; the user can still add them by hand.
 */
object HomeAppSuggestions {

    const val HOME_APP_LIMIT = 8

    /** Package prefixes of the calm essentials, in the order they should be offered. */
    internal val ESSENTIAL_PREFIXES = listOf(
        "com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer",
        "com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging",
        "com.whatsapp",
        "com.google.android.GoogleCamera", "com.android.camera", "com.sec.android.app.camera",
        "com.google.android.apps.maps",
        "com.google.android.calendar", "com.samsung.android.calendar",
        "com.google.android.gm",
        "com.google.android.apps.photos",
        "com.google.android.deskclock", "com.sec.android.app.clockpackage",
        "com.google.android.keep",
        "com.phonepe.app", "net.one97.paytm", "com.google.android.apps.nbu.paisa.user",
        "com.android.chrome",
        "com.android.settings"
    )

    /**
     * [key] is the selection identity to pick (see LauncherActivities.selectionKey); it
     * defaults to [packageName] for callers that don't need to disambiguate two launcher
     * activities sharing a package (e.g. MIUI's Phone + Contacts).
     */
    data class Candidate(val packageName: String, val label: String, val key: String = packageName)

    fun suggest(
        installed: List<Candidate>,
        distracting: Set<String>,
        usageMinutes: Map<String, Long> = emptyMap(),
        limit: Int = HOME_APP_LIMIT
    ): List<String> {
        val calm = installed.filter { it.packageName !in distracting }
        val picked = LinkedHashSet<String>()

        for (prefix in ESSENTIAL_PREFIXES) {
            calm.firstOrNull { it.packageName.startsWith(prefix) }?.let { picked += it.key }
        }
        calm.filter { (usageMinutes[it.packageName] ?: 0L) > 0L }
            .sortedByDescending { usageMinutes[it.packageName] }
            .forEach { picked += it.key }
        calm.sortedBy { it.label.lowercase() }.forEach { picked += it.key }

        return picked.take(limit)
    }

    /** Tapping an app: remove it if picked, add it if there's room, otherwise ignore. */
    fun toggle(selected: List<String>, key: String, limit: Int = HOME_APP_LIMIT): List<String> =
        when {
            key in selected -> selected - key
            selected.size < limit -> selected + key
            else -> selected
        }
}
