package com.zenlauncher.zenmode.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.minDimension
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.dropShadow
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// ── ZM_OS v3: Zen Score (Home tap target) ────────────────────────────
// Figma node 2026:2035. Reached by tapping the "Zen Score" widget in
// HomeHeader (top-left of Home) — see HomeScreen.kt's onZenScoreClick and
// MainActivity's ZenScoreActivity launch. Mirrors ZenGoldScreen's chrome
// (back arrow / centered title / hamburger) since both are secondary
// full-screen destinations off Home, not reached by the same swipe gesture.
//
// Category breakdown and session log both need real per-app usage
// categorization that doesn't exist yet (see zenmode_core_private/docs/plans).
// Wired as AppConstants.PLACEHOLDER_* for now, matching ZenGoldScreen.

private val ScreenMargin: Dp @Composable get() = 30.rdp

data class ZenScoreCategory(val label: String, val percent: Int, val colorRes: Int)

enum class SessionEventType { INTENTIONAL, ENTERTAINING, DISRUPTED }

data class ZenSessionLogEntry(val duration: String, val appName: String, val type: SessionEventType)

// Category breakdown percentages and the session log rows both need real per-app
// usage categorization (see file header) — these are illustrative placeholders,
// not derived from AppConstants since they're structured rather than scalar.
private fun defaultCategories() = listOf(
    ZenScoreCategory("Productivity", 45, R.color.zen_900),
    ZenScoreCategory("Entertainment", 10, R.color.score_status_red),
    ZenScoreCategory("Messaging", 30, R.color.score_category_messaging),
    ZenScoreCategory("Everything else", 15, R.color.zen_300)
)

private fun defaultSessionLog() = listOf(
    ZenSessionLogEntry("00:47:00", "Netflix", SessionEventType.ENTERTAINING),
    ZenSessionLogEntry("00:32:00", "Notion", SessionEventType.INTENTIONAL),
    ZenSessionLogEntry("00:14:00", "Twitter", SessionEventType.DISRUPTED),
    ZenSessionLogEntry("00:28:00", "Netflix", SessionEventType.ENTERTAINING),
    ZenSessionLogEntry("00:21:00", "Notion", SessionEventType.INTENTIONAL)
)

@Composable
fun ZenScoreScreen(
    score: Int = AppConstants.PLACEHOLDER_ZEN_SCORE,
    scoreMax: Int = 100,
    insight: String = AppConstants.PLACEHOLDER_SCORE_INSIGHT,
    categories: List<ZenScoreCategory> = remember { defaultCategories() },
    reclaimedMinutes: Int = AppConstants.PLACEHOLDER_RECLAIMED_MINUTES,
    sessionTotalLabel: String = AppConstants.PLACEHOLDER_SESSION_LOG_TOTAL,
    sessionLog: List<ZenSessionLogEntry> = remember { defaultSessionLog() },
    isPro: Boolean = false,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    onDownloadReportClick: () -> Unit = {},
    onShareScoreClick: () -> Unit = {}
) {
    val colors = ZenTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ZenScoreHeader(onBackClick = onBackClick, onMenuClick = onMenuClick)

            Spacer(modifier = Modifier.height(20.rdp))

            Column(
                modifier = Modifier
                    .padding(horizontal = ScreenMargin)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.rdp)
            ) {
                ZenScoreCard(
                    score = score,
                    scoreMax = scoreMax,
                    insight = insight,
                    categories = categories
                )

                SessionLogCard(
                    reclaimedMinutes = reclaimedMinutes,
                    sessionTotalLabel = sessionTotalLabel,
                    sessionLog = sessionLog
                )
            }

            Spacer(modifier = Modifier.height(24.rdp))

            Column(
                modifier = Modifier
                    .padding(horizontal = ScreenMargin)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.rdp)
            ) {
                DownloadReportButton(isPro = isPro, onClick = onDownloadReportClick)
                ShareScoreButton(onClick = onShareScoreClick)
            }

            Spacer(modifier = Modifier.height(24.rdp))
        }
    }
}

// ── Header ────────────────────────────────────────────────────────
// Mirrors ZenGoldHeader exactly — same chrome for every secondary
// full-screen destination off Home.

@Composable
private fun ZenScoreHeader(onBackClick: () -> Unit, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .padding(top = 12.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = "Back",
            modifier = Modifier
                .size(28.rdp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onBackClick
                ),
            colorFilter = ColorFilter.tint(colorResource(R.color.zen_900))
        )

        Text(
            text = "ZEN SCORE",
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 24.rsp,
            letterSpacing = (-0.48).sp,
            color = colorResource(R.color.zen_900),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Image(
            painter = painterResource(R.drawable.ic_hamburger_menu),
            contentDescription = "Menu",
            modifier = Modifier
                .width(29.rdp)
                .height(17.5.rdp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onMenuClick
                )
        )
    }
}

// ── Zen Score card ─────────────────────────────────────────────────

@Composable
private fun ZenScoreCard(
    score: Int,
    scoreMax: Int,
    insight: String,
    categories: List<ZenScoreCategory>
) {
    val colors = ZenTheme.colors
    val topBorder = colorResource(R.color.zen_700)
    val topBorderWidth = 1.rdp

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.rdp))
            .background(colors.bgSecondary)
            .drawBehind {
                drawLine(
                    color = topBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = topBorderWidth.toPx()
                )
            }
            .padding(16.rdp)
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
                color = colorResource(R.color.zen_900),
                modifier = Modifier.weight(1f)
            )
            PeriodToggle()
        }

        Spacer(modifier = Modifier.height(12.rdp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%02d".format(score),
                    fontFamily = DepartureMono,
                    fontSize = 31.5.rsp,
                    letterSpacing = (-2.5).sp,
                    style = TextStyle(brush = scoreGradient)
                )
                Text(
                    text = "/$scoreMax",
                    fontFamily = DepartureMono,
                    fontSize = 17.rsp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 3.rdp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ZenScoreDial(score = score, maxScore = scoreMax)
        }

        Spacer(modifier = Modifier.height(10.rdp))

        Text(
            text = insight,
            fontFamily = Geist,
            fontWeight = FontWeight.Normal,
            fontSize = 11.rsp,
            letterSpacing = (-0.11).sp,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(14.rdp))

        CategoryLegendGrid(categories = categories)
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

// ── 3D score dial (glossy puck with sweep progress) ────────────────
// Figma's own asset for this (imgGroup2147223863) is a flattened ring+bar
// raster with no vector data to reproduce faithfully, so this is drawn
// procedurally instead — a real gauge (sweep-gradient progress, a top-left
// specular highlight, and a graphicsLayer tilt) rather than a static copy.

@Composable
private fun ZenScoreDial(score: Int, maxScore: Int, modifier: Modifier = Modifier) {
    val progress = (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "zenScoreProgress"
    )
    val trackColor = colorResource(R.color.stone_200)
    val progressStart = colorResource(R.color.zen_700)
    val progressMid = colorResource(R.color.zen_300)

    Box(
        modifier = modifier
            .size(56.rdp)
            .graphicsLayer {
                rotationX = 10f
                cameraDistance = 18f * density
            }
            .dropShadow(color = Color.Black.copy(alpha = 0.18f), blur = 10.rdp, cornerRadius = 28.rdp, offsetY = 4.rdp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.18f
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            // Dome shading — light source from top-left for a puck-like feel.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(size.width * 0.32f, size.height * 0.28f),
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.42f
            )

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(colors = listOf(progressStart, progressMid, progressStart)),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color = Color.White.copy(alpha = 0.5f),
                startAngle = -150f,
                sweepAngle = 45f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth * 0.32f, cap = StrokeCap.Round)
            )
        }
    }
}

// ── Session log card ───────────────────────────────────────────────

@Composable
private fun SessionLogCard(
    reclaimedMinutes: Int,
    sessionTotalLabel: String,
    sessionLog: List<ZenSessionLogEntry>
) {
    val colors = ZenTheme.colors
    val topBorder = colorResource(R.color.zen_700)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = 3f
                cameraDistance = 24f * density
            }
            .clip(RoundedCornerShape(20.rdp))
            .background(
                Brush.verticalGradient(
                    listOf(colorResource(R.color.zen_050), colorResource(R.color.paper_sunk))
                )
            )
            .drawBehind {
                drawLine(
                    color = topBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            .padding(horizontal = 18.rdp, vertical = 20.rdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TODAY'S SESSION LOG",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 12.rsp,
            letterSpacing = 1.2.sp,
            color = colorResource(R.color.zen_900),
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
                    .background(colorResource(R.color.stone_200))
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

            sessionLog.forEach { entry ->
                SessionLogRow(entry)
            }
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
            .padding(horizontal = 10.rdp, vertical = 5.rdp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.duration,
                fontFamily = DepartureMono,
                fontSize = 10.rsp,
                letterSpacing = 1.sp,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.width(10.rdp))
            Text(
                text = entry.appName.uppercase(),
                fontFamily = DepartureMono,
                fontSize = 10.rsp,
                color = colors.textPrimary
            )
        }
        Text(
            text = statusLabel,
            fontFamily = DepartureMono,
            fontSize = 9.rsp,
            color = statusColor
        )
    }
}

// ── 3D reclaimed-minutes dial (speedometer style) ──────────────────
// Same rationale as ZenScoreDial — Figma's Repeat-group/Ellipse cluster here
// (nodes 2026:2051/2054/2059) is raster, not vector, so redrawn as a real
// tick-marked dial with a glass-dome highlight instead of a static copy.

@Composable
private fun ReclaimedMinutesDial(minutes: Int, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    val progress = (minutes / 2000f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "reclaimedProgress"
    )
    val litTickColor = colorResource(R.color.zen_700)
    val dimTickColor = colorResource(R.color.stone_300).copy(alpha = 0.6f)
    val progressStart = colorResource(R.color.zen_700)
    val progressMid = colorResource(R.color.zen_300)

    Box(
        modifier = modifier
            .size(140.rdp)
            .graphicsLayer {
                rotationX = 12f
                cameraDistance = 14f * density
            }
            .dropShadow(color = Color.Black.copy(alpha = 0.16f), blur = 18.rdp, cornerRadius = 70.rdp, offsetY = 8.rdp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tickCount = 48
            val ringRadius = size.minDimension / 2f * 0.96f
            val litTicks = (tickCount * animatedProgress).toInt()

            for (i in 0 until tickCount) {
                val angle = i * (360f / tickCount)
                val lit = i < litTicks
                rotate(degrees = angle, pivot = center) {
                    drawLine(
                        color = if (lit) litTickColor else dimTickColor,
                        start = Offset(center.x, center.y - ringRadius),
                        end = Offset(center.x, center.y - ringRadius + size.minDimension * 0.045f),
                        strokeWidth = size.minDimension * 0.012f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Glass dome — the "3D" heart of the dial.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.35f, size.height * 0.28f),
                    radius = size.minDimension * 0.6f
                ),
                radius = size.minDimension * 0.38f
            )

            drawCircle(color = colors.surfaceElevated, radius = size.minDimension * 0.34f)

            drawArc(
                brush = Brush.sweepGradient(colors = listOf(progressStart, progressMid, progressStart)),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(size.width * 0.145f, size.height * 0.145f),
                size = Size(size.width * 0.71f, size.height * 0.71f),
                style = Stroke(width = size.minDimension * 0.05f, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Reclaimed Today",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 9.rsp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "%,d".format(minutes),
                fontFamily = DepartureMono,
                fontWeight = FontWeight.Normal,
                fontSize = 26.rsp,
                letterSpacing = (-0.26).sp,
                color = colors.textPrimary
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
// Same pill visual language as HomeScreen's MilestoneOutlineButton /
// MilestoneSolidButton and ZenGoldScreen's InvestGoldButton / EditPromiseButton.

@Composable
private fun DownloadReportButton(isPro: Boolean, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(47.rdp)
            .clip(RoundedCornerShape(50))
            .background(colorResource(R.color.milestone_outline_bg))
            .border(1.rdp, colorResource(R.color.zen_700).copy(alpha = 0.26f), RoundedCornerShape(50))
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
            text = "Download Report PDF",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 17.rsp,
            letterSpacing = (-0.35).sp,
            color = colorResource(R.color.gold_delta_text)
        )
        if (!isPro) {
            Spacer(modifier = Modifier.width(8.rdp))
            ProBadge()
        }
    }
}

@Composable
private fun ShareScoreButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(47.rdp)
            .clip(RoundedCornerShape(50))
            .background(colorResource(R.color.zen_700))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        Text(
            text = "Share Zen Score",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 17.rsp,
            letterSpacing = (-0.35).sp,
            color = Color.White
        )
    }
}

@Composable
private fun ProBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(colorResource(R.color.ink_surface))
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
