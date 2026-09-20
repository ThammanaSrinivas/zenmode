package com.zenlauncher.zenmode.ui.screens

import com.zenlauncher.zenmode.Sfx
import com.zenlauncher.zenmode.ZenSound
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.drawToBitmap
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
import com.zenlauncher.zenmode.ui.components.BlazingFlame
import com.zenlauncher.zenmode.ui.components.MoodBackdrop
import com.zenlauncher.zenmode.ui.components.MoodSource
import com.zenlauncher.zenmode.ui.components.HomePage
import com.zenlauncher.zenmode.ui.components.HomePageDots
import com.zenlauncher.zenmode.ui.components.HomeReveal
import com.zenlauncher.zenmode.ui.components.pageSwipe
import androidx.compose.foundation.systemGestureExclusion
import com.zenlauncher.zenmode.ui.components.rememberCountUp
import com.zenlauncher.zenmode.ui.components.rememberReduceMotion
import com.zenlauncher.zenmode.ui.components.HomeRevealCue
import com.zenlauncher.zenmode.ui.components.revealPop
import com.zenlauncher.zenmode.ui.components.revealRise
import com.zenlauncher.zenmode.ui.components.revealSpin
import com.zenlauncher.zenmode.ui.components.revealStrike
import com.zenlauncher.zenmode.ui.components.taperedBorder
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
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
import java.time.LocalDate
import java.util.Locale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import com.zenlauncher.zenmode.AppGridPreferences
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.AppInfo
import com.zenlauncher.zenmode.AppLogic
import com.zenlauncher.zenmode.FileResult
import com.zenlauncher.zenmode.FileSearchRepository
import com.zenlauncher.zenmode.R
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.zenlauncher.zenmode.AppSearchRanking
import com.zenlauncher.zenmode.coreapi.ZenScore
import com.zenlauncher.zenmode.ui.components.saveImageToPictures
import com.zenlauncher.zenmode.ui.components.shareImage
import com.zenlauncher.zenmode.ui.components.taperedBorder
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.ZenTypography
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
import com.zenlauncher.zenmode.coreapi.PromisePreferences
import com.zenlauncher.zenmode.recap.RecapStore
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
private val HeaderIconTextGap: Dp @Composable get() = 6.rdp

// Figma node 2001:1504 — header text colours and gradients. Values live in colors.xml
// (SOURCE OF TRUTH: design tokens) — never inline a Color(0x...) literal here.
// Not private — the Zen Score share card (HomeShareOverlays.kt) draws its big number
// with the same left-to-right green→orange ramp the header uses, so the two match.
internal val ZenScoreGradient: Brush
    @Composable get() = Brush.linearGradient(
        listOf(
            colorResource(R.color.score_grad_start),
            colorResource(R.color.score_grad_mid),
            colorResource(R.color.score_orange)
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
    onZenGoldClick: () -> Unit = {},
    onZenScoreClick: () -> Unit = {},
    onGoogleSearch: (String) -> Unit,
    onPhoneClick: () -> Unit,
    onLockClick: () -> Unit,
    onInviteBuddyClick: () -> Unit,
    inviteButtonLabel: String = "Add Buddy",
    onSignInClick: () -> Unit,
    onBuddyCardClick: (() -> Unit)? = null,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    apps: List<AppInfo>,
    // Plays Home's entrance when the phone is unlocked onto it (see HomeMotion.kt).
    reveal: HomeRevealCue = HomeRevealCue(),
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    // Streaks and Gold have no page of their own, so their stat opens a shareable card
    // here (HomeShareOverlays.kt). Zen Score has a page — its tap opens that, and the
    // card lives behind the page's own "Share Zen Score".
    var showStreakOverlay by remember { mutableStateOf(false) }
    var showGoldOverlay by remember { mutableStateOf(false) }
    // Where the home pill sits on screen; the search bar opens in exactly that spot.
    var searchPillBounds by remember { mutableStateOf<Rect?>(null) }

    // Three variants, picked by today's screen time. The wash covers the whole screen,
    // pooling the mood colour in the middle and fading to cream at both edges.
    val todayMinutes = ((usage?.screenTimeInMillis ?: 0L) / 1000) / 60
    val mood = AppLogic.getMoodState(todayMinutes)
    // Every other page reads this so it opens already in today's colour.
    MoodSource.lastKnown = mood

    // Figma 2026:1207 — home stays behind search, blurred ~7.65px. No-op below API 31.
    val homeBlur by animateDpAsState(
        targetValue = if (showSearch) 8.rdp else 0.dp,
        animationSpec = tween(260),
        label = "home-blur"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            // No dock: Home is the middle of three pages — swipe right for Zen Score, left
            // for Zen Gold. Long-press to lock. Settings lives behind every page's ☰.
            .pageSwipe(onSwipeLeft = onZenGoldClick, onSwipeRight = onZenScoreClick)
            // Same reasoning as the Zen Score / Streaks icons below: on OEM ROMs with a
            // widened back-gesture edge zone (e.g. MIUI, often configured asymmetrically
            // per edge), a drag that starts near one screen edge can be intercepted by the
            // system before detectHorizontalDragGestures ever sees it — one swipe direction
            // silently does nothing while the other works. Exclude the whole page-swipe area.
            .systemGestureExclusion()
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {},
                onLongClick = onLockClick
            )
    ) {
        MoodBackdrop(mood)

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
                reveal = reveal,
                onZenScoreClick = onZenScoreClick,
                onStreakClick = { showStreakOverlay = true }
            )

            Spacer(modifier = Modifier.height(HeaderToGoldGap))

            // zone 2 · the reward row
            GoldInvestedRow(
                gold = goldInvested,
                changePercent = goldChangePercent,
                onClick = { showGoldOverlay = true },
                modifier = Modifier.revealRise(reveal, HomeReveal.GOLD)
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
                inviteButtonLabel = inviteButtonLabel,
                onSignInClick = onSignInClick,
                onBuddyCardClick = onBuddyCardClick,
                // My screen time lands first, then the bolt strikes, then my Zen Bro's /
                // Zen Circle's card — the comparison reads left to right.
                leftCardModifier = Modifier.revealPop(reveal, HomeReveal.MY_CARD),
                rightCardModifier = Modifier.revealPop(reveal, HomeReveal.BUDDY_CARD),
                boltModifier = Modifier.revealStrike(reveal, HomeReveal.BOLT),
                crownCue = reveal,
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

            HomePageDots(current = HomePage.HOME)

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
        // Compute milestone stats once per composition, re-derived when zenScore changes.
        val context = LocalContext.current
        val recapStore = remember { RecapStore(context) }
        val promiseHours = remember { PromisePreferences.getDailyHours(context) }
        val todayIsMindful = remember(zenScore) {
            AppLogic.isMindfulDay(
                screenTimeMinutes = todayMinutes,
                promiseHours = promiseHours
            )
        }
        val totalMindfulDays = remember(zenScore) { AppLogic.getTotalMindfulDays(recapStore, todayIsMindful) }
        val longestStreak = remember(zenScore) { AppLogic.getLongestStreak(recapStore, todayIsMindful) }
        val longestStreakDays = longestStreak?.days ?: 0
        val longestStreakRange = longestStreak?.let { AppLogic.formatStreakRange(it) } ?: ""

        AnimatedVisibility(
            visible = showStreakOverlay,
            modifier = Modifier.systemBarsPadding(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            StreakOverlay(
                onDismiss = { showStreakOverlay = false },
                totalMindfulDays = totalMindfulDays,
                longestStreakDays = longestStreakDays,
                longestStreakRange = longestStreakRange,
                currentStreakDays = streaks
            )
        }

        // Gold Invested overlay
        AnimatedVisibility(
            visible = showGoldOverlay,
            modifier = Modifier.systemBarsPadding(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            GoldInvestedOverlay(
                gold = goldInvested,
                changePercent = goldChangePercent,
                onDismiss = { showGoldOverlay = false }
            )
        }
    }
}

// ── Header ────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    zenScore: Int,
    streaks: Int,
    reveal: HomeRevealCue,
    onZenScoreClick: () -> Unit = {},
    onStreakClick: () -> Unit
) {
    val colors = ZenTheme.colors
    val shownScore = rememberCountUp(zenScore, reveal, HomeReveal.SCORE)
    val shownStreak = rememberCountUp(streaks, reveal, HomeReveal.STREAK, durationMillis = 600)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Zen Score — gradient mark from Figma node 2001:1504, exactly as tall as the
        // "Zen Score / 07/10" block beside it. Opens Zen Score (also a swipe right away).
        IconMatchedLabel(
            modifier = Modifier
                .clip(RoundedCornerShape(8.rdp))
                .clickable(onClickLabel = "Open Zen Score", onClick = onZenScoreClick)
                .systemGestureExclusion(),
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_zen_mark_gradient),
                    contentDescription = null,
                    modifier = Modifier.revealSpin(reveal, HomeReveal.SCORE)
                )
            }
        ) {
            Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                HeaderLabel("Zen Score")
                Spacer(modifier = Modifier.height(HeaderLineGap))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = ZenScore.format(shownScore),
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = HeaderValueSize,
                        lineHeight = HeaderValueSize,
                        style = HeaderTextStyle.copy(brush = ZenScoreGradient)
                    )
                    Text(
                        text = "/${ZenScore.MAX_DISPLAY}",
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.Medium,
                        fontSize = HeaderUnitSize,
                        lineHeight = HeaderUnitSize,
                        color = colors.textSecondary,
                        style = HeaderTextStyle
                    )
                }
            }
        }

        // Streaks — the flame burns as tall as the "13 days / Streaks" block beside it.
        IconMatchedLabel(
            modifier = Modifier
                .clip(RoundedCornerShape(8.rdp))
                .clickable(onClickLabel = "Open streaks", onClick = onStreakClick)
                .systemGestureExclusion(),
            icon = {
                BlazingFlame(
                    cue = reveal,
                    lit = streaks > 0,
                    contentDescription = null
                )
            }
        ) {
            Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                HeaderLabel("Streaks")
                Spacer(modifier = Modifier.height(HeaderLineGap))
                Row(verticalAlignment = Alignment.Bottom) {
                    // Solid ink, not the brand gradient: the gradient's yellow-green washed out
                    // on the green home wash. textPrimary flips dark/light with the theme.
                    Text(
                        text = shownStreak.toString(),
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = HeaderValueSize,
                        lineHeight = HeaderValueSize,
                        color = colors.textPrimary,
                        style = HeaderTextStyle
                    )
                    Spacer(modifier = Modifier.width(3.rdp))
                    Text(
                        text = if (streaks == 1) "day" else "days",
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.Medium,
                        fontSize = HeaderUnitSize,
                        lineHeight = HeaderUnitSize,
                        color = colors.textSecondary,
                        style = HeaderTextStyle
                    )
                }
            }
        }
    }
}

/** The small caption above each header number, shared so both blocks line up exactly. */
@Composable
private fun HeaderLabel(text: String) {
    Text(
        text = text,
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = HeaderLabelSize,
        lineHeight = HeaderLabelSize,
        color = ZenTheme.colors.textPrimary,
        style = HeaderTextStyle
    )
}

// Zen Score and Streaks share one type scale (label over value) so their blocks, and the
// icons sized to them, come out the same height and sit on the same lines.
private val HeaderLabelSize: TextUnit @Composable get() = 12.rsp
private val HeaderValueSize: TextUnit @Composable get() = 18.rsp
private val HeaderUnitSize: TextUnit @Composable get() = 12.rsp
private val HeaderLineGap: Dp @Composable get() = 3.rdp

/** Tight line boxes, so a two-line block's height is its type and nothing else. */
private val HeaderTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
)

/**
 * [icon] beside [label], with the icon sized to a square exactly as tall as the label block —
 * so the mark always matches its two lines of text, whatever the font scale.
 */
@Composable
private fun IconMatchedLabel(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit
) {
    val gap = HeaderIconTextGap
    Layout(contents = listOf(icon, label), modifier = modifier) { (iconMeasurables, labelMeasurables), constraints ->
        val gapPx = gap.roundToPx()
        val labelPlaceable = labelMeasurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val side = labelPlaceable.height
        val iconPlaceable = iconMeasurables.first().measure(Constraints.fixed(side, side))
        layout(side + gapPx + labelPlaceable.width, side) {
            iconPlaceable.place(0, 0)
            labelPlaceable.place(side + gapPx, 0)
        }
    }
}

// ── Gold Invested ─────────────────────────────────────────────────

/**
 * The Gold Invested pill (Figma node 71:6081). On Home it hugs its content under a top-only rule;
 * [fullWidth] is Zen Gold's variant (node 2026:1530) — ruled top and bottom, stretched to the
 * content width, with [trailing] (View all) inset from the right edge.
 */
@Composable
fun GoldInvestedRow(
    gold: String,
    changePercent: Int,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false,
    /** Home opens the shareable gold card from here; Zen Gold's own row isn't tappable. */
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    var amountVisible by remember { mutableStateOf(true) }
    val rule = colorResource(R.color.zen_700)
    // Figma draws this as CSS border-top (and border-bottom) on a 41.8dp radius: a rule that
    // is full width along the edge and thins to nothing as it curves down the sides.
    val radius = 41.777.rdp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
                .height(48.rdp)
                .taperedBorder(rule, radius, top = 1.rdp, bottom = if (fullWidth) 1.rdp else 0.dp)
                // No ripple: the pill has no filled surface to ripple inside, only a rule.
                .then(
                    if (onClick != null) Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClickLabel = "Open Gold Invested",
                        onClick = onClick
                    ) else Modifier
                )
                .padding(start = 13.rdp, end = if (fullWidth) 12.rdp else 13.rdp),
            horizontalArrangement = Arrangement.spacedBy(8.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_coin_gold),
                contentDescription = null,
                modifier = Modifier.size(38.rdp)
            )

            Column(horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GOLD INVESTED",
                        fontFamily = Geist,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.rsp,
                        color = GoldLabel
                    )
                    Spacer(modifier = Modifier.width(6.rdp))
                    Image(
                        painter = painterResource(R.drawable.ic_eye),
                        contentDescription = if (amountVisible) "Hide amount" else "Show amount",
                        modifier = Modifier
                            .width(13.rdp)
                            .height(9.rdp)
                            .clickable { amountVisible = !amountVisible },
                        colorFilter = ColorFilter.tint(GoldLabel)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.1.rdp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hiding blurs the amount (Figma's "Hide" variant: the same text under a
                    // 5.35px blur). Modifier.blur is a no-op below API 31 (no RenderEffect),
                    // which would leave the real figure fully readable despite "hidden" —
                    // fall back to masking the digits on those devices instead.
                    val canBlurAmount = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    val hideAmount = !amountVisible
                    Text(
                        text = if (hideAmount && !canBlurAmount) "\u20B9\u2022\u2022\u2022\u2022\u2022\u2022" else "\u20B9$gold",
                        fontFamily = DepartureMono,
                        fontSize = 17.rsp,
                        letterSpacing = (-0.85).sp,
                        color = GoldAmount,
                        modifier = if (hideAmount && canBlurAmount)
                            Modifier.blur(5.35.rdp, BlurredEdgeTreatment.Unbounded)
                        else Modifier
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

            if (trailing != null) {
                Spacer(modifier = Modifier.weight(1f))
                trailing()
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
        // Icons only, no labels on screen, so name the app for screen readers.
        modifier = Modifier
            .semantics { contentDescription = appInfo.label.toString() }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    ZenSound.play(Sfx.SELECT)
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
            .border(3.rdp, colors.textBrand.copy(alpha = 0.45f), RoundedCornerShape(percent = 50))
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
// Same weight as the resting pill's border, so pressing it doesn't thicken the outline.
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
private val SearchTopFade: Dp @Composable get() = 48.rdp

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
            AppSearchRanking.filter(
                apps = apps,
                query = query,
                label = { it.label.toString() },
                packageName = { it.packageName.toString() }
            )
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
                // Room above the first row, and a fade where a long list runs under the
                // status bar, so an overflowing result set reads as scrollable, not clipped.
                contentPadding = PaddingValues(top = SearchTopFade),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fadeTopEdge(SearchTopFade)
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

/** Fades content to transparent over the top [height] of the element. */
private fun Modifier.fadeTopEdge(height: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to Color.Black,
                startY = 0f,
                endY = height.toPx()
            ),
            blendMode = BlendMode.DstIn
        )
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

// The dark card's colour tokens, the sheet chrome around it and the Save/Share
// capture are shared with the Zen Score and Gold cards — see HomeShareOverlays.kt.

@Composable
private fun StreakOverlay(
    onDismiss: () -> Unit,
    totalMindfulDays: Int,
    longestStreakDays: Int,
    longestStreakRange: String,
    topPercentile: Int = AppConstants.PLACEHOLDER_MILESTONE_PERCENTILE,
    zenScoreThreshold: Int = AppConstants.MINDFUL_DAY_ZEN_SCORE_THRESHOLD,
    /** The same live count as the flame in Home's header. */
    currentStreakDays: Int = 0
) {
    val colors = ZenTheme.colors

    ShareSheet(
        eyebrow = "STREAKS",
        shareLabel = "Share my streaks",
        fileBaseName = "zenmode_milestone",
        shareText = "$totalMindfulDays days of intentional time with ZenMode.",
        chooserTitle = "Share Streak",
        onDismiss = onDismiss
    ) { cardModifier ->
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
            modifier = cardModifier,
            totalMindfulDays = totalMindfulDays,
            zenScoreThreshold = zenScoreThreshold
        )

        Spacer(modifier = Modifier.height(20.rdp))

        ShareStatLine(
            lead = "LONGEST \u00B7 $longestStreakDays DAYS",
            trail = "($longestStreakRange)"
        )

        Spacer(modifier = Modifier.height(10.rdp))

        ShareStatLine(
            lead = "CURRENT \u00B7 ${"%02d".format(Locale.US, currentStreakDays)} DAYS",
            trail = "(${currentStreakRange(currentStreakDays)})"
        )
    }
}

/** "SEP 11–PRESENT": the streak counts today, so it began [days] − 1 days ago. */
internal fun currentStreakRange(days: Int, today: LocalDate = LocalDate.now()): String {
    if (days <= 0) return "STARTS TODAY"
    val start = today.minusDays((days - 1).toLong())
    return "${StreakDateFormat.format(start).uppercase(Locale.US)}–PRESENT"
}

private val StreakDateFormat = java.time.format.DateTimeFormatter.ofPattern("MMM d", Locale.US)

@Composable
private fun MilestoneCard(
    totalMindfulDays: Int,
    zenScoreThreshold: Int,
    modifier: Modifier = Modifier
) {
    ShareCardFrame(stamp = "$totalMindfulDays DAY MILESTONE", modifier = modifier) {
        ShareCardHero(
            unit = "DAYS",
            caption = "That's ${approxMonths(totalMindfulDays)} of days that ended above Zen score $zenScoreThreshold.",
            badge = {
                Image(
                    painter = painterResource(R.drawable.ic_streak_fire),
                    contentDescription = null,
                    modifier = Modifier.size(46.rdp)
                )
            },
            value = { ShareCardValue(text = "$totalMindfulDays", color = ShareCardAmber) }
        )

        Spacer(modifier = Modifier.height(20.rdp))

        MilestoneDotGrid(
            filledCells = totalMindfulDays,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.rdp)
        )
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
    val filledColor = ShareCardAmber
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
