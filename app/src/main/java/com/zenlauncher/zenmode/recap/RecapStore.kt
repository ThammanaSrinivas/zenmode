package com.zenlauncher.zenmode.recap

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * On-device history behind every recap: one [DayRecord] per finished day, kept for
 * [RETENTION_DAYS]. Weeks are rebuilt from days, so the recap, Settings' report list and
 * the PDF all read the same numbers. Also remembers which weeks were announced and seen.
 */
class RecapStore(context: Context) {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val lock = Any()

    fun days(): Map<LocalDate, DayRecord> = synchronized(lock) { read().days }

    fun hasDay(date: LocalDate): Boolean = date in days()

    fun putDays(records: List<DayRecord>) = synchronized(lock) {
        if (records.isEmpty()) return@synchronized
        val state = read()
        val cutoff = LocalDate.now().minusDays(RETENTION_DAYS)
        val days = (state.days + records.associateBy { it.date }).filterKeys { it >= cutoff }
        write(state.copy(days = days))
    }

    /** Finished weeks with a full seven days of data, newest first. */
    fun completedWeeks(today: LocalDate = LocalDate.now()): List<WeeklyRecap> {
        val days = days()
        val currentWeek = weekStartOf(today)
        return days.keys.map(::weekStartOf).distinct()
            .filter { it < currentWeek }
            .mapNotNull { week -> recapFor(week, days) }
            .sortedByDescending { it.weekStart }
    }

    fun recap(weekStart: LocalDate): WeeklyRecap? = recapFor(weekStart, days())

    private fun recapFor(weekStart: LocalDate, days: Map<LocalDate, DayRecord>): WeeklyRecap? {
        val week = (0L..6L).mapNotNull { days[weekStart.plusDays(it)] }
        if (week.size < 7) return null
        val previous = (0L..6L).mapNotNull { days[weekStart.minusDays(7 - it)] }
        return WeeklyRecap(
            weekStart = weekStart,
            days = week,
            previousWeekTotalMinutes = previous.takeIf { it.size == 7 }?.sumOf { it.screenTimeMinutes }
        )
    }

    fun isAnnounced(weekStart: LocalDate) = weekStart in synchronized(lock) { read().announced }
    fun markAnnounced(weekStart: LocalDate) = synchronized(lock) {
        val state = read()
        write(state.copy(announced = state.announced + weekStart))
    }

    fun isSeen(weekStart: LocalDate) = weekStart in synchronized(lock) { read().seen }
    fun markSeen(weekStart: LocalDate) = synchronized(lock) {
        val state = read()
        write(state.copy(seen = state.seen + weekStart))
    }

    /** Newest announced week the user hasn't opened yet. */
    fun unseenWeek(): WeeklyRecap? {
        val state = synchronized(lock) { read() }
        return completedWeeks().firstOrNull { it.weekStart in state.announced && it.weekStart !in state.seen }
    }

    // ── Persistence ──────────────────────────────────────────────

    private data class State(
        val days: Map<LocalDate, DayRecord> = emptyMap(),
        val announced: Set<LocalDate> = emptySet(),
        val seen: Set<LocalDate> = emptySet()
    )

    private fun read(): State {
        if (!file.exists()) return State()
        return try {
            val root = JSONObject(file.readText())
            State(
                days = root.optJSONArray("days")?.let { arr ->
                    (0 until arr.length()).map { dayFromJson(arr.getJSONObject(it)) }.associateBy { it.date }
                } ?: emptyMap(),
                announced = root.optJSONArray("announced").toDates(),
                seen = root.optJSONArray("seen").toDates()
            )
        } catch (e: Exception) {
            // A corrupt file must never take the launcher down; start fresh.
            Log.w(TAG, "Recap store unreadable, resetting", e)
            State()
        }
    }

    private fun write(state: State) {
        val cutoff = LocalDate.now().minusDays(RETENTION_DAYS)
        val root = JSONObject()
            .put("version", 1)
            .put("days", JSONArray(state.days.values.sortedBy { it.date }.map(::dayToJson)))
            .put("announced", JSONArray(state.announced.filter { it >= cutoff }.map { it.toString() }))
            .put("seen", JSONArray(state.seen.filter { it >= cutoff }.map { it.toString() }))
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(root.toString())
        if (!tmp.renameTo(file)) {
            file.writeText(root.toString())
            tmp.delete()
        }
    }

    private fun dayToJson(day: DayRecord) = JSONObject()
        .put("date", day.date.toString())
        .put("minutes", day.screenTimeMinutes)
        .put("promise_hours", day.promiseHours)
        .put("late_night_minutes", day.lateNightMinutes)
        .put("pickups", day.pickups ?: JSONObject.NULL)
        .put("apps", JSONArray(day.appMinutes.map {
            JSONObject().put("pkg", it.packageName).put("label", it.label).put("minutes", it.minutes)
        }))

    private fun dayFromJson(json: JSONObject): DayRecord {
        val apps = json.optJSONArray("apps")
        return DayRecord(
            date = LocalDate.parse(json.getString("date")),
            screenTimeMinutes = json.getLong("minutes"),
            promiseHours = json.getInt("promise_hours"),
            appMinutes = (0 until (apps?.length() ?: 0)).map {
                val app = apps!!.getJSONObject(it)
                AppMinutes(app.getString("pkg"), app.getString("label"), app.getLong("minutes"))
            },
            lateNightMinutes = json.optLong("late_night_minutes", 0),
            pickups = if (json.isNull("pickups")) null else json.getInt("pickups")
        )
    }

    private fun JSONArray?.toDates(): Set<LocalDate> =
        if (this == null) emptySet() else (0 until length()).map { LocalDate.parse(getString(it)) }.toSet()

    private companion object {
        const val TAG = "ZenRecap"
        const val FILE_NAME = "weekly_recap_history.json"
        const val RETENTION_DAYS = 16L * 7
    }
}
