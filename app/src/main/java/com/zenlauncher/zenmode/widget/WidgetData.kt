package com.zenlauncher.zenmode.widget

import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.coreapi.CircleMember
import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.coreapi.week.MomentCopy
import com.zenlauncher.zenmode.coreapi.week.MomentFeeling
import com.zenlauncher.zenmode.coreapi.week.MomentKind
import com.zenlauncher.zenmode.coreapi.week.WeeklyStreak
import com.zenlauncher.zenmode.quiet.QuietSession
import com.zenlauncher.zenmode.week.WeekReader
import com.zenlauncher.zenmode.widget.art.Surface
import com.zenlauncher.zenmode.widget.art.WidgetLight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The one snapshot every widget reads, so they always agree with each other and with Home.
 * Widgets compute nothing: [WidgetDataLoader] builds this from the app's own stores, and every
 * rule that decides how a widget looks ([surfaceFor], [mood], the circle's ranking) lives here,
 * pure, so it is unit-tested.
 */
data class WidgetData(
    val now: Long,
    val today: LocalDate,
    val part: DayPart,
    /** Usage access is on: without it nothing is measured and the widgets ask for it, once. */
    val usageAccess: Boolean,
    val week: WeekReader.State,
    val todayScore: Int?,
    val yesterdayScore: Int?,
    val minutesToday: Long,
    val promiseHours: Int,
    val quiet: QuietState,
    val gold: GoldState,
    val circle: CircleState,
    /** Screen time for the last seven days, oldest first, today last; null where nothing was recorded. */
    val lastSevenMinutes: List<Long?> = List(6) { null } + minutesToday,
    /** Zeno's pokes today, and whether he is answering the last one right now. */
    val pokes: Int = 0,
    val answering: Boolean = false
) {
    val moment get() = week.moment
    val promiseMinutes: Long get() = promiseHours * 60L
    val minutesLeft: Long get() = promiseMinutes - minutesToday
    val overPromise: Boolean get() = usageAccess && minutesToday > promiseMinutes

    /** Today, as the hill shows it. */
    val mood: WidgetLight.Mood
        get() = when {
            !usageAccess || todayScore == null -> WidgetLight.Mood.NONE
            overPromise -> WidgetLight.Mood.OVER
            todayScore >= WeeklyStreak.GOOD_DAY_ABOVE_TENTHS -> WidgetLight.Mood.CALM
            else -> WidgetLight.Mood.DRIFT
        }

    /** The line Zeno says now: the day's variant, or his answer to a poke. */
    val zenoLine: String
        get() = when {
            !usageAccess -> MomentCopy.USAGE_ACCESS_OFF
            answering -> MomentCopy.pokeReply(moment, pokes)
            lastHour -> MomentCopy.lastHour(minutesLeft)
            else -> MomentCopy.line(moment, MomentCopy.daySeed(today) + pokes)
        }

    val zenoFeeling: MomentFeeling get() = if (answering) moment.feeling.lifted else moment.feeling

    /** Under an hour of the promise left, and not over it: Zeno says so. */
    val lastHour: Boolean get() = usageAccess && minutesLeft in 1 until 60 && !moment.isWon

    /** The week, Monday first, as display strings for day cells ("7.4", "" for none). */
    val dayScores: List<String>
        get() = week.week.days.mapIndexed { i, s ->
            when {
                s != null -> MomentCopy.formatTenths(s)
                i == week.week.today && usageAccess -> "·"
                else -> ""
            }
        }

    /**
     * The surface a widget sits on. One rule for every widget that follows the week: a running
     * Quiet session is night, a perfect week is night, a won week is gold, dark theme is ink.
     * The moment widgets ([followsNight]) also take the night stage after 21:30, as on iPhone.
     */
    fun surfaceFor(dark: Boolean, followsQuiet: Boolean = true, followsWeek: Boolean = true, followsNight: Boolean = false): Surface = when {
        followsQuiet && quiet.running != null -> Surface.NIGHT
        followsWeek && moment.kind == MomentKind.PERFECT_WEEK -> Surface.NIGHT
        followsNight && part == DayPart.NIGHT -> Surface.NIGHT
        followsWeek && moment.isWon -> Surface.GOLD
        dark -> Surface.INK
        else -> Surface.PAPER
    }
}

data class QuietState(
    /** The accessibility service is on, so a session can quiet anything. */
    val available: Boolean,
    val running: QuietSession.Running?,
    val ended: QuietSession.Ended?,
    val minutes: Int = QuietSession.MINUTES
)

/**
 * Zen Gold's gate, read through the same function the Zen Gold screen uses
 * ([com.zenlauncher.zenmode.ZenGoldPromise.weekly]), so the widget can never say gold pay is
 * open while the app says it isn't: [daysToOpen] kept days of 7. If the gate's rule changes,
 * only the loader changes and every face follows.
 */
data class GoldState(
    val balanceLabel: String,
    val invested: Boolean,
    val open: Boolean,
    val kept: Int,
    val outOfReach: Boolean,
    val daysToOpen: Int = AppConstants.PROMISE_DAYS_TO_UNLOCK
) {
    val stillNeeded: Int get() = (daysToOpen - kept).coerceAtLeast(0)
}

/** The circle as the widget shows it. */
sealed class CircleState {
    data object SignedOut : CircleState()
    data object NoCircle : CircleState()
    /** A circle of one: the seats are waiting. */
    data class Alone(val name: String) : CircleState()
    data class Ranked(val name: String, val seats: List<CircleSeat>, val myRank: Int) : CircleState() {
        val size: Int get() = seats.size
        val me: CircleSeat? get() = seats.firstOrNull { it.isYou }
        val leading: Boolean get() = myRank == 1

        /** Tenths between you and the seat above you; null when you lead or have no score. */
        val behindBy: Int?
            get() {
                val mine = me?.scoreTenths ?: return null
                val above = seats.getOrNull(myRank - 2)?.scoreTenths ?: return null
                return (above - mine).takeIf { it > 0 }
            }
    }

    companion object {
        /**
         * Ranks a circle by today's Zen Score, highest first. A member whose score wasn't synced
         * today has none today (the daily reset rule), and sits below everyone who has one.
         */
        fun rank(name: String, members: List<CircleMember>, myUid: String?, today: LocalDate, zone: ZoneId): CircleState {
            if (members.size <= 1) return Alone(name)
            val seats = members.map { m ->
                val syncedToday = m.lastUpdatedEpochMs > 0 &&
                    Instant.ofEpochMilli(m.lastUpdatedEpochMs).atZone(zone).toLocalDate() == today
                CircleSeat(
                    name = m.displayName?.takeIf { it.isNotBlank() }?.substringBefore(' ') ?: "Friend",
                    scoreTenths = if (syncedToday) m.zenScore else null,
                    isYou = m.uid == myUid
                )
            }.sortedWith(compareByDescending<CircleSeat> { it.scoreTenths ?: -1 }.thenBy { !it.isYou })
            return Ranked(name, seats, seats.indexOfFirst { it.isYou }.let { if (it < 0) seats.size else it + 1 })
        }
    }
}

data class CircleSeat(val name: String, val scoreTenths: Int?, val isYou: Boolean) {
    val initial: String get() = name.firstOrNull()?.uppercase() ?: "·"
}
