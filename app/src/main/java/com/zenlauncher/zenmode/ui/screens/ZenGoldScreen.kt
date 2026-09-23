package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.GoldOrder
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.HomePage
import com.zenlauncher.zenmode.ui.components.MoodBackdrop
import com.zenlauncher.zenmode.ui.components.PinnedPageFooter
import com.zenlauncher.zenmode.ui.components.moodWashColors
import com.zenlauncher.zenmode.ui.components.rememberTodayMood
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zenlauncher.zenmode.ui.components.SettingsMenuButton
import com.zenlauncher.zenmode.ui.components.pageSwipe
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.components.taperedBorder
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import java.time.LocalDate
import java.util.Locale

// ── ZM_OS v3: Zen Gold (Home left-swipe page) ───────────────────────
// Figma nodes 2026:1648 / 2026:1435. The right-hand home page: reached by swiping left on
// the home screen (Zen Score is the swipe-right page); back arrow / swipe right returns Home.
//
// Every number this screen shows — the weekly promise pattern, the daily
// average, the forecast — needs a real Gold Streak backend that doesn't exist
// yet (see zenmode_core_private/docs/plans/2026-07-gold-streak*.md). Wired as
// AppConstants.PLACEHOLDER_* for now, matching the rest of the v3 home screen.

// Figma node 2001:1481's margin (see HomeScreen.kt) — this screen shares it,
// not the app-wide Spacing.screenMargin, since it's the same v3 frame width.
private val ScreenMargin: Dp @Composable get() = 30.rdp

@Composable
fun ZenGoldScreen(
    dailyAverageMinutes: Int = AppConstants.PLACEHOLDER_DAILY_AVERAGE_MINUTES,
    promiseHours: Int = AppConstants.PLACEHOLDER_PROMISE_HOURS,
    weeklyPromiseStatus: List<Boolean?> = AppConstants.PLACEHOLDER_WEEKLY_PROMISE_STATUS,
    daysUntilUnlock: Int = AppConstants.PLACEHOLDER_DAYS_UNTIL_UNLOCK,
    daysLeftThisWeek: Int = AppConstants.PLACEHOLDER_DAYS_LEFT_THIS_WEEK,
    daysClearedUnder: Int = AppConstants.PLACEHOLDER_DAYS_CLEARED_UNDER,
    goldInvested: String = AppConstants.PLACEHOLDER_GOLD_INVESTED,
    goldChangePercent: Int = GoldOrder.changePercentFor(AppConstants.PLACEHOLDER_GOLD_INVESTED),
    forecastPercent: Int = AppConstants.PLACEHOLDER_FORECAST_PERCENT,
    forecastMonthlyAmount: Int = AppConstants.PLACEHOLDER_FORECAST_MONTHLY_AMOUNT,
    investGoldUnlocked: Boolean = AppConstants.PLACEHOLDER_INVEST_GOLD_UNLOCKED,
    onBackClick: () -> Unit,
    /** Tapping the Zen Score dot jumps straight there, same destination Home's right swipe reaches. */
    onZenScoreClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {},
    onInvestGoldClick: () -> Unit = {},
    onEditPromiseClick: () -> Unit = {},
    onViewTermsClick: () -> Unit = {}
) {
    val mood = rememberTodayMood()
    var footerHeight by remember { mutableStateOf(0.dp) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Home sits to the left of this page, so swiping right goes back to it.
            .pageSwipe(onSwipeRight = onBackClick)
    ) {
        MoodBackdrop(mood)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            ZenGoldHeader(onBackClick = onBackClick)

            Spacer(modifier = Modifier.height(14.rdp))

            Column(
                modifier = Modifier
                    .padding(horizontal = ScreenMargin)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.rdp)
            ) {
                ScreenTimeCard(
                    dailyAverageMinutes = dailyAverageMinutes,
                    promiseHours = promiseHours,
                    weeklyPromiseStatus = weeklyPromiseStatus,
                    daysUntilUnlock = daysUntilUnlock,
                    daysLeftThisWeek = daysLeftThisWeek,
                    unlocked = investGoldUnlocked,
                    daysClearedUnder = daysClearedUnder
                )

                ForecastCard(
                    forecastPercent = forecastPercent,
                    forecastMonthlyAmount = forecastMonthlyAmount
                )

                // Full-width variant: ruled top and bottom, with View all inset from the
                // right edge by the same padding the coin has on the left (node 2026:1530).
                GoldInvestedRow(
                    gold = goldInvested,
                    changePercent = goldChangePercent,
                    fullWidth = true,
                    trailing = { ViewAllPillButton(onClick = onViewAllClick) }
                )

                GoldUnlockDisclaimer(
                    daysUntilUnlock = daysUntilUnlock,
                    unlocked = investGoldUnlocked,
                    onViewTermsClick = onViewTermsClick
                )
            }

            // Room for the sticky footer, so the last card can scroll clear of it.
            Spacer(modifier = Modifier.height(footerHeight + 8.rdp))
        }

        // Invest Gold / Edit my promise stay on screen however far the page scrolls.
        PinnedPageFooter(
            current = HomePage.ZEN_GOLD,
            fadeTo = moodWashColors(mood).last(),
            onHeightChanged = { footerHeight = it },
            modifier = Modifier.align(Alignment.BottomCenter),
            onPageClick = { page ->
                when (page) {
                    HomePage.HOME -> onBackClick()
                    HomePage.ZEN_SCORE -> onZenScoreClick()
                    HomePage.ZEN_GOLD -> Unit
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = ScreenMargin)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.rdp)
            ) {
                InvestGoldButton(unlocked = investGoldUnlocked, onClick = onInvestGoldClick)
                EditPromiseButton(onClick = onEditPromiseClick)
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────

@Composable
private fun ZenGoldHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .padding(top = 12.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Same footprint as the ☰ on the right, so the title stays truly centred.
        Box(
            modifier = Modifier.size(width = 29.rdp, height = 28.rdp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                modifier = Modifier
                    .requiredSize(48.dp)
                    .clip(CircleShape)
                    .clickable(onClickLabel = "Back to home", onClick = onBackClick)
                    .padding(10.dp),
                colorFilter = ColorFilter.tint(ZenTheme.colors.textBrandStrong)
            )
        }

        Text(
            text = "ZEN GOLD",
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 24.rsp,
            letterSpacing = (-0.48).sp,
            color = ZenTheme.colors.textBrandStrong,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        SettingsMenuButton()
    }
}

// ── My Screen Time card ───────────────────────────────────────────

// SOURCE OF TRUTH: design tokens — values live in colors.xml (never a bare
// Color(0x...) literal here).
private val PromiseKeptGreen: Color @Composable get() = colorResource(R.color.gold_delta_text)
private val PromiseBrokenGray: Color @Composable get() = colorResource(R.color.stone_500)

@Composable
private fun ScreenTimeCard(
    dailyAverageMinutes: Int,
    promiseHours: Int,
    weeklyPromiseStatus: List<Boolean?>,
    daysUntilUnlock: Int,
    daysLeftThisWeek: Int,
    unlocked: Boolean,
    daysClearedUnder: Int
) {
    val colors = ZenTheme.colors
    val rule = colorResource(R.color.zen_700)
    val radius = 16.rdp
    val hrs = dailyAverageMinutes / 60
    val mins = dailyAverageMinutes % 60

    // Node 2026:1449 — a 3dp rule on the top edge that tapers round the corners, and the
    // status tab tucked into the bottom-right corner, flush with the card's own curve.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(if (unlocked) colors.statsCardFillHappy else colors.bgPrimary)
            .taperedBorder(rule, radius, top = 3.rdp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.rdp, end = 20.rdp, top = 17.rdp, bottom = 16.rdp + StatusTabHeight)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Screen time",
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.rsp,
                    letterSpacing = (-0.16).sp,
                    color = colors.textPrimary
                )
                WeeklyMonthlyToggle()
            }

            Spacer(modifier = Modifier.height(10.rdp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Average  •  This Week",
                    fontFamily = Geist,
                    fontSize = 12.rsp,
                    letterSpacing = (-0.12).sp,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "My promise - ",
                    fontFamily = Geist,
                    fontSize = 10.rsp,
                    color = colors.textPrimary
                )
                Text(
                    text = "${promiseHours}Hrs/day",
                    fontFamily = Geist,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.rsp,
                    color = rule
                )
            }

            Spacer(modifier = Modifier.height(2.rdp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%02d".format(hrs),
                        fontFamily = DepartureMono,
                        fontSize = 31.5.rsp,
                        letterSpacing = (-2.5).sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = " HRS  ",
                        fontFamily = DepartureMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 7.rsp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 6.rdp)
                    )
                    Text(
                        text = "%02d".format(mins),
                        fontFamily = DepartureMono,
                        fontSize = 33.6.rsp,
                        letterSpacing = (-2.7).sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = " MINS",
                        fontFamily = DepartureMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 6.rsp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 6.rdp)
                    )
                }
                WeeklyPromiseLegend()
            }

            Spacer(modifier = Modifier.height(10.rdp))

            WeeklyPromiseBars(weeklyPromiseStatus)

            Spacer(modifier = Modifier.height(10.rdp))

            BasicText(
                text = if (unlocked) {
                    buildAnnotatedString {
                        append("You cleared the line with $daysClearedUnder days under. ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                            append("Gold pay stays open until Sunday midnight.")
                        }
                    }
                } else {
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                            append("$daysUntilUnlock more days under ${promiseHours} hrs opens gold pay, ")
                        }
                        append("and you have $daysLeftThisWeek days left this week to do it.")
                    }
                },
                style = TextStyle(
                    fontFamily = Geist,
                    fontSize = 11.rsp,
                    lineHeight = 14.rsp,
                    letterSpacing = (-0.22).sp,
                    color = colors.textPrimary
                )
            )
        }

        StatusTab(
            unlocked = unlocked,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

private val StatusTabHeight: Dp @Composable get() = 22.rdp

/** UNLOCKED / IN PROGRESS tab sitting in the card's bottom-right corner. */
@Composable
private fun StatusTab(unlocked: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(105.rdp)
            .height(StatusTabHeight)
            .clip(RoundedCornerShape(topStart = 8.rdp))
            .background(if (unlocked) PromiseKeptGreen else colorResource(R.color.promise_badge_bg)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (unlocked) "UNLOCKED" else "IN PROGRESS",
            fontFamily = DepartureMono,
            fontSize = 9.5.rsp,
            letterSpacing = 0.4.sp,
            color = if (unlocked) Color.White else PromiseKeptGreen
        )
    }
}

@Composable
private fun WeeklyPromiseLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(4.rdp)) {
        WeeklyPromiseLegendRow(color = PromiseKeptGreen, label = "Promise within limits")
        WeeklyPromiseLegendRow(color = PromiseBrokenGray, label = "Promise Broken")
    }
}

@Composable
private fun WeeklyPromiseLegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.75.rdp)
    ) {
        Box(
            modifier = Modifier
                .size(6.75.rdp)
                .clip(RoundedCornerShape(1.5.rdp))
                .background(color)
        )
        Text(
            text = label,
            fontFamily = Geist,
            fontSize = 7.5.rsp,
            letterSpacing = (-0.075).sp,
            color = ZenTheme.colors.textPrimary
        )
    }
}

@Composable
private fun WeeklyMonthlyToggle() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .border(1.dp, colorResource(R.color.toggle_border_green), RoundedCornerShape(percent = 50))
            .padding(2.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.White)
                .border(0.5.dp, colorResource(R.color.toggle_pill_border), RoundedCornerShape(percent = 50))
                .padding(horizontal = 8.rdp, vertical = 3.rdp)
        ) {
            Text("Weekly", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 9.rsp, color = Color.Black)
        }
        Row(
            modifier = Modifier.padding(horizontal = 8.rdp, vertical = 3.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Monthly", fontFamily = Geist, fontSize = 9.rsp, color = colorResource(R.color.toggle_inactive_text))
            Spacer(modifier = Modifier.width(4.rdp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PromiseKeptGreen)
                    .padding(horizontal = 4.rdp, vertical = 1.rdp)
            ) {
                Text("PRO", fontFamily = Geist, fontSize = 5.6.rsp, color = Color.White)
            }
        }
    }
}

@Composable
private fun WeeklyPromiseBars(status: List<Boolean?>) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    // Each day is one equal column holding its bar and, centred beneath it, its letter —
    // so the letters always sit exactly under their bars.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.6.rdp)
    ) {
        dayLabels.forEachIndexed { i, label ->
            val kept = status.getOrNull(i)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.5.rdp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            when (kept) {
                                true -> PromiseKeptGreen
                                false -> PromiseBrokenGray
                                null -> colorResource(R.color.stone_200)
                            }
                        )
                )
                Spacer(modifier = Modifier.height(10.rdp))
                Text(label, fontFamily = Geist, fontSize = 12.rsp, letterSpacing = (-0.24).sp, color = ZenTheme.colors.textPrimary)
            }
        }
    }
}

// ── Forecast card ─────────────────────────────────────────────────

@Composable
private fun ForecastCard(forecastPercent: Int, forecastMonthlyAmount: Int) {
    val colors = ZenTheme.colors
    val rule = colorResource(R.color.zen_700)
    val zenGreen900 = colors.textBrandStrong
    val radius = 24.rdp

    // Real, not placeholder: whatever day the user opens this on, "today" sits at the
    // same column (see MonthsBeforeToday) with that many months of history behind it and
    // the rest as outlook — so the axis and headline are always true for this user, this day.
    val today = remember { LocalDate.now() }
    val windowMonths = remember(today) {
        (0 until ForecastWindowSize).map { i -> today.plusMonths((i - MonthsBeforeToday).toLong()).month }
    }
    val outlookMonthName = windowMonths.last().getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(colors.bgPrimary.copy(alpha = 0.71f))
            .taperedBorder(rule, radius, top = 1.rdp)
            .padding(horizontal = 20.rdp, vertical = 18.rdp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_zen_mark_gradient),
                contentDescription = null,
                modifier = Modifier.size(13.rdp),
                colorFilter = ColorFilter.tint(zenGreen900)
            )
            Spacer(modifier = Modifier.width(6.rdp))
            Text(
                text = "FORECAST",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 13.7.rsp,
                letterSpacing = (-0.14).sp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.width(4.rdp))
            Text(
                text = "(${LocalDate.now().year})",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 10.3.rsp,
                color = colors.textPrimary.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(12.rdp))

        Text(
            text = "Expect your Gold to rise by $forecastPercent% as $outlookMonthName approaches",
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.rsp,
            lineHeight = 23.rsp,
            letterSpacing = (-0.54).sp,
            color = zenGreen900
        )

        Spacer(modifier = Modifier.height(8.rdp))

        Text(
            text = "Invest an extra ₹$forecastMonthlyAmount/month & watch the magic.",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 13.rsp,
            letterSpacing = (-0.13).sp,
            color = rule
        )

        Spacer(modifier = Modifier.height(10.rdp))

        // Legend sits above the forecast half of the chart, over the last outlook columns.
        Column(
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 4.rdp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Forecast", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.rsp, letterSpacing = (-0.28).sp, color = colors.textPrimary)
            Text("₹$forecastMonthlyAmount/mo avg", fontFamily = DepartureMono, fontSize = 9.5.rsp, letterSpacing = (-0.19).sp, color = colors.textPrimary)
        }

        Spacer(modifier = Modifier.height(6.rdp))

        ForecastChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.rdp)
        )

        Spacer(modifier = Modifier.height(8.rdp))

        // Same six equal columns the chart's grid lines are centred in.
        Row(modifier = Modifier.fillMaxWidth()) {
            windowMonths.forEach { month ->
                Text(
                    text = month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()),
                    fontFamily = Geist,
                    fontSize = 10.3.rsp,
                    letterSpacing = (-0.21).sp,
                    color = colorResource(R.color.chart_axis_label),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// The axis always spans 6 months, "today" sitting 2 columns in — 2 months of history behind
// it, 3 months of outlook ahead. A layout decision, not user data, so it's a real constant
// rather than a AppConstants.PLACEHOLDER_*: true for every user on every day they open this.
private const val ForecastWindowSize = 6
private const val MonthsBeforeToday = 2

// Illustrative curve (0 = top of the chart, 1 = bottom) until a real gold-price forecast feed
// exists: a shallow dip into "today", then the forecast easing up before a climb into the
// outlook months. Shape is relative to the "today" column (index [MonthsBeforeToday]), not
// tied to any specific calendar month, so it stays correct as the real window slides.
private val ForecastCurve = listOf(0.34f, 0.55f, 0.68f, 0.60f, 0.53f, 0.12f)

/**
 * The gold-price chart (nodes 2026:1501–1510): one grid line per month, centred in the same
 * columns as the month labels; a solid line for the months so far, and a broken (dashed) line
 * carrying on from the "today" marker to the end of the outlook window — both running through
 * the same points, so they meet exactly at the marker.
 */
@Composable
private fun ForecastChart(modifier: Modifier = Modifier) {
    val pastGrid = colorResource(R.color.chart_grid_light).copy(alpha = 0.35f)
    val futureGrid = colorResource(R.color.chart_grid_dark).copy(alpha = 0.8f)
    val todayGrid = ZenTheme.colors.textPrimary
    val solidGreen = colorResource(R.color.gold_delta_text)
    val dashedGreen = colorResource(R.color.gold_delta_text).copy(alpha = 0.5f)
    val dotColor = colorResource(R.color.amber_500)
    val lineWidth = 3.rdp
    val today = MonthsBeforeToday.coerceIn(0, ForecastCurve.lastIndex)

    Canvas(modifier = modifier) {
        val column = size.width / ForecastCurve.size
        val points = ForecastCurve.mapIndexed { i, y -> Offset(column * (i + 0.5f), size.height * y) }

        points.forEachIndexed { i, p ->
            drawLine(
                color = when {
                    i == today -> todayGrid
                    i < today -> pastGrid
                    else -> futureGrid
                },
                start = Offset(p.x, 0f),
                end = Offset(p.x, size.height),
                strokeWidth = (if (i == today) 1.2.dp else 0.86.dp).toPx()
            )
        }

        drawPath(
            smoothPath(points.subList(0, today + 1)),
            color = solidGreen,
            style = Stroke(width = lineWidth.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            smoothPath(points.subList(today, points.size)),
            color = dashedGreen,
            style = Stroke(
                width = lineWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))
            )
        )

        // "Today" marker, with a soft halo so it reads over both lines.
        val marker = points[today]
        drawCircle(color = dotColor.copy(alpha = 0.25f), radius = 12.dp.toPx(), center = marker)
        drawCircle(color = dotColor, radius = 7.5.dp.toPx(), center = marker)
    }
}

/** A curve through [points] (Catmull-Rom as cubic Béziers), so neighbouring segments join smoothly. */
private fun smoothPath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points[0].x, points[0].y)
    for (i in 0 until points.lastIndex) {
        val p0 = points[(i - 1).coerceAtLeast(0)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[(i + 2).coerceAtMost(points.lastIndex)]
        cubicTo(
            p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
            p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
            p2.x, p2.y
        )
    }
}

// ── Buttons & footer ──────────────────────────────────────────────

@Composable
private fun ViewAllPillButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(34.rdp)
            .clip(RoundedCornerShape(percent = 50))
            .background(colorResource(R.color.ink_surface))
            .pressScale(onClick = onClick, onClickLabel = "View all gold")
            .padding(horizontal = 16.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "View all →",
            fontFamily = Geist,
            fontSize = 14.9.rsp,
            letterSpacing = (-0.3).sp,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun GoldUnlockDisclaimer(daysUntilUnlock: Int, unlocked: Boolean, onViewTermsClick: () -> Unit) {
    val colors = ZenTheme.colors
    val unlockOutOf = AppConstants.PROMISE_DAYS_TO_UNLOCK
    val unlockAt = unlockOutOf - daysUntilUnlock
    val bodyColor = if (unlocked) colorResource(R.color.ink_soft) else colors.textPrimary

    BasicText(
        text = if (unlocked) {
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append("Gold pay is open.")
                }
                append(" You pick the quantity, we never pick it for you, and we take nothing from it. ")
                pushStringAnnotation(tag = "terms", annotation = "terms")
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        color = PromiseKeptGreen,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("View T&C")
                }
                pop()
            }
        } else {
            buildAnnotatedString {
                val staySentence = if (daysUntilUnlock <= 1) {
                    "Stay under tomorrow and it opens then."
                } else {
                    "Stay under $daysUntilUnlock more days and it opens then."
                }
                append("Invest gold opens at $unlockAt of $unlockOutOf days under your Promise. $staySentence ")
                pushStringAnnotation(tag = "terms", annotation = "terms")
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        color = PromiseKeptGreen,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("View T&C")
                }
                pop()
            }
        },
        style = TextStyle(
            fontFamily = Geist,
            fontSize = 14.rsp,
            lineHeight = 19.rsp,
            letterSpacing = (-0.14).sp,
            color = bodyColor
        ),
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onViewTermsClick
        )
    )
}

@Composable
private fun InvestGoldButton(unlocked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(47.rdp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (unlocked) PromiseKeptGreen else colorResource(R.color.disabled_button_bg))
            .clickable(
                enabled = unlocked,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Invest Gold",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 17.6.rsp,
            letterSpacing = (-0.35).sp,
            color = if (unlocked) Color.White else colorResource(R.color.disabled_button_text)
        )
        if (!unlocked) {
            Spacer(modifier = Modifier.width(8.rdp))
            Image(
                painter = painterResource(R.drawable.ic_bangzhu),
                contentDescription = "Locked",
                modifier = Modifier.size(16.rdp)
            )
        }
    }
}

@Composable
private fun EditPromiseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.rdp)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Edit my promise",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 17.6.rsp,
            letterSpacing = (-0.35).sp,
            color = PromiseKeptGreen
        )
    }
}
