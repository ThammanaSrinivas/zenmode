package com.zenlauncher.zenmode.coreapi

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.zenlauncher.zenmode.coreapi.analytics.AnalyticsManager

class UsageRepository(private val context: Context, private val analyticsManager: AnalyticsManager) {

    private val prefs: SharedPreferences = context.getSharedPreferences("zen_mode_stats", Context.MODE_PRIVATE)

    fun updateScreenTime(duration: Long) {
        if (duration <= 0) return

        val today = getTodayDate()
        val savedDate = prefs.getString("last_date_screentime", "")

        // Archive previous day's screen time before resetting
        if (savedDate != null && savedDate.isNotEmpty() && savedDate != today) {
            val archiveKey = "screen_time_$savedDate"
            if (!prefs.contains(archiveKey)) {
                val previousDayTotal = prefs.getLong("daily_screen_time", 0L)
                if (previousDayTotal > 0) {
                    prefs.edit().putLong(archiveKey, previousDayTotal).apply()
                }
            }
        }

        // Try to get real-time stats first (only meaningful when usage access is granted)
        val realTimeFn = if (UsageAccess.isGranted(context)) getRealTimeScreenTime() else 0L
        val currentCachedParams = if (savedDate == today) prefs.getLong("daily_screen_time", 0) else 0

        var totalTime: Long
        if (realTimeFn > 0) {
            // System tracking is working and returning valid data.
            // Use this as the source of truth.
            totalTime = realTimeFn
        } else {
             // Fallback to manual accumulation
             totalTime = currentCachedParams + duration
        }

        // CRITICAL: Only update if the new total is greater than what we already have.
        // This prevents overwriting a high value with a low value (if system stats lag or error),
        // and ensures strictly increasing semantics.
        if (totalTime > currentCachedParams) {
            prefs.edit()
                .putLong("daily_screen_time", totalTime)
                .putString("last_date_screentime", today)
                .apply()
        }
    }

    private fun getRealTimeScreenTime(): Long =
        computeForegroundScreenTime(startOfTodayMillis(), System.currentTimeMillis())

    private fun getScreenTimeForDay(dateString: String): Long {
        val bounds = dayBoundsMillis(dateString) ?: return 0L
        return computeForegroundScreenTime(bounds.first, bounds.second)
    }

    /**
     * Total foreground app time in `[start, end)`.
     *
     * Primary signal: pairing per-app resume/pause events (reliable across OEMs). The previous
     * implementation summed SCREEN_INTERACTIVE/SCREEN_NON_INTERACTIVE durations, but MIUI/HyperOS
     * frequently withholds those screen on/off events from third-party apps, so the total stayed 0.
     *
     * Fallback: when event pairing yields 0 (events withheld), aggregate totalTimeInForeground from
     * queryUsageStats(INTERVAL_DAILY), which is exposed even when raw events are not.
     */
    private fun computeForegroundScreenTime(start: Long, end: Long): Long {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as? android.app.usage.UsageStatsManager ?: return 0L
        val fromEvents = sumForegroundFromEvents(usm, start, end)
        return if (fromEvents > 0L) fromEvents else sumForegroundFromStats(usm, start, end)
    }

    /**
     * ZenMode itself, the device's home/launcher package(s), and system UI/chooser surfaces —
     * these aren't "apps you used" and must never count toward screen time or the session log.
     * `SessionLogRepository` and `RecapCollector` reuse this rather than keeping their own copy.
     */
    fun excludedPackages(): Set<String> {
        val pm = context.packageManager
        val homes = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        ).map { it.activityInfo.packageName }
        return (homes + context.packageName + SYSTEM_PACKAGES).toSet()
    }

    private fun sumForegroundFromEvents(
        usm: android.app.usage.UsageStatsManager,
        start: Long,
        end: Long
    ): Long = foregroundSessions(usm, start, end).sumOf { it.endMillis - it.startMillis }

    /**
     * Every foreground app session in `[start, end)`, from paired resume/pause events.
     * Empty when usage access is missing or the OEM withholds raw events.
     */
    fun getForegroundSessions(start: Long, end: Long): List<ForegroundSession> {
        if (!UsageAccess.isGranted(context)) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as? android.app.usage.UsageStatsManager ?: return emptyList()
        return foregroundSessions(usm, start, end)
    }

    private fun foregroundSessions(
        usm: android.app.usage.UsageStatsManager,
        start: Long,
        end: Long
    ): List<ForegroundSession> {
        val excluded = excludedPackages()
        val events = usm.queryEvents(start, end)
        val event = android.app.usage.UsageEvents.Event()

        // Per-package resume timestamps so interleaved apps (split-screen, quick switches)
        // are not cross-attributed or double counted.
        val resumeAt = HashMap<String, Long>()
        val sessions = mutableListOf<ForegroundSession>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg in excluded) continue
            when (event.eventType) {
                // ACTIVITY_RESUMED (API 29+) and legacy MOVE_TO_FOREGROUND (==1)
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED,
                android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    // Overwrite any dangling resume (resume without a matching pause): drop the
                    // stale open session instead of counting it twice.
                    resumeAt[pkg] = event.timeStamp
                }
                // ACTIVITY_PAUSED (API 29+) and legacy MOVE_TO_BACKGROUND (==2)
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val started = resumeAt.remove(pkg)
                    // First event being a pause => started == null => ignored, because the matching
                    // resume happened before `start` (it belongs to the previous day).
                    if (started != null && event.timeStamp > started) {
                        sessions += ForegroundSession(pkg, started, event.timeStamp)
                    }
                }
            }
        }

        // Any app still in the foreground at `end`: close the open session(s) at `end`.
        for ((pkg, started) in resumeAt) {
            if (end > started) sessions += ForegroundSession(pkg, started, end)
        }
        return sessions
    }

    /** Unlocks (keyguard dismissals) in `[start, end)`. */
    fun getPickupCount(start: Long, end: Long): Int {
        if (!UsageAccess.isGranted(context)) return 0
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as? android.app.usage.UsageStatsManager ?: return 0
        val events = usm.queryEvents(start, end)
        val event = android.app.usage.UsageEvents.Event()
        var count = 0
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.KEYGUARD_HIDDEN) count++
        }
        return count
    }

    /**
     * Every "phone pickup" span in `[start, end)`: unlock (`KEYGUARD_HIDDEN`) to the next lock
     * (`KEYGUARD_SHOWN`), or to `end` if still unlocked. Same event stream [getPickupCount]
     * already trusts, just paired into spans instead of counted — the session-log building
     * block, since it's a much more direct "user picked up / put down the phone" signal than
     * inferring it from gaps between app-switch events.
     */
    fun getUnlockWindows(start: Long, end: Long): List<Pair<Long, Long>> {
        if (!UsageAccess.isGranted(context)) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as? android.app.usage.UsageStatsManager ?: return emptyList()
        val events = usm.queryEvents(start, end)
        val event = android.app.usage.UsageEvents.Event()
        val windows = mutableListOf<Pair<Long, Long>>()
        var unlockedAt: Long? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                android.app.usage.UsageEvents.Event.KEYGUARD_HIDDEN -> {
                    if (unlockedAt == null) unlockedAt = event.timeStamp
                }
                android.app.usage.UsageEvents.Event.KEYGUARD_SHOWN -> {
                    val opened = unlockedAt
                    if (opened != null && event.timeStamp > opened) windows += opened to event.timeStamp
                    unlockedAt = null
                }
            }
        }
        val opened = unlockedAt
        if (opened != null && end > opened) windows += opened to end
        return windows
    }

    /** Total screen time for `yyyy-MM-dd`, computed the same way as the home screen's. */
    fun getScreenTimeMillisForDate(dateString: String): Long = getScreenTimeForDay(dateString)

    private fun sumForegroundFromStats(
        usm: android.app.usage.UsageStatsManager,
        start: Long,
        end: Long
    ): Long {
        val excluded = excludedPackages()
        val stats = usm.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY, start, end
        ) ?: return 0L
        var total = 0L
        for (s in stats) {
            if (s.packageName in excluded) continue
            var t = s.totalTimeInForeground
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // totalTimeVisible covers PiP / visible-but-not-resumed; take the larger,
                // which better matches Digital Wellbeing on MIUI.
                t = maxOf(t, s.totalTimeVisible)
            }
            total += t
        }
        return total
    }

    private fun startOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun dayBoundsMillis(dateString: String): Pair<Long, Long>? {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString) ?: return null
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return startTime to calendar.timeInMillis
    }

    fun getYesterdayScreenTimeMillis(): Long {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayDate = dateFormat.format(yesterdayCal.time)

        val cacheKey = "screen_time_$yesterdayDate"
        val cached = prefs.getLong(cacheKey, 0L)
        if (cached > 0L) return cached

        val queried = getScreenTimeForDay(yesterdayDate)
        if (queried > 0L) {
            prefs.edit().putLong(cacheKey, queried).apply()
        }
        return queried
    }

    fun getWeeklyScreenTimeMillis(): List<Long> = getDailyScreenTimeMillis(7)

    /**
     * Screen time per day for the last [days] days, oldest first, today last.
     * Past days are cached; the cache keeps [CACHE_RETENTION_DAYS] so the Pro 30-day range
     * doesn't re-query UsageStats on every open. Call off the main thread for [days] > 7.
     */
    fun getDailyScreenTimeMillis(days: Int): List<Long> {
        require(days in 1..CACHE_RETENTION_DAYS) { "days must be in 1..$CACHE_RETENTION_DAYS" }
        val today = getTodayDate()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val result = mutableListOf<Long>()

        for (daysAgo in (days - 1) downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dateString = dateFormat.format(cal.time)

            val millis: Long = if (dateString == today) {
                getTodayUsage().screenTimeInMillis
            } else {
                val cacheKey = "screen_time_$dateString"
                val cached = prefs.getLong(cacheKey, 0L)
                if (cached > 0L) {
                    cached
                } else {
                    val queried = getScreenTimeForDay(dateString)
                    if (queried > 0L) {
                        prefs.edit().putLong(cacheKey, queried).apply()
                    }
                    queried
                }
            }

            result.add(millis)
        }

        // Cleanup stale entries older than the retention window
        val cutoffCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -CACHE_RETENTION_DAYS) }
        val cutoffDate = dateFormat.format(cutoffCal.time)
        val editor = prefs.edit()
        var hasRemovals = false
        for (key in prefs.all.keys) {
            if (key.startsWith("screen_time_") && key.length == 22) {
                val dateStr = key.removePrefix("screen_time_")
                if (dateStr < cutoffDate) {
                    editor.remove(key)
                    hasRemovals = true
                }
            }
        }
        if (hasRemovals) editor.apply()

        return result
    }

    fun getWeeklyScreenTimeHours(): List<Float> {
        return getWeeklyScreenTimeMillis().map { it / 3_600_000f }
    }

    fun getDailyScreenTimeHours(days: Int): List<Float> =
        getDailyScreenTimeMillis(days).map { it / 3_600_000f }

    fun getTodayUsage(): DailyUsage {
        val granted = UsageAccess.isGranted(context)
        val today = getTodayDate()

        val savedDateScreenTime = prefs.getString("last_date_screentime", "")
        var screenTimeInMillis = if (savedDateScreenTime == today) prefs.getLong("daily_screen_time", 0) else 0

        // Only read real-time stats when usage access is granted; otherwise queryEvents/queryUsageStats
        // return empty and we keep the last cached value (rather than overwriting with a misleading 0).
        if (granted) {
            try {
                val realTimeFn = getRealTimeScreenTime()
                // If we have valid real-time data that is MORE than our cached data, use it.
                // This handles the case where the user is actively using the device (so cache is stale/lower).
                if (realTimeFn > 0 && realTimeFn > screenTimeInMillis) {
                    screenTimeInMillis = realTimeFn

                    // Sync back to prefs so UI and other components see it
                    prefs.edit()
                        .putLong("daily_screen_time", screenTimeInMillis)
                        .putString("last_date_screentime", today)
                        .apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return DailyUsage(screenTimeInMillis, usagePermissionGranted = granted)
    }

    fun resetZenUnlockFlag() {
        prefs.edit().putBoolean("is_zen_unlocked", false).apply()
    }
    
    fun setZenUnlockFlag(unlocked: Boolean) {
        prefs.edit().putBoolean("is_zen_unlocked", unlocked).apply()
    }

    fun isZenUnlocked(): Boolean {
        return prefs.getBoolean("is_zen_unlocked", false)
    }

    fun isOnboardingComplete(): Boolean {
        return prefs.getBoolean("is_onboarding_complete", false)
    }

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean("is_onboarding_complete", complete).commit()
    }

    /**
     * The ZenMode OS (v3) onboarding. Separate from [isOnboardingComplete] so users who
     * finished the v2 onboarding still walk through the revamp once, as returning users.
     */
    fun isOsOnboardingComplete(): Boolean {
        return prefs.getBoolean("is_os_onboarding_complete", false)
    }

    fun setOsOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean("is_os_onboarding_complete", complete).commit()
    }

    /**
     * Set when onboarding finishes; the home screen plays "Entering ZenMode" once and
     * clears it. Lives on home because granting the home role relaunches it on top.
     */
    fun isEnteringCelebrationPending(): Boolean {
        return prefs.getBoolean("entering_celebration_pending", false)
    }

    fun setEnteringCelebrationPending(pending: Boolean) {
        prefs.edit().putBoolean("entering_celebration_pending", pending).apply()
    }

    /** Name of the onboarding step the user was on, so a re-created activity resumes there. */
    fun getOnboardingCurrentStep(): String? {
        return prefs.getString("onboarding_current_step", null)
    }

    fun setOnboardingCurrentStep(step: String) {
        prefs.edit().putString("onboarding_current_step", step).apply()
    }

    fun clearOnboardingCurrentStep() {
        prefs.edit().remove("onboarding_current_step").apply()
    }

    fun setOnboardingStartTime(timestamp: Long) {
        prefs.edit().putLong("onboarding_start_time", timestamp).apply()
    }

    fun getOnboardingStartTime(): Long {
        return prefs.getLong("onboarding_start_time", 0L)
    }

    fun recordPermissionGranted(permissionType: String) {
        val granted = getGrantedPermissionsList().toMutableSet()
        granted.add(permissionType)
        prefs.edit().putStringSet("permissions_granted_list", granted).apply()
    }

    fun getGrantedPermissionsList(): Set<String> {
        return prefs.getStringSet("permissions_granted_list", emptySet()) ?: emptySet()
    }

    fun getGrantedPermissionsCount(): Int {
        return getGrantedPermissionsList().size
    }

    fun clearOnboardingMetrics() {
        prefs.edit()
            .remove("onboarding_start_time")
            .remove("permissions_granted_list")
            .remove("onboarding_started_tracked")
            .apply()
    }

    fun setOnboardingStartedTracked(tracked: Boolean) {
        prefs.edit().putBoolean("onboarding_started_tracked", tracked).apply()
    }

    fun isOnboardingStartedTracked(): Boolean {
        return prefs.getBoolean("onboarding_started_tracked", false)
    }

    fun isFirstRun(): Boolean {
        return prefs.getBoolean("is_first_run", true)
    }

    fun setFirstRunComplete() {
        prefs.edit().putBoolean("is_first_run", false).apply()
    }

    fun saveUserUid(uid: String) {
        prefs.edit().putString("user_uid", uid).apply()
    }

    fun getUserUid(): String? {
        return prefs.getString("user_uid", null)
    }

    fun saveBuddyUid(uid: String) {
        prefs.edit().putString("buddy_uid", uid).apply()
    }

    fun getBuddyUid(): String? {
        return prefs.getString("buddy_uid", null)
    }

    fun hasCachedBuddy(): Boolean {
        return prefs.contains("buddy_uid")
    }

    fun clearCachedBuddy() {
        prefs.edit()
            .remove("buddy_uid")
            .remove("buddy_screen_time")
            .remove("has_buddy_cached")
            .remove("buddy_connection_date")
            .apply()
    }

    fun saveBuddyConnectionDate(epochMillis: Long) {
        prefs.edit().putLong("buddy_connection_date", epochMillis).apply()
    }

    fun getBuddyConnectionDate(): Long? {
        return if (prefs.contains("buddy_connection_date")) prefs.getLong("buddy_connection_date", 0L)
        else null
    }

    /** Returns null if not yet cached, true/false if previously set by StatSyncWorker. */
    fun getCachedHasBuddy(): Boolean? {
        return if (prefs.contains("has_buddy_cached")) prefs.getBoolean("has_buddy_cached", false)
        else null
    }

    fun saveHasBuddy(value: Boolean) {
        prefs.edit().putBoolean("has_buddy_cached", value).apply()
    }

    fun getLastRandomConnectAttemptTime(): Long {
        return prefs.getLong("random_connect_last_tried", 0L)
    }

    fun saveLastRandomConnectAttemptTime(time: Long) {
        prefs.edit().putLong("random_connect_last_tried", time).apply()
    }

    fun saveBuddyScreenTime(screenTime: Long) {
        prefs.edit()
            .putLong("buddy_screen_time", screenTime)
            .apply()
    }

    fun getBuddyScreenTime(): Long {
        return prefs.getLong("buddy_screen_time", 0)
    }

    // ── Zen Circle cache ──────────────────────────────────────────────
    // Same offline-first pattern as the buddy cache above, reshaped for up to
    // ZEN_CIRCLE_MAX_MEMBERS members: one JSON blob (org.json — no new serialization
    // library, matching the Gold Streak precedent) instead of N scalar keys per member.

    fun cacheCircle(circle: Circle) {
        val membersJson = org.json.JSONArray()
        circle.members.forEach { m ->
            membersJson.put(
                org.json.JSONObject()
                    .put("uid", m.uid)
                    .put("displayName", m.displayName ?: org.json.JSONObject.NULL)
                    .put("zenScore", m.zenScore)
                    .put("lastUpdatedEpochMs", m.lastUpdatedEpochMs)
                    .put("role", m.role.name)
                    .put("joinedAtEpochMs", m.joinedAtEpochMs)
            )
        }
        val json = org.json.JSONObject()
            .put("id", circle.id)
            .put("name", circle.name)
            .put("leaderUid", circle.leaderUid)
            .put("createdAtEpochMs", circle.createdAtEpochMs)
            .put("members", membersJson)
        prefs.edit().putString("circle_cache_json", json.toString()).apply()
    }

    fun getCachedCircle(): Circle? {
        val raw = prefs.getString("circle_cache_json", null) ?: return null
        return try {
            val json = org.json.JSONObject(raw)
            val membersJson = json.getJSONArray("members")
            val members = (0 until membersJson.length()).map { i ->
                val m = membersJson.getJSONObject(i)
                CircleMember(
                    uid = m.getString("uid"),
                    displayName = if (m.isNull("displayName")) null else m.getString("displayName"),
                    zenScore = m.getInt("zenScore"),
                    lastUpdatedEpochMs = m.getLong("lastUpdatedEpochMs"),
                    role = CircleRole.valueOf(m.getString("role")),
                    joinedAtEpochMs = m.getLong("joinedAtEpochMs")
                )
            }
            Circle(
                id = json.getString("id"),
                name = json.getString("name"),
                leaderUid = json.getString("leaderUid"),
                members = members,
                createdAtEpochMs = json.getLong("createdAtEpochMs")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearCachedCircle() {
        prefs.edit().remove("circle_cache_json").remove("circle_id_cached").apply()
    }

    /** Returns null if not yet cached, a circleId/"" (no circle) if previously set by StatSyncWorker's self-heal. */
    fun getCachedCircleId(): String? {
        return if (prefs.contains("circle_id_cached")) prefs.getString("circle_id_cached", null) else null
    }

    fun saveCircleId(circleId: String?) {
        prefs.edit().putString("circle_id_cached", circleId ?: "").apply()
    }

    // ── Circle reaction rate limit, per sender->target pair ─────────────
    // Generalizes the buddy-like limiter above (which only ever had one possible
    // target) — same window/count shape (see AppConstants doc comment), keyed by target
    // since Circle has up to ZEN_CIRCLE_MAX_MEMBERS - 1 possible recipients.

    fun getRecentReactionTimestamps(targetUid: String): List<Long> {
        val raw = prefs.getString("recent_reaction_timestamps_$targetUid", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        return raw.split(",")
            .mapNotNull { it.toLongOrNull() }
            .filter { now - it < CIRCLE_REACTION_WINDOW_MS }
            .sorted()
    }

    fun recordReactionSent(targetUid: String) {
        val pruned = (getRecentReactionTimestamps(targetUid) + System.currentTimeMillis()).joinToString(",")
        prefs.edit().putString("recent_reaction_timestamps_$targetUid", pruned).apply()
    }

    fun removeLastReactionTimestamp(targetUid: String) {
        val current = getRecentReactionTimestamps(targetUid).toMutableList()
        if (current.isEmpty()) return
        current.removeAt(current.size - 1)
        prefs.edit().putString("recent_reaction_timestamps_$targetUid", current.joinToString(",")).apply()
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getLastSyncedScreenTime(): Long {
        return prefs.getLong("last_synced_time", -1L)
    }

    fun saveLastSyncedScreenTime(screenTimeMins: Long) {
        prefs.edit()
            .putLong("last_synced_time", screenTimeMins)
            .apply()
    }

    /**
     * The zen_score last written to Firestore. Unlike screen time, the synced score also
     * depends on the promise and today's session quality — comparing the computed score
     * itself (rather than its inputs) is what catches a promise edit or a session-quality
     * shift alone needing a resync.
     */
    fun getLastSyncedZenScore(): Int {
        return prefs.getInt("last_synced_zen_score", -1)
    }

    fun saveLastSyncedZenScore(score: Int) {
        prefs.edit()
            .putInt("last_synced_zen_score", score)
            .apply()
    }

    fun getLastDailyTrackedDate(): String {
        return prefs.getString("last_daily_tracked_date", "") ?: ""
    }

    fun setLastDailyTrackedDate(date: String) {
        prefs.edit().putString("last_daily_tracked_date", date).apply()
    }

    fun getLastWeeklyTrackedDate(): String {
        return prefs.getString("last_weekly_tracked_date", "") ?: ""
    }

    fun setLastWeeklyTrackedDate(date: String) {
        prefs.edit().putString("last_weekly_tracked_date", date).apply()
    }

    fun getLastStatsProcessedTime(): Long {
        return prefs.getLong("last_stats_processed_timestamp", 0L)
    }

    fun updateLastStatsProcessedTime(time: Long) {
        prefs.edit().putLong("last_stats_processed_timestamp", time).apply()
    }

    fun getTodaySkipCount(): Int {
        val savedDate = prefs.getString("last_date_skips", "")
        return if (savedDate == getTodayDate()) prefs.getInt("daily_skip_count", 0) else 0
    }

    fun incrementSkipCount() {
        val today = getTodayDate()
        val savedDate = prefs.getString("last_date_skips", "")
        val current = if (savedDate == today) prefs.getInt("daily_skip_count", 0) else 0
        prefs.edit()
            .putString("last_date_skips", today)
            .putInt("daily_skip_count", current + 1)
            .apply()
    }

    fun isPostHogIdentified(): Boolean {
        return prefs.getBoolean("posthog_identified", false)
    }

    fun setPostHogIdentified(identified: Boolean) {
        prefs.edit().putBoolean("posthog_identified", identified).apply()
    }

    fun clearUserData() {
        prefs.edit()
            .remove("user_uid")
            .remove("buddy_uid")
            .remove("buddy_screen_time")
            .remove("has_buddy_cached")
            .remove("posthog_identified")
            .apply()
    }

    fun snoozeForceUpdate() {
        prefs.edit().putString("force_update_snoozed_date", getTodayDate()).apply()
    }

    fun isForceUpdateSnoozed(): Boolean {
        val snoozedDate = prefs.getString("force_update_snoozed_date", null) ?: return false
        return snoozedDate == getTodayDate()
    }

    // ── Home Apps ──────────────────────────────────────────────────

    /** The apps picked for the home screen, in order. Stored under the v2 "pinned_apps" key so old pins carry over. */
    fun getPinnedApps(): List<String> {
        val json = prefs.getString("pinned_apps", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePinnedApps(packageNames: List<String>) {
        val array = JSONArray(packageNames)
        prefs.edit().putString("pinned_apps", array.toString()).apply()
    }

    // ── React Rate Limit ──────────────────────────────────────────────

    /** Returns like-send timestamps that still fall within the rate-limit window, oldest first. */
    fun getRecentLikeTimestamps(): List<Long> {
        val raw = prefs.getString(KEY_RECENT_LIKE_TIMESTAMPS, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        return raw.split(",")
            .mapNotNull { it.toLongOrNull() }
            .filter { now - it < LIKE_WINDOW_MS }
            .sorted()
    }

    /** Records a like send at `now` and prunes anything outside the window. */
    fun recordLikeSent() {
        val pruned = (getRecentLikeTimestamps() + System.currentTimeMillis()).joinToString(",")
        prefs.edit().putString(KEY_RECENT_LIKE_TIMESTAMPS, pruned).apply()
    }

    /** Removes the most recent like timestamp. Used to roll back after a failed send. */
    fun removeLastLikeTimestamp() {
        val current = getRecentLikeTimestamps().toMutableList()
        if (current.isEmpty()) return
        current.removeAt(current.size - 1)
        prefs.edit().putString(KEY_RECENT_LIKE_TIMESTAMPS, current.joinToString(",")).apply()
    }

    fun getSessionNumber(): Int = prefs.getInt("session_number", 0)

    fun incrementSessionNumber(): Int {
        val next = getSessionNumber() + 1
        prefs.edit().putInt("session_number", next).apply()
        return next
    }

    fun isDay1CheckinTracked(): Boolean = prefs.getBoolean("day1_checkin_tracked", false)
    
    fun setDay1CheckinTracked(tracked: Boolean) {
        prefs.edit().putBoolean("day1_checkin_tracked", tracked).apply()
    }

    companion object {
        /** Longest range any screen asks for (the Pro 30-day chart). */
        const val CACHE_RETENTION_DAYS = 30

        /**
         * System UI/chooser surfaces that show up as foreground "apps" in UsageEvents but
         * aren't something the user opened: `android` owns the legacy share/open-with
         * ResolverActivity and the keyguard; `com.android.intentresolver` is the same chooser
         * UI split into its own mainline module on Android 13+.
         */
        private val SYSTEM_PACKAGES = setOf("com.android.systemui", "android", "com.android.intentresolver")

        private const val KEY_RECENT_LIKE_TIMESTAMPS = "recent_like_timestamps"
        const val LIKE_WINDOW_MS: Long = 20L * 60_000L  // 20 minutes
        const val LIKE_MAX_COUNT: Int = 4

        // Circle reaction rate limit — same shape as the buddy-like limiter above,
        // per sender->target pair rather than a single fixed target.
        const val CIRCLE_REACTION_WINDOW_MS: Long = 20L * 60_000L  // 20 minutes
        const val CIRCLE_REACTION_MAX_COUNT: Int = 4
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}
