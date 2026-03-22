package com.zenlauncher.zenmode.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.AppLogic
import com.zenlauncher.zenmode.MoodState
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.RedditMono
import com.zenlauncher.zenmode.ui.theme.Silkscreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

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

    val adviceText = when (moodState) {
        MoodState.HAPPY -> buildAnnotatedString {
            append("You're in ")
            withStyle(SpanStyle(color = moodColor)) { append("zenmode") }
            append(" & being so mindful")
            withStyle(SpanStyle(fontSize = 12.sp)) { append("\uD83D\uDC9A") }
        }
        MoodState.NEUTRAL -> buildAnnotatedString {
            append("Free advice: Don't lose your ")
            withStyle(SpanStyle(color = moodColor, fontWeight = FontWeight.ExtraBold)) { append("ZEN") }
        }
        MoodState.ANNOYED -> buildAnnotatedString {
            append("Put the phone down BRO, Live LIFE ")
            withStyle(SpanStyle(fontSize = 12.sp)) { append("\u2764\uFE0F") }
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
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ──
            ResistenceHeader(streaks = streaks)

            Spacer(modifier = Modifier.height(8.dp))

            // ── Central Image with Q1-Q4 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Q1-Q4 flush to left/right edges, centered on shuriken
                // Q1 - upper left
                Image(
                    painter = painterResource(q1Res),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 202.dp, height = 172.dp)
                        .align(Alignment.CenterStart)
                        .offset(y = (-86).dp)
                )
                // Q2 - upper right
                Image(
                    painter = painterResource(q2Res),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 202.dp, height = 172.dp)
                        .align(Alignment.CenterEnd)
                        .offset(y = (-86).dp)
                )
                // Q3 - lower right
                Image(
                    painter = painterResource(q3Res),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 202.dp, height = 172.dp)
                        .align(Alignment.CenterEnd)
                        .offset(y = 86.dp)
                )
                // Q4 - lower left
                Image(
                    painter = painterResource(q4Res),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 202.dp, height = 172.dp)
                        .align(Alignment.CenterStart)
                        .offset(y = 86.dp)
                )

                // Central mood image — slow spin
                val shurikenTransition = rememberInfiniteTransition(label = "shuriken_spin")
                val shurikenRotation by shurikenTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 12000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "shuriken_rotation"
                )
                Image(
                    painter = painterResource(centralImageRes),
                    contentDescription = "Mood",
                    modifier = Modifier
                        .size(220.dp)
                        .rotate(shurikenRotation)
                )
            }

            // ── My Screen Time ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "My Screen Time",
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = colors.textPrimary
                )
                if (yesterdayChangePercent != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${if (yesterdayChangePercent >= 0) "+" else ""}${yesterdayChangePercent}%",
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (yesterdayChangePercent <= 0) colors.textBrand else Color(0xFFF1634F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Time Display ──
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text(
                    text = String.format("%02d", hours),
                    fontFamily = RedditMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "HRS",
                    fontFamily = RedditMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp, end = 12.dp)
                )
                Text(
                    text = String.format("%02d", mins),
                    fontFamily = RedditMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "MINS",
                    fontFamily = RedditMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                )
                // Trend icon
                Image(
                    painter = painterResource(trendRes),
                    contentDescription = "Trend",
                    modifier = Modifier
                        .size(width = 45.dp, height = 44.dp)
                        .padding(bottom = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Mindfulness ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                Text(
                    text = "Mindfulness",
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                ResistenceMindfulnessBar(
                    progress = mindfulnessProgress,
                    moodState = moodState,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Advice Text ──
            Text(
                text = adviceText,
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
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

            Spacer(modifier = Modifier.height(16.dp))
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
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.app_icon),
            contentDescription = "ZenMode",
            modifier = Modifier.size(32.dp)
        )

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
            .height(18.dp)
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
                    val color = if (i < filledCount) {
                        val fraction = if (filledCount > 0) i.toFloat() / filledCount else 0f
                        androidx.compose.ui.graphics.lerp(fillColor, emptyColor, fraction)
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
        modifier = Modifier.size(110.dp)
    ) {
        Image(
            painter = painterResource(aroundTimeRes),
            contentDescription = null,
            modifier = Modifier.size(110.dp)
        )

        // Countdown number
        Text(
            text = if (countdownFinished) AppConstants.COUNTDOWN_SECONDS.toString() else countdownSeconds.toString(),
            fontFamily = RedditMono,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Settings
        Image(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = "Settings",
            modifier = Modifier
                .size(32.dp)
                .clickable { onSettingsClick() },
            colorFilter = ColorFilter.tint(colors.textBrand)
        )

        // Skip message centered between icons
        Text(
            text = if (canSkip) {
                buildAnnotatedString {
                    append("Only ")
                    withStyle(SpanStyle(color = moodColor, fontWeight = FontWeight.Bold)) {
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
            fontSize = 12.sp,
            color = if (canSkip) colors.textSecondary else colors.textSecondary.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = if (canSkip) Modifier.clickable { onSkipClick() } else Modifier
        )

        // Phone
        Image(
            painter = painterResource(R.drawable.ic_phone),
            contentDescription = "Phone",
            modifier = Modifier
                .size(32.dp)
                .clickable { onPhoneClick() },
            colorFilter = ColorFilter.tint(colors.textBrand)
        )
    }
}
