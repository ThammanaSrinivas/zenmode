package com.zenlauncher.zenmode.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import kotlin.random.Random
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.key
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.components.BuddyStatsCard
import com.zenlauncher.zenmode.ui.components.MyScreenTimeCard
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

// ── My Zen Circle ─────────────────────────────────────────────────
// The circle dashboard reached from "Maybe later" (Figma node 2026:2493): today's ranking of
// everyone in the circle. One shared position drives three things, so they always agree:
//   • the card stack — the selected member's card sits in front, the next one peeks behind
//   • the ranking band — rank, name and Zen Score of the selected member
//   • the name wheel — a clock dial on the dashed arc; the selected name gets the big needle
// Swipe the cards or the wheel (or tap a chevron) to move through members; it loops.
//
// Motion: on entry the wheel spins the first member into place (cards shuffle with it), the
// band draws out from the centre, and the chevrons nudge once to teach the swipe. Reactions
// (🫠 / 🤍) release bursts of flying emojis over the card.

/** One person in the circle, already resolved to display values. */
data class ZenCircleMember(
    val name: String,
    val isYou: Boolean,
    val screenTimeMinutes: Long,
    /** 0–100, shown as x.y out of 10. */
    val zenScore: Int,
    val streaks: Int,
    val changePercent: Int? = null
)

/** Degrees between neighbouring names on the wheel (Figma: ~94dp of arc on a 251dp radius). */
private const val WheelStepDegrees = 21.5f
private const val FrontCardScale = 201.66f / 150.67f
private const val BackCardScale = 190.25f / 150.67f

@Composable
fun ZenCircleScreen(
    members: List<ZenCircleMember>,
    userCode: String?,
    onBackClick: () -> Unit,
    onShareInviteLink: () -> Unit,
    onCopyInviteCode: () -> Unit,
    onBackToHome: () -> Unit,
    onSendLove: (ZenCircleMember) -> Unit,
    onSendMelt: (ZenCircleMember) -> Unit,
    onWeeklyClick: () -> Unit,
    removingBuddy: Boolean,
    onRemoveBuddy: () -> Unit,
    onLeaveCircle: () -> Unit,
    initialSheet: ZenCircleSheet? = null
) {
    require(members.isNotEmpty()) { "A Zen Circle always has at least you in it" }
    BackHandler(onBack = onBackClick)

    val washEdge = colorResource(R.color.wash_neutral_edge)
    val washCore = colorResource(R.color.wash_neutral_core)
    val inspection = LocalInspectionMode.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Rank = today's screen time, lowest first (same rule as the home screen crown).
    val ranks = remember(members) {
        members.indices.sortedBy { members[it].screenTimeMinutes }
            .withIndex().associate { (rank, memberIndex) -> memberIndex to rank + 1 }
    }

    val count = members.size
    // Continuous wheel position; the member at round(position) is selected. Starts one step
    // back so the entrance spins the first member into place.
    val position = remember { Animatable(if (inspection || count < 2) 0f else -1f) }
    val selected by remember { derivedStateOf { Math.floorMod(position.value.roundToInt(), count) } }
    LaunchedEffect(Unit) {
        if (position.value != 0f) {
            delay(260)
            position.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessVeryLow))
        }
    }
    var lastSelected by remember { mutableIntStateOf(selected) }
    LaunchedEffect(selected) {
        if (selected != lastSelected) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastSelected = selected
        }
    }

    val density = LocalDensity.current
    val stepPx = with(density) { 94.dp.toPx() }
    fun settle(target: Float) {
        scope.launch { position.animateTo(target, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
    }
    // A swipe moves at most one member, however far it travels — with two people a long
    // swipe would otherwise lap the loop and land back where it started.
    var dragOrigin by remember { mutableStateOf(0f) }
    val swipe = Modifier.draggable(
        orientation = Orientation.Horizontal,
        enabled = count > 1,
        state = rememberDraggableState { delta ->
            scope.launch {
                position.snapTo((position.value - delta / stepPx).coerceIn(dragOrigin - 1f, dragOrigin + 1f))
            }
        },
        onDragStarted = { dragOrigin = position.value.roundToInt().toFloat() },
        onDragStopped = { velocity ->
            val travelled = position.value - dragOrigin
            val target = when {
                abs(velocity) > 500f -> dragOrigin - sign(velocity)
                abs(travelled) > 0.35f -> dragOrigin + sign(travelled)
                else -> dragOrigin
            }
            settle(target)
        }
    )
    val step: (Int) -> Unit = { by -> if (count > 1) settle(position.value.roundToInt().toFloat() + by) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(washEdge, washCore, washEdge)))
            .pointerInput(Unit) { detectTapGestures() }
    ) {
        ZenCircleSheetHost(
            userCode = userCode,
            onShareInviteLink = onShareInviteLink,
            onCopyInviteCode = onCopyInviteCode,
            settings = ZenCircleSettings(
                buddyName = members.firstOrNull { !it.isYou }?.name ?: "your Zen Bro",
                memberCount = members.size,
                removing = removingBuddy,
                onRemoveBuddy = onRemoveBuddy,
                onLeaveCircle = onLeaveCircle
            ),
            initialSheet = initialSheet
        ) { pageModifier, openSheet ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // The Figma frame fits 898dp of content. The cards and band always keep their Figma
                // size; the difference on other phones is absorbed by the name wheel's open middle
                // and the gaps, so both buttons stay pinned near the bottom edge.
                val bars = WindowInsets.systemBars.asPaddingValues()
                val available = maxHeight - bars.calculateTopPadding() - bars.calculateBottomPadding()
                val designHeight = 898.rdp
                val deficit = (designHeight - available).coerceAtLeast(0.dp)
                val wheelCut = deficit.coerceAtMost(30.rdp)
                val gapFit = (1f - (deficit - wheelCut) / 112.75.rdp).coerceIn(0.55f, 1f)
                val wheelHeight = 198.8.rdp - wheelCut + (available - designHeight).coerceAtLeast(0.dp)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .then(pageModifier)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 12.rdp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(33.35.rdp * gapFit))
                    ZenCircleHeader(
                        onBackClick = onBackClick,
                        onMenuClick = { openSheet(ZenCircleSheet.Settings) }
                    )

                    Spacer(Modifier.height(14.5.rdp * gapFit))
                    DailyWeeklyToggle(onWeeklyClick = onWeeklyClick, modifier = Modifier.riseIn(delayMillis = 60))

                    Spacer(Modifier.height(17.1.rdp * gapFit))
                    CircleSummaryCard(
                        members = members,
                        onInviteClick = { openSheet(ZenCircleSheet.Invite) },
                        modifier = Modifier.riseIn(delayMillis = 120)
                    )

                    Spacer(Modifier.height(11.1.rdp * gapFit))
                    MemberCardStack(
                        members = members,
                        position = { position.value },
                        selected = selected,
                        onSendLove = onSendLove,
                        onSendMelt = onSendMelt,
                        modifier = swipe
                    )

                    Spacer(Modifier.height(25.3.rdp * gapFit))
                    RankingBand(members = members, ranks = ranks, selected = selected)

                    NameWheel(
                        members = members,
                        position = { position.value },
                        onPrevious = { step(-1) },
                        onNext = { step(1) },
                        height = wheelHeight,
                        modifier = swipe
                    )

                    Spacer(Modifier.height(11.4.rdp * gapFit))
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 30.3.rdp)
                            .riseIn(delayMillis = 420)
                    ) {
                        ZenCirclePillButton(
                            text = "Share & Invite to Zen Circle",
                            onClick = { openSheet(ZenCircleSheet.Invite) },
                            container = colorResource(R.color.zen_700),
                            content = Color.White,
                            height = 47.5.rdp,
                            fontSize = 17.76.rsp,
                            letterSpacing = (-0.36).sp
                        )
                        ZenCirclePillButton(
                            text = "Back to the Home",
                            onClick = onBackToHome,
                            container = Color.Transparent,
                            content = colorResource(R.color.gold_delta_text),
                            height = 47.5.rdp,
                            fontSize = 17.76.rsp,
                            letterSpacing = (-0.36).sp
                        )
                    }
                }
            }
        }
    }
}

// ── Toggle ────────────────────────────────────────────────────────

@Composable
private fun DailyWeeklyToggle(onWeeklyClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val wiggle = remember { Animatable(0f) }

    Row(
        modifier = modifier
            .width(258.5.rdp)
            .clip(CircleShape)
            .border(0.9.dp, colorResource(R.color.toggle_border_green), CircleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(134.23.rdp)
                .height(33.56.rdp)
                .clip(CircleShape)
                .background(Color.White)
                .border(0.9.dp, colorResource(R.color.toggle_pill_border), CircleShape)
                .semantics { contentDescription = "Daily ranking, selected" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Daily",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 15.94.rsp,
                letterSpacing = (-0.32).sp,
                color = Color.Black
            )
        }
        // Weekly is a PRO view — tapping it wiggles the badge instead of switching.
        Box(
            modifier = Modifier
                .width(107.93.rdp)
                .height(33.56.rdp)
                .clip(CircleShape)
                .clickable(onClickLabel = "Weekly ranking (PRO)") {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        for (angle in listOf(-14f, 12f, -8f, 5f, 0f)) wiggle.animateTo(angle, tween(55))
                    }
                    onWeeklyClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Weekly",
                fontFamily = Geist,
                fontSize = 15.94.rsp,
                letterSpacing = (-0.32).sp,
                color = colorResource(R.color.toggle_inactive_text)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 80.72.rdp, y = 8.16.rdp)
                    .graphicsLayer { rotationZ = wiggle.value }
                    .clip(CircleShape)
                    .background(colorResource(R.color.gold_delta_text))
                    .padding(horizontal = 3.2.rdp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PRO",
                    fontFamily = Geist,
                    fontSize = 5.7.rsp,
                    lineHeight = 9.07.rsp,
                    letterSpacing = (-0.11).sp,
                    color = Color.White
                )
            }
        }
    }
}

// ── Circle summary ────────────────────────────────────────────────

@Composable
private fun CircleSummaryCard(
    members: List<ZenCircleMember>,
    onInviteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val radius = 15.16.rdp
    Box(
        modifier = modifier
            .padding(horizontal = 32.34.rdp)
            .fillMaxWidth()
            .height(69.73.rdp)
            .topAccentBorder(colorResource(R.color.zen_circle_card_rule), 1.dp, radius)
            .clip(RoundedCornerShape(radius))
    ) {
        Row(
            modifier = Modifier.offset(x = 6.06.rdp, y = 10.11.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                // Circle tag = how many people are in it.
                text = String.format(Locale.US, "ZENCIR-%02d", members.size),
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 20.21.rsp,
                letterSpacing = (-0.2).sp,
                color = Color.Black,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.width(5.rdp))
            MemberAvatars(members)
        }
        Text(
            text = "RANKS RESET IN ${rememberTimeUntilMidnight()} HRS",
            fontFamily = DepartureMono,
            fontSize = 10.11.rsp,
            letterSpacing = (-0.61).sp,
            color = colorResource(R.color.zen_circle_reset_text),
            modifier = Modifier.offset(x = 6.06.rdp, y = 40.43.rdp)
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-5.rdp), y = 10.11.rdp)
                .width(68.69.rdp)
                .height(25.27.rdp)
                .clip(CircleShape)
                .background(colorResource(R.color.ink_surface))
                .clickable(onClick = onInviteClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_zen_circle_invite_plus),
                contentDescription = null,
                modifier = Modifier.size(7.1.rdp)
            )
            Spacer(Modifier.width(2.37.rdp))
            Text(
                text = "Invite",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 11.05.rsp,
                letterSpacing = (-0.22).sp,
                color = Color.White
            )
        }
    }
}

/** Overlapping initials — there are no profile photos to show yet. */
@Composable
private fun MemberAvatars(members: List<ZenCircleMember>) {
    val fills = listOf(colorResource(R.color.zen_700), colorResource(R.color.zen_circle_bro_dark))
    Row(modifier = Modifier.graphicsLayer { alpha = 0.76f }) {
        members.take(4).forEachIndexed { i, member ->
            Box(
                modifier = Modifier
                    .offset(x = (-4.04 * i).rdp)
                    .size(24.26.rdp)
                    .clip(CircleShape)
                    .background(fills[i % fills.size])
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.firstOrNull()?.uppercase() ?: "?",
                    fontFamily = Geist,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.rsp,
                    color = Color.White
                )
            }
        }
    }
}

/** "HH:MM" until local midnight, when the daily ranking resets. Ticks every 30s. */
@Composable
private fun rememberTimeUntilMidnight(): String {
    fun compute(): String {
        val now = Calendar.getInstance()
        val midnight = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val minutes = (midnight.timeInMillis - now.timeInMillis) / 60_000
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
    }
    var text by remember { mutableStateOf(compute()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            text = compute()
        }
    }
    return text
}

// ── Card stack ────────────────────────────────────────────────────

@Composable
private fun MemberCardStack(
    members: List<ZenCircleMember>,
    position: () -> Float,
    selected: Int,
    onSendLove: (ZenCircleMember) -> Unit,
    onSendMelt: (ZenCircleMember) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = members.size
    val density = LocalDensity.current
    val frontTop = with(density) { 52.2.rdp.toPx() }
    val backTop = with(density) { 26.rdp.toPx() }
    val swingPx = with(density) { 70.dp.toPx() }
    val selectedMember = members[selected]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(281.rdp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_zen_circle_watermark),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(281.13.rdp)
        )

        members.forEachIndexed { index, member ->
            // Signed distance from the front, wrapped so the stack loops.
            val depthState = remember(count) {
                derivedStateOf { wrapOffset(index - position(), count) }
            }
            val d by depthState
            val depth = abs(d).coerceAtMost(1f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f - abs(d))
                    .graphicsLayer {
                        val scale = FrontCardScale + (BackCardScale - FrontCardScale) * depth
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        scaleX = scale
                        scaleY = scale
                        translationY = frontTop + (backTop - frontTop) * depth
                        // Mid-shuffle the cards swing apart sideways, then settle.
                        val swing = sin(PI.toFloat() * depth)
                        translationX = -sign(d) * swing * swingPx
                        rotationZ = -sign(d) * swing * 8f
                        alpha = if (abs(d) > 1.2f) 0f else 1f
                    }
                    .width(150.67.rdp)
            ) {
                MemberCard(member)
            }
        }

        val canReact = !selectedMember.isYou
        ReactionButton(
            emoji = "🫠",
            label = "Send a melt to ${selectedMember.name}",
            enabled = canReact,
            flight = ReactionFlight.Melt,
            towardsCentre = 1f,
            onClick = { onSendMelt(selectedMember) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 40.42.rdp, y = 121.3.rdp)
                // Above the front card (zIndex 1) so the flying emojis pass over it.
                .zIndex(2f)
        )
        ReactionButton(
            emoji = "🤍",
            label = "Send love to ${selectedMember.name}",
            enabled = canReact,
            flight = ReactionFlight.Float,
            towardsCentre = -1f,
            onClick = { onSendLove(selectedMember) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-40.3).rdp, y = 121.3.rdp)
                .zIndex(2f)
        )
    }
}

@Composable
private fun MemberCard(member: ZenCircleMember) {
    if (member.isYou) {
        MyScreenTimeCard(
            usage = DailyUsage(screenTimeInMillis = member.screenTimeMinutes * 60_000),
            yesterdayChangePercent = member.changePercent,
            zenScore = member.zenScore,
            streaks = member.streaks
        )
    } else {
        BuddyStatsCard(
            buddyStats = BuddyStats(screenTimeMins = member.screenTimeMinutes),
            zenScore = member.zenScore,
            streaks = member.streaks,
            showReactions = false
        )
    }
}

/** Burst style: how a reaction's emojis leave the button. */
private enum class ReactionFlight(val riseDp: ClosedFloatingPointRange<Float>, val durationMs: IntRange, val count: Int) {
    /** Light and floaty — hearts drift high. */
    Float(riseDp = 150f..240f, durationMs = 1_050..1_450, count = 8),
    /** Heavy — melts wobble up a shorter way, slower, and sag before they fade. */
    Melt(riseDp = 90f..150f, durationMs = 1_250..1_700, count = 6)
}

private data class EmojiParticle(
    val id: Long,
    val driftDp: Float,
    val riseDp: Float,
    val spinDegrees: Float,
    val wobblePhase: Float,
    val sizeScale: Float,
    val delayMs: Int,
    val durationMs: Int
)

/**
 * Outlined emoji button that sends a reaction. Every tap pops the button and releases a burst
 * of the emoji that flies up and drifts towards the card ([towardsCentre] = +1 drifts right,
 * -1 left), wobbling and spinning as it fades. Rapid taps stack bursts (capped).
 */
@Composable
private fun ReactionButton(
    emoji: String,
    label: String,
    enabled: Boolean,
    flight: ReactionFlight,
    towardsCentre: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val pop = remember { Animatable(1f) }
    val particles = remember { mutableStateListOf<EmojiParticle>() }
    var nextId by remember { mutableStateOf(0L) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(43.6.rdp)
                .height(36.33.rdp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    alpha = if (enabled) 1f else 0.4f
                }
                .clip(CircleShape)
                .border(0.73.dp, colorResource(R.color.zen_circle_reaction_border), CircleShape)
                .clickable(enabled = enabled, onClickLabel = label) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        pop.snapTo(0.78f)
                        pop.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
                    }
                    repeat(flight.count) { i ->
                        particles += EmojiParticle(
                            id = nextId++,
                            driftDp = towardsCentre * Random.nextFloat() * 70f + (Random.nextFloat() - 0.5f) * 50f,
                            riseDp = flight.riseDp.start + Random.nextFloat() * (flight.riseDp.endInclusive - flight.riseDp.start),
                            spinDegrees = (Random.nextFloat() - 0.5f) * 70f,
                            wobblePhase = Random.nextFloat() * 2f * PI.toFloat(),
                            sizeScale = 0.7f + Random.nextFloat() * 0.55f,
                            delayMs = i * 45 + Random.nextInt(40),
                            durationMs = Random.nextInt(flight.durationMs.first, flight.durationMs.last)
                        )
                    }
                    while (particles.size > 30) particles.removeAt(0)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 23.25.rsp, letterSpacing = (-1.63).sp)
        }

        // Drawn after the button so the burst starts on top of it.
        particles.forEach { particle ->
            key(particle.id) {
                FlyingEmoji(
                    emoji = emoji,
                    particle = particle,
                    melt = flight == ReactionFlight.Melt,
                    onFinished = { particles.remove(particle) }
                )
            }
        }
    }
}

@Composable
private fun FlyingEmoji(emoji: String, particle: EmojiParticle, melt: Boolean, onFinished: () -> Unit) {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(particle.delayMs.toLong())
        progress.animateTo(1f, tween(particle.durationMs, easing = LinearEasing))
        onFinished()
    }
    val risePx = with(density) { particle.riseDp.dp.toPx() }
    val driftPx = with(density) { particle.driftDp.dp.toPx() }
    val wobblePx = with(density) { (if (melt) 10f else 7f).dp.toPx() }

    Text(
        text = emoji,
        fontSize = 22.rsp,
        modifier = Modifier.graphicsLayer {
            val t = progress.value
            if (t <= 0f) {
                alpha = 0f
                return@graphicsLayer
            }
            // Fast launch, slow drift at the top.
            val lift = 1f - (1f - t) * (1f - t) * (1f - t)
            translationY = -risePx * lift + (if (melt) risePx * 0.18f * t * t else 0f)
            translationX = driftPx * lift + sin(t * 2.4f * PI.toFloat() + particle.wobblePhase) * wobblePx * t
            val pop = if (t < 0.14f) 0.35f + 0.9f * (t / 0.14f) else 1.25f - 0.4f * ((t - 0.14f) / 0.86f)
            scaleX = pop * particle.sizeScale * (if (melt) 1f + 0.12f * t else 1f)
            scaleY = pop * particle.sizeScale * (if (melt) 1f - 0.1f * t else 1f)
            rotationZ = particle.spinDegrees * t
            alpha = if (t > 0.62f) (1f - t) / 0.38f else 1f
        }
    )
}

// ── Ranking band ──────────────────────────────────────────────────

@Composable
private fun RankingBand(members: List<ZenCircleMember>, ranks: Map<Int, Int>, selected: Int) {
    val rule = colorResource(R.color.zen_circle_band_rule)
    val draw = rememberEntrance(delayMillis = 300, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow))

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.6.rdp)
                .graphicsLayer {
                    scaleX = draw.value
                    alpha = draw.value.coerceIn(0f, 1f)
                }
                .background(colorResource(R.color.zen_circle_band_bg))
                .drawBehind {
                    val stroke = 1.dp.toPx()
                    drawRect(rule, size = size.copy(height = stroke))
                    drawRect(rule, topLeft = Offset(0f, size.height - stroke), size = size.copy(height = stroke))
                }
        ) {
            BandColumn(label = "Ranking", labelWeight = FontWeight.Medium, selected = selected) { i ->
                val rank = ranks.getValue(i)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (rank == 1) {
                        Image(
                            painter = painterResource(R.drawable.ic_zen_circle_rank_up),
                            contentDescription = null,
                            modifier = Modifier.width(8.2.rdp).height(7.37.rdp)
                        )
                        Spacer(Modifier.width(1.5.rdp))
                    }
                    Text(
                        text = String.format(Locale.US, "#%02d", rank),
                        fontFamily = Geist,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.17.rsp,
                        letterSpacing = (-0.32).sp,
                        color = Color.Black
                    )
                }
            }
            BandColumn(label = "Zen Bro’s Name", labelWeight = FontWeight.Medium, selected = selected) { i ->
                Text(
                    text = members[i].name,
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.17.rsp,
                    letterSpacing = (-0.97).sp,
                    color = colorResource(R.color.zen_700),
                    maxLines = 1
                )
            }
            BandColumn(label = "Zen Score", labelWeight = FontWeight.Normal, selected = selected) { i ->
                Text(
                    text = String.format(Locale.US, "%.1f", members[i].zenScore / 10f),
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            0.29f to colorResource(R.color.zen_circle_score_green),
                            0.51f to colorResource(R.color.score_grad_mid),
                            0.71f to colorResource(R.color.score_orange)
                        )
                    ),
                    fontFamily = DepartureMono,
                    fontSize = 20.21.rsp,
                    letterSpacing = (-4.45).sp
                )
            }
        }

        // Crown for whoever's leading, hanging off the band's top-left (node 2026:2518).
        AnimatedVisibility(
            visible = ranks.getValue(selected) == 1 && draw.value > 0.6f,
            modifier = Modifier.offset(x = 13.rdp, y = (-26).rdp),
            enter = scaleIn(spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit = scaleOut(tween(140)) + fadeOut(tween(140))
        ) {
            Image(
                painter = painterResource(R.drawable.ic_crown_v3),
                contentDescription = "Leading today",
                modifier = Modifier
                    .width(34.rdp)
                    .height(29.5.rdp)
                    .graphicsLayer { rotationZ = -14.15f }
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BandColumn(
    label: String,
    labelWeight: FontWeight,
    selected: Int,
    value: @Composable (memberIndex: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(top = 9.09.rdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontFamily = Geist,
            fontWeight = labelWeight,
            fontSize = 14.15.rsp,
            letterSpacing = (-0.28).sp,
            color = colorResource(R.color.zen_circle_band_label),
            maxLines = 1
        )
        // Values roll vertically when the selection changes.
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically(tween(220)) { if (up) it else -it } + fadeIn(tween(220)))
                    .togetherWith(slideOutVertically(tween(180)) { if (up) -it else it } + fadeOut(tween(160)))
            },
            label = "rankingBandValue"
        ) { memberIndex ->
            value(memberIndex)
        }
    }
}

// ── Name wheel ────────────────────────────────────────────────────
// A clock dial along the dashed arc. Every member gets a needle pointing along its own
// radius; as a name swings to the top its needle grows into a tapered clock hand with a hub
// on the arc and its label grows with it, while the rest keep short ticks and small type.
// Needles, ticks and labels all read the live wheel position, so they morph under the finger.

/** 0..1 — how much a slot [distance] steps from the top takes on the "selected" look. */
private fun needleCloseness(distance: Float) = (1f - distance).coerceIn(0f, 1f)

/** Needle length in dp: short tick for others, long hand for the selected name. */
private fun needleLengthDp(closeness: Float) = 9f + 19f * closeness

/** Longest a label may run along its radius before it would reach the swipe hint. */
private const val WheelLabelMaxDp = 74

@Composable
private fun NameWheel(
    members: List<ZenCircleMember>,
    position: () -> Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val count = members.size
    val radius = 251.14.rdp
    val arcTop = 27.9.rdp
    val density = LocalDensity.current
    val radiusPx = with(density) { radius.toPx() }
    val arcTopPx = with(density) { arcTop.toPx() }
    val green = colorResource(R.color.gold_delta_text)
    val muted = colorResource(R.color.zen_circle_wheel_name)

    // One-time nudge of the chevrons after the entrance, to hint that this swipes.
    val nudge = remember { Animatable(0f) }
    val inspection = LocalInspectionMode.current
    LaunchedEffect(Unit) {
        if (inspection) return@LaunchedEffect
        delay(1_300)
        repeat(2) {
            nudge.animateTo(1f, tween(170))
            nudge.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Image(
            painter = painterResource(R.drawable.bg_zen_circle_wheel_arc),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 0.5.dp, y = arcTop)
                // The circle is far taller than the wheel; hang it from the top and let it overflow.
                .wrapContentSize(Alignment.TopCenter, unbounded = true)
                .size(radius * 2)
                // Only the top of the circle belongs on screen (Figma's frame cuts the rest off);
                // on tall phones the bottom would otherwise reappear under the buttons.
                .drawWithContent { clipRect(bottom = 230.dp.toPx()) { this@drawWithContent.drawContent() } }
        )

        // Needles and minor ticks, drawn straight from the wheel position (no recomposition).
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val cx = size.width / 2f + 0.5.dp.toPx()
                    val cy = arcTopPx + radiusPx
                    val pos = position()
                    val first = floor(pos).toInt() - 3
                    for (major in first..first + 7) {
                        // Minor ticks: three between every pair of names.
                        for (quarter in 1..3) {
                            val d = major + quarter / 4f - pos
                            val angle = d * WheelStepDegrees
                            if (abs(angle) > 64f) continue
                            drawNeedle(
                                cx, cy, radiusPx, angle,
                                length = 5.dp.toPx(), baseWidth = 1.dp.toPx(),
                                color = muted.copy(alpha = 0.35f), hub = 0f
                            )
                        }
                        val d = major - pos
                        val angle = d * WheelStepDegrees
                        if (abs(angle) > 64f) continue
                        val c = needleCloseness(abs(d))
                        drawNeedle(
                            cx, cy, radiusPx, angle,
                            length = needleLengthDp(c).dp.toPx(),
                            baseWidth = (1.4f + 3.2f * c).dp.toPx(),
                            color = lerp(muted.copy(alpha = 0.6f), green, c),
                            hub = (1.3f + 3.2f * c).dp.toPx()
                        )
                    }
                }
        )

        // Names for the slots around the top; the list loops so neighbours repeat.
        val base = floor(position()).toInt()
        for (virtual in (base - 3)..(base + 3)) {
            val member = members[Math.floorMod(virtual, count)]
            WheelName(
                name = member.name,
                offset = { virtual - position() },
                radiusPx = radiusPx,
                arcTopPx = arcTopPx,
                selectedColor = green,
                mutedColor = muted
            )
        }

        Text(
            text = "Swipe Right or Left",
            fontFamily = Geist,
            fontWeight = FontWeight.Light,
            fontSize = 16.17.rsp,
            letterSpacing = (-0.97).sp,
            color = colorResource(R.color.zen_circle_swipe_hint),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = height - 34.1.rdp)
        )
        Chevron(mirrored = true, label = "Previous member", nudge = { nudge.value }, onClick = onPrevious, xFromCentre = (-93.5).rdp, y = height - 48.2.rdp)
        Chevron(mirrored = false, label = "Next member", nudge = { nudge.value }, onClick = onNext, xFromCentre = 95.5.rdp, y = height - 48.2.rdp)
    }
}

/**
 * A clock-hand needle standing on the arc at [angleDegrees] (0 = top), pointing towards the
 * dial centre: [baseWidth] wide at the arc, tapering to a point [length] in, with a round hub
 * of radius [hub] where it meets the arc.
 */
private fun DrawScope.drawNeedle(
    cx: Float,
    cy: Float,
    radiusPx: Float,
    angleDegrees: Float,
    length: Float,
    baseWidth: Float,
    color: Color,
    hub: Float
) {
    val a = angleDegrees * (PI.toFloat() / 180f)
    val dirX = sin(a)
    val dirY = -cos(a)
    // Outward unit vector is (dirX, dirY); the perpendicular is (-dirY, dirX).
    val baseX = cx + radiusPx * dirX
    val baseY = cy + radiusPx * dirY
    val tipX = cx + (radiusPx - length) * dirX
    val tipY = cy + (radiusPx - length) * dirY
    val half = baseWidth / 2f
    val tipHalf = (baseWidth * 0.12f).coerceAtLeast(0.35f)
    val path = Path().apply {
        moveTo(baseX - dirY * half, baseY + dirX * half)
        lineTo(tipX - dirY * tipHalf, tipY + dirX * tipHalf)
        lineTo(tipX + dirY * tipHalf, tipY - dirX * tipHalf)
        lineTo(baseX + dirY * half, baseY - dirX * half)
        close()
    }
    drawPath(path, color)
    if (hub > 0f) drawCircle(color, radius = hub, center = Offset(baseX, baseY))
}

@Composable
private fun BoxScope.WheelName(
    name: String,
    offset: () -> Float,
    radiusPx: Float,
    arcTopPx: Float,
    selectedColor: Color,
    mutedColor: Color
) {
    val density = LocalDensity.current
    val distance = abs(offset())
    if (distance > 2.6f) return

    val closeness = needleCloseness(distance)
    // Small type everywhere; only the name under the big needle grows.
    val fontSize = if (distance <= 1f) {
        lerp(10.5.rsp, 13.5.rsp, closeness)
    } else {
        lerp(10.rsp, 11.rsp, (2f - distance).coerceIn(0f, 1f))
    }
    Text(
        text = name,
        fontFamily = Geist,
        fontWeight = if (distance < 0.5f) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = fontSize,
        letterSpacing = (-0.2).sp,
        color = lerp(mutedColor, selectedColor, closeness),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .align(Alignment.TopStart)
            // Start the label just past its needle's tip, running along the same radius.
            .layout { measurable, constraints ->
                val maxLabel = with(density) { WheelLabelMaxDp.dp.roundToPx() }
                val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = maxLabel))
                layout(placeable.width, placeable.height) {
                    val o = offset()
                    val angle = o * WheelStepDegrees * (PI.toFloat() / 180f)
                    val needle = needleLengthDp(needleCloseness(abs(o)))
                    val inset = with(density) { (needle + 5f).dp.toPx() }
                    val centreX = constraints.maxWidth / 2f + with(density) { 0.5.dp.toPx() }
                    val centreY = arcTopPx + radiusPx
                    val px = centreX + (radiusPx - inset) * sin(angle)
                    val py = centreY - (radiusPx - inset) * cos(angle)
                    placeable.placeRelativeWithLayer(px.roundToInt(), (py - placeable.height / 2f).roundToInt()) {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationZ = 90f + o * WheelStepDegrees
                        alpha = if (abs(o) > 2f) (2.6f - abs(o)) / 0.6f else 1f
                    }
                }
            }
            // Far names blur (Figma 2.9px). Applied inside the rotation layer — as an outer
            // modifier the blur's own layer clipped the rotated text away entirely.
            .then(
                if (distance > 1.4f && SupportsBlur) {
                    Modifier.blur(2.93.dp * ((distance - 1.4f) / 0.6f).coerceIn(0f, 1f), BlurredEdgeTreatment.Unbounded)
                } else {
                    Modifier
                }
            )
    )
}

@Composable
private fun BoxScope.Chevron(
    mirrored: Boolean,
    label: String,
    nudge: () -> Float,
    onClick: () -> Unit,
    xFromCentre: Dp,
    y: Dp
) {
    val nudgePx = with(LocalDensity.current) { 6.dp.toPx() }
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(x = xFromCentre, y = y)
            .width(33.29.rdp)
            .height(48.21.rdp)
            .graphicsLayer { translationX = (if (mirrored) -1f else 1f) * nudge() * nudgePx }
            .clip(CircleShape)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_zen_circle_chevron),
            contentDescription = label,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = if (mirrored) -1f else 1f }
        )
    }
}

/** [raw] wrapped into (-count/2, count/2] so a looping list has a single nearest copy. */
private fun wrapOffset(raw: Float, count: Int): Float {
    if (count <= 1) return raw
    var r = raw % count
    if (r > count / 2f) r -= count
    if (r <= -count / 2f) r += count
    return r
}
