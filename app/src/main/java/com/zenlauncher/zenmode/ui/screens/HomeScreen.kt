package com.zenlauncher.zenmode.ui.screens

import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.zenlauncher.zenmode.AppInfo
import com.zenlauncher.zenmode.AppLogic
import com.zenlauncher.zenmode.MoodState
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.RedditMono
import com.zenlauncher.zenmode.ui.theme.Silkscreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

// ── Main Home Screen ──────────────────────────────────────────────

@Composable
fun HomeScreen(
    usage: DailyUsage?,
    streaks: Int,
    yesterdayChangePercent: Int?,
    showSearch: Boolean,
    onShowSearchChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onGoogleSearch: (String) -> Unit,
    onPhoneClick: () -> Unit,
    onLockClick: () -> Unit,
    onInviteBuddyClick: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    apps: List<AppInfo>
) {
    val colors = ZenTheme.colors

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bgPrimary)
                .padding(top = 48.dp)
        ) {
            // Header
            HomeHeader(streaks = streaks)

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Cards
            StatsCardsRow(
                usage = usage,
                yesterdayChangePercent = yesterdayChangePercent,
                onInviteBuddyClick = onInviteBuddyClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            // App Grid
            AppGridPager(
                apps = apps,
                onAppClick = onAppClick,
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
    }
}

// ── Header ────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(streaks: Int) {
    val colors = ZenTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Zen logo
        Image(
            painter = painterResource(R.drawable.logo_only_pins),
            contentDescription = "ZenMode",
            modifier = Modifier.size(32.dp)
        )

        // Streaks badge
        // Streaks badge — Silkscreen font, green bg pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(46.dp))
                .background(colors.borderFocus)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = "streaks: $streaks",
                fontFamily = Silkscreen,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                letterSpacing = (-1.3).sp,
                color = colors.bgPrimary
            )
        }
    }
}

// ── Stats Cards Row ───────────────────────────────────────────────

@Composable
private fun StatsCardsRow(
    usage: DailyUsage?,
    yesterdayChangePercent: Int?,
    onInviteBuddyClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // My Screen Time card
            MyScreenTimeCard(
                usage = usage,
                yesterdayChangePercent = yesterdayChangePercent,
                modifier = Modifier.weight(1f)
            )

            // Buddy Invite card
            BuddyInviteCard(
                onInviteBuddyClick = onInviteBuddyClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Flash icon between cards
        Image(
            painter = painterResource(R.drawable.flash),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp)
                .zIndex(1f)
        )
    }
}

// ── My Screen Time Card ───────────────────────────────────────────

@Composable
private fun MyScreenTimeCard(
    usage: DailyUsage?,
    yesterdayChangePercent: Int?,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val totalMillis = usage?.screenTimeInMillis ?: 0L
    val minutes = (totalMillis / 1000) / 60
    val hours = minutes / 60
    val mins = minutes % 60
    val moodState = AppLogic.getMoodState(minutes)
    val mindfulnessProgress = AppLogic.getMindfulnessPercentage(minutes)

    val cardBgColor = when (moodState) {
        MoodState.HAPPY -> Color(0x3300C700)
        MoodState.NEUTRAL -> Color(0x33EBDE27)
        MoodState.ANNOYED -> Color(0x33F1634F)
    }

    val borderColor = when (moodState) {
        MoodState.HAPPY -> colors.borderFocus
        MoodState.NEUTRAL -> Color(0xFFEBDE27)
        MoodState.ANNOYED -> Color(0xFFF1634F)
    }

    val faceRes = when (moodState) {
        MoodState.HAPPY -> R.drawable.face_happy
        MoodState.NEUTRAL -> R.drawable.face_neutral
        MoodState.ANNOYED -> R.drawable.face_annoyed
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBgColor)
            .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(bottom = 10.dp)
    ) {
        // Face with king crown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(faceRes),
                contentDescription = "Mood face",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
            // King crown
            Image(
                painter = painterResource(R.drawable.king),
                contentDescription = "King",
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopStart)
                    .offset(x = 8.dp, y = 4.dp)
            )
        }

        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            // Title row with percentage change
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "My Screen Time",
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = colors.textPrimary
                )
                if (yesterdayChangePercent != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (yesterdayChangePercent >= 0) "+" else ""}${yesterdayChangePercent}%",
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        color = if (yesterdayChangePercent <= 0) colors.textBrand else Color(0xFFF1634F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Time display: 00 HRS 37 MIN
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format("%02d", hours),
                    fontFamily = RedditMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "HRS",
                    fontFamily = RedditMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 8.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 5.dp, start = 2.dp, end = 6.dp)
                )
                Text(
                    text = String.format("%02d", mins),
                    fontFamily = RedditMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "MIN",
                    fontFamily = RedditMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 8.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Mindfulness bar
            MindfulnessBar(
                progress = mindfulnessProgress,
                moodState = moodState
            )
        }
    }
}

// ── Mindfulness Bar ───────────────────────────────────────────────

@Composable
private fun MindfulnessBar(
    progress: Int,
    moodState: MoodState
) {
    val colors = ZenTheme.colors
    val emptyColor = Color(0xFFD9D9D9)

    val fillStartColor = when (moodState) {
        MoodState.HAPPY -> colors.borderFocus
        MoodState.NEUTRAL -> Color(0xFFEBDE27)
        MoodState.ANNOYED -> Color(0xFFF1634F)
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Mindfulness",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Gradient segmented bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .drawBehind {
                    val segmentCount = 15
                    val gap = 3.dp.toPx()
                    val totalGap = gap * (segmentCount - 1)
                    val segmentWidth = (size.width - totalGap) / segmentCount
                    val filledCount =
                        (progress.toFloat() / 100 * segmentCount).toInt().coerceAtLeast(0)
                    val radius = CornerRadius(3.dp.toPx())

                    for (i in 0 until segmentCount) {
                        val left = i * (segmentWidth + gap)
                        val fraction = if (filledCount > 0) i.toFloat() / filledCount else 0f
                        val color = if (i < filledCount) {
                            androidx.compose.ui.graphics.lerp(
                                fillStartColor,
                                emptyColor,
                                fraction
                            )
                        } else {
                            emptyColor.copy(alpha = 0.3f)
                        }
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(left, 0f),
                            size = Size(segmentWidth, size.height),
                            cornerRadius = radius
                        )
                    }
                }
        )
    }
}

// ── Buddy Invite Card (Pre-Connect) ──────────────────────────────

@Composable
private fun BuddyInviteCard(
    onInviteBuddyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgSecondary)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
            .padding(bottom = 10.dp)
    ) {
        // Grey face
        Image(
            painter = painterResource(R.drawable.face_neutral),
            contentDescription = "Buddy face",
            alpha = 0.4f,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
        )

        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                text = "Get your Buddy!",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = buildAnnotatedString {
                    append("pick a wise one!")
                    withStyle(SpanStyle(fontSize = 10.sp)) {
                        append("\u2764\uFE0F")
                    }
                },
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Invite buddy button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.actionPrimary)
                    .clickable { onInviteBuddyClick() }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Invite buddy",
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colors.actionPrimaryText
                )
            }
        }
    }
}

// ── App Grid with Horizontal Pager ───────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridPager(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onLockClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    // Every page: 11 apps (lock is fixed outside pager, takes 1 slot visually)
    val appsPerPage = 11
    val pages = remember(apps) { apps.chunked(appsPerPage) }
    val totalPages = pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { totalPages })

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) { page ->
                val appsInPage = pages.getOrElse(page) { emptyList() }

                Box(
                    modifier = Modifier.fillMaxSize(),
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
                        items(appsInPage.size) { index ->
                            AppIconItem(
                                appInfo = appsInPage[index],
                                onClick = { onAppClick(appsInPage[index]) }
                            )
                        }
                    }
                }
            }
        }

        // Page indicators
        if (totalPages > 1) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(totalPages) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) colors.textBrand
                                else colors.textSecondary.copy(alpha = 0.4f)
                            )
                    )
                }
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
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ── App Icon Item ─────────────────────────────────────────────────

@Composable
private fun AppIconItem(
    appInfo: AppInfo,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ZenTheme.colors.bgSecondary),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(appInfo.icon)
                    }
                },
                modifier = Modifier.size(40.dp)
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
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { /* consume click */ }
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
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontFamily = CabinetGrotesque,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
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
                                fontSize = 16.sp,
                                color = colors.textSecondary
                            )
                        }
                        innerTextField()
                    }
                )

                if (query.isNotEmpty()) {
                    Text(
                        text = "\u2715",
                        fontSize = 18.sp,
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
                        AndroidView(
                            factory = { context ->
                                ImageView(context).apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    setImageDrawable(app.icon)
                                }
                            },
                            update = { imageView ->
                                imageView.setImageDrawable(app.icon)
                            },
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = app.label.toString(),
                            fontFamily = CabinetGrotesque,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
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
                                fontSize = 14.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
                .padding(horizontal = 12.dp)
                .border(
                    width = 1.5.dp,
                    color = colors.borderFocus,
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
                .clickable { onSearchClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Search apps & everything",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
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
