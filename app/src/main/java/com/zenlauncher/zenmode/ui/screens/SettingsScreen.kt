package com.zenlauncher.zenmode.ui.screens

import com.zenlauncher.zenmode.ThemePreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import coil.compose.AsyncImage
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.RedditMono
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rsp
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.components.ZenSettingToggleItem
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

// ── Constants ─────────────────────────────────────────────────────

private const val GRAPH_OPACITY = 0.42f
private const val MAX_GRAPH_HOURS = 6f
private const val GRAPH_HOUR_LINES = 4 // 0h, 2h, 4h, 6h

// ── Main Settings Screen ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    weeklyHours: List<Float> = List(7) { 0f },
    profilePhotoUrl: String? = null,
    onBackClick: () -> Unit,
    onChangeDistractingAppsClick: () -> Unit,
    onAccountabilityPartnerClick: () -> Unit,
    onContributeClick: () -> Unit,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    onLogoutClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {}
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    var isDarkMode by remember { mutableStateOf(ThemePreferences.isDarkMode(context)) }
    var showProfileSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bgPrimary)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ────────────────────────────────────────────────
            SettingsHeader(
                onBackClick = onBackClick,
                profilePhotoUrl = profilePhotoUrl,
                onProfileClick = { showProfileSheet = true }
            )

            Spacer(modifier = Modifier.height(24.rdp))

            // ── My Weekly Stats ───────────────────────────────────────
            WeeklyStatsSection(weeklyHours = weeklyHours)

            Spacer(modifier = Modifier.height(28.rdp))

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

            Spacer(modifier = Modifier.height(28.rdp))

            // ── You're the Hero! ─────────────────────────────────────
            HeroSection()

            Spacer(modifier = Modifier.height(24.rdp))

            // ── Rate Us Button ────────────────────────────────────────
            RateUsButton(onClick = onRateClick)

            Spacer(modifier = Modifier.height(16.rdp))

            // ── Share ZenMode ─────────────────────────────────────────
            ShareZenModeRow(onClick = onShareClick)

            Spacer(modifier = Modifier.height(32.rdp))
        }

        // ── Profile Bottom Sheet ──────────────────────────────────
        if (showProfileSheet) {
            ProfileBottomSheet(
                onDismiss = { showProfileSheet = false },
                onLogoutClick = {
                    showProfileSheet = false
                    onLogoutClick()
                },
                onDeleteAccountClick = {
                    showProfileSheet = false
                    onDeleteAccountClick()
                }
            )
        }
    }
}

// ── Header ────────────────────────────────────────────────────────

@Composable
private fun SettingsHeader(
    onBackClick: () -> Unit,
    profilePhotoUrl: String? = null,
    onProfileClick: () -> Unit = {}
) {
    val colors = ZenTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.rdp, vertical = 16.rdp)
    ) {
        // Back button — left
        Image(
            painter = painterResource(R.drawable.button_back),
            contentDescription = "Back",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onBackClick() }
                .height(36.rdp),
            contentScale = ContentScale.Fit
        )

        // App icon — center
        Image(
            painter = painterResource(R.drawable.app_icon),
            contentDescription = "ZenMode",
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.rdp)
        )

        // Profile picture — right
        if (profilePhotoUrl != null) {
            AsyncImage(
                model = profilePhotoUrl,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(36.rdp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() },
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(36.rdp)
                    .clip(CircleShape)
                    .background(colors.textSecondary.copy(alpha = 0.3f))
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.log_out),
                    contentDescription = "Profile",
                    modifier = Modifier.size(18.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

// ── Weekly Stats Section ──────────────────────────────────────────

@Composable
private fun WeeklyStatsSection(weeklyHours: List<Float>) {
    val colors = ZenTheme.colors

    Column(modifier = Modifier.padding(horizontal = 20.rdp)) {
        // Section title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.onboarding_usage_access_permission),
                contentDescription = null,
                modifier = Modifier.size(28.rdp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "My Weekly stats",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 20.rsp,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.rdp))

        // Graph card
        ScreenTimeGraphCard(weeklyHours = weeklyHours)
    }
}

// ── Screen Time Graph Card ────────────────────────────────────────

@Composable
private fun ScreenTimeGraphCard(weeklyHours: List<Float>) {
    val colors = ZenTheme.colors
    val brandColor = colors.textBrand

    // Calculate dynamic max based on data (at least 6h, rounded to multiple of 3 for nice labels)
    val maxOfData = weeklyHours.maxOrNull() ?: 0f
    val dynamicMax = maxOf(6f, (kotlin.math.ceil(maxOfData / 3f) * 3f))

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
            .padding(16.rdp)
    ) {
        // Title
        Text(
            text = "My  Screen time",
            fontFamily = CabinetGrotesque,
            fontWeight = FontWeight.Medium,
            fontSize = 14.rsp,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.rdp))

        // Graph area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.rdp)
        ) {
            // Y-axis labels
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (h in listOf(dynamicMax.toInt(), (dynamicMax * 2 / 3).toInt(), (dynamicMax / 3).toInt(), 0)) {
                    Text(
                        text = "${h}h",
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.rsp,
                        color = colors.textSecondary
                    )
                }
            }

            // Canvas for graph
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.rdp)
            ) {
                val graphWidth = size.width
                val graphHeight = size.height
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                // Dashed horizontal lines at 1/3, 2/3, 3/3 of dynamicMax
                for (i in 1..3) {
                    val y = graphHeight - (graphHeight * (i / 3f))
                    drawLine(
                        color = brandColor.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(graphWidth, y),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }

                // Build line path from data
                val points = weeklyHours.mapIndexed { index, hours ->
                    val x = if (weeklyHours.size <= 1) graphWidth / 2
                    else graphWidth * index / (weeklyHours.size - 1)
                    val y = graphHeight - (graphHeight * (hours / dynamicMax))
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
                .padding(start = 28.rdp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.rsp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.rdp)
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

    Column(modifier = Modifier.padding(horizontal = 20.rdp)) {
        // Section title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.star),
                contentDescription = null,
                modifier = Modifier.size(28.rdp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Personalise, Your Way!",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 20.rsp,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.rdp))

        // Settings card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgSecondary)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Dark mode toggle
            ZenSettingToggleItem(
                text = "Dark mode",
                checked = isDarkMode,
                onCheckedChange = onDarkModeChange,
                modifier = Modifier.padding(vertical = 6.dp)
            )

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
        fontSize = 16.rsp,
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

    val missionLineHeight = 20.rsp
    Column(
        modifier = Modifier.padding(horizontal = 20.rdp)
    ) {
        // Title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.king),
                contentDescription = null,
                modifier = Modifier.size(28.rdp)
            )
            Text(
                text = "You're the Hero!",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 20.rsp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "⭐",
                fontSize = 14.rsp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mission text
        Text(
            text = "You can be anywhere, but thanks for joining our journey to help people with mindful digital time.",
            fontFamily = CabinetGrotesque,
            fontWeight = FontWeight.Normal,
            fontSize = 14.rsp,
            color = colors.textSecondary,
            lineHeight = missionLineHeight
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Copyright
        Text(
            text = "\u00A9 2026 Zenmode. All rights reserved.",
            fontFamily = CabinetGrotesque,
            fontWeight = FontWeight.Normal,
            fontSize = 12.rsp,
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
                fontSize = 14.rsp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Image(
                painter = painterResource(R.drawable.heart_byhand),
                contentDescription = null,
                modifier = Modifier.size(20.rdp)
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
            .padding(horizontal = 20.rdp)
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
            fontSize = 18.rsp,
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
            fontSize = 18.rsp,
            color = ZenTheme.colors.textBrand
        )
        Spacer(modifier = Modifier.width(8.dp))
        Image(
            painter = painterResource(R.drawable.share_zenmode),
            contentDescription = "Share",
            modifier = Modifier.size(20.rdp),
            contentScale = ContentScale.Fit
        )
    }
}

// ── Profile Bottom Sheet ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileBottomSheet(
    onDismiss: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    val colors = ZenTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgSecondary,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textSecondary.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.rdp)
                .padding(bottom = 32.rdp)
        ) {
            if (!showDeleteConfirmation) {
                // Title row with app icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Main title
                        Text(
                            text = buildAnnotatedString {
                                append("You\u2019re about to break my ")
                                withStyle(SpanStyle(color = colors.textBrand)) {
                                    append("\uD83D\uDC9A")
                                }
                            },
                            fontFamily = CabinetGrotesque,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.rsp,
                            color = colors.textPrimary,
                            lineHeight = 28.rsp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle
                        Text(
                            text = "Hey though I\u2019m always here waiting to help you!",
                            fontFamily = CabinetGrotesque,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.rsp,
                            color = colors.textSecondary,
                            lineHeight = 20.rsp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // App icon — top right
                    Image(
                        painter = painterResource(R.drawable.app_icon),
                        contentDescription = "ZenMode",
                        modifier = Modifier.size(48.rdp)
                    )
                }

                Spacer(modifier = Modifier.height(28.rdp))

                // Delete account row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteConfirmation = true }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.delete_account),
                        contentDescription = "Delete account",
                        modifier = Modifier.size(20.rdp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Delete account",
                        fontFamily = CabinetGrotesque,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.rsp,
                        color = Color(0xFFE53935)
                    )
                }

                // Divider
                HorizontalDivider(
                    color = colors.textSecondary.copy(alpha = 0.15f),
                    thickness = 1.dp
                )

                // Log out row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLogoutClick() }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.log_out),
                        contentDescription = "Log out",
                        modifier = Modifier.size(20.rdp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Log out",
                        fontFamily = CabinetGrotesque,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.rsp,
                        color = colors.textPrimary
                    )
                }
            } else {
                // Delete confirmation view
                Text(
                    text = "Are you sure?",
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.rsp,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This will permanently delete your account and all associated data. This action cannot be undone.",
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.rsp,
                    color = colors.textSecondary,
                    lineHeight = 20.rsp
                )

                Spacer(modifier = Modifier.height(24.rdp))

                // Delete button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE53935))
                        .clickable { onDeleteAccountClick() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delete my account",
                        fontFamily = CabinetGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.rsp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cancel button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.bgPrimary)
                        .clickable { showDeleteConfirmation = false }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = CabinetGrotesque,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.rsp,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}
