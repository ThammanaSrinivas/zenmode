package com.zenlauncher.zenmode.coreapi

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.zenlauncher.zenmode.coreapi.analytics.AnalyticsManager

class UsageRepository(private val context: Context, private val analyticsManager: AnalyticsManager) {

    private val prefs: SharedPreferences = context.getSharedPreferences("zen_mode_stats", Context.MODE_PRIVATE)

    fun incrementUnlockCount() {
        val today = getTodayDate()
        val savedDateUnlocks = prefs.getString("last_date_unlocks", "")
        val editor = prefs.edit()
        
        var unlocks = 0
        if (savedDateUnlocks != today) {
            editor.putString("last_date_unlocks", today)
            editor.putInt("unlock_count", 1)
            unlocks = 1
        } else {
            val currentCount = prefs.getInt("unlock_count", 0)
            unlocks = currentCount + 1
            editor.putInt("unlock_count", unlocks)
        }
        editor.apply()
        
        analyticsManager.trackEvent("unlock_recorded", mapOf("count" to unlocks, "date" to today))
    }

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

    fun getTodayUsage(): DailyUsage {
        val today = getTodayDate()
        
        // Unlocks
        val savedDateUnlocks = prefs.getString("last_date_unlocks", "")
        val unlocks = if (savedDateUnlocks == today) prefs.getInt("unlock_count", 0) else 0

        // Screen Time
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

        return DailyUsage(unlocks, screenTimeInMillis)
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
        prefs.edit().remove("buddy_uid").remove("buddy_screen_time").remove("buddy_unlocks").apply()
    }

    fun saveBuddyStats(screenTime: Long, unlocks: Int) {
        prefs.edit()
            .putLong("buddy_screen_time", screenTime)
            .putInt("buddy_unlocks", unlocks)
            .apply()
    }

    fun getBuddyStats(): Pair<Long, Int> {
        val time = prefs.getLong("buddy_screen_time", 0)
        val unlocks = prefs.getInt("buddy_unlocks", 0)
        return Pair(time, unlocks)
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getLastSyncedStats(): Pair<Long, Int> {
        val time = prefs.getLong("last_synced_time", -1L)
        val unlocks = prefs.getInt("last_synced_unlocks", -1)
        return Pair(time, unlocks)
    }

    fun saveLastSyncedStats(screenTimeMins: Long, unlocks: Int) {
        prefs.edit()
            .putInt("last_synced_unlocks", unlocks)
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
}
