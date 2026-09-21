package com.zenlauncher.zenmode.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.zenlauncher.zenmode.HomeStackMember
import com.zenlauncher.zenmode.ui.screens.wrapOffset
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/**
 * Home's right-hand stats card, queued the same way the Zen Circle dashboard's
 * `MemberCardStack` (ZenCircleScreen.kt) queues its cards -- peeking behind at reduced scale,
 * offset down, faded -- reusing [wrapOffset] rather than re-deriving the same loop math. Only
 * meaningful with 2+ members; StatsCardsRow only renders this when there's something to queue,
 * otherwise it keeps the plain single [BuddyStatsCard].
 */
@Composable
fun HomeBuddyCardStack(
    members: List<HomeStackMember>,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val count = members.size
    val density = LocalDensity.current
    val stepPx = with(density) { 40.dp.toPx() }
    val scope = rememberCoroutineScope()
    val position = remember(count) { Animatable(0f) }
    var dragOrigin by remember { mutableStateOf(0f) }

    fun settle(target: Float) {
        scope.launch { position.animateTo(target, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
    }

    Box(
        modifier = modifier
            // Quick tap opens the Zen Circle dashboard -- same behavior as the plain card.
            .clickable(onClickLabel = "Open Zen Circle", onClick = onTap)
            // A drag past touch slop is claimed and consumed by this (inner) node before
            // Home's outer page-swipe (Zen Score / Zen Gold, registered on HomeScreen's
            // top-level Box) ever sees it -- Compose's main pointer pass dispatches
            // child-first, the same reason every other clickable already nested inside that
            // outer Box (header, gold row, app icons) doesn't misfire the page swipe today.
            .draggable(
                orientation = Orientation.Horizontal,
                enabled = count > 1,
                state = rememberDraggableState { delta ->
                    scope.launch {
                        position.snapTo((position.value - delta / stepPx).coerceIn(dragOrigin - 1f, dragOrigin + 1f))
                    }
                },
                // One member per swipe, however far it travels -- same clamp reasoning as
                // MemberCardStack: a long swipe shouldn't lap the loop.
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
    ) {
        members.forEachIndexed { index, member ->
            val d = wrapOffset(index - position.value, count)
            val depth = abs(d).coerceAtMost(1f)
            Box(
                modifier = Modifier
                    .zIndex(1f - depth)
                    .graphicsLayer {
                        val scale = 1f - 0.12f * depth
                        scaleX = scale
                        scaleY = scale
                        // A visibly peeking sliver below the front card -- at Home's compact
                        // card size (much smaller than the dashboard's dedicated stage), the
                        // dashboard's own 26dp gap would read as barely-there, so this is scaled
                        // up for legibility rather than reused verbatim.
                        translationY = 14.dp.toPx() * depth
                        rotationZ = -sign(d) * sin(PI.toFloat() * depth) * 4f
                        // Continuous fade -- the dashboard's own stack only cuts opacity at a
                        // hard threshold past a member; this is the one deliberate visual
                        // divergence from it, so the queued cards read as receding, not just
                        // shrinking in place.
                        alpha = 1f - 0.65f * depth
                    }
            ) {
                BuddyStatsCard(
                    buddyStats = member.stats,
                    zenScore = member.zenScore,
                    streaks = 0,
                    // Home has never shown reaction badges (StatsCardsRow's showReactions=false
                    // for Home today) -- unchanged, out of scope here.
                    showReactions = false
                )
            }
        }
    }
}
