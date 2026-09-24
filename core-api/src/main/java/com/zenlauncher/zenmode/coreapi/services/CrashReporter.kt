package com.zenlauncher.zenmode.coreapi.services

/**
 * Crash reporting for errors the app caught and survived (non-fatals). Uncaught crashes are
 * picked up by the backend SDK on its own; this is only for code that swallows a throwable on
 * purpose -- e.g. the accessibility service, where letting it propagate gets the service killed.
 *
 * Implementations must never throw: callers are catch blocks.
 */
interface CrashReporter {
    /** [keys] are attached to this one report only (e.g. "component" to "a11y"). */
    fun recordNonFatal(throwable: Throwable, keys: Map<String, String> = emptyMap())
}

/** Default until an [AppInitializer] installs a real one, so catch blocks never hit a lateinit. */
object NoOpCrashReporter : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, keys: Map<String, String>) {}
}
