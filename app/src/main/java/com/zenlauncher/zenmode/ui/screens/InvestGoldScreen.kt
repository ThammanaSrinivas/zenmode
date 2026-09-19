package com.zenlauncher.zenmode.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.GoldOrder
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.ClashLineHeight
import com.zenlauncher.zenmode.ui.components.DepartureMonoLineHeight
import com.zenlauncher.zenmode.ui.components.FullLineBox
import com.zenlauncher.zenmode.ui.components.GeistLineHeight
import com.zenlauncher.zenmode.ui.components.V3BulletDot
import com.zenlauncher.zenmode.ui.components.V3CardDivider
import com.zenlauncher.zenmode.ui.components.V3PrimaryPillButton
import com.zenlauncher.zenmode.ui.components.V3RollingNumber
import com.zenlauncher.zenmode.ui.components.V3ScreenHeader
import com.zenlauncher.zenmode.ui.components.V3ValueStepper
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import com.zenlauncher.zenmode.ui.components.v3GradientCard
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.delay

// ── ZM_OS v3: Invest Gold · Step 01 (quantity) ──────────────────────
// Figma node 2026:1250. Opened from Zen Gold's "Invest Gold" once the week's promise
// is kept. Light-only like its sibling My Promise, so colors come from colors.xml.
// ZenMode never places the order: the CTA hands off to Kite, where the user confirms.
//
// The Figma frame's sample numbers don't agree with each other (₹123.82/unit vs.
// "08 × ₹125.9 = ₹1,251.90", "10 units ≈ ₹824"); every amount here is derived from
// one unit price via GoldOrder instead.

private val ContentMargin: Dp @Composable get() = 32.rdp
private val HeadingMargin: Dp @Composable get() = 33.rdp

private val AmountText: Color @Composable get() = colorResource(R.color.invest_gold_amount_text)
private val SecondaryText: Color @Composable get() = colorResource(R.color.invest_gold_secondary_text)

// Section order for the top-down entrance cascade.
private object Entrance {
    const val HEADER = 0
    const val STEP = 1
    const val TITLE = 2
    const val INSTRUMENT = 3
    const val CARD = 4
    const val NOTE = 5
    const val CTA = 6
    const val STEP_MILLIS = 55
}

@Composable
fun InvestGoldScreen(
    units: Int,
    onUnitsChange: (Int) -> Unit,
    onBackClick: () -> Unit,
    onReviewInKiteClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    onViewTermsClick: () -> Unit = {},
    symbol: String = AppConstants.PLACEHOLDER_GOLD_SYMBOL,
    fundName: String = AppConstants.PLACEHOLDER_GOLD_FUND_NAME,
    exchange: String = AppConstants.PLACEHOLDER_GOLD_EXCHANGE,
    unitPricePaise: Long = AppConstants.PLACEHOLDER_GOLD_UNIT_PRICE_PAISE,
    dematMaskedId: String = AppConstants.PLACEHOLDER_DEMAT_MASKED_ID
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
                        title = "Invest Gold",
                        backLabel = "Back to Zen Gold",
                        onBackClick = onBackClick,
                        dotGap = 3.5.rdp,
                        centerOnTitle = true,
                        dotPulse = true,
                        modifier = Modifier.staggeredEntrance(Entrance.HEADER),
                        trailing = {
                            Image(
                                painter = painterResource(R.drawable.ic_hamburger_menu),
                                contentDescription = "Menu",
                                modifier = Modifier
                                    .pressScale(onClick = onMenuClick)
                                    .width(29.rdp)
                                    .height(17.5.rdp)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(20.rdp))
                    StepIndicator(modifier = Modifier.staggeredEntrance(Entrance.STEP))
                    Spacer(modifier = Modifier.height(40.2.rdp))
                    Text(
                        text = "How much gold?",
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.Medium,
                        fontSize = 32.rsp,
                        lineHeight = (32 * ClashLineHeight).rsp,
                        letterSpacing = (-0.96).sp,
                        color = colorResource(R.color.invest_gold_title_text),
                        style = FullLineBox,
                        modifier = Modifier
                            .padding(horizontal = HeadingMargin)
                            .staggeredEntrance(Entrance.TITLE)
                            .semantics { heading() }
                    )
                    Spacer(modifier = Modifier.height(17.64.rdp))
                    Column(
                        modifier = Modifier
                            .padding(horizontal = ContentMargin)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(18.rdp)
                    ) {
                        InstrumentRow(
                            symbol = symbol,
                            fundName = fundName,
                            exchange = exchange,
                            unitPricePaise = unitPricePaise,
                            modifier = Modifier.staggeredEntrance(Entrance.INSTRUMENT)
                        )
                        QuantityCard(
                            units = units,
                            unitPricePaise = unitPricePaise,
                            onUnitsChange = onUnitsChange,
                            modifier = Modifier.staggeredEntrance(Entrance.CARD)
                        )
                        DematNote(
                            dematMaskedId = dematMaskedId,
                            onViewTermsClick = onViewTermsClick,
                            modifier = Modifier.staggeredEntrance(Entrance.NOTE)
                        )
                    }
                }

                Column {
                    Spacer(modifier = Modifier.height(29.rdp))
                    V3PrimaryPillButton(
                        text = "Review in Kite",
                        onClick = onReviewInKiteClick,
                        modifier = Modifier
                            .padding(horizontal = 31.rdp)
                            .staggeredEntrance(Entrance.CTA, rise = 28.dp)
                    )
                    Spacer(modifier = Modifier.height(45.rdp))
                }
            }
        }
    }
}

// ── Step indicator ────────────────────────────────────────────────

@Composable
private fun StepIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "Step 1: choose the quantity and amount" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Step 01",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 14.rsp,
            lineHeight = (14 * GeistLineHeight).rsp,
            letterSpacing = (-0.28).sp,
            color = Color.Black,
            maxLines = 1,
            style = FullLineBox
        )
        V3BulletDot(slotWidth = 24.rdp, dotSize = 4.rdp, color = SecondaryText)
        Text(
            text = "Choose The Quantity & Amount",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 16.rsp,
            lineHeight = (16 * GeistLineHeight).rsp,
            letterSpacing = (-0.32).sp,
            color = SecondaryText,
            maxLines = 1,
            style = FullLineBox
        )
    }
}

// ── Instrument row ────────────────────────────────────────────────

@Composable
private fun InstrumentRow(
    symbol: String,
    fundName: String,
    exchange: String,
    unitPricePaise: Long,
    modifier: Modifier = Modifier
) {
    val price = GoldOrder.formatInr(unitPricePaise)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(62.rdp)
            .clip(RoundedCornerShape(4.rdp))
            .background(colorResource(R.color.invest_gold_instrument_bg))
            .clearAndSetSemantics { contentDescription = "$symbol, $fundName on $exchange, $price per unit" }
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 11.rdp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.rdp)
        ) {
            SpinningCoin()
            Column {
                Text(
                    text = symbol,
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.513.rsp,
                    lineHeight = (20.513 * ClashLineHeight).rsp,
                    letterSpacing = (-0.6154).sp,
                    color = colorResource(R.color.invest_gold_symbol_text),
                    maxLines = 1,
                    style = FullLineBox
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InstrumentMeta(fundName)
                    V3BulletDot(slotWidth = 18.rdp, dotSize = 3.5.rdp, color = SecondaryText)
                    InstrumentMeta(exchange)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.rdp, end = 4.87.rdp)
                .width(75.133.rdp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.rdp)
        ) {
            Text(
                text = price,
                fontFamily = DepartureMono,
                fontSize = 18.4.rsp,
                lineHeight = (18.4 * 1.257).rsp,
                letterSpacing = (-2.208).sp,
                color = colorResource(R.color.zen_700),
                maxLines = 1,
                softWrap = false,
                style = FullLineBox
            )
            Text(
                text = "PER UNIT",
                fontFamily = DepartureMono,
                fontSize = 10.rsp,
                lineHeight = (10 * DepartureMonoLineHeight).rsp,
                letterSpacing = (-0.3).sp,
                color = SecondaryText,
                maxLines = 1,
                style = FullLineBox
            )
        }
    }
}

@Composable
private fun InstrumentMeta(text: String) {
    Text(
        text = text,
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 12.rsp,
        lineHeight = (12 * GeistLineHeight).rsp,
        letterSpacing = (-0.36).sp,
        color = SecondaryText,
        maxLines = 1,
        style = FullLineBox
    )
}

/** The coin flips in on its vertical axis as its row arrives, then settles with a wobble. */
@Composable
private fun SpinningCoin() {
    val inspection = LocalInspectionMode.current
    val rotation = remember { Animatable(if (inspection) 0f else -540f) }
    LaunchedEffect(Unit) {
        delay(Entrance.INSTRUMENT * Entrance.STEP_MILLIS.toLong())
        rotation.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow))
    }
    Image(
        painter = painterResource(R.drawable.ic_invest_gold_coin),
        contentDescription = null,
        modifier = Modifier
            .size(56.rdp)
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 12f * density
            }
    )
}

// ── Quantity card ─────────────────────────────────────────────────

@Composable
private fun QuantityCard(
    units: Int,
    unitPricePaise: Long,
    onUnitsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val maxUnits = AppConstants.INVEST_GOLD_MAX_UNITS_PER_WEEK
    val total = GoldOrder.totalPaise(units, unitPricePaise)
    // Set from the tap itself so both rolling numbers agree on direction.
    var rollUp by remember { mutableStateOf(true) }

    fun select(target: Int) {
        val clamped = GoldOrder.clampUnits(target)
        if (clamped == units) return
        rollUp = clamped > units
        onUnitsChange(clamped)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .v3GradientCard()
            .padding(top = 21.rdp, bottom = 22.6.rdp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardLabel("QUANTITY")
            V3BulletDot(slotWidth = 18.rdp, dotSize = 3.5.rdp, color = Color.Black)
            CardLabel("WHOLE  UNITS")
            Spacer(modifier = Modifier.weight(1f))
            WeeklyCapLabel(atCap = units == maxUnits)
        }

        Spacer(modifier = Modifier.height(28.4.rdp))

        V3ValueStepper(
            value = units,
            maxValue = maxUnits,
            rollUp = rollUp,
            minDigits = 2,
            caption = "UNITS",
            captionLetterSpacing = (-0.4021).sp,
            captionGap = 0.dp,
            valueDescription = "$units ${if (units == 1) "unit" else "units"}, ${GoldOrder.formatInr(total)}",
            decreaseLabel = "Decrease quantity",
            increaseLabel = "Increase quantity",
            canDecrease = units > AppConstants.INVEST_GOLD_MIN_UNITS,
            canIncrease = units < maxUnits,
            onStep = { delta -> select(units + delta) }
        )

        Spacer(modifier = Modifier.height(20.94.rdp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.074.rdp, Alignment.CenterHorizontally)
        ) {
            AppConstants.INVEST_GOLD_QUICK_PICK_UNITS.forEach { preset ->
                QuickPickChip(
                    units = preset,
                    selected = preset == units,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        select(preset)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(21.rdp))

        V3CardDivider()

        Spacer(modifier = Modifier.height(4.rdp))

        OrderBreakdown(units = units, unitPricePaise = unitPricePaise, totalPaise = total, rollUp = rollUp)

        Spacer(modifier = Modifier.height(8.rdp))

        Text(
            text = "The weekly Maximum cap is $maxUnits units, about " +
                "${GoldOrder.formatInr(GoldOrder.weeklyCapPaise(unitPricePaise))}. " +
                "It is our limit on this screen, not on you.",
            fontFamily = Geist,
            fontSize = 12.rsp,
            lineHeight = 18.72.rsp,
            letterSpacing = (-0.36).sp,
            color = Color.Black,
            style = FullLineBox,
            modifier = Modifier.padding(horizontal = 28.rdp)
        )
    }
}

@Composable
private fun CardLabel(text: String) {
    Text(
        text = text,
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 12.rsp,
        lineHeight = (12 * GeistLineHeight).rsp,
        letterSpacing = (-0.36).sp,
        color = Color.Black,
        maxLines = 1,
        style = FullLineBox
    )
}

/** Gives a short bump each time the quantity lands on the weekly cap. */
@Composable
private fun WeeklyCapLabel(atCap: Boolean) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(atCap) {
        if (atCap) {
            scale.animateTo(1f, keyframes {
                durationMillis = 420
                1.12f at 120 using FastOutSlowInEasing
                0.97f at 260
            })
        }
    }
    Text(
        text = "MAX: ${AppConstants.INVEST_GOLD_MAX_UNITS_PER_WEEK} UNITS/WEEK",
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.rsp,
        lineHeight = (12 * GeistLineHeight).rsp,
        letterSpacing = (-0.36).sp,
        color = AmountText,
        maxLines = 1,
        style = FullLineBox,
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            transformOrigin = TransformOrigin(1f, 0.5f)
        }
    )
}

@Composable
private fun QuickPickChip(units: Int, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(5.037.rdp)
    val colorSpec = tween<Color>(durationMillis = 220, easing = FastOutSlowInEasing)
    val background by animateColorAsState(
        if (selected) colorResource(R.color.invest_gold_chip_selected_bg) else Color.White,
        colorSpec,
        label = "ChipBackground"
    )
    val textColor by animateColorAsState(
        if (selected) colorResource(R.color.invest_gold_chip_selected_text) else colorResource(R.color.invest_gold_chip_text),
        colorSpec,
        label = "ChipText"
    )
    val label = if (units == 1) "1 unit" else "$units units"

    Box(
        modifier = Modifier
            .width(60.444.rdp)
            .height(34.rdp)
            .pressScale(onClick = onClick, onClickLabel = "Choose $label")
            .semantics { this.selected = selected }
            .clip(shape)
            .background(background)
            .border(1.259.dp, colorResource(R.color.my_promise_stepper_border), shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 14.rsp,
            lineHeight = (14 * GeistLineHeight).rsp,
            letterSpacing = (-0.42).sp,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            style = FullLineBox
        )
    }
}

@Composable
private fun OrderBreakdown(units: Int, unitPricePaise: Long, totalPaise: Long, rollUp: Boolean) {
    // Counts to the new total rather than snapping, in step with the rolling quantity.
    val animatedTotal by animateIntAsState(
        targetValue = totalPaise.toInt(),
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "OrderTotal"
    )
    val lineStyle = FullLineBox.copy(
        fontFamily = DepartureMono,
        fontSize = 16.rsp,
        lineHeight = (16 * DepartureMonoLineHeight).rsp,
        letterSpacing = (-0.48).sp,
        color = Color.Black
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.rdp)
            .clearAndSetSemantics {
                contentDescription = "$units × ${GoldOrder.formatInr(unitPricePaise)} = ${GoldOrder.formatInr(totalPaise)}"
            },
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(5.rdp)
        ) {
            V3RollingNumber(
                value = units,
                maxValue = AppConstants.INVEST_GOLD_MAX_UNITS_PER_WEEK,
                rollUp = rollUp,
                minDigits = 2,
                style = lineStyle
            )
            Text(text = "×", style = lineStyle.copy(letterSpacing = (-1.92).sp), maxLines = 1)
            Text(
                text = GoldOrder.formatInr(unitPricePaise),
                style = lineStyle.copy(letterSpacing = (-1.92).sp),
                maxLines = 1,
                softWrap = false
            )
        }
        Text(
            text = GoldOrder.formatInr(animatedTotal.toLong()),
            fontFamily = DepartureMono,
            fontSize = 20.rsp,
            lineHeight = (20 * DepartureMonoLineHeight).rsp,
            letterSpacing = (-2.4).sp,
            color = AmountText,
            maxLines = 1,
            softWrap = false,
            style = FullLineBox
        )
    }
}

// ── Demat note ────────────────────────────────────────────────────

@Composable
private fun DematNote(dematMaskedId: String, onViewTermsClick: () -> Unit, modifier: Modifier = Modifier) {
    val zen900 = colorResource(R.color.zen_900)
    val link = colorResource(R.color.invest_gold_terms_link)
    val inspection = LocalInspectionMode.current
    val checkScale = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(Entrance.NOTE * Entrance.STEP_MILLIS + 180L)
        checkScale.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMediumLow))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.rdp)
            .clip(RoundedCornerShape(6.rdp))
            .background(colorResource(R.color.my_promise_note_bg))
            .padding(horizontal = 7.rdp, vertical = 10.rdp),
        horizontalArrangement = Arrangement.spacedBy(6.rdp),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(R.drawable.ic_invest_gold_success),
            contentDescription = null,
            modifier = Modifier
                .size(16.rdp)
                .graphicsLayer {
                    scaleX = checkScale.value
                    scaleY = checkScale.value
                }
        )
        Text(
            text = buildAnnotatedString {
                append("Demat linked • ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = zen900)) {
                    append("$dematMaskedId.")
                }
                append(" ZenMode never holds, moves or advises on your money the order is placed by you in Kite. ")
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "terms",
                        styles = TextLinkStyles(SpanStyle(fontWeight = FontWeight.SemiBold, color = link))
                    ) { onViewTermsClick() }
                ) {
                    append("Read the terms & conditions")
                }
            },
            style = FullLineBox.copy(
                fontFamily = Geist,
                fontWeight = FontWeight.Normal,
                fontSize = 12.rsp,
                lineHeight = (12 * GeistLineHeight).rsp,
                color = Color.Black
            ),
            modifier = Modifier.weight(1f)
        )
    }
}
