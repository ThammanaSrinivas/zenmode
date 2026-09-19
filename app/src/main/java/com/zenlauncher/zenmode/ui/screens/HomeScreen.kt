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
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.em
import com.zenlauncher.zenmode.ui.components.runningGradientStroke
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import android.content.ContentValues
import android.provider.MediaStore
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
    onZenScoreClick: () -> Unit = {},
    onGoogleSearch: (String) -> Unit,
    onPhoneClick: () -> Unit,
    onLockClick: () -> Unit,
    onInviteBuddyClick: () -> Unit,
    onSignInClick: () -> Unit,
    onBuddyCardClick: (() -> Unit)? = null,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    apps: List<AppInfo>,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    var showStreakOverlay by remember { mutableStateOf(false) }
    // Where the home pill sits on screen; the search bar opens in exactly that spot.
    var searchPillBounds by remember { mutableStateOf<Rect?>(null) }

    // Three variants, picked by today's screen time. The wash covers the whole screen,
    // pooling the mood colour in the middle and fading to cream at both edges.
    val todayMinutes = ((usage?.screenTimeInMillis ?: 0L) / 1000) / 60
    val mood = AppLogic.getMoodState(todayMinutes)
    val wash = colors.moodWash(mood)

    // Figma 2026:1207 — home stays behind search, blurred ~7.65px. No-op below API 31.
    val homeBlur by animateDpAsState(
        targetValue = if (showSearch) 8.rdp else 0.dp,
        animationSpec = tween(260),
        label = "home-blur"
    )

    Box(
        modifier = modifier
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
    ) {
        // Insets live on the children, not this Box, so the search scrim can run
        // edge to edge behind the status and navigation bars.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .then(if (homeBlur > 0.dp) Modifier.blur(homeBlur) else Modifier)
        ) {
            Spacer(modifier = Modifier.height(TopToHeaderGap))

            // zone 1 · identity + reward
            HomeHeader(
                zenScore = zenScore,
                streaks = streaks,
                onZenScoreClick = onZenScoreClick,
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
                onAppLongClick = onAppLongClick
            )

            Spacer(modifier = Modifier.weight(1f).heightIn(min = AppsToSearchGap))

            // zone 5 · search + page dots
            SearchPill(
                onClick = { onShowSearchChange(true) },
                modifier = Modifier.onGloballyPositioned { searchPillBounds = it.boundsInWindow() }
            )

            Spacer(modifier = Modifier.height(SearchToDotsGap))

            PageDots(onSettingsClick = onSettingsClick, onZenGoldClick = onZenGoldClick)

            Spacer(modifier = Modifier.height(DotsToBottomGap))
        }

        // Search overlay — the scrim fades over the blurring home screen while the bar
        // ignites its stroke in place over the home pill (see ZenSearchBar)
        AnimatedVisibility(
            visible = showSearch,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180))
        ) {
            SearchOverlay(
                apps = apps,
                anchorBounds = searchPillBounds,
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
            modifier = Modifier.systemBarsPadding(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
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
    onZenScoreClick: () -> Unit = {},
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
        // Zen Score — real gradient mark from Figma node 2001:1504. Tap target for
        // ZenScoreActivity (Figma node 2026:2035, ui/screens/ZenScoreScreen.kt).
        Row(
            modifier = Modifier.clickable { onZenScoreClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    onAppLongClick: (AppInfo) -> Unit = {}
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
                            onLongClick = { onAppLongClick(app) }
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
// Long-press goes straight to App info — it's the only action, so a
// single-item menu would just be an extra tap for no choice.

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIconItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val view = LocalView.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                onLongClick()
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
    }
}

// ── Search Pill ───────────────────────────────────────────────────

@Composable
private fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors

    Row(
        modifier = modifier
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
// Figma node 2026:1207 — "centralised search / active state". The home screen stays
// put, blurred under a dark scrim; everything is anchored to the bottom, stacking up
// from the search bar: the Google row sits right above the bar, then Apps, then
// on-device files. Sources are unchanged from before (installed apps, MediaStore
// files behind FILE_SEARCH_ENABLED, Google handed off to the system).

// Matches the home pill (not Figma's 46) so the pressed state lands exactly on top of it.
private val SearchBarHeight: Dp @Composable get() = SearchPillHeight
private val SearchStrokeWidth: Dp @Composable get() = 3.rdp
private val SearchGlyphSize: Dp @Composable get() = 19.2.rdp
// Results sit 26dp in from the bar's edge (x=56 against the bar's x=30).
private val SearchResultInset: Dp @Composable get() = 26.rdp
private val SearchTileSize: Dp @Composable get() = 30.rdp
private val SearchRowGap: Dp @Composable get() = 16.rdp
private val SearchHeaderGap: Dp @Composable get() = 24.rdp
private val SearchSectionGap: Dp @Composable get() = 28.rdp
private val SearchBarGap: Dp @Composable get() = 26.rdp
private const val SearchCollapsedRows = 3

// One trip of the gradient round the pill. Slow enough to read as calm, quick
// enough to read as "listening".
private const val StrokeRunMillis = 2800
private const val StrokeTraceMillis = 720
private const val StrokeGlowRest = 0.45f

private sealed interface SearchRow {
    val key: String

    data class Header(val label: String) : SearchRow {
        override val key = "header:$label"
    }
    data class App(val app: AppInfo) : SearchRow {
        override val key = "app:${app.packageName}"
    }
    data class File(val file: FileResult) : SearchRow {
        override val key = "file:${file.uri}"
    }
    data class More(val section: String, val count: Int, val noun: String) : SearchRow {
        override val key = "more:$section"
    }
    data object FilePermission : SearchRow {
        override val key = "file-permission"
    }
    data class Google(val query: String) : SearchRow {
        override val key = "google"
    }
    data class Gap(val section: String) : SearchRow {
        override val key = "gap:$section"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchOverlay(
    apps: List<AppInfo>,
    anchorBounds: Rect?,
    onAppClick: (AppInfo) -> Unit,
    onGoogleSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

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
        if (!fileSearchEnabled || !hasFilePermission || query.isBlank()) {
            fileResults = emptyList()
            return@LaunchedEffect
        }
        // MediaStore is a disk query — let a burst of typing settle first. A newer
        // keystroke cancels this effect, so only the last query ever hits the provider.
        delay(120)
        fileResults = FileSearchRepository.search(context, query)
    }

    // "+N more" expands a section; a new query starts collapsed again.
    var appsExpanded by remember(query) { mutableStateOf(false) }
    var filesExpanded by remember(query) { mutableStateOf(false) }

    // Top-to-bottom, as Figma draws it.
    val rows = buildList {
        if (query.isBlank()) return@buildList

        if (fileSearchEnabled && (!hasFilePermission || fileResults.isNotEmpty())) {
            add(SearchRow.Header("ON DEVICE FILES"))
            if (!hasFilePermission) {
                add(SearchRow.FilePermission)
            } else {
                val shown = if (filesExpanded) fileResults else fileResults.take(SearchCollapsedRows)
                shown.forEach { add(SearchRow.File(it)) }
                val hidden = fileResults.size - shown.size
                if (hidden > 0) add(SearchRow.More("files", hidden, "files"))
            }
            add(SearchRow.Gap("files"))
        }

        if (filteredApps.isNotEmpty()) {
            add(SearchRow.Header("APPS"))
            val shown = if (appsExpanded) filteredApps else filteredApps.take(SearchCollapsedRows)
            shown.forEach { add(SearchRow.App(it)) }
            val hidden = filteredApps.size - shown.size
            if (hidden > 0) add(SearchRow.More("apps", hidden, if (hidden == 1) "app" else "apps"))
            add(SearchRow.Gap("apps"))
        }

        add(SearchRow.Google(query))
    }

    BackHandler(enabled = true) {
        onDismiss()
    }

    var dragY by remember { mutableStateOf(0f) }
    val listState = rememberLazyListState()

    // Pin the bar to the home pill: same bottom edge, so pressing search changes the
    // pill's look, never its position. Only a docked keyboard that would cover it
    // pushes it up. Until the pill has been measured, fall back to the nav bar gap.
    val density = LocalDensity.current
    var overlayBottomInWindow by remember { mutableStateOf<Float?>(null) }
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val navBottomPx = WindowInsets.navigationBars.getBottom(density)
    val anchoredGapPx = if (anchorBounds != null && overlayBottomInWindow != null) {
        (overlayBottomInWindow!! - anchorBounds.bottom).coerceAtLeast(0f)
    } else {
        with(density) { navBottomPx + SearchBarGap.toPx() }
    }
    val keyboardGapPx = if (imeBottomPx > 0) imeBottomPx + with(density) { 12.rdp.toPx() } else 0f
    val barBottomGap = with(density) { maxOf(anchoredGapPx, keyboardGapPx).toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayBottomInWindow = it.boundsInWindow().bottom }
            .background(searchScrim())
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragY > 150f) onDismiss()
                        dragY = 0f
                    },
                    onVerticalDrag = { _, dragAmount -> dragY += dragAmount }
                )
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, dragY.coerceAtLeast(0f).roundToInt()) }
                .statusBarsPadding()
                .padding(bottom = barBottomGap)
        ) {
            // Reverse layout pins the list to the bar: short result sets hug the
            // bottom, long ones scroll up from it. Rows are emitted bottom-first.
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = ScreenMargin)
            ) {
                items(rows.asReversed(), key = { it.key }) { row ->
                    val itemModifier = Modifier.animateItem(
                        fadeInSpec = tween(200),
                        fadeOutSpec = tween(120)
                    )
                    when (row) {
                        is SearchRow.Header -> SearchSectionHeader(row.label, itemModifier)
                        is SearchRow.App -> AppResultRow(
                            app = row.app,
                            onClick = { onAppClick(row.app) },
                            modifier = itemModifier
                        )
                        is SearchRow.File -> FileResultRow(
                            file = row.file,
                            onClick = {
                                onDismiss()
                                FileSearchRepository.open(context, row.file)
                            },
                            modifier = itemModifier
                        )
                        is SearchRow.More -> SearchMoreRow(
                            text = "+${row.count} more ${row.noun}",
                            onClick = {
                                if (row.section == "apps") appsExpanded = true
                                else filesExpanded = true
                            },
                            modifier = itemModifier
                        )
                        SearchRow.FilePermission -> FilePermissionRow(
                            onGrantClick = {
                                permissionLauncher.launch(FileSearchRepository.requiredPermissions)
                            },
                            modifier = itemModifier
                        )
                        is SearchRow.Google -> GoogleFallbackRow(
                            query = row.query,
                            onClick = { onGoogleSearch(row.query) },
                            modifier = itemModifier
                        )
                        is SearchRow.Gap -> Spacer(itemModifier.height(SearchSectionGap - 8.rdp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(SearchBarGap - 8.rdp))

            // Same modifier chain as SearchPill's outer width, so the edges line up too.
            ZenSearchBar(
                modifier = Modifier.padding(horizontal = ScreenMargin),
                query = query,
                onQueryChange = { query = it },
                onSubmit = {
                    val top = filteredApps.firstOrNull()
                    if (top != null) onAppClick(top) else if (query.isNotBlank()) onGoogleSearch(query)
                }
            )
        }
    }

    // New results arrive at the bottom edge; keep the bar-side end in view.
    LaunchedEffect(query) { listState.scrollToItem(0) }
}

/** Figma blurs the home screen 7.65px under a 67% black scrim; we run it at 85% because
 *  the cream home wash bleeds through 67% and washes out the white result text.
 *  RenderEffect blur only exists from API 31, so below that the scrim deepens further. */
@Composable
private fun searchScrim(): Color {
    val scrim = colorResource(R.color.search_scrim)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) scrim
    else scrim.copy(alpha = 0.92f)
}

// ── Search bar ────────────────────────────────────────────────────
// Pressing search on home ignites the stroke: it traces out from the glyph in both
// directions, meets on the right, and then the ZenMode OS gradient keeps running
// round the pill for as long as search is open. Every keystroke flares the halo.

@Composable
private fun ZenSearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val white = colorResource(R.color.white)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val reduceMotion = rememberReduceMotion()

    val strokeColors = listOf(
        colorResource(R.color.os_grad_ember),  // left
        colorResource(R.color.os_grad_amber),  // bottom
        colorResource(R.color.os_grad_glow),   // right
        colorResource(R.color.os_grad_amber)   // top
    )

    val trace = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val glow = remember { Animatable(0f) }
    val running = rememberInfiniteTransition(label = "search-stroke")
    val phase by running.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(StrokeRunMillis, easing = LinearEasing)),
        label = "search-stroke-phase"
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        if (reduceMotion) {
            glow.snapTo(StrokeGlowRest)
            return@LaunchedEffect
        }
        launch { trace.animateTo(1f, tween(StrokeTraceMillis, easing = FastOutSlowInEasing)) }
        glow.animateTo(1f, tween(StrokeTraceMillis, easing = FastOutSlowInEasing))
        glow.animateTo(StrokeGlowRest, tween(900, easing = FastOutSlowInEasing))
    }

    // Keystroke flare. Skips the initial empty value so opening doesn't double-flare.
    var lastQuery by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (query == lastQuery || reduceMotion) return@LaunchedEffect
        lastQuery = query
        if (trace.value < 1f) return@LaunchedEffect
        glow.animateTo(1f, tween(90))
        glow.animateTo(StrokeGlowRest, tween(650, easing = FastOutSlowInEasing))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SearchBarHeight)
            .runningGradientStroke(
                colors = strokeColors,
                strokeWidth = SearchStrokeWidth,
                trace = { trace.value },
                phase = { if (reduceMotion) 0f else phase },
                glow = { glow.value }
            )
            .clip(RoundedCornerShape(percent = 50))
            // swallow taps so they don't fall through to the scrim's dismiss
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusRequester.requestFocus() }
            // 19 + 19.2 glyph + 11 gap puts the glyph centre and text start where
            // SearchPill has them, so nothing jumps when search opens
            .padding(start = 19.rdp, end = 4.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_search_v3),
            contentDescription = null,
            modifier = Modifier.size(SearchGlyphSize),
            colorFilter = ColorFilter.tint(colorResource(R.color.search_glyph))
        )
        Spacer(modifier = Modifier.width(11.rdp))

        val textStyle = TextStyle(
            fontFamily = Geist,
            fontWeight = FontWeight.Normal,
            fontSize = 16.rsp,
            letterSpacing = (-0.01).em,
            color = white
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) keyboardController?.show()
                },
            textStyle = textStyle,
            cursorBrush = SolidColor(colorResource(R.color.search_glyph)),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search apps, files & everything",
                        style = textStyle,
                        color = white.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
                innerTextField()
            }
        )

        // Keep the slot so the text field doesn't reflow when the clear appears.
        Box(
            modifier = Modifier
                .size(40.rdp)
                .clip(CircleShape)
                .then(
                    if (query.isNotEmpty()) Modifier.clickable { onQueryChange("") }
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (query.isNotEmpty()) {
                Image(
                    painter = painterResource(R.drawable.ic_search_clear),
                    contentDescription = "Clear search",
                    modifier = Modifier.size(width = 9.5.rdp, height = 9.rdp),
                    colorFilter = ColorFilter.tint(white)
                )
            }
        }
    }
}

/** Honours the system "Remove animations" setting — the stroke still appears, it just
 *  doesn't trace or run. */
@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}

// ── Search rows ───────────────────────────────────────────────────
// Each row is a 30dp tile + label with 8dp of vertical padding, so the visual 16dp
// gap from Figma doubles as a 46dp touch target.

@Composable
private fun SearchSectionHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.rsp,
        letterSpacing = (-0.01).em,
        color = colorResource(R.color.white),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = SearchResultInset, bottom = SearchHeaderGap - 8.rdp)
    )
}

@Composable
private fun SearchResultRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tile: @Composable () -> Unit,
    label: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.rdp))
            .clickable { onClick() }
            .padding(horizontal = SearchResultInset, vertical = SearchRowGap / 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(SearchTileSize), contentAlignment = Alignment.Center) {
            tile()
        }
        Spacer(modifier = Modifier.width(16.rdp))
        label()
    }
}

@Composable
private fun SearchRowLabel(text: String, weight: FontWeight = FontWeight.Normal, alpha: Float = 1f) {
    Text(
        text = text,
        fontFamily = Geist,
        fontWeight = weight,
        fontSize = 16.rsp,
        letterSpacing = (-0.01).em,
        color = colorResource(R.color.white).copy(alpha = alpha),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun AppResultRow(app: AppInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SearchResultRow(
        onClick = onClick,
        modifier = modifier,
        tile = {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_XY
                        setImageDrawable(app.icon)
                    }
                },
                update = { imageView -> imageView.setImageDrawable(app.icon) },
                modifier = Modifier
                    .fillMaxSize()
                    // the home grid's tile radius, scaled to the 30dp tile
                    .clip(RoundedCornerShape(AppTileRadius * (30f / 47f)))
            )
        },
        label = { SearchRowLabel(app.label.toString()) }
    )
}

@Composable
private fun FileResultRow(file: FileResult, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SearchResultRow(
        onClick = onClick,
        modifier = modifier,
        tile = { SearchFileTile(tint = colorResource(R.color.white)) },
        label = { SearchRowLabel(file.displayName) }
    )
}

@Composable
private fun SearchFileTile(tint: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.rdp))
            .background(colorResource(R.color.search_tile)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_file),
            contentDescription = null,
            modifier = Modifier.size(16.rdp),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}

@Composable
private fun SearchMoreRow(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Empty tile keeps the label on the same column as the rows above it.
    SearchResultRow(
        onClick = onClick,
        modifier = modifier,
        tile = {},
        label = { SearchRowLabel(text, weight = FontWeight.SemiBold) }
    )
}

@Composable
private fun FilePermissionRow(onGrantClick: () -> Unit, modifier: Modifier = Modifier) {
    val glyph = colorResource(R.color.search_glyph)
    SearchResultRow(
        onClick = onGrantClick,
        modifier = modifier,
        tile = { SearchFileTile(tint = glyph) },
        label = {
            Text(
                text = "Allow file access to search your files",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.rsp,
                letterSpacing = (-0.01).em,
                color = glyph,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun GoogleFallbackRow(query: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.rdp))
            .clickable { onClick() }
            .padding(horizontal = SearchResultInset, vertical = 8.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_search_v3),
            contentDescription = "Google",
            modifier = Modifier.size(SearchGlyphSize),
            colorFilter = ColorFilter.tint(colorResource(R.color.white))
        )
        Spacer(modifier = Modifier.width(10.rdp))
        Text(
            text = "Search Google for “$query”",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 16.rsp,
            letterSpacing = (-0.01).em,
            color = colorResource(R.color.white).copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
