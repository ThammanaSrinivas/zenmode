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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.AppLogic
import com.zenlauncher.zenmode.MoodState
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.components.FitToWidth
import com.zenlauncher.zenmode.ui.components.StreakStat
import com.zenlauncher.zenmode.ui.theme.Spacing
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.DepartureMono
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
        MoodState.HAPPY -> colors.moodHappy
        MoodState.NEUTRAL -> colors.moodNeutral
        MoodState.ANNOYED -> colors.moodAnnoyed
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
                // The status bar is 24dp on some ROMs and 54dp on a punch-hole phone; the
                // old flat 48dp top pad either wasted a band of screen or ran under the clock.
                .statusBarsPadding()
                .padding(top = 12.rdp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ──
            ResistenceHeader(streaks = streaks)

            Spacer(modifier = Modifier.height(8.rdp))

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
                    // Margin first, so the block's natural width is measured against the space
                    // it is actually allowed, not the full screen.
                    .padding(horizontal = Spacing.screenMargin)
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
                        fontFamily = Geist,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.rsp,
                        color = colors.textPrimary
                    )
                    if (yesterdayChangePercent != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${if (yesterdayChangePercent >= 0) "+" else ""}${yesterdayChangePercent}%",
                            fontFamily = DepartureMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.rsp,
                            color = colors.percentageChangeColor(yesterdayChangePercent)
                        )
                    }
                }

                // ── Time Display ──
                // One fixed-ratio readout from the Figma frame: at a 1.3x+ font scale on a
                // 360dp phone it is wider than the screen, so it scales down as a block
                // rather than wrapping "MINS" onto a second line.
                FitToWidth(modifier = Modifier.offset(y = (-4).rdp)) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val timeLineHeight = 42.rsp
                        Text(
                            text = "%02d".format(Locale.US, hours),
                            fontFamily = DepartureMono,
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
                            fontFamily = DepartureMono,
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
                            text = "%02d".format(Locale.US, mins),
                            fontFamily = DepartureMono,
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
                            fontFamily = DepartureMono,
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
                                .padding(start = 2.rdp)
                                .offset(y = (-4).rdp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.rdp))

                // ── Mindfulness ──
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mindfulness",
                        fontFamily = Geist,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.rsp,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(12.rdp))
                    ResistenceMindfulnessBar(
                        progress = mindfulnessProgress,
                        moodState = moodState,
                        modifier = Modifier.width(100.rdp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.rdp))

            // ── Advice Text ──
            Text(
                text = adviceText,
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 13.rsp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.screenMargin)
            )

            Spacer(modifier = Modifier.height(24.rdp))

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenMargin),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_zen_mark_gradient),
            contentDescription = PRODUCT_NAME,
            modifier = Modifier.size(44.rdp)
        )

        // The v3 Streaks block, shared with Home ([StreakStat]) so the two can't drift.
        // Not tappable here: the resistance screen is a wait, not a place to navigate from.
        StreakStat(streaks = streaks)
    }
}

// ── Mindfulness Bar ─────────────────────────────────────────────────

@Composable
private fun ResistenceMindfulnessBar(
    progress: Int,
    moodState: MoodState,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val emptyColor = colors.borderSubtle

    val fillColor = when (moodState) {
        MoodState.HAPPY -> colors.moodHappy
        MoodState.NEUTRAL -> colors.moodNeutral
        MoodState.ANNOYED -> colors.moodAnnoyed
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
            fontFamily = DepartureMono,
            fontWeight = FontWeight.Bold,
            fontSize = 36.rsp,
            color = colors.textPrimary
        )
    }
}

// ── Bottom Dock ─────────────────────────────────────────────────────

/** Android's minimum touch target; the dock's controls were drawn smaller than their art. */
private val MinTouchTarget: Dp @Composable get() = 48.rdp

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
            // Gesture-nav phones put the pill right where the dock sat.
            .navigationBarsPadding()
            .padding(horizontal = Spacing.screenMargin)
            .padding(top = 8.rdp, bottom = 16.rdp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Settings
        Image(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = "Settings",
            modifier = Modifier
                // 32dp of art inside a 48dp target — the icons were below the minimum.
                .size(MinTouchTarget)
                .clip(CircleShape)
                .clickable { onSettingsClick() }
                .padding(8.rdp),
            colorFilter = ColorFilter.tint(colors.textBrand)
        )

        // Skip message centered between icons
        Text(
            text = if (canSkip) {
                buildAnnotatedString {
                    append("Only ")
                    withStyle(SpanStyle(fontFamily = DepartureMono, color = moodColor, fontWeight = FontWeight.Bold)) {
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
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 12.rsp,
            color = if (canSkip) colors.textSecondary else colors.textSecondary.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                // Weighted, so the longer "Only 3 skip & open left for Today" takes the slack
                // instead of shoving the two icons off the edges of the dock.
                .weight(1f)
                .then(
                    if (canSkip) Modifier
                        .heightIn(min = MinTouchTarget)
                        .clip(RoundedCornerShape(8.rdp))
                        .clickable(onClickLabel = "Skip the wait", role = Role.Button) { onSkipClick() }
                        .wrapContentHeight()
                        .padding(horizontal = 8.rdp)
                    else Modifier.padding(horizontal = 8.rdp)
                )
        )

        // Phone
        Image(
            painter = painterResource(R.drawable.ic_phone),
            contentDescription = "Phone",
            modifier = Modifier
                .size(MinTouchTarget)
                .clip(CircleShape)
                .clickable { onPhoneClick() }
                .padding(8.rdp),
            colorFilter = ColorFilter.tint(colors.textBrand)
        )
    }
}
