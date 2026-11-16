package com.zenlauncher.zenmode.widget.glance

import android.appwidget.AppWidgetManager
import android.content.Intent
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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
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
 * The one choice a widget offers, behind the system's "reconfigure" on a long press (the
 * providers declare `reconfigurable|configuration_optional`, so nothing is asked when a widget
 * is first placed and Light is the default; widget quality bar WS-4). Three backdrops, each shown
 * as this very widget, now.
 */
class WidgetStyleActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val face = faceFor(appWidgetId)
        val (w, h) = sizeOf(appWidgetId)
        val current = WidgetPrefs.backdrop(this, appWidgetId)
        setContent {
            ZenTheme {
                Picker(face, w, h, current, ::choose)
            }
        }
    }

    private fun choose(backdrop: Backdrop) {
        WidgetPrefs.setBackdrop(this, appWidgetId, backdrop)
        lifecycleScope.launch {
            ZenWidgets.updateAll(applicationContext)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }

    private fun faceFor(id: Int): Face = when (AppWidgetManager.getInstance(this).getAppWidgetInfo(id)?.provider?.className) {
        ScoreWidgetReceiver::class.java.name -> ScoreFace
        WeekWidgetReceiver::class.java.name -> WeekFace
        ScreenTimeWidgetReceiver::class.java.name -> ScreenTimeFace
        QuietWidgetReceiver::class.java.name -> QuietFace
        GoldWidgetReceiver::class.java.name -> GoldFace
        CircleWidgetReceiver::class.java.name -> CircleFace
        else -> ZenoFace
    }

    /** The widget's size in portrait, as the launcher reported it. */
    private fun sizeOf(id: Int): Pair<Float, Float> {
        val o = AppWidgetManager.getInstance(this).getAppWidgetOptions(id)
        val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).takeIf { it > 0 } ?: DEFAULT_W
        val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).takeIf { it > 0 } ?: DEFAULT_H
        return w.toFloat() to h.toFloat()
    }

    private companion object {
        const val DEFAULT_W = 184
        const val DEFAULT_H = 220
    }
}

private data class Option(val backdrop: Backdrop, val name: String, val note: String)

private val options = listOf(
    Option(Backdrop.LIGHT, "Light", "Soft light that follows your day: the hour, today's mood, the week's moment."),
    Option(Backdrop.GLASS, "Glass", "Your wallpaper shows through."),
    Option(Backdrop.PLAIN, "Plain", "Paper and ink, nothing else.")
)

@Composable
private fun Picker(face: Face, w: Float, h: Float, current: Backdrop, onChoose: (Backdrop) -> Unit) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    var previews by remember { mutableStateOf<Map<Backdrop, Bitmap>>(emptyMap()) }
    LaunchedEffect(Unit) {
        previews = withContext(Dispatchers.Default) {
            val data = WidgetDataLoader.load(context)
            val dark = ZenGlanceWidget.systemIsDark()
            val radius = ZenGlanceWidget.widgetRadius(context)
            Backdrop.entries.associateWith {
                WidgetRenderer.render(context, face, FaceInput(data, w, h, dark, it, radius), context.resources.displayMetrics.density).bitmap
            }
        }
    }
    Column(
        Modifier.fillMaxSize().background(colors.bgPrimary).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Backdrop", style = TextStyle(fontFamily = ClashDisplay, fontSize = 28.sp, lineHeight = 34.sp, color = colors.textPrimary))
        Text(
            "How this widget sits on your home screen.",
            style = TextStyle(fontFamily = Geist, fontSize = 15.sp, lineHeight = 22.sp, color = colors.textSecondary)
        )
        Spacer(Modifier.height(4.dp))
        options.forEach { option ->
            val selected = option.backdrop == current
            Column(
                Modifier.fillMaxWidth()
                    .border(if (selected) 2.dp else 1.dp, if (selected) colors.textBrand else colors.borderSubtle, RoundedCornerShape(24.dp))
                    .background(colors.surfaceElevated, RoundedCornerShape(24.dp))
                    .clickable(role = Role.RadioButton) { onChoose(option.backdrop) }
                    .semantics {
                        this.selected = selected
                        contentDescription = "${option.name}. ${option.note}"
                    }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth().height(minOf(h, 240f).dp).background(Brush.linearGradient(WidgetStyleColors.wallpaper), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    previews[option.backdrop]?.let {
                        val scale = minOf(1f, 220f / h)
                        Image(it.asImageBitmap(), null, Modifier.size((w * scale).dp, (h * scale).dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(option.name, style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.textPrimary))
                    if (selected) Text("Current", style = TextStyle(fontFamily = Geist, fontSize = 13.sp, color = colors.textBrand))
                }
                Text(option.note, style = TextStyle(fontFamily = Geist, fontSize = 14.sp, lineHeight = 20.sp, color = colors.textSecondary))
            }
        }
    }
}
