package com.zenlauncher.zenmode.ui.screens

import com.zenlauncher.zenmode.ThemePreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.RedditMono
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

// ── Constants ─────────────────────────────────────────────────────

private const val GRAPH_OPACITY = 0.42f
private const val MAX_GRAPH_HOURS = 6f
private const val GRAPH_HOUR_LINES = 4 // 0h, 2h, 4h, 6h

// Dummy weekly data (hours) — will be replaced with real logic later
private val DUMMY_WEEKLY_HOURS = listOf(2.5f, 3.0f, 4.2f, 3.8f, 4.5f, 5.2f, 5.8f)

// ── Main Settings Screen ──────────────────────────────────────────

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onChangeDistractingAppsClick: () -> Unit,
    onAccountabilityPartnerClick: () -> Unit,
    onContributeClick: () -> Unit,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    var isDarkMode by remember { mutableStateOf(ThemePreferences.isDarkMode(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────
        SettingsHeader(onBackClick = onBackClick)

        Spacer(modifier = Modifier.height(24.dp))

        // ── My Weekly Stats ───────────────────────────────────────
        WeeklyStatsSection()

        Spacer(modifier = Modifier.height(28.dp))

        // ── Personalise, Your Way! ────────────────────────────────
        PersonaliseSection(
            isDarkMode = isDarkMode,
            onDarkModeChange = { enabled ->
                isDarkMode = enabled
                ThemePreferences.setDarkMode(context, enabled)
            },
            onChangeDistractingAppsClick = onChangeDistractingAppsClick,
            onAccountabilityPartnerClick = onAccountabilityPartnerClick,
            onContributeClick = onContributeClick
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── You're the Hero! ─────────────────────────────────────
        HeroSection()

        Spacer(modifier = Modifier.height(24.dp))

        // ── Rate Us Button ────────────────────────────────────────
        RateUsButton(onClick = onRateClick)

        Spacer(modifier = Modifier.height(16.dp))

        // ── Share ZenMode ─────────────────────────────────────────
        ShareZenModeRow(onClick = onShareClick)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Header ────────────────────────────────────────────────────────

@Composable
private fun SettingsHeader(onBackClick: () -> Unit) {
    val colors = ZenTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Back button — left
        Image(
            painter = painterResource(R.drawable.button_back),
            contentDescription = "Back",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onBackClick() }
                .height(36.dp),
            contentScale = ContentScale.Fit
        )

        // App icon — center
        Image(
            painter = painterResource(R.drawable.app_icon),
            contentDescription = "ZenMode",
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp)
        )
    }
}

// ── Weekly Stats Section ──────────────────────────────────────────

@Composable
private fun WeeklyStatsSection() {
    val colors = ZenTheme.colors

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // Section title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.onboarding_usage_access_permission),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "My Weekly stats",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Graph card
        ScreenTimeGraphCard()
    }
}

// ── Screen Time Graph Card ────────────────────────────────────────

@Composable
private fun ScreenTimeGraphCard() {
    val colors = ZenTheme.colors
    val brandColor = colors.textBrand

    // Compute last 7 day labels from today
    val dayLabels = remember {
        val today = LocalDate.now()
        (6 downTo 0).map { daysAgo ->
            today.minusDays(daysAgo.toLong())
                .dayOfWeek
                .getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgSecondary)
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "My  Screen time",
            fontFamily = CabinetGrotesque,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Graph area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            // Y-axis labels
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (h in listOf(6, 4, 2, 0)) {
                    Text(
                        text = "${h}h",
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                }
            }

            // Canvas for graph
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.dp)
            ) {
                val graphWidth = size.width
                val graphHeight = size.height
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                // Dashed horizontal lines at 2h, 4h, 6h
                for (i in 1..3) {
                    val y = graphHeight - (graphHeight * (i * 2f / MAX_GRAPH_HOURS))
                    drawLine(
                        color = brandColor.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(graphWidth, y),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }

                // Build line path from data
                val points = DUMMY_WEEKLY_HOURS.mapIndexed { index, hours ->
                    val x = if (DUMMY_WEEKLY_HOURS.size <= 1) graphWidth / 2
                    else graphWidth * index / (DUMMY_WEEKLY_HOURS.size - 1)
                    val y = graphHeight - (graphHeight * (hours / MAX_GRAPH_HOURS))
                    Offset(x, y)
                }

                // Filled area under the curve
                if (points.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(points.first().x, graphHeight)
                        lineTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cx1 = (prev.x + curr.x) / 2
                            cubicTo(cx1, prev.y, cx1, curr.y, curr.x, curr.y)
                        }
                        lineTo(points.last().x, graphHeight)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                brandColor.copy(alpha = GRAPH_OPACITY),
                                brandColor.copy(alpha = 0.05f)
                            ),
                            startY = 0f,
                            endY = graphHeight
                        ),
                        style = Fill
                    )

                    // Line stroke
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cx1 = (prev.x + curr.x) / 2
                            cubicTo(cx1, prev.y, cx1, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = brandColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-axis day labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}

// ── Personalise Section ───────────────────────────────────────────

@Composable
private fun PersonaliseSection(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onChangeDistractingAppsClick: () -> Unit,
    onAccountabilityPartnerClick: () -> Unit,
    onContributeClick: () -> Unit
) {
    val colors = ZenTheme.colors

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // Section title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.star),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Personalise, Your Way!",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Settings card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgSecondary)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Dark mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dark  mode",
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = onDarkModeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.textBrand,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = colors.textSecondary
                    )
                )
            }

            // Change distracting app list
            SettingsClickableItem(
                text = "Change distracting app list",
                color = colors.textBrand,
                onClick = onChangeDistractingAppsClick
            )

            // Accountability partner settings
            SettingsClickableItem(
                text = "Accountability partner settings",
                color = colors.textBrand,
                onClick = onAccountabilityPartnerClick
            )

            // Contribute zenmode (via GitHub)
            SettingsClickableItem(
                text = "Contribute zenmode (via GitHub)",
                color = colors.textBrand,
                onClick = onContributeClick
            )
        }
    }
}

@Composable
private fun SettingsClickableItem(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontFamily = CabinetGrotesque,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    )
}

// ── Hero Section ──────────────────────────────────────────────────

@Composable
private fun HeroSection() {
    val colors = ZenTheme.colors

    Column(
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        // Title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.king),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "You're the Hero!",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "⭐",
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mission text
        Text(
            text = "You can be anywhere, but thanks for joining our journey to help people with mindful digital time.",
            fontFamily = CabinetGrotesque,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = colors.textSecondary,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Copyright
        Text(
            text = "\u00A9 2026 Zenmode. All rights reserved.",
            fontFamily = CabinetGrotesque,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Hashtags row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.textBrand, fontWeight = FontWeight.Bold)) {
                        append("#Zenmode ")
                    }
                    withStyle(SpanStyle(color = colors.textBrand, fontWeight = FontWeight.Bold)) {
                        append("#IMZ ")
                    }
                    withStyle(SpanStyle(color = colors.textBrand, fontWeight = FontWeight.Bold)) {
                        append("#InMyZone")
                    }
                },
                fontFamily = CabinetGrotesque,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Image(
                painter = painterResource(R.drawable.heart_byhand),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Rate Us Button ────────────────────────────────────────────────

@Composable
private fun RateUsButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ZenTheme.colors.actionPrimary)
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Rate us on play store",
            fontFamily = CabinetGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = ZenTheme.colors.actionPrimaryText
        )
    }
}

// ── Share ZenMode Row ─────────────────────────────────────────────

@Composable
private fun ShareZenModeRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Share zenmode",
            fontFamily = CabinetGrotesque,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            color = ZenTheme.colors.textBrand
        )
        Spacer(modifier = Modifier.width(8.dp))
        Image(
            painter = painterResource(R.drawable.share_zenmode),
            contentDescription = "Share",
            modifier = Modifier.size(20.dp),
            contentScale = ContentScale.Fit
        )
    }
}
