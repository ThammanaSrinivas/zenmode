package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ThemeMode
import com.zenlauncher.zenmode.HomeTheme
import com.zenlauncher.zenmode.ui.components.accent
import com.zenlauncher.zenmode.ui.components.wash
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import com.zenlauncher.zenmode.ui.theme.isInk
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.Brush
import com.zenlauncher.zenmode.ThemePreferences
import com.zenlauncher.zenmode.ui.components.ZenMotion
import com.zenlauncher.zenmode.ui.components.rememberZenFeedback
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.colorResource
import kotlin.math.cos
import kotlin.math.sin
import com.zenlauncher.zenmode.ui.components.SegmentOption
import com.zenlauncher.zenmode.ui.components.ZenButton
import com.zenlauncher.zenmode.ui.components.ZenEyebrow
import com.zenlauncher.zenmode.ui.components.ZenSegmented
import com.zenlauncher.zenmode.ui.components.ZenSheet
import com.zenlauncher.zenmode.ui.components.ZenSheetBody
import com.zenlauncher.zenmode.ui.components.ZenSheetTitle
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * Settings → Look & sound: the Pro home-theme picker and the paper/ink Appearance control.
 * Split out of SettingsScreen.kt to keep that file under the 1000-line ceiling.
 */

// ── Home themes (Pro) ──────────────────────────────────────────────

@Composable
internal fun HomeThemeSheet(current: HomeTheme, onPick: (HomeTheme) -> Unit, onDismiss: () -> Unit) {
    val feedback = rememberZenFeedback()
    ZenSheet(onDismiss = onDismiss) {
        ZenEyebrow("Included in your Pro")
        ZenSheetTitle("Home-screen theme")
        ZenSheetBody("The wash behind Home, your Zen Score and Zen Gold. Mood keeps grading your day; the rest just stay calm.")
        HomeTheme.entries.chunked(3).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.rdp)) {
                row.forEachIndexed { i, theme ->
                    HomeThemeTile(
                        theme = theme,
                        selected = theme == current,
                        onClick = {
                            if (theme != current) {
                                feedback.select()
                                onPick(theme)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .staggeredEntrance(index = rowIndex * 3 + i, stepMillis = 40)
                    )
                }
            }
        }
        ZenButton(text = "Done", onClick = onDismiss)
    }
}

/** A tiny phone: the theme's wash with one soft pool, the name beneath. Mood shows all three days. */
@Composable
private fun HomeThemeTile(theme: HomeTheme, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    val ink = colors.isInk
    val shape = RoundedCornerShape(16.rdp)
    val lift by animateFloatAsState(if (selected) 1f else 0f, ZenMotion.bouncy(), label = "tileLift")
    val ring by animateColorAsState(if (selected) colors.textBrand else colors.borderSubtle, ZenMotion.arrive(), label = "tileRing")
    val washes = if (theme == HomeTheme.MOOD) {
        listOf(colors.washHappy, colors.washNeutral, colors.washAnnoyed)
    } else listOf(theme.wash(ink))
    val accent = if (theme == HomeTheme.MOOD) colors.strokeHappy else theme.accent(ink)
    Column(
        modifier = modifier
            .graphicsLayer {
                val scale = 0.96f + 0.04f * lift
                scaleX = scale
                scaleY = scale
            }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.rdp)
                .clip(shape)
                .border((1f + lift).dp, ring, shape)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val band = size.width / washes.size
                washes.forEachIndexed { i, wash ->
                    drawRect(
                        brush = Brush.verticalGradient(wash),
                        topLeft = Offset(band * i, 0f),
                        size = Size(band, size.height)
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.45f), accent.copy(alpha = 0f)),
                        center = Offset(size.width * 0.7f, size.height * 0.62f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.7f, size.height * 0.62f)
                )
            }
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.rdp)
                        .size(18.rdp)
                        .graphicsLayer {
                            scaleX = lift
                            scaleY = lift
                        }
                        .clip(CircleShape)
                        .background(colors.textBrand),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", fontSize = 11.rsp, color = colors.actionPrimaryText)
                }
            }
        }
        Text(
            text = theme.label,
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.rsp,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 6.rdp)
        )
        Text(
            text = theme.caption,
            fontFamily = Geist,
            fontSize = 11.rsp,
            color = colors.textSecondary,
            maxLines = 1
        )
    }
}

// ── Appearance ─────────────────────────────────────────────────────

@Composable
internal fun AppearanceRow(mode: ThemeMode, onModeChange: (ThemeMode) -> Unit) {
    val colors = ZenTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.rdp, vertical = 14.rdp),
        verticalArrangement = Arrangement.spacedBy(12.rdp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Appearance",
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.rsp,
                    lineHeight = 22.rsp,
                    color = colors.textPrimary
                )
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        (fadeIn(ZenMotion.arrive()) + slideInVertically(ZenMotion.arrive()) { it / 3 })
                            .togetherWith(fadeOut(ZenMotion.leave()))
                    },
                    label = "appearanceCaption"
                ) { shown ->
                    Text(
                        text = when (shown) {
                            ThemeMode.SYSTEM -> "Follows your phone, paper by day and ink by night."
                            ThemeMode.LIGHT -> "Paper. Warm and bright."
                            ThemeMode.DARK -> "Ink. Easy on the eyes after dark."
                        },
                        fontFamily = Geist,
                        fontSize = 13.rsp,
                        lineHeight = 18.rsp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.rdp)
                    )
                }
            }
            ThemeSwatch(dark = ThemePreferences.isDarkMode(LocalContext.current))
        }
        ZenSegmented(
            options = ThemeMode.entries.map { SegmentOption(it, it.label) },
            selected = mode,
            onSelect = { if (it.value != mode) onModeChange(it.value) },
            mono = false
        )
    }
}

/** A small sun/moon that turns over when the theme changes. */
@Composable
private fun ThemeSwatch(dark: Boolean) {
    val colors = ZenTheme.colors
    val turn by animateFloatAsState(if (dark) 180f else 0f, ZenMotion.bouncy(), label = "swatchTurn")
    val moonCut by animateFloatAsState(if (dark) 1f else 0f, ZenMotion.settle(), label = "moonCut")
    val sun = colorResource(R.color.amber_500)
    Canvas(
        Modifier
            .size(36.rdp)
            .graphicsLayer { rotationZ = turn }
    ) {
        val r = size.minDimension * 0.24f
        val c = center
        // Rays shrink into the body as it becomes a moon.
        val rayAlpha = 1f - moonCut
        if (rayAlpha > 0f) {
            repeat(8) { i ->
                val a = Math.toRadians(i * 45.0)
                val inner = r * 1.45f
                val outer = r * (1.45f + 0.55f * rayAlpha)
                drawLine(
                    color = sun.copy(alpha = rayAlpha),
                    start = Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                    end = Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        drawCircle(lerp(sun, colors.textBrand, moonCut), radius = r * (1f + 0.25f * moonCut), center = c)
        if (moonCut > 0f) {
            // The bite that makes it a crescent slides in from the corner.
            drawCircle(
                color = colors.bgSecondary,
                radius = r * 1.05f,
                center = Offset(c.x + r * 0.75f, c.y - r * 0.6f) + Offset(r * 1.4f, -r * 1.4f) * (1f - moonCut)
            )
        }
    }
}
