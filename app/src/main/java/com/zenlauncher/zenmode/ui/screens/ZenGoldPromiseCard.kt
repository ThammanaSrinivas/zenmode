package com.zenlauncher.zenmode.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.GoldPromisePeriod
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.Sfx
import com.zenlauncher.zenmode.ZenGoldPromiseState
import com.zenlauncher.zenmode.ZenSound
import com.zenlauncher.zenmode.recap.weekRangeLabel
import com.zenlauncher.zenmode.ui.components.GlyphKind
import com.zenlauncher.zenmode.ui.components.PromiseBrokenGray
import com.zenlauncher.zenmode.ui.components.PromiseKeptGreen
import com.zenlauncher.zenmode.ui.components.PromiseUndecidedGray
import com.zenlauncher.zenmode.ui.components.PromiseUnitBars
import com.zenlauncher.zenmode.ui.components.ZenGlyph
import com.zenlauncher.zenmode.ui.components.taperedBorder
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

// ── My Screen Time card ───────────────────────────────────────────

// SOURCE OF TRUTH: design tokens — values live in colors.xml (never a bare
// Color(0x...) literal here).

@Composable
internal fun PromiseScreenTimeCard(
    weekly: ZenGoldPromiseState,
    monthly: ZenGoldPromiseState,
    isPro: Boolean,
    /** The week the Weekly tab is actually showing — [weekly] itself at offset 0. */
    displayedWeek: ZenGoldPromiseState = weekly,
    weekOffset: Int = 0,
    /** How far back history reaches, as a negative offset. 0 means there's nothing behind. */
    earliestWeekOffset: Int = 0,
    onWeekOffsetChange: (Int) -> Unit = {}
) {
    var isMonthly by remember { mutableStateOf(false) }
    val state = if (isMonthly) monthly else displayedWeek
    // On the current week the badge and happy fill mean "gold pay is open", which only the
    // live weekly result decides — on either tab, so the card never reads UNLOCKED over a
    // locked Invest Gold button. Looking back at a finished week it reports *that* week's
    // own verdict instead, which is the only thing the user is asking about there.
    val historic = !isMonthly && displayedWeek.isHistoric
    val goldPayOpen = weekly.goalMet
    val cardWon = if (historic) displayedWeek.goalMet else goldPayOpen

    val colors = ZenTheme.colors
    val rule = colorResource(R.color.zen_700)
    val radius = 16.rdp
    val hrs = state.dailyAverageMinutes / 60
    val mins = state.dailyAverageMinutes % 60

    // Node 2026:1449 — a 3dp rule on the top edge that tapers round the corners, and the
    // status tab tucked into the bottom-right corner, flush with the card's own curve.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(if (cardWon) colors.statsCardFillHappy else colors.bgPrimary)
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
                WeeklyMonthlyToggle(
                    isPro = isPro,
                    isMonthly = isMonthly,
                    onToggleChange = { isMonthly = it }
                )
            }

            // Which week this is, and (Pro) the way back through the ones before it. Monthly
            // spans several weeks at once, so it has no single number to show — and neither
            // does a state that hasn't been worked out yet (the page's first frame, before
            // history loads): no number at all beats a "WEEK 00" that flashes and changes.
            if (!isMonthly && displayedWeek.weekNumber > 0) {
                Spacer(modifier = Modifier.height(12.rdp))
                WeekNavigator(
                    weekNumber = displayedWeek.weekNumber,
                    weekStart = displayedWeek.weekStart,
                    isPro = isPro,
                    weekOffset = weekOffset,
                    earliestWeekOffset = earliestWeekOffset,
                    onWeekOffsetChange = onWeekOffsetChange
                )
            }

            Spacer(modifier = Modifier.height(10.rdp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Average  •  " + when {
                        isMonthly -> "This Month"
                        state.isHistoric -> "That Week"
                        else -> "This Week"
                    },
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
                    text = "${state.promiseHours}Hrs/day",
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

            PromiseUnitBars(state.units)

            Spacer(modifier = Modifier.height(10.rdp))

            BasicText(
                text = promiseStatusMessage(state),
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
            unlocked = cardWon,
            historic = historic,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

/**
 * "WEEK 41" plus, for Pro, the way back through finished weeks.
 *
 * The number is the ISO week of the year, so it lines up with a calendar app or a
 * spreadsheet's WEEKNUM rather than counting from install. Free users see the current week's
 * number — the history is the Pro part, gated with the same wiggle-and-toast the Weekly /
 * Monthly toggle already uses, so a locked tap is refused the same way twice on one card.
 */
@Composable
private fun WeekNavigator(
    weekNumber: Int,
    weekStart: LocalDate?,
    isPro: Boolean,
    weekOffset: Int,
    earliestWeekOffset: Int,
    onWeekOffsetChange: (Int) -> Unit
) {
    val colors = ZenTheme.colors
    val rule = colorResource(R.color.zen_700)
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val wiggle = remember { Animatable(0f) }

    // There's always somewhere to go back to for a Pro user with history; a free user is
    // offered the tap so the Pro value is discoverable, and told why it didn't move.
    val hasHistory = earliestWeekOffset < 0
    val canGoBack = weekOffset > earliestWeekOffset
    val canGoForward = weekOffset < 0

    fun refuse() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        ZenSound.play(Sfx.ERROR)
        scope.launch {
            for (angle in listOf(-10f, 8f, -5f, 3f, 0f)) wiggle.animateTo(angle, tween(55))
        }
        Toast.makeText(context, "Past weeks are a Pro thing.", Toast.LENGTH_SHORT).show()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer { rotationZ = wiggle.value }
    ) {
        if (hasHistory) {
            WeekNavArrow(
                kind = GlyphKind.Back,
                label = "Previous week",
                enabled = !isPro || canGoBack,
                tint = if (isPro && !canGoBack) colors.textMuted else rule,
                onClick = { if (!isPro) refuse() else onWeekOffsetChange(weekOffset - 1) }
            )
            Spacer(modifier = Modifier.width(2.rdp))
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.US, "WEEK %02d", weekNumber),
                    fontFamily = DepartureMono,
                    fontSize = 11.rsp,
                    letterSpacing = 0.4.sp,
                    color = rule
                )
                if (hasHistory && !isPro) {
                    Spacer(modifier = Modifier.width(5.rdp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(PromiseKeptGreen)
                            .padding(horizontal = 3.rdp, vertical = 1.rdp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PRO", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 5.6.rsp, color = Color.White)
                    }
                }
            }
            if (weekStart != null) {
                Text(
                    text = weekRangeLabel(weekStart),
                    fontFamily = Geist,
                    fontSize = 9.rsp,
                    lineHeight = 11.rsp,
                    letterSpacing = (-0.09).sp,
                    color = colors.textMuted
                )
            }
        }

        if (hasHistory) {
            Spacer(modifier = Modifier.width(2.rdp))
            WeekNavArrow(
                kind = GlyphKind.Chevron,
                label = "Next week",
                enabled = canGoForward,
                tint = if (canGoForward) rule else colors.textMuted,
                onClick = { onWeekOffsetChange(weekOffset + 1) }
            )
        }
    }
}

/** 32dp touch slab around a 14dp drawn glyph — small mark, finger-sized target. */
@Composable
private fun WeekNavArrow(
    kind: GlyphKind,
    label: String,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.rdp)
            .clip(RoundedCornerShape(percent = 50))
            .then(
                if (enabled) {
                    Modifier.clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        ZenGlyph(
            kind = kind,
            color = tint.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(14.rdp)
        )
    }
}

/** The card's bottom line, tailored to the period. Only WEEKLY talks about gold pay —
 * Monthly is a streak and never gates it (see [ZenGoldPromiseState.goalMet]). */
private fun promiseStatusMessage(state: ZenGoldPromiseState) = when (state.period) {
    GoldPromisePeriod.WEEKLY -> when {
        // A finished week is a result, not a goal: it never talks about what's still
        // possible, and never about gold pay, which is always settled on the live week.
        state.isHistoric && state.goalMet -> buildAnnotatedString {
            append("You kept ${days(state.unitsKept)} of ${AppConstants.PROMISE_DAYS_PER_WEEK} that week. ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append("The week was yours.")
            }
        }
        state.isHistoric -> buildAnnotatedString {
            append("${days(state.unitsKept)} under the line that week, ")
            append("${AppConstants.PROMISE_DAYS_TO_UNLOCK} were needed. ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append("It's on the record, not on you.")
            }
        }
        state.goalMet -> buildAnnotatedString {
            append("You cleared the line with ${days(state.unitsKept)} under. ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append("Gold pay stays open until Sunday midnight.")
            }
        }
        state.goalOutOfReach -> buildAnnotatedString {
            append("Gold pay can't open this week any more. ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append("A fresh week starts Monday.")
            }
        }
        else -> {
            val daysUntilUnlock = (AppConstants.PROMISE_DAYS_TO_UNLOCK - state.unitsKept).coerceAtLeast(0)
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append("${days(daysUntilUnlock)} more under ${state.promiseHours} hrs opens gold pay, ")
                }
                append("and you have ${days(state.unitsRemaining)} left this week to do it.")
            }
        }
    }
    GoldPromisePeriod.MONTHLY -> when {
        state.goalMet -> buildAnnotatedString {
            append("Not a single week missed. ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append("Your monthly streak is intact.")
            }
        }
        state.unitsMissed > 0 -> buildAnnotatedString {
            append("Not every week went as planned this month. ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append("Stay under ${state.promiseHours} hrs/day this week to begin again.")
            }
        }
        else -> buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append("Stay under ${state.promiseHours} hrs/day this week ")
            }
            append("to start your monthly streak.")
        }
    }
}

private fun days(count: Int) = if (count == 1) "1 day" else "$count days"

private val StatusTabHeight: Dp @Composable get() = 22.rdp

/**
 * UNLOCKED / IN PROGRESS tab sitting in the card's bottom-right corner — or, for a week
 * being looked back at, that week's finished verdict. A past week is never "in progress".
 */
@Composable
private fun StatusTab(unlocked: Boolean, historic: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(105.rdp)
            .height(StatusTabHeight)
            .clip(RoundedCornerShape(topStart = 8.rdp))
            .background(if (unlocked) PromiseKeptGreen else colorResource(R.color.promise_badge_bg)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                historic && unlocked -> "WEEK KEPT"
                historic -> "WEEK MISSED"
                unlocked -> "UNLOCKED"
                else -> "IN PROGRESS"
            },
            fontFamily = DepartureMono,
            fontSize = 9.5.rsp,
            letterSpacing = 0.4.sp,
            color = if (unlocked) Color.White else PromiseKeptGreen
        )
    }
}

@Composable
private fun WeeklyPromiseLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(3.rdp)) {
        WeeklyPromiseLegendRow(color = PromiseKeptGreen, label = "Promise within limits")
        WeeklyPromiseLegendRow(color = PromiseBrokenGray, label = "Promise Broken")
        WeeklyPromiseLegendRow(color = PromiseUndecidedGray, label = "Not decided yet")
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
            // Explicit line height: the inherited body style (~24sp) triples each row's height.
            lineHeight = 9.rsp,
            letterSpacing = (-0.075).sp,
            color = ZenTheme.colors.textPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun WeeklyMonthlyToggle(isPro: Boolean, isMonthly: Boolean, onToggleChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val wiggle = remember { androidx.compose.animation.core.Animatable(0f) }

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
                .background(if (!isMonthly) Color.White else Color.Transparent)
                .then(if (!isMonthly) Modifier.border(0.5.dp, colorResource(R.color.toggle_pill_border), RoundedCornerShape(percent = 50)) else Modifier)
                .clickable { onToggleChange(false) }
                .padding(horizontal = 12.rdp, vertical = 4.rdp),
            contentAlignment = Alignment.Center
        ) {
            Text("Weekly", fontFamily = Geist, fontWeight = if (!isMonthly) FontWeight.Medium else FontWeight.Normal, fontSize = 10.rsp, color = if (!isMonthly) Color.Black else colorResource(R.color.toggle_inactive_text))
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(if (isMonthly) Color.White else Color.Transparent)
                .then(if (isMonthly) Modifier.border(0.5.dp, colorResource(R.color.toggle_pill_border), RoundedCornerShape(percent = 50)) else Modifier)
                .clickable {
                    if (isPro) {
                        onToggleChange(true)
                    } else {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        ZenSound.play(Sfx.ERROR)
                        scope.launch {
                            for (angle in listOf(-14f, 12f, -8f, 5f, 0f)) wiggle.animateTo(angle, androidx.compose.animation.core.tween(55))
                        }
                        Toast.makeText(context, "You need to be Pro to access this.", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 14.rdp, vertical = 4.rdp),
            contentAlignment = Alignment.Center
        ) {
            Text("Monthly", fontFamily = Geist, fontWeight = if (isMonthly) FontWeight.Medium else FontWeight.Normal, fontSize = 10.rsp, color = if (isMonthly) Color.Black else colorResource(R.color.toggle_inactive_text))
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.rdp, y = (-2).rdp)
                    .graphicsLayer { rotationZ = wiggle.value }
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PromiseKeptGreen)
                    .padding(horizontal = 3.rdp, vertical = 1.rdp),
                contentAlignment = Alignment.Center
            ) {
                Text("PRO", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 5.6.rsp, color = Color.White)
            }
        }
    }
}
