package com.zenlauncher.zenmode.island

import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.coreapi.week.MomentKind
import com.zenlauncher.zenmode.coreapi.week.WeekMoment
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Which notifications reach the island, and how. The island is in a launcher built against
 * doomscrolling, so it is choosy: it shows what is happening now (a call, a timer, music) and
 * what a person would want to see (a message), and keeps everything else out of the way.
 */
object NoteRules {

    /** The facts about one notification that the rules read, collected by [IslandNotes]. */
    data class Facts(
        val pkg: String,
        /** Posted by ZenMode OS itself. */
        val own: Boolean,
        val category: String?,
        val ongoing: Boolean,
        val groupSummary: Boolean,
        /** `NotificationManager.IMPORTANCE_*` of its channel. */
        val importance: Int,
        /** Do Not Disturb lets it through. */
        val passesDnd: Boolean,
        val chronometer: Boolean,
        /** Android 16's promoted ongoing notification (a Live Update). */
        val promoted: Boolean,
        val determinateProgress: Boolean,
        val mediaStyle: Boolean,
        val foregroundService: Boolean,
        /** An update to a notification that asked to alert only once. */
        val quietUpdate: Boolean,
        val hasWords: Boolean
    )

    data class Context(
        /** Settings: other apps' notifications may peek. */
        val peeksOn: Boolean,
        /** Settings: calls, timers and music may stay in the island. */
        val liveOn: Boolean,
        /** A Quiet session is running. */
        val quiet: Boolean,
        /** The app on screen now, if known. */
        val foregroundPkg: String?,
        /** Apps the person quieted in the blocker. */
        val quietedApps: Set<String>
    )

    enum class Route {
        IGNORE,
        CALL,
        LIVE,
        PEEK,
        /** Kept back for now (a Quiet session, or an app the person quieted), counted, not shown. */
        HOLD
    }

    private const val IMPORTANCE_DEFAULT = 3
    private const val IMPORTANCE_HIGH = 4

    /** Categories that never peek: they ring, show themselves, or are the phone talking to itself. */
    private val SILENT_CATEGORIES = setOf("alarm", "sys", "service", "status", "transport", "progress", "navigation")

    fun route(f: Facts, c: Context): Route = when {
        // Music comes from its media session, which carries the artwork and the controls.
        f.mediaStyle -> Route.IGNORE
        f.category == "call" && f.ongoing -> if (c.liveOn) Route.CALL else Route.IGNORE
        f.ongoing && (f.chronometer || f.promoted || f.determinateProgress || f.category == "navigation") ->
            if (c.liveOn && !f.own) Route.LIVE else Route.IGNORE
        f.ongoing || f.foregroundService -> Route.IGNORE
        !c.peeksOn -> Route.IGNORE
        f.groupSummary || f.quietUpdate || !f.hasWords -> Route.IGNORE
        f.importance < IMPORTANCE_DEFAULT || !f.passesDnd -> Route.IGNORE
        f.category in SILENT_CATEGORIES -> Route.IGNORE
        // Already in that app: it is telling you itself.
        f.pkg == c.foregroundPkg -> Route.IGNORE
        c.quiet || f.pkg in c.quietedApps -> Route.HOLD
        else -> Route.PEEK
    }

    /**
     * The system shows high-importance notifications as a heads-up banner by itself, so for those
     * the island only widens for a glance; for the rest, which would arrive unseen, it opens.
     */
    fun opens(importance: Int): Boolean = importance < IMPORTANCE_HIGH
}

/**
 * When the week itself is in the island, ported from iPhone's `IslandPlanner`: only on days that
 * mean something, never every day.
 *
 *  - a celebration: the 4th good day landed (or 7 of 7). One hour; it arrives with the card open.
 *  - a decisive day: one good day to go, or the streak on the line. From now until the night,
 *    at most eight hours.
 *  - a new week: Monday morning with a streak carried in, for two hours.
 *
 * One a day at most, nothing at night, and once swiped away it stays away until tomorrow.
 */
object MomentPlan {

    enum class Reason { CELEBRATION, DECISIVE_DAY, NEW_WEEK }

    val CELEBRATION_MILLIS: Long = TimeUnit.HOURS.toMillis(1)
    val NEW_WEEK_MILLIS: Long = TimeUnit.HOURS.toMillis(2)
    val DECISIVE_MAX_MILLIS: Long = TimeUnit.HOURS.toMillis(8)

    /** What happened with the moment today, kept in prefs by [IslandPrefs]. */
    data class Log(
        val startedDay: LocalDate? = null,
        val startedAt: Long = 0L,
        val reason: Reason? = null,
        val endsAt: Long = 0L,
        val dismissedDay: LocalDate? = null
    )

    data class Show(val reason: Reason, val endsAt: Long, val startedAt: Long, val arrives: Boolean)

    /** [nightStartsAt] is today's 21:30, when the night stage begins. */
    fun decide(m: WeekMoment, part: DayPart, now: Long, today: LocalDate, nightStartsAt: Long, log: Log): Show? {
        if (log.dismissedDay == today || part == DayPart.NIGHT) return null
        val startedToday = log.startedDay == today
        return when (m.kind) {
            MomentKind.JUST_WON, MomentKind.PERFECT_WEEK -> when {
                // A running celebration keeps its own hour; re-deciding never stretches it.
                startedToday && log.reason == Reason.CELEBRATION -> log.endsAt.takeIf { now < it }?.let { Show(Reason.CELEBRATION, it, log.startedAt, false) }
                else -> Show(Reason.CELEBRATION, minOf(now + CELEBRATION_MILLIS, nightStartsAt), now, true)
            }
            MomentKind.ONE_AWAY, MomentKind.ON_THE_LINE -> {
                if (startedToday && log.reason != Reason.DECISIVE_DAY) return null
                val start = if (startedToday) log.startedAt else now
                val ends = minOf(nightStartsAt, start + DECISIVE_MAX_MILLIS)
                if (now < ends) Show(Reason.DECISIVE_DAY, ends, start, false) else null
            }
            MomentKind.FRESH_WEEK -> {
                if (m.streakWeeks <= 0 || part != DayPart.MORNING) return null
                if (startedToday && log.reason != Reason.NEW_WEEK) return null
                val start = if (startedToday) log.startedAt else now
                val ends = start + NEW_WEEK_MILLIS
                if (now < ends) Show(Reason.NEW_WEEK, ends, start, false) else null
            }
            else -> null
        }
    }
}
