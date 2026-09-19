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
    val changePercent: Int? = null,
    /** Empty for the classic-buddy reskin (no real circle, nothing to react into yet);
     * a real Firebase Auth UID once backed by an actual Circle. Needed to target
     * reactions at a specific member -- see ReactionButton's onSendLove/onSendMelt. */
    val uid: String = ""
)

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
    val selected by remember(count) { derivedStateOf { Math.floorMod(position.value.roundToInt(), count) } }
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
