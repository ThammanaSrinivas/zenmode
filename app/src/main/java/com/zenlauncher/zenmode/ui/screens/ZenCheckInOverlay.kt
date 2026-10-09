package com.zenlauncher.zenmode.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.PromiseUnit
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ZenCheckInCard
import com.zenlauncher.zenmode.ZenCheckInKind
import com.zenlauncher.zenmode.ui.components.PromiseKeptGreen
import com.zenlauncher.zenmode.ui.components.PromiseUnitBars
import com.zenlauncher.zenmode.ui.components.ZenButton
import com.zenlauncher.zenmode.ui.components.ZenButtonStyle
import com.zenlauncher.zenmode.ui.components.TodayCoin
import com.zenlauncher.zenmode.ui.components.ZenConfetti
import com.zenlauncher.zenmode.ui.components.ZenStreakCoins
import com.zenlauncher.zenmode.ui.components.taperedBorder
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * The daily check-in card, drawn over a blurred Home (the caller applies
 * [com.zenlauncher.zenmode.ui.components.zenOverlayBlur]).
 *
 * One card, three readings — see [ZenCheckInKind]:
 *  · **Celebration** (evening, still under the promise) — confetti in the brand's own
 *    gradient stops, and the plain statement that the screen limit is under promise.
 *  · **Encouragement** (evening, already over) — no confetti, and the week-not-the-day rule
 *    said out loud, because that's exactly the moment a user assumes they've lost the streak.
 *  · **Last hour** — the nudge while there's still a promise left to keep.
 *
 * The week strip is the same component the Zen Gold card draws
 * ([com.zenlauncher.zenmode.ui.components.PromiseUnitBars]), so "5 of 7" means the same thing
 * on both screens.
 */
@Composable
fun ZenCheckInOverlay(
    card: ZenCheckInCard,
    /** This week's days, as [com.zenlauncher.zenmode.ZenGoldPromise.weekly] computes them. */
    weekUnits: List<PromiseUnit>,
    onDismiss: () -> Unit,
    onSeeMyWeekClick: () -> Unit = {},
    /** "Lock in today" — the act that actually banks the day, not just a way to close. */
    onLockToday: () -> Unit = onDismiss,
    /**
     * Opens the streak's share card. Offered on the celebration only — the evening the promise
     * was kept is the moment worth posting; a warning or an over-promise day isn't.
     */
    onShareClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val radius = 20.rdp
    // The last hour is the one card that isn't green: ember is the app's "this is being
    // deducted" hue, and the rule, the eyebrow and the fracture all share it so the card
    // reads as a warning at a glance, before a word of it is read.
    val urgent = card.kind == ZenCheckInKind.LAST_HOUR
    val accent = if (urgent) colors.accentDeduct else colorResource(R.color.zen_700)

    BackHandler { onDismiss() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgPrimary.copy(alpha = 0.42f))
            // Tapping the scrim dismisses, the same as every other Home overlay.
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            )
            .systemBarsPadding()
            .padding(horizontal = 24.rdp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radius))
                .background(if (card.kind.confetti) colors.statsCardFillHappy else colors.bgPrimary)
                .taperedBorder(accent, radius, top = 3.rdp)
                // Swallow taps so they don't reach the dismiss scrim.
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
                .padding(horizontal = 22.rdp, vertical = 22.rdp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.kind.eyebrow,
                    fontFamily = DepartureMono,
                    fontSize = 9.5.rsp,
                    letterSpacing = 0.6.sp,
                    color = if (urgent) accent else PromiseKeptGreen,
                    modifier = Modifier.weight(1f)
                )
                if (onShareClick != null && card.kind == ZenCheckInKind.CELEBRATION) {
                    // A 48dp target that draws as a quiet glyph, so the card's one loud thing
                    // stays the streak.
                    val label = "Share my streak"
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .offset(x = 12.dp, y = (-4).dp)
                            .clip(CircleShape)
                            .clickable(onClickLabel = label, role = Role.Button, onClick = onShareClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = label,
                            tint = PromiseKeptGreen,
                            modifier = Modifier.size(20.rdp)
                        )
                    }
                }
            }

            if (urgent) {
                LastHourCheckInContent(
                    card = card,
                    onLockToday = onLockToday,
                    onSeeMyWeekClick = onSeeMyWeekClick
                )
            } else {
                Spacer(modifier = Modifier.height(14.rdp))

                StreakHero(card)

                Spacer(modifier = Modifier.height(16.rdp))

                Text(
                    text = card.headline,
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.rsp,
                    lineHeight = 25.rsp,
                    letterSpacing = (-0.6).sp,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(10.rdp))

                Text(
                    text = card.body,
                    fontFamily = Geist,
                    fontSize = 13.rsp,
                    lineHeight = 18.rsp,
                    letterSpacing = (-0.13).sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(20.rdp))

                if (card.kind == ZenCheckInKind.CELEBRATION) {
                    BankedCoinsBlock(card)
                    Spacer(modifier = Modifier.height(18.rdp))
                }

                WeekStripBlock(card, weekUnits)

                Spacer(modifier = Modifier.height(22.rdp))

                ZenButton(text = card.actionLabel, onClick = onDismiss)

                Spacer(modifier = Modifier.height(4.rdp))

                ZenButton(
                    text = "See my week",
                    onClick = onSeeMyWeekClick,
                    style = ZenButtonStyle.Ghost
                )
            }
        }

        // Over the card, not behind it: a Canvas takes no pointer input, so the paper falls
        // across the whole card without stealing the buttons underneath.
        if (card.kind.confetti) {
            ZenConfetti(play = true, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * The streak, front and centre. ClashDisplay rather than DepartureMono for the headline
 * figure, matching Home's own streak stat (StreakStat) — this is the same number the flame
 * shows there, and the two must not look like different metrics. Every *supporting* number
 * below stays in DepartureMono.
 */
@Composable
private fun StreakHero(card: ZenCheckInCard) {
    val colors = ZenTheme.colors
    val days = card.streak.days

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_streak_fire),
            contentDescription = null,
            modifier = Modifier.size(38.rdp)
        )

        Spacer(modifier = Modifier.width(10.rdp))

        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.semantics(mergeDescendants = true) {}
        ) {
            Text(
                text = days.toString(),
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.rsp,
                lineHeight = 36.rsp,
                letterSpacing = (-1.4).sp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.width(4.rdp))
            Text(
                text = if (days == 1) "day kept" else "days kept",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 13.rsp,
                lineHeight = 13.rsp,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 4.rdp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // The time that matters for this card: what's left today, or what was spent.
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (card.minutesLeft > 0) "LEFT TODAY" else "OVER BY",
                fontFamily = DepartureMono,
                fontSize = 7.rsp,
                letterSpacing = 0.4.sp,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(3.rdp))
            Text(
                text = card.clockValue,
                fontFamily = DepartureMono,
                fontSize = 17.rsp,
                letterSpacing = (-0.6).sp,
                color = if (card.minutesLeft > 0) colors.textSecondary else colorResource(R.color.ember_700)
            )
        }
    }
}

@Composable
private fun WeekStripBlock(card: ZenCheckInCard, weekUnits: List<PromiseUnit>) {
    val colors = ZenTheme.colors
    val kept = card.streak.daysKeptThisWeek

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "THIS WEEK",
                fontFamily = DepartureMono,
                fontSize = 8.rsp,
                letterSpacing = 0.5.sp,
                color = colors.textMuted
            )
            Text(
                text = "$kept / ${AppConstants.PROMISE_DAYS_TO_UNLOCK} KEPT",
                fontFamily = DepartureMono,
                fontSize = 8.rsp,
                letterSpacing = 0.5.sp,
                color = if (kept >= AppConstants.PROMISE_DAYS_TO_UNLOCK) PromiseKeptGreen else colors.textMuted
            )
        }

        Spacer(modifier = Modifier.height(10.rdp))

        PromiseUnitBars(
            units = weekUnits,
            barHeight = 7.rdp,
            labelSize = 11.rsp,
            labelSpacing = (-0.2).sp,
            labelColor = colors.textSecondary
        )
    }
}

private val ZenCheckInKind.eyebrow: String
    get() = when (this) {
        ZenCheckInKind.CELEBRATION, ZenCheckInKind.ENCOURAGEMENT -> "DAILY CHECK-IN"
        ZenCheckInKind.LAST_HOUR -> "LAST HOUR"
    }

/** "1:05" of promise left, or how far past it the day went. */
private val ZenCheckInCard.clockValue: String
    get() {
        val minutes = if (minutesLeft > 0) minutesLeft else todayMinutes - promiseHours * 60L
        val safe = minutes.coerceAtLeast(0)
        return "%d:%02d".format(safe / 60, safe % 60)
    }

/**
 * The success state of the streak row: today struck and stamped. Shown on the evening card,
 * which is the moment the day is actually secured — the same coins the last-hour card shows
 * part-struck, finished.
 */
@Composable
private fun BankedCoinsBlock(card: ZenCheckInCard) {
    val colors = ZenTheme.colors
    Column {
        ZenStreakCoins(
            banked = (card.streak.days - 1).coerceAtLeast(0),
            today = TodayCoin.Banked,
            modifier = Modifier
                .fillMaxWidth()
                .height(26.rdp)
                .semantics { contentDescription = "${card.streak.days} days banked, today included" }
        )
        Spacer(modifier = Modifier.height(9.rdp))
        Text(
            text = "Today — banked",
            fontFamily = DepartureMono,
            fontSize = 9.rsp,
            letterSpacing = 0.5.sp,
            color = PromiseKeptGreen
        )
    }
}
