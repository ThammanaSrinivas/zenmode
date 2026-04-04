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
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import com.zenlauncher.zenmode.AppInfo
import com.zenlauncher.zenmode.AppLogic
import com.zenlauncher.zenmode.MoodState
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.RedditMono
import com.zenlauncher.zenmode.ui.theme.Silkscreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rsp
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.components.StatsCardsRow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bgPrimary)
                .padding(top = 48.dp)
        ) {
            // Header
            HomeHeader(
                streaks = streaks,
                onStreakClick = { showStreakOverlay = true }
            )

            StatsCardsRow(
                usage = usage,
                yesterdayChangePercent = yesterdayChangePercent,
                hasBuddies = hasBuddies,
                buddyStats = buddyStats,
                isSignedIn = isSignedIn,
                onInviteBuddyClick = onInviteBuddyClick,
                onSignInClick = onSignInClick,
                onBuddyCardClick = onBuddyCardClick,
                modifier = Modifier.padding(horizontal = 28.rdp)
            )

            // App Grid
            AppGridPager(
                apps = apps,
                onAppClick = onAppClick,
                onAppLongClick = onAppLongClick,
                onAppInfoClick = onAppInfoClick,
                onLockClick = onLockClick,
                modifier = Modifier.weight(1f)
            )

            // Bottom Dock
            BottomDock(
                onSettingsClick = onSettingsClick,
                onSearchClick = { onShowSearchChange(true) },
                onPhoneClick = onPhoneClick
            )
        }

        // Search overlay
        AnimatedVisibility(
            visible = showSearch,
            enter = fadeIn(),
            exit = fadeOut()
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
private fun HomeHeader(streaks: Int, onStreakClick: () -> Unit) {
    val colors = ZenTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Zen logo — centered
        Image(
            painter = painterResource(R.drawable.app_icon),
            contentDescription = "ZenMode",
            modifier = Modifier.size(44.dp)
        )

        // Streaks badge — pinned to end
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(46.dp))
                .background(colors.borderFocus)
                .clickable { onStreakClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "streaks: $streaks",
                fontFamily = Silkscreen,
                fontWeight = FontWeight.Normal,
                fontSize = 11.rsp,
                letterSpacing = (-1.3).sp,
                color = colors.bgPrimary
            )
        }
    }
}

// ── App Grid with Horizontal Pager ───────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridPager(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    onAppInfoClick: (AppInfo) -> Unit = {},
    onLockClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val coroutineScope = rememberCoroutineScope()
    // Every page: 11 apps (lock is fixed outside pager, takes 1 slot visually)
    val appsPerPage = 11
    val pages = remember(apps) { apps.chunked(appsPerPage) }
    val totalPages = pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { totalPages })

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) { page ->
                val appsInPage = pages.getOrElse(page) { emptyList() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(44.dp),
                        userScrollEnabled = false
                    ) {
                        // Lock is first item on every page so it stays persistent
                        item {
                            LockItem(onClick = onLockClick)
                        }
                        items(appsInPage.size, key = { index -> appsInPage[index].packageName.toString() }) { index ->
                            AppIconItem(
                                appInfo = appsInPage[index],
                                onClick = { onAppClick(appsInPage[index]) },
                                onLongClick = { onAppLongClick(appsInPage[index]) },
                                onAppInfoClick = { onAppInfoClick(appsInPage[index]) }
                            )
                        }
                    }
                }
            }

            // Page indicators — directly below grid, draggable, only selected segment lit
            if (totalPages > 1) {
                val litColor = colors.textBrand
                val dimColor = colors.textSecondary.copy(alpha = 0.25f)
                Box(
                    modifier = Modifier
                        .padding(top = 36.dp)
                        .width(212.rdp)
                        .height(16.dp)
                        .pointerInput(totalPages) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                val targetPage = (fraction * totalPages).toInt().coerceIn(0, totalPages - 1)
                                if (targetPage != pagerState.currentPage) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(targetPage)
                                    }
                                }
                            }
                        }
                        .drawBehind {
                            val barWidth = 2.dp.toPx()
                            val gap = 4.dp.toPx()
                            val barCount = ((size.width + gap) / (barWidth + gap)).toInt()
                            if (barCount <= 0) return@drawBehind
                            val barsPerPage = barCount / totalPages
                            val startLit = pagerState.currentPage * barsPerPage
                            val endLit = if (pagerState.currentPage == totalPages - 1) barCount
                                else startLit + barsPerPage
                            val cornerRadius = CornerRadius(barWidth / 2f)
                            for (i in 0 until barCount) {
                                val left = i * (barWidth + gap)
                                drawRoundRect(
                                    color = if (i in startLit until endLit) litColor else dimColor,
                                    topLeft = Offset(left, 0f),
                                    size = Size(barWidth, size.height),
                                    cornerRadius = cornerRadius
                                )
                            }
                        }
                )
            }
        }
    }
}

// ── Lock Item (First Grid Slot) ───────────────────────────────────

@Composable
private fun LockItem(onClick: () -> Unit) {
    val colors = ZenTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.bgSecondary),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.lock),
                contentDescription = "Lock phone",
                modifier = Modifier.size(36.dp)
            )
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
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_XY
                        setImageDrawable(appInfo.icon)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )

            // Pin star indicator — bottom-right of icon (outside clip)
            if (appInfo.isPinned) {
                Image(
                    painter = painterResource(id = R.drawable.star),
                    contentDescription = "Pinned",
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                )
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
                        text = if (appInfo.isPinned) "Unpin" else "Pin",
                        fontFamily = CabinetGrotesque
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
                        text = "App Info",
                        fontFamily = CabinetGrotesque
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

// ── Search Overlay ────────────────────────────────────────────

@Composable
private fun SearchOverlay(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onGoogleSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ZenTheme.colors
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current


    val filteredApps by remember(query, apps) {
        derivedStateOf {
            if (query.isBlank()) emptyList()
            else apps.filter {
                it.label.toString().contains(query, ignoreCase = true)
            }
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
            .background(colors.bgPrimary.copy(alpha = 0.95f))
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
                .padding(horizontal = 20.dp, vertical = 48.dp)
        ) {
            // Search input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, colors.borderFocus, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        fontFamily = CabinetGrotesque,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.rsp,
                        color = colors.textPrimary
                    ),
                    cursorBrush = SolidColor(colors.textBrand),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search apps & everything",
                                fontFamily = CabinetGrotesque,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.rsp,
                                color = colors.textSecondary
                            )
                        }
                        innerTextField()
                    }
                )

                if (query.isNotEmpty()) {
                    Text(
                        text = "\u2715",
                        fontSize = 18.rsp,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .clickable { query = "" }
                            .padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Matching apps
                items(filteredApps, key = { it.packageName.toString() }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAppClick(app) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Transparent),
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
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = app.label.toString(),
                            fontFamily = CabinetGrotesque,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.rsp,
                            color = colors.textPrimary
                        )
                    }
                }

                // Google fallback
                if (query.isNotBlank()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.bgSecondary)
                                .clickable { onGoogleSearch(query) }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_google),
                                contentDescription = "Google",
                                modifier = Modifier.size(24.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.textBrand)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Search Google for \"$query\"",
                                fontFamily = CabinetGrotesque,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.rsp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
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
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colors.bgSecondary)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { /* consume click */ }
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 32.dp),
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

            Spacer(modifier = Modifier.height(20.dp))

            // Title row: "My Zenmode Streak" + top-right icon group
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "My Zenmode Streak",
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.rsp,
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                // Top-right: blurred shuriken + app_icon + arrow
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Blurred shuriken behind
                    Image(
                        painter = painterResource(R.drawable.resistence_screen_happy_shuriken),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .blur(8.dp)
                            .alpha(0.5f),
                        contentScale = ContentScale.Fit
                    )
                    // App icon
                    Image(
                        painter = painterResource(R.drawable.app_icon),
                        contentDescription = null,
                        modifier = Modifier.size(47.dp)
                    )
                    // Arrow hitting the icon — tip touches center of app_icon
                    Image(
                        painter = painterResource(R.drawable.arrow),
                        contentDescription = null,
                        modifier = Modifier
                            .width(55.dp)
                            .offset(x = -26.dp, y = 18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Streak count row: king icon + "N Days Streak"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.king),
                    contentDescription = "Crown",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%02d Days Streak", streaks),
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.rsp,
                    color = colors.textPrimary
                )
            }

            Text(
                text = streakSubtitle,
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Normal,
                fontSize = 14.rsp,
                color = colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Weekly calendar row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = colors.textSecondary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 16.dp)
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
                                fontFamily = CabinetGrotesque,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.rsp,
                                color = colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isFuture) {
                                // Show date number for future days
                                Text(
                                    text = "${dayDate.dayOfMonth}",
                                    fontFamily = RedditMono,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.rsp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.size(28.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                // Show shuriken based on mood
                                val minutes = (millis / 1000) / 60
                                val mood = AppLogic.getMoodState(minutes)
                                val dayShurikenRes = when (mood) {
                                    MoodState.HAPPY -> R.drawable.resistence_screen_happy_shuriken
                                    MoodState.NEUTRAL -> R.drawable.resistence_screen_neutral_shuriken
                                    MoodState.ANNOYED -> R.drawable.resistence_screen_annoyed_shuriken
                                }
                                Image(
                                    painter = painterResource(dayShurikenRes),
                                    contentDescription = "$mood",
                                    modifier = Modifier.size(32.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Share my streak button
            Image(
                painter = painterResource(R.drawable.button_share_my_streak),
                contentDescription = "Share my streak",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
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

// ── Bottom Dock ───────────────────────────────────────────────────

@Composable
private fun BottomDock(
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPhoneClick: () -> Unit
) {
    val colors = ZenTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Settings gear
        Image(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = "Settings",
            modifier = Modifier
                .size(32.dp)
                .clickable { onSettingsClick() },
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.textBrand)
        )

        // Search bar
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.5.dp,
                    color = colors.borderFocus,
                    shape = RoundedCornerShape(18.dp)
                )
                .clip(RoundedCornerShape(18.dp))
                .clickable { onSearchClick() }
                .padding(start = 14.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Search apps & everything",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Medium,
                fontSize = 13.rsp,
                color = colors.textSecondary
            )
        }

        // Phone icon
        Image(
            painter = painterResource(R.drawable.ic_phone),
            contentDescription = "Phone",
            modifier = Modifier
                .size(32.dp)
                .clickable { onPhoneClick() },
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.textBrand)
        )
    }
}
