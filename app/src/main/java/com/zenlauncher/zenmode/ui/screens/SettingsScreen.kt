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
import com.zenlauncher.zenmode.ui.components.ZenModeOsSettingsTitle
import com.zenlauncher.zenmode.ResistancePreferences
import com.zenlauncher.zenmode.ThemePreferences
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
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

// ZenMode OS v3 settings.
//
// Groups follow what someone is trying to do: Focus, Home screen, Accountability, Your data,
// ZenMode. Account actions live behind the avatar. Every v2 flow is still here.
//
// Pro surfaces (plan card, PRO tags, gates) only render when [isProAvailable] — a backend
// that can't sell Pro shows the plain free app with nothing locked.

private enum class ScreenTimeRange { WEEK, MONTH }

@Composable
fun SettingsScreen(
    weeklyHours: List<Float> = List(7) { 0f },
    profilePhotoUrl: String? = null,
    displayName: String? = null,
    isProAvailable: Boolean = false,
    entitlement: Entitlement = Entitlement.Free,
    /** From [com.zenlauncher.zenmode.ProAccess], the same answer every other screen gets. */
    isPro: Boolean = entitlement.isPro,
    offers: List<PlanOffer> = emptyList(),
    isContentBlockingOn: Boolean = false,
    homeAppsChosenCount: Int = 0,
    isNotificationBadgesEnabled: Boolean = false,
    loadMonthlyHours: suspend () -> List<Float> = { emptyList() },
    onNotificationBadgesClick: () -> Unit = {},
    onBackClick: () -> Unit,
    onBlockInAppContentClick: () -> Unit = {},
    onChooseHomeAppsClick: () -> Unit = {},
    onAccountabilityPartnerClick: () -> Unit,
    onContributeClick: () -> Unit,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    onOpenPro: (ProEntry) -> Unit = {},
    onProGateShown: (ProFeature) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {},
    weeklyReports: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    var isDarkMode by remember { mutableStateOf(ThemePreferences.isDarkMode(context)) }
    var isResistanceEnabled by remember { mutableStateOf(ResistancePreferences.isEnabled(context)) }
    val homeAppCount = remember { AppGridPreferences.getAppCount(context) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var gate by remember { mutableStateOf<ProFeature?>(null) }
    var soon by remember { mutableStateOf<ProFeature?>(null) }

    @Suppress("NAME_SHADOWING")
    val isPro = isProAvailable && isPro
    fun proTag() = when {
        !isProAvailable -> ProTagState.None
        isPro -> ProTagState.Unlocked
        else -> ProTagState.Locked
    }
    /** Free → the gate sheet. Pro → an honest "arriving" note until the feature's surface ships. */
    fun openProFeature(feature: ProFeature) {
        if (isPro) {
            soon = feature
        } else {
            gate = feature
            onProGateShown(feature)
        }
    }

    // Plain paper, not the mood backdrop: Settings is a place to read and change things.
    Box(modifier = modifier.fillMaxSize().background(colors.bgPrimary)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            SettingsTopBar(
                profilePhotoUrl = profilePhotoUrl,
                displayName = displayName,
                onBackClick = onBackClick,
                onAccountClick = { showAccountSheet = true }
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = Spacing.screenMargin)
                    .padding(top = 4.rdp, bottom = 40.rdp),
                verticalArrangement = Arrangement.spacedBy(24.rdp)
            ) {
                if (isProAvailable) {
                    PlanCard(
                        entitlement = entitlement,
                        isPro = isPro,
                        offers = offers,
                        onClick = { onOpenPro(if (isPro) ProEntry.MANAGE else ProEntry.PLAN_CARD) }
                    )
                }

                ScreenTimeCard(
                    weeklyHours = weeklyHours,
                    showMonthOption = isProAvailable,
                    isPro = isPro,
                    loadMonthlyHours = loadMonthlyHours,
                    onMonthLocked = { openProFeature(ProFeature.FULL_HISTORY) }
                )

                // ── Weekly reports (PRO) ──
                weeklyReports?.invoke()

                ZenSettingsGroup(label = "Focus") {
                    ZenSettingToggleItem(
                        text = "Resistance screen",
                        subtitle = "A short pause before a distracting app opens.",
                        checked = isResistanceEnabled,
                        onCheckedChange = { enabled ->
                            isResistanceEnabled = enabled
                            ResistancePreferences.setEnabled(context, enabled)
                        }
                    )
                    ZenRowDivider()
                    ZenSettingsRow(
                        title = "Distraction Blocker",
                        subtitle = "Quiet reels, shorts and the apps that pull you in.",
                        value = if (isContentBlockingOn) "On" else "Off",
                        onClick = onBlockInAppContentClick
                    )
                }

                ZenSettingsGroup(label = "Home screen") {
                    ZenSettingsRow(
                        title = "Choose home apps",
                        subtitle = "Pick which apps sit on home, and in what order.",
                        value = if (homeAppsChosenCount == 0) "A–Z"
                        else "${homeAppsChosenCount.coerceAtMost(homeAppCount)} of $homeAppCount",
                        onClick = onChooseHomeAppsClick
                    )
                    ZenRowDivider()
                    ZenSettingToggleItem(
                        text = "Notification badges",
                        subtitle = if (isNotificationBadgesEnabled) "Dots show on app icons."
                        else "Needs notification access in Android settings.",
                        checked = isNotificationBadgesEnabled,
                        // Access is granted or revoked in system settings; the switch reflects it on resume.
                        onCheckedChange = { onNotificationBadgesClick() }
                    )
                    ZenRowDivider()
                    ZenSettingToggleItem(
                        text = "Dark mode (beta)",
                        subtitle = "Ink theme. Still being finished.",
                        checked = isDarkMode,
                        onCheckedChange = { enabled ->
                            isDarkMode = enabled
                            ThemePreferences.setDarkMode(context, enabled)
                        }
                    )
                    if (isProAvailable) {
                        ZenRowDivider()
                        ZenSettingsRow(
                            title = "Home-screen themes",
                            subtitle = "Keep the mood washes, or pick your own.",
                            pro = proTag(),
                            onClick = { openProFeature(ProFeature.HOME_THEMES) }
                        )
                    }
                }

                ZenSettingsGroup(
                    label = "Accountability",
                    trailingLabel = if (isProAvailable) {
                        "Partners · ${if (isPro) Entitlement.PRO_PARTNER_LIMIT else Entitlement.FREE_PARTNER_LIMIT} max"
                    } else null
                ) {
                    ZenSettingsRow(
                        title = "Accountability partner",
                        subtitle = "They see your score and its direction. Nothing else.",
                        onClick = onAccountabilityPartnerClick
                    )
                    if (isProAvailable) {
                        ZenRowDivider()
                        ZenSettingsRow(
                            title = "Add another partner",
                            value = if (isPro) "Up to ${Entitlement.PRO_PARTNER_LIMIT}" else null,
                            pro = proTag(),
                            onClick = { openProFeature(ProFeature.EXTRA_PARTNERS) }
                        )
                    }
                }

                if (isProAvailable) {
                    ZenSettingsGroup(label = "Your data") {
                        ZenSettingsRow(
                            title = "Export data",
                            subtitle = "A CSV of your sessions and scores.",
                            pro = proTag(),
                            onClick = { openProFeature(ProFeature.DATA_EXPORT) }
                        )
                    }
                }

                PhoneSettingsGroup()

                ZenSettingsGroup(label = "ZenMode") {
                    ZenSettingsRow(title = "Rate on Play Store", trailing = RowTrailing.External, onClick = onRateClick)
                    ZenRowDivider()
                    ZenSettingsRow(title = "Share ZenMode", trailing = RowTrailing.External, onClick = onShareClick)
                    ZenRowDivider()
                    ZenSettingsRow(
                        title = "Contribute on GitHub",
                        subtitle = "The whole app is open source.",
                        trailing = RowTrailing.External,
                        onClick = onContributeClick
                    )
                }

                SettingsFooter()
            }
        }

        if (showAccountSheet) {
            AccountSheet(
                displayName = displayName,
                isPro = isPro,
                onDismiss = { showAccountSheet = false },
                onLogoutClick = {
                    showAccountSheet = false
                    onLogoutClick()
                },
                onDeleteAccountClick = {
                    showAccountSheet = false
                    onDeleteAccountClick()
                }
            )
        }

        gate?.let { feature ->
            ProGateSheet(
                feature = feature,
                offers = offers,
                onSeePro = {
                    gate = null
                    onOpenPro(ProEntry.GATE)
                },
                onDismiss = { gate = null }
            )
        }

        soon?.let { feature ->
            ZenSheet(onDismiss = { soon = null }) {
                ZenEyebrow("Included in your Pro")
                ZenSheetTitle(feature.gateCopy().first)
                ZenSheetBody("This arrives in the next update. It'll be in the changelog, not a notification.")
                ZenButton(text = "Got it", onClick = { soon = null }, style = ZenButtonStyle.Outline)
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────

@Composable
private fun SettingsTopBar(
    profilePhotoUrl: String?,
    displayName: String?,
    onBackClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    val colors = ZenTheme.colors
    // The back button and avatar draw at 38dp inside 48dp touch targets; pulling the row in by
    // the difference puts both visible circles exactly on the cards' margin below.
    val touchInset = (Spacing.touchTarget - 38.rdp) / 2
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenMargin - touchInset, vertical = 8.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.touchTarget)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBackClick)
                .semantics { contentDescription = "Back" },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.rdp)
                    .clip(CircleShape)
                    .background(colors.bgSecondary)
                    .border(1.dp, colors.borderOutline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                ZenGlyph(GlyphKind.Back, colors.textPrimary, Modifier.size(18.rdp))
            }
        }
        Spacer(Modifier.width(8.rdp))
        Box(modifier = Modifier.weight(1f).semantics { heading() }) {
            ZenModeOsSettingsTitle(fontSize = 22.rsp, color = colors.textPrimary)
        }
        Box(
            modifier = Modifier
                .size(Spacing.touchTarget)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onAccountClick)
                .semantics { contentDescription = "Account" },
            contentAlignment = Alignment.Center
        ) {
            if (profilePhotoUrl != null) {
                AsyncImage(
                    model = profilePhotoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(38.rdp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.rdp)
                        .clip(CircleShape)
                        .background(colors.surfaceTint)
                        .border(1.dp, colors.surfaceTintLine, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName?.trim()?.firstOrNull()?.uppercase() ?: "Z",
                        fontFamily = Geist,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.rsp,
                        color = colors.textOnTint
                    )
                }
            }
        }
    }
}

// ── Plan card ──────────────────────────────────────────────────────

@Composable
private fun PlanCard(entitlement: Entitlement, isPro: Boolean, offers: List<PlanOffer>, onClick: () -> Unit) {
    val colors = ZenTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zenCard()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.rdp),
        verticalArrangement = Arrangement.spacedBy(8.rdp)
    ) {
        if (isPro) OsRule(Modifier.padding(bottom = 2.rdp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ZenEyebrow(
                text = if (isPro) {
                    "Supporter" + (entitlement.since?.let { " since ${it.asZenMonth()}" } ?: "")
                } else "Your plan",
                modifier = Modifier.weight(1f)
            )
            if (isPro) {
                ZenProTag(unlocked = true)
            } else {
                Text(
                    text = "FREE",
                    style = ZenTypography.monoLabel,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.rdp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.rdp))
                        .padding(horizontal = 8.rdp, vertical = 2.rdp)
                )
            }
        }
        Text(
            text = "ZenMode Pro",
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.rsp,
            lineHeight = 28.rsp,
            color = colors.textPrimary
        )
        if (isPro) {
            Text(
                text = entitlement.statusLine(offers),
                fontFamily = Geist,
                fontSize = 14.rsp,
                lineHeight = 20.rsp,
                color = colors.textSecondary
            )
        } else {
            Text(
                text = "Adds range, not access. The whole loop stays free.",
                fontFamily = Geist,
                fontSize = 14.rsp,
                lineHeight = 20.rsp,
                color = colors.textSecondary
            )
            // The price sits in the row, not behind the tap.
            offers.priceSummary()?.let { price ->
                Text(text = price, fontFamily = DepartureMono, fontSize = 14.rsp, color = colors.textPrimary)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.borderSubtle, modifier = Modifier.padding(top = 4.rdp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isPro) "Manage" else "See what it adds",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.rsp,
                color = colors.textBrand,
                modifier = Modifier.weight(1f)
            )
            ZenGlyph(GlyphKind.Chevron, colors.textBrand, Modifier.size(18.rdp))
        }
    }
}

// ── Screen time ────────────────────────────────────────────────────

@Composable
private fun ScreenTimeCard(
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

@Composable
private fun ScreenTimeBars(hours: List<Float>, labelDays: Boolean) {
    val colors = ZenTheme.colors
    val bar = colors.textBrand
    val grid = colors.borderHairlineSoft
    val max = maxOf(6f, kotlin.math.ceil(hours.maxOrNull() ?: 0f))
    val dayLabels = remember(hours.size) {
        val today = LocalDate.now()
        (hours.size - 1 downTo 0).map {
            today.minusDays(it.toLong()).dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).uppercase()
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.rdp)
            .semantics {
                contentDescription = "Screen time per day, ${hours.size} days. Today ${formatHours(hours.lastOrNull() ?: 0f)}."
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

private fun formatHours(hours: Float): String {
    val minutes = (hours * 60).toInt()
    return "${minutes / 60}h ${(minutes % 60).toString().padStart(2, '0')}m"
}

// ── Footer ─────────────────────────────────────────────────────────

// ── Sheets ─────────────────────────────────────────────────────────

/** Title, what free keeps, what Pro adds. Free's promise is always named before the price. */
private fun ProFeature.gateCopy(): Triple<String, String, String> = when (this) {
    ProFeature.FULL_HISTORY -> Triple(
        "Full history",
        "Free shows the last 7 days.",
        "Pro opens everything since you installed. None of it is deleted on free, it just isn't shown."
    )
    ProFeature.EXTRA_PARTNERS -> Triple(
        "Add a second partner",
        "One partner is enough for the loop to work. Free keeps one partner, forever.",
        "Pro lets you keep up to three: a friend, a sibling, and the person who actually notices."
    )
    ProFeature.HOME_THEMES -> Triple(
        "Home-screen themes",
        "The mood washes still follow your day on free.",
        "Pro lets you pick the wash yourself and keep it."
    )
    ProFeature.DATA_EXPORT -> Triple(
        "Export your data",
        "Everything already stays on your phone.",
        "Pro lets you take it with you as a CSV of sessions and scores."
    )
    ProFeature.PERIOD_REPORTS -> Triple(
        "Weekly and monthly reports",
        "Your daily Zen Report stays free, every item and every minute.",
        "Pro adds the week and the month, so you can see the direction and not just the day."
    )
    ProFeature.CUSTOM_SESSION_LENGTHS -> Triple(
        "Custom session lengths",
        "Free keeps four lengths: 5, 15, 25 and 45 minutes.",
        "Pro lets you set your own, down to the minute."
    )
    ProFeature.RANDOM_CONNECT -> Triple(
        "Random Connect",
        "Free keeps the partner you already have.",
        "Pro pairs you with one other ZenMode user. No feed, no profile."
    )
    ProFeature.SUPPORTERS_LIST -> Triple(
        "Supporters list",
        "The app is the same either way.",
        "Pro puts your name, if you want it there, in the list of people who keep it running."
    )
}

@Composable
private fun ProGateSheet(
    feature: ProFeature,
    offers: List<PlanOffer>,
    onSeePro: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ZenTheme.colors
    val (title, free, pro) = feature.gateCopy()
    ZenSheet(onDismiss = onDismiss) {
        ZenEyebrow("Part of Pro")
        ZenSheetTitle(title)
        ZenSheetBody(free)
        ZenSheetBody(pro)
        offers.priceSummary()?.let { price ->
            val trial = offers.offer(BillingPeriod.ANNUAL)
                ?.freeTrialDays?.let { it > 0 } == true
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.rdp))
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.rdp))
                    .padding(horizontal = 14.rdp, vertical = 12.rdp)
            ) {
                ZenEyebrow("What it costs")
                Text(
                    text = price + if (trial) ". The first month of the annual plan is free." else ".",
                    fontFamily = Geist,
                    fontSize = 15.rsp,
                    lineHeight = 22.rsp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 4.rdp)
                )
            }
        }
        ZenButton(text = "See what Pro adds", onClick = onSeePro)
        ZenButton(text = "Not now", onClick = onDismiss, style = ZenButtonStyle.Ghost)
    }
}

@Composable
private fun AccountSheet(
    displayName: String?,
    isPro: Boolean,
    onDismiss: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    val colors = ZenTheme.colors
    var confirmDelete by remember { mutableStateOf(false) }
    ZenSheet(onDismiss = onDismiss) {
        if (!confirmDelete) {
            ZenSheetTitle(displayName?.let { "Signed in as $it" } ?: "Your account")
            ZenSheetBody("You're about to break our heart a little. We'll be right here if you come back.")
            ZenButton(text = "Log out", onClick = onLogoutClick, style = ZenButtonStyle.Outline)
            ZenButton(
                text = "Delete account",
                onClick = { confirmDelete = true },
                style = ZenButtonStyle.Ghost,
                contentColor = colors.accentDeduct
            )
        } else {
            ZenEyebrow("Permanent", color = colors.accentDeduct)
            ZenSheetTitle("Delete your account?")
            ZenSheetBody(
                "This removes your account, your partner link and your scores from our servers, " +
                    "and clears usage history on this phone. It can't be undone."
            )
            if (isPro) {
                ZenSheetBody(
                    "Pro is billed by Google Play. Deleting your account doesn't cancel it, so cancel Pro first.",
                    emphasis = true
                )
            }
            ZenButton(text = "Delete my account", onClick = onDeleteAccountClick, style = ZenButtonStyle.Danger)
            ZenButton(text = "Keep my account", onClick = { confirmDelete = false }, style = ZenButtonStyle.Outline)
        }
    }
}
