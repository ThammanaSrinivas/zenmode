package com.zenlauncher.zenmode.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalInspectionMode
import com.zenlauncher.zenmode.ui.components.rememberReduceMotion
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.minDimension
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.SessionEventType
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.components.taperedBorder
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// Zen Score's session-log card and its reclaimed-minutes dial -- split out of
// ZenScoreScreen.kt to keep both under the line-count ceiling (scripts/check-line-limit.sh).

// ── Session log card ───────────────────────────────────────────────

/** Rows the free tier sees; also how many rows fit before the Pro list starts scrolling. */
private const val FREE_SESSION_LOG_LIMIT = 7
private val SessionLogRowHeight: Dp @Composable get() = 22.rdp
private val SessionLogListPadding: Dp @Composable get() = 6.rdp
/** Exactly [FREE_SESSION_LOG_LIMIT] rows tall, so a full free list never leaves a gap below it. */
private val SessionLogListMaxHeight: Dp @Composable
    get() = SessionLogRowHeight * FREE_SESSION_LOG_LIMIT + SessionLogListPadding * 2

@Composable
internal fun SessionLogCard(
    reclaimedMinutes: Int,
    sessionTotalLabel: String,
    sessionLog: List<ZenSessionLogEntry>,
    isPro: Boolean,
    onUpgradeProClick: () -> Unit
) {
    val colors = ZenTheme.colors
    val radius = 20.rdp
    // Free tier only ever sees today's most recent 7 sessions (list is already newest-first);
    // Pro sees everything. The list hugs its rows up to 7 of them, then scrolls internally
    // instead of growing with however many sessions the day produced.
    val visibleLog = if (isPro) sessionLog else sessionLog.take(FREE_SESSION_LOG_LIMIT)
    val hiddenCount = (sessionLog.size - FREE_SESSION_LOG_LIMIT).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = 3f
                cameraDistance = 24f * density
            }
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.verticalGradient(
                    listOf(ZenTheme.colors.bgMoodHappy, ZenTheme.colors.surfaceSunk)
                )
            )
            .taperedBorder(colorResource(R.color.zen_700), radius, top = 1.5.dp)
            .padding(horizontal = 18.rdp, vertical = 20.rdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TODAY'S SESSION LOG",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 12.rsp,
            letterSpacing = 1.2.sp,
            color = ZenTheme.colors.textBrandStrong,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.rdp))

        ReclaimedMinutesDial(minutes = reclaimedMinutes)

        Spacer(modifier = Modifier.height(16.rdp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.rdp))
                .background(colors.surfaceElevated.copy(alpha = 0.6f))
                .border(1.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(10.rdp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZenTheme.colors.borderSubtle)
                    .padding(horizontal = 10.rdp, vertical = 8.rdp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SESSION LOG",
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.rsp,
                    letterSpacing = 1.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = sessionTotalLabel,
                    fontFamily = DepartureMono,
                    fontSize = 11.rsp,
                    color = colors.textSecondary
                )
            }

            if (visibleLog.isEmpty()) {
                SessionLogEmptyRow()
            } else {
                // Capped, not fixed: a short day doesn't leave dead space under the rows, and a
                // long (Pro) day scrolls here rather than stretching the card. Newest first, so
                // nothing to scroll past to see the latest session.
                LazyColumn(
                    modifier = Modifier.heightIn(max = SessionLogListMaxHeight),
                    contentPadding = PaddingValues(vertical = SessionLogListPadding)
                ) {
                    items(visibleLog) { entry -> SessionLogRow(entry) }
                }
            }

            if (!isPro && hiddenCount > 0) {
                UpgradeForMoreSessionsRow(hiddenCount = hiddenCount, onClick = onUpgradeProClick)
            }
        }
    }
}

@Composable
private fun UpgradeForMoreSessionsRow(hiddenCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZenTheme.colors.borderSubtle)
            .pressScale(onClick = onClick, onClickLabel = "Upgrade to Pro to see all sessions")
            .padding(horizontal = 10.rdp, vertical = 8.rdp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "+$hiddenCount more session${if (hiddenCount == 1) "" else "s"} today",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 10.rsp,
            color = ZenTheme.colors.textSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Upgrade to see all",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.rsp,
                letterSpacing = (-0.1).sp,
                color = colorResource(R.color.zen_700)
            )
            Spacer(modifier = Modifier.width(6.rdp))
            ProBadge()
        }
    }
}

@Composable
private fun SessionLogEmptyRow() {
    Text(
        text = "No sessions yet today",
        fontFamily = Geist,
        fontSize = 11.rsp,
        color = ZenTheme.colors.textSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.rdp, vertical = 14.rdp)
    )
}

@Composable
private fun SessionLogRow(entry: ZenSessionLogEntry) {
    val colors = ZenTheme.colors
    val (statusLabel, statusColor) = when (entry.type) {
        SessionEventType.INTENTIONAL -> "INTENTIONAL" to colorResource(R.color.gold_delta_text)
        SessionEventType.ENTERTAINING -> "ENTERTAINING" to colorResource(R.color.score_status_red)
        SessionEventType.DISRUPTED -> "DISRUPTED" to colorResource(R.color.amber_500)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SessionLogRowHeight)
            .padding(horizontal = 10.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.duration,
            fontFamily = DepartureMono,
            fontSize = 10.rsp,
            lineHeight = 10.rsp,
            letterSpacing = 1.sp,
            maxLines = 1,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.width(10.rdp))
        // Takes the slack so a long app name ellipsizes instead of shoving the status off-card.
        Text(
            text = entry.appName.uppercase(),
            fontFamily = DepartureMono,
            fontSize = 10.rsp,
            lineHeight = 10.rsp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.rdp))
        Text(
            text = statusLabel,
            fontFamily = DepartureMono,
            fontSize = 9.rsp,
            lineHeight = 9.rsp,
            maxLines = 1,
            color = statusColor
        )
    }
}

// ── Reclaimed-minutes dial ─────────────────────────────────────────
// The session log's centrepiece (design: a ticked bezel round a thick two-tone ring round a
// glowing green disc). On arrival the ring fills and the bezel's minute ticks light up in step
// with it; afterwards a soft highlight keeps sweeping the lit ticks and the disc breathes, so
// the dial feels live without pulling focus. Counts the minutes up as it fills.

@Composable
private fun ReclaimedMinutesDial(minutes: Int, modifier: Modifier = Modifier) {
    val progress = (minutes / 2000f).coerceIn(0f, 1f)
    val fill = remember { Animatable(0f) }
    LaunchedEffect(progress) { fill.animateTo(progress, tween(1_300, easing = FastOutSlowInEasing)) }

    val still = rememberReduceMotion() || LocalInspectionMode.current
    val motion = rememberInfiniteTransition(label = "reclaimed")
    val scan by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_200, easing = LinearEasing)),
        label = "reclaimed-scan"
    )
    val breathe by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "reclaimed-breathe"
    )

    val tickLit = colorResource(R.color.zen_700)
    val tickDim = colorResource(R.color.stone_300).copy(alpha = 0.45f)
    val ringTrack = colorResource(R.color.stone_300).copy(alpha = 0.55f)
    val ringFill = colorResource(R.color.zen_700)
    val ringFillDeep = colorResource(R.color.zen_900)
    val discCore = colorResource(R.color.zen_200)
    val discEdge = colorResource(R.color.zen_100)
    val ink = colorResource(R.color.ink_base)
    val shown = (minutes * fill.value / progress.coerceAtLeast(0.0001f)).toInt().coerceAtMost(minutes)

    Box(
        modifier = modifier
            .size(168.rdp)
            .semantics(mergeDescendants = true) { contentDescription = "Reclaimed today: $minutes minutes" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val ticks = 60
            val litTicks = ticks * fill.value

            // Bezel: 60 minute ticks, every fifth one longer.
            for (i in 0 until ticks) {
                val major = i % 5 == 0
                val t = i / ticks.toFloat()
                val on = i < litTicks
                // The sweep highlight trails off behind a moving head.
                val trail = (((scan - t) % 1f) + 1f) % 1f
                val glow = if (on && !still) (1f - trail / 0.25f).coerceIn(0f, 1f) else 0f
                val color = if (on) lerp(tickLit, discEdge, glow * 0.8f) else tickDim
                rotate(degrees = t * 360f, pivot = center) {
                    drawLine(
                        color = color,
                        start = Offset(center.x, center.y - r * 0.98f),
                        end = Offset(center.x, center.y - r * (if (major) 0.86f else 0.9f)),
                        strokeWidth = r * (if (major) 0.03f else 0.018f),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Two-tone ring with a soft drop shadow underneath.
            val ringR = r * 0.7f
            val ringW = r * 0.16f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent),
                    center = center.copy(y = center.y + r * 0.05f),
                    radius = ringR + ringW
                ),
                radius = ringR + ringW,
                center = center.copy(y = center.y + r * 0.05f)
            )
            val arcTopLeft = Offset(center.x - ringR, center.y - ringR)
            val arcSize = Size(ringR * 2, ringR * 2)
            drawArc(ringTrack, -90f, 360f, false, arcTopLeft, arcSize, style = Stroke(width = ringW))
            if (fill.value > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(ringFillDeep, ringFill, ringFillDeep), center),
                    startAngle = -90f,
                    sweepAngle = 360f * fill.value,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = ringW, cap = StrokeCap.Round)
                )
            }

            // Glowing disc: light core, inner shadow at the rim, breathing halo.
            val discR = ringR - ringW / 2f - r * 0.02f
            drawCircle(
                color = discEdge.copy(alpha = 0.35f + 0.25f * (if (still) 0.5f else breathe)),
                radius = discR + r * 0.03f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(discCore, discEdge), center, discR),
                radius = discR,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0.78f to Color.Transparent,
                    1f to ringFillDeep.copy(alpha = 0.18f),
                    center = center,
                    radius = discR
                ),
                radius = discR,
                center = center
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Reclaimed Today",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 9.rsp,
                color = ink.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "%,d".format(shown),
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 28.rsp,
                letterSpacing = (-0.84).sp,
                color = ink
            )
            Text(
                text = "Minutes",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 9.rsp,
                color = colorResource(R.color.gold_delta_text)
            )
        }
    }
}
