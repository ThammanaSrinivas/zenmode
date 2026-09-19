package com.zenlauncher.zenmode.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.zIndex
import com.zenlauncher.zenmode.ui.theme.rdp
import kotlin.math.abs
import kotlin.math.sin

/** One card in a [MemberCardFan]. Zen Score in tenths, see [com.zenlauncher.zenmode.ZenScore]. */
@Immutable
data class FanMember(
    val name: String,
    val screenTimeMinutes: Long,
    val zenScore: Int,
    val streaks: Int,
    val isYou: Boolean = false
) {
    val cardLabel: String get() = if (isYou) "My Time" else "$name's Time"
}

/**
 * Zen Circle members' mood cards fanned out in an arc, as in the Figma "Sharable"
 * leaderboard (node 74:8055). [ranked] is leader first; the leader sits in front at the
 * centre and the rest alternate outward. At most five cards are shown.
 *
 * [spread] (0 = stacked, 1 = fanned) and [floatPhase] (radians) let a caller animate the
 * fan opening and the cards drifting; pass 1f and 0f for a still image.
 */
@Composable
fun MemberCardFan(
    ranked: List<FanMember>,
    modifier: Modifier = Modifier,
    spread: Float = 1f,
    floatPhase: Float = 0f
) {
    val naturalCardWidth = 150.rdp
    // Slot 0 is the centre; leader first, then right/left alternately.
    val slots = listOf(0, 1, -1, 2, -2)
    val shown = ranked.take(slots.size).mapIndexed { i, member -> slots[i] to member }
        // Outer cards first so the leader, drawn last, sits on top.
        .sortedByDescending { abs(it.first) }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // Fewer people, bigger cards: two cards don't need to leave room for four more.
        val widthShare = when (shown.size) {
            1, 2 -> 0.4f
            3 -> 0.33f
            else -> 0.28f
        }
        val cardWidth = min(maxWidth * widthShare, maxHeight * 0.62f)
        val scale = cardWidth / naturalCardWidth
        // Keep the group centred when the slots in use are lopsided (two people: 0 and +1).
        val centreSlot = shown.map { it.first }.average().toFloat()
        shown.forEach { (slot, member) ->
            val bob = 4.dp * sin(floatPhase + slot * 0.9f)
            CircleMemberCard(
                label = member.cardLabel,
                screenTimeMinutes = member.screenTimeMinutes,
                zenScore = member.zenScore,
                streaks = member.streaks,
                isWinner = slot == 0,
                modifier = Modifier
                    .zIndex(3f - abs(slot))
                    .offset(
                        x = cardWidth * 0.74f * (slot - centreSlot) * spread,
                        y = cardWidth * (0.12f * slot * slot - 0.1f) * spread + bob
                    )
                    // Laid out at its natural size so the card's own type stays in
                    // proportion, then scaled down to fit the fan.
                    .requiredWidth(naturalCardWidth)
                    .graphicsLayer {
                        val s = scale * if (slot == 0) 1.12f else 0.96f
                        scaleX = s
                        scaleY = s
                        rotationZ = slot * 9f * spread
                        alpha = if (slot == 0) 1f else spread.coerceIn(0f, 1f)
                    }
            )
        }
    }
}
