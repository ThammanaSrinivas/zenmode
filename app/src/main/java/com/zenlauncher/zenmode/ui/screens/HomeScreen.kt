package com.zenlauncher.zenmode.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import com.zenlauncher.zenmode.AppGridPreferences
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.AppInfo
import com.zenlauncher.zenmode.AppLogic
import com.zenlauncher.zenmode.FileResult
import com.zenlauncher.zenmode.FileSearchRepository
import com.zenlauncher.zenmode.MoodState
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.ZenTypography
import com.zenlauncher.zenmode.ui.theme.moodWash
import com.zenlauncher.zenmode.ui.theme.rsp
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.components.StatsCardsRow
import com.zenlauncher.zenmode.ui.components.WeightSpacer
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

// ── Constants ─────────────────────────────────────────────────────
// Figma node 2001:1481 ("Frame 2147224192", the home screen's content frame) is
// designed at 411.92 x 606.6 — which is the dp reference width Android itself uses
// for this device class, so these numbers translate to dp near enough 1:1. Vertical
// gaps below are read straight off that frame rather than guessed.
//
// Every one of these reads LocalScreenScale (via .rdp), which is only available in
// composable scope — hence `@Composable get()` rather than a plain `private val`.
// The same value on a 360dp phone comes out ~0.88x smaller, matching how the rest
// of the app already scales; without this the v3 home screen would be the one
// screen in the app that didn't.

private val AppGridIconSize: Dp @Composable get() = 47.rdp
private val AppTileRadius: Dp @Composable get() = 17.rdp
// 30dp, not [Spacing.screenMargin] — Figma node 2001:1481 confirms the v3 home
// screen margin genuinely differs from the rest of the (still-v2) app. Revisit
// once other screens migrate to v3 and this can collapse into the shared token.
private val ScreenMargin: Dp @Composable get() = 30.rdp
private val SearchPillHeight: Dp @Composable get() = 48.rdp

private val TopToHeaderGap: Dp @Composable get() = 32.rdp
private val HeaderToGoldGap: Dp @Composable get() = 40.rdp
private val GoldToCardsGap: Dp @Composable get() = 46.rdp
private val CardsToAppsGap: Dp @Composable get() = 40.rdp
private val AppRowGap: Dp @Composable get() = 45.rdp
private val AppsToSearchGap: Dp @Composable get() = 40.rdp
private val SearchToDotsGap: Dp @Composable get() = 25.rdp
private val DotsToBottomGap: Dp @Composable get() = 20.rdp
private val HeaderIconSize: Dp @Composable get() = 33.rdp
private val HeaderIconTextGap: Dp @Composable get() = 4.rdp

// Figma node 2001:1504 — header text colours and gradients
private val ZenInk = Color(0xFF16210F)
private val StreakDaysColor = Color(0xFF3C3C3C)
private val ZenScoreGradient = Brush.linearGradient(
    listOf(Color(0xFF019A01), Color(0xFFE8CA11), Color(0xFFFE6801))
)
private val StreakGradient = Brush.horizontalGradient(
    listOf(Color(0xFFFE6801), Color(0xFFFF9F02), Color(0xFFBFD126), Color(0xFF60DE5E))
)

// Gold row, sampled from Figma node 71:6081
private val GoldLabel = Color(0xFF484848)
private val GoldAmount = Color(0xFFFF9601)
private val GoldDeltaBg = Color(0xFFE5E5E5)
private val GoldDeltaText = Color(0xFF007700)

// ── Main Home Screen ──────────────────────────────────────────────

@Composable
fun HomeScreen(
    usage: DailyUsage?,
    streaks: Int,
    weeklyScreenTimeMillis: List<Long> = List(7) { 0L },
    yesterdayChangePercent: Int?,
    hasBuddies: Boolean,
    buddyStats: BuddyStats?,
    isSignedIn: Boolean,
    showSearch: Boolean,
    zenScore: Int,
    goldInvested: String,
    goldChangePercent: Int,
    appCount: Int = AppGridPreferences.DEFAULT_APP_COUNT,
    myLikes: Long = 0L,
    buddyLikes: Long = 0L,
    onLikeClick: () -> Unit = {},
    onShowSearchChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onGoogleSearch: (String) -> Unit,
    onPhoneClick: () -> Unit,
    onLockClick: () -> Unit,
    onInviteBuddyClick: () -> Unit,
    onSignInClick: () -> Unit,
    onBuddyCardClick: (() -> Unit)? = null,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    onAppInfoClick: (AppInfo) -> Unit = {},
    apps: List<AppInfo>
) {
    val colors = ZenTheme.colors
    var showStreakOverlay by remember { mutableStateOf(false) }

    // Three variants, picked by today's screen time. The wash covers the whole screen,
    // pooling the mood colour in the middle and fading to cream at both edges.
    val todayMinutes = ((usage?.screenTimeInMillis ?: 0L) / 1000) / 60
    val mood = AppLogic.getMoodState(todayMinutes)
    val wash = colors.moodWash(mood)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(wash))
            // No dock any more: swipe left anywhere on the home screen for Settings,
            // long-press to lock. Right is reserved for a second page.
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < -40f) {
                        change.consume()
                        onSettingsClick()
                    }
                }
            }
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {},
                onLongClick = onLockClick
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(TopToHeaderGap))

            // zone 1 · identity + reward
            HomeHeader(
                zenScore = zenScore,
                streaks = streaks,
                onStreakClick = { showStreakOverlay = true }
            )

            Spacer(modifier = Modifier.height(HeaderToGoldGap))

            // zone 2 · the reward row
            GoldInvestedRow(
                gold = goldInvested,
                changePercent = goldChangePercent
            )

            Spacer(modifier = Modifier.height(GoldToCardsGap))

            // zone 3 · widget pair
            StatsCardsRow(
                usage = usage,
                yesterdayChangePercent = yesterdayChangePercent,
                hasBuddies = hasBuddies,
                buddyStats = buddyStats,
                isSignedIn = isSignedIn,
                zenScore = zenScore,
                streaks = streaks,
                buddyZenScore = AppConstants.PLACEHOLDER_BUDDY_ZEN_SCORE,
                buddyStreaks = AppConstants.PLACEHOLDER_BUDDY_STREAK,
                showReactions = false,
                myLikes = myLikes,
                buddyLikes = buddyLikes,
                onLikeClick = onLikeClick,
                onInviteBuddyClick = onInviteBuddyClick,
                onSignInClick = onSignInClick,
                onBuddyCardClick = onBuddyCardClick,
                modifier = Modifier.padding(horizontal = ScreenMargin)
            )

            // Every gap above is a fixed Figma measurement, but the Column fills the
            // full screen height and this device's dp height won't exactly match
            // Figma's 917dp reference — that mismatch was landing as dead space below
            // the dots (Arrangement.Top leaves any leftover at the bottom). Splitting
            // the surplus evenly across *this* gap and the one below the app grid —
            // equal weight, each floored at its own Figma minimum — keeps both gaps
            // growing together instead of dumping all the slack into one, so the
            // search pill and dots stay anchored near the bottom on every device
            // rather than floating mid-screen with a blank strip beneath them.
            Spacer(modifier = Modifier.weight(1f).heightIn(min = CardsToAppsGap))

            // zone 4 · apps. One page, exactly `appCount` of them.
            AppGrid(
                apps = apps.take(appCount),
                onAppClick = onAppClick,
                onAppLongClick = onAppLongClick,
                onAppInfoClick = onAppInfoClick
            )

            Spacer(modifier = Modifier.weight(1f).heightIn(min = AppsToSearchGap))

            // zone 5 · search + page dots
            SearchPill(onClick = { onShowSearchChange(true) })

            Spacer(modifier = Modifier.height(SearchToDotsGap))

            PageDots(onSettingsClick = onSettingsClick)

            Spacer(modifier = Modifier.height(DotsToBottomGap))
        }

        // Search overlay — rises from the bottom, where the search pill is
        AnimatedVisibility(
            visible = showSearch,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(280)
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(220)
            ) + fadeOut(tween(160))
        ) {
            SearchOverlay(
                apps = apps,
                onAppClick = { app ->
                    onShowSearchChange(false)
                    onAppClick(app)
                },
                onGoogleSearch = { query ->
                    onShowSearchChange(false)
                    onGoogleSearch(query)
                },
                onDismiss = { onShowSearchChange(false) }
            )
        }

        // Streak overlay
        AnimatedVisibility(
            visible = showStreakOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            StreakOverlay(
                streaks = streaks,
                weeklyScreenTimeMillis = weeklyScreenTimeMillis,
                onDismiss = { showStreakOverlay = false }
            )
        }
    }
}

// ── Header ────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    zenScore: Int,
    streaks: Int,
    onStreakClick: () -> Unit
) {
    val colors = ZenTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Zen Score — real gradient mark from Figma node 2001:1504.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_zen_mark_gradient),
                contentDescription = null,
                modifier = Modifier.size(HeaderIconSize)
            )

            Spacer(modifier = Modifier.width(HeaderIconTextGap))

            Column {
                Text(
                    text = "Zen Score",
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.4.rsp,
                    color = colors.textPrimary
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = zenScore.toString(),
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.9.rsp,
                        style = TextStyle(brush = ZenScoreGradient)
                    )
                    Text(
                        text = "/100",
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.4.rsp,
                        color = ZenInk
                    )
                }
            }
        }

        // Streaks — real two-tone flame + gradient number, same node.
        Row(
            modifier = Modifier.clickable { onStreakClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_streak_fire),
                contentDescription = "Streaks",
                modifier = Modifier.size(HeaderIconSize)
            )

            Spacer(modifier = Modifier.width(HeaderIconTextGap))

            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = streaks.toString(),
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.4.rsp,
                        style = TextStyle(brush = StreakGradient)
                    )
                    Spacer(modifier = Modifier.width(3.rdp))
                    Text(
                        text = "days",
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.4.rsp,
                        color = StreakDaysColor
                    )
                }
                Text(
                    text = "Streaks",
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.4.rsp,
                    color = colors.textPrimary
                )
            }
        }
    }
}

// ── Gold Invested ─────────────────────────────────────────────────

@Composable
private fun GoldInvestedRow(
    gold: String,
    changePercent: Int
) {
    val colors = ZenTheme.colors
    var amountVisible by remember { mutableStateOf(true) }
    val topRule = colors.textBrand

    // Every number below is the Figma frame's own geometry (node 71:6081).
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        // Hoisted out of drawBehind: reading LocalScreenScale (what .rdp needs) only
        // works in composable scope, not inside the DrawScope lambda below.
        val arcRadius = 41.777.rdp
        val arcStroke = 1.rdp

        Row(
            modifier = Modifier
                .height(56.4.rdp)
                // The design puts a border on the TOP edge only, so the rounded
                // corners render as an arc that runs down and stops.
                .drawBehind {
                    val r = arcRadius.toPx()
                    val stroke = arcStroke.toPx()
                    val path = Path().apply {
                        moveTo(0f, r)
                        arcTo(
                            rect = Rect(0f, 0f, r * 2, r * 2),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        lineTo(size.width - r, 0f)
                        arcTo(
                            rect = Rect(size.width - r * 2, 0f, size.width, r * 2),
                            startAngleDegrees = 270f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                    }
                    drawPath(path, topRule, style = Stroke(width = stroke))
                }
                .padding(horizontal = 15.5.rdp, vertical = 3.5.rdp),
            horizontalArrangement = Arrangement.spacedBy(6.4.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_coin_gold),
                contentDescription = null,
                modifier = Modifier.size(49.3.rdp)
            )

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GOLD INVESTED",
                        fontFamily = Geist,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.rsp,
                        color = GoldLabel
                    )
                    Spacer(modifier = Modifier.width(6.rdp))
                    Image(
                        painter = painterResource(R.drawable.ic_eye),
                        contentDescription = if (amountVisible) "Hide amount" else "Show amount",
                        modifier = Modifier
                            .width(15.9.rdp)
                            .height(10.9.rdp)
                            .clickable { amountVisible = !amountVisible },
                        colorFilter = ColorFilter.tint(GoldLabel)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.1.rdp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hiding blurs the amount rather than masking it — the Figma
                    // "Hide" variant is the same text under a 5.35px blur.
                    Text(
                        text = "\u20B9$gold",
                        fontFamily = DepartureMono,
                        fontSize = 20.9.rsp,
                        letterSpacing = (-1.045).sp,
                        color = GoldAmount,
                        modifier = if (amountVisible) Modifier
                        else Modifier.blur(5.35.rdp, BlurredEdgeTreatment.Unbounded)
                    )
                    Box(
                        modifier = Modifier
                            .height(12.4.rdp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(GoldDeltaBg)
                            .padding(horizontal = 6.5.rdp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${if (changePercent >= 0) "+" else ""}$changePercent%",
                            fontFamily = DepartureMono,
                            fontSize = 7.6.rsp,
                            lineHeight = 7.6.rsp,
                            color = GoldDeltaText,
                            style = LocalTextStyle.current.copy(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }
        }
    }
}

// ── App Grid ──────────────────────────────────────────────────────
// One page, no pager. The count is a setting; everything past it lives in search.

@Composable
private fun AppGrid(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    onAppInfoClick: (AppInfo) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(AppRowGap)
    ) {
        apps.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                row.forEach { app ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AppIconItem(
                            appInfo = app,
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) },
                            onAppInfoClick = { onAppInfoClick(app) }
                        )
                    }
                }
                repeat(4 - row.size) {
                    Box(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

// ── App Icon Item ─────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIconItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onAppInfoClick: () -> Unit = {}
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                showMenu = true
            }
        )
    ) {
        Box(
            modifier = Modifier.size(AppGridIconSize),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_XY
                        setImageDrawable(appInfo.icon)
                    }
                },
                update = { imageView ->
                    imageView.setImageDrawable(appInfo.icon)
                    imageView.scaleType = ImageView.ScaleType.FIT_XY
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AppTileRadius))
            )

            // Pin star indicator — bottom-right of icon (outside clip)
            if (appInfo.isPinned) {
                Image(
                    painter = painterResource(id = R.drawable.star),
                    contentDescription = "Pinned",
                    modifier = Modifier
                        .size(14.rdp)
                        .align(Alignment.BottomEnd)
                )
            }

            // Notification badge — top-right of icon
            if (appInfo.notificationCount > 0) {
                val badgeStrokeColor = ZenTheme.colors.notificationBadgeStroke
                Box(
                    modifier = Modifier
                        .size(18.rdp)
                        .align(Alignment.TopEnd)
                        .offset(x = 6.rdp, y = (-6).dp)
                        .border(width = 1.5.rdp, color = badgeStrokeColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.notification_circle),
                        contentDescription = "${appInfo.notificationCount} notifications",
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = if (appInfo.notificationCount > 99) "99+" else appInfo.notificationCount.toString(),
                        color = Color.White,
                        fontSize = 9.rsp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Geist,
                        lineHeight = 9.rsp,
                        maxLines = 1,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }

        // Long-press context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (appInfo.isPinned) "Unpin" else "Pin app",
                        fontFamily = Geist
                    )
                },
                onClick = {
                    showMenu = false
                    onLongClick()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "App info",
                        fontFamily = Geist
                    )
                },
                onClick = {
                    showMenu = false
                    onAppInfoClick()
                }
            )
        }
    }
}

// ── Search Pill ───────────────────────────────────────────────────

@Composable
private fun SearchPill(onClick: () -> Unit) {
    val colors = ZenTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .height(SearchPillHeight)
            .clip(RoundedCornerShape(percent = 50))
            .border(1.rdp, colors.textBrand.copy(alpha = 0.45f), RoundedCornerShape(percent = 50))
            .clickable { onClick() }
            .padding(horizontal = 20.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(17.rdp),
            colorFilter = ColorFilter.tint(colors.textSecondary)
        )
        Spacer(modifier = Modifier.width(12.rdp))
        Text(
            text = "Search apps, files & everything",
            fontFamily = Geist,
            fontWeight = FontWeight.Normal,
            fontSize = 14.rsp,
            color = colors.textSecondary
        )
    }
}

// ── Search Overlay ────────────────────────────────────────────────
// Three parts, always in this order and always labelled: what is already
// installed (Apps), what is in storage (Files), then the web (Google).

@Composable
private fun SearchOverlay(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onGoogleSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val fileSearchEnabled = remember { FileSearchRepository.isEnabled() }
    var hasFilePermission by remember {
        mutableStateOf(FileSearchRepository.hasPermission(context))
    }
    var fileResults by remember { mutableStateOf<List<FileResult>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        hasFilePermission = granted.values.any { it }
    }

    val filteredApps by remember(query, apps) {
        derivedStateOf {
            if (query.isBlank()) emptyList()
            else apps.filter {
                it.label.toString().contains(query, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(query, hasFilePermission) {
        fileResults = if (fileSearchEnabled && hasFilePermission) {
            FileSearchRepository.search(context, query)
        } else {
            emptyList()
        }
    }

    BackHandler(enabled = true) {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = offsetY.coerceAtLeast(0f).dp)
            .background(colors.bgPrimary.copy(alpha = 0.97f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) {
                            onDismiss()
                        }
                        offsetY = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        offsetY += dragAmount
                    }
                )
            }
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ScreenMargin, vertical = 48.rdp)
        ) {
            // Search input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SearchPillHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .border(1.5.rdp, colors.borderFocus, RoundedCornerShape(percent = 50))
                    .padding(horizontal = 18.rdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    modifier = Modifier.size(17.rdp),
                    colorFilter = ColorFilter.tint(colors.textSecondary)
                )
                Spacer(modifier = Modifier.width(10.rdp))

                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                keyboardController?.show()
                            }
                        },
                    textStyle = TextStyle(
                        fontFamily = Geist,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.rsp,
                        color = colors.textPrimary
                    ),
                    cursorBrush = SolidColor(colors.textBrand),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Apps, files & everything",
                                fontFamily = Geist,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.rsp,
                                color = colors.textSecondary
                            )
                        }
                        innerTextField()
                    }
                )

                if (query.isNotEmpty()) {
                    Text(
                        text = "✕",
                        fontSize = 18.rsp,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .clickable { query = "" }
                            .padding(start = 8.rdp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.rdp))

            // Results — three labelled sections
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.rdp)
            ) {
                // 1 · Apps
                item { SearchSectionHeader("APPS") }

                if (filteredApps.isEmpty()) {
                    item {
                        SearchEmptyRow(
                            text = if (query.isBlank()) "Type to find an app"
                            else "No app matches “$query”"
                        )
                    }
                } else {
                    items(filteredApps) { app ->
                        AppResultRow(app = app, onClick = { onAppClick(app) })
                    }
                }

                // 2 · Files
                if (fileSearchEnabled) {
                    item {
                        Spacer(modifier = Modifier.height(20.rdp))
                        SearchSectionHeader("FILES")
                    }

                    when {
                        !hasFilePermission -> item {
                            FilePermissionRow(
                                onGrantClick = {
                                    permissionLauncher.launch(FileSearchRepository.requiredPermissions)
                                }
                            )
                        }
                        fileResults.isEmpty() -> item {
                            SearchEmptyRow(
                                text = if (query.isBlank()) "Type to find a file"
                                else "No file matches “$query”"
                            )
                        }
                        else -> items(fileResults) { file ->
                            FileResultRow(
                                file = file,
                                onClick = {
                                    onDismiss()
                                    FileSearchRepository.open(context, file)
                                }
                            )
                        }
                    }
                }

                // 3 · Google
                item {
                    Spacer(modifier = Modifier.height(20.rdp))
                    SearchSectionHeader("GOOGLE")
                }

                item {
                    if (query.isBlank()) {
                        SearchEmptyRow(text = "Type to search the web")
                    } else {
                        GoogleFallbackRow(
                            query = query,
                            onClick = { onGoogleSearch(query) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(label: String) {
    Text(
        text = label,
        style = ZenTypography.monoLabel,
        fontSize = 11.rsp,
        color = ZenTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = 12.rdp, vertical = 8.rdp)
    )
}

@Composable
private fun SearchEmptyRow(text: String) {
    Text(
        text = text,
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 14.rsp,
        color = ZenTheme.colors.textSecondary.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 12.rdp, vertical = 10.rdp)
    )
}

@Composable
private fun AppResultRow(app: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.rdp))
            .clickable { onClick() }
            .padding(horizontal = 12.rdp, vertical = 10.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.rdp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_XY
                        setImageDrawable(app.icon)
                    }
                },
                update = { imageView ->
                    imageView.setImageDrawable(app.icon)
                    imageView.scaleType = ImageView.ScaleType.FIT_XY
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AppTileRadius))
            )
        }
        Spacer(modifier = Modifier.width(12.rdp))
        Text(
            text = app.label.toString(),
            fontFamily = Geist,
            fontWeight = FontWeight.Normal,
            fontSize = 16.rsp,
            color = ZenTheme.colors.textPrimary
        )
    }
}

@Composable
private fun FileResultRow(file: FileResult, onClick: () -> Unit) {
    val colors = ZenTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.rdp))
            .clickable { onClick() }
            .padding(horizontal = 12.rdp, vertical = 10.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.rdp)
                .clip(RoundedCornerShape(AppTileRadius))
                .background(colors.bgSecondary),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_file),
                contentDescription = null,
                modifier = Modifier.size(20.rdp),
                colorFilter = ColorFilter.tint(colors.textSecondary)
            )
        }
        Spacer(modifier = Modifier.width(12.rdp))
        Column {
            Text(
                text = file.displayName,
                fontFamily = Geist,
                fontWeight = FontWeight.Normal,
                fontSize = 16.rsp,
                color = colors.textPrimary,
                maxLines = 1
            )
            file.mimeType?.let { mime ->
                Text(
                    text = mime,
                    style = ZenTypography.monoLabel,
                    fontSize = 10.rsp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun FilePermissionRow(onGrantClick: () -> Unit) {
    val colors = ZenTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.rdp))
            .background(colors.bgSecondary)
            .clickable { onGrantClick() }
            .padding(horizontal = 12.rdp, vertical = 14.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_file),
            contentDescription = null,
            modifier = Modifier.size(20.rdp),
            colorFilter = ColorFilter.tint(colors.textBrand)
        )
        Spacer(modifier = Modifier.width(12.rdp))
        Text(
            text = "Allow file access to search your files",
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.rsp,
            color = colors.textBrand
        )
    }
}

@Composable
private fun GoogleFallbackRow(query: String, onClick: () -> Unit) {
    val colors = ZenTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.rdp))
            .background(colors.bgSecondary)
            .clickable { onClick() }
            .padding(horizontal = 12.rdp, vertical = 14.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = "Google",
            modifier = Modifier.size(20.rdp),
            colorFilter = ColorFilter.tint(colors.textBrand)
        )
        Spacer(modifier = Modifier.width(12.rdp))
        Text(
            text = "Search Google for “$query”",
            fontFamily = Geist,
            fontWeight = FontWeight.Normal,
            fontSize = 14.rsp,
            color = colors.textSecondary
        )
    }
}

// ── Streak Overlay ───────────────────────────────────────────────

@Composable
private fun StreakOverlay(
    streaks: Int,
    weeklyScreenTimeMillis: List<Long>,
    onDismiss: () -> Unit
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var offsetY by remember { mutableStateOf(0f) }
    var cardBounds by remember { mutableStateOf<Rect?>(null) }

    // Map rolling 7-day data to current week (Mon-Sun)
    val today = LocalDate.now()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val todayDayIndex = today.dayOfWeek.value - 1 // 0=Mon, 6=Sun

    // weeklyScreenTimeMillis is [6 days ago .. today] (7 items)
    // Map to current week days
    val currentWeekMillis = remember(weeklyScreenTimeMillis) {
        val result = LongArray(7) { -1L } // -1 = future/no data
        for (dayIdx in 0..6) {
            val date = monday.plusDays(dayIdx.toLong())
            val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(date, today).toInt()
            if (daysAgo in 0..6 && dayIdx <= todayDayIndex) {
                // Index in weeklyScreenTimeMillis: last item is today (index 6), 1 day ago is index 5, etc.
                val dataIdx = 6 - daysAgo
                result[dayIdx] = weeklyScreenTimeMillis[dataIdx]
            }
        }
        result.toList()
    }

    val dayLabels = listOf("Mon", "Tue", "Wed", "Thurs", "Fri", "Sat", "Sun")
    val streakSubtitle = if (streaks > 0) "Your mindfulness at peak!!" else "Keep going, build your streak!"

    BackHandler(enabled = true) { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = offsetY.coerceAtLeast(0f).dp)
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) onDismiss()
                        offsetY = 0f
                    },
                    onVerticalDrag = { _, dragAmount -> offsetY += dragAmount }
                )
            }
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .onGloballyPositioned { coords ->
                    cardBounds = coords.boundsInWindow()
                }
                .clip(RoundedCornerShape(topStart = 24.rdp, topEnd = 24.rdp))
                .background(colors.bgSecondary)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { /* consume click */ }
                .padding(horizontal = ScreenMargin)
                .padding(top = 12.rdp, bottom = 32.rdp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.rdp)
                    .height(4.rdp)
                    .clip(RoundedCornerShape(2.rdp))
                    .background(colors.textSecondary.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(20.rdp))

            // Title row: "My Zenmode Streak" + top-right icon group
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "My Zenmode Streak",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                // Top-right: blurred shuriken + app_icon + arrow
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(56.rdp),
                    contentAlignment = Alignment.Center
                ) {
                    // Blurred shuriken behind
                    Image(
                        painter = painterResource(R.drawable.resistence_screen_happy_shuriken),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.rdp)
                            .blur(8.rdp)
                            .alpha(0.5f),
                        contentScale = ContentScale.Fit
                    )
                    // App icon
                    Image(
                        painter = painterResource(R.drawable.app_icon),
                        contentDescription = null,
                        modifier = Modifier.size(47.rdp)
                    )
                    // Arrow hitting the icon — tip touches center of app_icon
                    Image(
                        painter = painterResource(R.drawable.arrow),
                        contentDescription = null,
                        modifier = Modifier
                            .width(55.rdp)
                            .offset(x = -26.rdp, y = 18.rdp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.rdp))

            // Streak count row: king icon + "N Days Streak"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.king),
                    contentDescription = "Crown",
                    modifier = Modifier.size(32.rdp),
                    colorFilter = ColorFilter.tint(colors.accentReward)
                )
                Spacer(modifier = Modifier.width(8.rdp))
                Text(
                    text = String.format("%02d Days Streak", streaks),
                    fontFamily = DepartureMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 24.rsp,
                    color = colors.textPrimary
                )
            }

            Text(
                text = streakSubtitle,
                fontFamily = Geist,
                fontWeight = FontWeight.Normal,
                fontSize = 14.rsp,
                color = colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.rdp)
            )

            Spacer(modifier = Modifier.height(24.rdp))

            // Weekly calendar row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.rdp,
                        color = colors.borderSubtle,
                        shape = RoundedCornerShape(20.rdp)
                    )
                    .padding(horizontal = 12.rdp, vertical = 16.rdp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 16.rdp)
                    ,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (dayIdx in 0..6) {
                        val millis = currentWeekMillis[dayIdx]
                        val isFuture = millis < 0
                        val dayDate = monday.plusDays(dayIdx.toLong())

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayLabels[dayIdx],
                                fontFamily = Geist,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.rsp,
                                color = colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(8.rdp))

                            if (isFuture) {
                                // Show date number for future days
                                Text(
                                    text = "${dayDate.dayOfMonth}",
                                    fontFamily = DepartureMono,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.rsp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.size(28.rdp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                // Show shuriken based on mood
                                val dayMinutes = (millis / 1000) / 60
                                val dayMood = AppLogic.getMoodState(dayMinutes)
                                val dayShurikenRes = when (dayMood) {
                                    MoodState.HAPPY -> R.drawable.resistence_screen_happy_shuriken
                                    MoodState.NEUTRAL -> R.drawable.resistence_screen_neutral_shuriken
                                    MoodState.ANNOYED -> R.drawable.resistence_screen_annoyed_shuriken
                                }
                                Image(
                                    painter = painterResource(dayShurikenRes),
                                    contentDescription = "$dayMood",
                                    modifier = Modifier.size(32.rdp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.rdp))

            // Share my streak button
            Image(
                painter = painterResource(R.drawable.button_share_my_streak),
                contentDescription = "Share my streak",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.rdp)
                    .clip(RoundedCornerShape(12.rdp))
                    .clickable {
                        scope.launch {
                            try {
                                // Capture on Main thread (required by drawToBitmap)
                                val fullBitmap = view.drawToBitmap()

                                // Crop to card bounds
                                val bounds = cardBounds ?: return@launch
                                val cropped = Bitmap.createBitmap(
                                    fullBitmap,
                                    bounds.left.toInt().coerceAtLeast(0),
                                    bounds.top.toInt().coerceAtLeast(0),
                                    bounds.width.toInt().coerceAtMost(fullBitmap.width - bounds.left.toInt().coerceAtLeast(0)),
                                    bounds.height.toInt().coerceAtMost(fullBitmap.height - bounds.top.toInt().coerceAtLeast(0))
                                )

                                // Save on IO thread
                                withContext(Dispatchers.IO) {
                                    val imagesFolder = File(context.cacheDir, "shared_images")
                                    imagesFolder.mkdirs()
                                    val file = File(imagesFolder, "streak_share.png")
                                    file.outputStream().use { out ->
                                        cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    }

                                    // Share (back to Main for intent)
                                    withContext(Dispatchers.Main) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "I'm on a $streaks-day mindfulness streak on ZenMode!"
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(intent, "Share Streak")
                                        )
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                    },
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

// ── Page Dots ─────────────────────────────────────────────────────
// The dock is gone. These mark the home pages: swipe left for Settings, and the
// third slot is reserved for a page we have not built yet.

@Composable
private fun PageDots(onSettingsClick: () -> Unit) {
    val colors = ZenTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < -40f) {
                        change.consume()
                        onSettingsClick()
                    }
                }
            }
            .padding(vertical = 8.rdp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val active = i == 1
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.rdp)
                    .size(7.rdp)
                    .clip(CircleShape)
                    .background(
                        if (active) colors.textBrand
                        else colors.textPrimary.copy(alpha = 0.35f)
                    )
            )
        }
    }
}
