package com.zenlauncher.zenmode.ui.screens

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.graphicsLayer
import com.zenlauncher.zenmode.ZenSound
import com.zenlauncher.zenmode.Sfx
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.LinkAnnotation
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
import com.zenlauncher.zenmode.GoldPromisePeriod
import com.zenlauncher.zenmode.PromiseUnit
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ZenGoldPromiseState
import com.zenlauncher.zenmode.ui.components.HomePage
import com.zenlauncher.zenmode.ui.components.LocalZenClock
import com.zenlauncher.zenmode.ui.components.MoodBackdrop
import com.zenlauncher.zenmode.ui.components.PinnedPageFooter
import com.zenlauncher.zenmode.ui.components.PromiseBrokenGray
import com.zenlauncher.zenmode.ui.components.PromiseKeptGreen
import com.zenlauncher.zenmode.ui.components.PromiseUndecidedGray
import com.zenlauncher.zenmode.ui.components.PromiseUnitBars
import com.zenlauncher.zenmode.ui.components.GlyphKind
import com.zenlauncher.zenmode.ui.components.ZenGlyph
import com.zenlauncher.zenmode.recap.weekRangeLabel
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
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
// The Weekly/Monthly promise card (weekly/monthly params) is real, on-device data — see
// ZenGoldPromise.kt. The gold-invested balance and price forecast below it still need the
// real Gold Streak backend that doesn't exist yet (see
// zenmode_core_private/docs/plans/2026-07-gold-streak*.md), so those stay on
// AppConstants.PLACEHOLDER_*.

// Figma node 2001:1481's margin (see HomeScreen.kt) — this screen shares it,
// not the app-wide Spacing.screenMargin, since it's the same v3 frame width.
private val ScreenMargin: Dp @Composable get() = 30.rdp

/** Every Zen Gold sub-screen's way back: header label and bottom button alike. */
internal const val BACK_TO_ZEN_GOLD = "Back to Zen Gold"

@Composable
fun ZenGoldScreen(
    // Real, on-device Weekly/Monthly promise tracking — see ZenGoldPromise.kt. Gold pay's
    // actual unlock gate is always [weekly], regardless of which tab the card is showing.
    weekly: ZenGoldPromiseState = ZenGoldPromiseState(),
    monthly: ZenGoldPromiseState = ZenGoldPromiseState(period = GoldPromisePeriod.MONTHLY),
    /**
     * The week the Weekly tab shows. Equal to [weekly] at offset 0; a finished week when the
     * user has paged back (Pro). Kept separate so gold pay's gate stays on the live week
     * however far back the card is looking.
     */
    displayedWeek: ZenGoldPromiseState = weekly,
    weekOffset: Int = 0,
    /** How far back recorded history reaches, as a negative offset; 0 means no history. */
    earliestWeekOffset: Int = 0,
    onWeekOffsetChange: (Int) -> Unit = {},
    /** The balance from [com.zenlauncher.zenmode.GoldLedger], as printed after the ₹. */
    goldInvested: String = "0",
    /** Gain or loss on the balance; null while there's no live gold price to measure it by. */
    goldChangePercent: Int? = null,
    forecastPercent: Int = AppConstants.PLACEHOLDER_FORECAST_PERCENT,
    forecastMonthlyAmount: Int = AppConstants.PLACEHOLDER_FORECAST_MONTHLY_AMOUNT,
    isPro: Boolean = false,
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
                PromiseScreenTimeCard(
                    weekly = weekly,
                    monthly = monthly,
                    isPro = isPro,
                    displayedWeek = displayedWeek,
                    weekOffset = weekOffset,
                    earliestWeekOffset = earliestWeekOffset,
                    onWeekOffsetChange = onWeekOffsetChange
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
                    daysUntilUnlock = (AppConstants.PROMISE_DAYS_TO_UNLOCK - weekly.unitsKept).coerceAtLeast(0),
                    unlocked = weekly.goalMet,
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
            },
            horizontalMargin = ScreenMargin,
            primary = { InvestGoldButton(unlocked = weekly.goalMet, onClick = onInvestGoldClick) },
            secondary = { EditPromiseButton(onClick = onEditPromiseClick) }
        )
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
    // The app's clock, not the system's: screenshot goldens pin it, so the axis and headline
    // don't roll over with the month the suite happens to run in.
    val clock = LocalZenClock.current
    val today = remember(clock) { LocalDate.now(clock) }
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
                text = "(${today.year})",
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
    // Only "View T&C" is tappable -- a whole-paragraph clickable also fired on scroll swipes.
    val termsLink = LinkAnnotation.Clickable(
        tag = "terms",
        styles = TextLinkStyles(
            SpanStyle(
                fontWeight = FontWeight.SemiBold,
                color = PromiseKeptGreen,
                textDecoration = TextDecoration.Underline
            )
        )
    ) { onViewTermsClick() }

    BasicText(
        text = if (unlocked) {
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append("Gold pay is open.")
                }
                append(" You pick the quantity, we never pick it for you, and we take nothing from it. ")
                withLink(termsLink) {
                    append("View T&C")
                }
            }
        } else {
            buildAnnotatedString {
                val staySentence = if (daysUntilUnlock <= 1) {
                    "Stay under tomorrow and it opens then."
                } else {
                    "Stay under $daysUntilUnlock more days and it opens then."
                }
                append("Invest gold opens at $unlockAt of $unlockOutOf days under your Promise. $staySentence ")
                withLink(termsLink) {
                    append("View T&C")
                }
            }
        },
        style = TextStyle(
            fontFamily = Geist,
            fontSize = 14.rsp,
            lineHeight = 19.rsp,
            letterSpacing = (-0.14).sp,
            color = bodyColor
        )
    )
}

@Composable
private fun InvestGoldButton(unlocked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
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
            .fillMaxHeight()
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
