package com.zenlauncher.zenmode.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.AppConstants.BACK_TO_ZEN_GOLD
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.dropShadow
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.*
import com.zenlauncher.zenmode.ui.theme.rsp

// ── Data model ───────────────────────────────────────────────────

data class GoldHoldingEntry(
    val periodLabel: String,
    val statusLabel: String,
    val amountLabel: String? = null,
    val unitsLabel: String? = null,
    val isAccessDenied: Boolean = false
)

/** "6 of 7 days promise kept", the one spelling of a week's status line. */
private fun keptStatus(daysKept: Int) = "$daysKept of 7 days promise kept"

/** A week that fell short: the status line with Gold access denied. */
private fun deniedStatus(daysKept: Int) = "Only ${keptStatus(daysKept)}, access denied"

private fun defaultGoldHoldings() = listOf(
    GoldHoldingEntry("Sep 01-07, 2026", keptStatus(6), "₹180", "3 Units"),
    GoldHoldingEntry("Oct 12-18, 2026", deniedStatus(3), isAccessDenied = true),
    GoldHoldingEntry("Nov 03-09, 2026", keptStatus(6), "₹320", "5 Units"),
    GoldHoldingEntry("Dec 07-13, 2026", keptStatus(6), "₹320", "5 Units"),
    GoldHoldingEntry("Jan 15-21, 2027", deniedStatus(3), isAccessDenied = true),
    GoldHoldingEntry("Feb 01-07, 2027", keptStatus(6), "₹265", "4 Units"),
    GoldHoldingEntry("Mar 08-14, 2027", keptStatus(6), "₹265", "4 Units")
)

// ── Main Gold Holdings Screen ───────────────────────────────────────
//
// Reached from "View all" on the Zen Gold home card. Not yet wired
// into navigation; self-contained with sensible defaults so it can be
// previewed and dropped into a nav graph later.

@Composable
fun GoldHoldingsScreen(
    investedSum: Int = 20543,
    unitsHeld: Int = 164,
    sincePercent: Int = 25,
    sinceLabel: String = "SINCE MARCH",
    tickerLabel: String = "GOLDBEES",
    headline: String = "Quiet Weeks & Earned Gold",
    holdings: List<GoldHoldingEntry> = remember { defaultGoldHoldings() },
    isPro: Boolean = false,
    onBack: () -> Unit = {},
    onBackToZenGold: () -> Unit = {},
    onDownloadPdfClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(GoldBg, GoldBgEnd))
            )
    ) {
        // Ambient warm glow — echoes the gold coin sheen behind the content
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = 0.5f }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(GoldGlow.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .blur(70.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            GoldHoldingsTopBar(onBack = onBack)

            Spacer(modifier = Modifier.height(28.dp))

            BreadcrumbRow(tickerLabel = tickerLabel)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = headline,
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.rsp,
                letterSpacing = (-0.6).sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(18.dp))

            GoldSummaryRow(
                investedSum = investedSum,
                unitsHeld = unitsHeld,
                sincePercent = sincePercent,
                sinceLabel = sinceLabel
            )

            Spacer(modifier = Modifier.height(20.dp))

            GoldHoldingsListCard(
                holdings = holdings,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            BackToZenGoldButton(onClick = onBackToZenGold)

            Spacer(modifier = Modifier.height(10.dp))

            DownloadPdfLink(isPro = isPro, onClick = onDownloadPdfClick)

            Spacer(modifier = Modifier.height(16.dp))

            PageDots(totalPages = 3, currentPage = 2, modifier = Modifier.align(Alignment.CenterHorizontally))

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Top bar: back arrow + coin mark + title ─────────────────────────

@Composable
private fun GoldHoldingsTopBar(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        BackArrow(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_zen_gold_coin),
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = "GOLD HOLDINGS",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.rsp,
                letterSpacing = (-0.3).sp,
                color = GoldGreen900
            )
        }
    }
}

@Composable
private fun BackArrow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(28.dp)
            .clickable { onClick() }
    ) {
        val midY = size.height / 2f
        val strokeW = size.width * 0.09f
        drawLine(
            color = Color.Black,
            start = Offset(size.width * 0.78f, midY),
            end = Offset(size.width * 0.18f, midY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.Black,
            start = Offset(size.width * 0.42f, size.height * 0.18f),
            end = Offset(size.width * 0.18f, midY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.Black,
            start = Offset(size.width * 0.42f, size.height * 0.82f),
            end = Offset(size.width * 0.18f, midY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun BreadcrumbRow(tickerLabel: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "HOLDINGS",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 12.rsp,
            letterSpacing = 1.sp,
            color = GoldMuted
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(GoldMuted)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = tickerLabel,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 12.rsp,
            letterSpacing = 1.sp,
            color = GoldMuted
        )
    }
}

// ── Summary row: invested sum / units / since % ─────────────────────

@Composable
private fun GoldSummaryRow(
    investedSum: Int,
    unitsHeld: Int,
    sincePercent: Int,
    sinceLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SummaryStat(
            value = "₹%,d".format(java.util.Locale.US, investedSum),
            label = "INVESTED SUM",
            valueColor = GoldGreen700,
            valueFontSize = 22
        )
        SummaryStat(
            value = "$unitsHeld",
            label = "UNITS",
            valueColor = GoldGreenDeep,
            valueFontSize = 26
        )
        SummaryStat(
            value = "$sincePercent%",
            label = sinceLabel,
            valueColor = GoldGreen700,
            valueFontSize = 26
        )
    }
}

@Composable
private fun SummaryStat(
    value: String,
    label: String,
    valueColor: Color,
    valueFontSize: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = DepartureMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = valueFontSize.rsp,
            letterSpacing = (-0.8).sp,
            color = valueColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 11.rsp,
            letterSpacing = 0.2.sp,
            color = GoldLabelGrey
        )
    }
}

// ── Holdings list card ──────────────────────────────────────────────

@Composable
private fun GoldHoldingsListCard(holdings: List<GoldHoldingEntry>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GoldCardFill)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            itemsIndexed(holdings) { index, entry ->
                GoldHoldingRow(entry)
                if (index != holdings.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(GoldDivider)
                    )
                }
            }
        }

        GoldListScrollbar(
            listState = listState,
            itemCount = holdings.size,
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp, horizontal = 8.dp)
        )
    }
}

@Composable
private fun GoldHoldingRow(entry: GoldHoldingEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (entry.isAccessDenied) GoldDivider else GoldGreen700)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = entry.periodLabel,
                    fontFamily = Geist,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.rsp,
                    letterSpacing = (-0.3).sp,
                    color = GoldRowTitle
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = entry.statusLabel,
                    fontFamily = Geist,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.rsp,
                    letterSpacing = (-0.2).sp,
                    color = GoldRowSubtitle
                )
            }
        }

        if (!entry.isAccessDenied && entry.amountLabel != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.amountLabel,
                    fontFamily = DepartureMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.rsp,
                    letterSpacing = (-1.2).sp,
                    color = GoldAmountBrown
                )
                if (entry.unitsLabel != null) {
                    Text(
                        text = entry.unitsLabel,
                        fontFamily = Geist,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.rsp,
                        color = GoldUnitsGreen
                    )
                }
            }
        }
    }
}

/** Thin scrollbar thumb that mirrors the list's scroll position. */
@Composable
private fun GoldListScrollbar(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    val scrollFraction: State<Float> = remember(listState, itemCount) {
        derivedStateOf {
            if (itemCount <= 1) return@derivedStateOf 0f
            listState.firstVisibleItemIndex.toFloat() / (itemCount - 1)
        }
    }
    val animatedFraction by animateFloatAsState(
        targetValue = scrollFraction.value,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "goldScrollbar"
    )
    val thumbHeightFraction = 0.28f

    Box(
        modifier = modifier
            .width(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(GoldDivider)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight(fraction = thumbHeightFraction)
                .width(4.dp)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = animatedFraction * (size.height / thumbHeightFraction) * (1f - thumbHeightFraction)
                }
                .clip(RoundedCornerShape(2.dp))
                .background(GoldGreen700)
        )
    }
}

// ── CTA buttons ───────────────────────────────────────────────────

@Composable
private fun BackToZenGoldButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(50))
            .background(GoldButtonGreen)
            .dropShadow(color = GoldButtonGreen.copy(alpha = 0.35f), blur = 16.dp, cornerRadius = 25.dp, offsetY = 6.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = BACK_TO_ZEN_GOLD,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 16.rsp,
            color = Color.White
        )
    }
}

@Composable
private fun DownloadPdfLink(isPro: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Download PDF",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 15.rsp,
            color = GoldButtonGreenText
        )
        if (!isPro) {
            Spacer(modifier = Modifier.width(6.dp))
            GoldProBadge()
        }
    }
}

@Composable
private fun GoldProBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GoldButtonGreenText)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = "PRO",
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 7.rsp,
            color = Color.White
        )
    }
}

@Composable
private fun PageDots(totalPages: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (index == currentPage) GoldGreen700 else GoldMuted.copy(alpha = 0.3f))
            )
        }
    }
}

// ── Preview ──────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun GoldHoldingsScreenPreview() {
    ZenTheme {
        GoldHoldingsScreen()
    }
}
