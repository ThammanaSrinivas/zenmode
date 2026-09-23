package com.zenlauncher.zenmode.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalInspectionMode
import com.zenlauncher.zenmode.ui.components.rememberReduceMotion
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.minDimension
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.SessionEventType
import com.zenlauncher.zenmode.coreapi.ZenScore
import com.zenlauncher.zenmode.ui.components.HomePage
import com.zenlauncher.zenmode.ui.components.MoodBackdrop
import com.zenlauncher.zenmode.ui.components.PinnedPageFooter
import com.zenlauncher.zenmode.ui.components.moodWashColors
import com.zenlauncher.zenmode.ui.components.rememberTodayMood
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zenlauncher.zenmode.ui.components.dropShadow
import com.zenlauncher.zenmode.ui.components.openSettings
import com.zenlauncher.zenmode.ui.components.pageSwipe
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.components.taperedBorder
import com.zenlauncher.zenmode.ui.components.zenOverlayBlur
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import java.time.LocalDate
import java.time.format.TextStyle as TextStyleJava
import java.util.Locale
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// ── ZM_OS v3: Zen Score (Home right-swipe page) ─────────────────────
// Figma node 2026:2035 ("home/ variant-green/ Zen Score"). The left-hand of the three home
// pages: reached by swiping right on Home or tapping Home's Zen Score; swipe left (or back)
// returns. The header greets the user; their avatar opens Settings, like every ☰ does.
//
// Category breakdown and session log are real, from SessionLogRepository (core-api) via
// ZenScoreActivity — see that file for how [categories]/[sessionLog] are built.

private val ScreenMargin: Dp @Composable get() = 30.rdp

data class ZenScoreCategory(val label: String, val percent: Int, val colorRes: Int)

data class ZenSessionLogEntry(val duration: String, val appName: String, val type: SessionEventType)

@Composable
fun ZenScoreScreen(
    /** Today's score in tenths (93 = 9.3), from [com.zenlauncher.zenmode.ZenScoreStore]. */
    score: Int,
    /** Yesterday's saved score in tenths, if there is one, for the insight line. */
    yesterdayScore: Int? = null,
    categories: List<ZenScoreCategory> = emptyList(),
    reclaimedMinutes: Int = 0,
    sessionTotalLabel: String = "",
    sessionLog: List<ZenSessionLogEntry> = emptyList(),
    userName: String? = null,
    photoUrl: String? = null,
    isPro: Boolean = false,
    today: LocalDate = LocalDate.now(),
    onBackClick: () -> Unit,
    /** Tapping the Zen Gold dot jumps straight there, same destination Home's left swipe reaches. */
    onZenGoldClick: () -> Unit = {},
    onUpgradeProClick: () -> Unit = {},
    onDownloadReportClick: () -> Unit = {}
) {
    val mood = rememberTodayMood()
    var footerHeight by remember { mutableStateOf(0.dp) }
    // "Share Zen Score" opens the shareable card rather than a bare text share — the same
    // sheet Home's Streaks and Gold stats open (HomeShareOverlays.kt).
    var showShareOverlay by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Home sits to the right of this page, so swiping left goes back to it.
            .pageSwipe(onSwipeLeft = onBackClick)
    ) {
        // "Share Zen Score" opens over a blurred page rather than a blacked-out one
        // (Figma node 252:3646, "streaks OVerlay") — same mechanism the app-actions /
        // apps-picker frosted overlays use. The overlay itself sits outside this Box
        // so it stays sharp.
        Box(modifier = Modifier.fillMaxSize().zenOverlayBlur(showShareOverlay)) {
            MoodBackdrop(mood)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.rdp))

                GreetingHeader(
                    userName = userName,
                    photoUrl = photoUrl,
                    today = today,
                    isPro = isPro,
                    onUpgradeProClick = onUpgradeProClick
                )

                Spacer(modifier = Modifier.height(16.rdp))

                Column(
                    modifier = Modifier
                        .padding(horizontal = ScreenMargin)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.rdp)
                ) {
                    ZenScoreCard(
                        score = score,
                        insight = scoreInsight(score, yesterdayScore),
                        categories = categories
                    )

                    SessionLogCard(
                        reclaimedMinutes = reclaimedMinutes,
                        sessionTotalLabel = sessionTotalLabel,
                        sessionLog = sessionLog,
                        isPro = isPro,
                        onUpgradeProClick = onUpgradeProClick
                    )
                }

                // Room for the sticky footer, so the session log can scroll clear of it.
                Spacer(modifier = Modifier.height(footerHeight + 8.rdp))
            }

            // Download / Share stay on screen however far the page scrolls.
            PinnedPageFooter(
                current = HomePage.ZEN_SCORE,
                fadeTo = moodWashColors(mood).last(),
                onHeightChanged = { footerHeight = it },
                modifier = Modifier.align(Alignment.BottomCenter),
                onPageClick = { page ->
                    when (page) {
                        HomePage.HOME -> onBackClick()
                        HomePage.ZEN_GOLD -> onZenGoldClick()
                        HomePage.ZEN_SCORE -> Unit
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = ScreenMargin)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.rdp)
                ) {
                    DownloadReportButton(isPro = isPro, onClick = onDownloadReportClick)
                    ShareScoreButton(onClick = { showShareOverlay = true })
                }
            }
        }

        AnimatedVisibility(
            visible = showShareOverlay,
            modifier = Modifier.systemBarsPadding(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ZenScoreOverlay(
                zenScore = score,
                reclaimedMinutes = reclaimedMinutes,
                today = today,
                onDismiss = { showShareOverlay = false }
            )
        }
    }
}

// ── Greeting header ───────────────────────────────────────────────
// Avatar, "Hey, <first name>", today's date, and Upgrade Pro. The avatar is the way into
// Settings from this page, standing in for the ☰ the other pages carry.

@Composable
private fun GreetingHeader(
    userName: String?,
    photoUrl: String?,
    today: LocalDate,
    isPro: Boolean,
    onUpgradeProClick: () -> Unit
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    val firstName = userName?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(
            name = firstName,
            photoUrl = photoUrl,
            modifier = Modifier
                .size(46.rdp)
                .clip(CircleShape)
                .pressScale(onClick = { openSettings(context) }, onClickLabel = "Open settings")
        )

        Spacer(modifier = Modifier.width(10.rdp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (firstName != null) "Hey, $firstName" else "Hey there",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.rsp,
                letterSpacing = (-0.6).sp,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = greetingDate(today),
                fontFamily = Geist,
                fontSize = 13.rsp,
                letterSpacing = (-0.13).sp,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.rdp))

        if (isPro) {
            ProBadge()
        } else {
            Box(
                modifier = Modifier
                    .height(38.rdp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colorResource(R.color.zen_700))
                    .pressScale(onClick = onUpgradeProClick, onClickLabel = "Upgrade to PRO")
                    .padding(horizontal = 18.rdp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Upgrade Pro",
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.rsp,
                    letterSpacing = (-0.3).sp,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

/** "Tue · 12 may" — as the design sets it. */
private fun greetingDate(date: LocalDate): String {
    val day = date.dayOfWeek.getDisplayName(TextStyleJava.SHORT, Locale.getDefault())
    val month = date.month.getDisplayName(TextStyleJava.SHORT, Locale.getDefault()).lowercase(Locale.getDefault())
    return "$day · ${date.dayOfMonth} $month"
}

/** The Google profile photo when there is one; otherwise the first initial on brand green. */
@Composable
private fun ProfileAvatar(name: String?, photoUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(colorResource(R.color.zen_700))
            .semantics { contentDescription = "Profile and settings" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name?.firstOrNull()?.uppercase() ?: "Z",
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 22.rsp,
            color = Color.White
        )
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ── Zen Score card ─────────────────────────────────────────────────

private fun scoreInsight(score: Int, yesterday: Int?): String = when {
    yesterday == null -> "Your score updates live through the day. Check back tomorrow to compare."
    score > yesterday -> "Up ${ZenScore.formatDelta(score - yesterday)} from yesterday. Keep it calm."
    score < yesterday -> "Down ${ZenScore.formatDelta(score - yesterday)} from yesterday. There's still time today."
    else -> "Level with yesterday."
}

@Composable
private fun ZenScoreCard(
    score: Int,
    insight: String,
    categories: List<ZenScoreCategory>
) {
    val colors = ZenTheme.colors
    val radius = 16.rdp

    // Same gradient tokens as HomeHeader's "Zen Score" number (ZenScoreGradient in
    // HomeScreen.kt) — that val is private to its file, so rebuilt here from the
    // same underlying colorResource tokens rather than duplicated as raw hex.
    val scoreGradient = Brush.linearGradient(
        listOf(
            colorResource(R.color.score_grad_start),
            colorResource(R.color.score_grad_mid),
            colorResource(R.color.score_orange)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(colors.bgSecondary)
            .taperedBorder(colorResource(R.color.zen_700), radius, top = 1.rdp)
    ) {
        ZenScoreCardPattern(modifier = Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.rdp, vertical = 18.rdp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Zen Score",
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.rsp,
                    letterSpacing = (-0.16).sp,
                    color = ZenTheme.colors.textBrandStrong,
                    modifier = Modifier.weight(1f)
                )
                PeriodToggle()
            }

            Spacer(modifier = Modifier.height(14.rdp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = ZenScore.format(score),
                        fontFamily = DepartureMono,
                        fontSize = 31.5.rsp,
                        letterSpacing = (-2.5).sp,
                        style = TextStyle(brush = scoreGradient)
                    )
                    Text(
                        text = "/${ZenScore.MAX_DISPLAY}",
                        fontFamily = DepartureMono,
                        fontSize = 17.rsp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 3.rdp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                ZenScoreDial(score = score, maxScore = ZenScore.MAX_TENTHS)
            }

            Spacer(modifier = Modifier.height(12.rdp))

            Text(
                text = insight,
                fontFamily = Geist,
                fontWeight = FontWeight.Normal,
                fontSize = 11.rsp,
                letterSpacing = (-0.11).sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.rdp))

            CategoryLegendGrid(categories = categories)
        }
    }
}

/**
 * Faint brand dot-grid plus a soft glow behind where the ring sits — gives the card some
 * texture instead of a flat fill, without competing with the score number/ring for attention.
 */
@Composable
private fun ZenScoreCardPattern(modifier: Modifier = Modifier) {
    val dot = colorResource(R.color.zen_700).copy(alpha = 0.07f)
    val glow = colorResource(R.color.zen_300).copy(alpha = 0.16f)

    Canvas(modifier = modifier) {
        val glowCenter = Offset(size.width * 0.86f, size.height * 0.3f)
        val glowRadius = size.minDimension * 0.7f
        drawCircle(
            brush = Brush.radialGradient(listOf(glow, Color.Transparent), center = glowCenter, radius = glowRadius),
            radius = glowRadius,
            center = glowCenter
        )

        val spacing = 14.dp.toPx()
        val dotRadius = 1.dp.toPx()
        var y = spacing / 2f
        while (y < size.height) {
            var x = spacing / 2f
            while (x < size.width) {
                drawCircle(color = dot, radius = dotRadius, center = Offset(x, y))
                x += spacing
            }
            y += spacing
        }
    }
}

@Composable
private fun PeriodToggle() {
    val colors = ZenTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .border(1.dp, colorResource(R.color.toggle_border_green), RoundedCornerShape(percent = 50))
            .padding(2.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(colors.surfaceElevated)
                .border(0.5.dp, colorResource(R.color.toggle_pill_border), RoundedCornerShape(percent = 50))
                .padding(horizontal = 8.rdp, vertical = 3.rdp)
        ) {
            Text("Daily", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 9.rsp, color = colors.textPrimary)
        }
        Row(
            modifier = Modifier.padding(horizontal = 8.rdp, vertical = 3.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Weekly", fontFamily = Geist, fontSize = 9.rsp, color = colorResource(R.color.toggle_inactive_text))
            Spacer(modifier = Modifier.width(4.rdp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colorResource(R.color.gold_delta_text))
                    .padding(horizontal = 4.rdp, vertical = 1.rdp)
            ) {
                Text("PRO", fontFamily = Geist, fontSize = 5.6.rsp, color = Color.White)
            }
        }
    }
}

@Composable
private fun CategoryLegendGrid(categories: List<ZenScoreCategory>) {
    val colors = ZenTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.rdp)) {
        categories.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { category ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.rdp)
                                .clip(RoundedCornerShape(2.rdp))
                                .background(colorResource(category.colorRes))
                        )
                        Spacer(modifier = Modifier.width(6.rdp))
                        Text(
                            text = "${category.label} - ${category.percent}%",
                            fontFamily = Geist,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.rsp,
                            letterSpacing = (-0.11).sp,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

// ── 3D score ring ─────────────────────────────────────────────────
// A thick green ring seen from above at an angle, like the design's torus. It is drawn as a
// real extruded solid — a stack of darker walls under a lit top face — and it turns: today's
// score is a bright band that travels round the ring while the ring gently rocks on its tilt.
// Lighting is fixed (front brighter than back) so the motion reads as rotation, not blinking.

@Composable
private fun ZenScoreDial(score: Int, maxScore: Int, modifier: Modifier = Modifier) {
    val progress = (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
    val fill = remember { Animatable(0f) }
    LaunchedEffect(progress) { fill.animateTo(progress, tween(1_100, easing = FastOutSlowInEasing)) }

    val still = rememberReduceMotion() || LocalInspectionMode.current
    val motion = rememberInfiniteTransition(label = "score-ring")
    val spin by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "score-ring-spin"
    )
    val rock by motion.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(tween(3_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "score-ring-rock"
    )

    val lit = colorResource(R.color.zen_300)
    val litDeep = colorResource(R.color.zen_500)
    val wall = colorResource(R.color.zen_900)
    val track = colorResource(R.color.zen_700)

    Canvas(
        modifier = modifier
            .size(width = 118.rdp, height = 78.rdp)
            .semantics { contentDescription = "Zen Score ring, ${(progress * 100).toInt()} percent" }
    ) {
        val band = size.width * 0.15f
        val depth = size.height * 0.11f
        val tilt = if (still) 0.46f else rock
        val rx = size.width / 2f - band / 2f
        val ry = rx * tilt
        val cx = size.width / 2f
        val cy = size.height / 2f - depth / 2f
        val start = if (still) -90f else spin - 90f
        val sweep = 360f * fill.value

        // Contact shadow.
        drawOval(
            brush = Brush.radialGradient(
                listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(cx, cy + depth + ry * 0.35f),
                radius = rx * 1.05f
            ),
            topLeft = Offset(cx - rx * 1.05f, cy + depth + ry * 0.35f - ry * 0.7f),
            size = Size(rx * 2.1f, ry * 1.4f)
        )

        // Walls: the same ring stacked downwards, darkening with depth.
        val steps = depth.toInt().coerceAtLeast(1)
        for (i in steps downTo 1) {
            val shade = 0.55f + 0.35f * (1f - i / steps.toFloat())
            drawOval(
                color = wall.copy(alpha = shade),
                topLeft = Offset(cx - rx, cy - ry + i),
                size = Size(rx * 2, ry * 2),
                style = Stroke(width = band)
            )
        }

        // Top face in short segments, so each can take its own light and colour.
        val segment = 4f
        var a = 0f
        while (a < 360f) {
            val mid = Math.toRadians((a + segment / 2f).toDouble())
            // Front of the ring (bottom of the ellipse) faces the viewer and catches the light.
            val light = 0.72f + 0.28f * kotlin.math.sin(mid).toFloat()
            val inBand = ((a - start) % 360f + 360f) % 360f < sweep
            val base = if (inBand) lerp(litDeep, lit, light) else track.copy(alpha = 0.35f + 0.25f * light)
            drawArc(
                color = base,
                startAngle = a,
                sweepAngle = segment + 0.6f,
                useCenter = false,
                topLeft = Offset(cx - rx, cy - ry),
                size = Size(rx * 2, ry * 2),
                style = Stroke(width = band, cap = StrokeCap.Butt)
            )
            a += segment
        }

        // Fixed specular glint on the inner front edge.
        drawArc(
            color = Color.White.copy(alpha = 0.45f),
            startAngle = 110f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = Offset(cx - rx + band * 0.3f, cy - ry + band * 0.3f * tilt),
            size = Size((rx - band * 0.3f) * 2, (ry - band * 0.3f * tilt) * 2),
            style = Stroke(width = band * 0.18f, cap = StrokeCap.Round)
        )
    }
}

// ── Session log card ───────────────────────────────────────────────

/** Rows visible before the free-tier cap kicks in and before the card needs to scroll. */
private const val FREE_SESSION_LOG_LIMIT = 7
private val SessionLogListHeight: Dp @Composable get() = 220.rdp

@Composable
private fun SessionLogCard(
    reclaimedMinutes: Int,
    sessionTotalLabel: String,
    sessionLog: List<ZenSessionLogEntry>,
    isPro: Boolean,
    onUpgradeProClick: () -> Unit
) {
    val colors = ZenTheme.colors
    val radius = 20.rdp
    // Free tier only ever sees today's most recent 7 sessions (list is already newest-first);
    // Pro sees everything. Either way the card itself has a fixed height -- it scrolls
    // internally instead of growing with however many sessions the day produced.
    val visibleLog = if (isPro) sessionLog else sessionLog.take(FREE_SESSION_LOG_LIMIT)
    val hiddenCount = (sessionLog.size - FREE_SESSION_LOG_LIMIT).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = 3f
                cameraDistance = 24f * density
            }
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.verticalGradient(
                    listOf(ZenTheme.colors.bgMoodHappy, ZenTheme.colors.surfaceSunk)
                )
            )
            .taperedBorder(colorResource(R.color.zen_700), radius, top = 1.5.dp)
            .padding(horizontal = 18.rdp, vertical = 20.rdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TODAY'S SESSION LOG",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 12.rsp,
            letterSpacing = 1.2.sp,
            color = ZenTheme.colors.textBrandStrong,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.rdp))

        ReclaimedMinutesDial(minutes = reclaimedMinutes)

        Spacer(modifier = Modifier.height(16.rdp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.rdp))
                .background(colors.surfaceElevated.copy(alpha = 0.6f))
                .border(1.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(10.rdp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZenTheme.colors.borderSubtle)
                    .padding(horizontal = 10.rdp, vertical = 8.rdp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SESSION LOG",
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.rsp,
                    letterSpacing = 1.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = sessionTotalLabel,
                    fontFamily = DepartureMono,
                    fontSize = 11.rsp,
                    color = colors.textSecondary
                )
            }

            // Fixed height so the card stops growing with however many sessions today has
            // -- it scrolls internally instead (newest session first, so nothing extra to
            // scroll past to see it).
            LazyColumn(modifier = Modifier.height(SessionLogListHeight)) {
                items(visibleLog) { entry -> SessionLogRow(entry) }
            }

            if (!isPro && hiddenCount > 0) {
                UpgradeForMoreSessionsRow(hiddenCount = hiddenCount, onClick = onUpgradeProClick)
            }
        }
    }
}

@Composable
private fun UpgradeForMoreSessionsRow(hiddenCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZenTheme.colors.borderSubtle)
            .pressScale(onClick = onClick, onClickLabel = "Upgrade to Pro to see all sessions")
            .padding(horizontal = 10.rdp, vertical = 8.rdp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "+$hiddenCount more session${if (hiddenCount == 1) "" else "s"} today",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 10.rsp,
            color = ZenTheme.colors.textSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Upgrade to see all",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.rsp,
                letterSpacing = (-0.1).sp,
                color = colorResource(R.color.zen_700)
            )
            Spacer(modifier = Modifier.width(6.rdp))
            ProBadge()
        }
    }
}

@Composable
private fun SessionLogRow(entry: ZenSessionLogEntry) {
    val colors = ZenTheme.colors
    val (statusLabel, statusColor) = when (entry.type) {
        SessionEventType.INTENTIONAL -> "INTENTIONAL" to colorResource(R.color.gold_delta_text)
        SessionEventType.ENTERTAINING -> "ENTERTAINING" to colorResource(R.color.score_status_red)
        SessionEventType.DISRUPTED -> "DISRUPTED" to colorResource(R.color.amber_500)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.rdp, vertical = 2.rdp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.duration,
                fontFamily = DepartureMono,
                fontSize = 10.rsp,
                lineHeight = 10.rsp,
                letterSpacing = 1.sp,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.width(10.rdp))
            Text(
                text = entry.appName.uppercase(),
                fontFamily = DepartureMono,
                fontSize = 10.rsp,
                lineHeight = 10.rsp,
                color = colors.textPrimary
            )
        }
        Text(
            text = statusLabel,
            fontFamily = DepartureMono,
            fontSize = 9.rsp,
            lineHeight = 9.rsp,
            color = statusColor
        )
    }
}

// ── Reclaimed-minutes dial ─────────────────────────────────────────
// The session log's centrepiece (design: a ticked bezel round a thick two-tone ring round a
// glowing green disc). On arrival the ring fills and the bezel's minute ticks light up in step
// with it; afterwards a soft highlight keeps sweeping the lit ticks and the disc breathes, so
// the dial feels live without pulling focus. Counts the minutes up as it fills.

@Composable
private fun ReclaimedMinutesDial(minutes: Int, modifier: Modifier = Modifier) {
    val progress = (minutes / 2000f).coerceIn(0f, 1f)
    val fill = remember { Animatable(0f) }
    LaunchedEffect(progress) { fill.animateTo(progress, tween(1_300, easing = FastOutSlowInEasing)) }

    val still = rememberReduceMotion() || LocalInspectionMode.current
    val motion = rememberInfiniteTransition(label = "reclaimed")
    val scan by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_200, easing = LinearEasing)),
        label = "reclaimed-scan"
    )
    val breathe by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "reclaimed-breathe"
    )

    val tickLit = colorResource(R.color.zen_700)
    val tickDim = colorResource(R.color.stone_300).copy(alpha = 0.45f)
    val ringTrack = colorResource(R.color.stone_300).copy(alpha = 0.55f)
    val ringFill = colorResource(R.color.zen_700)
    val ringFillDeep = colorResource(R.color.zen_900)
    val discCore = colorResource(R.color.zen_200)
    val discEdge = colorResource(R.color.zen_100)
    val ink = colorResource(R.color.ink_base)
    val shown = (minutes * fill.value / progress.coerceAtLeast(0.0001f)).toInt().coerceAtMost(minutes)

    Box(
        modifier = modifier
            .size(168.rdp)
            .semantics(mergeDescendants = true) { contentDescription = "Reclaimed today: $minutes minutes" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val ticks = 60
            val litTicks = ticks * fill.value

            // Bezel: 60 minute ticks, every fifth one longer.
            for (i in 0 until ticks) {
                val major = i % 5 == 0
                val t = i / ticks.toFloat()
                val on = i < litTicks
                // The sweep highlight trails off behind a moving head.
                val trail = (((scan - t) % 1f) + 1f) % 1f
                val glow = if (on && !still) (1f - trail / 0.25f).coerceIn(0f, 1f) else 0f
                val color = if (on) lerp(tickLit, discEdge, glow * 0.8f) else tickDim
                rotate(degrees = t * 360f, pivot = center) {
                    drawLine(
                        color = color,
                        start = Offset(center.x, center.y - r * 0.98f),
                        end = Offset(center.x, center.y - r * (if (major) 0.86f else 0.9f)),
                        strokeWidth = r * (if (major) 0.03f else 0.018f),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Two-tone ring with a soft drop shadow underneath.
            val ringR = r * 0.7f
            val ringW = r * 0.16f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent),
                    center = center.copy(y = center.y + r * 0.05f),
                    radius = ringR + ringW
                ),
                radius = ringR + ringW,
                center = center.copy(y = center.y + r * 0.05f)
            )
            val arcTopLeft = Offset(center.x - ringR, center.y - ringR)
            val arcSize = Size(ringR * 2, ringR * 2)
            drawArc(ringTrack, -90f, 360f, false, arcTopLeft, arcSize, style = Stroke(width = ringW))
            if (fill.value > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(ringFillDeep, ringFill, ringFillDeep), center),
                    startAngle = -90f,
                    sweepAngle = 360f * fill.value,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = ringW, cap = StrokeCap.Round)
                )
            }

            // Glowing disc: light core, inner shadow at the rim, breathing halo.
            val discR = ringR - ringW / 2f - r * 0.02f
            drawCircle(
                color = discEdge.copy(alpha = 0.35f + 0.25f * (if (still) 0.5f else breathe)),
                radius = discR + r * 0.03f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(discCore, discEdge), center, discR),
                radius = discR,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0.78f to Color.Transparent,
                    1f to ringFillDeep.copy(alpha = 0.18f),
                    center = center,
                    radius = discR
                ),
                radius = discR,
                center = center
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Reclaimed Today",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 9.rsp,
                color = ink.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "%,d".format(shown),
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 28.rsp,
                letterSpacing = (-0.84).sp,
                color = ink
            )
            Text(
                text = "Minutes",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 9.rsp,
                color = colorResource(R.color.gold_delta_text)
            )
        }
    }
}

// ── CTA buttons ─────────────────────────────────────────────────────
// Same pill visual language as HomeShareOverlays' ShareOutlineButton /
// ShareSolidButton and ZenGoldScreen's InvestGoldButton / EditPromiseButton.

@Composable
private fun DownloadReportButton(isPro: Boolean, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.rdp)
            .clip(RoundedCornerShape(50))
            .pressScale(onClick = onClick, onClickLabel = "Download report PDF", pressedScale = 0.96f)
            .padding(top = 8.rdp)
    ) {
        Text(
            text = "Download Report PDF",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 19.rsp,
            letterSpacing = (-0.38).sp,
            color = colorResource(R.color.zen_700)
        )
        if (!isPro) {
            // Sits up at the cap height, like a superscript (as in the design).
            Spacer(modifier = Modifier.width(6.rdp))
            ProBadge(modifier = Modifier.offset(y = (-4).rdp))
        }
    }
}

@Composable
private fun ShareScoreButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.rdp)
            .clip(RoundedCornerShape(50))
            .background(colorResource(R.color.zen_950))
            .pressScale(onClick = onClick, onClickLabel = "Share Zen Score", pressedScale = 0.97f)
    ) {
        Text(
            text = "Share Zen Score",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 18.rsp,
            letterSpacing = (-0.36).sp,
            color = Color.White
        )
    }
}

@Composable
private fun ProBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(colorResource(R.color.zen_700))
            .padding(horizontal = 5.rdp, vertical = 2.rdp)
    ) {
        Text(
            text = "PRO",
            fontFamily = Geist,
            fontWeight = FontWeight.Bold,
            fontSize = 8.rsp,
            color = Color.White
        )
    }
}
