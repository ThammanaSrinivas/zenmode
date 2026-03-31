package com.zenlauncher.zenmode.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.AppLogic
import com.zenlauncher.zenmode.MoodState
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.RedditMono
import com.zenlauncher.zenmode.ui.theme.Silkscreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.percentageChangeColor
import com.zenlauncher.zenmode.ui.theme.rsp
import com.zenlauncher.zenmode.ui.theme.rdp

// ── Main Resistence Screen ──────────────────────────────────────────

@Composable
fun ResistenceScreen(
    usage: DailyUsage?,
    streaks: Int,
    yesterdayChangePercent: Int?,
    skipsLeft: Int,
    countdownSeconds: Int,
    countdownFinished: Boolean,
    onSettingsClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    val colors = ZenTheme.colors

    val totalMillis = usage?.screenTimeInMillis ?: 0L
    val minutes = (totalMillis / 1000) / 60
    val hours = minutes / 60
    val mins = minutes % 60
    val moodState = AppLogic.getMoodState(minutes)
    val mindfulnessProgress = AppLogic.getMindfulnessPercentage(minutes)

    val moodColor = when (moodState) {
        MoodState.HAPPY -> Color(0xFF00C700)
        MoodState.NEUTRAL -> Color(0xFFEBDE27)
        MoodState.ANNOYED -> Color(0xFFF1634F)
    }

    val centralImageRes = when (moodState) {
        MoodState.HAPPY -> R.drawable.resistence_screen_happy_shuriken
        MoodState.NEUTRAL -> R.drawable.resistence_screen_neutral_shuriken
        MoodState.ANNOYED -> R.drawable.resistence_screen_annoyed_shuriken
    }

    val q1Res = when (moodState) {
        MoodState.HAPPY -> R.drawable.resistence_screen_happy_q1
        MoodState.NEUTRAL -> R.drawable.resistence_screen_neutral_q1
        MoodState.ANNOYED -> R.drawable.resistence_screen_annoyed_q1
    }
    val q2Res = when (moodState) {
        MoodState.HAPPY -> R.drawable.resistence_screen_happy_q2
        MoodState.NEUTRAL -> R.drawable.resistence_screen_neutral_q2
        MoodState.ANNOYED -> R.drawable.resistence_screen_annoyed_q2
    }
    val q3Res = when (moodState) {
        MoodState.HAPPY -> R.drawable.resistence_screen_happy_q3
        MoodState.NEUTRAL -> R.drawable.resistence_screen_neutral_q3
        MoodState.ANNOYED -> R.drawable.resistence_screen_annoyed_q3
    }
    val q4Res = when (moodState) {
        MoodState.HAPPY -> R.drawable.resistence_screen_happy_q4
        MoodState.NEUTRAL -> R.drawable.resistence_screen_neutral_q4
        MoodState.ANNOYED -> R.drawable.resistence_screen_annoyed_q4
    }

    val trendRes = when (moodState) {
        MoodState.HAPPY -> R.drawable.resistence_screen_happy_trend
        MoodState.NEUTRAL -> R.drawable.resistence_screen_neutral_trend
        MoodState.ANNOYED -> R.drawable.resistence_screen_annoyed_trend
    }

    val layerBlurRes = when (moodState) {
        MoodState.HAPPY -> R.drawable.resistence_screen_happy_layerblur
        MoodState.NEUTRAL -> R.drawable.resistence_screen_neutral_layerblur
        MoodState.ANNOYED -> R.drawable.resistence_screen_annoyed_layerblur
    }

    val emojiSize = 12.rsp
    val adviceText = when (moodState) {
        MoodState.HAPPY -> buildAnnotatedString {
            append("You're in ")
            withStyle(SpanStyle(color = moodColor)) { append("zenmode") }
            append(" & being so mindful")
            withStyle(SpanStyle(fontSize = emojiSize)) { append("\uD83D\uDC9A") }
        }
        MoodState.NEUTRAL -> buildAnnotatedString {
            append("Free advice: Don't lose your ")
            withStyle(SpanStyle(color = moodColor, fontWeight = FontWeight.ExtraBold)) { append("ZEN") }
        }
        MoodState.ANNOYED -> buildAnnotatedString {
            append("Put the phone down BRO, Live LIFE ")
            withStyle(SpanStyle(fontSize = emojiSize)) { append("\u2764\uFE0F") }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.rdp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ──
            ResistenceHeader(streaks = streaks)

            Spacer(modifier = Modifier.height(8.dp))

            // ── Central Image with Q1-Q4 (stepped highlight) ──
            // Phase order: 0=q2, 1=q3, 2=q4, 3=q1
            var rotationSteps by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(2000L)
                    rotationSteps++
                }
            }
            val currentPhase = rotationSteps % 4 // 0=q2, 1=q3, 2=q4, 3=q1

            val dimAlpha = 0.5f
            val q1Alpha = if (currentPhase == 3) 1f else dimAlpha
            val q2Alpha = if (currentPhase == 0) 1f else dimAlpha
            val q3Alpha = if (currentPhase == 1) 1f else dimAlpha
            val q4Alpha = if (currentPhase == 2) 1f else dimAlpha

            val shurikenRotation by animateFloatAsState(
                targetValue = rotationSteps * 90f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "shuriken_rotation"
            )

            // Glow: mood-colored tint for highlighted quadrant
            val glowTint = ColorFilter.tint(moodColor.copy(alpha = 0.55f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Q1 - upper left
                Box(
                    modifier = Modifier
                        .size(width = 101.rdp, height = 172.rdp)
                        .align(Alignment.CenterStart)
                        .offset(y = (-86).rdp)
                ) {
                    if (currentPhase == 3) {
                        Image(
                            painter = painterResource(q1Res),
                            contentDescription = null,
                            colorFilter = glowTint,
                            modifier = Modifier.matchParentSize().blur(12.dp)
                        )
                    }
                    Image(
                        painter = painterResource(q1Res),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().alpha(q1Alpha)
                    )
                }
                // Q2 - upper right
                Box(
                    modifier = Modifier
                        .size(width = 101.rdp, height = 172.rdp)
                        .align(Alignment.CenterEnd)
                        .offset(y = (-86).rdp)
                ) {
                    if (currentPhase == 0) {
                        Image(
                            painter = painterResource(q2Res),
                            contentDescription = null,
                            colorFilter = glowTint,
                            modifier = Modifier.matchParentSize().blur(12.dp)
                        )
                    }
                    Image(
                        painter = painterResource(q2Res),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().alpha(q2Alpha)
                    )
                }
                // Q3 - lower right
                Box(
                    modifier = Modifier
                        .size(width = 101.rdp, height = 172.rdp)
                        .align(Alignment.CenterEnd)
                        .offset(y = 86.rdp)
                ) {
                    if (currentPhase == 1) {
                        Image(
                            painter = painterResource(q3Res),
                            contentDescription = null,
                            colorFilter = glowTint,
                            modifier = Modifier.matchParentSize().blur(12.dp)
                        )
                    }
                    Image(
                        painter = painterResource(q3Res),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().alpha(q3Alpha)
                    )
                }
                // Q4 - lower left
                Box(
                    modifier = Modifier
                        .size(width = 101.rdp, height = 172.rdp)
                        .align(Alignment.CenterStart)
                        .offset(y = 86.rdp)
                ) {
                    if (currentPhase == 2) {
                        Image(
                            painter = painterResource(q4Res),
                            contentDescription = null,
                            colorFilter = glowTint,
                            modifier = Modifier.matchParentSize().blur(12.dp)
                        )
                    }
                    Image(
                        painter = painterResource(q4Res),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().alpha(q4Alpha)
                    )
                }

                // Central shuriken — steps by 90° quarters
                Image(
                    painter = painterResource(centralImageRes),
                    contentDescription = "Mood",
                    modifier = Modifier
                        .size(260.rdp)
                        .rotate(shurikenRotation)
                )
            }

            // ── Combined Stats Card ──
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.rdp),
                horizontalAlignment = Alignment.Start
            ) {
                // ── My Screen Time ──
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Screen Time",
                        fontFamily = CabinetGrotesque,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.rsp,
                        color = colors.textPrimary
                    )
                    if (yesterdayChangePercent != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${if (yesterdayChangePercent >= 0) "+" else ""}${yesterdayChangePercent}%",
                            fontFamily = RedditMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.rsp,
                            color = colors.percentageChangeColor(yesterdayChangePercent)
                        )
                    }
                }

                // ── Time Display ──
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.offset(y = (-4).dp)
                ) {
                    val timeLineHeight = 42.rsp
                    Text(
                        text = String.format("%02d", hours),
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 56.rsp,
                        color = colors.textPrimary,
                        style = androidx.compose.ui.text.TextStyle(
                            lineHeight = timeLineHeight,
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                    Text(
                        text = "HRS",
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.rsp,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .offset(y = (-8).rdp)
                            .padding(end = 10.rdp),
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                    Text(
                        text = String.format("%02d", mins),
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 56.rsp,
                        color = colors.textPrimary,
                        style = androidx.compose.ui.text.TextStyle(
                            lineHeight = timeLineHeight,
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                    Text(
                        text = "MINS",
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.rsp,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .offset(y = (-8).rdp),
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                    // Trend icon
                    Image(
                        painter = painterResource(trendRes),
                        contentDescription = "Trend",
                        modifier = Modifier
                            .size(width = 60.rdp, height = 56.rdp)
                            .padding(start = 2.dp)
                            .offset(y = (-4).dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Mindfulness ──
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mindfulness",
                        fontFamily = CabinetGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.rsp,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    ResistenceMindfulnessBar(
                        progress = mindfulnessProgress,
                        moodState = moodState,
                        modifier = Modifier.width(100.rdp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Advice Text ──
            Text(
                text = adviceText,
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Medium,
                fontSize = 13.rsp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.rdp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Countdown Timer ──
            CountdownCircle(
                countdownSeconds = countdownSeconds,
                countdownFinished = countdownFinished
            )

            Spacer(modifier = Modifier.weight(0.1f))

            // ── Bottom Dock with skip message ──
            ResistenceBottomDock(
                skipsLeft = skipsLeft,
                moodColor = moodColor,
                onSettingsClick = onSettingsClick,
                onSkipClick = onSkipClick,
                onPhoneClick = onPhoneClick
            )

        }
    }
}

// ── Header ──────────────────────────────────────────────────────────

@Composable
private fun ResistenceHeader(streaks: Int) {
    val colors = ZenTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.rdp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.app_icon),
            contentDescription = "ZenMode",
            modifier = Modifier.size(44.rdp)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(46.dp))
                .background(colors.borderFocus)
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

// ── Mindfulness Bar ─────────────────────────────────────────────────

@Composable
private fun ResistenceMindfulnessBar(
    progress: Int,
    moodState: MoodState,
    modifier: Modifier = Modifier
) {
    val emptyColor = Color(0xFFD9D9D9)

    val fillColor = when (moodState) {
        MoodState.HAPPY -> Color(0xFF00C700)
        MoodState.NEUTRAL -> Color(0xFFEBDE27)
        MoodState.ANNOYED -> Color(0xFFF1634F)
    }

    Box(
        modifier = modifier
            .height(20.dp)
            .drawBehind {
                val segmentCount = 12
                val gap = 4.dp.toPx()
                val barWidth = (size.width - (segmentCount - 1) * gap) / segmentCount
                val filledCount =
                    (progress.toFloat() / 100 * segmentCount).toInt().coerceAtLeast(0)
                val radius = CornerRadius(barWidth / 2f)

                for (i in 0 until segmentCount) {
                    val left = i * (barWidth + gap)
                    val color = if (i < filledCount) {
                        val fraction = if (filledCount > 0) i.toFloat() / filledCount else 0f
                        androidx.compose.ui.graphics.lerp(fillColor, emptyColor, fraction)
                    } else {
                        emptyColor.copy(alpha = 0.3f)
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = radius
                    )
                }
            }
    )
}

// ── Countdown Circle ────────────────────────────────────────────────
// Uses resistence_screen_around_time drawable, revealed progressively:
//   0→2s: 3 lines (stage1), 2→4s: 6 lines (stage2),
//   4→6s: 8 lines (stage3), 6→7s: all 10 lines (full)

@Composable
private fun CountdownCircle(
    countdownSeconds: Int,
    countdownFinished: Boolean
) {
    val colors = ZenTheme.colors
    val totalSeconds = AppConstants.COUNTDOWN_SECONDS

    // Pick the stage drawable based on elapsed seconds
    val aroundTimeRes = when {
        countdownFinished -> R.drawable.resistence_screen_around_time
        else -> {
            val t2 = (totalSeconds * 2.0 / 7).toInt().coerceAtLeast(1)
            val t4 = (totalSeconds * 4.0 / 7).toInt().coerceAtLeast(2)
            val t6 = (totalSeconds * 6.0 / 7).toInt().coerceAtLeast(3)
            when {
                countdownSeconds >= t6 -> R.drawable.resistence_screen_around_time
                countdownSeconds >= t4 -> R.drawable.resistence_screen_around_time_stage3
                countdownSeconds >= t2 -> R.drawable.resistence_screen_around_time_stage2
                countdownSeconds >= 1 -> R.drawable.resistence_screen_around_time_stage1
                else -> R.drawable.resistence_screen_around_time_stage1
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(110.rdp)
    ) {
        Image(
            painter = painterResource(aroundTimeRes),
            contentDescription = null,
            modifier = Modifier.size(110.rdp)
        )

        // Countdown number
        Text(
            text = if (countdownFinished) AppConstants.COUNTDOWN_SECONDS.toString() else countdownSeconds.toString(),
            fontFamily = RedditMono,
            fontWeight = FontWeight.Bold,
            fontSize = 36.rsp,
            color = colors.textPrimary
        )
    }
}

// ── Bottom Dock ─────────────────────────────────────────────────────

@Composable
private fun ResistenceBottomDock(
    skipsLeft: Int,
    moodColor: Color,
    onSettingsClick: () -> Unit,
    onSkipClick: () -> Unit,
    onPhoneClick: () -> Unit
) {
    val colors = ZenTheme.colors
    val canSkip = skipsLeft > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.rdp)
            .padding(top = 8.dp, bottom = 32.rdp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Settings
        Image(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = "Settings",
            modifier = Modifier
                .size(32.rdp)
                .clickable { onSettingsClick() },
            colorFilter = ColorFilter.tint(colors.textBrand)
        )

        // Skip message centered between icons
        Text(
            text = if (canSkip) {
                buildAnnotatedString {
                    append("Only ")
                    withStyle(SpanStyle(fontFamily = RedditMono, color = moodColor, fontWeight = FontWeight.Bold)) {
                        append("$skipsLeft")
                    }
                    withStyle(SpanStyle(color = moodColor, fontWeight = FontWeight.Bold)) {
                        append(" skip & open ")
                    }
                    append("left for Today")
                }
            } else {
                buildAnnotatedString {
                    append("No skips left for Today")
                }
            },
            fontFamily = CabinetGrotesque,
            fontWeight = FontWeight.Medium,
            fontSize = 12.rsp,
            color = if (canSkip) colors.textSecondary else colors.textSecondary.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = if (canSkip) Modifier.clickable { onSkipClick() } else Modifier
        )

        // Phone
        Image(
            painter = painterResource(R.drawable.ic_phone),
            contentDescription = "Phone",
            modifier = Modifier
                .size(32.rdp)
                .clickable { onPhoneClick() },
            colorFilter = ColorFilter.tint(colors.textBrand)
        )
    }
}
