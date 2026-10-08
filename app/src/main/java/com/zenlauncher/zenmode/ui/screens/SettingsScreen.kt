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
import com.zenlauncher.zenmode.GesturePreferences
import com.zenlauncher.zenmode.HomeGesture
import com.zenlauncher.zenmode.ui.components.ZenModeOsSettingsTitle
import com.zenlauncher.zenmode.ResistancePreferences
import com.zenlauncher.zenmode.HomeThemePreferences
import com.zenlauncher.zenmode.ThemePreferences
import com.zenlauncher.zenmode.ZenSound
import com.zenlauncher.zenmode.ui.components.ZenMotion
import com.zenlauncher.zenmode.ui.components.rememberZenFeedback
import androidx.compose.runtime.collectAsState
import com.zenlauncher.zenmode.coreapi.ZEN_CIRCLE_MAX_MEMBERS
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
import com.zenlauncher.zenmode.ui.components.BugReportSheet
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
import com.zenlauncher.zenmode.ui.components.LocalZenClock
import java.time.format.TextStyle as JavaTextStyle
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Color
import com.zenlauncher.zenmode.ZenCheckInPreferences
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ZenMode OS v3 settings.
//
// Groups follow what someone is trying to do: Focus, Home screen, Accountability, Your data,
// ZenMode. Account actions live behind the avatar. Every v2 flow is still here.
//
// Pro surfaces (plan card, PRO tags, gates) only render when [isProAvailable] — a backend
// that can't sell Pro shows the plain free app with nothing locked.


@Composable
fun SettingsScreen(
    weeklyHours: List<Float> = List(7) { 0f },
    profilePhotoUrl: String? = null,
    displayName: String? = null,
    isProAvailable: Boolean = false,
    /** Pro is unlocked without charging (early access): no upcoming prices are quoted. */
    isProSimulated: Boolean = false,
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
    /** Opens the public product board (zenmodeos.com/board) in the browser. */
    onFeatureRequestClick: () -> Unit = {},
    /** No mail app on the phone: the host falls back to the Telegram group. */
    onBugReportFallback: () -> Unit = {},
    onOpenPro: (ProEntry) -> Unit = {},
    onProGateShown: (ProFeature) -> Unit = {},
    /** Pro: writes the CSV export and opens the share sheet. */
    onExportData: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {},
    weeklyReports: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    val themeMode by remember { ThemePreferences.modeState(context) }.collectAsState()
    val soundsOn by ZenSound.enabled.collectAsState()
    val feedback = rememberZenFeedback()
    var isResistanceEnabled by remember { mutableStateOf(ResistancePreferences.isEnabled(context)) }
    val homeAppCount = remember { AppGridPreferences.getAppCount(context) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var gate by remember { mutableStateOf<ProFeature?>(null) }
    var soon by remember { mutableStateOf<ProFeature?>(null) }
    var themePicker by remember { mutableStateOf(false) }
    var bugReport by remember { mutableStateOf(false) }
    var checkInTimePicker by remember { mutableStateOf(false) }
    var isCheckInEnabled by remember { mutableStateOf(ZenCheckInPreferences.isEnabled(context)) }
    var checkInTime by remember { mutableStateOf(ZenCheckInPreferences.getEveningTime(context)) }
    val homeTheme by remember { HomeThemePreferences.state(context) }.collectAsState()
    // Everything switched on, Pro rows included — the rows show their own stored state even
    // while locked, so cancelling Pro doesn't look like the setting was thrown away.
    val enabledGestures by remember { GesturePreferences.state(context) }.collectAsState()

    @Suppress("NAME_SHADOWING")
    val isPro = isProAvailable && isPro
    fun proTag() = when {
        !isProAvailable -> ProTagState.None
        isPro -> ProTagState.Unlocked
        else -> ProTagState.Locked
    }
    /** Free → the gate sheet. Pro → the feature, or an honest "arriving" note until its surface ships. */
    fun openProFeature(feature: ProFeature) {
        if (isPro) {
            when (feature) {
                ProFeature.HOME_THEMES -> themePicker = true
                ProFeature.DATA_EXPORT -> onExportData()
                else -> soon = feature
            }
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
                        isSimulated = isProSimulated,
                        isPro = isPro,
                        offers = offers,
                        onClick = { onOpenPro(if (isPro) ProEntry.MANAGE else ProEntry.PLAN_CARD) }
                    )
                }

                SettingsScreenTimeCard(
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

                ZenSettingsGroup(label = "Daily check-in") {
                    ZenSettingToggleItem(
                        text = "Evening check-in",
                        subtitle = "A card on home each evening: confetti when you're under " +
                            "your promise, and where the week stands when you're not.",
                        checked = isCheckInEnabled,
                        onCheckedChange = { enabled ->
                            isCheckInEnabled = enabled
                            ZenCheckInPreferences.setEnabled(context, enabled)
                        }
                    )
                    if (isCheckInEnabled) {
                        ZenRowDivider()
                        ZenSettingsRow(
                            title = "Check-in time",
                            subtitle = "When the evening card appears.",
                            value = checkInTime.format(CheckInTimeFormat),
                            onClick = { checkInTimePicker = true }
                        )
                    }
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
                    if (isProAvailable) {
                        ZenRowDivider()
                        ZenSettingsRow(
                            title = "Home-screen themes",
                            subtitle = "Keep the mood washes, or pick your own.",
                            value = if (isPro) homeTheme.label else null,
                            pro = proTag(),
                            onClick = { openProFeature(ProFeature.HOME_THEMES) }
                        )
                    }
                }

                ZenSettingsGroup(label = "Gestures") {
                    GestureToggle(
                        gesture = HomeGesture.SWIPE_UP_SEARCH,
                        title = "Swipe up to search",
                        subtitle = "Opens search from anywhere on home, and takes the search bar " +
                            "off the screen — the swipe is the way in.",
                        enabled = enabledGestures,
                        isPro = isPro,
                        isProAvailable = isProAvailable,
                        proTag = proTag(),
                        onLocked = { openProFeature(ProFeature.GESTURES) }
                    )
                    ZenRowDivider()
                    GestureToggle(
                        gesture = HomeGesture.MARGIN_TAP_PAGES,
                        title = "Tap the margins",
                        subtitle = "Tap the left or right edge of home for Zen Score or Zen Gold, " +
                            "the same way the swipes go.",
                        enabled = enabledGestures,
                        isPro = isPro,
                        isProAvailable = isProAvailable,
                        proTag = proTag(),
                        onLocked = { openProFeature(ProFeature.GESTURES) }
                    )
                    ZenRowDivider()
                    GestureToggle(
                        gesture = HomeGesture.DOUBLE_TAP_LOCK,
                        title = "Double-tap to lock",
                        subtitle = "Double-tap an empty part of home. A long press still locks " +
                            "either way.",
                        enabled = enabledGestures,
                        isPro = isPro,
                        isProAvailable = isProAvailable,
                        proTag = proTag(),
                        onLocked = { openProFeature(ProFeature.GESTURES) }
                    )
                }

                ZenSettingsGroup(label = "Look & sound") {
                    AppearanceRow(
                        mode = themeMode,
                        onModeChange = { mode ->
                            val wasDark = ThemePreferences.isDarkMode(context)
                            // Let ZenTheme's crossfade play on this screen first; AppCompat then
                            // recreates every open activity onto colours that already match.
                            ThemePreferences.setMode(context, mode, applyAfterMillis = ZenMotion.SLOW + 60L)
                            val nowDark = ThemePreferences.isDarkMode(context)
                            if (nowDark != wasDark) feedback.theme(nowDark)
                        }
                    )
                    ZenRowDivider()
                    ZenSettingToggleItem(
                        text = "Interface sounds",
                        subtitle = "Soft ticks and chimes. Silent whenever your phone is.",
                        checked = soundsOn,
                        onCheckedChange = { on ->
                            ZenSound.setEnabled(context, on)
                            // Turning sounds on should prove itself.
                            if (on) feedback.toggle(true)
                        }
                    )
                }

                ZenSettingsGroup(
                    label = "Accountability",
                    trailingLabel = "Zen Circle · beta"
                ) {
                    ZenSettingsRow(
                        title = "Accountability partner",
                        subtitle = "They see your score and its direction. Nothing else.",
                        onClick = onAccountabilityPartnerClick
                    )
                    ZenRowDivider()
                    ZenSettingsRow(
                        title = "Zen Circle members",
                        // Everyone shares one cap during beta — see coreapi.ZEN_CIRCLE_MAX_MEMBERS.
                        // The free/Pro split (Entitlement.FREE_PARTNER_LIMIT / PRO_PARTNER_LIMIT)
                        // isn't live yet; this line is the honest, current state, not a sales pitch.
                        subtitle = "Beta: everyone can add up to $ZEN_CIRCLE_MAX_MEMBERS for now. " +
                            "It'll move to separate free and Pro limits soon.",
                        onClick = onAccountabilityPartnerClick
                    )
                }

                if (isProAvailable) {
                    ZenSettingsGroup(label = "Your data") {
                        ZenSettingsRow(
                            title = "Export data",
                            subtitle = "A CSV of every day ZenMode OS remembers.",
                            pro = proTag(),
                            onClick = { openProFeature(ProFeature.DATA_EXPORT) }
                        )
                    }
                }

                PhoneSettingsGroup()

                ZenSettingsGroup(label = "Help us build it") {
                    ZenSettingsRow(
                        title = "Report a bug",
                        subtitle = "Tell us what happened. A screenshot helps.",
                        onClick = { bugReport = true }
                    )
                    ZenRowDivider()
                    ZenSettingsRow(
                        title = "Feature requests",
                        subtitle = "The board of what's being built, and what you'd like next.",
                        trailing = RowTrailing.External,
                        onClick = onFeatureRequestClick
                    )
                }

                ZenSettingsGroup(label = "ZenMode OS") {
                    ZenSettingsRow(title = "Rate on Play Store", trailing = RowTrailing.External, onClick = onRateClick)
                    ZenRowDivider()
                    ZenSettingsRow(title = "Share ZenMode OS", trailing = RowTrailing.External, onClick = onShareClick)
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

        if (checkInTimePicker) {
            CheckInTimeSheet(
                current = checkInTime,
                onPick = { picked ->
                    checkInTime = picked
                    ZenCheckInPreferences.setEveningTime(context, picked)
                    checkInTimePicker = false
                },
                onDismiss = { checkInTimePicker = false }
            )
        }

        if (bugReport) {
            BugReportSheet(
                onDismiss = { bugReport = false },
                onSent = { bugReport = false },
                onNoMailApp = {
                    bugReport = false
                    onBugReportFallback()
                }
            )
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

        if (themePicker) {
            HomeThemeSheet(
                current = homeTheme,
                onPick = { HomeThemePreferences.set(context, it) },
                onDismiss = { themePicker = false }
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
private fun PlanCard(
    entitlement: Entitlement,
    isSimulated: Boolean,
    isPro: Boolean,
    offers: List<PlanOffer>,
    onClick: () -> Unit
) {
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
            text = "ZenMode OS Pro",
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.rsp,
            lineHeight = 28.rsp,
            color = colors.textPrimary
        )
        if (isPro) {
            Text(
                text = entitlement.statusLine(offers, isSimulated),
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
            if (isSimulated) {
                Text(
                    text = "Free during early access. Nothing is charged.",
                    fontFamily = Geist,
                    fontSize = 13.rsp,
                    color = colors.textBrand
                )
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
private fun GestureToggle(
    gesture: HomeGesture,
    title: String,
    subtitle: String,
    enabled: Set<HomeGesture>,
    isPro: Boolean,
    isProAvailable: Boolean,
    proTag: ProTagState,
    onLocked: () -> Unit
) {
    val context = LocalContext.current
    val gated = gesture.isPro && isProAvailable
    ZenSettingToggleItem(
        text = title,
        subtitle = subtitle,
        checked = gesture in enabled,
        pro = if (gated) proTag else ProTagState.None,
        onLocked = if (gated && !isPro) onLocked else null,
        onCheckedChange = { on -> GesturePreferences.setEnabled(context, gesture, on) }
    )
}

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
        "Pro lets you take it with you: a CSV of your days, promises and scores."
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
        "Pro pairs you with one other ZenMode OS user. No feed, no profile."
    )
    ProFeature.SUPPORTERS_LIST -> Triple(
        "Supporters list",
        "The app is the same either way.",
        "Pro puts your name, if you want it there, in the list of people who keep it running."
    )
    ProFeature.PROMISE_EDIT_FLEXIBILITY -> Triple(
        "Edit my promise",
        "Free edits the promise once a week, on Sundays.",
        "Pro edits twice a week, any day."
    )
    ProFeature.GESTURES -> Triple(
        "Home gestures",
        "Swipe up for search is free, and so is the long press that locks your phone.",
        "Pro adds the rest: tap either margin to move between pages, and double-tap to lock."
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
