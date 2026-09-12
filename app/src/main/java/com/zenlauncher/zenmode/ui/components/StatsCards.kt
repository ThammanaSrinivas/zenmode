package com.zenlauncher.zenmode.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.cardGradient
import com.zenlauncher.zenmode.ui.theme.statsCardStroke
import com.zenlauncher.zenmode.ui.theme.rsp
import com.zenlauncher.zenmode.ui.theme.rdp

// ── Constants ─────────────────────────────────────────────────────
// The v3 card: one rounded square, a mood gradient, the face filling the top.
// No border, no glow, no progress bar — the colour carries the state.

// .rdp needs LocalScreenScale, which only exists in composable scope — these were
// plain `private val`s until responsiveness surfaced that as a real crash risk, not
// just a missed scale-down; that pattern (below and in Spacing.kt) is now the rule
// for any dp/sp constant a screen reuses.
private val CardRadius: Dp @Composable get() = 20.rdp
private const val FaceAspect = 165f / 89f
// Real card geometry, Figma node 71:5450 (150.671 x 164.605) — portrait, not square.
private const val CardAspect = 150.671f / 164.605f
private val CardBorder: Dp @Composable get() = 1.9.rdp
private val ChipRadius: Dp @Composable get() = 4.8.rdp
// Score chip: rgba(0,199,0,.38) -> rgb(232,202,17) -> rgb(254,104,1), from the .fig
private val ChipGreen = Color(0x6100C700)
private val ChipYellow = Color(0xFFE8CA11)
private val ChipOrange = Color(0xFFFE6801)
private val CardInk = Color(0xFF101010)

// ── Shared Stats Row ──────────────────────────────────────────────

@Composable
fun StatsCardsRow(
    usage: DailyUsage?,
    yesterdayChangePercent: Int?,
    hasBuddies: Boolean,
    buddyStats: BuddyStats?,
    isSignedIn: Boolean,
    isWeekly: Boolean = false,
    zenScore: Int? = null,
    streaks: Int = 0,
    // BuddyStats has no score/streak fields (see MainViewModel) — there is no real
    // data to show here. Separate params, not a reuse of zenScore/streaks above, so
    // a caller can never accidentally relabel the signed-in user's own numbers as
    // the buddy's.
    buddyZenScore: Int? = null,
    buddyStreaks: Int = 0,
    showReactions: Boolean = true,
    myLikes: Long = 0L,
    buddyLikes: Long = 0L,
    onLikeClick: () -> Unit = {},
    onInviteBuddyClick: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onBuddyCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val myMinutes = ((usage?.screenTimeInMillis ?: 0L) / 1000) / 60
    // King goes to whoever has less screen time; default to me if no buddy
    val kingOnBuddy = hasBuddies && buddyStats != null && buddyStats.screenTimeMins < myMinutes

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.rdp),
            // Figma: card1 ends at x=181.67 (31+150.67), card2 starts at x=231 — a
            // 49.33dp gap, not the 20dp I'd guessed. Narrower cards, wider gap.
            horizontalArrangement = Arrangement.spacedBy(49.rdp)
        ) {
            if (isSignedIn) {
                MyScreenTimeCard(
                    usage = usage,
                    yesterdayChangePercent = yesterdayChangePercent,
                    isWeekly = isWeekly,
                    zenScore = zenScore,
                    streaks = streaks,
                    isWinner = !kingOnBuddy,
                    buddyLikes = if (hasBuddies && showReactions) buddyLikes else 0L,
                    modifier = Modifier.weight(1f)
                )
            } else {
                SignInCard(
                    onSignInClick = onSignInClick,
                    modifier = Modifier.weight(1f)
                )
            }

            if (hasBuddies && buddyStats != null) {
                BuddyStatsCard(
                    buddyStats = buddyStats,
                    onCardClick = onBuddyCardClick,
                    zenScore = buddyZenScore,
                    streaks = buddyStreaks,
                    isWinner = kingOnBuddy,
                    showReactions = showReactions,
                    myLikes = myLikes,
                    onLikeClick = onLikeClick,
                    modifier = Modifier.weight(1f)
                )
            } else {
                BuddyInviteCard(
                    onInviteBuddyClick = onInviteBuddyClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bolt between the two cards — Figma node 2001:1521
        Image(
            painter = painterResource(R.drawable.ic_bolt_v3),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .width(18.rdp)
                .height(29.4.rdp)
                .zIndex(1f)
        )
    }
}

// ── Mood Card ─────────────────────────────────────────────────────

@Composable
private fun MoodCard(
    faceRes: Int,
    faceDescription: String,
    label: String,
    minutes: Long,
    mood: MoodState,
    zenScore: Int?,
    streaks: Int,
    changePercent: Int?,
    isWinner: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors

    val stroke = colors.statsCardStroke(mood)

    Box(modifier = modifier.aspectRatio(CardAspect)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .dropShadow(
                    color = Color.Black.copy(alpha = 0.25f),
                    blur = 24.3.rdp,
                    cornerRadius = CardRadius,
                    offsetY = 7.1.rdp
                )
                .clip(RoundedCornerShape(CardRadius))
                .background(Brush.verticalGradient(colors.cardGradient(mood)))
                .innerGlow(color = stroke, cornerRadius = CardRadius, blur = 28.9.rdp, spread = (-8.7).dp)
                .border(CardBorder, stroke.copy(alpha = 0.2f), RoundedCornerShape(CardRadius))
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The face drawables are 165x89. Render at their own ratio and nothing
            // gets cropped — any other value cuts the top of the head or the chin.
            Image(
                painter = painterResource(faceRes),
                contentDescription = faceDescription,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(FaceAspect)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 20.rdp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // "My Buddy's Stats" is longer than "My Screen Time" and this row
                    // has no fixed height — on a narrower card (smaller phone, or the
                    // weekly variant's extra +N% badge eating width) it could wrap to
                    // two lines and collide with the time row below. It reads fine
                    // clipped; it must never wrap.
                    Text(
                        text = label,
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.rsp,
                        letterSpacing = (-0.26).sp,
                        color = CardInk,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (changePercent != null) {
                        Spacer(modifier = Modifier.width(4.rdp))
                        Text(
                            text = "${if (changePercent >= 0) "+" else ""}$changePercent%",
                            fontFamily = DepartureMono,
                            fontSize = 9.rsp,
                            color = CardInk.copy(alpha = 0.55f),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.rdp))

                Row(verticalAlignment = Alignment.Bottom) {
                    TimeUnit(value = minutes / 60, unit = "HRS", unitSize = 6.8.rsp)
                    Spacer(modifier = Modifier.width(4.rdp))
                    TimeUnit(value = minutes % 60, unit = "MINS", unitSize = 5.8.rsp)
                }
            }
        }
        }

        // Score, bottom-left — the same number as the header badge
        zenScore?.let { score ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    // Figma node 71:5455: square top-left corner (flush with the
                    // card's own curve), the other three rounded to 4.8dp.
                    .clip(
                        RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = ChipRadius,
                            bottomEnd = ChipRadius,
                            bottomStart = ChipRadius
                        )
                    )
                    .background(
                        Brush.linearGradient(
                            0f to ChipGreen,
                            0.33f to ChipYellow,
                            0.9f to ChipOrange
                        )
                    )
                    .padding(start = 9.rdp, end = 10.rdp, top = 4.rdp, bottom = 5.rdp)
            ) {
                Text(
                    text = score.toString(),
                    fontFamily = DepartureMono,
                    fontSize = 12.rsp,
                    letterSpacing = (-2).sp,
                    color = CardInk,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }

        // Streak, bottom-right — Figma node 71:5457: the real flame silhouette
        // at 80% opacity with the count set in Geist SemiBold, not mono.
        if (streaks > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.rdp, bottom = 4.rdp)
                    .width(22.rdp)
                    .aspectRatio(27.3856f / 35.3743f)
                    .alpha(0.8f)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_card_flame),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = streaks.toString(),
                    fontFamily = Geist,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.rsp,
                    letterSpacing = (-0.6).sp,
                    color = Color.Black,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 4.rdp),
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }

        // Crown — hand-drawn, hanging off the winner's top-left corner.
        // Figma node 2001:1521; real proportions, not a square icon.
        if (isWinner) {
            Image(
                painter = painterResource(R.drawable.ic_crown_v3),
                contentDescription = "Lowest screen time",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-4).dp, y = (-16).dp)
                    .width(30.5.rdp)
                    .height(26.5.rdp)
            )
        }
    }
}

@Composable
private fun TimeUnit(value: Long, unit: String, unitSize: androidx.compose.ui.unit.TextUnit) {
    Text(
        text = buildAnnotatedString {
            append(String.format("%02d", value))
            // The numerals' tight negative tracking would otherwise bleed into
            // this suffix and collapse "MINS" into itself — reset it explicitly.
            withStyle(
                SpanStyle(
                    fontSize = unitSize,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
            ) {
                append(unit)
            }
        },
        fontFamily = DepartureMono,
        fontSize = 32.rsp,
        letterSpacing = (-2.5).sp,
        color = CardInk,
        maxLines = 1,
        softWrap = false
    )
}

private fun faceFor(mood: MoodState) = when (mood) {
    MoodState.HAPPY -> R.drawable.face_happy
    MoodState.NEUTRAL -> R.drawable.face_neutral
    MoodState.ANNOYED -> R.drawable.face_annoyed
}

// ── My Screen Time Card ───────────────────────────────────────────

@Composable
fun MyScreenTimeCard(
    usage: DailyUsage?,
    yesterdayChangePercent: Int?,
    isWeekly: Boolean = false,
    zenScore: Int? = null,
    streaks: Int = 0,
    isWinner: Boolean = false,
    buddyLikes: Long = 0L,
    modifier: Modifier = Modifier
) {
    val totalMillis = usage?.screenTimeInMillis ?: 0L
    val minutes = (totalMillis / 1000) / 60
    val moodState = if (isWeekly) AppLogic.getWeeklyMoodState(minutes)
                    else AppLogic.getMoodState(minutes)

    Box(modifier = modifier) {
        MoodCard(
            faceRes = faceFor(moodState),
            faceDescription = "Mood face",
            label = if (isWeekly) "My Weekly Time" else "My Screen Time",
            minutes = minutes,
            mood = moodState,
            zenScore = zenScore,
            streaks = streaks,
            changePercent = yesterdayChangePercent,
            isWinner = isWinner
        )

        if (buddyLikes > 0) {
            ReactBadge(
                count = buddyLikes,
                clickable = false,
                onClick = {},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.rdp, y = (-6).dp)
            )
        }
    }
}

// ── Buddy Stats Card ──────────────────────────────────────────────

@Composable
fun BuddyStatsCard(
    buddyStats: BuddyStats,
    onCardClick: (() -> Unit)? = null,
    zenScore: Int? = null,
    streaks: Int = 0,
    isWinner: Boolean = false,
    showReactions: Boolean = true,
    myLikes: Long = 0L,
    onLikeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val minutes = buddyStats.screenTimeMins
    val moodState = AppLogic.getMoodState(minutes)

    Box(
        modifier = modifier.then(
            if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier
        )
    ) {
        MoodCard(
            faceRes = faceFor(moodState),
            faceDescription = "Buddy mood face",
            label = "My Buddy's Stats",
            minutes = minutes,
            mood = moodState,
            zenScore = zenScore,
            streaks = streaks,
            changePercent = null,
            isWinner = isWinner
        )

        if (showReactions) {
            ReactBadge(
                count = myLikes,
                clickable = true,
                onClick = onLikeClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.rdp, y = (-6).dp)
            )
        }
    }
}

// ── React Badge ───────────────────────────────────────────────────

@Composable
fun ReactBadge(
    count: Long,
    clickable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val size = 36.rdp

    Box(
        modifier = modifier.size(size + 8.rdp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(colors.bgSecondary)
                .then(
                    if (clickable) Modifier.border(1.rdp, colors.borderFocus, CircleShape)
                    else Modifier
                )
                .then(
                    if (clickable) Modifier.clickable(onClick = onClick) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.heart_react),
                contentDescription = "React",
                modifier = Modifier.size(size * 0.55f),
                contentScale = ContentScale.Fit
            )
        }

        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.rdp)
                    .clip(CircleShape)
                    .background(colors.borderFocus),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    fontFamily = DepartureMono,
                    fontSize = 10.rsp,
                    lineHeight = 10.rsp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

// ── Placeholder Cards ─────────────────────────────────────────────

@Composable
private fun DashedCard(
    faceDescription: String,
    title: String,
    subtitle: String,
    buttonRes: Int,
    buttonDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    // Same shape as the real stats cards, so a dashed placeholder sitting next to
    // one in the row reads as the same card, not a mismatched square.
    val dashStroke = 1.rdp
    val dashOn = 8.rdp
    val dashOff = 8.rdp
    val cardRadius = CardRadius

    Column(
        modifier = modifier
            .aspectRatio(CardAspect)
            .clip(RoundedCornerShape(cardRadius))
            .background(colors.bgSecondary)
            .drawWithContent {
                drawContent()
                val strokeWidth = dashStroke.toPx()
                val dash = dashOn.toPx()
                val gap = dashOff.toPx()
                drawRoundRect(
                    color = colors.textSecondary,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    cornerRadius = CornerRadius(cardRadius.toPx()),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f)
                    )
                )
            }
            .padding(bottom = 10.rdp)
    ) {
        Image(
            painter = painterResource(R.drawable.face_get_your_buddy),
            contentDescription = faceDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Column(modifier = Modifier.padding(horizontal = 10.rdp)) {
            Text(
                text = title,
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.rsp,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                fontFamily = Geist,
                fontWeight = FontWeight.Normal,
                fontSize = 9.rsp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(4.rdp))

            Image(
                painter = painterResource(buttonRes),
                contentDescription = buttonDescription,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.rdp))
                    .clickable { onClick() }
            )
        }
    }
}

@Composable
fun BuddyInviteCard(
    onInviteBuddyClick: () -> Unit,
    modifier: Modifier = Modifier
) = DashedCard(
    faceDescription = "Buddy face",
    title = "Get your Buddy!",
    subtitle = "pick a wise one!",
    buttonRes = R.drawable.button_invite_buddy,
    buttonDescription = "Invite buddy",
    onClick = onInviteBuddyClick,
    modifier = modifier
)

@Composable
fun SignInCard(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) = DashedCard(
    faceDescription = "Sign in face",
    title = "Sign In",
    subtitle = "to track & sync stats",
    buttonRes = R.drawable.button_sign_in,
    buttonDescription = "Sign in with Google",
    onClick = onSignInClick,
    modifier = modifier
)

// ── Modifiers ─────────────────────────────────────────────────────

/** Inset glow: Figma's `shadow-[inset_0_0_28.875px_-8.663px_#12b117]` on the card. */
fun Modifier.innerGlow(
    color: Color,
    cornerRadius: Dp,
    blur: Dp,
    spread: Dp = 0.dp
) = drawWithContent {
    drawContent()
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            this.color = color.toArgb()
            isAntiAlias = true
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
                    spreadPx, spreadPx,
                    size.width - spreadPx, size.height - spreadPx
                ),
                cornerRadiusPx, cornerRadiusPx,
                android.graphics.Path.Direction.CCW
            )
        }
        canvas.nativeCanvas.drawPath(outerPath, paint)
        canvas.nativeCanvas.restore()
    }
}

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
