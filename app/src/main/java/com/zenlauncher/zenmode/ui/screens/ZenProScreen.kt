package com.zenlauncher.zenmode.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.services.BillingPeriod
import com.zenlauncher.zenmode.coreapi.services.Entitlement
import com.zenlauncher.zenmode.coreapi.services.PlanOffer
import com.zenlauncher.zenmode.coreapi.services.ProStatus
import com.zenlauncher.zenmode.ui.components.GlyphKind
import com.zenlauncher.zenmode.ui.components.ZenMotion
import com.zenlauncher.zenmode.ui.components.ZenProTag
import com.zenlauncher.zenmode.ui.components.rememberReduceMotion
import com.zenlauncher.zenmode.ui.components.rememberZenFeedback
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
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

// ZenMode Pro — plan page, confirm sheet, manage and cancel.
//
// Sales rules (from the plans spec) this file must keep:
//  · price above every button, read from the store, never hardcoded
//  · the free tier is listed in full before the price
//  · cancelling takes the same two taps as starting; both cancel-dialog buttons weigh the same
//  · nothing here ever references the Zen Score

/** Where the Pro page was opened from. Analytics only — the page itself never changes by entry. */
enum class ProEntry(val analyticsName: String) {
    PLAN_CARD("plan_card"),
    GATE("gate"),
    MANAGE("manage")
}

private val FreeIncludes = listOf(
    "The launcher",
    "Intent before an app opens",
    "Declared sessions",
    "Zen Score",
    "Daily Zen Report",
    "One partner",
    "7 days of history"
)

private val ProAdds = listOf(
    "Everything in free",
    "Full history",
    "Weekly and monthly reports",
    "Up to three partners",
    "Custom session lengths",
    "Home-screen themes",
    "Data export",
    "Your name in the supporters list"
)

fun List<PlanOffer>.offer(period: BillingPeriod): PlanOffer? = firstOrNull { it.period == period }

/** "₹699/year, or ₹75/month" — null until the store has answered. */
fun List<PlanOffer>.priceSummary(): String? {
    val annual = offer(BillingPeriod.ANNUAL) ?: return null
    val monthly = offer(BillingPeriod.MONTHLY) ?: return "${annual.formattedPrice}/year"
    return "${annual.formattedPrice}/year, or ${monthly.formattedPrice}/month"
}

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val MonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

fun Long.asZenDate(): String = DateFormat.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
fun Long.asZenMonth(): String = MonthFormat.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

/** One line under the plan card / manage header, e.g. "Renews 17 Oct 2027". */
fun Entitlement.statusLine(offers: List<PlanOffer>): String {
    val price = period?.let { offers.offer(it) }
        ?.let { if (it.period == BillingPeriod.ANNUAL) "${it.formattedPrice}/year" else "${it.formattedPrice}/month" }
    return when (status) {
        ProStatus.FREE -> ""
        ProStatus.TRIAL -> buildString {
            append("Free month ends ${trialEndsOn?.asZenDate() ?: "soon"}")
            if (price != null) append(", then $price")
            append(".")
        }
        ProStatus.ACTIVE -> buildString {
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
    errorMessage: String?,
    onBackClick: () -> Unit,
    onPurchase: (BillingPeriod) -> Unit,
    onCancel: () -> Unit,
    onResume: () -> Unit
) {
    val colors = ZenTheme.colors
    val feedback = rememberZenFeedback()
    LaunchedEffect(errorMessage) { if (errorMessage != null) feedback.error() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ProTopBar(
            title = if (isPro) "Manage Pro" else "ZenMode Pro",
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin)
                .padding(top = 4.rdp, bottom = 40.rdp),
            verticalArrangement = Arrangement.spacedBy(24.rdp)
        ) {
            when {
                // A real subscription (trial/active/ending) has billing to manage.
                entitlement.isPro -> ManagePro(entitlement, offers, isWorking, onCancel, onResume)
                // Server-granted only: they're Pro, but there's no subscription behind it.
                isPro -> GrantedPro(entitlement)
                else -> PlanPage(offers, isWorking, onPurchase)
            }
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
    onPurchase: (BillingPeriod) -> Unit
) {
    val colors = ZenTheme.colors
    var picked by rememberSaveable { mutableStateOf(BillingPeriod.ANNUAL) }
    var confirming by remember { mutableStateOf(false) }
    val annual = offers.offer(BillingPeriod.ANNUAL)
    val monthly = offers.offer(BillingPeriod.MONTHLY)
    val chosen = offers.offer(picked)
    val trial = (chosen?.freeTrialDays ?: 0) > 0

    Column(
        modifier = Modifier.staggeredEntrance(0),
        verticalArrangement = Arrangement.spacedBy(12.rdp)
    ) {
        OsRule(Modifier.width(48.rdp))
        Text(
            text = "Two of us built this. Pro is how it keeps going.",
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 28.rsp,
            lineHeight = 34.rsp,
            color = colors.textPrimary
        )
        Text(
            text = "Kamal designs it, Srinivas writes the code. Pro pays the server bill, so the app never has to sell your attention to anyone else. The whole loop is free, forever. Pro adds range.",
            fontFamily = Geist,
            fontSize = 15.rsp,
            lineHeight = 22.rsp,
            color = colors.textSecondary
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).staggeredEntrance(1),
        horizontalArrangement = Arrangement.spacedBy(10.rdp)
    ) {
        TierColumn(
            label = "Free",
            amount = annual?.formattedPrice?.let { zeroLike(it) } ?: "Free",
            caption = "Forever",
            items = FreeIncludes,
            highlighted = false,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        TierColumn(
            label = "Pro",
            amount = annual?.formattedPrice ?: "…",
            caption = "Per year",
            items = ProAdds,
            highlighted = true,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }

    Column(
        modifier = Modifier.selectableGroup().staggeredEntrance(2),
        verticalArrangement = Arrangement.spacedBy(8.rdp)
    ) {
        ZenEyebrow("Pick a plan")
        if (annual != null) {
            PlanOption(
                title = "Annual",
                price = "${annual.formattedPrice}/year",
                detail = "${annual.formattedPerMonth} a month, billed once a year." +
                    if (annual.freeTrialDays > 0) " First month free." else "",
                selected = picked == BillingPeriod.ANNUAL,
                onSelect = { picked = BillingPeriod.ANNUAL }
            )
        }
        if (monthly != null) {
            PlanOption(
                title = "Monthly",
                price = "${monthly.formattedPrice}/month",
                detail = "${monthly.formattedYearTotal} over a year." +
                    if (monthly.freeTrialDays == 0) " No trial." else "",
                selected = picked == BillingPeriod.MONTHLY,
                onSelect = { picked = BillingPeriod.MONTHLY }
            )
        }
        if (offers.isEmpty()) {
            Text(
                text = "Loading prices from Google Play…",
                fontFamily = Geist,
                fontSize = 14.rsp,
                color = colors.textSecondary
            )
        }
    }

    Column(
        modifier = Modifier.staggeredEntrance(3),
        verticalArrangement = Arrangement.spacedBy(10.rdp)
    ) {
        if (chosen != null) {
            // The line under the button re-states the price whenever the plan changes; it
            // rolls rather than cuts so the eye catches that it changed.
            AnimatedContent(
                targetState = chosen.priceLine(),
                transitionSpec = {
                    (fadeIn(ZenMotion.arrive()) + slideInVertically(ZenMotion.arrive()) { it / 2 })
                        .togetherWith(fadeOut(ZenMotion.leave()) + slideOutVertically(ZenMotion.leave()) { -it / 2 })
                },
                label = "priceLine"
            ) { line -> PriceLine(line) }
        }
        ZenButton(
            text = if (trial) "Start the free month" else "Subscribe",
            onClick = { confirming = true },
            enabled = chosen != null && !isWorking
        )
        Text(
            text = "Cancelling takes the same two taps as starting.",
            fontFamily = Geist,
            fontSize = 13.rsp,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().staggeredEntrance(4),
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

    if (confirming && chosen != null) {
        ConfirmSheet(
            offer = chosen,
            isWorking = isWorking,
            onConfirm = {
                confirming = false
                onPurchase(chosen.period)
            },
            onDismiss = { confirming = false }
        )
    }
}

/** "₹0" in the store's own currency format, derived from a real price string. */
private fun zeroLike(formatted: String): String {
    val prefix = formatted.takeWhile { !it.isDigit() }
    return "${prefix}0"
}

private fun PlanOffer.priceLine(): String = when {
    period == BillingPeriod.ANNUAL && freeTrialDays > 0 ->
        "$formattedPrice/year, free for the first month. You'll get a reminder before it charges."
    period == BillingPeriod.ANNUAL -> "$formattedPrice/year, charged today."
    else -> "$formattedPrice/month, charged today. $formattedYearTotal over a year."
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

@Composable
private fun TierColumn(
    label: String,
    amount: String,
    caption: String,
    items: List<String>,
    highlighted: Boolean,
    modifier: Modifier = Modifier
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
        ZenEyebrow(label, color = if (highlighted) colors.textBrand else colors.textSecondary)
        Text(amount, fontFamily = DepartureMono, fontSize = 24.rsp, lineHeight = 28.rsp, color = colors.textPrimary)
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
    onSelect: () -> Unit
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
                Text(price, fontFamily = DepartureMono, fontSize = 14.rsp, color = colors.textPrimary)
            }
            Text(detail, fontFamily = Geist, fontSize = 13.rsp, lineHeight = 18.rsp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.rdp))
        }
    }
}

@Composable
private fun ConfirmSheet(
    offer: PlanOffer,
    isWorking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val trial = offer.period == BillingPeriod.ANNUAL && offer.freeTrialDays > 0
    val unit = if (offer.period == BillingPeriod.ANNUAL) "year" else "month"
    ZenSheet(onDismiss = onDismiss) {
        ZenEyebrow("Confirm")
        ZenSheetTitle(
            if (trial) "Free for a month, then ${offer.formattedPrice}/$unit"
            else "${offer.formattedPrice}/$unit, starting today"
        )
        Column {
            ZenReceiptLine("Today", if (trial) zeroLike(offer.formattedPrice) else offer.formattedPrice)
            ZenReceiptLine(if (trial) "After the free month" else "Next charge", offer.formattedPrice)
            ZenReceiptLine("Then", if (offer.period == BillingPeriod.ANNUAL) "Once a year" else "Every month", showDivider = false)
        }
        ZenSheetBody(
            (if (trial) "A notice arrives three days before the free month ends. No silent charge. " else "") +
                "Cancel any time: Settings → ZenMode Pro → Cancel. Two taps, the same two as this."
        )
        PriceLine(offer.priceLine())
        ZenButton(
            text = if (trial) "Start the free month" else "Subscribe",
            onClick = onConfirm,
            enabled = !isWorking
        )
        ZenButton(text = "Back", onClick = onDismiss, style = ZenButtonStyle.Ghost)
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
                    ProStatus.TRIAL -> "Free month"
                    ProStatus.ENDING -> "Ending"
                    else -> "Active"
                }
            )
            when (entitlement.status) {
                ProStatus.TRIAL -> ZenReceiptLine("First charge", entitlement.trialEndsOn?.asZenDate() ?: "-")
                ProStatus.ENDING -> ZenReceiptLine("Pro until", entitlement.endsOn?.asZenDate() ?: "-")
                else -> ZenReceiptLine("Renews", entitlement.renewsOn?.asZenDate() ?: "-")
            }
            ZenReceiptLine(
                "Then",
                if (ending) "Back to free" else offer?.let { "${it.formattedPrice} a $unit" } ?: "-",
                showDivider = false
            )
        }
    }

    Text(
        text = "Thank you. This is the server bill. If we ever break one of our own sales rules, it goes in the changelog.",
        fontFamily = Geist,
        fontSize = 15.rsp,
        lineHeight = 22.rsp,
        color = colors.textSecondary
    )

    if (ending) {
        ZenButton(text = "Resume Pro", onClick = onResume, enabled = !isWorking)
    } else {
        ZenButton(text = "Cancel Pro", onClick = { confirmCancel = true }, style = ZenButtonStyle.Outline, enabled = !isWorking)
    }

    if (confirmCancel) {
        val until = (if (entitlement.status == ProStatus.TRIAL) entitlement.trialEndsOn else entitlement.renewsOn)
            ?.asZenDate()
        ZenSheet(onDismiss = { confirmCancel = false }) {
            ZenSheetTitle("Cancel Pro?")
            ZenSheetBody(
                "Pro runs until ${until ?: "the end of this period"}, then the app goes back to free. " +
                    "The launcher, intent, sessions, Zen Score, your daily report and one partner all stay."
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

// ── "You're Pro" ───────────────────────────────────────────────────

/**
 * The one flourish in the sales path, and it comes *after* the money: a quiet bloom of the OS
 * gradient, a chord, the list of what just opened up, then back to where they were.
 */
@Composable
fun ProWelcome(onContinue: () -> Unit) {
    val colors = ZenTheme.colors
    val feedback = rememberZenFeedback()
    val inspection = LocalInspectionMode.current
    val bloom = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) {
        feedback.proUnlocked()
        bloom.animateTo(1f, tween(1_400, easing = ZenMotion.EaseOut))
    }
    val ember = colorResource(R.color.ember_500)
    val amber = colorResource(R.color.amber_500)
    val green = colorResource(R.color.zen_300)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .drawBehind {
                // Three soft rings of the OS gradient open out from behind the title.
                val c = Offset(size.width / 2f, size.height * 0.34f)
                val b = bloom.value
                listOf(ember to 0.9f, amber to 0.65f, green to 0.42f).forEachIndexed { i, (col, reach) ->
                    val r = size.width * reach * (0.35f + 0.65f * b)
                    drawCircle(
                        Brush.radialGradient(listOf(col.copy(alpha = 0.22f * b), col.copy(alpha = 0f)), c, r),
                        radius = r,
                        center = c
                    )
                    drawCircle(
                        color = col.copy(alpha = (0.35f - i * 0.08f) * (1f - b * 0.6f)),
                        radius = r * 0.62f,
                        center = c,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.screenMargin),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.9f))
            ZenProTag(unlocked = true, modifier = Modifier.staggeredEntrance(2, stepMillis = 120))
            Spacer(Modifier.height(16.rdp))
            Text(
                text = "You're Pro. Thank you.",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 32.rsp,
                lineHeight = 36.rsp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.staggeredEntrance(3, stepMillis = 120).semantics { heading() }
            )
            Spacer(Modifier.height(10.rdp))
            Text(
                text = "You're keeping the servers on and the app ad-free. We're grateful.",
                fontFamily = Geist,
                fontSize = 15.rsp,
                lineHeight = 22.rsp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.staggeredEntrance(4, stepMillis = 120)
            )
            Spacer(Modifier.height(28.rdp))
            Column(
                modifier = Modifier.fillMaxWidth().zenCard().padding(16.rdp),
                verticalArrangement = Arrangement.spacedBy(10.rdp)
            ) {
                // Only what works today. Features still arriving land in the changelog, not here.
                listOf("Full history", "Weekly and monthly reports", "Home-screen themes", "Data export")
                    .forEachIndexed { i, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.staggeredEntrance(6 + i, stepMillis = 90)
                    ) {
                        Box(
                            Modifier.size(18.rdp).clip(CircleShape).background(colors.surfaceTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", fontSize = 10.rsp, color = colors.textOnTint)
                        }
                        Spacer(Modifier.width(10.rdp))
                        Text(item, fontFamily = Geist, fontSize = 14.rsp, color = colors.textPrimary)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            ZenButton(
                text = "Back to ZenMode",
                onClick = onContinue,
                modifier = Modifier.padding(bottom = 16.rdp).staggeredEntrance(12, stepMillis = 90)
            )
        }
    }
}
