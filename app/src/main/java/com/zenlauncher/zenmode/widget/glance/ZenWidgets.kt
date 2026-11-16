package com.zenlauncher.zenmode.widget.glance

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.coreapi.zenPrefs
import com.zenlauncher.zenmode.quiet.QuietSession
import com.zenlauncher.zenmode.widget.WidgetDataLoader
import com.zenlauncher.zenmode.widget.art.Backdrop
import com.zenlauncher.zenmode.widget.faces.CircleFace
import com.zenlauncher.zenmode.widget.faces.GoldFace
import com.zenlauncher.zenmode.widget.faces.QuietFace
import com.zenlauncher.zenmode.widget.faces.ScoreFace
import com.zenlauncher.zenmode.widget.faces.ScreenTimeFace
import com.zenlauncher.zenmode.widget.faces.WeekFace
import com.zenlauncher.zenmode.widget.faces.ZenoFace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

// ── The family: seven widgets, each one face at every size ──────────

// The size each is first placed at: an n × m widget is (73n − 16) × (118m − 16) dp.
private val TWO_BY_TWO = DpSize(184.dp, 220.dp)
private val FOUR_BY_TWO = DpSize(380.dp, 220.dp)
private val FOUR_BY_ONE = DpSize(380.dp, 102.dp)

class ZenoWidget : ZenGlanceWidget(ZenoFace, TWO_BY_TWO)
class ScoreWidget : ZenGlanceWidget(ScoreFace, TWO_BY_TWO)
class WeekWidget : ZenGlanceWidget(WeekFace, FOUR_BY_TWO) {
    override val needsBloom: Boolean = true
}
class ScreenTimeWidget : ZenGlanceWidget(ScreenTimeFace, FOUR_BY_TWO)
class QuietWidget : ZenGlanceWidget(QuietFace, FOUR_BY_ONE)
class GoldWidget : ZenGlanceWidget(GoldFace, TWO_BY_TWO)
class CircleWidget : ZenGlanceWidget(CircleFace, FOUR_BY_TWO)

class ZenoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ZenoWidget()
}
class ScoreWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScoreWidget()
}
class WeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekWidget()
}
class ScreenTimeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScreenTimeWidget()
}
class QuietWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuietWidget()
}
class GoldWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoldWidget()
}
class CircleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CircleWidget()
}

/**
 * Keeping the widgets current. They change with the day, the week and the person, so they are
 * redrawn:
 *
 *  · whenever the app refreshes the day (Home resuming, [refresh]);
 *  · every 15 minutes by WorkManager, with the system's 30-minute update as the net;
 *  · at each part of the day (05:00, 11:00, 17:00, 21:30) and just after midnight, so Zeno's
 *    light follows the day and a new day or week never shows yesterday's numbers. These are
 *    inexact alarms: no exact-alarm permission is asked for;
 *  · at the end of a Quiet session, so the countdown gives way to "Done";
 *  · after a reboot, a time change or an update, which also re-arm the alarms.
 */
object ZenWidgets {

    /** Every widget in the family, in the order Settings lists them. */
    val receivers = listOf(
        ZenoWidgetReceiver::class.java, WeekWidgetReceiver::class.java, ScreenTimeWidgetReceiver::class.java, ScoreWidgetReceiver::class.java,
        QuietWidgetReceiver::class.java, GoldWidgetReceiver::class.java, CircleWidgetReceiver::class.java
    )

    private val widgets: List<GlanceAppWidget>
        get() = listOf(ZenoWidget(), ScoreWidget(), WeekWidget(), ScreenTimeWidget(), QuietWidget(), GoldWidget(), CircleWidget())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _tick = MutableStateFlow(0L)

    /** Live widgets recompose when this moves. */
    val tick: StateFlow<Long> = _tick

    private const val WORK_NAME = "zen_widgets_refresh"
    private const val REFRESH_MINUTES = 15L
    private const val KEY_LAST_REFRESH = "widgets_last_refresh"
    private const val KEY_PREVIEWS_VERSION = "widgets_previews_version"
    /** Bump when the faces change enough that the picker's previews should be redrawn within one version. */
    private const val PREVIEWS_REVISION = 2
    /** Home resumes far more often than a widget needs redrawing. */
    private val RESUME_THROTTLE_MILLIS = TimeUnit.MINUTES.toMillis(2)

    /** New data: drop the shared snapshot and wake every live widget. */
    fun bump() {
        WidgetDataLoader.invalidate()
        _tick.value = SystemClock.elapsedRealtimeNanos()
    }

    /** Redraws every placed widget now. */
    suspend fun updateAll(context: Context) {
        bump()
        if (!hasAny(context)) return
        widgets.forEach { runCatching { it.updateAll(context) } }
    }

    /** From the app: redraw soon, at most once every couple of minutes. */
    fun refresh(context: Context, force: Boolean = false) {
        val app = context.applicationContext
        val prefs = app.zenPrefs()
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong(KEY_LAST_REFRESH, 0L) < RESUME_THROTTLE_MILLIS) return
        prefs.edit { putLong(KEY_LAST_REFRESH, now) }
        scope.launch { updateAll(app) }
    }

    /** Any of our widgets placed anywhere? Saves the work when none are. */
    suspend fun hasAny(context: Context): Boolean {
        val manager = GlanceAppWidgetManager(context)
        return listOf(ZenoWidget::class.java, ScoreWidget::class.java, WeekWidget::class.java, ScreenTimeWidget::class.java, QuietWidget::class.java, GoldWidget::class.java, CircleWidget::class.java)
            .any { runCatching { manager.getGlanceIds(it).isNotEmpty() }.getOrDefault(false) }
    }

    /** Called from Application.onCreate and on boot: the periodic refresh and the next alarm. */
    fun schedule(context: Context) {
        val app = context.applicationContext
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(REFRESH_MINUTES, TimeUnit.MINUTES).build()
        )
        armNextBoundary(app)
        QuietSession.running(app)?.let { armAt(app, it.endsAt) }
        publishPreviews(app)
    }

    /**
     * Android 15 and later draw the picker's previews from the widgets themselves
     * ([ZenGlanceWidget.providePreview]). The system rate-limits this, so each widget's preview is
     * published once per app version and preview revision, and one the system refused is retried
     * at the next start without repeating the ones that went through.
     */
    private fun publishPreviews(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val stamp = "${com.zenlauncher.zenmode.BuildConfig.VERSION_CODE}.$PREVIEWS_REVISION"
        val prefs = context.zenPrefs()
        scope.launch {
            val manager = GlanceAppWidgetManager(context)
            receivers.forEach { receiver ->
                val key = KEY_PREVIEWS_VERSION + "_" + receiver.simpleName
                if (prefs.getString(key, null) == stamp) return@forEach
                val result = runCatching { manager.setWidgetPreviews(receiver.kotlin) }.getOrNull()
                if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) prefs.edit { putString(key, stamp) }
            }
        }
    }

    /** The next part of the day, or a few seconds past midnight, whichever comes first. */
    fun armNextBoundary(context: Context, now: LocalDateTime = LocalDateTime.now()) {
        val minute = now.hour * 60 + now.minute
        val next = DayPart.nextBoundaryAfter(minute)
        val midnight = DayPart.MINUTES_PER_DAY
        val target = now.toLocalDate().atStartOfDay()
            .plusMinutes(minOf(next, midnight).toLong())
            .plusSeconds(MIDNIGHT_SLACK_SECONDS)
        armAt(context, target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), REQUEST_BOUNDARY)
    }

    /** An inexact, idle-friendly alarm at [atMillis] that redraws every widget. Never early. */
    fun armAt(context: Context, atMillis: Long, request: Int = REQUEST_QUIET_END) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, WidgetTickReceiver::class.java).setAction(ACTION_TICK)
        val pending = PendingIntent.getBroadcast(context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // An exact alarm only if the person already granted one; it is never asked for.
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact && request == REQUEST_QUIET_END) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC, atMillis, pending)
        }
    }

    // ── Per-widget backdrop ─────────────────────────────────────────

    fun backdropKey(appWidgetId: Int) = "widget_backdrop_$appWidgetId"

    internal const val ACTION_TICK = "com.zenlauncher.zenmode.widget.TICK"
    private const val REQUEST_BOUNDARY = 4101
    private const val REQUEST_QUIET_END = 4102
    private const val MIDNIGHT_SLACK_SECONDS = 5L
}

/** The backdrop each placed widget wears (Scene, Glass or Plain), by app widget id. */
object WidgetPrefs {
    fun backdrop(context: Context, appWidgetId: Int): Backdrop =
        Backdrop.of(context.zenPrefs().getString(ZenWidgets.backdropKey(appWidgetId), null))

    fun setBackdrop(context: Context, appWidgetId: Int, backdrop: Backdrop) {
        context.zenPrefs().edit { putString(ZenWidgets.backdropKey(appWidgetId), backdrop.name) }
    }
}

/** The 15-minute refresh. */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ZenWidgets.updateAll(applicationContext)
        ZenWidgets.armNextBoundary(applicationContext)
        return Result.success()
    }
}

/** The day's boundaries, the end of a Quiet session, and the system events that move time. */
class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (intent.action != ZenWidgets.ACTION_TICK) ZenWidgets.schedule(app)
                ZenWidgets.armNextBoundary(app)
                ZenWidgets.updateAll(app)
            } finally {
                pending.finish()
            }
        }
    }
}
