package com.zenlauncher.zenmode.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ZenScore
import com.zenlauncher.zenmode.ui.components.ZenModeOsWordmark
import com.zenlauncher.zenmode.ui.components.rememberBrandOsGradient
import com.zenlauncher.zenmode.ui.components.saveImageToPictures
import com.zenlauncher.zenmode.ui.components.shareImage
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Home share overlays ───────────────────────────────────────────
// Home's header has three tappable stats. Streaks already opened a shareable
// milestone card (Figma node 2026:2137); Zen Score and Gold Invested now open the
// same sheet with their own card, so all three read as one family:
//
//   sheet chrome (eyebrow + close, headline, card, stat lines, Save/Share)  → [ShareSheet]
//   card chrome  (wordmark + eyebrow, hero stat, meter, tagline + domain)   → [ShareCardFrame]
//
// Everything each card does differently lives in its own composable below; nothing
// about the sheet is re-typed per card.
//
// SOURCE OF TRUTH: design tokens — the card is the one dark surface in the app, so
// its colours come from colors.xml (never a bare Color(0x...) literal here).

internal val ShareCardBg: Color @Composable get() = colorResource(R.color.ink_base)
internal val ShareCardMuted: Color @Composable get() = colorResource(R.color.milestone_muted)
internal val ShareCardDim: Color @Composable get() = colorResource(R.color.milestone_dim)
internal val ShareCardAmber: Color @Composable get() = colorResource(R.color.amber_500)
internal val ShareCardTagline: Color @Composable get() = colorResource(R.color.milestone_tagline)
internal val ShareCardOutlineBg: Color @Composable get() = colorResource(R.color.milestone_outline_bg)
internal val ShareCardOutlineBorder: Color @Composable get() = colorResource(R.color.zen_700)
internal val ShareCardSolidBg: Color @Composable get() = colorResource(R.color.zen_700)

/** The sheet keeps Home's 30dp margin so it lines up with the header it opened from. */
private val SheetMargin: Dp @Composable get() = 30.rdp

/** Card inner rhythm, shared by all three cards so they stack to the same height. */
private val CardPadding: Dp @Composable get() = 20.rdp
private val CardRadius: Dp @Composable get() = 24.rdp
private val CardHeroSize: Dp @Composable get() = 73.rdp

private val ShareDateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.US)

// ── Sheet ─────────────────────────────────────────────────────────

/**
 * The bottom sheet every share card sits in: scrim, drag-to-dismiss, [eyebrow] + close,
 * then [content], then Save / Share.
 *
 * [content] receives the modifier its card must carry — that's what the two buttons
 * crop to, so the saved image is the card and nothing of the sheet around it.
 */
@Composable
internal fun ShareSheet(
    eyebrow: String,
    shareLabel: String,
    fileBaseName: String,
    shareText: String,
    chooserTitle: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(cardModifier: Modifier) -> Unit
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var offsetY by remember { mutableStateOf(0f) }
    // Save / Share render this layer, not a crop of the window: it holds the card alone,
    // at its own size, and can't be broken by a hardware bitmap elsewhere on the page
    // (an avatar photo, say) the way a software capture of the whole view can.
    val cardLayer = rememberGraphicsLayer()

    BackHandler(enabled = true) { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = offsetY.coerceAtLeast(0f).dp)
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) onDismiss()
                        offsetY = 0f
                    },
                    onVerticalDrag = { _, dragAmount -> offsetY += dragAmount }
                )
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(topStart = 24.rdp, topEnd = 24.rdp))
                .background(colors.bgSecondary.copy(alpha = 0.96f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume click */ }
                .padding(horizontal = SheetMargin)
                .padding(top = 17.rdp, bottom = 32.rdp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = eyebrow,
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.rsp,
                    letterSpacing = (-0.42).sp,
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Image(
                    painter = painterResource(R.drawable.ic_milestone_close),
                    contentDescription = "Close",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(24.rdp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onDismiss() }
                )
            }

            content(
                Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        cardLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(cardLayer)
                    }
            )

            Spacer(modifier = Modifier.height(24.rdp))

            Column(verticalArrangement = Arrangement.spacedBy(12.rdp)) {
                ShareOutlineButton(
                    text = "Save as image",
                    onClick = {
                        scope.launch {
                            deliverShareCard(cardLayer, context, false, fileBaseName, shareText, chooserTitle)
                        }
                    }
                )
                ShareSolidButton(
                    text = shareLabel,
                    onClick = {
                        scope.launch {
                            deliverShareCard(cardLayer, context, true, fileBaseName, shareText, chooserTitle)
                        }
                    }
                )
            }
        }
    }
}

@Composable
internal fun ShareOutlineButton(text: String, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(47.rdp)
            .clip(RoundedCornerShape(50))
            .background(ShareCardOutlineBg)
            .border(1.rdp, ShareCardOutlineBorder.copy(alpha = 0.26f), RoundedCornerShape(50))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        Image(
            painter = painterResource(R.drawable.ic_download),
            contentDescription = null,
            modifier = Modifier.size(16.rdp)
        )
        Spacer(modifier = Modifier.width(8.rdp))
        Text(
            text = text,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 17.rsp,
            letterSpacing = (-0.35).sp,
            color = GoldDeltaText
        )
    }
}

@Composable
internal fun ShareSolidButton(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(47.rdp)
            .clip(RoundedCornerShape(50))
            .background(ShareCardSolidBg)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 17.rsp,
            letterSpacing = (-0.35).sp,
            color = Color.White
        )
    }
}

/**
 * Turns the recorded card layer into a PNG and either shares it or saves it. Save targets
 * the public Pictures/ZenMode gallery folder via MediaStore on Android 10+; below that (no
 * scoped storage, and not worth a runtime WRITE_EXTERNAL_STORAGE prompt for a shrinking API
 * tail) it falls back to app-private storage.
 */
internal suspend fun deliverShareCard(
    cardLayer: GraphicsLayer,
    context: android.content.Context,
    share: Boolean,
    fileBaseName: String,
    shareText: String,
    chooserTitle: String
) {
    val bitmap = runCatching { cardLayer.toImageBitmap().asAndroidBitmap() }.getOrElse { error ->
        Log.e("ShareSheet", "couldn't render $fileBaseName", error)
        return
    }
    if (share) {
        shareImage(
            context = context,
            bitmap = bitmap,
            fileName = "$fileBaseName.png",
            text = shareText,
            chooserTitle = chooserTitle
        )
    } else {
        saveImageToPictures(context, bitmap, fileBaseName = fileBaseName)
    }
}

// ── Card frame ────────────────────────────────────────────────────

/**
 * The dark card itself: wordmark and [stamp] across the top, [content] in the middle,
 * tagline and domain along the bottom. Every share card is this frame plus a middle.
 */
@Composable
internal fun ShareCardFrame(
    stamp: String,
    modifier: Modifier = Modifier,
    tagline: String = "Quiet the noise.",
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CardRadius))
            .background(ShareCardBg)
            .padding(CardPadding)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                ZenModeOsWordmark(
                    fontSize = 18.rsp,
                    markSize = 20.rdp,
                    markGap = 6.rdp,
                    zenModeColor = Color.White
                )
                Text(
                    text = stamp,
                    fontFamily = DepartureMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.rsp,
                    color = ShareCardMuted
                )
            }

            Spacer(modifier = Modifier.height(28.rdp))

            content()

            Spacer(modifier = Modifier.height(16.rdp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tagline,
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.rsp,
                    letterSpacing = (-0.16).sp,
                    color = ShareCardTagline
                )
                Text(
                    text = "Zenmodeos.com",
                    fontFamily = DepartureMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.rsp,
                    letterSpacing = (-0.12).sp,
                    color = ShareCardMuted
                )
            }
        }
    }
}

/**
 * The hero line every card shares: a round badge, then a big number with its unit and a
 * sentence under it — the same shape as Home's `IconMatchedLabel`, one size up.
 */
@Composable
internal fun ShareCardHero(
    unit: String,
    caption: String,
    badge: @Composable () -> Unit,
    value: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(CardHeroSize)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) { badge() }
        Spacer(modifier = Modifier.width(14.rdp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                value()
                Spacer(modifier = Modifier.width(8.rdp))
                Text(
                    text = unit,
                    fontFamily = DepartureMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.rsp,
                    letterSpacing = (-0.96).sp,
                    color = ShareCardDim,
                    modifier = Modifier.padding(bottom = 5.rdp)
                )
            }
            Spacer(modifier = Modifier.height(8.rdp))
            Text(
                text = caption,
                fontFamily = Geist,
                fontWeight = FontWeight.Normal,
                fontSize = 12.rsp,
                letterSpacing = (-0.12).sp,
                lineHeight = 16.rsp,
                color = ShareCardMuted
            )
        }
    }
}

/** The big numeral in a card's hero, in the one type the cards use for data. */
@Composable
internal fun ShareCardValue(text: String, color: Color? = null, brush: Brush? = null) {
    Text(
        text = text,
        fontFamily = DepartureMono,
        fontWeight = FontWeight.Normal,
        fontSize = 32.rsp,
        letterSpacing = (-0.96).sp,
        color = color ?: Color.Unspecified,
        style = if (brush != null) TextStyle(brush = brush) else TextStyle.Default
    )
}

// ── Zen Score overlay ─────────────────────────────────────────────

@Composable
internal fun ZenScoreOverlay(
    /** Today's score in tenths (93 = 9.3), the same value Home's header counts up to. */
    zenScore: Int,
    onDismiss: () -> Unit,
    reclaimedMinutes: Int = AppConstants.PLACEHOLDER_RECLAIMED_MINUTES,
    topPercentile: Int = AppConstants.PLACEHOLDER_MILESTONE_PERCENTILE,
    today: LocalDate = LocalDate.now()
) {
    val colors = ZenTheme.colors
    val formatted = ZenScore.format(zenScore)

    ShareSheet(
        eyebrow = "ZEN SCORE",
        shareLabel = "Share my Zen Score",
        fileBaseName = "zenmode_zen_score",
        shareText = "$formatted/${ZenScore.MAX_DISPLAY} on ZenMode OS today.",
        chooserTitle = "Share Zen Score",
        onDismiss = onDismiss
    ) { cardModifier ->
        Spacer(modifier = Modifier.height(35.rdp))

        Text(
            text = "${scoreWord(zenScore)} day — $formatted out of ${ZenScore.MAX_DISPLAY} on how calmly today was spent.",
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 20.rsp,
            lineHeight = 24.rsp,
            letterSpacing = (-0.6).sp,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(15.rdp))

        Text(
            text = "You're in the top $topPercentile% of the Zen Bros",
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.rsp,
            letterSpacing = (-0.16).sp,
            color = colors.textBrand
        )

        Spacer(modifier = Modifier.height(15.rdp))

        ZenScoreShareCard(
            modifier = cardModifier,
            zenScore = zenScore,
            reclaimedMinutes = reclaimedMinutes,
            today = today
        )

        Spacer(modifier = Modifier.height(20.rdp))

        ShareStatLine(
            lead = "TODAY · $formatted/${ZenScore.MAX_DISPLAY}",
            trail = "(${ShareDateFormat.format(today).uppercase(Locale.US)})"
        )

        Spacer(modifier = Modifier.height(10.rdp))

        ShareStatLine(
            lead = "RECLAIMED · ${"%,d".format(Locale.US, reclaimedMinutes)} MINS",
            trail = "(THIS MONTH)"
        )
    }
}

@Composable
private fun ZenScoreShareCard(
    zenScore: Int,
    reclaimedMinutes: Int,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    ShareCardFrame(
        stamp = ShareDateFormat.format(today).uppercase(Locale.US),
        modifier = modifier
    ) {
        ShareCardHero(
            unit = "/${ZenScore.MAX_DISPLAY}",
            caption = "That's ${"%,d".format(Locale.US, reclaimedMinutes)} minutes reclaimed this month — time that " +
                "went somewhere better than a feed.",
            badge = {
                Image(
                    painter = painterResource(R.drawable.ic_zen_mark_gradient),
                    contentDescription = null,
                    modifier = Modifier.size(42.rdp)
                )
            },
            // Same green→orange ramp as Home's header number, so the two read as one stat.
            value = { ShareCardValue(text = ZenScore.format(zenScore), brush = ZenScoreGradient) }
        )

        Spacer(modifier = Modifier.height(20.rdp))

        ZenScoreMeter(
            zenScore = zenScore,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.rdp)
        )
    }
}

/**
 * Ten bars for ten points, filling left to right — the card's read-at-a-glance version of
 * Zen Score's dial. The bar the score lands inside fills by its fraction, so 7.4 reads as
 * seven full bars and a short eighth rather than snapping to a whole number.
 *
 * The brand gradient runs across the whole row, and at its -67.9° it lands orange on the
 * low bars and green on the high ones — which is exactly how a score should read.
 */
@Composable
private fun ZenScoreMeter(zenScore: Int, modifier: Modifier = Modifier) {
    val gradient = rememberBrandOsGradient()
    val emptyColor = Color.White.copy(alpha = 0.08f)
    val points = zenScore.coerceIn(0, ZenScore.MAX_TENTHS) / 10f
    Canvas(modifier = modifier) {
        val bars = ZenScore.MAX_DISPLAY
        val gap = 6.dp.toPx()
        val barWidth = (size.width - gap * (bars - 1)) / bars
        val radius = CornerRadius(3.dp.toPx())
        for (i in 0 until bars) {
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = emptyColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = radius
            )
            val fill = (points - i).coerceIn(0f, 1f)
            if (fill > 0f) {
                drawRoundRect(
                    brush = gradient,
                    topLeft = Offset(x, size.height * (1f - fill)),
                    size = Size(barWidth, size.height * fill),
                    cornerRadius = radius
                )
            }
        }
    }
}

/** "Calm"/"Steady"/"Noisy" — the one-word verdict the headline opens with. */
private fun scoreWord(tenths: Int): String = when {
    tenths >= 80 -> "A calm"
    tenths >= 60 -> "A steady"
    tenths >= 40 -> "A mixed"
    else -> "A noisy"
}

// ── Gold Invested overlay ─────────────────────────────────────────

// Twelve weeks of investing, the shape of the story rather than measured history: real
// week-by-week amounts need the Gold Streak backend that doesn't exist yet (see
// zenmode_core_private/docs/plans), same as the rest of the gold placeholders.
private val PlaceholderGoldWeeks =
    listOf(0.18f, 0.24f, 0.21f, 0.33f, 0.4f, 0.36f, 0.5f, 0.58f, 0.55f, 0.72f, 0.86f, 1f)

@Composable
internal fun GoldInvestedOverlay(
    /** The same formatted amount Home's pill shows, e.g. "2,350". */
    gold: String,
    changePercent: Int,
    onDismiss: () -> Unit,
    daysInvested: Int = AppConstants.PLACEHOLDER_MILESTONE_DAYS,
    today: LocalDate = LocalDate.now()
) {
    val colors = ZenTheme.colors
    val units = goldUnits(gold)

    ShareSheet(
        eyebrow = "GOLD INVESTED",
        shareLabel = "Share my Zen Gold",
        fileBaseName = "zenmode_gold_invested",
        shareText = "₹$gold of screen time turned into gold on ZenMode OS.",
        chooserTitle = "Share Zen Gold",
        onDismiss = onDismiss
    ) { cardModifier ->
        Spacer(modifier = Modifier.height(35.rdp))

        Text(
            text = "₹$gold of the time I didn't scroll, sitting in gold instead.",
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 20.rsp,
            lineHeight = 24.rsp,
            letterSpacing = (-0.6).sp,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(15.rdp))

        Text(
            text = "Up $changePercent% since the first promise kept",
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.rsp,
            letterSpacing = (-0.16).sp,
            color = colors.textBrand
        )

        Spacer(modifier = Modifier.height(15.rdp))

        GoldInvestedShareCard(
            modifier = cardModifier,
            gold = gold,
            changePercent = changePercent,
            daysInvested = daysInvested,
            today = today
        )

        Spacer(modifier = Modifier.height(20.rdp))

        ShareStatLine(
            lead = "INVESTED · ₹$gold",
            trail = "($units UNITS ${AppConstants.PLACEHOLDER_GOLD_SYMBOL})"
        )

        Spacer(modifier = Modifier.height(10.rdp))

        ShareStatLine(
            lead = "EARNED OVER · $daysInvested DAYS",
            trail = "(${AppConstants.PLACEHOLDER_GOLD_EXCHANGE})"
        )
    }
}

@Composable
private fun GoldInvestedShareCard(
    gold: String,
    changePercent: Int,
    daysInvested: Int,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    ShareCardFrame(
        stamp = "${ShareDateFormat.format(today).uppercase(Locale.US)} · +$changePercent%",
        modifier = modifier
    ) {
        ShareCardHero(
            unit = "IN GOLD",
            caption = "Every hour under the promise buys a little gold. $daysInvested days of " +
                "keeping it bought this much.",
            badge = {
                Image(
                    painter = painterResource(R.drawable.ic_coin_gold),
                    contentDescription = null,
                    modifier = Modifier.size(52.rdp)
                )
            },
            value = { ShareCardValue(text = "₹$gold", color = ShareCardAmber) }
        )

        Spacer(modifier = Modifier.height(20.rdp))

        GoldGrowthBars(
            weeks = PlaceholderGoldWeeks,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.rdp)
        )
    }
}

/**
 * Twelve weeks of investing, tallest last — the card's version of Zen Gold's forecast.
 * Bars only, no empty track: the shape of the climb is the point, and a full-height
 * track behind each one turned the row into a wall of blocks.
 */
@Composable
private fun GoldGrowthBars(weeks: List<Float>, modifier: Modifier = Modifier) {
    val filledColor = ShareCardAmber
    Canvas(modifier = modifier) {
        val gap = 5.dp.toPx()
        val barWidth = (size.width - gap * (weeks.size - 1)) / weeks.size
        val radius = CornerRadius(2.dp.toPx())
        weeks.forEachIndexed { i, fraction ->
            val height = (size.height * fraction.coerceIn(0f, 1f)).coerceAtLeast(barWidth)
            drawRoundRect(
                // The newest week is the brightest; earlier ones recede.
                color = filledColor.copy(alpha = 0.35f + 0.5f * (i + 1) / weeks.size),
                topLeft = Offset(i * (barWidth + gap), size.height - height),
                size = Size(barWidth, height),
                cornerRadius = radius
            )
        }
    }
}

/** "2,350" -> whole units at the placeholder unit price. */
private fun goldUnits(gold: String): Int {
    val rupees = gold.filter { it.isDigit() }.toLongOrNull() ?: return 0
    return (rupees * 100 / AppConstants.PLACEHOLDER_GOLD_UNIT_PRICE_PAISE).toInt()
}

// ── Shared sheet bits ─────────────────────────────────────────────

/** The mono stat lines under every card: ink lead, muted parenthetical. */
@Composable
internal fun ShareStatLine(lead: String, trail: String) {
    val colors = ZenTheme.colors
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = colors.textPrimary)) { append("$lead ") }
            withStyle(SpanStyle(color = colors.textSecondary)) { append(trail) }
        },
        fontFamily = DepartureMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.rsp,
        letterSpacing = (-0.14).sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
