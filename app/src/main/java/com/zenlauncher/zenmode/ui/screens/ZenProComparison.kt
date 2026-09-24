package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.ui.components.ZenEyebrow
import com.zenlauncher.zenmode.ui.components.zenCard
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.ZenTypography
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// ZenMode Pro -- the plan page's full free-vs-pro comparison table and the coming-soon
// roadmap chips. Split out of ZenProScreen.kt; the sales rules at the top of that file apply
// here too.

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

/** The row-by-row diff. The Pro column carries a soft, continuous tint so the eye reads the
 * whole right-hand side as "what you get," not fourteen separate rows. */
@Composable
internal fun FeatureComparisonTable(modifier: Modifier = Modifier) {
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
internal fun ComingSoonSection(modifier: Modifier = Modifier) {
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
