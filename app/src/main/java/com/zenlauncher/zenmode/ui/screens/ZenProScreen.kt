package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.services.BillingPeriod
import com.zenlauncher.zenmode.coreapi.services.Entitlement
import com.zenlauncher.zenmode.coreapi.services.PlanOffer
import com.zenlauncher.zenmode.coreapi.services.ProStatus
import com.zenlauncher.zenmode.ui.components.GlyphKind
import com.zenlauncher.zenmode.ui.components.ZenMotion
import com.zenlauncher.zenmode.ui.components.rememberReduceMotion
import com.zenlauncher.zenmode.ui.components.rememberZenFeedback
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalInspectionMode
import com.zenlauncher.zenmode.ui.components.ZenButton
import com.zenlauncher.zenmode.ui.components.ZenButtonStyle
import com.zenlauncher.zenmode.ui.components.ZenEyebrow
import com.zenlauncher.zenmode.ui.components.ZenGlyph
import com.zenlauncher.zenmode.ui.components.ZenReceiptLine
import com.zenlauncher.zenmode.ui.components.ZenSheet
import com.zenlauncher.zenmode.ui.components.ZenSheetBody
import com.zenlauncher.zenmode.ui.components.ZenSheetTitle
import com.zenlauncher.zenmode.ui.components.zenCard
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.Spacing
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.ZenTypography
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// ZenMode Pro — plan page, confirm sheet, manage and cancel.
//
// Sales rules (from the plans spec) this file must keep:
//  · price above every button, read from the store, never hardcoded
//  · the free tier is listed in full before the price
//  · cancelling takes the same two taps as starting; both cancel-dialog buttons weigh the same
//  · the Zen Score itself is never a sales lever — only report cadence (daily vs weekly) differs
//  · every conversion device below (badges, savings, the sticky footer) states a real, store-backed
//    fact — nothing invented, no fake urgency, nothing that isn't also true on the manage/cancel page

/** Where the Pro page was opened from. Analytics only — the page itself never changes by entry. */
enum class ProEntry(val analyticsName: String) {
    PLAN_CARD("plan_card"),
    GATE("gate"),
    MANAGE("manage")
}

/** Short, card-sized highlights — the at-a-glance read. The full row-by-row diff is [CompareRows]. */
private val FreeHighlights = listOf(
    "The launcher, free forever",
    "Zen Score, daily average",
    "Zen Circle (beta)",
    "7 days of history",
    "Centralised search"
)

private val ProHighlights = listOf(
    "Everything in Free, plus",
    "All-time history, downloadable",
    "50 random connects a week, first priority",
    "30-minute distraction-blocker pause",
    "Data export + Telegram community"
)

private val ComingSoon = listOf(
    "More export formats",
    "Zen Circle enhancements",
    "AI suggestions to optimise",
    "New modes, and more"
)

/** A comparison cell: either a plain value both tiers state in their own words, or an include/exclude mark. */
private data class CompareCell(val text: String? = null, val included: Boolean? = null)
private data class CompareRow(val label: String, val free: CompareCell, val pro: CompareCell)

private fun cell(text: String) = CompareCell(text = text)
private val Yes = CompareCell(included = true)
private val No = CompareCell(included = false)

/** The full free-vs-pro diff, row for row. Keep in the same order the plans spec lists them. */
private val CompareRows = listOf(
    CompareRow("The launcher, with accountability", Yes, Yes),
    CompareRow("Zen Score average", cell("Daily"), cell("Weekly")),
    CompareRow("Zen Circle", cell("Beta, all members"), cell("Beta, all members")),
    CompareRow("History", cell("7 days"), cell("All time")),
    CompareRow("Session log", cell("Last 7 events"), cell("The day's events, downloadable")),
    CompareRow("Centralised search", Yes, Yes),
    CompareRow("Random connect", cell("Up to 5/week"), cell("Up to 50/week, first priority")),
    CompareRow("Screen time average", cell("Weekly"), cell("Monthly")),
    CompareRow("Edit my promise", cell("Once a week"), cell("Twice a week")),
    CompareRow("Weekly report history", No, Yes),
    CompareRow("Devices per account", cell("One"), cell("One")),
    CompareRow("Distraction blocker pause", No, cell("30 minutes")),
    CompareRow("Data export", No, Yes),
    CompareRow("Telegram community", No, Yes)
)

fun List<PlanOffer>.offer(period: BillingPeriod): PlanOffer? = firstOrNull { it.period == period }

/** "₹699/year, or ₹75/month" — null until the store has answered. */
fun List<PlanOffer>.priceSummary(): String? {
    val annual = offer(BillingPeriod.ANNUAL) ?: return null
    val monthly = offer(BillingPeriod.MONTHLY) ?: return "${annual.formattedPrice}/year"
    return "${annual.formattedPrice}/year, or ${monthly.formattedPrice}/month"
}

/** First numeric run in a store-formatted price string, e.g. "₹2,499" → 2499.0. Never hardcoded. */
private fun String.storeNumber(): Double? =
    Regex("[0-9]+(\\.[0-9]+)?").find(replace(",", ""))?.value?.toDoubleOrNull()

/** How much cheaper this offer's current price is than its own regular price — a same-plan,
 * store-backed discount, not a cross-plan comparison built to make the number look bigger. */
private fun PlanOffer.discountPercent(): Int? {
    val original = originalPrice?.storeNumber() ?: return null
    val current = formattedPrice.storeNumber() ?: return null
    if (original <= current) return null
    return (((original - current) / original) * 100).roundToInt().takeIf { it > 0 }
}

/** What the subscribe button should say for this offer's actual trial length. */
private fun PlanOffer.startLabel(): String = when {
    freeTrialDays <= 0 -> "Subscribe"
    freeTrialDays >= 28 -> "Start the free month"
    else -> "Start your $freeTrialDays-day free trial"
}

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val MonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

fun Long.asZenDate(): String = DateFormat.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
fun Long.asZenMonth(): String = MonthFormat.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

/**
 * One line under the plan card / manage header, e.g. "Renews 17 Oct 2027". With [isSimulated]
 * no price is ever quoted as upcoming, since nothing will be charged.
 */
fun Entitlement.statusLine(offers: List<PlanOffer>, isSimulated: Boolean = false): String {
    val price = period?.takeUnless { isSimulated }?.let { offers.offer(it) }
        ?.let { if (it.period == BillingPeriod.ANNUAL) "${it.formattedPrice}/year" else "${it.formattedPrice}/month" }
    return when (status) {
        // Server-granted Pro has no subscription behind it.
        ProStatus.FREE -> "Early access, nothing to manage."
        ProStatus.TRIAL -> buildString {
            append("Free trial ends ${trialEndsOn?.asZenDate() ?: "soon"}")
            if (price != null) append(", then $price")
            append(".")
        }
        ProStatus.ACTIVE -> if (isSimulated) "Early access. Nothing is charged." else buildString {
            append("Renews ${renewsOn?.asZenDate() ?: "automatically"}")
            if (price != null) append(" · $price")
            append(".")
        }
        ProStatus.ENDING -> "Pro until ${endsOn?.asZenDate() ?: "the end of this period"}. It won't renew."
    }
}

@Composable
fun ZenProScreen(
    entitlement: Entitlement,
    /** From [com.zenlauncher.zenmode.ProAccess]: also true for an invite-only server grant
     * with no subscription behind it, which [entitlement] alone can't see. */
    isPro: Boolean = entitlement.isPro,
    offers: List<PlanOffer>,
    isWorking: Boolean,
    /** No store behind the provider: checkout unlocks Pro without charging, and says so. */
    isSimulated: Boolean = false,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onPurchase: (BillingPeriod) -> Unit,
    onCancel: () -> Unit,
    onResume: () -> Unit
) {
    val colors = ZenTheme.colors
    val feedback = rememberZenFeedback()
    LaunchedEffect(errorMessage) { if (errorMessage != null) feedback.error() }
    // The plan page manages its own scroll + a pinned price/CTA footer, so the price and the
    // button that spends money are never more than one screen-height apart from whatever a
    // shopper is reading. Manage/granted pages are short and don't need that.
    val onPlanPage = !entitlement.isPro && !isPro

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .statusBarsPadding()
            .then(if (onPlanPage) Modifier else Modifier.navigationBarsPadding())
    ) {
        ProTopBar(
            title = if (isPro) "Manage Pro" else "ZenMode Pro",
            onBackClick = onBackClick
        )
        if (onPlanPage) {
            PlanPage(
                offers = offers,
                isWorking = isWorking,
                isSimulated = isSimulated,
                errorMessage = errorMessage,
                onPurchase = onPurchase,
                modifier = Modifier.weight(1f)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenMargin)
                    .padding(top = 4.rdp, bottom = 40.rdp),
                verticalArrangement = Arrangement.spacedBy(24.rdp)
            ) {
                // A real subscription (trial/active/ending) has billing to manage.
                if (entitlement.isPro) ManagePro(entitlement, offers, isWorking, isSimulated, onCancel, onResume)
                // Server-granted only: they're Pro, but there's no subscription behind it.
                else GrantedPro(entitlement)
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        fontFamily = Geist,
                        fontSize = 14.rsp,
                        lineHeight = 20.rsp,
                        color = colors.accentDeduct
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProTopBar(title: String, onBackClick: () -> Unit) {
    val colors = ZenTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.rdp, vertical = 6.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.touchTarget)
                .clip(CircleShape)
                .clickable(onClickLabel = "Back", role = Role.Button, onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.rdp)
                    .clip(CircleShape)
                    .background(colors.bgSecondary)
                    .border(1.dp, colors.borderOutline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                ZenGlyph(GlyphKind.Back, colors.textPrimary, Modifier.size(18.rdp))
            }
        }
        Spacer(Modifier.width(8.rdp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
private fun PlanPage(
    offers: List<PlanOffer>,
    isWorking: Boolean,
    isSimulated: Boolean,
    errorMessage: String?,
    onPurchase: (BillingPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    var picked by rememberSaveable { mutableStateOf(BillingPeriod.ANNUAL) }
    var confirming by remember { mutableStateOf(false) }
    // The sheet stays up through the store round trip, showing its working state, and closes
    // itself once that settles: success hands over to the welcome, a failure shows in the footer.
    var submitted by remember { mutableStateOf(false) }
    LaunchedEffect(isWorking) {
        if (!isWorking && submitted) {
            submitted = false
            confirming = false
        }
    }
    val annual = offers.offer(BillingPeriod.ANNUAL)
    val monthly = offers.offer(BillingPeriod.MONTHLY)
    val chosen = offers.offer(picked)

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin)
                .padding(top = 4.rdp, bottom = 16.rdp),
            verticalArrangement = Arrangement.spacedBy(24.rdp)
        ) {
            HeroSection(Modifier.staggeredEntrance(0))
            TierSummaryRow(annual, Modifier.staggeredEntrance(1))

            Column(
                modifier = Modifier.selectableGroup().staggeredEntrance(2),
                verticalArrangement = Arrangement.spacedBy(8.rdp)
            ) {
                ZenEyebrow("Pick a plan")
                if (annual != null) {
                    PlanOption(
                        title = "Annual",
                        originalPrice = annual.originalPrice?.let { "$it/year" },
                        price = "${annual.formattedPrice}/year",
                        detail = "${annual.formattedPerMonth} a month, billed annually." +
                            if (annual.freeTrialDays > 0) " First month free." else "",
                        selected = picked == BillingPeriod.ANNUAL,
                        badge = annual.discountPercent()?.let { "$it% off" },
                        onSelect = { picked = BillingPeriod.ANNUAL }
                    )
                }
                if (monthly != null) {
                    PlanOption(
                        title = "Monthly",
                        price = "${monthly.formattedPrice}/month",
                        detail = "≈ ${monthly.formattedYearTotal}/year — flexible billing, cancel anytime." +
                            if (monthly.freeTrialDays > 0) " ${monthly.freeTrialDays}-day free trial." else "",
                        selected = picked == BillingPeriod.MONTHLY,
                        onSelect = { picked = BillingPeriod.MONTHLY }
                    )
                }
                if (offers.isEmpty()) {
                    Text(
                        text = "Loading prices from Google Play…",
                        fontFamily = Geist,
                        fontSize = 14.rsp,
                        color = ZenTheme.colors.textSecondary
                    )
                }
            }

            FeatureComparisonTable(Modifier.staggeredEntrance(3))
            ComingSoonSection(Modifier.staggeredEntrance(4))
            TrustSection(Modifier.staggeredEntrance(5))
        }

        // A plain sibling below the scroll area, not an overlay — the price and the button
        // that spends money get their own space, never sharing a screen region with content
        // that hasn't scrolled away yet.
        PlanFooter(
            chosen = chosen,
            isWorking = isWorking,
            isSimulated = isSimulated,
            errorMessage = errorMessage,
            onSubscribe = { confirming = true },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (confirming && chosen != null) {
        ConfirmSheet(
            offer = chosen,
            isWorking = isWorking,
            isSimulated = isSimulated,
            onConfirm = {
                submitted = true
                onPurchase(chosen.period)
            },
            // Mid-transaction the sheet can't be swiped away from under the result.
            onDismiss = { if (!isWorking) confirming = false }
        )
    }
}

@Composable
private fun HeroSection(modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.rdp)) {
        OsRule(Modifier.width(48.rdp))
        Text(
            text = "Welcome in. A generous free plan to find your Zen.",
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 28.rsp,
            lineHeight = 34.rsp,
            color = colors.textPrimary
        )
        Text(
            text = "Free, forever — no ads, ever. We hold your data to a stricter ethic than most apps bother with. Pro simply takes it one step further.",
            fontFamily = Geist,
            fontSize = 15.rsp,
            lineHeight = 22.rsp,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun TierSummaryRow(annual: PlanOffer?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.rdp)
    ) {
        TierColumn(
            label = "Free",
            amount = annual?.formattedPrice?.let { zeroLike(it) } ?: "Free",
            caption = "Forever",
            items = FreeHighlights,
            highlighted = false,
            icon = { TierGlyphBadge(pro = false, modifier = Modifier.padding(bottom = 6.rdp)) },
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        TierColumn(
            label = "Pro",
            amount = annual?.formattedPrice ?: "…",
            originalAmount = annual?.originalPrice,
            caption = "Per year",
            items = ProHighlights,
            highlighted = true,
            badge = if (annual?.originalPrice != null) "Launching price" else null,
            icon = { TierGlyphBadge(pro = true, modifier = Modifier.padding(bottom = 6.rdp)) },
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

/** A small leading mark on each tier card: an open ring for Free (the loop that stays free),
 * a sparkle for Pro (the reward accent already used on badges elsewhere on this page). */
@Composable
private fun TierGlyphBadge(pro: Boolean, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    val bg = if (pro) colors.rewardSurface else colors.surfaceTint
    val fg = if (pro) colors.accentReward else colors.textBrand
    Box(
        modifier = modifier.size(30.rdp).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (pro) SparkGlyph(fg, Modifier.size(15.rdp)) else RingGlyph(fg, Modifier.size(15.rdp))
    }
}

/** An open ring — the whole loop, still free. */
@Composable
private fun RingGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawCircle(color = color, radius = size.minDimension / 2f, style = Stroke(width = size.minDimension * 0.16f))
    }
}

/** A four-point sparkle — the reward mark, filled rather than stroked. */
@Composable
private fun SparkGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val path = Path().apply {
            moveTo(cx, 0f)
            quadraticTo(cx + w * 0.09f, cy - h * 0.09f, w, cy)
            quadraticTo(cx + w * 0.09f, cy + h * 0.09f, cx, h)
            quadraticTo(cx - w * 0.09f, cy + h * 0.09f, 0f, cy)
            quadraticTo(cx - w * 0.09f, cy - h * 0.09f, cx, 0f)
            close()
        }
        drawPath(path, color = color)
    }
}

/** "₹0" in the store's own currency format, derived from a real price string. */
private fun zeroLike(formatted: String): String {
    val prefix = formatted.takeWhile { !it.isDigit() }
    return "${prefix}0"
}

private fun PlanOffer.priceLine(): String {
    val unit = if (period == BillingPeriod.ANNUAL) "year" else "month"
    return when {
        freeTrialDays >= 28 -> "$formattedPrice/$unit, free for the first month. You'll get a reminder before it charges."
        freeTrialDays > 0 -> "$formattedPrice/$unit, free for $freeTrialDays days. You'll get a reminder before it charges."
        period == BillingPeriod.ANNUAL -> "$formattedPrice/year, charged today."
        else -> "$formattedPrice/month, charged today. $formattedYearTotal over a year."
    }
}

/** "Annual · ₹699/year, 30-day trial" — the plan's facts with no claim about charging. */
private fun PlanOffer.planLine(): String {
    val unit = if (period == BillingPeriod.ANNUAL) "year" else "month"
    val name = if (period == BillingPeriod.ANNUAL) "Annual" else "Monthly"
    return "$name · $formattedPrice/$unit" + if (freeTrialDays > 0) ", $freeTrialDays-day trial" else ""
}

@Composable
private fun PriceLine(text: String) {
    Text(
        text = text,
        fontFamily = Geist,
        fontSize = 13.rsp,
        lineHeight = 18.rsp,
        color = ZenTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Small reward-toned pill: "Save 22%", "Launching price" — always a real, store-derived claim. */
@Composable
private fun RewardBadge(text: String, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    val shape = RoundedCornerShape(6.rdp)
    Text(
        text = text.uppercase(),
        fontFamily = DepartureMono,
        fontSize = 10.rsp,
        letterSpacing = 0.5.sp,
        color = colors.accentReward,
        modifier = modifier
            .clip(shape)
            .background(colors.rewardSurface)
            .border(1.dp, colors.rewardSurfaceLine, shape)
            .padding(horizontal = 6.rdp, vertical = 2.rdp)
    )
}

@Composable
private fun TierColumn(
    label: String,
    amount: String,
    caption: String,
    items: List<String>,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    originalAmount: String? = null,
    badge: String? = null,
    icon: @Composable (() -> Unit)? = null
) {
    val colors = ZenTheme.colors
    val shape = RoundedCornerShape(20.rdp)
    Column(
        modifier = modifier
            .then(
                if (highlighted) Modifier.clip(shape).background(colors.surfaceTint).border(1.dp, colors.surfaceTintLine, shape)
                else Modifier.zenCard(shape)
            )
            .padding(14.rdp),
        verticalArrangement = Arrangement.spacedBy(6.rdp)
    ) {
        if (highlighted) OsRule(Modifier.padding(bottom = 4.rdp))
        icon?.invoke()
        ZenEyebrow(label, color = if (highlighted) colors.textBrand else colors.textSecondary)
        // Badge sits on its own line rather than sharing the eyebrow's row: "Launching price"
        // next to "PRO" doesn't fit a half-width card on narrow phones without wrapping mid-word.
        if (badge != null) RewardBadge(badge, modifier = Modifier.padding(top = 2.rdp))
        if (originalAmount != null) {
            Text(
                originalAmount,
                fontFamily = DepartureMono,
                fontSize = 13.rsp,
                color = colors.textMuted,
                textDecoration = TextDecoration.LineThrough,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(top = if (badge != null) 4.rdp else 0.dp)
            )
        }
        Text(
            amount,
            fontFamily = DepartureMono,
            fontSize = 22.rsp,
            lineHeight = 26.rsp,
            color = colors.textPrimary,
            maxLines = 1,
            softWrap = false
        )
        Text(caption.uppercase(), style = ZenTypography.monoLabel, color = colors.textMuted)
        Spacer(Modifier.height(2.rdp))
        items.forEach { item ->
            Text(
                text = item,
                fontFamily = Geist,
                fontSize = 13.rsp,
                lineHeight = 17.rsp,
                color = if (highlighted) colors.textOnTint else colors.textSecondary
            )
        }
    }
}

@Composable
private fun PlanOption(
    title: String,
    price: String,
    detail: String,
    selected: Boolean,
    onSelect: () -> Unit,
    originalPrice: String? = null,
    badge: String? = null
) {
    val colors = ZenTheme.colors
    val shape = RoundedCornerShape(16.rdp)
    val feedback = rememberZenFeedback()
    // One 0→1 value drives the whole selection: fill, ring, border weight and the radio dot,
    // which pops in on a spring slightly behind the ring.
    val on by animateFloatAsState(if (selected) 1f else 0f, ZenMotion.settle(), label = "planSelected")
    val dot by animateFloatAsState(if (selected) 1f else 0f, ZenMotion.bouncy(), label = "planDot")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.rdp)
            .graphicsLayer {
                val scale = 0.985f + 0.015f * on
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(lerp(colors.bgPrimary, colors.bgSecondary, on))
            .border((1f + on).dp, lerp(colors.borderSubtle, colors.textBrand, on), shape)
            .selectable(selected = selected, role = Role.RadioButton) {
                if (!selected) feedback.select()
                onSelect()
            }
            .padding(horizontal = 16.rdp, vertical = 14.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.rdp)
                .clip(CircleShape)
                .border(2.dp, lerp(colors.borderOutline, colors.textBrand, on), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (dot > 0.01f) {
                Box(
                    Modifier
                        .size(10.rdp)
                        .graphicsLayer {
                            scaleX = dot
                            scaleY = dot
                        }
                        .clip(CircleShape)
                        .background(colors.textBrand)
                )
            }
        }
        Spacer(Modifier.width(12.rdp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 16.rsp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                if (badge != null) {
                    RewardBadge(badge)
                    Spacer(Modifier.width(6.rdp))
                }
                if (originalPrice != null) {
                    Text(
                        originalPrice,
                        fontFamily = DepartureMono,
                        fontSize = 12.rsp,
                        color = colors.textMuted,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.padding(end = 4.rdp)
                    )
                }
                Text(price, fontFamily = DepartureMono, fontSize = 14.rsp, color = colors.textPrimary)
            }
            Text(detail, fontFamily = Geist, fontSize = 13.rsp, lineHeight = 18.rsp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.rdp))
        }
    }
}

/** The row-by-row diff. The Pro column carries a soft, continuous tint so the eye reads the
 * whole right-hand side as "what you get," not fourteen separate rows. */
@Composable
private fun FeatureComparisonTable(modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().zenCard(),
        verticalArrangement = Arrangement.spacedBy(0.rdp)
    ) {
        ZenEyebrow("Compare every feature", modifier = Modifier.padding(start = 14.rdp, end = 14.rdp, top = 14.rdp, bottom = 10.rdp))
        Row(Modifier.fillMaxWidth().padding(start = 14.rdp, end = 14.rdp, bottom = 8.rdp)) {
            Spacer(Modifier.weight(1.3f))
            Text(
                text = "FREE",
                style = ZenTypography.monoLabel,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "PRO",
                style = ZenTypography.monoLabel,
                color = colors.textBrand,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
        CompareRows.forEachIndexed { index, row ->
            CompareRowView(row)
            if (index != CompareRows.lastIndex) {
                Box(
                    Modifier.fillMaxWidth().height(1.dp)
                        .padding(start = 14.rdp)
                        .background(colors.borderHairlineSoft)
                )
            }
        }
        Spacer(Modifier.height(6.rdp))
    }
}

@Composable
private fun CompareRowView(row: CompareRow, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.label,
            fontFamily = Geist,
            fontSize = 13.rsp,
            lineHeight = 17.rsp,
            color = colors.textSecondary,
            modifier = Modifier.weight(1.3f).padding(top = 10.rdp, bottom = 10.rdp, start = 14.rdp, end = 6.rdp)
        )
        CompareCellView(row.free, tinted = false, modifier = Modifier.weight(1f).fillMaxHeight())
        CompareCellView(row.pro, tinted = true, modifier = Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun CompareCellView(cell: CompareCell, tinted: Boolean, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    Box(
        modifier = modifier
            .then(if (tinted) Modifier.background(colors.surfaceTint.copy(alpha = 0.30f)) else Modifier)
            .padding(vertical = 10.rdp, horizontal = 6.rdp),
        contentAlignment = Alignment.Center
    ) {
        when (cell.included) {
            true -> CheckBadge(tinted)
            false -> DashBadge()
            null -> Text(
                text = cell.text.orEmpty(),
                fontFamily = Geist,
                fontWeight = if (tinted) FontWeight.Medium else FontWeight.Normal,
                fontSize = 12.rsp,
                lineHeight = 15.rsp,
                textAlign = TextAlign.Center,
                color = if (tinted) colors.textOnTint else colors.textSecondary
            )
        }
    }
}

/** Drawn, not a text glyph: a Unicode "✓" depends on whatever font the ambient text style
 * inherits (this Box carries no fontFamily of its own) and, at 9sp inside a 16dp badge, was
 * landing clipped or missing on real devices — see [SparkGlyph] / [RingGlyph] for the same
 * drawn-not-typed approach used elsewhere on this page. */
@Composable
private fun CheckBadge(tinted: Boolean) {
    val colors = ZenTheme.colors
    Box(
        modifier = Modifier.size(16.rdp).clip(CircleShape).background(if (tinted) colors.textBrand else colors.surfaceTint),
        contentAlignment = Alignment.Center
    ) {
        CheckGlyph(if (tinted) colors.textOnBrand else colors.textOnTint, Modifier.size(8.rdp))
    }
}

@Composable
private fun CheckGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.02f, size.height * 0.55f)
            lineTo(size.width * 0.38f, size.height * 0.92f)
            lineTo(size.width * 0.98f, size.height * 0.12f)
        }
        drawPath(
            path,
            color = color,
            style = Stroke(width = size.minDimension * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun DashBadge() {
    val colors = ZenTheme.colors
    Box(
        modifier = Modifier.size(16.rdp).clip(CircleShape).border(1.dp, colors.borderOutline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(width = 7.rdp, height = 1.6.rdp)
                .background(colors.textMuted)
        )
    }
}

@Composable
private fun ComingSoonSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.rdp)) {
        ZenEyebrow("Coming soon, for Pro")
        ComingSoon.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.rdp)) {
                pair.forEach { item -> ComingSoonChip(item, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Dashed, not solid — visually marks these as the roadmap, not a shipped feature. */
@Composable
private fun ComingSoonChip(text: String, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    val radius = 12.rdp
    Box(
        modifier = modifier
            .drawBehind {
                val stroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f), 0f))
                drawRoundRect(color = colors.borderOutline, style = stroke, cornerRadius = CornerRadius(radius.toPx()))
            }
            .padding(horizontal = 12.rdp, vertical = 10.rdp)
    ) {
        Text(text, fontFamily = Geist, fontSize = 12.rsp, lineHeight = 16.rsp, color = colors.textSecondary)
    }
}

@Composable
private fun TrustSection(modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.rdp)
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
        Spacer(Modifier.height(8.rdp))
        ZenEyebrow("What Pro never does")
        listOf(
            "Your Zen Score is never a sales lever.",
            "No upsell on a low day.",
            "Nothing interrupts a declared session. Not even this.",
            "Pro is paid for with money, not attention."
        ).forEach { rule ->
            Row {
                Text("×", fontFamily = DepartureMono, fontSize = 14.rsp, color = colors.textMuted)
                Spacer(Modifier.width(10.rdp))
                Text(rule, fontFamily = Geist, fontSize = 14.rsp, lineHeight = 20.rsp, color = colors.textSecondary)
            }
        }
    }
}

/**
 * Pinned above the nav bar, always showing the price of whatever plan is picked above and the
 * one button that spends money — so neither is ever more than a glance away, however far the
 * comparison table has been scrolled.
 */
@Composable
private fun PlanFooter(
    chosen: PlanOffer?,
    isWorking: Boolean,
    isSimulated: Boolean,
    errorMessage: String?,
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgPrimary)
            .drawBehind {
                drawLine(
                    color = colors.borderSubtle,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = Spacing.screenMargin)
            .padding(top = 14.rdp, bottom = 10.rdp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.rdp)
    ) {
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                fontFamily = Geist,
                fontSize = 13.rsp,
                lineHeight = 18.rsp,
                color = colors.accentDeduct
            )
        }
        if (chosen != null) {
            // The line above the button re-states the price whenever the plan changes; it
            // rolls rather than cuts so the eye catches that it changed.
            AnimatedContent(
                // Early access charges nothing, so it states the plan, not a billing schedule.
                targetState = if (isSimulated) chosen.planLine() else chosen.priceLine(),
                transitionSpec = {
                    (fadeIn(ZenMotion.arrive()) + slideInVertically(ZenMotion.arrive()) { it / 2 })
                        .togetherWith(fadeOut(ZenMotion.leave()) + slideOutVertically(ZenMotion.leave()) { -it / 2 })
                },
                label = "footerPriceLine"
            ) { line -> PriceLine(line) }
        }
        ZenButton(
            text = if (isSimulated) "Unlock Pro" else chosen?.startLabel() ?: "Subscribe",
            onClick = onSubscribe,
            enabled = chosen != null && !isWorking
        )
        Text(
            text = if (isSimulated) "Early access: Pro unlocks on this device and nothing is charged."
            else "Cancelling takes the same two taps as starting.",
            fontFamily = Geist,
            fontSize = 12.rsp,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ConfirmSheet(
    offer: PlanOffer,
    isWorking: Boolean,
    isSimulated: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val trial = offer.freeTrialDays > 0
    val trialPhrase = if (offer.freeTrialDays >= 28) "a month" else "${offer.freeTrialDays} days"
    val unit = if (offer.period == BillingPeriod.ANNUAL) "year" else "month"
    ZenSheet(onDismiss = onDismiss) {
        ZenEyebrow(if (isSimulated) "Early access" else "Confirm")
        if (isSimulated) {
            // No store behind this build: say plainly that nothing is charged, so the receipt
            // below can't be read as a bill.
            ZenSheetTitle("Pro, free during early access")
            Column {
                ZenReceiptLine("Plan", "${offer.formattedPrice}/$unit")
                if (trial) ZenReceiptLine("Free trial", trialPhrase.replaceFirstChar { it.uppercase() })
                ZenReceiptLine("Charged", "Nothing", showDivider = false)
            }
            ZenSheetBody(
                "Pro unlocks on this device right away. No payment details, no charge. " +
                    "When paid plans open we'll ask first; nothing moves to a paid plan on its own."
            )
        } else {
            ZenSheetTitle(
                if (trial) "Free for $trialPhrase, then ${offer.formattedPrice}/$unit"
                else "${offer.formattedPrice}/$unit, starting today"
            )
            Column {
                ZenReceiptLine("Today", if (trial) zeroLike(offer.formattedPrice) else offer.formattedPrice)
                ZenReceiptLine(if (trial) "After the trial" else "Next charge", offer.formattedPrice)
                ZenReceiptLine("Then", if (offer.period == BillingPeriod.ANNUAL) "Once a year" else "Every month", showDivider = false)
            }
            ZenSheetBody(
                (if (trial) "A notice arrives before the trial ends. No silent charge. " else "") +
                    "Cancel any time: Settings → ZenMode Pro → Cancel. Two taps, the same two as this."
            )
            PriceLine(offer.priceLine())
        }
        ZenButton(
            text = when {
                isWorking -> if (isSimulated) "Unlocking Pro…" else "One moment…"
                isSimulated -> "Unlock Pro"
                else -> offer.startLabel()
            },
            onClick = onConfirm,
            enabled = !isWorking
        )
        ZenButton(text = "Back", onClick = onDismiss, style = ZenButtonStyle.Ghost, enabled = !isWorking)
    }
}

/** Invite-only server grant, no subscription behind it: nothing to cancel or renew. */
@Composable
private fun GrantedPro(entitlement: Entitlement) {
    val colors = ZenTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().zenCard().padding(16.rdp),
        verticalArrangement = Arrangement.spacedBy(6.rdp)
    ) {
        OsRule()
        Spacer(Modifier.height(6.rdp))
        ZenEyebrow("Pro supporter" + (entitlement.since?.let { " · since ${it.asZenMonth()}" } ?: ""))
        Text(
            text = "Early access",
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.rsp,
            lineHeight = 28.rsp,
            color = colors.textPrimary
        )
        Text(
            text = "You're in on invite-only early access — nothing to manage or pay here.",
            fontFamily = Geist,
            fontSize = 14.rsp,
            lineHeight = 20.rsp,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun ManagePro(
    entitlement: Entitlement,
    offers: List<PlanOffer>,
    isWorking: Boolean,
    isSimulated: Boolean,
    onCancel: () -> Unit,
    onResume: () -> Unit
) {
    val colors = ZenTheme.colors
    var confirmCancel by remember { mutableStateOf(false) }
    val ending = entitlement.status == ProStatus.ENDING
    val offer = entitlement.period?.let { offers.offer(it) }
    val unit = if (entitlement.period == BillingPeriod.MONTHLY) "month" else "year"

    Column(
        modifier = Modifier.fillMaxWidth().staggeredEntrance(0).zenCard().padding(16.rdp),
        verticalArrangement = Arrangement.spacedBy(6.rdp)
    ) {
        OsRule()
        Spacer(Modifier.height(6.rdp))
        ZenEyebrow("Pro supporter" + (entitlement.since?.let { " · since ${it.asZenMonth()}" } ?: ""))
        Text(
            text = buildString {
                append(if (entitlement.period == BillingPeriod.MONTHLY) "Monthly" else "Annual")
                if (offer != null) append(" · ${offer.formattedPrice}/$unit")
            },
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.rsp,
            lineHeight = 28.rsp,
            color = colors.textPrimary
        )
        Column {
            ZenReceiptLine(
                "Status",
                when (entitlement.status) {
                    ProStatus.TRIAL -> "Free trial"
                    ProStatus.ENDING -> "Ending"
                    else -> "Active"
                }
            )
            when (entitlement.status) {
                ProStatus.TRIAL -> ZenReceiptLine(
                    if (isSimulated) "Trial ends" else "First charge",
                    entitlement.trialEndsOn?.asZenDate() ?: "-"
                )
                ProStatus.ENDING -> ZenReceiptLine("Pro until", entitlement.endsOn?.asZenDate() ?: "-")
                else -> ZenReceiptLine("Renews", entitlement.renewsOn?.asZenDate() ?: "-")
            }
            ZenReceiptLine(
                "Then",
                when {
                    ending -> "Back to free"
                    isSimulated -> "No charge, early access"
                    else -> offer?.let { "${it.formattedPrice} a $unit" } ?: "-"
                },
                showDivider = false
            )
        }
    }

    Text(
        text = when {
            isSimulated && ending ->
                "You're on early access, so nothing was charged. Resume any time before Pro ends."
            isSimulated ->
                "You're on early access: nothing is charged. When paid plans open we'll ask " +
                    "first, and your Pro carries on until then."
            // Real billing: Play, not this screen, is the source of truth for cancel/resume --
            // no app can act on a subscription on the user's behalf, only Play's own UI can.
            else ->
                "Renewals, cancellations and refunds are all managed in the Play Store, not " +
                    "here. Changes made there can take a few minutes to show up in ZenMode."
        },
        fontFamily = Geist,
        fontSize = 15.rsp,
        lineHeight = 22.rsp,
        color = colors.textSecondary
    )

    if (isSimulated) {
        if (ending) {
            ZenButton(text = if (isWorking) "Resuming…" else "Resume Pro", onClick = onResume, enabled = !isWorking)
        } else {
            ZenButton(
                text = if (isWorking) "Cancelling…" else "Cancel Pro",
                onClick = { confirmCancel = true },
                style = ZenButtonStyle.Outline,
                enabled = !isWorking
            )
        }
    } else {
        // One button either way: cancel and resume are the same Play Store screen, and we
        // can't promise the tap did anything until Play's own RTDN reconciles state back to us.
        ZenButton(
            text = "Manage in Play Store",
            onClick = if (ending) onResume else onCancel,
            style = ZenButtonStyle.Outline,
            enabled = !isWorking
        )
    }

    if (isSimulated && confirmCancel) {
        val until = (if (entitlement.status == ProStatus.TRIAL) entitlement.trialEndsOn else entitlement.renewsOn)
            ?.asZenDate()
        ZenSheet(onDismiss = { confirmCancel = false }) {
            ZenSheetTitle("Cancel Pro?")
            ZenSheetBody(
                "Pro runs until ${until ?: "the end of this period"}, then the app goes back to free. " +
                    "The launcher, intent, sessions, Zen Score, your daily report and Zen Circle all stay."
            )
            ZenSheetBody("History past 7 days stops showing. None of it is deleted.")
            // Same weight on purpose: no retention offer, no "are you sure" chain.
            Row(horizontalArrangement = Arrangement.spacedBy(8.rdp)) {
                ZenButton(
                    text = "Cancel Pro",
                    onClick = {
                        confirmCancel = false
                        onCancel()
                    },
                    style = ZenButtonStyle.Outline,
                    modifier = Modifier.weight(1f)
                )
                ZenButton(
                    text = "Keep Pro",
                    onClick = { confirmCancel = false },
                    style = ZenButtonStyle.Outline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * The OS gradient as a 3dp rule. A reward mark on Pro surfaces only — never a background.
 * The colours drift slowly along it (one pass every 6s) so it reads as alive, not as a border.
 */
@Composable
internal fun OsRule(modifier: Modifier = Modifier) {
    val stops = listOf(
        colorResource(R.color.ember_500),
        colorResource(R.color.amber_500),
        colorResource(R.color.zen_700)
    )
    val still = rememberReduceMotion() || LocalInspectionMode.current
    val drift = rememberInfiniteTransition(label = "osRule")
    val shift = drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "osRuleShift"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.rdp)
            .clip(RoundedCornerShape(3.rdp))
            .drawBehind {
                val w = size.width
                val offset = if (still) 0f else shift.value * w * 2f
                drawRect(
                    Brush.linearGradient(
                        colors = stops,
                        start = Offset(offset - w, 0f),
                        end = Offset(offset, 0f),
                        tileMode = TileMode.Mirror
                    )
                )
            }
    )
}
