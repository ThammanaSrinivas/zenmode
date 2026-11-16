package com.zenlauncher.zenmode.island

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.zenlauncher.zenmode.ContentBlockPrefs
import com.zenlauncher.zenmode.ZenNotificationListenerService
import com.zenlauncher.zenmode.coreapi.week.MomentCopy
import com.zenlauncher.zenmode.widget.WidgetData
import com.zenlauncher.zenmode.widget.WidgetDataLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.ZoneId

/**
 * Everything that feeds the island while it runs, besides notifications: ZenMode OS's own
 * moments (a Quiet session, the last hour of the promise, the week), the charger, and music.
 *
 * ZenMode's moments come from the same snapshot the widgets read ([WidgetDataLoader]), so the
 * island never says something a widget contradicts. It is read once a minute while the screen is
 * on, at once when the screen comes on, and whenever a Quiet session starts or ends.
 */
class IslandSources(private val app: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val media = IslandMedia(app)
    private var started = false

    private val tick = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, TICK_MILLIS)
        }
    }

    /** Look again when the running Quiet session or the week's moment is due to end. */
    private val atQuietEnd = Runnable { refresh() }
    private val atMomentEnd = Runnable { refresh() }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> if (interactive()) charging()
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    refresh()
                    handler.removeCallbacks(tick)
                    handler.postDelayed(tick, TICK_MILLIS)
                }
                Intent.ACTION_SCREEN_OFF -> handler.removeCallbacks(tick)
            }
        }
    }

    // Held as a field: SharedPreferences only keeps listeners weakly.
    private val quietListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == ContentBlockPrefs.KEY_QUIET_ENDS_AT) {
            IslandStore.resetHeld()
            // The shared snapshot is cached for a few seconds; a session that just started or
            // ended must show now, not at the next minute.
            WidgetDataLoader.invalidate()
            refresh()
        }
    }

    fun start() {
        if (started) return
        started = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContentBlockPrefs.prefs(app).registerOnSharedPreferenceChangeListener(quietListener)
        if (ZenNotificationListenerService.isEnabledInSettings(app)) media.start()
        handler.post(tick)
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { app.unregisterReceiver(receiver) }
        ContentBlockPrefs.prefs(app).unregisterOnSharedPreferenceChangeListener(quietListener)
        handler.removeCallbacksAndMessages(null)
        media.stop()
        scope.cancel()
    }

    /** Reads ZenMode's moments again now. */
    fun refresh() {
        if (!started) return
        scope.launch {
            val data = runCatching { withContext(Dispatchers.IO) { WidgetDataLoader.load(app) } }.getOrNull() ?: return@launch
            if (started) apply(data)
        }
    }

    private fun interactive(): Boolean = app.getSystemService(PowerManager::class.java)?.isInteractive != false

    private fun charging() {
        val level = app.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: return
        if (level in 0..100) IslandStore.post(ChargingPeek(level))
    }

    private fun apply(d: WidgetData) {
        // A Quiet session: live until it ends, then a word that it's done (once per session).
        val run = d.quiet.running
        if (run != null) {
            IslandStore.putLive(QuietItem(run.startedAt, run.endsAt, IslandStore.held))
            handler.removeCallbacks(atQuietEnd)
            handler.postDelayed(atQuietEnd, (run.endsAt - System.currentTimeMillis()).coerceAtLeast(0L) + 600L)
        } else {
            IslandStore.removeLive(QuietItem.ID)
        }
        d.quiet.ended?.let { ended ->
            val fresh = d.now - ended.at < QUIET_DONE_FRESH_MILLIS
            if (fresh && IslandPrefs.quietDoneSeen(app) != ended.at) {
                IslandPrefs.setQuietDoneSeen(app, ended.at)
                IslandStore.post(QuietDonePeek(ended.minutes, ended.sessionsToday))
            }
        }

        // The promise: the last hour stays; reaching it is said once a day.
        if (d.lastHour) {
            IslandStore.putLive(LastHourItem(d.minutesLeft, d.promiseMinutes, d.minutesToday))
        } else {
            IslandStore.removeLive(LastHourItem.ID)
        }
        if (d.overPromise && IslandPrefs.promiseSeenOn(app) != d.today.toString()) {
            IslandPrefs.setPromiseSeen(app, d.today)
            IslandStore.post(PromisePeek(d.promiseHours))
        }

        // The week, on the days that mean something.
        val zone = ZoneId.systemDefault()
        val night = d.today.atTime(NIGHT_STARTS).atZone(zone).toInstant().toEpochMilli()
        val show = if (d.usageAccess) MomentPlan.decide(d.moment, d.part, d.now, d.today, night, IslandPrefs.momentLog(app)) else null
        if (show == null) {
            IslandStore.removeLive(MomentItem.ID)
            return
        }
        IslandPrefs.recordMoment(app, d.today, show)
        val pokes = (IslandStore.liveItem(MomentItem.ID) as? MomentItem)?.pokes ?: 0
        IslandStore.putLive(
            MomentItem(
                moment = d.moment,
                words = if (pokes > 0) MomentCopy.pokeReply(d.moment, pokes) else MomentCopy.line(d.moment, MomentCopy.daySeed(d.today)),
                eyebrow = MomentCopy.eyebrow(d.moment),
                scoreTenths = d.todayScore,
                reason = show.reason,
                endsAt = show.endsAt,
                pokes = pokes
            )
        )
        // The win arrives with the card open for a beat, as on iPhone.
        if (show.arrives) IslandStore.open(MomentItem.ID, ARRIVAL_MILLIS)
        handler.removeCallbacks(atMomentEnd)
        handler.postDelayed(atMomentEnd, (show.endsAt - System.currentTimeMillis()).coerceAtLeast(0L) + 600L)
    }

    /**
     * Settings' "Show me": the week's card opens now, whatever the day, and leaves again unless
     * the day is one the island would have shown anyway.
     */
    fun preview() {
        scope.launch {
            val data = runCatching { withContext(Dispatchers.IO) { WidgetDataLoader.load(app) } }.getOrNull() ?: return@launch
            IslandStore.putLive(IslandSamples.moment(data))
            IslandStore.open(MomentItem.ID, ARRIVAL_MILLIS)
            handler.removeCallbacks(atMomentEnd)
            handler.postDelayed(atMomentEnd, ARRIVAL_MILLIS + 600L)
        }
    }

    /** Pet Zeno: he answers in the island, and the widgets answer too. */
    fun poke() {
        val item = IslandStore.liveItem(MomentItem.ID) as? MomentItem ?: return
        WidgetDataLoader.poke(app)
        val next = item.pokes + 1
        IslandStore.putLive(item.copy(pokes = next, words = MomentCopy.pokeReply(item.moment, next)))
        IslandStore.touch()
    }

    companion object {
        const val TICK_MILLIS = 60_000L
        const val ARRIVAL_MILLIS = 6_000L
        private const val QUIET_DONE_FRESH_MILLIS = 120_000L
        private val NIGHT_STARTS: LocalTime = LocalTime.of(21, 30)
    }
}
