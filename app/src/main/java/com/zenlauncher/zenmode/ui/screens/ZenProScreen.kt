package com.zenlauncher.zenmode.ui.screens

import androidx.annotation.StringRes
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
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
import com.zenlauncher.zenmode.R
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

/**
 * A line of copy: the resource that holds the words and the arguments it needs.
 *
 * The screen resolves it with [CopyLine.resolve]; the copy test resolves the same
 * resource from res/values/strings.xml, so the sentence stays pinned by a test.
 */
data class CopyLine(@StringRes val res: Int, val args: List<Any> = emptyList())

/** Short, card-sized highlights — the at-a-glance read. The full row-by-row diff is
 *  on the compare table further down the page. */
@Composable
private fun freeHighlights(): List<String> = listOf(
    stringResource(R.string.zen_pro_highlight_free_launcher),
    stringResource(R.string.zen_pro_highlight_free_zen_score),
    stringResource(R.string.zen_pro_highlight_free_zen_circle),
    stringResource(R.string.zen_pro_highlight_free_history),
    stringResource(R.string.zen_pro_highlight_free_search)
)

/** What the extra money buys, in the same card-sized lines. */
@Composable
private fun proHighlights(): List<String> = listOf(
    stringResource(R.string.zen_pro_highlight_pro_intro),
    stringResource(R.string.zen_pro_highlight_pro_history),
    stringResource(R.string.zen_pro_highlight_pro_random_connect),
    stringResource(R.string.zen_pro_highlight_pro_blocker),
    stringResource(R.string.zen_pro_highlight_pro_export)
)

fun List<PlanOffer>.offer(period: BillingPeriod): PlanOffer? = firstOrNull { it.period == period }

/** "₹699/year, or ₹75/month" — null until the store has answered. */
fun List<PlanOffer>.priceSummaryCopy(): CopyLine? {
    val annual = offer(BillingPeriod.ANNUAL) ?: return null
    val monthly = offer(BillingPeriod.MONTHLY)
        ?: return CopyLine(R.string.zen_pro_price_annual, listOf(annual.formattedPrice))
    return CopyLine(
        R.string.zen_pro_price_summary_both,
        listOf(annual.formattedPrice, monthly.formattedPrice)
    )
}

/** The same line, resolved for the screen. */
@Composable
fun List<PlanOffer>.priceSummary(): String? = priceSummaryCopy()?.resolve()

/** The price as the screen shows it: "₹699/year" or "₹75/month". */
private fun PlanOffer.priceCopy(): CopyLine =
    if (period == BillingPeriod.ANNUAL) {
        CopyLine(R.string.zen_pro_price_annual, listOf(formattedPrice))
    } else {
        CopyLine(R.string.zen_pro_price_monthly, listOf(formattedPrice))
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
@Composable
private fun PlanOffer.startLabel(): String = when {
    freeTrialDays <= 0 -> stringResource(R.string.zen_pro_subscribe_button)
    freeTrialDays >= 28 -> stringResource(R.string.zen_pro_start_free_month)
    else -> stringResource(R.string.zen_pro_start_free_trial, freeTrialDays)
}

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val MonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

fun Long.asZenDate(): String = DateFormat.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
fun Long.asZenMonth(): String = MonthFormat.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

/**
 * One line under the plan card / manage header, e.g. "Renews 17 Oct 2027". With [isSimulated]
 * no price is ever quoted as upcoming, since nothing will be charged.
 */
fun Entitlement.statusLineCopy(offers: List<PlanOffer>, isSimulated: Boolean = false): CopyLine {
    val price = period?.takeUnless { isSimulated }?.let { offers.offer(it) }?.priceCopy()
    val trialEnd = trialEndsOn?.asZenDate()
    val renews = renewsOn?.asZenDate()
    val ends = endsOn?.asZenDate()
    return when (status) {
        // Server-granted Pro has no subscription behind it.
        ProStatus.FREE -> CopyLine(R.string.zen_pro_status_free)
        ProStatus.TRIAL -> when {
            trialEnd == null && price == null -> CopyLine(R.string.zen_pro_status_trial_soon)
            trialEnd == null -> CopyLine(R.string.zen_pro_status_trial_soon_then_price, listOf(price!!))
            price == null -> CopyLine(R.string.zen_pro_status_trial, listOf(trialEnd))
            else -> CopyLine(R.string.zen_pro_status_trial_then_price, listOf(trialEnd, price))
        }
        ProStatus.ACTIVE -> when {
            isSimulated -> CopyLine(R.string.zen_pro_status_active_simulated)
            renews == null && price == null -> CopyLine(R.string.zen_pro_status_active_auto)
            renews == null -> CopyLine(R.string.zen_pro_status_active_auto_then_price, listOf(price!!))
            price == null -> CopyLine(R.string.zen_pro_status_active, listOf(renews))
            else -> CopyLine(R.string.zen_pro_status_active_then_price, listOf(renews, price))
        }
        ProStatus.ENDING -> if (ends == null) {
            CopyLine(R.string.zen_pro_status_ending_period)
        } else {
            CopyLine(R.string.zen_pro_status_ending, listOf(ends))
        }
    }
}

/** The same line, resolved for the screen. */
@Composable
fun Entitlement.statusLine(offers: List<PlanOffer>, isSimulated: Boolean = false): String =
    statusLineCopy(offers, isSimulated).resolve()

/** Resolve the line into the words the user reads. */
@Composable
fun CopyLine.resolve(): String {
    val resolved = args.map { if (it is CopyLine) it.resolve() else it }
    return if (resolved.isEmpty()) stringResource(res) else stringResource(res, *resolved.toTypedArray())
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
            title = if (isPro) stringResource(R.string.zen_pro_manage_title) else stringResource(R.string.zen_pro_product_title, PRODUCT_NAME),
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
                ZenEyebrow(stringResource(R.string.zen_pro_pick_a_plan))
                if (annual != null) {
                    PlanOption(
                        title = stringResource(R.string.zen_pro_plan_annual),
                        originalPrice = annual.originalPrice?.let { stringResource(R.string.zen_pro_price_annual, it) },
                        price = stringResource(R.string.zen_pro_price_annual, annual.formattedPrice),
                        detail = stringResource(R.string.zen_pro_detail_annual, annual.formattedPerMonth) +
                            if (annual.freeTrialDays > 0) stringResource(R.string.zen_pro_detail_first_month_free) else "",
                        selected = picked == BillingPeriod.ANNUAL,
                        badge = annual.discountPercent()?.let { stringResource(R.string.zen_pro_badge_percent_off, it) },
                        onSelect = { picked = BillingPeriod.ANNUAL }
                    )
                }
                if (monthly != null) {
                    PlanOption(
                        title = stringResource(R.string.common_monthly),
                        price = stringResource(R.string.zen_pro_price_monthly, monthly.formattedPrice),
                        detail = stringResource(R.string.zen_pro_detail_monthly, monthly.formattedYearTotal) +
                            if (monthly.freeTrialDays > 0) stringResource(R.string.zen_pro_detail_free_trial, monthly.freeTrialDays) else "",
                        selected = picked == BillingPeriod.MONTHLY,
                        onSelect = { picked = BillingPeriod.MONTHLY }
                    )
                }
                if (offers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.zen_pro_loading_prices),
                        fontFamily = Geist,
                        fontSize = 14.rsp,
                        color = ZenTheme.colors.textSecondary
                    )
                }
            }

            // Promo codes are redeemed inside Google Play's checkout, so there's nothing to
            // redeem against in early access (no store behind it).
            if (!isSimulated) RedeemCodeCard(Modifier.staggeredEntrance(3))
            FeatureComparisonTable(Modifier.staggeredEntrance(4))
            ComingSoonSection(Modifier.staggeredEntrance(5))
            TrustSection(Modifier.staggeredEntrance(6))
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
            text = stringResource(R.string.zen_pro_hero_headline),
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 28.rsp,
            lineHeight = 34.rsp,
            color = colors.textPrimary
        )
        Text(
            text = stringResource(R.string.zen_pro_hero_subtext),
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
            label = stringResource(R.string.common_free),
            amount = annual?.formattedPrice?.let { zeroLike(it) } ?: stringResource(R.string.common_free),
            caption = stringResource(R.string.zen_pro_tier_free_caption),
            items = freeHighlights(),
            highlighted = false,
            icon = { TierGlyphBadge(pro = false, modifier = Modifier.padding(bottom = 6.rdp)) },
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        TierColumn(
            label = stringResource(R.string.zen_pro_tier_pro_label),
            amount = annual?.formattedPrice ?: "…",
            originalAmount = annual?.originalPrice,
            caption = stringResource(R.string.zen_pro_tier_pro_caption),
            items = proHighlights(),
            highlighted = true,
            badge = if (annual?.originalPrice != null) stringResource(R.string.zen_pro_launching_price_badge) else null,
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

@Composable
private fun PlanOffer.priceLine(): String {
    val unit = if (period == BillingPeriod.ANNUAL) {
        stringResource(R.string.common_unit_year)
    } else {
        stringResource(R.string.common_unit_month)
    }
    return when {
        freeTrialDays >= 28 -> stringResource(
            R.string.zen_pro_price_line_free_month,
            formattedPrice,
            unit
        )
        freeTrialDays > 0 -> stringResource(
            R.string.zen_pro_price_line_free_days,
            formattedPrice,
            unit,
            freeTrialDays
        )
        period == BillingPeriod.ANNUAL ->
            stringResource(R.string.zen_pro_price_line_annual_today, formattedPrice)
        else -> stringResource(
            R.string.zen_pro_price_line_monthly_today,
            formattedPrice,
            formattedYearTotal
        )
    }
}

/** "Annual · ₹699/year, 30-day trial" — the plan's facts with no claim about charging. */
@Composable
private fun PlanOffer.planLine(): String {
    val unit = if (period == BillingPeriod.ANNUAL) {
        stringResource(R.string.common_unit_year)
    } else {
        stringResource(R.string.common_unit_month)
    }
    val name = if (period == BillingPeriod.ANNUAL) {
        stringResource(R.string.zen_pro_plan_annual)
    } else {
        stringResource(R.string.common_monthly)
    }
    return if (freeTrialDays > 0) {
        stringResource(R.string.zen_pro_plan_line_trial, name, formattedPrice, unit, freeTrialDays)
    } else {
        stringResource(R.string.zen_pro_plan_line, name, formattedPrice, unit)
    }
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

/** "Got a code?" — opens the zenmodeos.com guide to redeeming a promo code at Play checkout. */
@Composable
private fun RedeemCodeCard(modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    val uriHandler = LocalUriHandler.current
    val shape = RoundedCornerShape(20.rdp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touchTarget)
            .zenCard(shape)
            .clickable(onClickLabel = stringResource(R.string.zen_pro_redeem_code_action_label), role = Role.Button) {
                uriHandler.openUri(AppConstants.REDEEM_CODE_URL)
            }
            .padding(horizontal = 16.rdp, vertical = 14.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.rdp).clip(CircleShape).background(colors.accentReward))
        Spacer(Modifier.width(12.rdp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.zen_pro_redeem_code_title), fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 14.rsp, color = colors.textPrimary)
            Text(
                stringResource(R.string.zen_pro_redeem_code_body),
                fontFamily = Geist,
                fontSize = 13.rsp,
                lineHeight = 18.rsp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.rdp)
            )
        }
        Spacer(Modifier.width(10.rdp))
        Text(stringResource(R.string.zen_pro_redeem_code_cta), style = ZenTypography.monoLabel, color = colors.textBrand)
    }
}

/** The promises the Pro page makes. Each one is a rule the app keeps. */
@Composable
private fun trustRules(): List<String> = listOf(
    stringResource(R.string.zen_pro_trust_rule_score),
    stringResource(R.string.zen_pro_trust_rule_upsell),
    stringResource(R.string.zen_pro_trust_rule_session),
    stringResource(R.string.zen_pro_trust_rule_money)
)

@Composable
private fun TrustSection(modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.rdp)
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
        Spacer(Modifier.height(8.rdp))
        ZenEyebrow(stringResource(R.string.zen_pro_trust_eyebrow))
        trustRules().forEach { rule ->
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
            text = if (isSimulated) stringResource(R.string.common_unlock_pro)
            else if (chosen != null) chosen.startLabel()
            else stringResource(R.string.zen_pro_subscribe_button),
            onClick = onSubscribe,
            enabled = chosen != null && !isWorking
        )
        Text(
            text = if (isSimulated) stringResource(R.string.zen_pro_footer_early_access_note)
            else stringResource(R.string.zen_pro_footer_cancel_note),
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
    val trialPhrase = if (offer.freeTrialDays >= 28) stringResource(R.string.zen_pro_trial_phrase_month) else stringResource(R.string.zen_pro_trial_phrase_days, offer.freeTrialDays)
    val unit = if (offer.period == BillingPeriod.ANNUAL) stringResource(R.string.common_unit_year) else stringResource(R.string.common_unit_month)
    ZenSheet(onDismiss = onDismiss) {
        ZenEyebrow(if (isSimulated) stringResource(R.string.common_early_access) else stringResource(R.string.zen_pro_confirm_eyebrow_confirm))
        if (isSimulated) {
            // No store behind this build: say plainly that nothing is charged, so the receipt
            // below can't be read as a bill.
            ZenSheetTitle(stringResource(R.string.zen_pro_sheet_title_early_access))
            Column {
                ZenReceiptLine(stringResource(R.string.zen_pro_receipt_plan_label), stringResource(R.string.zen_pro_receipt_plan_value, offer.formattedPrice, unit))
                if (trial) ZenReceiptLine(stringResource(R.string.common_free_trial), trialPhrase.replaceFirstChar { it.uppercase() })
                ZenReceiptLine(stringResource(R.string.zen_pro_receipt_charged_label), stringResource(R.string.zen_pro_receipt_nothing_value), showDivider = false)
            }
            ZenSheetBody(
                stringResource(R.string.zen_pro_sheet_body_unlocks) +
                    stringResource(R.string.zen_pro_sheet_body_paid_plans)
            )
        } else {
            ZenSheetTitle(
                if (trial) stringResource(R.string.zen_pro_sheet_title_free_trial, trialPhrase, offer.formattedPrice, unit)
                else stringResource(R.string.zen_pro_sheet_title_starting_today, offer.formattedPrice, unit)
            )
            Column {
                ZenReceiptLine(stringResource(R.string.zen_pro_receipt_today_label), if (trial) zeroLike(offer.formattedPrice) else offer.formattedPrice)
                ZenReceiptLine(if (trial) stringResource(R.string.zen_pro_receipt_after_trial_label) else stringResource(R.string.zen_pro_receipt_next_charge_label), offer.formattedPrice)
                ZenReceiptLine(stringResource(R.string.common_then), if (offer.period == BillingPeriod.ANNUAL) stringResource(R.string.zen_pro_receipt_once_a_year) else stringResource(R.string.zen_pro_receipt_every_month), showDivider = false)
            }
            ZenSheetBody(
                (if (trial) stringResource(R.string.zen_pro_sheet_body_trial_notice) else "") +
                    stringResource(R.string.zen_pro_sheet_body_cancel_anytime, PRODUCT_NAME)
            )
            PriceLine(offer.priceLine())
        }
        ZenButton(
            text = when {
                isWorking -> if (isSimulated) stringResource(R.string.zen_pro_unlocking_pro) else stringResource(R.string.zen_pro_one_moment)
                isSimulated -> stringResource(R.string.common_unlock_pro)
                else -> offer.startLabel()
            },
            onClick = onConfirm,
            enabled = !isWorking
        )
        ZenButton(text = stringResource(R.string.common_back), onClick = onDismiss, style = ZenButtonStyle.Ghost, enabled = !isWorking)
    }
}
