package com.zenlauncher.zenmode.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.ZenTypography
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// ZenMode OS v3 settings primitives. Radii follow the token set: 20 card, 16 row/tile,
// 8 chip. Every interactive element keeps the 48dp minimum target.

private val CardRadius: Dp @Composable get() = 20.rdp
private val ControlRadius: Dp @Composable get() = 16.rdp
private val ChipRadius: Dp @Composable get() = 8.rdp

/** Paper card: a tight contact shadow and one long soft drop, never a flat blur. */
@Composable
fun Modifier.zenCard(shape: Shape = RoundedCornerShape(CardRadius)): Modifier {
    val colors = ZenTheme.colors
    return this
        .shadow(
            elevation = 10.rdp,
            shape = shape,
            ambientColor = colors.textPrimary.copy(alpha = 0.06f),
            spotColor = colors.textPrimary.copy(alpha = 0.16f)
        )
        .clip(shape)
        .background(colors.bgSecondary)
}

/** Uppercase Departure Mono section eyebrow. Only for section labels. */
@Composable
fun ZenEyebrow(text: String, modifier: Modifier = Modifier, color: Color = ZenTheme.colors.textSecondary) {
    Text(
        text = text.uppercase(),
        style = ZenTypography.monoLabel,
        color = color,
        modifier = modifier.semantics { heading() }
    )
}

/** A labelled group of rows on one card. Rows inside are separated by inset hairlines. */
@Composable
fun ZenSettingsGroup(
    label: String,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.rdp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZenEyebrow(label, Modifier.weight(1f))
            if (trailingLabel != null) {
                Text(trailingLabel.uppercase(), style = ZenTypography.monoLabel, color = ZenTheme.colors.textMuted)
            }
        }
        Column(modifier = Modifier.fillMaxWidth().zenCard(), content = content)
    }
}

@Composable
fun ZenRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.rdp),
        thickness = 1.dp,
        color = ZenTheme.colors.borderHairlineSoft
    )
}

enum class RowTrailing { Chevron, External, None }

/**
 * Tier state for a row. [Locked] rows stay visible and tappable — they open the Pro sheet —
 * but read quieter. Never hide a Pro control from a Free user.
 */
enum class ProTagState { None, Locked, Unlocked }

@Composable
fun ZenProTag(unlocked: Boolean, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    val bg = if (unlocked) colors.surfaceTint else colors.rewardSurface
    val line = if (unlocked) colors.surfaceTintLine else colors.rewardSurfaceLine
    val fg = if (unlocked) colors.textOnTint else colors.accentReward
    Text(
        text = "PRO",
        fontFamily = DepartureMono,
        fontSize = 10.rsp,
        letterSpacing = 1.sp,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(6.rdp))
            .background(bg)
            .border(1.dp, line, RoundedCornerShape(6.rdp))
            .padding(horizontal = 6.rdp, vertical = 1.rdp)
    )
}

@Composable
private fun RowTitle(title: String, pro: ProTagState) {
    val colors = ZenTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 16.rsp,
            lineHeight = 22.rsp,
            color = if (pro == ProTagState.Locked) colors.textSecondary else colors.textPrimary
        )
        if (pro != ProTagState.None) {
            Spacer(Modifier.width(8.rdp))
            ZenProTag(unlocked = pro == ProTagState.Unlocked)
        }
    }
}

@Composable
private fun RowSubtitle(text: String) {
    Text(
        text = text,
        fontFamily = Geist,
        fontSize = 13.rsp,
        lineHeight = 18.rsp,
        color = ZenTheme.colors.textSecondary,
        modifier = Modifier.padding(top = 2.rdp)
    )
}

/** Navigational row: title, optional subtitle, optional value, trailing affordance. */
@Composable
fun ZenSettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    valueIsNumber: Boolean = false,
    pro: ProTagState = ProTagState.None,
    trailing: RowTrailing = RowTrailing.Chevron
) {
    val colors = ZenTheme.colors
    val feedback = rememberZenFeedback()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.rdp)
            .clickable(onClickLabel = title, role = Role.Button) {
                feedback.tap()
                onClick()
            }
            .padding(horizontal = 16.rdp, vertical = 10.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            RowTitle(title, pro)
            if (subtitle != null) RowSubtitle(subtitle)
        }
        if (value != null) {
            Spacer(Modifier.width(12.rdp))
            Text(
                text = value,
                fontFamily = if (valueIsNumber) DepartureMono else Geist,
                fontSize = 14.rsp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.rdp)
            )
        }
        when (trailing) {
            RowTrailing.Chevron -> ZenGlyph(GlyphKind.Chevron, colors.textMuted, Modifier.padding(start = 4.rdp).size(20.rdp))
            RowTrailing.External -> ZenGlyph(GlyphKind.External, colors.textMuted, Modifier.padding(start = 8.rdp).size(16.rdp))
            RowTrailing.None -> Unit
        }
    }
}

enum class GlyphKind { Chevron, Back, External }

/** 2dp round-capped line glyphs, drawn rather than pulled from an icon font. Mirrors in RTL. */
@Composable
fun ZenGlyph(kind: GlyphKind, color: Color, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun x(v: Float) = if (rtl && kind != GlyphKind.External) w - v else v
        val path = Path()
        when (kind) {
            GlyphKind.Chevron -> {
                path.moveTo(x(w * 0.38f), h * 0.22f)
                path.lineTo(x(w * 0.66f), h * 0.5f)
                path.lineTo(x(w * 0.38f), h * 0.78f)
            }
            GlyphKind.Back -> {
                path.moveTo(x(w * 0.62f), h * 0.2f)
                path.lineTo(x(w * 0.32f), h * 0.5f)
                path.lineTo(x(w * 0.62f), h * 0.8f)
            }
            GlyphKind.External -> {
                path.moveTo(w * 0.3f, h * 0.7f)
                path.lineTo(w * 0.7f, h * 0.3f)
                path.moveTo(w * 0.38f, h * 0.3f)
                path.lineTo(w * 0.7f, h * 0.3f)
                path.lineTo(w * 0.7f, h * 0.62f)
            }
        }
        drawPath(path, color, style = stroke)
    }
}

/**
 * Visual-only switch; the whole row carries the toggle semantics. [onTrack] / [offTrack]
 * recolour it for surfaces that aren't the plain settings background (e.g. a green card).
 */
@Composable
fun ZenSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onTrack: Color = ZenTheme.colors.textBrand,
    offTrack: Color = ZenTheme.colors.surfaceSunk,
    outlineWhenOff: Boolean = true
) {
    val colors = ZenTheme.colors
    val track by animateColorAsState(if (checked) onTrack else offTrack, ZenMotion.arrive(ZenMotion.MEDIUM), label = "track")
    // 0 → 1 travel on a spring with a little overshoot; the thumb stretches toward where it's
    // going mid-flight and snaps round again as it lands, like a drop of something viscous.
    val travel by animateFloatAsState(if (checked) 1f else 0f, ZenMotion.bouncy(), label = "thumb")
    val inFlight = (1f - abs(travel * 2f - 1f)).coerceIn(0f, 1f)
    val stretch = 6.rdp * inFlight
    val offset = 20.rdp * travel - if (checked) stretch else 0.rdp
    Box(
        modifier = modifier
            .size(width = 52.rdp, height = 32.rdp)
            .clip(CircleShape)
            .background(track)
            .then(if (checked || !outlineWhenOff) Modifier else Modifier.border(1.dp, colors.borderOutline, CircleShape))
            .padding(4.rdp)
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(width = 24.rdp + stretch, height = 24.rdp)
                .shadow(2.rdp, CircleShape)
                .clip(CircleShape)
                .background(colors.switchThumb)
        )
    }
}

@Composable
fun ZenSettingToggleItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.rdp)
            .zenToggleable(value = checked, onValueChange = onCheckedChange)
            .padding(horizontal = 16.rdp, vertical = 10.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            RowTitle(text, ProTagState.None)
            if (subtitle != null) RowSubtitle(subtitle)
        }
        Spacer(Modifier.width(12.rdp))
        ZenSwitch(checked)
    }
}

data class SegmentOption<T>(val value: T, val label: String, val locked: Boolean = false)

/** Pill segmented control. A locked option stays selectable and routes to the Pro sheet. */
@Composable
fun <T> ZenSegmented(
    options: List<SegmentOption<T>>,
    selected: T,
    onSelect: (SegmentOption<T>) -> Unit,
    modifier: Modifier = Modifier,
    mono: Boolean = true
) {
    val colors = ZenTheme.colors
    val feedback = rememberZenFeedback()
    val density = LocalDensity.current
    // Each option reports where it sits; one pill slides between them on a spring rather than
    // each option painting its own background.
    val bounds = remember(options.size) { mutableStateListOf(*Array(options.size) { 0f to 0f }) }
    val selectedIndex = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    val target = bounds.getOrElse(selectedIndex) { 0f to 0f }
    val pillX by animateFloatAsState(target.first, ZenMotion.bouncy(), label = "segmentX")
    val pillW by animateFloatAsState(target.second, ZenMotion.settle(), label = "segmentW")
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.surfaceSunk)
            .padding(3.rdp)
    ) {
        if (target.second > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .offset { IntOffset(pillX.roundToInt(), 0) }
                    .width(with(density) { pillW.toDp() })
                    .fillMaxHeight()
                    .shadow(1.rdp, CircleShape)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
            )
        }
    Row(horizontalArrangement = Arrangement.spacedBy(2.rdp)) {
        options.forEachIndexed { index, option ->
            val isSelected = option.value == selected
            val label by animateColorAsState(
                if (isSelected) colors.textPrimary else colors.textSecondary,
                ZenMotion.arrive(ZenMotion.FAST),
                label = "segmentLabel"
            )
            Row(
                modifier = Modifier
                    .heightIn(min = 36.rdp)
                    .onPlaced { bounds[index] = it.positionInParent().x to it.size.width.toFloat() }
                    .clip(CircleShape)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = {
                        if (!isSelected) feedback.select()
                        onSelect(option)
                    })
                    .padding(horizontal = 12.rdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option.label,
                    fontFamily = if (mono) DepartureMono else Geist,
                    fontWeight = if (mono) FontWeight.Normal else FontWeight.Medium,
                    fontSize = if (mono) 12.rsp else 13.rsp,
                    color = label
                )
                if (option.locked) {
                    Spacer(Modifier.width(6.rdp))
                    ZenProTag(unlocked = false)
                }
            }
        }
    }
    }
}

enum class ZenButtonStyle { Primary, Outline, Ghost, Danger }

@Composable
fun ZenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ZenButtonStyle = ZenButtonStyle.Primary,
    enabled: Boolean = true,
    contentColor: Color? = null
) {
    val colors = ZenTheme.colors
    val shape = RoundedCornerShape(ControlRadius)
    val (bg, fg, border) = when (style) {
        ZenButtonStyle.Primary -> Triple(colors.actionPrimary, colors.actionPrimaryText, null)
        ZenButtonStyle.Outline -> Triple(null, colors.textPrimary, BorderStroke(1.dp, colors.borderOutline))
        ZenButtonStyle.Ghost -> Triple(null, colors.textSecondary, null)
        ZenButtonStyle.Danger -> Triple(colors.accentDeduct, colors.textOnDeduct, null)
    }
    val feedback = rememberZenFeedback()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Sinks fast under the finger, springs back with a touch of give.
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        if (pressed) ZenMotion.snappy() else ZenMotion.bouncy(),
        label = "buttonPress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (style == ZenButtonStyle.Ghost) 48.rdp else 52.rdp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(if (bg != null) Modifier.background(bg) else Modifier)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, enabled = enabled, role = Role.Button) {
                feedback.tap()
                onClick()
            }
            .padding(horizontal = 20.rdp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.rsp,
            color = (contentColor ?: fg).copy(alpha = if (enabled) 1f else 0.5f)
        )
    }
}

/** v3 bottom sheet: paper surface, 24dp top radius, quiet grabber. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ZenTheme.colors
    val feedback = rememberZenFeedback()
    DisposableEffect(Unit) {
        feedback.sheetOpen()
        onDispose { feedback.sheetClose() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.bgSecondary,
        scrimColor = colors.textPrimary.copy(alpha = 0.38f),
        shape = RoundedCornerShape(topStart = 24.rdp, topEnd = 24.rdp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.rdp, bottom = 12.rdp)
                    .size(width = 40.rdp, height = 4.rdp)
                    .clip(CircleShape)
                    .background(colors.borderOutline)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .staggeredEntrance(index = 1, stepMillis = 70, rise = 12.dp)
                .padding(horizontal = 20.rdp)
                .padding(bottom = 28.rdp),
            verticalArrangement = Arrangement.spacedBy(14.rdp),
            content = content
        )
    }
}

@Composable
fun ZenSheetTitle(text: String) {
    Text(
        text = text,
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 24.rsp,
        lineHeight = 30.rsp,
        color = ZenTheme.colors.textPrimary,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
fun ZenSheetBody(text: String, emphasis: Boolean = false) {
    Text(
        text = text,
        fontFamily = Geist,
        fontSize = 15.rsp,
        lineHeight = 22.rsp,
        color = if (emphasis) ZenTheme.colors.textPrimary else ZenTheme.colors.textSecondary
    )
}

/** Mono key / value line for receipts. */
@Composable
fun ZenReceiptLine(key: String, value: String, showDivider: Boolean = true) {
    val colors = ZenTheme.colors
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(key.uppercase(), style = ZenTypography.monoLabel, color = colors.textSecondary, modifier = Modifier.weight(1f))
            Text(value, fontFamily = DepartureMono, fontSize = 16.rsp, color = colors.textPrimary)
        }
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = colors.borderHairlineSoft)
    }
}

@Composable
fun VerticalGap(height: Dp) = Spacer(Modifier.height(height))
