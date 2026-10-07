package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zenlauncher.zenmode.AppGridPreferences
import com.zenlauncher.zenmode.GesturePreferences
import com.zenlauncher.zenmode.HomeGesture
import com.zenlauncher.zenmode.ui.components.ZenModeOsSettingsTitle
import com.zenlauncher.zenmode.ResistancePreferences
import com.zenlauncher.zenmode.HomeThemePreferences
import com.zenlauncher.zenmode.ThemePreferences
import com.zenlauncher.zenmode.ZenSound
import com.zenlauncher.zenmode.ui.components.ZenMotion
import com.zenlauncher.zenmode.ui.components.rememberZenFeedback
import androidx.compose.runtime.collectAsState
import com.zenlauncher.zenmode.coreapi.ZEN_CIRCLE_MAX_MEMBERS
import com.zenlauncher.zenmode.coreapi.services.BillingPeriod
import com.zenlauncher.zenmode.coreapi.services.Entitlement
import com.zenlauncher.zenmode.coreapi.services.PlanOffer
import com.zenlauncher.zenmode.coreapi.services.ProFeature
import com.zenlauncher.zenmode.ui.components.GlyphKind
import com.zenlauncher.zenmode.ui.components.ProTagState
import com.zenlauncher.zenmode.ui.components.RowTrailing
import com.zenlauncher.zenmode.ui.components.SegmentOption
import com.zenlauncher.zenmode.ui.components.ZenButton
import com.zenlauncher.zenmode.ui.components.ZenButtonStyle
import com.zenlauncher.zenmode.ui.components.ZenEyebrow
import com.zenlauncher.zenmode.ui.components.ZenGlyph
import com.zenlauncher.zenmode.ui.components.ZenProTag
import com.zenlauncher.zenmode.ui.components.ZenRowDivider
import com.zenlauncher.zenmode.ui.components.ZenSegmented
import com.zenlauncher.zenmode.ui.components.ZenSettingToggleItem
import com.zenlauncher.zenmode.ui.components.ZenSettingsGroup
import com.zenlauncher.zenmode.ui.components.ZenSettingsRow
import com.zenlauncher.zenmode.ui.components.BugReportSheet
import com.zenlauncher.zenmode.ui.components.ZenSheet
import com.zenlauncher.zenmode.ui.components.ZenSheetBody
import com.zenlauncher.zenmode.ui.components.ZenSheetTitle
import com.zenlauncher.zenmode.ui.components.zenCard
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.Spacing
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.ZenTypography
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import java.time.LocalDate
import com.zenlauncher.zenmode.ui.components.LocalZenClock
import java.time.format.TextStyle as JavaTextStyle
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Color
import com.zenlauncher.zenmode.ZenCheckInPreferences
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The range the settings screen-time chart is showing. */
internal enum class ScreenTimeRange { WEEK, MONTH }

@Composable
internal fun SettingsScreenTimeCard(
    weeklyHours: List<Float>,
    showMonthOption: Boolean,
    isPro: Boolean,
    loadMonthlyHours: suspend () -> List<Float>,
    onMonthLocked: () -> Unit
) {
    val colors = ZenTheme.colors
    var range by rememberSaveable { mutableStateOf(ScreenTimeRange.WEEK) }
    var monthlyHours by remember { mutableStateOf<List<Float>?>(null) }
    val showingMonth = range == ScreenTimeRange.MONTH && isPro

    LaunchedEffect(showingMonth) {
        if (showingMonth && monthlyHours == null) monthlyHours = loadMonthlyHours()
    }
    // A cancelled subscription drops back to the week without leaving a locked range selected.
    LaunchedEffect(isPro) { if (!isPro) range = ScreenTimeRange.WEEK }

    val data = if (showingMonth) monthlyHours.orEmpty() else weeklyHours
    val total = data.sum()
    val average = if (data.isEmpty()) 0f else total / data.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zenCard()
            .padding(16.rdp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                ZenEyebrow(if (showingMonth) "Last 30 days" else "This week")
                Text(
                    text = formatHours(total),
                    fontFamily = DepartureMono,
                    fontSize = 34.rsp,
                    lineHeight = 40.rsp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 4.rdp)
                )
                Text(
                    text = "${formatHours(average)} a day on average",
                    fontFamily = Geist,
                    fontSize = 13.rsp,
                    color = colors.textSecondary
                )
            }
            if (showMonthOption) {
                ZenSegmented(
                    options = listOf(
                        SegmentOption(ScreenTimeRange.WEEK, "7D"),
                        SegmentOption(ScreenTimeRange.MONTH, "30D", locked = !isPro)
                    ),
                    selected = if (showingMonth) ScreenTimeRange.MONTH else ScreenTimeRange.WEEK,
                    onSelect = { option ->
                        if (option.locked) {
                            onMonthLocked()
                        } else {
                            range = option.value
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(16.rdp))
        if (showingMonth && monthlyHours == null) {
            Box(Modifier.fillMaxWidth().height(150.rdp), contentAlignment = Alignment.Center) {
                Text("Counting the month…", fontFamily = Geist, fontSize = 13.rsp, color = colors.textSecondary)
            }
        } else {
            ScreenTimeBars(data, labelDays = !showingMonth)
        }
        Text(
            text = "Counted on this phone. Your partner sees your Zen Score, never these hours.",
            fontFamily = Geist,
            fontSize = 13.rsp,
            lineHeight = 18.rsp,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 10.rdp)
        )
    }
}

/** "3h", or "3.5h" when the midpoint tick lands on a half hour. */
private fun axisHourLabel(hours: Float): String {
    val whole = kotlin.math.round(hours).toInt()
    return if (kotlin.math.abs(hours - whole) < 0.05f) "${whole}h" else "${"%.1f".format(hours)}h"
}

@Composable
private fun ScreenTimeBars(hours: List<Float>, labelDays: Boolean) {
    val colors = ZenTheme.colors
    val bar = colors.textBrand
    val grid = colors.borderHairlineSoft
    val max = maxOf(6f, kotlin.math.ceil(hours.maxOrNull() ?: 0f))
    val clock = LocalZenClock.current
    val dayLabels = remember(hours.size, clock) {
        val today = LocalDate.now(clock)
        (hours.size - 1 downTo 0).map {
            today.minusDays(it.toLong()).dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).uppercase()
        }
    }

    Row(Modifier.fillMaxWidth()) {
        // Y axis: hour scale for the three gridlines the chart draws below.
        Column(
            modifier = Modifier
                .height(130.rdp)
                .width(24.rdp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val axisStyle = ZenTypography.monoLabel.copy(fontSize = 10.rsp, lineHeight = 11.rsp, letterSpacing = 0.rsp)
            Text(axisHourLabel(max), style = axisStyle, color = colors.textMuted)
            Text(axisHourLabel(max / 2f), style = axisStyle, color = colors.textMuted)
            Text(axisHourLabel(0f), style = axisStyle, color = colors.textMuted)
        }
        Spacer(Modifier.width(6.rdp))
        Column(Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.rdp)
                    .semantics {
                        contentDescription = "Screen time per day, ${hours.size} days, up to ${axisHourLabel(max)}. Today ${formatHours(hours.lastOrNull() ?: 0f)}."
                    }
            ) {
                val n = hours.size.coerceAtLeast(1)
                val gap = if (n > 7) 3.dp.toPx() else 10.dp.toPx()
                val barWidth = (size.width - gap * (n - 1)) / n
                listOf(0f, 0.5f, 1f).forEach { f ->
                    val y = size.height * (1 - f)
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                hours.forEachIndexed { i, h ->
                    val barHeight = (h / max).coerceIn(0f, 1f) * size.height
                    val isToday = i == hours.lastIndex
                    drawRoundRect(
                        color = if (isToday) bar else bar.copy(alpha = 0.3f),
                        topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(if (n > 7) 2.dp.toPx() else 6.dp.toPx())
                    )
                }
            }
            Spacer(Modifier.height(6.rdp))
            if (labelDays) {
                Row(Modifier.fillMaxWidth()) {
                    dayLabels.forEachIndexed { i, label ->
                        Text(
                            text = label,
                            style = ZenTypography.monoLabel,
                            color = if (i == dayLabels.lastIndex) colors.textPrimary else colors.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth()) {
                    Text("30 DAYS AGO", style = ZenTypography.monoLabel, color = colors.textMuted, modifier = Modifier.weight(1f))
                    Text("TODAY", style = ZenTypography.monoLabel, color = colors.textPrimary)
                }
            }
        }
    }
}

private fun formatHours(hours: Float): String {
    val minutes = (hours * 60).toInt()
    return "${minutes / 60}h ${(minutes % 60).toString().padStart(2, '0')}m"
}

// ── Footer ─────────────────────────────────────────────────────────

// ── Gestures ──────────────────────────────────────

/**
 * One gesture's row. A Pro gesture reads as locked unless Pro is active, and its switch then
 * routes to the gate sheet rather than flipping — [GesturePreferences.active] would ignore it
 * on Home anyway, so a switch that moved would be lying. On a backend that can't sell Pro
 * nothing is locked and every gesture is simply available.
 */
