package com.zenlauncher.zenmode.recap

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.onboarding.DarkSystemBars
import com.zenlauncher.zenmode.onboarding.OnboardingButton
import com.zenlauncher.zenmode.onboarding.OnboardingButtonStyle
import com.zenlauncher.zenmode.onboarding.OnboardingTextButton
import com.zenlauncher.zenmode.onboarding.SegmentedProgress
import com.zenlauncher.zenmode.ui.components.ZenModeOsWordmark
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.components.rememberStoryState
import com.zenlauncher.zenmode.ui.components.storyGestures
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * "Your week in Zen": five full-bleed story cards. A kept week ends on Invest; a missed
 * week names what got in the way and ends on recommitting, with Invest shown locked.
 */
@Composable
fun RecapScreen(
    recap: WeeklyRecap,
    onCardViewed: (card: RecapCard, position: Int) -> Unit,
    onInvest: () -> Unit,
    onRecommit: () -> Unit,
    onClose: () -> Unit
) {
    val cards = remember(recap) { RecapStory.cards(recap) }
    val story = rememberStoryState(cards.size, storyMillis = 6_500)
    val card = cards[story.index]
    val palette = paletteFor(card, recap.outcome)
    val background by animateColorAsState(palette.background, tween(500), label = "recapBg")
    val content = palette.content
    val isDark = background.luminance() < 0.5f

    LaunchedEffect(story.index) { onCardViewed(card, story.index) }
    BackHandler { if (story.index > 0) story.previous() else onClose() }
    if (isDark) DarkSystemBars()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        palette.glow?.let { glow ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(glow.copy(alpha = 0.35f), Color.Transparent), radius = 1100f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Chrome: story bar, wordmark, close.
            Column(Modifier.padding(horizontal = 16.rdp).padding(top = 10.rdp)) {
                SegmentedProgress(
                    segments = cards.size,
                    currentIndex = story.index,
                    currentFraction = story.fraction,
                    dark = isDark
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZenModeOsWordmark(
                        fontSize = 17.rsp,
                        markSize = 18.rdp,
                        zenModeColor = content,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.rdp)
                    )
                    Box(
                        modifier = Modifier
                            .requiredSize(44.dp)
                            .pressScale(onClick = onClose, onClickLabel = "Close recap"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = content)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .storyGestures(story)
            ) {
                AnimatedContent(
                    targetState = story.index,
                    transitionSpec = {
                        (fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 12 })
                            .togetherWith(fadeOut(tween(180)))
                    },
                    label = "recapCard"
                ) { i ->
                    CardBody(cards[i], recap, content, palette.accent)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.rdp)
                    .padding(bottom = 16.rdp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.rdp)
            ) {
                when (card) {
                    is RecapCard.InvestUnlocked -> {
                        OnboardingButton(text = "Invest in gold", onClick = onInvest, style = OnboardingButtonStyle.Ink)
                        OnboardingTextButton(text = "Maybe later", onClick = onClose, color = content.copy(alpha = 0.7f))
                        Disclaimer(content)
                    }
                    is RecapCard.Recommit -> {
                        OnboardingButton(text = "Recommit for this week", onClick = onRecommit, style = OnboardingButtonStyle.Light)
                        LockedInvestPill(recap.daysToUnlock, content)
                    }
                    else -> Text(
                        text = "Tap to continue",
                        fontFamily = Geist,
                        fontSize = 13.rsp,
                        color = content.copy(alpha = 0.55f),
                        modifier = Modifier.padding(vertical = 14.rdp)
                    )
                }
            }
        }
    }
}

// ── Palettes ──────────────────────────────────────────────────────

private data class CardPalette(val background: Color, val content: Color, val accent: Color, val glow: Color? = null)

@Composable
private fun paletteFor(card: RecapCard, outcome: RecapOutcome): CardPalette {
    val ink = colorResource(R.color.ink_surface)
    val white = Color.White
    return when (card) {
        // Dark grounds behind the chrome keep the green mark and gradient "OS" legible.
        is RecapCard.Opener -> if (outcome == RecapOutcome.KEPT) {
            CardPalette(colorResource(R.color.onboarding_night), white, colorResource(R.color.zen_300), colorResource(R.color.os_grad_glow))
        } else {
            CardPalette(colorResource(R.color.recap_dusk), white, colorResource(R.color.recap_dusk_glow), colorResource(R.color.recap_dusk_glow))
        }
        is RecapCard.Total -> CardPalette(colorResource(R.color.paper_raised), ink, colorResource(R.color.zen_700))
        is RecapCard.Promise -> CardPalette(colorResource(R.color.ink_base), white, colorResource(R.color.zen_300))
        is RecapCard.Highlight -> CardPalette(colorResource(R.color.amber_500), ink, colorResource(R.color.amber_800))
        is RecapCard.Obstacle -> CardPalette(colorResource(R.color.ember_on), ink, colorResource(R.color.ember_700))
        is RecapCard.InvestUnlocked -> CardPalette(colorResource(R.color.recap_gold_light), ink, colorResource(R.color.amber_800), colorResource(R.color.recap_gold_deep))
        is RecapCard.Recommit -> CardPalette(colorResource(R.color.onboarding_night), white, colorResource(R.color.zen_300), colorResource(R.color.zen_500))
    }
}

// ── Cards ─────────────────────────────────────────────────────────

@Composable
private fun CardBody(card: RecapCard, recap: WeeklyRecap, content: Color, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.rdp)
            .padding(top = 12.rdp)
    ) {
        when (card) {
            is RecapCard.Opener -> OpenerCard(card, recap, content, accent)
            is RecapCard.Total -> TotalCard(card, recap, content, accent)
            is RecapCard.Promise -> PromiseCard(card, content, accent)
            is RecapCard.Highlight -> HighlightCard(card, content, accent)
            is RecapCard.Obstacle -> ObstacleCard(card, recap, content, accent)
            is RecapCard.InvestUnlocked -> InvestCard(card, content, accent)
            is RecapCard.Recommit -> RecommitCard(card, content, accent)
        }
    }
}

@Composable
private fun ColumnScope.OpenerCard(card: RecapCard.Opener, recap: WeeklyRecap, content: Color, accent: Color) {
    val transition = rememberInfiniteTransition(label = "opener")
    val drift by transition.animateFloat(-6f, 6f, infiniteRepeatable(tween(4_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "drift")
    Spacer(Modifier.weight(0.5f))
    Eyebrow("Week of ${recap.rangeLabel()}", accent)
    Spacer(Modifier.height(14.rdp))
    Headline(card.headline, content, size = 58f)
    Spacer(Modifier.height(16.rdp))
    Body(card.subline, content.copy(alpha = 0.78f), size = 18f)
    Spacer(Modifier.weight(1f))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
        Image(
            painter = painterResource(R.drawable.ic_zen_mark_gradient),
            contentDescription = null,
            modifier = Modifier
                .size(150.rdp)
                .offset(x = 30.rdp)
                .rotate(drift)
                .alpha(0.9f)
        )
    }
}

@Composable
private fun ColumnScope.TotalCard(card: RecapCard.Total, recap: WeeklyRecap, content: Color, accent: Color) {
    val countUp = rememberCountUp(card.totalMinutes)
    Spacer(Modifier.height(24.rdp))
    Eyebrow("Screen time this week", accent)
    Spacer(Modifier.height(12.rdp))
    Text(
        text = formatMinutes(countUp),
        fontFamily = DepartureMono,
        fontSize = 68.rsp,
        letterSpacing = (-2).sp,
        color = content,
        maxLines = 1,
        modifier = Modifier.semantics { contentDescription = "${formatMinutes(card.totalMinutes)} of screen time" }
    )
    card.changeVsLastWeekMinutes?.let { change ->
        Spacer(Modifier.height(10.rdp))
        val better = change <= 0
        DeltaPill(
            text = (if (better) "▼ " else "▲ ") + formatMinutes(kotlin.math.abs(change)) + " vs last week",
            container = colorResource(if (better) R.color.zen_050 else R.color.ember_on),
            content = colorResource(if (better) R.color.zen_700 else R.color.ember_700)
        )
    }
    Spacer(Modifier.height(16.rdp))
    Body(card.caption, content.copy(alpha = 0.75f))
    Spacer(Modifier.weight(1f))
    WeekBars(recap, content)
    Spacer(Modifier.height(12.rdp))
}

@Composable
private fun ColumnScope.PromiseCard(card: RecapCard.Promise, content: Color, accent: Color) {
    Spacer(Modifier.height(24.rdp))
    Eyebrow("Your promise", accent)
    Spacer(Modifier.height(12.rdp))
    Headline(card.headline, content, size = 40f)
    Spacer(Modifier.height(14.rdp))
    Body(card.caption, content.copy(alpha = 0.75f))
    Spacer(Modifier.weight(1f))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        card.days.forEachIndexed { i, day ->
            DayDot(day, delayMillis = 120 * i, content = content)
        }
    }
    Spacer(Modifier.height(18.rdp))
    Text(
        text = if (card.daysKept >= card.daysToUnlock) "Invest unlocked · ${card.daysToUnlock} of 7 needed"
        else "${card.daysKept} of ${card.daysToUnlock} needed to unlock Invest",
        fontFamily = DepartureMono,
        fontSize = 13.rsp,
        letterSpacing = 0.6.sp,
        color = content.copy(alpha = 0.6f),
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.weight(0.6f))
}

@Composable
private fun ColumnScope.HighlightCard(card: RecapCard.Highlight, content: Color, accent: Color) {
    Spacer(Modifier.weight(0.4f))
    Eyebrow(card.headline, accent)
    Spacer(Modifier.height(12.rdp))
    Headline(card.value, content, size = 64f)
    Spacer(Modifier.height(18.rdp))
    Body(card.caption, content.copy(alpha = 0.8f), size = 20f)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun ColumnScope.ObstacleCard(card: RecapCard.Obstacle, recap: WeeklyRecap, content: Color, accent: Color) {
    Spacer(Modifier.height(24.rdp))
    Eyebrow(card.headline, accent)
    Spacer(Modifier.height(18.rdp))
    val culprit = recap.topApps.firstOrNull()
    if (culprit != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(culprit.packageName, size = 64)
            Spacer(Modifier.width(16.rdp))
            Column {
                Text(
                    text = culprit.label,
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 30.rsp,
                    color = content,
                    maxLines = 1
                )
                Text(
                    text = formatMinutes(culprit.minutes),
                    fontFamily = DepartureMono,
                    fontSize = 22.rsp,
                    color = accent
                )
            }
        }
        Spacer(Modifier.height(20.rdp))
    }
    Body(card.detail, content.copy(alpha = 0.85f), size = 18f)
    Spacer(Modifier.weight(1f))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.rdp))
            .background(Color.White)
            .padding(16.rdp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = colorResource(R.color.amber_500), modifier = Modifier.size(22.rdp))
        Spacer(Modifier.width(10.rdp))
        Body(card.tip, content, size = 15f)
    }
    Spacer(Modifier.height(12.rdp))
}

@Composable
private fun ColumnScope.InvestCard(card: RecapCard.InvestUnlocked, content: Color, accent: Color) {
    val transition = rememberInfiniteTransition(label = "coin")
    val float by transition.animateFloat(-8f, 8f, infiniteRepeatable(tween(2_200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    Spacer(Modifier.height(24.rdp))
    Eyebrow("Zen Gold", accent)
    Spacer(Modifier.height(12.rdp))
    Headline(card.headline, content, size = 46f)
    Spacer(Modifier.height(14.rdp))
    Body(card.caption, content.copy(alpha = 0.8f))
    Spacer(Modifier.weight(1f))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.ic_invest_gold_coin),
            contentDescription = null,
            modifier = Modifier
                .size(170.rdp)
                .graphicsLayer { translationY = float * density }
        )
    }
    Spacer(Modifier.weight(1f))
}

@Composable
private fun ColumnScope.RecommitCard(card: RecapCard.Recommit, content: Color, accent: Color) {
    Spacer(Modifier.weight(0.4f))
    Eyebrow("A fresh week", accent)
    Spacer(Modifier.height(12.rdp))
    Headline(card.headline, content, size = 50f)
    Spacer(Modifier.height(16.rdp))
    Body(card.caption, content.copy(alpha = 0.85f), size = 17f)
    card.suggestedPromiseHours?.let { hours ->
        Spacer(Modifier.height(20.rdp))
        DeltaPill(
            text = "Suggested: $hours ${if (hours == 1) "hour" else "hours"} a day",
            container = Color.White.copy(alpha = 0.16f),
            content = Color.White
        )
    }
    Spacer(Modifier.weight(1f))
}

// ── Pieces ────────────────────────────────────────────────────────

@Composable
private fun Eyebrow(text: String, color: Color) {
    Text(
        text = text.uppercase(),
        fontFamily = DepartureMono,
        fontSize = 13.rsp,
        letterSpacing = 1.3.sp,
        color = color,
        maxLines = 1
    )
}

@Composable
private fun Headline(text: String, color: Color, size: Float) {
    Text(
        text = text,
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = size.rsp,
        lineHeight = (size * 1.02f).rsp,
        letterSpacing = (-size * 0.025f).sp,
        color = color,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun Body(text: String, color: Color, size: Float = 16f) {
    Text(
        text = text,
        fontFamily = Geist,
        fontSize = size.rsp,
        lineHeight = (size * 1.4f).rsp,
        color = color
    )
}

@Composable
private fun DeltaPill(text: String, container: Color, content: Color) {
    Text(
        text = text,
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.rsp,
        color = content,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .padding(horizontal = 12.rdp, vertical = 6.rdp)
    )
}

/** The week as seven bars against a dashed promise line; kept days green, missed ember. */
@Composable
private fun WeekBars(recap: WeeklyRecap, content: Color) {
    val kept = colorResource(R.color.zen_500)
    val missed = colorResource(R.color.ember_500)
    val line = content.copy(alpha = 0.4f)
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val maxMinutes = maxOf(recap.days.maxOf { it.screenTimeMinutes }, recap.promiseHours * 60L, 1L)

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.rdp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                recap.days.forEach { day ->
                    val fraction = day.screenTimeMinutes.toFloat() / maxMinutes
                    Box(
                        Modifier
                            .width(30.rdp)
                            .fillMaxHeight(fraction * grow.value)
                            .clip(RoundedCornerShape(topStart = 8.rdp, topEnd = 8.rdp))
                            .background(if (day.keptPromise) kept else missed)
                    )
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * (1f - recap.promiseHours * 60f / maxMinutes)
                drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
        }
        Spacer(Modifier.height(8.rdp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            recap.days.forEach { day ->
                Text(
                    text = day.shortDayName().take(1),
                    fontFamily = DepartureMono,
                    fontSize = 12.rsp,
                    color = content.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(30.rdp)
                )
            }
        }
        Spacer(Modifier.height(6.rdp))
        Text(
            text = "- - -  your promise: ${recap.promiseHours}h a day",
            fontFamily = Geist,
            fontSize = 12.rsp,
            color = content.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun DayDot(day: DayRecord, delayMillis: Int, content: Color) {
    val inspection = LocalInspectionMode.current
    val pop = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
    }
    val kept = colorResource(R.color.os_grad_glow)
    val missed = colorResource(R.color.ember_500)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(38.rdp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(CircleShape)
                .then(
                    if (day.keptPromise) Modifier.background(kept)
                    else Modifier.border(2.dp, missed, CircleShape)
                )
                .semantics {
                    contentDescription = "${day.fullDayName()}: ${if (day.keptPromise) "kept" else "missed"}, " +
                        formatMinutes(day.screenTimeMinutes)
                },
            contentAlignment = Alignment.Center
        ) {
            if (day.keptPromise) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = colorResource(R.color.ink_base), modifier = Modifier.size(22.rdp))
            } else {
                Text("×", color = missed, fontSize = 20.rsp, fontFamily = Geist)
            }
        }
        Spacer(Modifier.height(8.rdp))
        Text(
            text = day.shortDayName().take(3),
            fontFamily = DepartureMono,
            fontSize = 11.rsp,
            color = content.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun LockedInvestPill(daysToUnlock: Int, content: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.rdp)
            .clip(RoundedCornerShape(percent = 50))
            .border(1.dp, content.copy(alpha = 0.35f), RoundedCornerShape(percent = 50))
            .semantics(mergeDescendants = true) { contentDescription = "Invest is locked. It unlocks at $daysToUnlock of 7 days." },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = content.copy(alpha = 0.75f), modifier = Modifier.size(16.rdp))
        Spacer(Modifier.width(8.rdp))
        Text(
            text = "Invest unlocks at $daysToUnlock of 7 days",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 14.rsp,
            color = content.copy(alpha = 0.8f)
        )
    }
}

/** Required wherever Invest is offered (see the Gold Streak legal notes). */
@Composable
private fun Disclaimer(content: Color) {
    Text(
        text = "ZenMode never holds or receives your money. You invest in your own broker's app, " +
            "at your discretion. This is not investment advice, and gold prices can go down as well as up.",
        fontFamily = Geist,
        fontSize = 11.rsp,
        lineHeight = 15.rsp,
        color = content.copy(alpha = 0.6f),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AppIcon(packageName: String, size: Int) {
    val context = LocalContext.current
    val px = (size * context.resources.displayMetrics.density).toInt()
    val icon: ImageBitmap? = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(px, px).asImageBitmap() }.getOrNull()
    }
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(size.rdp))
    } else {
        Box(
            Modifier
                .size(size.rdp)
                .clip(RoundedCornerShape(16.rdp))
                .background(Color.Black.copy(alpha = 0.08f))
        )
    }
}

@Composable
private fun rememberCountUp(target: Long): Long {
    val inspection = LocalInspectionMode.current
    val progress = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(target) { progress.animateTo(1f, tween(1_400, easing = FastOutSlowInEasing)) }
    return (target * progress.value).toLong()
}
