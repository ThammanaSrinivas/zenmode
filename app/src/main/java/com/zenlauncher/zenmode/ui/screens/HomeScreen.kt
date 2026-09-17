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
import androidx.compose.ui.res.colorResource
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import android.os.Environment
import android.widget.Toast
import android.util.Log

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

// Figma node 2001:1504 — header text colours and gradients. Values live in colors.xml
// (SOURCE OF TRUTH: design tokens) — never inline a Color(0x...) literal here.
private val ZenInk: Color @Composable get() = colorResource(R.color.ink_gain)
private val StreakDaysColor: Color @Composable get() = colorResource(R.color.streak_days)
private val ZenScoreGradient: Brush
    @Composable get() = Brush.linearGradient(
        listOf(
            colorResource(R.color.score_grad_start),
            colorResource(R.color.score_grad_mid),
            colorResource(R.color.score_orange)
        )
    )
private val StreakGradient: Brush
    @Composable get() = Brush.horizontalGradient(
        listOf(
            colorResource(R.color.score_orange),
            colorResource(R.color.streak_grad_2),
            colorResource(R.color.streak_grad_3),
            colorResource(R.color.streak_grad_4)
        )
    )

// Gold row, sampled from Figma node 71:6081. Not private — ZenGoldScreen.kt
// (Figma node 2026:1648) reuses these same tokens via GoldInvestedRow.
val GoldLabel: Color @Composable get() = colorResource(R.color.gold_label)
val GoldAmount: Color @Composable get() = colorResource(R.color.gold_amount)
val GoldDeltaBg: Color @Composable get() = colorResource(R.color.gold_delta_bg)
val GoldDeltaText: Color @Composable get() = colorResource(R.color.gold_delta_text)

// ── Main Home Screen ──────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
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
    onZenGoldClick: () -> Unit = {},
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
            // swipe right for Zen Gold, long-press to lock.
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < -40f) {
                        change.consume()
                        onSettingsClick()
                    } else if (dragAmount > 40f) {
                        change.consume()
                        onZenGoldClick()
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

            PageDots(onSettingsClick = onSettingsClick, onZenGoldClick = onZenGoldClick)

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
            // weeklyScreenTimeMillis (HomeScreen's own param, still fed by MainActivity)
            // no longer reaches this overlay — the v3 milestone-card design doesn't
            // use a per-day weekly view. Left wired above pending real streak-history
            // tracking (see AppConstants' milestone placeholders).
            StreakOverlay(
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
fun GoldInvestedRow(
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
// v3 redesign — Figma node 2026:2137 ("ZM_OS v3' Zen Home/ streaks OVerlay").
// Replaces the old weekly-calendar sheet with a shareable milestone card. Total
// mindful days, community percentile and longest streak all need real streak-
// history tracking that doesn't exist yet (see AppConstants placeholders).

// SOURCE OF TRUTH: design tokens — values live in colors.xml (never a bare
// Color(0x...) literal here). Where Figma's own sampled value sits within a
// hair of an existing token (this card and zen_700 differ by one hex digit),
// reuse the token rather than add a near-duplicate.
private val MilestoneCardBg: Color @Composable get() = colorResource(R.color.ink_base)
private val MilestoneMuted: Color @Composable get() = colorResource(R.color.milestone_muted)
private val MilestoneDim: Color @Composable get() = colorResource(R.color.milestone_dim)
private val MilestoneDaysColor: Color @Composable get() = colorResource(R.color.amber_500)
private val MilestoneTagline: Color @Composable get() = colorResource(R.color.milestone_tagline)
private val MilestoneOutlineBg: Color @Composable get() = colorResource(R.color.milestone_outline_bg)
private val MilestoneOutlineBorder: Color @Composable get() = colorResource(R.color.zen_700)
private val MilestoneSolidBg: Color @Composable get() = colorResource(R.color.zen_700)

@Composable
private fun StreakOverlay(
    onDismiss: () -> Unit,
    totalMindfulDays: Int = AppConstants.PLACEHOLDER_MILESTONE_DAYS,
    topPercentile: Int = AppConstants.PLACEHOLDER_MILESTONE_PERCENTILE,
    zenScoreThreshold: Int = AppConstants.PLACEHOLDER_MILESTONE_SCORE_THRESHOLD,
    longestStreakDays: Int = AppConstants.PLACEHOLDER_LONGEST_STREAK_DAYS,
    longestStreakRange: String = AppConstants.PLACEHOLDER_LONGEST_STREAK_RANGE
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var offsetY by remember { mutableStateOf(0f) }
    var cardBounds by remember { mutableStateOf<Rect?>(null) }

    BackHandler(enabled = true) { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = offsetY.coerceAtLeast(0f).dp)
            .background(Color.Black.copy(alpha = 0.45f))
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
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(topStart = 24.rdp, topEnd = 24.rdp))
                .background(colors.bgSecondary.copy(alpha = 0.96f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume click */ }
                .padding(horizontal = ScreenMargin)
                .padding(top = 17.rdp, bottom = 32.rdp)
        ) {
            // Header row: "STREAKS" eyebrow + dismiss
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "STREAKS",
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.rsp,
                    letterSpacing = (-0.42).sp,
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Image(
                    painter = painterResource(R.drawable.ic_milestone_close),
                    contentDescription = "Close",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(24.rdp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(35.rdp))

            // Headline — spelled-out day count, matching the design's voice
            Text(
                text = "${numberToWords(totalMindfulDays)} days of intentional time with mobile & promise kept safe.",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 20.rsp,
                lineHeight = 24.rsp,
                letterSpacing = (-0.6).sp,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(15.rdp))

            Text(
                text = "You're in the top $topPercentile% of the Zen Bros",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.rsp,
                letterSpacing = (-0.16).sp,
                color = colors.textBrand
            )

            Spacer(modifier = Modifier.height(15.rdp))

            // The shareable milestone card — "Save as image" / "Share my streaks"
            // crop exactly this, not the whole sheet.
            MilestoneCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords -> cardBounds = coords.boundsInWindow() },
                totalMindfulDays = totalMindfulDays,
                zenScoreThreshold = zenScoreThreshold
            )

            Spacer(modifier = Modifier.height(20.rdp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.textPrimary)) {
                        append("LONGEST · $longestStreakDays DAYS ")
                    }
                    withStyle(SpanStyle(color = colors.textSecondary)) {
                        append("($longestStreakRange)")
                    }
                },
                fontFamily = DepartureMono,
                fontWeight = FontWeight.Normal,
                fontSize = 14.rsp,
                letterSpacing = (-0.14).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.rdp))

            // Save as image / Share my streaks
            Column(verticalArrangement = Arrangement.spacedBy(12.rdp)) {
                MilestoneOutlineButton(
                    text = "Save as image",
                    onClick = {
                        scope.launch {
                            saveOrShareMilestoneCard(
                                view = view,
                                context = context,
                                cardBounds = cardBounds,
                                share = false,
                                totalMindfulDays = totalMindfulDays
                            )
                        }
                    }
                )
                MilestoneSolidButton(
                    text = "Share my streaks",
                    onClick = {
                        scope.launch {
                            saveOrShareMilestoneCard(
                                view = view,
                                context = context,
                                cardBounds = cardBounds,
                                share = true,
                                totalMindfulDays = totalMindfulDays
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MilestoneCard(
    totalMindfulDays: Int,
    zenScoreThreshold: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.rdp))
            .background(MilestoneCardBg)
            .padding(20.rdp)
    ) {
        Column {
            // Header: logo + milestone label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.logo_only_pins),
                        contentDescription = null,
                        modifier = Modifier.size(20.rdp)
                    )
                    Spacer(modifier = Modifier.width(6.rdp))
                    Text(text = "ZenMode", fontFamily = ClashDisplay, fontWeight = FontWeight.Medium, fontSize = 18.rsp, color = Color.White)
                    Text(text = "OS", fontFamily = ClashDisplay, fontWeight = FontWeight.Medium, fontSize = 18.rsp, style = TextStyle(brush = ZenScoreGradient))
                }
                Text(
                    text = "$totalMindfulDays DAY MILESTONE",
                    fontFamily = DepartureMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.rsp,
                    color = MilestoneMuted
                )
            }

            Spacer(modifier = Modifier.height(28.rdp))

            // Flame circle + big count + description
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(73.rdp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_streak_fire),
                        contentDescription = null,
                        modifier = Modifier.size(46.rdp)
                    )
                }
                Spacer(modifier = Modifier.width(14.rdp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$totalMindfulDays",
                            fontFamily = DepartureMono,
                            fontWeight = FontWeight.Normal,
                            fontSize = 32.rsp,
                            letterSpacing = (-0.96).sp,
                            color = MilestoneDaysColor
                        )
                        Spacer(modifier = Modifier.width(8.rdp))
                        Text(
                            text = "DAYS",
                            fontFamily = DepartureMono,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.rsp,
                            letterSpacing = (-0.96).sp,
                            color = MilestoneDim,
                            modifier = Modifier.padding(bottom = 5.rdp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.rdp))
                    Text(
                        text = "That's ${approxMonths(totalMindfulDays)} of days that ended above Zen score $zenScoreThreshold.",
                        fontFamily = Geist,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.rsp,
                        letterSpacing = (-0.12).sp,
                        lineHeight = 16.rsp,
                        color = MilestoneMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.rdp))

            MilestoneDotGrid(
                filledCells = totalMindfulDays,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.rdp)
            )

            Spacer(modifier = Modifier.height(16.rdp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Quiet the noise.",
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.rsp,
                    letterSpacing = (-0.16).sp,
                    color = MilestoneTagline
                )
                Text(
                    text = "Zenmodeos.com",
                    fontFamily = DepartureMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.rsp,
                    letterSpacing = (-0.12).sp,
                    color = MilestoneMuted
                )
            }
        }
    }
}

/**
 * Decorative contribution-style grid inside the milestone card. Figma's own node for
 * this area (2026:2206) carried no exported vector data — drawn procedurally here,
 * filling cells left-to-right/top-to-bottom in proportion to [filledCells] against a
 * fixed 30x4 grid, rather than reproducing exact source pixels we don't have.
 */
@Composable
private fun MilestoneDotGrid(filledCells: Int, modifier: Modifier = Modifier) {
    val filledColor = MilestoneDaysColor
    val emptyColor = Color.White.copy(alpha = 0.08f)
    Canvas(modifier = modifier) {
        val columns = 30
        val rows = 4
        val gap = 3.dp.toPx()
        val cell = ((size.width - gap * (columns - 1)) / columns)
            .coerceAtMost((size.height - gap * (rows - 1)) / rows)
        val totalWidth = cell * columns + gap * (columns - 1)
        val startX = (size.width - totalWidth) / 2f
        val totalCells = columns * rows
        val filled = filledCells.coerceIn(0, totalCells)
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val index = row * columns + col
                drawRoundRect(
                    color = if (index < filled) filledColor.copy(alpha = 0.85f) else emptyColor,
                    topLeft = Offset(startX + col * (cell + gap), row * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun MilestoneOutlineButton(text: String, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(47.rdp)
            .clip(RoundedCornerShape(50))
            .background(MilestoneOutlineBg)
            .border(1.rdp, MilestoneOutlineBorder.copy(alpha = 0.26f), RoundedCornerShape(50))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        Image(
            painter = painterResource(R.drawable.ic_download),
            contentDescription = null,
            modifier = Modifier.size(16.rdp)
        )
        Spacer(modifier = Modifier.width(8.rdp))
        Text(
            text = text,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 17.rsp,
            letterSpacing = (-0.35).sp,
            color = GoldDeltaText
        )
    }
}

@Composable
private fun MilestoneSolidButton(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(47.rdp)
            .clip(RoundedCornerShape(50))
            .background(MilestoneSolidBg)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 17.rsp,
            letterSpacing = (-0.35).sp,
            color = Color.White
        )
    }
}

/** "120" -> "One hundred and twenty". Placeholder-metric scale only (0-999). */
private fun numberWordsRaw(n: Int): String {
    if (n == 0) return "zero"
    val ones = arrayOf(
        "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
        "seventeen", "eighteen", "nineteen"
    )
    val tens = arrayOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

    if (n >= 1000) return n.toString()

    val parts = mutableListOf<String>()
    var rem = n
    if (rem >= 100) {
        parts += "${ones[rem / 100]} hundred"
        rem %= 100
        if (rem > 0) parts += "and"
    }
    when {
        rem in 1..19 -> parts += ones[rem]
        rem >= 20 -> {
            val onesDigit = rem % 10
            parts += if (onesDigit > 0) "${tens[rem / 10]}-${ones[onesDigit]}" else tens[rem / 10]
        }
    }
    return parts.joinToString(" ")
}

private fun numberToWords(n: Int): String =
    numberWordsRaw(n).replaceFirstChar { it.uppercase() }

private fun approxMonths(days: Int): String {
    val months = (days / 30).coerceAtLeast(1)
    return "${numberWordsRaw(months)} month${if (months == 1) "" else "s"}"
}

/**
 * Crops the milestone card out of the current frame and either shares it (matches the
 * old share-to-bitmap flow this replaces) or saves it as a PNG. Save targets the public
 * Pictures/ZenMode gallery folder via MediaStore on Android 10+; below that (no scoped
 * storage, and not worth a runtime WRITE_EXTERNAL_STORAGE prompt for a shrinking API
 * tail) it falls back to app-private storage.
 */
private suspend fun saveOrShareMilestoneCard(
    view: android.view.View,
    context: android.content.Context,
    cardBounds: Rect?,
    share: Boolean,
    totalMindfulDays: Int
) {
    try {
        val bounds = cardBounds ?: run {
            Log.e("StreakOverlayShare", "cardBounds is null")
            return
        }
        // Capture on Main thread (required by drawToBitmap)
        val fullBitmap = view.drawToBitmap()
        val left = bounds.left.toInt().coerceAtLeast(0)
        val top = bounds.top.toInt().coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(
            fullBitmap,
            left,
            top,
            bounds.width.toInt().coerceAtMost(fullBitmap.width - left),
            bounds.height.toInt().coerceAtMost(fullBitmap.height - top)
        )

        if (share) {
            withContext(Dispatchers.IO) {
                val imagesFolder = File(context.cacheDir, "shared_images")
                imagesFolder.mkdirs()
                val file = File(imagesFolder, "zenmode_milestone.png")
                file.outputStream().use { out -> cropped.compress(Bitmap.CompressFormat.PNG, 100, out) }

                withContext(Dispatchers.Main) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, "$totalMindfulDays days of intentional time with ZenMode.")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Streak"))
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                val fileName = "zenmode_milestone_${System.currentTimeMillis()}.png"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ZenMode")
                    }
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri == null) {
                        Log.e("StreakOverlayShare", "MediaStore insert() returned null")
                    } else {
                        val stream = resolver.openOutputStream(uri)
                        if (stream == null) {
                            Log.e("StreakOverlayShare", "openOutputStream() returned null for $uri")
                        } else {
                            stream.use { out -> cropped.compress(Bitmap.CompressFormat.PNG, 100, out) }
                            Log.i("StreakOverlayShare", "saved to $uri")
                        }
                    }
                } else {
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    dir?.mkdirs()
                    File(dir, fileName).outputStream().use { out ->
                        cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved to Pictures", Toast.LENGTH_SHORT).show()
                }
            }
        }
    } catch (e: Exception) {
        Log.e("StreakOverlayShare", "save/share failed", e)
    }
}

// ── Page Dots ─────────────────────────────────────────────────────
// The dock is gone. These mark the home pages: swipe left for Settings, swipe
// right for Zen Gold (ZenGoldActivity). Purely a visual echo of the swipe zone
// on the full-screen Box above — not an independent hit target.

@Composable
private fun PageDots(onSettingsClick: () -> Unit, onZenGoldClick: () -> Unit = {}) {
    val colors = ZenTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < -40f) {
                        change.consume()
                        onSettingsClick()
                    } else if (dragAmount > 40f) {
                        change.consume()
                        onZenGoldClick()
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
