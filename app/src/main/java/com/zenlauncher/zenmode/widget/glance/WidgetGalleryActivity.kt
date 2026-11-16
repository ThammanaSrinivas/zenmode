package com.zenlauncher.zenmode.widget.glance

import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.WidgetStyleColors
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.widget.WidgetDataLoader
import com.zenlauncher.zenmode.widget.WidgetRenderer
import com.zenlauncher.zenmode.widget.art.Backdrop
import com.zenlauncher.zenmode.widget.faces.CircleFace
import com.zenlauncher.zenmode.widget.faces.Face
import com.zenlauncher.zenmode.widget.faces.FaceInput
import com.zenlauncher.zenmode.widget.faces.GoldFace
import com.zenlauncher.zenmode.widget.faces.QuietFace
import com.zenlauncher.zenmode.widget.faces.ScoreFace
import com.zenlauncher.zenmode.widget.faces.ScreenTimeFace
import com.zenlauncher.zenmode.widget.faces.WeekFace
import com.zenlauncher.zenmode.widget.faces.ZenoFace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings → Widgets: the family, each drawn live with today's numbers, with an Add button that
 * asks the home screen to place it (`requestPinGlanceAppWidget`). When the home screen can't
 * take a pinned widget (ZenMode OS itself is the home app, for one), it says where widgets go.
 */
class WidgetGalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenTheme { Gallery(onBack = ::finish) } }
    }
}

/** One widget as the gallery shows it, at the size it is first placed. */
private class Entry(
    val nameRes: Int,
    val descriptionRes: Int,
    val size: String,
    val w: Float,
    val h: Float,
    val face: Face,
    val receiver: Class<out GlanceAppWidgetReceiver>,
    val widget: () -> GlanceAppWidget
)

private val family = listOf(
    Entry(R.string.widget_zeno_name, R.string.widget_zeno_description, "2 × 2", 184f, 220f, ZenoFace, ZenoWidgetReceiver::class.java) { ZenoWidget() },
    Entry(R.string.widget_week_name, R.string.widget_week_description, "4 × 2", 380f, 220f, WeekFace, WeekWidgetReceiver::class.java) { WeekWidget() },
    Entry(R.string.widget_screen_time_name, R.string.widget_screen_time_description, "4 × 2", 380f, 220f, ScreenTimeFace, ScreenTimeWidgetReceiver::class.java) { ScreenTimeWidget() },
    Entry(R.string.widget_score_name, R.string.widget_score_description, "2 × 2", 184f, 220f, ScoreFace, ScoreWidgetReceiver::class.java) { ScoreWidget() },
    Entry(R.string.widget_quiet_name, R.string.widget_quiet_description, "4 × 1", 380f, 102f, QuietFace, QuietWidgetReceiver::class.java) { QuietWidget() },
    Entry(R.string.widget_gold_name, R.string.widget_gold_description, "2 × 2", 184f, 220f, GoldFace, GoldWidgetReceiver::class.java) { GoldWidget() },
    Entry(R.string.widget_circle_name, R.string.widget_circle_description, "4 × 2", 380f, 220f, CircleFace, CircleWidgetReceiver::class.java) { CircleWidget() }
)

@Composable
private fun Gallery(onBack: () -> Unit) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val canPin = remember { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }
    var faces by remember { mutableStateOf<Map<Entry, Bitmap>>(emptyMap()) }
    LaunchedEffect(Unit) {
        faces = withContext(Dispatchers.Default) {
            val data = WidgetDataLoader.load(context)
            val dark = ZenGlanceWidget.systemIsDark()
            val radius = ZenGlanceWidget.widgetRadius(context)
            family.associateWith {
                WidgetRenderer.render(context, it.face, FaceInput(data, it.w, it.h, dark, Backdrop.LIGHT, radius), context.resources.displayMetrics.density).bitmap
            }
        }
    }
    Column(
        Modifier.fillMaxSize().background(colors.bgPrimary).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "‹ Settings",
            Modifier.clickable(role = Role.Button, onClick = onBack).padding(vertical = 12.dp),
            style = TextStyle(fontFamily = Geist, fontSize = 15.sp, color = colors.textBrand)
        )
        Text("Widgets", style = TextStyle(fontFamily = ClashDisplay, fontSize = 32.sp, lineHeight = 38.sp, color = colors.textPrimary))
        Text(
            if (canPin) "Your week on any home screen. They change with the week, and turn gold when you win it."
            else "Widgets live on other home screens. There, long-press an empty spot, choose Widgets, and find $PRODUCT_NAME.",
            style = TextStyle(fontFamily = Geist, fontSize = 15.sp, lineHeight = 22.sp, color = colors.textSecondary)
        )
        family.forEach { entry ->
            Column(
                Modifier.fillMaxWidth()
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(24.dp))
                    .background(colors.surfaceElevated, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth().height((entry.h * 0.8f + 24f).dp).background(Brush.linearGradient(WidgetStyleColors.wallpaper), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    faces[entry]?.let { Image(it.asImageBitmap(), null, Modifier.size((entry.w * 0.8f).dp, (entry.h * 0.8f).dp)) }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(context.getString(entry.nameRes), style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.textPrimary))
                            Text(entry.size, style = ZenTheme.typography.monoLabel.copy(color = colors.textSecondary))
                        }
                        Text(context.getString(entry.descriptionRes), style = TextStyle(fontFamily = Geist, fontSize = 14.sp, lineHeight = 20.sp, color = colors.textSecondary))
                    }
                    if (canPin) {
                        Text(
                            "Add",
                            Modifier.padding(start = 12.dp)
                                .background(colors.actionPrimary, RoundedCornerShape(999.dp))
                                .clickable(role = Role.Button) {
                                    scope.launch {
                                        GlanceAppWidgetManager(context).requestPinGlanceAppWidget(
                                            entry.receiver, entry.widget(), DpSize(entry.w.dp, entry.h.dp), null, null
                                        )
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.actionPrimaryText)
                        )
                    }
                }
            }
        }
    }
}
