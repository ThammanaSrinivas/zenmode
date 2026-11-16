package com.zenlauncher.zenmode.widget.glance

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import com.zenlauncher.zenmode.DistractionBlockerActivity
import com.zenlauncher.zenmode.MainActivity
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ZenGoldActivity
import com.zenlauncher.zenmode.ZenScoreActivity
import com.zenlauncher.zenmode.quiet.QuietSession
import com.zenlauncher.zenmode.widget.WidgetBloomLoader
import com.zenlauncher.zenmode.widget.WidgetData
import com.zenlauncher.zenmode.widget.WidgetDataLoader
import com.zenlauncher.zenmode.widget.WidgetRenderer
import com.zenlauncher.zenmode.widget.art.Backdrop
import com.zenlauncher.zenmode.widget.faces.Face
import com.zenlauncher.zenmode.widget.faces.FaceInput
import com.zenlauncher.zenmode.widget.faces.WidgetAction
import kotlinx.coroutines.delay

/**
 * The frame every ZenMode OS widget shares. Each widget is one [Face]; this draws it at exactly
 * the size the launcher gives it ([SizeMode.Exact]), as one image with a spoken description,
 * lays its tappable parts over it and, for a Quiet session, the ticking countdown.
 *
 * A live widget recomposes on [ZenWidgets.tick], so a refresh reaches widgets whose Glance
 * session is still open as well as ones that have to start a new one.
 */
abstract class ZenGlanceWidget(private val face: Face, previewSize: DpSize) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    /** The picker draws the preview at the widget's first-placed size, not at its smallest. */
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(previewSize))

    /** The bloom's hours are only read by the widget that draws them. */
    protected open val needsBloom: Boolean = false

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        provideContent { Content(appWidgetId) }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val preview = WidgetDemo.data()
        provideContent { Frame(preview, Backdrop.LIGHT, private = false, bloom = WidgetDemo.bloom) }
    }

    @Composable
    private fun Content(appWidgetId: Int) {
        val context = LocalContext.current
        val tick by ZenWidgets.tick.collectAsState()
        val data = remember(tick) { WidgetDataLoader.load(context) }
        val bloom = remember(tick) { if (needsBloom) WidgetBloomLoader.load(context) else null }
        val backdrop = remember(tick) { WidgetPrefs.backdrop(context, appWidgetId) }
        val private = remember(tick) { onLockScreen(context, appWidgetId) }
        // Zeno answers a poke for a few seconds, then goes back to the moment's line.
        if (data.answering) {
            LaunchedEffect(tick) {
                delay(WidgetDataLoader.ANSWER_MILLIS)
                ZenWidgets.bump()
            }
        }
        Frame(data, backdrop, private, bloom)
    }

    @Composable
    private fun Frame(data: WidgetData, backdrop: Backdrop, private: Boolean, bloom: List<com.zenlauncher.zenmode.widget.faces.BloomCell>?) {
        val context = LocalContext.current
        val size = LocalSize.current
        val dark = systemIsDark()
        val radius = widgetRadius(context)
        val rendered = remember(data, size, backdrop, private, dark, bloom) {
            WidgetRenderer.render(
                context, face,
                FaceInput(data, size.width.value, size.height.value, dark, backdrop, radius, private, bloom),
                density = context.resources.displayMetrics.density
            )
        }
        val result = rendered.face
        Box(
            GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) it.cornerRadius(android.R.dimen.system_app_widget_background_radius) else it }
                .clickable(actionFor(context, result.tap))
        ) {
            Image(
                provider = ImageProvider(rendered.bitmap),
                contentDescription = result.description,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            result.hits.forEach { hit ->
                Box(GlanceModifier.fillMaxSize().padding(start = hit.rect.left.dp, top = hit.rect.top.dp)) {
                    Box(GlanceModifier.size(hit.rect.width().dp, hit.rect.height().dp).clickable(actionFor(context, hit.action))) {}
                }
            }
            result.clock?.let { clock ->
                Box(GlanceModifier.fillMaxSize().padding(start = clock.x.dp, top = clock.y.dp)) {
                    AndroidRemoteViews(clockViews(context, clock), GlanceModifier.size(clock.w.dp, clock.h.dp))
                }
            }
        }
    }

    /** The countdown as a system Chronometer: it ticks inside the host with no updates from us. */
    private fun clockViews(context: Context, clock: com.zenlauncher.zenmode.widget.faces.Clock): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_quiet_clock).apply {
            // Chronometer counts against elapsedRealtime, which restarts at boot: rebuilt on every update.
            val base = SystemClock.elapsedRealtime() + (clock.endsAt - System.currentTimeMillis()).coerceAtLeast(0L)
            setChronometer(R.id.quiet_clock, base, null, true)
            setChronometerCountDown(R.id.quiet_clock, true)
            setTextColor(R.id.quiet_clock, clock.color)
            // dp, not sp: the face reserved exactly this much room for it.
            setTextViewTextSize(R.id.quiet_clock, TypedValue.COMPLEX_UNIT_DIP, clock.size)
            setString(R.id.quiet_clock, "setFontFeatureSettings", "tnum")
        }

    companion object {
        /** The system's own dark theme, not the app's: a widget lives on someone else's home screen. */
        fun systemIsDark(): Boolean =
            Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        /** The system's widget radius (never more than 28dp); 22dp, the app's own widget radius, before Android 12. */
        fun widgetRadius(context: Context): Float {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val px = context.resources.getDimension(android.R.dimen.system_app_widget_background_radius)
                return px / context.resources.displayMetrics.density
            }
            return 22f
        }

        /** On the lock screen (Android 16 QPR1 and later) a widget keeps minutes, the circle and gold to itself. */
        fun onLockScreen(context: Context, appWidgetId: Int): Boolean {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return false
            val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
            return options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1) == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
        }

        fun actionFor(context: Context, action: WidgetAction): Action = when (action) {
            WidgetAction.POKE -> actionRunCallback<PokeZeno>()
            WidgetAction.QUIET_START -> actionRunCallback<StartQuiet>()
            WidgetAction.QUIET_END -> actionRunCallback<EndQuiet>()
            else -> actionStartActivity(intentFor(context, action))
        }

        /**
         * Where a tap goes. Today and Week both open the Zen Score page for now; if they get
         * screens of their own, only this function changes.
         */
        fun intentFor(context: Context, action: WidgetAction): Intent = when (action) {
            WidgetAction.OPEN_GOLD -> Intent(context, ZenGoldActivity::class.java)
            WidgetAction.OPEN_CIRCLE, WidgetAction.OPEN_SIGN_IN ->
                Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_SHOW_BUDDY_CONNECT, true)
            WidgetAction.OPEN_QUIET_SETUP -> Intent(context, DistractionBlockerActivity::class.java)
            WidgetAction.OPEN_USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}"))
            else -> Intent(context, ZenScoreActivity::class.java)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

/** Tap Zeno: he answers, in place. */
class PokeZeno : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetDataLoader.poke(context)
        ZenWidgets.updateAll(context)
    }
}

/** Start a Quiet session from the widget, without opening the app. */
class StartQuiet : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        if (!QuietSession.isAvailable(context)) {
            // Quieting was turned off since the widget was drawn: redraw it, and its button
            // becomes "Set up", which opens the screen directly. Starting an activity from here
            // would be a background start, which Android may refuse.
            ZenWidgets.updateAll(context)
            return
        }
        val run = QuietSession.start(context)
        ZenWidgets.armAt(context, run.endsAt)
        ZenWidgets.updateAll(context)
    }
}

/** End the running Quiet session early. */
class EndQuiet : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        QuietSession.end(context)
        ZenWidgets.updateAll(context)
    }
}
