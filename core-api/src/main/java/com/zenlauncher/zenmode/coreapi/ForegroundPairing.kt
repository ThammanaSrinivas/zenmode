package com.zenlauncher.zenmode.coreapi

/**
 * Pairs raw resume/pause events into per-app foreground sessions: the one counting rule behind
 * Home's screen time, the session log and the weekly recap (all via
 * [UsageRepository.getForegroundSessions]). Pure maths, no Android calls, so it's unit-tested.
 */
object ForegroundPairing {

    enum class Kind { RESUMED, PAUSED }

    data class Event(val timeMillis: Long, val packageName: String, val kind: Kind)

    /** [sawActivity] is false when the OEM sent no activity events at all (not even ours). */
    class Result(val sessions: List<ForegroundSession>, val sawActivity: Boolean)

    /**
     * Pop-up screens that other apps borrow: Google's account picker and sign-in sheets (Play
     * services), the "Allow …?" permission prompts, the app installer. They're on screen, so the
     * time counts, but it's credited to the app they popped up over. Nobody "used Google Play
     * services": a sign-in sheet over LinkedIn is LinkedIn time, and one over ZenMode's own
     * sign-in isn't screen time at all, same as ZenMode itself.
     */
    val HELPER_PACKAGES = setOf(
        "com.google.android.gms",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )

    /**
     * @param events in time order, as UsageStatsManager.queryEvents returns them.
     * @param end where a session still open at the end of the events is closed.
     * @param excluded packages that are never screen time (ZenMode, the launcher, system UI).
     */
    fun pair(
        events: Iterable<Event>,
        end: Long,
        excluded: Set<String>,
        helpers: Set<String> = HELPER_PACKAGES
    ): Result {
        // Per-package resume timestamps so interleaved apps (split-screen, quick switches)
        // are not cross-attributed or double counted.
        val resumeAt = HashMap<String, Long>()
        // For an open helper screen, the app it popped up over (null: nothing known before it).
        val hostOf = HashMap<String, String?>()
        var lastHost: String? = null
        var sawActivity = false
        val sessions = mutableListOf<ForegroundSession>()

        fun close(pkg: String, started: Long, until: Long) {
            val credited = if (pkg in helpers) hostOf.remove(pkg) else pkg
            if (credited != null && credited !in excluded && until > started) {
                sessions += ForegroundSession(credited, started, until)
            }
        }

        for (event in events) {
            val pkg = event.packageName
            when (event.kind) {
                Kind.RESUMED -> {
                    sawActivity = true
                    if (pkg in helpers) {
                        hostOf[pkg] = lastHost
                    } else {
                        lastHost = pkg
                        if (pkg in excluded) continue
                    }
                    // Overwrite any dangling resume (resume without a matching pause): drop the
                    // stale open session instead of counting it twice.
                    resumeAt[pkg] = event.timeMillis
                }
                Kind.PAUSED -> {
                    // A pause with no resume => the resume happened before the range started
                    // (it belongs to the previous day), so it's ignored.
                    val started = resumeAt.remove(pkg) ?: continue
                    close(pkg, started, event.timeMillis)
                }
            }
        }

        // Anything still in the foreground at `end`: close the open session(s) there.
        for ((pkg, started) in resumeAt.toList()) close(pkg, started, end)
        return Result(sessions, sawActivity)
    }
}
