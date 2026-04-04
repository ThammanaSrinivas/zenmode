package com.zenlauncher.zenmode.coreapi

import android.content.Context
import android.content.SharedPreferences
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
        
        // Try to get real-time stats first
        val realTimeFn = getRealTimeScreenTime()
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

    private fun getRealTimeScreenTime(): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager ?: return 0L
        
        // Check for permission - if not granted, queryEvents usually returns empty
        // We rely on the fact that existing logic handles fallback if this returns 0 or fails

        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(startTime, endTime)
        var totalTime = 0L
        var lastEventTime = 0L
        var isScreenOn = false

        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
             if (event.eventType == android.app.usage.UsageEvents.Event.SCREEN_INTERACTIVE) {
                 lastEventTime = event.timeStamp
                 isScreenOn = true
             } else if (event.eventType == android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
                 if (isScreenOn && lastEventTime > 0) {
                     totalTime += (event.timeStamp - lastEventTime)
                 }
                 isScreenOn = false
             }
        }
        
        // If still on, add time from last interactive to now
        if (isScreenOn && lastEventTime > 0) {
            totalTime += (System.currentTimeMillis() - lastEventTime)
        }
        
        return totalTime
    }

    private fun getScreenTimeForDay(dateString: String): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as? android.app.usage.UsageStatsManager ?: return 0L

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.parse(dateString) ?: return 0L

        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = calendar.timeInMillis

        val events = usageStatsManager.queryEvents(startTime, endTime)
        var totalTime = 0L
        var lastEventTime = 0L
        var isScreenOn = false

        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.SCREEN_INTERACTIVE) {
                lastEventTime = event.timeStamp
                isScreenOn = true
            } else if (event.eventType == android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
                if (isScreenOn && lastEventTime > 0) {
                    totalTime += (event.timeStamp - lastEventTime)
                }
                isScreenOn = false
            }
        }

        return totalTime
    }

    fun getYesterdayScreenTimeMillis(): Long {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayDate = dateFormat.format(yesterdayCal.time)

        val cacheKey = "screen_time_$yesterdayDate"
        return if (prefs.contains(cacheKey)) {
            prefs.getLong(cacheKey, 0L)
        } else {
            val queried = getScreenTimeForDay(yesterdayDate)
            prefs.edit().putLong(cacheKey, queried).apply()
            queried
        }
    }

    fun getWeeklyScreenTimeMillis(): List<Long> {
        val today = getTodayDate()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val result = mutableListOf<Long>()

        for (daysAgo in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dateString = dateFormat.format(cal.time)

            val millis: Long = if (dateString == today) {
                getTodayUsage().screenTimeInMillis
            } else {
                val cacheKey = "screen_time_$dateString"
                if (prefs.contains(cacheKey)) {
                    prefs.getLong(cacheKey, 0L)
                } else {
                    val queried = getScreenTimeForDay(dateString)
                    prefs.edit().putLong(cacheKey, queried).apply()
                    queried
                }
            }

            result.add(millis)
        }

        // Cleanup stale entries older than 7 days
        val cutoffCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
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

    fun getTodayUsage(): DailyUsage {
        val today = getTodayDate()

        val savedDateScreenTime = prefs.getString("last_date_screentime", "")
        var screenTimeInMillis = if (savedDateScreenTime == today) prefs.getLong("daily_screen_time", 0) else 0

        // Try to get real-time stats
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

        return DailyUsage(screenTimeInMillis)
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

    fun getOnboardingCurrentPage(): Int {
        return prefs.getInt("onboarding_current_page", 0)
    }

    fun setOnboardingCurrentPage(page: Int) {
        prefs.edit().putInt("onboarding_current_page", page).apply()
    }

    fun clearOnboardingCurrentPage() {
        prefs.edit().remove("onboarding_current_page").apply()
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

    // ── Pinned Apps ────────────────────────────────────────────────

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

    fun togglePinnedApp(packageName: String): Boolean {
        val current = getPinnedApps().toMutableList()
        return if (current.contains(packageName)) {
            current.remove(packageName)
            savePinnedApps(current)
            false
        } else {
            if (current.size >= MAX_PINNED_APPS) return false
            current.add(packageName)
            savePinnedApps(current)
            true
        }
    }

    companion object {
        const val MAX_PINNED_APPS = 4
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}
