package com.zenlauncher.zenmode.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// Frosted overlay: the screen underneath goes soft, a translucent paper panel rises from the
// bottom. Eyebrow + close on top, display title, green tagline, icon rows, hairline, footnote.
// The blur itself is applied by the caller to the screen behind via [zenOverlayBlur], because
// Compose can only blur what it draws, not what sits under a sibling.

/** Blurs the screen behind an open [ZenFrostedOverlay]. A no-op below Android 12; the scrim covers it. */
@Composable
fun Modifier.zenOverlayBlur(active: Boolean): Modifier {
    val radius by animateDpAsState(if (active) 24.dp else 0.dp, tween(260), label = "overlayBlur")
    return if (radius > 0.dp) blur(radius) else this
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ZenFrostedOverlay(
    visible: Boolean,
    eyebrow: String,
    onDismiss: () -> Unit,
    onBack: () -> Unit = onDismiss,
    /** Pin the panel at its full height, so content that changes (e.g. search results) never resizes it. */
    fillHeight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ZenTheme.colors

    if (visible) BackHandler { onBack() }

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(180))) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bgPrimary.copy(alpha = 0.28f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                )
        ) {
            // Leave the back arrow clear above the panel.
            val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val panelMaxHeight = maxHeight - statusBar - 72.rdp

            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 12.rdp, top = 8.rdp)
                    .size(48.rdp)
                    .clip(CircleShape)
                    .clickable(onClickLabel = "Back", role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                BackArrowGlyph(colors.textBrand)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .animateEnterExit(
                        enter = slideInVertically(tween(280)) { it / 3 },
                        exit = slideOutVertically(tween(200)) { it / 3 }
                    )
                    .fillMaxWidth()
                    .then(if (fillHeight) Modifier.height(panelMaxHeight) else Modifier.heightIn(max = panelMaxHeight))
                    .clip(RoundedCornerShape(topStart = 28.rdp, topEnd = 28.rdp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.bgSecondary.copy(alpha = 0.92f),
                                lerpTint(colors.bgSecondary, colors.textBrand).copy(alpha = 0.96f)
                            )
                        )
                    )
                    // Swallow taps so they don't reach the dismiss scrim.
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {}
                    )
                    .navigationBarsPadding()
                    .padding(start = 24.rdp, end = 16.rdp, top = 20.rdp, bottom = 24.rdp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = eyebrow.uppercase(),
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.rsp,
                        letterSpacing = 0.2.rsp,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f).semantics { heading() }
                    )
                    Box(
                        modifier = Modifier
                            .size(40.rdp)
                            .clip(CircleShape)
                            .clickable(onClickLabel = "Close", role = Role.Button, onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        CloseGlyph(colors.textSecondary)
                    }
                }
                // Flexes so a scrolling child (e.g. a grid) can take the room that's left.
                Column(modifier = Modifier.weight(1f, fill = fillHeight).padding(end = 8.rdp), content = content)
            }
        }
    }
}

/** Paper with a breath of brand green, for the bottom of the frosted panel. */
private fun lerpTint(base: Color, tint: Color): Color =
    androidx.compose.ui.graphics.lerp(base, tint, 0.06f)

/** Long left arrow, as in the frosted overlay design. */
@Composable
private fun BackArrowGlyph(color: Color) {
    Canvas(Modifier.size(width = 26.rdp, height = 18.rdp)) {
        val stroke = 2.2.dp.toPx()
        val midY = size.height / 2
        val head = size.height / 2
        drawLine(color, Offset(0f, midY), Offset(size.width, midY), stroke, StrokeCap.Round)
        drawLine(color, Offset(0f, midY), Offset(head, midY - head), stroke, StrokeCap.Round)
        drawLine(color, Offset(0f, midY), Offset(head, midY + head), stroke, StrokeCap.Round)
    }
}

@Composable
private fun CloseGlyph(color: Color) {
    Canvas(Modifier.size(14.rdp)) {
        val stroke = 1.6.dp.toPx()
        drawLine(color, Offset.Zero, Offset(size.width, size.height), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
    }
}

@Composable
fun ZenOverlayTitle(text: String) {
    Text(
        text = text,
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 24.rsp,
        lineHeight = 30.rsp,
        color = ZenTheme.colors.textPrimary,
        modifier = Modifier.padding(top = 2.rdp)
    )
}

@Composable
fun ZenOverlayTagline(text: String) {
    Text(
        text = text,
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.rsp,
        lineHeight = 24.rsp,
        color = ZenTheme.colors.textBrand,
        modifier = Modifier.padding(top = 10.rdp)
    )
}

/** Icon + title + one-line subtitle. The icon sits in a 24dp column, brand green. */
@Composable
fun ZenOverlayAction(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val colors = ZenTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.rdp))
            .clickable(onClickLabel = title, role = Role.Button, onClick = onClick)
            .padding(vertical = 12.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(24.rdp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(16.rdp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 17.rsp,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                fontFamily = Geist,
                fontSize = 13.rsp,
                lineHeight = 18.rsp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.rdp)
            )
        }
    }
}

@Composable
fun ZenOverlayFootnote(text: String) {
    Column(Modifier.padding(top = 8.rdp)) {
        HorizontalDivider(thickness = 1.dp, color = ZenTheme.colors.borderHairlineSoft)
        Text(
            text = text,
            fontFamily = Geist,
            fontSize = 14.rsp,
            lineHeight = 20.rsp,
            color = ZenTheme.colors.textSecondary,
            modifier = Modifier.padding(top = 16.rdp, start = 12.rdp, end = 12.rdp)
        )
    }
}

/** 2×2 rounded tiles: the home grid. */
@Composable
fun HomeGridGlyph(color: Color = ZenTheme.colors.textBrand) {
    Canvas(Modifier.size(18.rdp)) {
        val gap = size.width * 0.16f
        val tile = (size.width - gap) / 2
        val r = androidx.compose.ui.geometry.CornerRadius(tile * 0.3f)
        for (row in 0..1) for (col in 0..1) {
            drawRoundRect(
                color = color,
                topLeft = Offset(col * (tile + gap), row * (tile + gap)),
                size = androidx.compose.ui.geometry.Size(tile, tile),
                cornerRadius = r
            )
        }
    }
}

/** Circled "i". */
@Composable
fun InfoGlyph(color: Color = ZenTheme.colors.textBrand) {
    Canvas(Modifier.size(20.rdp)) {
        val stroke = 1.8.dp.toPx()
        drawCircle(color, radius = size.minDimension / 2 - stroke / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawCircle(color, radius = stroke * 0.75f, center = Offset(size.width / 2, size.height * 0.3f))
        drawLine(color, Offset(size.width / 2, size.height * 0.45f), Offset(size.width / 2, size.height * 0.72f), stroke, StrokeCap.Round)
    }
}
