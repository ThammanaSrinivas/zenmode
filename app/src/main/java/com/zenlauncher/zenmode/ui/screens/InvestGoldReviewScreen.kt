package com.zenlauncher.zenmode.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.GoldOrder
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.SettingsMenuButton
import com.zenlauncher.zenmode.ui.components.BrandedText
import com.zenlauncher.zenmode.ui.components.ClashLineHeight
import com.zenlauncher.zenmode.ui.components.DepartureMonoLineHeight
import com.zenlauncher.zenmode.ui.components.FullLineBox
import com.zenlauncher.zenmode.ui.components.GeistLineHeight
import com.zenlauncher.zenmode.ui.components.V3BulletDot
import com.zenlauncher.zenmode.ui.components.V3CardDivider
import com.zenlauncher.zenmode.ui.components.V3PrimaryPillButton
import com.zenlauncher.zenmode.ui.components.V3ScreenHeader
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import com.zenlauncher.zenmode.ui.components.v3GradientCard
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// ── ZM_OS v3: Invest Gold · Step 02 (review) ────────────────────────
// The order the user picked on step 01, read back before handing off to Kite.
// Same light-only chrome as step 01.
//
// The Figma copy says "We build the basket"; the Kite hand-off is a plain app open
// (no prefill) until Zerodha approves prefilled deep links, so the copy here says
// the user places the order in Kite. Put the basket wording back once
// KiteBasketActivity's prefilled flow is live.

private val ReviewContentMargin: Dp @Composable get() = 32.rdp
private val ReviewHeadingMargin: Dp @Composable get() = 33.rdp

private val ReviewAmountText: Color @Composable get() = colorResource(R.color.invest_gold_amount_text)
private val ReviewSecondaryText: Color @Composable get() = colorResource(R.color.invest_gold_secondary_text)

private object ReviewEntrance {
    const val HEADER = 0
    const val STEP = 1
    const val TITLE = 2
    const val CARD = 3
    const val NOTE = 4
    const val CTA = 5
    const val STEP_MILLIS = 55
}

@Composable
fun InvestGoldReviewScreen(
    units: Int,
    onBackClick: () -> Unit,
    onOpenKiteClick: () -> Unit,
    onChangeQuantityClick: () -> Unit,
    symbol: String = AppConstants.PLACEHOLDER_GOLD_SYMBOL,
    exchange: String = AppConstants.PLACEHOLDER_GOLD_EXCHANGE,
    unitPricePaise: Long = AppConstants.PLACEHOLDER_GOLD_UNIT_PRICE_PAISE
) {
    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.my_promise_bg))
    ) {
        Image(
            painter = painterResource(R.drawable.bg_my_promise_glow),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = integerResource(R.integer.my_promise_glow_alpha_pct) / 100f,
            modifier = Modifier.matchParentSize()
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(modifier = Modifier.height(34.rdp))
                    V3ScreenHeader(
                        title = "Review",
                        backLabel = "Back to quantity",
                        onBackClick = onBackClick,
                        dotGap = 3.5.rdp,
                        centerOnTitle = true,
                        dotPulse = true,
                        modifier = Modifier.staggeredEntrance(ReviewEntrance.HEADER),
                        trailing = { SettingsMenuButton() }
                    )
                    Spacer(modifier = Modifier.height(20.rdp))
                    InvestGoldStepIndicator(
                        step = 2,
                        label = "Complete the payment",
                        modifier = Modifier.staggeredEntrance(ReviewEntrance.STEP)
                    )
                    Spacer(modifier = Modifier.height(40.2.rdp))
                    Column(
                        modifier = Modifier
                            .padding(horizontal = ReviewHeadingMargin)
                            .staggeredEntrance(ReviewEntrance.TITLE)
                    ) {
                        Text(
                            text = "Complete with Kite",
                            fontFamily = ClashDisplay,
                            fontWeight = FontWeight.Medium,
                            fontSize = 32.rsp,
                            lineHeight = (32 * ClashLineHeight).rsp,
                            letterSpacing = (-0.96).sp,
                            color = colorResource(R.color.invest_gold_title_text),
                            style = FullLineBox,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(modifier = Modifier.height(8.rdp))
                        Text(
                            text = "Kite opens next. You place and authorise this order inside Zerodha Kite.",
                            fontFamily = Geist,
                            fontSize = 12.rsp,
                            lineHeight = (12 * GeistLineHeight).rsp,
                            letterSpacing = (-0.36).sp,
                            color = ZenTheme.colors.textPrimary,
                            style = FullLineBox
                        )
                    }
                    Spacer(modifier = Modifier.height(22.rdp))
                    Column(
                        modifier = Modifier
                            .padding(horizontal = ReviewContentMargin)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(18.rdp)
                    ) {
                        OrderCard(
                            units = units,
                            symbol = symbol,
                            exchange = exchange,
                            unitPricePaise = unitPricePaise,
                            modifier = Modifier.staggeredEntrance(ReviewEntrance.CARD)
                        )
                        HandOffNotes(
                            units = units,
                            symbol = symbol,
                            modifier = Modifier.staggeredEntrance(ReviewEntrance.NOTE)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 31.rdp)
                        .staggeredEntrance(ReviewEntrance.CTA, rise = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(29.rdp))
                    V3PrimaryPillButton(text = "Open Kite to authorise", onClick = onOpenKiteClick)
                    Spacer(modifier = Modifier.height(8.rdp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.rdp)
                            .pressScale(onClick = onChangeQuantityClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Change the quantity",
                            fontFamily = Geist,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.rsp,
                            lineHeight = (16 * GeistLineHeight).rsp,
                            letterSpacing = (-0.32).sp,
                            color = ReviewSecondaryText,
                            maxLines = 1,
                            style = FullLineBox
                        )
                    }
                    Spacer(modifier = Modifier.height(37.rdp))
                }
            }
        }
    }
}

// ── Order card ────────────────────────────────────────────────────

@Composable
private fun OrderCard(
    units: Int,
    symbol: String,
    exchange: String,
    unitPricePaise: Long,
    modifier: Modifier = Modifier
) {
    val total = GoldOrder.formatInr(GoldOrder.totalPaise(units, unitPricePaise))
    val unitsLabel = if (units == 1) "1 unit" else "$units units"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .v3GradientCard()
            .padding(top = 20.rdp, bottom = 20.rdp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrderEyebrow("ORDER")
            V3BulletDot(slotWidth = 18.rdp, dotSize = 3.5.rdp, color = ReviewAmountText)
            OrderEyebrow("1 ITEM")
        }
        Spacer(modifier = Modifier.height(16.rdp))
        Column(
            modifier = Modifier.padding(horizontal = 18.rdp),
            verticalArrangement = Arrangement.spacedBy(12.rdp)
        ) {
            OrderRow(label = "Instrument", value = "$symbol • $exchange")
            OrderRow(label = "Quantity", value = "× $unitsLabel")
            OrderRow(label = "Order type", value = "Market • delivery")
            OrderRow(label = "Rate", value = "${GoldOrder.formatInr(unitPricePaise)}/ unit")
        }
        Spacer(modifier = Modifier.height(18.rdp))
        V3CardDivider()
        Spacer(modifier = Modifier.height(14.rdp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.rdp)
                .clearAndSetSemantics { contentDescription = "Order sum $total" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ORDER SUM",
                fontFamily = DepartureMono,
                fontSize = 12.rsp,
                lineHeight = (12 * DepartureMonoLineHeight).rsp,
                letterSpacing = (-0.36).sp,
                color = ReviewSecondaryText,
                maxLines = 1,
                style = FullLineBox,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = total,
                fontFamily = DepartureMono,
                fontSize = 20.rsp,
                lineHeight = (20 * DepartureMonoLineHeight).rsp,
                letterSpacing = (-2.4).sp,
                color = ReviewAmountText,
                maxLines = 1,
                softWrap = false,
                style = FullLineBox
            )
        }
    }
}

@Composable
private fun OrderEyebrow(text: String) {
    Text(
        text = text,
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.rsp,
        lineHeight = (12 * GeistLineHeight).rsp,
        letterSpacing = (-0.36).sp,
        color = ReviewAmountText,
        maxLines = 1,
        style = FullLineBox
    )
}

@Composable
private fun OrderRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$label: $value" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = Geist,
            fontSize = 14.rsp,
            lineHeight = (14 * GeistLineHeight).rsp,
            letterSpacing = (-0.42).sp,
            color = ReviewSecondaryText,
            maxLines = 1,
            style = FullLineBox,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontFamily = DepartureMono,
            fontSize = 14.rsp,
            lineHeight = (14 * DepartureMonoLineHeight).rsp,
            letterSpacing = (-0.84).sp,
            color = ZenTheme.colors.textPrimary,
            maxLines = 1,
            softWrap = false,
            style = FullLineBox
        )
    }
}

// ── Hand-off notes ────────────────────────────────────────────────

@Composable
private fun HandOffNotes(units: Int, symbol: String, modifier: Modifier = Modifier) {
    val unitsLabel = if (units == 1) "1 unit" else "$units units"
    val lines = listOf(
        "In Kite, buy $symbol, $unitsLabel. Nothing else goes with it.",
        "No money moves through ZenMode OS. We never see your funds or your login.",
        "We can't cancel, modify or exit the position for you. Kite handles everything and is solely responsible."
    )
    val noteStyle = FullLineBox.copy(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 12.rsp,
        lineHeight = (12 * GeistLineHeight).rsp,
        color = ZenTheme.colors.textPrimary
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.rdp))
            .background(colorResource(R.color.my_promise_note_bg))
            .padding(horizontal = 7.rdp, vertical = 12.rdp),
        verticalArrangement = Arrangement.spacedBy(12.rdp)
    ) {
        lines.forEachIndexed { i, line ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.rdp),
                verticalAlignment = Alignment.Top
            ) {
                InvestGoldNoteCheck(
                    popDelayMillis = ReviewEntrance.NOTE * ReviewEntrance.STEP_MILLIS + 180L + i * 90L
                )
                BrandedText(text = line, style = noteStyle, modifier = Modifier.weight(1f))
            }
        }
    }
}
