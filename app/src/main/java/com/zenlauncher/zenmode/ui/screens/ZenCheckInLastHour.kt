package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ZenCheckIn
import com.zenlauncher.zenmode.ZenCheckInCard
import com.zenlauncher.zenmode.ui.components.TodayCoin
import com.zenlauncher.zenmode.ui.components.ZenBufferRing
import com.zenlauncher.zenmode.ui.components.ZenButton
import com.zenlauncher.zenmode.ui.components.ZenButtonStyle
import com.zenlauncher.zenmode.ui.components.ZenStreakCoins
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * The last-hour card: **the win first, the hour second.**
 *
 * An earlier version of this screen led with a shattering coin and a countdown, and it was
 * wrong in a way worth recording. The user arriving here has kept their promise thirteen days
 * running; opening with destruction told them they were about to fail rather than that they
 * were nearly done. The order of this card is now the order the feeling should arrive in:
 *
 *  1. **The streak**, loud and plainly stated. This is the emotional anchor.
 *  2. **One status**, as a ring — the share of today's budget already spent, with the figure
 *     that matters spelled out beside it in words. No pair of numbers to reconcile.
 *  3. **One ask** the user can actually complete today.
 *  4. **The coins**, with today part-struck rather than missing.
 *  5. **The week**, demoted to a single quiet line beneath them, in the same gold language,
 *     so today's buffer and the weekly quota read as two scales of one mechanic.
 *  6. **A button with a job** — it locks the phone, which is the act that banks the day.
 *
 * Urgency is carried only by the ring's burnt-orange arc and nothing else. It is a second
 * note, never the loudest thing on screen.
 */
@Composable
internal fun ColumnScope.LastHourCheckInContent(
    card: ZenCheckInCard,
    onLockToday: () -> Unit,
    onSeeMyWeekClick: () -> Unit
) {
    val colors = ZenTheme.colors

    Spacer(modifier = Modifier.height(10.rdp))

    CelebrationLine(card)

    Spacer(modifier = Modifier.height(18.rdp))

    BufferStatus(card)

    Spacer(modifier = Modifier.height(16.rdp))

    Text(
        text = card.body,
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 18.rsp,
        lineHeight = 23.rsp,
        letterSpacing = (-0.5).sp,
        color = colors.textPrimary
    )

    Spacer(modifier = Modifier.height(20.rdp))

    StreakCoinsBlock(card)

    Spacer(modifier = Modifier.height(22.rdp))

    ZenButton(text = card.actionLabel, onClick = onLockToday)

    Spacer(modifier = Modifier.height(4.rdp))

    ZenButton(text = "See my week", onClick = onSeeMyWeekClick, style = ZenButtonStyle.Ghost)
}

/** The win, first and loudest. */
@Composable
private fun CelebrationLine(card: ZenCheckInCard) {
    val colors = ZenTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_streak_fire),
            contentDescription = null,
            modifier = Modifier.size(30.rdp)
        )
        Spacer(modifier = Modifier.width(9.rdp))
        Text(
            text = card.headline,
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 27.rsp,
            lineHeight = 31.rsp,
            letterSpacing = (-0.9).sp,
            color = colors.textPrimary
        )
    }
}

/**
 * One status, one number. The ring carries "how much of today is gone"; the sentence beside
 * it carries the only figure the user needs to act on.
 */
@Composable
private fun BufferStatus(card: ZenCheckInCard) {
    val colors = ZenTheme.colors
    val shape = RoundedCornerShape(16.rdp)
    val spent = card.todayMinutes
    val budget = (card.promiseHours * 60L).coerceAtLeast(1L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.accentDeduct.copy(alpha = 0.07f))
            .border(1.dp, colors.accentDeduct.copy(alpha = 0.22f), shape)
            .padding(horizontal = 16.rdp, vertical = 15.rdp)
            .semantics {
                contentDescription = ZenCheckIn.bufferSpoken(card.minutesLeft, card.promiseHours)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        ZenBufferRing(
            usedFraction = spent.toFloat() / budget.toFloat(),
            track = colors.accentDeduct.copy(alpha = 0.16f),
            used = colors.accentDeduct,
            modifier = Modifier.size(58.rdp)
        )

        Spacer(modifier = Modifier.width(15.rdp))

        Column(verticalArrangement = Arrangement.spacedBy(2.rdp)) {
            Text(
                text = ZenCheckIn.bufferCompact(card.minutesLeft),
                fontFamily = DepartureMono,
                fontSize = 30.rsp,
                lineHeight = 32.rsp,
                letterSpacing = (-1.2).sp,
                color = colors.accentDeduct
            )
            Text(
                text = "of buffer left today",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.rsp,
                letterSpacing = (-0.12).sp,
                color = colors.textSecondary
            )
        }
    }
}

/**
 * What has been put away, and the single quiet line about the week. The week sits directly
 * under the coins and speaks the same gold so the two scales read as one mechanic rather than
 * two unrelated widgets.
 */
@Composable
private fun StreakCoinsBlock(card: ZenCheckInCard) {
    val colors = ZenTheme.colors

    Column {
        ZenStreakCoins(
            banked = (card.streak.days - 1).coerceAtLeast(0),
            today = TodayCoin.InProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(26.rdp)
                .semantics {
                    contentDescription = ZenCheckIn.coinsSpoken(card.streak.days)
                }
        )

        Spacer(modifier = Modifier.height(9.rdp))

        Text(
            text = "Today — almost banked",
            fontFamily = DepartureMono,
            fontSize = 9.rsp,
            letterSpacing = 0.5.sp,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(7.rdp))

        Text(
            text = ZenCheckIn.weekQuotaLine(card.streak),
            fontFamily = Geist,
            fontSize = 11.5.rsp,
            letterSpacing = (-0.11).sp,
            color = colors.textMuted
        )
    }
}
