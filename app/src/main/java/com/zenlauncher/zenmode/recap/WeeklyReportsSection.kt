package com.zenlauncher.zenmode.recap

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.components.rememberBrandOsGradient
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import java.time.LocalDate

/**
 * Settings → "Weekly reports". PRO users get every stored week (replay + PDF download);
 * everyone else sees a locked preview that opens the PRO sheet.
 */
@Composable
fun WeeklyReportsSection(
    reports: List<WeeklyRecap>,
    isPro: Boolean,
    downloadingWeek: LocalDate?,
    onOpen: (LocalDate) -> Unit,
    onDownload: (LocalDate) -> Unit,
    onShare: (LocalDate) -> Unit,
    onUnlockPro: () -> Unit
) {
    val colors = ZenTheme.colors
    // Settings already applies the screen margin; this section sits on the same edges as the cards.
    Column(modifier = Modifier.padding(horizontal = 4.rdp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Weekly reports",
                fontFamily = Geist,
                fontWeight = FontWeight.Bold,
                fontSize = 20.rsp,
                color = colors.textPrimary
            )
            Spacer(Modifier.width(8.rdp))
            ProBadge()
        }
        Spacer(Modifier.height(4.rdp))
        Text(
            text = "Every week you've spent in Zen. Replay it or download it as a PDF.",
            fontFamily = Geist,
            fontSize = 14.rsp,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(12.rdp))

        when {
            reports.isEmpty() -> EmptyReports()
            isPro -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.rdp))
                    .background(colors.bgSecondary)
            ) {
                reports.forEachIndexed { i, recap ->
                    ReportRow(
                        recap = recap,
                        downloading = downloadingWeek == recap.weekStart,
                        onOpen = { onOpen(recap.weekStart) },
                        onDownload = { onDownload(recap.weekStart) },
                        onShare = { onShare(recap.weekStart) }
                    )
                    if (i < reports.lastIndex) {
                        Box(
                            Modifier
                                .padding(horizontal = 16.rdp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.borderSubtle)
                        )
                    }
                }
            }
            else -> LockedReports(reports, onUnlockPro)
        }
    }
}

@Composable
private fun ReportRow(
    recap: WeeklyRecap,
    downloading: Boolean,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit = {},
    interactive: Boolean = true
) {
    val colors = ZenTheme.colors
    val kept = recap.outcome == RecapOutcome.KEPT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (interactive) Modifier.clickable(onClickLabel = "Replay this week", role = Role.Button, onClick = onOpen)
                else Modifier
            )
            .padding(start = 16.rdp, top = 12.rdp, bottom = 12.rdp, end = 6.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "${recap.rangeLabel()}, ${recap.weekEnd.year}",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.rsp,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(2.rdp))
            Text(
                text = "${formatMinutes(recap.totalMinutes)} · ${recap.daysKept}/7 days kept",
                fontFamily = DepartureMono,
                fontSize = 12.rsp,
                color = colors.textSecondary
            )
        }
        OutcomeChip(kept)
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(
                    if (interactive) Modifier.pressScale(onClick = onShare, onClickLabel = "Share report")
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Share, contentDescription = "Share report", tint = colors.textPrimary, modifier = Modifier.size(20.rdp))
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(
                    if (interactive) Modifier.pressScale(onClick = onDownload, enabled = !downloading, onClickLabel = "Download PDF report")
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (downloading) {
                CircularProgressIndicator(modifier = Modifier.size(18.rdp), strokeWidth = 2.dp, color = colors.textBrand)
            } else {
                Icon(Icons.Rounded.Download, contentDescription = "Download PDF", tint = colors.textPrimary, modifier = Modifier.size(22.rdp))
            }
        }
    }
}

@Composable
private fun OutcomeChip(kept: Boolean) {
    Text(
        text = if (kept) "Kept" else "Missed",
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.rsp,
        color = colorResource(if (kept) R.color.zen_700 else R.color.ember_700),
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(colorResource(if (kept) R.color.zen_050 else R.color.ember_on))
            .padding(horizontal = 10.rdp, vertical = 4.rdp)
    )
}

/** Blurred real rows behind a lock — shows what PRO holds without giving it away. */
@Composable
private fun LockedReports(reports: List<WeeklyRecap>, onUnlockPro: () -> Unit) {
    val colors = ZenTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.rdp))
            .background(colors.bgSecondary)
            .clickable(onClickLabel = "Unlock weekly reports with PRO", role = Role.Button, onClick = onUnlockPro)
    ) {
        Column(
            Modifier
                .blur(6.dp)
                .alpha(0.55f)
        ) {
            // Preview only: rows must not take taps meant for the unlock card.
            reports.take(3).forEach { ReportRow(it, downloading = false, onOpen = {}, onDownload = {}, interactive = false) }
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(20.rdp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(24.rdp))
            Spacer(Modifier.height(8.rdp))
            Text(
                text = "${reports.size} ${if (reports.size == 1) "week" else "weeks"} saved",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.rsp,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(10.rdp))
            Text(
                text = "Unlock with PRO",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.rsp,
                color = colors.actionPrimaryText,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colors.actionPrimary)
                    .padding(horizontal = 18.rdp, vertical = 10.rdp)
            )
        }
    }
}

@Composable
private fun EmptyReports() {
    val colors = ZenTheme.colors
    Text(
        text = "Your first report arrives on Monday morning, after a full week with ZenMode.",
        fontFamily = Geist,
        fontSize = 14.rsp,
        color = colors.textSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.rdp))
            .background(colors.bgSecondary)
            .padding(16.rdp)
    )
}

@Composable
fun ProBadge() {
    val gradient = rememberBrandOsGradient()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.rdp))
            .background(gradient)
            .padding(horizontal = 7.rdp, vertical = 2.rdp)
            .semantics { contentDescription = "PRO" }
    ) {
        Text("PRO", fontFamily = DepartureMono, fontSize = 11.rsp, letterSpacing = 0.8.sp, color = Color.White)
    }
}

// ── PRO sheet ─────────────────────────────────────────────────────

/**
 * What PRO includes. Purchasing isn't live yet, so the CTA requests early access;
 * debug builds also get a switch to turn PRO on locally for testing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProUpsellSheet(
    onDismiss: () -> Unit,
    onRequestAccess: () -> Unit,
    onEnableForTesting: (() -> Unit)?
) {
    val colors = ZenTheme.colors
    val gradient = rememberBrandOsGradient()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.bgPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.rdp)
                .padding(bottom = 24.rdp)
                .navigationBarsPadding()
        ) {
            Row {
                val heading = TextStyle(
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 32.rsp,
                    color = colors.textPrimary
                )
                Text(text = "ZenMode ", style = heading)
                // Own Text so the gradient spans exactly "PRO".
                Text(text = "PRO", style = heading.copy(brush = gradient))
            }
            Spacer(Modifier.height(6.rdp))
            Text(
                text = "For people who want to see the whole journey.",
                fontFamily = Geist,
                fontSize = 15.rsp,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(20.rdp))
            Column(verticalArrangement = Arrangement.spacedBy(14.rdp)) {
                Benefit("Every weekly report, saved for 16 weeks")
                Benefit("Download reports as PDF")
                Benefit("Weekly rankings in your Zen Circle")
            }
            Spacer(Modifier.height(26.rdp))
            Text(
                text = "Request early access",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.rsp,
                color = colors.actionPrimaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colors.actionPrimary)
                    .clickable(role = Role.Button, onClick = onRequestAccess)
                    .padding(vertical = 16.rdp)
            )
            Spacer(Modifier.height(8.rdp))
            Text(
                text = "PRO is opening to early Zens first. We'll reply personally.",
                fontFamily = Geist,
                fontSize = 12.rsp,
                color = colors.textSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            if (onEnableForTesting != null) {
                Spacer(Modifier.height(16.rdp))
                Text(
                    text = "Enable PRO on this device (debug build)",
                    fontFamily = DepartureMono,
                    fontSize = 12.rsp,
                    color = colors.textBrand,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.rdp))
                        .clickable(role = Role.Button, onClick = onEnableForTesting)
                        .padding(horizontal = 12.rdp, vertical = 8.rdp)
                )
            }
        }
    }
}

@Composable
private fun Benefit(text: String) {
    val colors = ZenTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.rdp)
                .clip(CircleShape)
                .background(colorResource(R.color.zen_050)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = colorResource(R.color.zen_700), modifier = Modifier.size(15.rdp))
        }
        Spacer(Modifier.width(12.rdp))
        Text(text = text, fontFamily = Geist, fontSize = 15.rsp, color = colors.textPrimary)
    }
}
