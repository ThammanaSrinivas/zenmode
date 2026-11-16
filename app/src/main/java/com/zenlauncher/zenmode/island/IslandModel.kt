package com.zenlauncher.zenmode.island

import android.app.PendingIntent
import android.graphics.Bitmap
import com.zenlauncher.zenmode.coreapi.week.WeekMoment

/**
 * What the island can show. Two kinds of thing, as on iPhone:
 *
 *  - an [IslandItem] is live: it stays until it ends (a Quiet session, a call, music, a timer,
 *    the last hour of the promise, a day that matters for the week). The lowest [IslandItem.rank]
 *    takes the pill; the next sits beside it as a small bubble.
 *  - an [IslandPeek] passes: a notification, the charger going in, a Quiet session finishing.
 *    It takes the island for a few seconds, then the live things come back.
 *
 * Plain data, so [IslandStage] is a pure function and unit-tested.
 */
sealed interface IslandItem {
    val id: String
    val rank: Int
}

/** A notification action, drawn as a pill in the open card. */
data class IslandAction(val label: String, val intent: PendingIntent?)

data class QuietItem(val startedAt: Long, val endsAt: Long, val held: Int = 0) : IslandItem {
    override val id: String get() = ID
    override val rank: Int get() = 10
    val totalMillis: Long get() = (endsAt - startedAt).coerceAtLeast(1L)

    companion object {
        const val ID = "quiet"
    }
}

/** A call in progress, from any phone app. Nothing covers it. */
data class CallItem(
    val key: String,
    val name: String,
    val since: Long,
    val open: PendingIntent?,
    val actions: List<IslandAction>,
    /** Still ringing: the pill says so instead of counting. */
    val incoming: Boolean = false
) : IslandItem {
    override val id: String get() = "call:$key"
    override val rank: Int get() = 0
}

/** Another app's live activity: a timer, a stopwatch, a recording, a delivery, directions. */
data class LiveItem(
    val key: String,
    val pkg: String,
    val appLabel: String,
    val icon: Bitmap?,
    val title: String,
    val text: String,
    /** Wall-clock base of a running chronometer, or null. */
    val chronoBase: Long?,
    val countDown: Boolean,
    /** 0..1 for a determinate progress, or null. */
    val progress: Float?,
    val open: PendingIntent?,
    val actions: List<IslandAction>
) : IslandItem {
    override val id: String get() = "live:$key"
    override val rank: Int get() = 30
}

/** Music or a podcast playing in any app, read from its media session. */
data class MediaItem(
    val pkg: String,
    val appLabel: String,
    val title: String,
    val artist: String,
    val art: Bitmap?,
    /** The artwork's colour, lifted for the night, for the bars and the progress. */
    val tint: Int,
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    /** `SystemClock.elapsedRealtime()` when [positionMs] was true. */
    val positionAt: Long,
    val speed: Float
) : IslandItem {
    override val id: String get() = ID
    override val rank: Int get() = 40

    fun positionNow(elapsedRealtime: Long): Long {
        if (!playing) return positionMs
        val p = positionMs + ((elapsedRealtime - positionAt) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0L, durationMs) else p.coerceAtLeast(0L)
    }

    companion object {
        const val ID = "media"
    }
}

/** Under an hour of today's promise left. */
data class LastHourItem(val minutesLeft: Long, val promiseMinutes: Long, val minutesToday: Long) : IslandItem {
    override val id: String get() = ID
    override val rank: Int get() = 50

    companion object {
        const val ID = "last_hour"
    }
}

/** A day that matters for the week (iPhone's Live Activity): a won week, one to go, a new week. */
data class MomentItem(
    val moment: WeekMoment,
    val words: String,
    val eyebrow: String,
    val scoreTenths: Int?,
    val reason: MomentPlan.Reason,
    val endsAt: Long,
    val pokes: Int = 0
) : IslandItem {
    override val id: String get() = ID
    override val rank: Int get() = 60

    companion object {
        const val ID = "moment"
    }
}

sealed interface IslandPeek {
    val id: String
    /** The card opens by itself (otherwise the pill only widens). */
    val opens: Boolean
    /** How long it stays before the live things come back. */
    val millis: Long
}

/** A notification from any app (or from ZenMode OS itself, [own], drawn with Zeno). */
data class NotePeek(
    val key: String,
    val pkg: String,
    val appLabel: String,
    val icon: Bitmap?,
    val avatar: Bitmap?,
    val own: Boolean,
    val title: String,
    val text: String,
    val at: Long,
    val open: PendingIntent?,
    val actions: List<IslandAction>,
    override val opens: Boolean
) : IslandPeek {
    override val id: String get() = "note:$key"
    override val millis: Long get() = if (opens) 5_500L else 3_800L
}

data class ChargingPeek(val level: Int) : IslandPeek {
    override val id: String get() = "charging"
    override val opens: Boolean get() = false
    override val millis: Long get() = 2_800L
}

data class QuietDonePeek(val minutes: Int, val today: Int) : IslandPeek {
    override val id: String get() = "quiet_done"
    override val opens: Boolean get() = true
    override val millis: Long get() = 5_500L
}

data class PromisePeek(val hours: Int) : IslandPeek {
    override val id: String get() = "promise"
    override val opens: Boolean get() = true
    override val millis: Long get() = 6_000L
}

/**
 * What the island draws right now: [main] in the pill (or the card when [open]), [bubble]
 * beside it, and a [peek] in place of both while one passes.
 */
data class IslandScene(
    val main: IslandItem? = null,
    val bubble: IslandItem? = null,
    val peek: IslandPeek? = null,
    val open: Boolean = false,
    /** Hidden for the moment (the shade is down, a video is full screen, the phone is sideways). */
    val hidden: Boolean = false
) {
    val isEmpty: Boolean get() = hidden || (main == null && peek == null)

    /** The id of what the pill or card shows. */
    val shownId: String? get() = peek?.id ?: main?.id

    companion object {
        val EMPTY = IslandScene()
    }
}

object IslandStage {

    /**
     * Picks what shows. The lowest rank takes the pill, the next becomes the bubble. A peek takes
     * the island while it lasts, except over a call. [openId] opens the card on that thing; a
     * bubble that is opened swaps places with the pill.
     */
    fun scene(live: Collection<IslandItem>, peek: IslandPeek?, openId: String?, hidden: Boolean): IslandScene {
        val sorted = live.sortedBy { it.rank }
        val first = sorted.firstOrNull()
        val shownPeek = peek.takeIf { first !is CallItem }
        if (shownPeek != null) {
            return IslandScene(main = first, bubble = null, peek = shownPeek, open = openId == shownPeek.id, hidden = hidden)
        }
        val main = sorted.firstOrNull { it.id == openId } ?: first
        val bubble = sorted.firstOrNull { it !== main }
        return IslandScene(main = main, bubble = bubble, peek = null, open = main != null && openId == main.id, hidden = hidden)
    }
}
