package com.zenlauncher.zenmode

/**
 * Reads the colon-separated component lists Android keeps in `Settings.Secure`
 * (`enabled_accessibility_services`, `enabled_notification_listeners`).
 *
 * The same component can be stored as `pkg/pkg.Cls` or `pkg/.Cls`: the Settings app writes
 * the full form when the user toggles a service on, but system_server rewrites the whole
 * list in the short form whenever it persists it (e.g. another accessibility app is
 * force-stopped or uninstalled). A plain string match flips to "not enabled" at that point
 * while the service is still running — which users saw as the permission resetting.
 * So entries are parsed and compared as package + class, never as raw strings.
 */
object EnabledComponents {

    fun contains(setting: String?, packageName: String, className: String): Boolean =
        setting.orEmpty().split(':').any { entry ->
            val slash = entry.indexOf('/')
            if (slash <= 0) return@any false
            val pkg = entry.substring(0, slash).trim()
            val cls = entry.substring(slash + 1).trim().let { if (it.startsWith('.')) pkg + it else it }
            pkg == packageName && cls == className
        }
}
