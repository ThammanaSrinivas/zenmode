package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// ── ZM_OS v3: Zen Gold (Home right-swipe page) ──────────────────────
// Figma node 2026:1648. Reached by swiping right on the home screen (mirrors
// swipe-left-for-Settings); back arrow / swipe-left-here returns to Home.
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
    goldInvested: String = AppConstants.PLACEHOLDER_GOLD_INVESTED,
    goldChangePercent: Int = AppConstants.PLACEHOLDER_GOLD_CHANGE_PERCENT,
    forecastPercent: Int = AppConstants.PLACEHOLDER_FORECAST_PERCENT,
    forecastMonthlyAmount: Int = AppConstants.PLACEHOLDER_FORECAST_MONTHLY_AMOUNT,
    investGoldUnlocked: Boolean = AppConstants.PLACEHOLDER_INVEST_GOLD_UNLOCKED,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {},
    onInvestGoldClick: () -> Unit = {},
    onEditPromiseClick: () -> Unit = {},
    onViewTermsClick: () -> Unit = {}
) {
    val colors = ZenTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .systemBarsPadding()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount > 40f) {
                        change.consume()
                        onBackClick()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ZenGoldHeader(onBackClick = onBackClick, onMenuClick = onMenuClick)

            Spacer(modifier = Modifier.height(20.rdp))

            Column(
                modifier = Modifier
                    .padding(horizontal = ScreenMargin)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.rdp)
            ) {
                ScreenTimeCard(
                    dailyAverageMinutes = dailyAverageMinutes,
                    promiseHours = promiseHours,
                    weeklyPromiseStatus = weeklyPromiseStatus,
                    daysUntilUnlock = daysUntilUnlock,
                    daysLeftThisWeek = daysLeftThisWeek
                )

                ForecastCard(
                    forecastPercent = forecastPercent,
                    forecastMonthlyAmount = forecastMonthlyAmount
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    GoldInvestedRow(gold = goldInvested, changePercent = goldChangePercent)
                    ViewAllPillButton(
                        onClick = onViewAllClick,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }

                GoldUnlockDisclaimer(
                    daysUntilUnlock = daysUntilUnlock,
                    onViewTermsClick = onViewTermsClick
                )
            }

            Spacer(modifier = Modifier.height(24.rdp))

            Column(
                modifier = Modifier
                    .padding(horizontal = ScreenMargin)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.rdp)
            ) {
                InvestGoldButton(unlocked = investGoldUnlocked, onClick = onInvestGoldClick)
                EditPromiseButton(onClick = onEditPromiseClick)
            }

            Spacer(modifier = Modifier.height(16.rdp))

            ZenGoldPageDots(onSwipeLeft = onBackClick)

            Spacer(modifier = Modifier.height(20.rdp))
        }
    }
}

// ── Header ────────────────────────────────────────────────────────

@Composable
private fun ZenGoldHeader(onBackClick: () -> Unit, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .padding(top = 12.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = "Back",
            modifier = Modifier
                .size(28.rdp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onBackClick
                ),
            colorFilter = ColorFilter.tint(colorResource(R.color.zen_900))
        )

        Text(
            text = "ZEN GOLD",
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 24.rsp,
            letterSpacing = (-0.48).sp,
            color = colorResource(R.color.zen_900),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Image(
            painter = painterResource(R.drawable.ic_hamburger_menu),
            contentDescription = "Menu",
            modifier = Modifier
                .width(29.rdp)
                .height(17.5.rdp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onMenuClick
                )
        )
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
    daysLeftThisWeek: Int
) {
    val colors = ZenTheme.colors
    val topBorder = colorResource(R.color.zen_700)
    val hrs = dailyAverageMinutes / 60
    val mins = dailyAverageMinutes % 60
    val topBorderWidth = 3.rdp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.rdp))
            .background(colors.bgPrimary)
            .drawBehind {
                drawLine(
                    color = topBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = topBorderWidth.toPx()
                )
            }
            .padding(horizontal = 16.rdp, vertical = 16.rdp)
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

        Spacer(modifier = Modifier.height(8.rdp))

        Row(verticalAlignment = Alignment.CenterVertically) {
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
                color = topBorder
            )
        }

        Spacer(modifier = Modifier.height(4.rdp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "%02d".format(hrs),
                fontFamily = DepartureMono,
                fontSize = 31.5.rsp,
                letterSpacing = (-2.5).sp,
                color = colorResource(R.color.ink_soft)
            )
            Text(
                text = " HRS  ",
                fontFamily = DepartureMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 7.rsp,
                color = colorResource(R.color.ink_soft),
                modifier = Modifier.padding(bottom = 6.rdp)
            )
            Text(
                text = "%02d".format(mins),
                fontFamily = DepartureMono,
                fontSize = 33.6.rsp,
                letterSpacing = (-2.7).sp,
                color = colorResource(R.color.ink_soft)
            )
            Text(
                text = " MINS",
                fontFamily = DepartureMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 6.rsp,
                color = colorResource(R.color.ink_soft),
                modifier = Modifier.padding(bottom = 6.rdp)
            )
        }

        Spacer(modifier = Modifier.height(12.rdp))

        WeeklyPromiseBars(weeklyPromiseStatus)

        Spacer(modifier = Modifier.height(12.rdp))

        // Two-tone caption: bold lead, regular tail — copy as authored in Figma (the
        // "7 hrs" here doesn't match "My promise - ${promiseHours}Hrs/day" above;
        // reproduced as-is, not reconciled).
        BasicText(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append("$daysUntilUnlock more days under 7 hrs opens gold pay, ")
                }
                append("and you have $daysLeftThisWeek days left this week to do it.")
            },
            style = TextStyle(
                fontFamily = Geist,
                fontSize = 11.rsp,
                letterSpacing = (-0.22).sp,
                color = colors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.rdp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.rdp))
                .background(colorResource(R.color.promise_badge_bg))
                .padding(horizontal = 10.rdp, vertical = 5.rdp)
        ) {
            Text(
                text = "IN PROGRESS",
                fontFamily = DepartureMono,
                fontSize = 9.5.rsp,
                letterSpacing = (-0.19).sp,
                color = PromiseKeptGreen
            )
        }
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.rdp)
        ) {
            status.take(7).forEach { kept ->
                Box(
                    modifier = Modifier
                        .weight(1f)
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
            }
        }
        Spacer(modifier = Modifier.height(8.rdp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dayLabels.forEach { label ->
                Text(label, fontFamily = Geist, fontSize = 12.rsp, letterSpacing = (-0.24).sp, color = Color.Black)
            }
        }
    }
}

// ── Forecast card ─────────────────────────────────────────────────

@Composable
private fun ForecastCard(forecastPercent: Int, forecastMonthlyAmount: Int) {
    val colors = ZenTheme.colors
    val topBorder = colorResource(R.color.zen_700)
    val zenGreen900 = colorResource(R.color.zen_900)
    val topBorderWidth = 1.rdp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.rdp))
            .background(colors.bgPrimary.copy(alpha = 0.71f))
            .drawBehind {
                drawLine(
                    color = topBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = topBorderWidth.toPx()
                )
            }
            .padding(20.rdp)
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
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(4.rdp))
            Text(
                text = "(2026)",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 10.3.rsp,
                color = Color.Black.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(12.rdp))

        Text(
            text = "Expect your Gold rise by $forecastPercent% as the Christmas approach",
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.2.rsp,
            lineHeight = 26.rsp,
            letterSpacing = (-0.6).sp,
            color = zenGreen900
        )

        Spacer(modifier = Modifier.height(8.rdp))

        Text(
            text = "Invest an extra ₹$forecastMonthlyAmount/month & watch the magic.",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 13.rsp,
            letterSpacing = (-0.13).sp,
            color = topBorder
        )

        Spacer(modifier = Modifier.height(20.rdp))

        Box(modifier = Modifier.fillMaxWidth()) {
            ForecastChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.rdp)
            )
            Column(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalAlignment = Alignment.End
            ) {
                Text("Forecast", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.rsp, letterSpacing = (-0.28).sp, color = Color.Black)
                Text("₹$forecastMonthlyAmount/mo avg", fontFamily = DepartureMono, fontSize = 9.5.rsp, letterSpacing = (-0.19).sp, color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(4.rdp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Jul", "Aug", "Sep", "Oct", "Nov", "Dec").forEach { month ->
                Text(month, fontFamily = Geist, fontSize = 10.3.rsp, letterSpacing = (-0.21).sp, color = colorResource(R.color.chart_axis_label))
            }
        }
    }
}

/**
 * The actual/forecast gold-price curve. Figma's own vector data for this (a solid
 * "recent" segment and a dashed "forecast" segment meeting at a marker) is
 * reproduced here at the exact relative positions and colors from the source SVGs
 * (nodes 2026:1722/1723/1724 area) rather than as static images, since it's a
 * shape fully described by those control points — not illustration we don't have
 * the data for.
 */
@Composable
private fun ForecastChart(modifier: Modifier = Modifier) {
    val gridColorLight = colorResource(R.color.chart_grid_light).copy(alpha = 0.25f)
    val gridColorDark = colorResource(R.color.chart_grid_dark).copy(alpha = 0.8f)
    val solidGreen = colorResource(R.color.gold_delta_text)
    val dashedGreen = colorResource(R.color.gold_delta_text).copy(alpha = 0.5f)
    val dotColor = colorResource(R.color.amber_500)
    val strokeWidthDp = 3.rdp

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 6 evenly spaced month grid lines — first 3 lighter, last 3 darker, per source.
        for (i in 0..5) {
            val x = w * (i / 5f)
            drawLine(
                color = if (i < 3) gridColorLight else gridColorDark,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 0.86.dp.toPx()
            )
        }

        val junctionX = w * 0.41f

        // Solid "recent" line: shallow valley, flat at both ends (~38% down from top).
        val solidPath = Path().apply {
            moveTo(0f, h * 0.38f)
            cubicTo(
                w * 0.15f, h * 0.46f,
                w * 0.28f, h * 0.50f,
                junctionX, h * 0.46f
            )
        }
        drawPath(
            solidPath,
            color = solidGreen,
            style = Stroke(width = strokeWidthDp.toPx(), cap = StrokeCap.Round)
        )

        // Dashed "forecast" line: rises from the junction to the top-right.
        val dashedPath = Path().apply {
            moveTo(junctionX, h * 0.73f)
            cubicTo(
                w * 0.6f, h * 0.55f,
                w * 0.8f, h * 0.30f,
                w, h * 0.14f
            )
        }
        drawPath(
            dashedPath,
            color = dashedGreen,
            style = Stroke(
                width = strokeWidthDp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
            )
        )

        // "Today" marker at the junction.
        drawCircle(color = dotColor, radius = 8.dp.toPx(), center = Offset(junctionX, h * 0.55f))
    }
}

// ── Buttons & footer ──────────────────────────────────────────────

@Composable
private fun ViewAllPillButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(colorResource(R.color.ink_surface))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 14.rdp, vertical = 8.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "View all →",
            fontFamily = Geist,
            fontSize = 14.9.rsp,
            letterSpacing = (-0.3).sp,
            color = Color.White
        )
    }
}

@Composable
private fun GoldUnlockDisclaimer(daysUntilUnlock: Int, onViewTermsClick: () -> Unit) {
    val colors = ZenTheme.colors
    val unlockOutOf = 7
    val unlockAt = unlockOutOf - daysUntilUnlock

    BasicText(
        text = buildAnnotatedString {
            append("Invest gold opens at $unlockAt of $unlockOutOf days under your Promise. Stay under tomorrow and it opens then. ")
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
        },
        style = TextStyle(
            fontFamily = Geist,
            fontSize = 16.rsp,
            letterSpacing = (-0.16).sp,
            color = colors.textPrimary
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
            .height(47.rdp)
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

/**
 * This screen's own page-dots row — third dot active (Zen Gold is the rightmost
 * of the three home pages). Mirrors HomeScreen's PageDots; kept local rather than
 * shared since the two screens' navigation targets differ (Settings/Zen Gold vs.
 * Zen Gold/nothing) and the app has no existing shared nav-dots component to
 * extend into.
 */
@Composable
private fun ZenGoldPageDots(onSwipeLeft: () -> Unit) {
    val colors = ZenTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount > 40f) {
                        change.consume()
                        onSwipeLeft()
                    }
                }
            }
            .padding(vertical = 8.rdp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val active = i == 2
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.rdp)
                    .size(7.rdp)
                    .clip(CircleShape)
                    .background(
                        if (active) colors.textBrand
                        else colors.textPrimary.copy(alpha = 0.35f)
                    )
            )
        }
    }
}
