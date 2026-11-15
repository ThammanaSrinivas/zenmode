package com.zenlauncher.zenmode.ui.screens

import androidx.annotation.StringRes
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.onSizeChanged
import com.zenlauncher.zenmode.ui.components.LocalSettingsQuery
import com.zenlauncher.zenmode.ui.components.SettingsSearchField
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
import androidx.compose.ui.res.stringResource
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.ZenCheckInPreferences
import com.zenlauncher.zenmode.R
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
    var searchQuery by remember { mutableStateOf("") }
    val searching = searchQuery.isNotBlank()
    var resultsHeight by remember { mutableStateOf(1) }
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

            SettingsSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = Spacing.screenMargin).padding(top = 4.rdp, bottom = 12.rdp)
            )

            CompositionLocalProvider(LocalSettingsQuery provides searchQuery) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenMargin)
                        .padding(top = 4.rdp, bottom = if (searching) 0.rdp else 40.rdp)
                        .onSizeChanged { resultsHeight = it.height },
                    // Searching: groups that folded away must not leave their gaps behind, so each
                    // surviving group brings its own (see ZenSettingsGroup).
                    verticalArrangement = if (searching) Arrangement.spacedBy(0.rdp) else Arrangement.spacedBy(24.rdp)
                ) {
                    // Only settings are searched; the plan, chart and report cards step aside.
                    if (isProAvailable && !searching) {
                        PlanCard(
                            entitlement = entitlement,
                            isSimulated = isProSimulated,
                            isPro = isPro,
                            offers = offers,
                            onClick = { onOpenPro(if (isPro) ProEntry.MANAGE else ProEntry.PLAN_CARD) }
                        )
                    }

                    if (!searching) {
                        SettingsScreenTimeCard(
                            weeklyHours = weeklyHours,
                            showMonthOption = isProAvailable,
                            isPro = isPro,
                            loadMonthlyHours = loadMonthlyHours,
                            onMonthLocked = { openProFeature(ProFeature.FULL_HISTORY) }
                        )

                        // ── Weekly reports (PRO) ──
                        weeklyReports?.invoke()
                    }

                    ZenSettingsGroup(label = stringResource(R.string.settings_focus_section)) {
                        ZenSettingToggleItem(
                            text = stringResource(R.string.settings_resistance_title),
                            subtitle = stringResource(R.string.settings_resistance_subtitle),
                            checked = isResistanceEnabled,
                            onCheckedChange = { enabled ->
                                isResistanceEnabled = enabled
                                ResistancePreferences.setEnabled(context, enabled)
                            }
                        )
                        ZenRowDivider()
                        ZenSettingsRow(
                            title = stringResource(R.string.common_distraction_blocker),
                            subtitle = stringResource(R.string.settings_distraction_blocker_subtitle),
                            value = if (isContentBlockingOn) stringResource(R.string.common_state_on) else stringResource(R.string.common_state_off),
                            onClick = onBlockInAppContentClick
                        )
                    }

                    ZenSettingsGroup(label = stringResource(R.string.settings_daily_checkin_section)) {
                        ZenSettingToggleItem(
                            text = stringResource(R.string.settings_evening_checkin_title),
                            subtitle = stringResource(R.string.settings_evening_checkin_subtitle_1) +
                                stringResource(R.string.settings_evening_checkin_subtitle_2),
                            checked = isCheckInEnabled,
                            onCheckedChange = { enabled ->
                                isCheckInEnabled = enabled
                                ZenCheckInPreferences.setEnabled(context, enabled)
                            }
                        )
                        if (isCheckInEnabled) {
                            ZenRowDivider()
                            ZenSettingsRow(
                                title = stringResource(R.string.settings_checkin_time_title),
                                subtitle = stringResource(R.string.settings_checkin_time_subtitle),
                                value = checkInTime.format(CheckInTimeFormat),
                                onClick = { checkInTimePicker = true }
                            )
                        }
                    }

                    ZenSettingsGroup(label = stringResource(R.string.settings_home_screen_section)) {
                        ZenSettingsRow(
                            title = stringResource(R.string.settings_choose_home_apps_title),
                            subtitle = stringResource(R.string.settings_choose_home_apps_subtitle),
                            value = if (homeAppsChosenCount == 0) stringResource(R.string.settings_home_apps_sort_az)
                            else stringResource(R.string.settings_home_apps_chosen, homeAppsChosenCount.coerceAtMost(homeAppCount), homeAppCount),
                            onClick = onChooseHomeAppsClick
                        )
                        ZenRowDivider()
                        ZenSettingToggleItem(
                            text = stringResource(R.string.settings_notification_badges_title),
                            subtitle = if (isNotificationBadgesEnabled) stringResource(R.string.settings_notification_badges_on)
                            else stringResource(R.string.settings_notification_access_needed),
                            checked = isNotificationBadgesEnabled,
                            // Access is granted or revoked in system settings; the switch reflects it on resume.
                            onCheckedChange = { onNotificationBadgesClick() }
                        )
                        if (isProAvailable) {
                            ZenRowDivider()
                            ZenSettingsRow(
                                title = stringResource(R.string.settings_home_themes_title),
                                subtitle = stringResource(R.string.settings_home_themes_subtitle),
                                value = if (isPro) homeTheme.label else null,
                                pro = proTag(),
                                onClick = { openProFeature(ProFeature.HOME_THEMES) }
                            )
                        }
                    }

                    ZenSettingsGroup(label = stringResource(R.string.settings_gestures_section)) {
                        GestureToggle(
                            gesture = HomeGesture.SWIPE_UP_SEARCH,
                            title = stringResource(R.string.settings_swipe_up_search_title),
                            subtitle = stringResource(R.string.settings_swipe_up_search_subtitle_1) +
                                stringResource(R.string.settings_swipe_up_search_subtitle_2),
                            enabled = enabledGestures,
                            isPro = isPro,
                            isProAvailable = isProAvailable,
                            proTag = proTag(),
                            onLocked = { openProFeature(ProFeature.GESTURES) }
                        )
                        ZenRowDivider()
                        GestureToggle(
                            gesture = HomeGesture.MARGIN_TAP_PAGES,
                            title = stringResource(R.string.settings_tap_margins_title),
                            subtitle = stringResource(R.string.settings_tap_margins_subtitle_1) +
                                stringResource(R.string.settings_tap_margins_subtitle_2),
                            enabled = enabledGestures,
                            isPro = isPro,
                            isProAvailable = isProAvailable,
                            proTag = proTag(),
                            onLocked = { openProFeature(ProFeature.GESTURES) }
                        )
                        ZenRowDivider()
                        GestureToggle(
                            gesture = HomeGesture.DOUBLE_TAP_LOCK,
                            title = stringResource(R.string.settings_double_tap_lock_title),
                            subtitle = stringResource(R.string.settings_double_tap_lock_subtitle_1) +
                                stringResource(R.string.settings_double_tap_lock_subtitle_2),
                            enabled = enabledGestures,
                            isPro = isPro,
                            isProAvailable = isProAvailable,
                            proTag = proTag(),
                            onLocked = { openProFeature(ProFeature.GESTURES) }
                        )
                    }

                    ZenSettingsGroup(label = stringResource(R.string.settings_look_sound_section)) {
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
                            text = stringResource(R.string.settings_interface_sounds_title),
                            subtitle = stringResource(R.string.settings_interface_sounds_subtitle),
                            checked = soundsOn,
                            onCheckedChange = { on ->
                                ZenSound.setEnabled(context, on)
                                // Turning sounds on should prove itself.
                                if (on) feedback.toggle(true)
                            }
                        )
                    }

                    ZenSettingsGroup(
                        label = stringResource(R.string.settings_accountability_section),
                        trailingLabel = stringResource(R.string.settings_zen_circle_beta_label)
                    ) {
                        ZenSettingsRow(
                            title = stringResource(R.string.settings_accountability_partner_title),
                            subtitle = stringResource(R.string.settings_accountability_partner_subtitle),
                            onClick = onAccountabilityPartnerClick
                        )
                        ZenRowDivider()
                        ZenSettingsRow(
                            title = stringResource(R.string.settings_zen_circle_members_title),
                            // Everyone shares one cap during beta — see coreapi.ZEN_CIRCLE_MAX_MEMBERS.
                            // The free/Pro split (Entitlement.FREE_PARTNER_LIMIT / PRO_PARTNER_LIMIT)
                            // isn't live yet; this line is the honest, current state, not a sales pitch.
                            subtitle = stringResource(R.string.settings_zen_circle_members_beta_1, ZEN_CIRCLE_MAX_MEMBERS) +
                                stringResource(R.string.settings_zen_circle_members_beta_2),
                            onClick = onAccountabilityPartnerClick
                        )
                    }

                    if (isProAvailable) {
                        ZenSettingsGroup(label = stringResource(R.string.settings_your_data_section)) {
                            ZenSettingsRow(
                                title = stringResource(R.string.settings_export_data_title),
                                subtitle = stringResource(R.string.settings_export_data_subtitle, PRODUCT_NAME),
                                pro = proTag(),
                                onClick = { openProFeature(ProFeature.DATA_EXPORT) }
                            )
                        }
                    }

                    PhoneSettingsGroup()

                    ZenSettingsGroup(label = stringResource(R.string.settings_help_build_section)) {
                        ZenSettingsRow(
                            title = stringResource(R.string.settings_report_bug_title),
                            subtitle = stringResource(R.string.settings_report_bug_subtitle),
                            onClick = { bugReport = true }
                        )
                        ZenRowDivider()
                        ZenSettingsRow(
                            title = stringResource(R.string.settings_feature_requests_title),
                            subtitle = stringResource(R.string.settings_feature_requests_subtitle),
                            trailing = RowTrailing.External,
                            onClick = onFeatureRequestClick
                        )
                    }

                    ZenSettingsGroup(label = PRODUCT_NAME) {
                        ZenSettingsRow(title = stringResource(R.string.settings_rate_on_play_store), trailing = RowTrailing.External, onClick = onRateClick)
                        ZenRowDivider()
                        ZenSettingsRow(title = stringResource(R.string.settings_share_app_title, PRODUCT_NAME), trailing = RowTrailing.External, onClick = onShareClick)
                        ZenRowDivider()
                        ZenSettingsRow(
                            title = stringResource(R.string.settings_contribute_github_title),
                            subtitle = stringResource(R.string.settings_contribute_github_subtitle),
                            trailing = RowTrailing.External,
                            onClick = onContributeClick
                        )
                    }

                    if (!searching) SettingsFooter()
                }
            }

            if (searching && resultsHeight == 0) {
                Text(
                    text = stringResource(R.string.settings_no_match_message, searchQuery.trim()),
                    fontFamily = Geist,
                    fontSize = 14.rsp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.screenMargin, vertical = 24.rdp)
                )
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
            BugReportSheet(onDismiss = { bugReport = false })
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
                ZenEyebrow(stringResource(R.string.common_included_in_pro))
                ZenSheetTitle(stringResource(feature.gateCopy().title))
                ZenSheetBody(stringResource(R.string.settings_soon_sheet_body))
                ZenButton(text = stringResource(R.string.settings_got_it_button), onClick = { soon = null }, style = ZenButtonStyle.Outline)
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
        val commonBack = stringResource(R.string.common_back)
        Box(
            modifier = Modifier
                .size(Spacing.touchTarget)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBackClick)
                .semantics { contentDescription = commonBack },
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
        val settingsAccountContentDescription = stringResource(R.string.settings_account_content_description)
        Box(
            modifier = Modifier
                .size(Spacing.touchTarget)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onAccountClick)
                .semantics { contentDescription = settingsAccountContentDescription },
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
                        text = displayName?.trim()?.firstOrNull()?.uppercase() ?: stringResource(R.string.common_avatar_fallback_initial),
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
                    stringResource(R.string.settings_supporter_eyebrow) + (entitlement.since?.let { stringResource(R.string.settings_supporter_since, it.asZenMonth()) } ?: "")
                } else stringResource(R.string.settings_your_plan_eyebrow),
                modifier = Modifier.weight(1f)
            )
            if (isPro) {
                ZenProTag(unlocked = true)
            } else {
                Text(
                    text = stringResource(R.string.common_free_badge),
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
            text = stringResource(R.string.settings_product_pro, PRODUCT_NAME),
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
                text = stringResource(R.string.settings_free_plan_description),
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
                    text = stringResource(R.string.settings_early_access_note),
                    fontFamily = Geist,
                    fontSize = 13.rsp,
                    color = colors.textBrand
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.borderSubtle, modifier = Modifier.padding(top = 4.rdp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isPro) stringResource(R.string.settings_manage_plan) else stringResource(R.string.settings_see_what_it_adds),
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
private data class GateCopy(
    @StringRes val title: Int,
    @StringRes val free: Int,
    @StringRes val pro: Int,
    val proArg: Any? = null
)

private fun ProFeature.gateCopy(): GateCopy = when (this) {
    ProFeature.FULL_HISTORY -> GateCopy(
        R.string.settings_pro_full_history_title,
        R.string.settings_pro_full_history_free,
        R.string.settings_pro_full_history_pro
    )
    ProFeature.EXTRA_PARTNERS -> GateCopy(
        R.string.settings_pro_second_partner_title,
        R.string.settings_pro_second_partner_free,
        R.string.settings_pro_second_partner_pro
    )
    ProFeature.HOME_THEMES -> GateCopy(
        R.string.settings_home_themes_title,
        R.string.settings_pro_home_themes_free,
        R.string.settings_pro_home_themes_pro
    )
    ProFeature.DATA_EXPORT -> GateCopy(
        R.string.settings_pro_data_export_title,
        R.string.settings_pro_data_export_free,
        R.string.settings_pro_data_export_pro
    )
    ProFeature.PERIOD_REPORTS -> GateCopy(
        R.string.settings_pro_period_reports_title,
        R.string.settings_pro_period_reports_free,
        R.string.settings_pro_period_reports_pro
    )
    ProFeature.CUSTOM_SESSION_LENGTHS -> GateCopy(
        R.string.settings_pro_session_lengths_title,
        R.string.settings_pro_session_lengths_free,
        R.string.settings_pro_session_lengths_pro
    )
    ProFeature.RANDOM_CONNECT -> GateCopy(
        R.string.settings_pro_random_connect_title,
        R.string.settings_pro_random_connect_free,
        R.string.settings_pro_random_connect_pro,
        PRODUCT_NAME
    )
    ProFeature.SUPPORTERS_LIST -> GateCopy(
        R.string.settings_pro_supporters_list_title,
        R.string.settings_pro_supporters_list_free,
        R.string.settings_pro_supporters_list_pro
    )
    ProFeature.PROMISE_EDIT_FLEXIBILITY -> GateCopy(
        R.string.settings_pro_edit_promise_title,
        R.string.settings_pro_edit_promise_free,
        R.string.settings_pro_edit_promise_pro
    )
    ProFeature.GESTURES -> GateCopy(
        R.string.settings_pro_home_gestures_title,
        R.string.settings_pro_home_gestures_free,
        R.string.settings_pro_home_gestures_pro
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
    val copy = feature.gateCopy()
    val title = stringResource(copy.title)
    val free = stringResource(copy.free)
    val pro = copy.proArg?.let { stringResource(copy.pro, it) } ?: stringResource(copy.pro)
    ZenSheet(onDismiss = onDismiss) {
        ZenEyebrow(stringResource(R.string.settings_part_of_pro_eyebrow))
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
                ZenEyebrow(stringResource(R.string.settings_what_it_costs_eyebrow))
                Text(
                    text = price + if (trial) stringResource(R.string.settings_annual_first_month_free) else ".",
                    fontFamily = Geist,
                    fontSize = 15.rsp,
                    lineHeight = 22.rsp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 4.rdp)
                )
            }
        }
        ZenButton(text = stringResource(R.string.settings_see_what_pro_adds_button), onClick = onSeePro)
        ZenButton(text = stringResource(R.string.settings_not_now_button), onClick = onDismiss, style = ZenButtonStyle.Ghost)
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
            ZenSheetTitle(displayName?.let { stringResource(R.string.settings_signed_in_as_title, it) } ?: stringResource(R.string.settings_your_account_title))
            ZenSheetBody(stringResource(R.string.settings_logout_sheet_body))
            ZenButton(text = stringResource(R.string.settings_log_out_button), onClick = onLogoutClick, style = ZenButtonStyle.Outline)
            ZenButton(
                text = stringResource(R.string.settings_delete_account_button),
                onClick = { confirmDelete = true },
                style = ZenButtonStyle.Ghost,
                contentColor = colors.accentDeduct
            )
        } else {
            ZenEyebrow(stringResource(R.string.settings_permanent_eyebrow), color = colors.accentDeduct)
            ZenSheetTitle(stringResource(R.string.settings_delete_account_title))
            ZenSheetBody(
                stringResource(R.string.settings_delete_account_body_1) +
                    stringResource(R.string.settings_delete_account_body_2)
            )
            if (isPro) {
                ZenSheetBody(
                    stringResource(R.string.settings_delete_account_pro_note),
                    emphasis = true
                )
            }
            ZenButton(text = stringResource(R.string.settings_delete_my_account_button), onClick = onDeleteAccountClick, style = ZenButtonStyle.Danger)
            ZenButton(text = stringResource(R.string.settings_keep_my_account_button), onClick = { confirmDelete = false }, style = ZenButtonStyle.Outline)
        }
    }
}
