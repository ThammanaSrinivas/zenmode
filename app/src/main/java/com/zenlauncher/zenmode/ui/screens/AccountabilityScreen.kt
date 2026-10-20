package com.zenlauncher.zenmode.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.zenlauncher.zenmode.ui.theme.Spacing
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import com.zenlauncher.zenmode.AccountabilityUiState
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.components.StatsCardsRow
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountabilityScreen(
    uiState: AccountabilityUiState,
    onBackClick: () -> Unit,
    onCopyCode: (String) -> Unit,
    onBackToHomeClick: () -> Unit,
    onChangeBuddyConfirmed: () -> Unit,
    myLikes: Long = 0L,
    buddyLikes: Long = 0L,
    onLikeClick: () -> Unit = {},
    onSwitchToZenCircle: () -> Unit = {}
) {
    val colors = ZenTheme.colors
    var showChangeBuddyDialog by remember { mutableStateOf(false) }
    var offsetY by remember { mutableStateOf(0f) }

    if (showChangeBuddyDialog) {
        ChangeBuddyConfirmDialog(
            onConfirm = {
                showChangeBuddyDialog = false
                onChangeBuddyConfirmed()
            },
            onDismiss = { showChangeBuddyDialog = false }
        )
    }

    BackHandler(enabled = true) { onBackClick() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = offsetY.coerceAtLeast(0f).dp)
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) onBackClick()
                        offsetY = 0f
                    },
                    onVerticalDrag = { _, dragAmount -> offsetY += dragAmount }
                )
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onBackClick() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colors.bgSecondary)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume */ }
                .padding(horizontal = Spacing.screenMargin)
                .padding(top = 12.rdp, bottom = 32.rdp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textSecondary.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(20.rdp))

            // Title: "Zen Buddy Summary" with heart immediately after
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.accountability_summary_title),
                    fontFamily = Geist,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.rsp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Image(
                    painter = painterResource(R.drawable.heart_sharukhan),
                    contentDescription = null,
                    modifier = Modifier.size(26.rdp)
                )
            }

            Spacer(modifier = Modifier.height(12.rdp))

            // User Code
            uiState.userCode?.let { code ->
                UserCodeRow(code = code, onCopyCode = { onCopyCode(code) })
            }

            Spacer(modifier = Modifier.height(20.rdp))

            // Weekly Battle Heading
            Text(
                text = stringResource(R.string.accountability_weekly_battle_title),
                fontFamily = Geist,
                fontWeight = FontWeight.Bold,
                fontSize = 18.rsp,
                color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.rdp))

            // Stats Cards (weekly mode)
            StatsCardsRow(
                usage = uiState.myUsage,
                yesterdayChangePercent = null,
                hasBuddies = uiState.buddyStats != null,
                buddyStats = uiState.buddyStats,
                isSignedIn = true,
                isWeekly = true,
                myLikes = myLikes,
                buddyLikes = buddyLikes,
                onLikeClick = onLikeClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.rdp))

            // Lead / lag text
            LeadText(myUsage = uiState.myUsage, buddyStats = uiState.buddyStats)

            Spacer(modifier = Modifier.height(8.rdp))

            // Connection date
            uiState.connectionDateMillis?.let { millis ->
                Text(
                    text = stringResource(R.string.accountability_buddy_connected_since, formatConnectionDate(millis)),
                    fontFamily = Geist,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.rsp,
                    color = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(28.rdp))

            // Back to Home Button
            Image(
                painter = painterResource(R.drawable.button_back_to_home),
                contentDescription = stringResource(R.string.common_back_to_home),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBackToHomeClick() }
            )

            Spacer(modifier = Modifier.height(12.rdp))

            // Change My Buddy
            Text(
                text = stringResource(R.string.accountability_change_my_buddy),
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 16.rsp,
                color = colors.textBrand,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showChangeBuddyDialog = true }
                    .padding(vertical = 8.rdp)
            )

            // Standing escape hatch into the new flow - always available, one-way once tapped.
            Text(
                text = stringResource(R.string.accountability_switch_to_zen_circle),
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 14.rsp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSwitchToZenCircle() }
                    .padding(vertical = 8.rdp)
            )
        }
    }
}

// ── User Code Row ─────────────────────────────────────────────────

@Composable
private fun UserCodeRow(code: String, onCopyCode: () -> Unit) {
    val colors = ZenTheme.colors
    val displayCode = if (code.length > 8) "${code.take(8)}..." else code

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.accountability_my_code_label),
            fontFamily = Geist,
            fontWeight = FontWeight.Normal,
            fontSize = 14.rsp,
            color = colors.textSecondary
        )
        Text(
            text = displayCode,
            fontFamily = DepartureMono,
            fontWeight = FontWeight.Bold,
            fontSize = 14.rsp,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.width(8.rdp))
        Image(
            painter = painterResource(R.drawable.ic_content_copy),
            contentDescription = stringResource(R.string.accountability_copy_code_description),
            colorFilter = ColorFilter.tint(colors.textBrand),
            modifier = Modifier
                .size(20.rdp)
                .clickable { onCopyCode() }
        )
    }
}

// ── Lead Text ─────────────────────────────────────────────────────

@Composable
private fun LeadText(myUsage: DailyUsage?, buddyStats: BuddyStats?) {
    val colors = ZenTheme.colors
    val myMinutes = ((myUsage?.screenTimeInMillis ?: 0L) / 1000) / 60
    val buddyMinutes = buddyStats?.screenTimeMins ?: 0L

    val leadText: String
    val highlightText: String

    if (buddyStats == null) {
        leadText = stringResource(R.string.accountability_lead_connect_buddy)
        highlightText = ""
    } else {
        val diffMins = kotlin.math.abs(myMinutes - buddyMinutes)
        val hh = diffMins / 60
        val mm = diffMins % 60
        val timeStr = String.format("%02d:%02d", hh, mm)
        when {
            myMinutes < buddyMinutes -> {
                leadText = stringResource(R.string.accountability_lead_you_lead)
                highlightText = "$timeStr Hours"
            }
            myMinutes > buddyMinutes -> {
                leadText = stringResource(R.string.accountability_lead_buddy_leads)
                highlightText = "$timeStr Hours"
            }
            else -> {
                leadText = stringResource(R.string.accountability_lead_tied)
                highlightText = ""
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = leadText,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 15.rsp,
            color = colors.textPrimary
        )
        if (highlightText.isNotEmpty()) {
            Text(
                text = highlightText,
                fontFamily = DepartureMono,
                fontWeight = FontWeight.Bold,
                fontSize = 15.rsp,
                color = colors.textBrand
            )
        }
    }
}

// ── Change Buddy Dialog ───────────────────────────────────────────

@Composable
private fun ChangeBuddyConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ZenTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.bgSecondary,
        title = {
            Text(
                text = stringResource(R.string.accountability_change_buddy_title),
                fontFamily = Geist,
                fontWeight = FontWeight.Bold,
                fontSize = 20.rsp,
                color = colors.textPrimary
            )
        },
        text = {
            Text(
                text = stringResource(R.string.accountability_change_buddy_message),
                fontFamily = Geist,
                fontWeight = FontWeight.Normal,
                fontSize = 14.rsp,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            Text(
                text = stringResource(R.string.accountability_change_buddy_confirm),
                fontFamily = Geist,
                fontWeight = FontWeight.Bold,
                fontSize = 16.rsp,
                color = colors.moodAnnoyed,
                modifier = Modifier
                    .clickable { onConfirm() }
                    .padding(8.rdp)
            )
        },
        dismissButton = {
            Text(
                text = stringResource(R.string.accountability_change_buddy_cancel),
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 16.rsp,
                color = colors.textPrimary,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.rdp)
            )
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────

private fun formatConnectionDate(epochMillis: Long): String {
    return SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(epochMillis))
}
