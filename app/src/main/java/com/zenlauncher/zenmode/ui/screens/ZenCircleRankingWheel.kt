package com.zenlauncher.zenmode.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.ZenScore
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

// Split out of ZenCircleScreen.kt to stay under the line-count ceiling. The ranking band and
// name wheel are the circle dashboard's rank/name/score readout and the clock-dial carousel
// underneath it — a cohesive visual subsystem driven by the shared wheel `position`/`selected`
// state that ZenCircleScreen.kt owns and passes in.

/** Degrees between neighbouring names on the wheel (Figma: ~94dp of arc on a 251dp radius). */
private const val WheelStepDegrees = 21.5f

/** Minute-mark divisions between neighbouring names (5 marks per gap). Even, so one sits halfway. */
private const val MinutesPerStep = 6

// ── Ranking band ──────────────────────────────────────────────────

@Composable
fun RankingBand(members: List<ZenCircleMember>, ranks: Map<Int, Int>, selected: Int) {
    val rule = colorResource(R.color.zen_circle_band_rule)
    val draw = rememberEntrance(delayMillis = 300, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow))

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.6.rdp)
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
                        color = ZenTheme.colors.textPrimary
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
                    color = ZenTheme.colors.textBrand,
                    maxLines = 1
                )
            }
            BandColumn(label = "Zen Score", labelWeight = FontWeight.Normal, selected = selected) { i ->
                Text(
                    text = ZenScore.format(members[i].zenScore),
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
            // Room under the value too, so the name card doesn't sit on the needle below it.
            .padding(top = 9.09.rdp, bottom = 10.rdp),
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
fun NameWheel(
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
    val green = ZenTheme.colors.textBrand
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
                        // Minute marks, like a watch face: five between every pair of names,
                        // with the middle one a touch longer so the dial reads in halves.
                        for (minute in 1 until MinutesPerStep) {
                            val d = major + minute / MinutesPerStep.toFloat() - pos
                            val angle = d * WheelStepDegrees
                            if (abs(angle) > 64f) continue
                            val half = minute * 2 == MinutesPerStep
                            drawNeedle(
                                cx, cy, radiusPx, angle,
                                length = (if (half) 7.5f else 5.5f).dp.toPx(),
                                baseWidth = (if (half) 1.3f else 1.1f).dp.toPx(),
                                color = muted.copy(alpha = if (half) 0.6f else 0.45f), hub = 0f
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
                    val inset = with(density) { (needle + 7f).dp.toPx() }
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
fun wrapOffset(raw: Float, count: Int): Float {
    if (count <= 1) return raw
    var r = raw % count
    if (r > count / 2f) r -= count
    if (r <= -count / 2f) r += count
    return r
}
