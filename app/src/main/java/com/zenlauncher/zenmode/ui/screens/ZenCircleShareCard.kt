package com.zenlauncher.zenmode.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.ZenScore
import com.zenlauncher.zenmode.ui.components.FanMember
import com.zenlauncher.zenmode.ui.components.MemberCardFan
import com.zenlauncher.zenmode.ui.components.ZenModeOsWordmark
import com.zenlauncher.zenmode.ui.components.rememberBrandOsGradient
import com.zenlauncher.zenmode.ui.components.saveImageToPictures
import com.zenlauncher.zenmode.ui.components.shareImage
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.LightOnly
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── My Zen Circle: shareable leaderboard card ─────────────────────
// Figma "Sharable" (node 74:8055): today's circle as an image to post. The members'
// cards fan out with the leader in front, the ranking strip names the leader, the
// dashed arc carries everyone's names, and the footer invites people to start their own.
// Always light (see LightOnly): it's an image that leaves the app, so it doesn't follow dark mode.

/** Fixed logical width so the exported image looks the same on every phone. */
private val ShareCardWidth = 360.dp

@Composable
fun ZenCircleShareCard(
    members: List<ZenCircleMember>,
    ranks: Map<Int, Int>,
    modifier: Modifier = Modifier
) {
    LightOnly { ShareCardContent(members, ranks, modifier) }
}

@Composable
private fun ShareCardContent(members: List<ZenCircleMember>, ranks: Map<Int, Int>, modifier: Modifier) {
    val ranked = remember(members, ranks) {
        members.indices.sortedBy { ranks[it] ?: Int.MAX_VALUE }.map { members[it] }
    }
    val leader = ranked.first()
    val green = colorResource(R.color.gold_delta_text)
    val ink = colorResource(R.color.ink_surface)
    val rule = colorResource(R.color.zen_circle_card_rule)
    val date = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

    Column(
        modifier = modifier
            .requiredWidth(ShareCardWidth)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorResource(R.color.wash_neutral_core),
                        colorResource(R.color.wash_neutral_edge),
                        colorResource(R.color.wash_neutral_edge)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_zen_circle_mark),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Zencircle daily leaderboard",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                letterSpacing = (-0.4).sp,
                color = Color.Black,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(text = date, fontFamily = Geist, fontSize = 11.sp, color = green)
        }

        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(rule))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(Locale.US, "ZENCIR-%02d", members.size),
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.Medium,
                        fontSize = 19.sp,
                        color = Color.Black
                    )
                    Spacer(Modifier.width(6.dp))
                    MemberAvatars(members)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "RANKS RESET IN ${rememberTimeUntilMidnight()} HRS",
                    fontFamily = DepartureMono,
                    fontSize = 9.5.sp,
                    color = colorResource(R.color.zen_circle_reset_text)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "%02d MEMBERS", members.size),
                    fontFamily = DepartureMono,
                    fontSize = 15.sp,
                    color = green
                )
                Text(text = "Member count", fontFamily = Geist, fontSize = 12.sp, color = ink)
            }
        }

        MemberCardFan(
            ranked = ranked.map { FanMember(it.name, it.screenTimeMinutes, it.zenScore, it.streaks, it.isYou) },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )

        LeaderStrip(leader = leader, green = green, rule = rule)

        NameArc(
            names = ranked.map { arcName(it) },
            green = green,
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
        )

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "FROM", fontFamily = Geist, fontSize = 11.sp, letterSpacing = 1.sp, color = colorResource(R.color.stone_600))
            Spacer(Modifier.height(4.dp))
            ZenModeOsWordmark(fontSize = 20.sp, markSize = 22.dp, markGap = 6.dp)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Create your Zen Circle, with your loved ones! #ZenTogether",
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.3).sp,
            color = green
        )
    }
}

/** "Ranking ▲#01 · Zen Bro's name · Zen Score 9.1" for today's leader. */
@Composable
private fun LeaderStrip(leader: ZenCircleMember, green: Color, rule: Color) {
    val label = TextStyle(fontFamily = Geist, fontSize = 12.sp, color = colorResource(R.color.stone_600))
    val value = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = green)
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(rule))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.35f))
                .padding(vertical = 8.dp)
        ) {
            listOf(
                "Ranking" to "▲ #01",
                "Zen Bro's name" to arcName(leader)
            ).forEach { (title, text) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, style = label, maxLines = 1)
                    Text(text, style = value, maxLines = 1)
                }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Zen Score", style = label, maxLines = 1)
                Text(
                    ZenScore.format(leader.zenScore),
                    style = TextStyle(fontFamily = DepartureMono, fontSize = 17.sp, brush = rememberBrandOsGradient())
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(rule))
    }
}

/** First name only, capped, so a long name can't run off the arc into the footer. */
private fun arcName(member: ZenCircleMember): String {
    if (member.isYou) return "You"
    val first = member.name.trim().substringBefore(' ').lowercase().replaceFirstChar { it.titlecase() }
    return if (first.length > 10) first.take(9) + "…" else first
}

/** The dashed arc with everyone's names on it, the leader's upright under an arrow. */
@Composable
private fun NameArc(names: List<String>, green: Color, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val muted = colorResource(R.color.zen_circle_reset_text)
    Canvas(modifier = modifier.semantics { contentDescription = "Circle members: ${names.joinToString()}" }) {
        val radius = size.width * 0.62f
        val center = Offset(size.width / 2, radius + 14.dp.toPx())
        drawArc(
            color = green.copy(alpha = 0.7f),
            startAngle = 212f,
            sweepAngle = 116f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 9f)))
        )
        // Leader at the top, the rest alternating outward, 22° apart.
        names.take(5).forEachIndexed { i, name ->
            val side = if (i % 2 == 1) -1 else 1
            val angle = ((i + 1) / 2) * 22f * side
            val leader = i == 0
            val layout = measurer.measure(
                "-$name",
                TextStyle(
                    fontFamily = Geist,
                    fontWeight = if (leader) FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (leader) 15.sp else 12.sp,
                    color = if (leader) green else muted
                )
            )
            rotate(angle, pivot = center) {
                val top = center.y - radius + 10.dp.toPx() + if (leader) 12.dp.toPx() else 0f
                // Reads downward from the arc, like the Figma wheel.
                rotate(90f, pivot = Offset(center.x, top)) {
                    drawText(layout, topLeft = Offset(center.x, top - layout.size.height / 2f))
                }
                if (leader) {
                    val tip = center.y - radius + 2.dp.toPx()
                    drawLine(green, Offset(center.x, tip + 12.dp.toPx()), Offset(center.x, tip), strokeWidth = 2.dp.toPx())
                    drawLine(green, Offset(center.x, tip), Offset(center.x - 4.dp.toPx(), tip + 5.dp.toPx()), strokeWidth = 2.dp.toPx())
                    drawLine(green, Offset(center.x, tip), Offset(center.x + 4.dp.toPx(), tip + 5.dp.toPx()), strokeWidth = 2.dp.toPx())
                }
            }
        }
    }
}

/**
 * Full-screen preview of [ZenCircleShareCard] with "Save as image" and "Share". The card
 * is drawn into a graphics layer at full size, so the saved image is sharp even when
 * the preview is scaled down to fit a short screen.
 */
@Composable
fun ZenCircleSharePreview(
    visible: Boolean,
    members: List<ZenCircleMember>,
    ranks: Map<Int, Int>,
    // The fully-built share message -- BuddyConnector.inviteMessage()/circleInviteMessage(),
    // whichever the caller's context needs. Resolved by the caller so this component never
    // has to know or re-derive whether it's showing a Buddy or a Circle.
    shareText: String?,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val layer = rememberGraphicsLayer()
        var cardSize by remember { mutableStateOf(IntSize.Zero) }
        val shareText = shareText ?: "Join my Zen Circle on ZenMode OS. #ZenTogether"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val fit = if (cardSize == IntSize.Zero) 1f else minOf(
                    1f,
                    constraints.maxWidth / cardSize.width.toFloat(),
                    constraints.maxHeight / cardSize.height.toFloat()
                )
                ZenCircleShareCard(
                    members = members,
                    ranks = ranks,
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .graphicsLayer {
                            scaleX = fit
                            scaleY = fit
                        }
                        .onSizeChanged { cardSize = it }
                        .drawWithContent {
                            layer.record { this@drawWithContent.drawContent() }
                            drawLayer(layer)
                        }
                        // Taps on the card shouldn't close the preview.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                )
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ZenCirclePillButton(
                    text = "Share my Zen Circle",
                    onClick = {
                        scope.launch {
                            shareImage(
                                context = context,
                                bitmap = layer.toImageBitmap().asAndroidBitmap(),
                                fileName = "zenmode_circle.png",
                                text = shareText,
                                chooserTitle = "Share my Zen Circle"
                            )
                        }
                    },
                    container = colorResource(R.color.zen_700),
                    content = Color.White
                )
                ZenCirclePillButton(
                    text = "Save as image",
                    onClick = {
                        scope.launch {
                            saveImageToPictures(context, layer.toImageBitmap().asAndroidBitmap(), fileBaseName = "zenmode_circle")
                        }
                    },
                    container = Color.Transparent,
                    content = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}
