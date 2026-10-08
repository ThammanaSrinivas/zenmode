package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.ZenCheckInPreferences
import com.zenlauncher.zenmode.ui.components.ZenSheet
import com.zenlauncher.zenmode.ui.components.ZenSheetBody
import com.zenlauncher.zenmode.ui.components.ZenSheetTitle
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 12-hour, no leading zero -- "8:00 pm", matching how the rest of the app writes times. */
internal val CheckInTimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

/**
 * Picks when the evening check-in card appears.
 *
 * Half-hour slots rather than a free clock: the card is a nudge, not an alarm, and a tidy
 * set of pills is one tap where a spinner is several. The window is late afternoon to just
 * before midnight ([ZenCheckInPreferences.earliest]/[ZenCheckInPreferences.latest]) -- a
 * "check-in" at 06:00 would report on a day that hasn't happened.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CheckInTimeSheet(
    current: LocalTime,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ZenTheme.colors
    // Counted in minutes past midnight, not by stepping a LocalTime: LocalTime.plusMinutes
    // wraps round midnight, so 23:30 + 30 is 00:00, which is never "after" the 23:30 bound --
    // a loop on that condition never ends and takes the app down with an OutOfMemoryError.
    val slots = remember {
        val first = ZenCheckInPreferences.earliest
        val last = ZenCheckInPreferences.latest
        val firstMinute = first.hour * 60 + first.minute
        val lastMinute = last.hour * 60 + last.minute
        (firstMinute..lastMinute step 30).map { LocalTime.of(it / 60, it % 60) }
    }

    ZenSheet(onDismiss = onDismiss) {
        ZenSheetTitle("Check-in time")
        ZenSheetBody(
            "Your evening card appears at this time, once a day, the next time you're on home."
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.rdp),
            verticalArrangement = Arrangement.spacedBy(8.rdp)
        ) {
            slots.forEach { slot ->
                val selected = slot == current
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (selected) colors.actionPrimary else Color.Transparent)
                        .border(
                            1.dp,
                            if (selected) Color.Transparent else colors.borderOutline,
                            RoundedCornerShape(percent = 50)
                        )
                        .clickable(
                            onClickLabel = "Check in at ${slot.format(CheckInTimeFormat)}",
                            role = Role.Button
                        ) { onPick(slot) }
                        .padding(horizontal = 14.rdp, vertical = 10.rdp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = slot.format(CheckInTimeFormat),
                        fontFamily = DepartureMono,
                        fontSize = 12.rsp,
                        color = if (selected) colors.actionPrimaryText else colors.textSecondary
                    )
                }
            }
        }
    }
}
