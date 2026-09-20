package com.zenlauncher.zenmode.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.MoodBackdrop
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.components.StatsCardsRow
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Zen Bro connect, screen 3 ─────────────────────────────────────
// "You're Zen Bros now" — shown right after a random or code connect succeeds
// (Figma node 2026:2346). Choreography, top to bottom:
//   0ms    two circles slide in from the sides and merge, a ring pulses out
//   ~250ms headline, line, then the three shared-stat tags pop in
//   ~550ms the two stat cards swing in from their edges once the buddy's stats are in
//          (or after BuddyStatsWaitMillis); the bolt snaps on landing (+ haptic)
//   then   the Zen Circle panel rises, always after the cards so it never lands into a gap
// Screenshot tests / previews render the settled frame.

/** How long to wait for the buddy's stats before animating the cards in anyway. */
private const val BuddyStatsWaitMillis = 800L

@Composable
fun ZenBroConnectedScreen(
    buddyName: String,
    usage: DailyUsage?,
    streaks: Int,
    zenScore: Int,
    buddyStats: BuddyStats?,
    userCode: String?,
    onBackClick: () -> Unit,
    onShareInviteLink: () -> Unit,
    onCopyInviteCode: () -> Unit,
    onMaybeLater: () -> Unit,
    initialInviteSheetOpen: Boolean = false
) {
    BackHandler(onBack = onBackClick)

    val inspection = LocalInspectionMode.current
    var cardsEntering by remember { mutableStateOf(inspection) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures() }
    ) {
        MoodBackdrop()

        // "Invite people" opens the Zen Circle invite sheet over this page (Figma node 2026:2383).
        ZenCircleSheetHost(
            userCode = userCode,
            onShareInviteLink = onShareInviteLink,
            onCopyInviteCode = onCopyInviteCode,
            initialSheet = if (initialInviteSheetOpen) ZenCircleSheet.Invite else null
        ) { pageModifier, openSheet ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .then(pageModifier)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.rdp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(33.rdp))
                ZenCircleHeader(onBackClick = onBackClick)

                Spacer(Modifier.height(35.rdp))
                MergingCircles()

                Spacer(Modifier.height(18.rdp))
                Headline(buddyName = buddyName)

                Spacer(Modifier.height(22.rdp))
                ConnectedCards(
                    usage = usage,
                    streaks = streaks,
                    zenScore = zenScore,
                    buddyStats = buddyStats,
                    onEntranceStart = { cardsEntering = true }
                )

                Spacer(Modifier.height(40.rdp))
                ZenCirclePanel(
                    visible = cardsEntering,
                    onInvitePeople = { openSheet(ZenCircleSheet.Invite) },
                    onMaybeLater = onMaybeLater
                )
            }
        }
    }
}

// ── Circles ───────────────────────────────────────────────────────

@Composable
private fun MergingCircles() {
    val diameter = 65.7.rdp
    // Settled centres are 47.7dp apart (Figma 2085:1061); they start 70dp further out.
    val settledHalfGap = 23.85.rdp
    val startExtra = 70.dp
    val extraPx = with(LocalDensity.current) { startExtra.toPx() }

    val merge = rememberEntrance(delayMillis = 0, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
    val ring = rememberEntrance(delayMillis = 380, tween(durationMillis = 900, easing = FastOutSlowInEasing))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(diameter),
        contentAlignment = Alignment.Center
    ) {
        // One ring that grows and fades from the overlap — the "click" of connecting.
        Box(
            modifier = Modifier
                .size(diameter)
                .graphicsLayer {
                    val p = ring.value
                    scaleX = 1f + p * 1.3f
                    scaleY = 1f + p * 1.3f
                    alpha = if (p <= 0f || p >= 1f) 0f else (1f - p) * 0.55f
                }
                .border(2.dp, colorResource(R.color.gold_delta_text), CircleShape)
        )
        Circle(
            color = colorResource(R.color.zen_circle_bro_dark),
            diameter = diameter,
            offsetX = -settledHalfGap,
            enterPx = -extraPx,
            progress = { merge.value }
        )
        Circle(
            color = colorResource(R.color.gold_delta_text),
            diameter = diameter,
            offsetX = settledHalfGap,
            enterPx = extraPx,
            progress = { merge.value }
        )
    }
}

@Composable
private fun Circle(color: Color, diameter: Dp, offsetX: Dp, enterPx: Float, progress: () -> Float) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .size(diameter)
            .graphicsLayer {
                // Read inside the layer so the slide doesn't recompose every frame.
                val p = progress()
                translationX = with(density) { offsetX.toPx() } + (1f - p) * enterPx
                alpha = p.coerceIn(0f, 1f)
            }
            .clip(CircleShape)
            .background(color)
    )
}

// ── Headline ──────────────────────────────────────────────────────

@Composable
private fun Headline(buddyName: String) {
    Column(
        modifier = Modifier.padding(horizontal = 38.rdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "You’re Zen Bros now",
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 32.3.rsp,
            letterSpacing = (-0.65).sp,
            color = ZenTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .semantics { heading() }
                .riseIn(delayMillis = 250)
        )
        Spacer(Modifier.height(7.rdp))
        Text(
            text = "You and $buddyName now see the same three things about each other.",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 17.3.rsp,
            lineHeight = 19.2.rsp,
            color = colorResource(R.color.zen_circle_connected_body),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 15.rdp)
                .riseIn(delayMillis = 330)
        )
        Spacer(Modifier.height(10.rdp))
        // The caption becomes three tags that pop in one by one — each is a thing you now share.
        Row(horizontalArrangement = Arrangement.spacedBy(6.rdp)) {
            listOf("Screen time", "Zen Score", "Streaks").forEachIndexed { i, label ->
                SharedStatTag(label = label, delayMillis = 430 + i * 80)
            }
        }
    }
}

@Composable
private fun SharedStatTag(label: String, delayMillis: Int) {
    val pop = rememberEntrance(delayMillis, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
    Text(
        text = label,
        fontFamily = Geist,
        fontSize = 13.8.rsp,
        color = colorResource(R.color.zen_circle_connected_caption),
        modifier = Modifier
            .graphicsLayer {
                val p = pop.value
                scaleX = 0.6f + 0.4f * p
                scaleY = 0.6f + 0.4f * p
                alpha = p.coerceIn(0f, 1f)
            }
            .clip(CircleShape)
            .background(ZenTheme.colors.surfaceElevated.copy(alpha = 0.6f))
            .padding(horizontal = 10.rdp, vertical = 3.rdp)
    )
}

// ── Cards ─────────────────────────────────────────────────────────

@Composable
private fun ConnectedCards(
    usage: DailyUsage?,
    streaks: Int,
    zenScore: Int,
    buddyStats: BuddyStats?,
    onEntranceStart: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val inspection = LocalInspectionMode.current
    val density = LocalDensity.current
    val swing = with(density) { 90.dp.toPx() }

    // Hold the entrance until the buddy's stats arrive (fetched right after connecting),
    // so the crown and faces don't visibly flip a moment after the cards land.
    var ready by remember { mutableStateOf(inspection || buddyStats != null) }
    LaunchedEffect(buddyStats) {
        if (buddyStats != null) ready = true
    }
    LaunchedEffect(Unit) {
        delay(BuddyStatsWaitMillis)
        ready = true
    }

    val cards = remember { Animatable(if (inspection) 1f else 0f) }
    val bolt = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(ready) {
        if (!ready || cards.value == 1f) return@LaunchedEffect
        delay(150)
        onEntranceStart()
        launch {
            // Snap the bolt in as the cards meet.
            delay(260)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            bolt.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
        cards.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
    }

    StatsCardsRow(
        usage = usage,
        yesterdayChangePercent = null,
        hasBuddies = true,
        // Until the real numbers land, the card reads 00:00 rather than an invite placeholder.
        buddyStats = buddyStats ?: BuddyStats(screenTimeMins = 0),
        isSignedIn = true,
        zenScore = zenScore,
        streaks = streaks,
        showReactions = false,
        leftCardModifier = Modifier.graphicsLayer {
            val p = cards.value
            translationX = -(1f - p) * swing
            rotationZ = -(1f - p) * 10f
            alpha = p.coerceIn(0f, 1f)
        },
        rightCardModifier = Modifier.graphicsLayer {
            val p = cards.value
            translationX = (1f - p) * swing
            rotationZ = (1f - p) * 10f
            alpha = p.coerceIn(0f, 1f)
        },
        boltModifier = Modifier.graphicsLayer {
            val p = bolt.value
            scaleX = p
            scaleY = p
            rotationZ = (1f - p) * -40f
            alpha = p.coerceIn(0f, 1f)
        },
        modifier = Modifier.padding(horizontal = 30.rdp)
    )
}

// ── Zen Circle panel ──────────────────────────────────────────────

@Composable
private fun ZenCirclePanel(visible: Boolean, onInvitePeople: () -> Unit, onMaybeLater: () -> Unit) {
    val brandGreen = colorResource(R.color.gold_delta_text)

    Column(
        modifier = Modifier
            .padding(horizontal = 29.5.rdp)
            .fillMaxWidth()
            .riseIn(delayMillis = 420, rise = 40.dp, start = visible)
            .clip(RoundedCornerShape(20.7.rdp))
            .background(ZenTheme.colors.bgMoodHappy)
            .padding(horizontal = 20.7.rdp, vertical = 16.rdp)
    ) {
        Text(
            text = "Your Zen Circle starts here",
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 24.9.rsp,
            letterSpacing = (-0.5).sp,
            color = ZenTheme.colors.textPrimary,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(12.rdp))
        Text(
            text = "Invite up to 7 people you trust to keep each other going.",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 16.2.rsp,
            lineHeight = 20.rsp,
            letterSpacing = (-0.32).sp,
            color = colorResource(R.color.zen_circle_connected_body)
        )
        Spacer(Modifier.height(16.rdp))
        ZenCirclePillButton(
            text = "Invite people",
            onClick = onInvitePeople,
            container = colorResource(R.color.zen_700),
            content = Color.White
        )
        Spacer(Modifier.height(7.rdp))
        ZenCirclePillButton(
            text = "Maybe later",
            onClick = onMaybeLater,
            container = Color.Transparent,
            content = brandGreen
        )
    }
}

// ── Motion helpers ────────────────────────────────────────────────

/** A 0→1 progress that starts after [delayMillis]; settled immediately in previews/screenshots. */
@Composable
internal fun rememberEntrance(
    delayMillis: Int,
    spec: androidx.compose.animation.core.AnimationSpec<Float>,
    start: Boolean = true
): Animatable<Float, *> {
    val inspection = LocalInspectionMode.current
    val progress = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(start) {
        if (!start || progress.value == 1f) return@LaunchedEffect
        delay(delayMillis.toLong())
        progress.animateTo(1f, spec)
    }
    return progress
}

/** Fades in while rising [rise] into place, [delayMillis] after [start] turns true. */
@Composable
internal fun Modifier.riseIn(delayMillis: Int, rise: Dp = 16.dp, start: Boolean = true): Modifier {
    val p = rememberEntrance(delayMillis, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow), start)
    val risePx = with(LocalDensity.current) { rise.toPx() }
    return graphicsLayer {
        alpha = p.value.coerceIn(0f, 1f)
        translationY = (1f - p.value) * risePx
    }
}
