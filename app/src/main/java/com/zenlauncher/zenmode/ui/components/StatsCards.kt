package com.zenlauncher.zenmode.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zenlauncher.zenmode.AppLogic
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.MoodState
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.RedditMono
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.percentageChangeColor
import com.zenlauncher.zenmode.ui.theme.rsp
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.statsCardFill

// ── Shared Stats Row ──────────────────────────────────────────────

@Composable
fun StatsCardsRow(
    usage: DailyUsage?,
    yesterdayChangePercent: Int?,
    hasBuddies: Boolean,
    buddyStats: BuddyStats?,
    isSignedIn: Boolean,
    isWeekly: Boolean = false,
    onInviteBuddyClick: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onBuddyCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val myMinutes = ((usage?.screenTimeInMillis ?: 0L) / 1000) / 60
    // King goes to whoever has less screen time; default to me if no buddy
    val kingOnBuddy = hasBuddies && buddyStats != null && buddyStats.screenTimeMins < myMinutes

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // King crown positioned above the winning card (least screen time)
        Image(
            painter = painterResource(R.drawable.king),
            contentDescription = "King",
            modifier = Modifier
                .padding(bottom = 8.dp)
                .size(28.dp)
                .then(
                    if (kingOnBuddy) Modifier.align(Alignment.CenterHorizontally).offset(x = 16.dp)
                    else Modifier.align(Alignment.Start)
                )
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // My Screen Time card (or Sign In card when logged out)
                if (isSignedIn) {
                    MyScreenTimeCard(
                        usage = usage,
                        yesterdayChangePercent = yesterdayChangePercent,
                        isWeekly = isWeekly,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(end = 8.dp)
                    )
                } else {
                    SignInCard(
                        onSignInClick = onSignInClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(end = 8.dp)
                    )
                }

                // Show buddy stats if connected, otherwise show invite card
                if (hasBuddies && buddyStats != null) {
                    BuddyStatsCard(
                        buddyStats = buddyStats,
                        onCardClick = onBuddyCardClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 8.dp)
                    )
                } else {
                    BuddyInviteCard(
                        onInviteBuddyClick = onInviteBuddyClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 8.dp)
                    )
                }
            }

            // Flash icon between cards
            Image(
                painter = painterResource(R.drawable.flash),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(21.dp)
                    .height(35.dp)
                    .zIndex(1f)
            )
        }
    }
}

// ── My Screen Time Card ───────────────────────────────────────────

@Composable
fun MyScreenTimeCard(
    usage: DailyUsage?,
    yesterdayChangePercent: Int?,
    isWeekly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val totalMillis = usage?.screenTimeInMillis ?: 0L
    val minutes = (totalMillis / 1000) / 60
    val hours = minutes / 60
    val mins = minutes % 60
    val moodState = if (isWeekly) AppLogic.getWeeklyMoodState(minutes)
                    else AppLogic.getMoodState(minutes)
    val mindfulnessProgress = if (isWeekly) AppLogic.getWeeklyMindfulnessPercentage(minutes)
                              else AppLogic.getMindfulnessPercentage(minutes)

    val faceRes = when (moodState) {
        MoodState.HAPPY -> R.drawable.face_happy
        MoodState.NEUTRAL -> R.drawable.face_neutral
        MoodState.ANNOYED -> R.drawable.face_annoyed
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.statsCardFill(moodState))
                .border(2.dp, when (moodState) {
                    MoodState.HAPPY -> colors.strokeHappy
                    MoodState.NEUTRAL -> colors.strokeNeutral
                    MoodState.ANNOYED -> colors.strokeAnnoyed
                }, RoundedCornerShape(12.dp))
                .innerShadow(
                    color = when (moodState) {
                        MoodState.HAPPY -> colors.strokeHappy
                        MoodState.NEUTRAL -> colors.strokeNeutral
                        MoodState.ANNOYED -> colors.strokeAnnoyed
                    },
                    cornerRadius = 12.dp,
                    blur = 30.dp,
                    spread = (-9).dp
                )
                .padding(bottom = 10.dp)
        ) {
            // Face
            Image(
                painter = painterResource(faceRes),
                contentDescription = "Mood face",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )

            Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                // Title row with percentage change
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                ) {
                    Text(
                        text = if (isWeekly) "My Weekly Time" else "My Screen Time",
                        fontFamily = CabinetGrotesque,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.rsp,
                        color = colors.textPrimary
                    )
                    if (yesterdayChangePercent != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (yesterdayChangePercent >= 0) "+" else ""}${yesterdayChangePercent}%",
                            fontFamily = RedditMono,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.rsp,
                            color = colors.percentageChangeColor(yesterdayChangePercent)
                        )
                    }
                }

                // Time display
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.offset(y = (-4).dp)
                ) {
                    Text(
                        text = String.format("%02d", hours),
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.rsp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "HRS",
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 8.rsp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(start = 2.dp, end = 4.rdp)
                    )
                    Text(
                        text = String.format("%02d", mins),
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.rsp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "MINS",
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 8.rsp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Mindfulness bar
                MindfulnessBar(
                    progress = mindfulnessProgress,
                    moodState = moodState
                )
            }
        }
    }
}

// ── Buddy Stats Card ──────────────────────────────────────────────

@Composable
fun BuddyStatsCard(
    buddyStats: BuddyStats,
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val minutes = buddyStats.screenTimeMins
    val hours = minutes / 60
    val mins = minutes % 60
    val moodState = AppLogic.getMoodState(minutes)
    val mindfulnessProgress = AppLogic.getMindfulnessPercentage(minutes)

    val faceRes = when (moodState) {
        MoodState.HAPPY -> R.drawable.face_happy
        MoodState.NEUTRAL -> R.drawable.face_neutral
        MoodState.ANNOYED -> R.drawable.face_annoyed
    }

    Box(
        modifier = modifier.then(
            if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.statsCardFill(moodState))
                .border(2.dp, when (moodState) {
                    MoodState.HAPPY -> colors.strokeHappy
                    MoodState.NEUTRAL -> colors.strokeNeutral
                    MoodState.ANNOYED -> colors.strokeAnnoyed
                }, RoundedCornerShape(12.dp))
                .innerShadow(
                    color = when (moodState) {
                        MoodState.HAPPY -> colors.strokeHappy
                        MoodState.NEUTRAL -> colors.strokeNeutral
                        MoodState.ANNOYED -> colors.strokeAnnoyed
                    },
                    cornerRadius = 12.dp,
                    blur = 30.dp,
                    spread = (-9).dp
                )
                .padding(bottom = 10.dp)
        ) {
            // Face
            Image(
                painter = painterResource(faceRes),
                contentDescription = "Buddy mood face",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )

            Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                // Title
                Text(
                    text = "My Buddy's Stats",
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.rsp,
                    color = colors.textPrimary
                )

                // Time display
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.offset(y = (-4).dp)
                ) {
                    Text(
                        text = String.format("%02d", hours),
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.rsp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "HRS",
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 8.rsp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(start = 2.dp, end = 4.rdp)
                    )
                    Text(
                        text = String.format("%02d", mins),
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.rsp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "MINS",
                        fontFamily = RedditMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 8.rsp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Mindfulness bar
                MindfulnessBar(
                    progress = mindfulnessProgress,
                    moodState = moodState
                )
            }
        }
    }
}

// ── Mindfulness Bar ───────────────────────────────────────────────

@Composable
fun MindfulnessBar(
    progress: Int,
    moodState: MoodState
) {
    val colors = ZenTheme.colors
    val emptyColor = Color(0xFFD9D9D9)

    val fillStartColor = when (moodState) {
        MoodState.HAPPY -> colors.borderFocus
        MoodState.NEUTRAL -> colors.moodNeutral
        MoodState.ANNOYED -> colors.moodAnnoyed
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Mindfulness",
            fontFamily = CabinetGrotesque,
            fontWeight = FontWeight.Medium,
            fontSize = 10.rsp,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Gradient segmented bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .drawBehind {
                    val segmentCount = 12
                    val barWidth = 3.dp.toPx()
                    val gap = 2.dp.toPx()
                    val filledCount =
                        (progress.toFloat() / 100 * segmentCount).toInt().coerceAtLeast(0)
                    val radius = CornerRadius(barWidth / 2f)

                    for (i in 0 until segmentCount) {
                        val left = i * (barWidth + gap)
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
                            size = Size(barWidth, size.height),
                            cornerRadius = radius
                        )
                    }
                }
        )
    }
}

// ── Buddy Invite Card ─────────────────────────────────────────────

@Composable
fun BuddyInviteCard(
    onInviteBuddyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.borderSubtle)
            .drawWithContent {
                drawContent()
                val strokeWidth = 1.dp.toPx()
                val dash = 8.dp.toPx()
                val gap = 8.dp.toPx()
                val cr = 12.dp.toPx()
                drawRoundRect(
                    color = colors.textSecondary,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    cornerRadius = CornerRadius(cr),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f)
                    )
                )
            }
            .innerShadow(
                color = colors.textSecondary,
                cornerRadius = 12.dp,
                blur = 30.dp,
                spread = (-9).dp
            )
            .padding(bottom = 10.dp)
    ) {
        // Grey face
        Image(
            painter = painterResource(R.drawable.face_get_your_buddy),
            contentDescription = "Buddy face",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
        )

        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                text = "Get your Buddy!",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.rsp,
                color = colors.textPrimary
            )

            val emojiSize = 10.rsp
            Text(
                text = buildAnnotatedString {
                    append("pick a wise one!")
                    withStyle(SpanStyle(fontSize = emojiSize)) {
                        append("❤️")
                    }
                },
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Medium,
                fontSize = 9.rsp,
                color = colors.textSecondary,
                modifier = Modifier.offset(y = (-4).dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Invite buddy button
            Image(
                painter = painterResource(R.drawable.button_invite_buddy),
                contentDescription = "Invite buddy",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onInviteBuddyClick() }
            )
        }
    }
}

// ── Sign In Card ──────────────────────────────────────────────────

@Composable
fun SignInCard(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.borderSubtle)
            .drawWithContent {
                drawContent()
                val strokeWidth = 1.dp.toPx()
                val dash = 8.dp.toPx()
                val gap = 8.dp.toPx()
                val cr = 12.dp.toPx()
                drawRoundRect(
                    color = colors.textSecondary,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    cornerRadius = CornerRadius(cr),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f)
                    )
                )
            }
            .innerShadow(
                color = colors.textSecondary,
                cornerRadius = 12.dp,
                blur = 30.dp,
                spread = (-9).dp
            )
            .padding(bottom = 10.dp)
    ) {
        // Grey face
        Image(
            painter = painterResource(R.drawable.face_get_your_buddy),
            contentDescription = "Sign in face",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
        )

        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                text = "Sign In",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.rsp,
                color = colors.textPrimary
            )

            Text(
                text = "to track & sync stats",
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Medium,
                fontSize = 9.rsp,
                color = colors.textSecondary,
                modifier = Modifier.offset(y = (-4).dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Sign in button
            Image(
                painter = painterResource(R.drawable.button_sign_in),
                contentDescription = "Sign in with Google",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSignInClick() }
            )
        }
    }
}

// ── Modifiers ─────────────────────────────────────────────────────

fun Modifier.dropShadow(
    color: Color,
    blur: Dp,
    cornerRadius: Dp = 0.dp,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    spread: Dp = 0.dp
) = drawBehind {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            this.color = color.toArgb()
            this.isAntiAlias = true
            if (blur.toPx() > 0) {
                maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
        }
        val spreadPx = spread.toPx()
        val cornerRadiusPx = cornerRadius.toPx()

        val left = offsetX.toPx() - spreadPx
        val top = offsetY.toPx() - spreadPx
        val right = size.width + offsetX.toPx() + spreadPx
        val bottom = size.height + offsetY.toPx() + spreadPx

        canvas.nativeCanvas.drawRoundRect(
            android.graphics.RectF(left, top, right, bottom),
            cornerRadiusPx, cornerRadiusPx,
            paint
        )
    }
}

fun Modifier.innerShadow(
    color: Color,
    cornerRadius: Dp,
    blur: Dp,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    spread: Dp = 0.dp
) = drawWithContent {
    drawContent()
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            this.color = color.toArgb()
            this.isAntiAlias = true
            if (blur.toPx() > 0) {
                maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
        }
        val cornerRadiusPx = cornerRadius.toPx()
        val spreadPx = spread.toPx()

        canvas.nativeCanvas.save()

        val clipPath = android.graphics.Path().apply {
            addRoundRect(
                android.graphics.RectF(0f, 0f, size.width, size.height),
                cornerRadiusPx, cornerRadiusPx,
                android.graphics.Path.Direction.CW
            )
        }
        canvas.nativeCanvas.clipPath(clipPath)

        val outerPath = android.graphics.Path().apply {
            addRect(
                android.graphics.RectF(-100f, -100f, size.width + 100f, size.height + 100f),
                android.graphics.Path.Direction.CW
            )
            addRoundRect(
                android.graphics.RectF(
                    spreadPx + offsetX.toPx(),
                    spreadPx + offsetY.toPx(),
                    size.width - spreadPx + offsetX.toPx(),
                    size.height - spreadPx + offsetY.toPx()
                ),
                cornerRadiusPx, cornerRadiusPx,
                android.graphics.Path.Direction.CCW
            )
        }
        canvas.nativeCanvas.drawPath(outerPath, paint)

        canvas.nativeCanvas.restore()
    }
}
