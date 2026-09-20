package com.zenlauncher.zenmode.ui.components

import com.zenlauncher.zenmode.ui.theme.ZenTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ── ZenMode OS wordmark ─────────────────────────────────────────────
// Brand rule: in "ZenMode OS" the "OS" is always drawn in the brand OS gradient.
// Every screen that names the product goes through this file.

const val PRODUCT_NAME = "ZenMode OS"

/**
 * Figma's "Brand zen gradient" style: a CSS linear-gradient at -67.92deg through
 * score_grad_start / score_grad_mid / score_orange. Brush.linearGradient can't express
 * a CSS angle against an unknown text box, so the line is resolved from the laid-out size.
 */
@Composable
fun rememberBrandOsGradient(): Brush {
    val start = colorResource(R.color.score_grad_start)
    val mid = colorResource(R.color.score_grad_mid)
    val end = colorResource(R.color.score_orange)
    return remember(start, mid, end) {
        cssLinearGradient(
            angleDegrees = -67.92291865051479f,
            colors = listOf(start, mid, end),
            stops = listOf(0.23558f, 0.50636f, 0.71412f)
        )
    }
}

fun cssLinearGradient(angleDegrees: Float, colors: List<Color>, stops: List<Float>): ShaderBrush =
    object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            val radians = Math.toRadians(angleDegrees.toDouble())
            val dx = sin(radians).toFloat()
            val dy = -cos(radians).toFloat()
            val halfLength = (abs(size.width * dx) + abs(size.height * dy)) / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            return LinearGradientShader(
                from = Offset(cx - dx * halfLength, cy - dy * halfLength),
                to = Offset(cx + dx * halfLength, cy + dy * halfLength),
                colors = colors,
                colorStops = stops
            )
        }
    }

/**
 * A [Text] for running copy that names the product: every "ZenMode OS" in [text] gets
 * the gradient "OS". A span brush would be sized to the whole paragraph, so the "OS" is
 * an inline element measured in the same style and filled with its own gradient.
 */
@Composable
fun BrandedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    if (!text.contains(PRODUCT_NAME)) {
        Text(text = text, modifier = modifier, style = style, maxLines = maxLines, overflow = overflow)
        return
    }
    val gradient = rememberBrandOsGradient()
    val osStyle = style.copy(brush = gradient, fontWeight = FontWeight.SemiBold)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val osSize = remember(osStyle, density) { measurer.measure("OS", osStyle, maxLines = 1).size }
    val annotated = remember(text) {
        buildAnnotatedString {
            var cursor = 0
            while (true) {
                val at = text.indexOf(PRODUCT_NAME, cursor)
                if (at < 0) break
                val os = at + PRODUCT_NAME.length - 2
                append(text.substring(cursor, os))
                appendInlineContent(OS_INLINE_ID, "OS")
                cursor = os + 2
            }
            append(text.substring(cursor))
        }
    }
    val inline = with(density) {
        mapOf(
            OS_INLINE_ID to InlineTextContent(
                Placeholder(
                    width = osSize.width.toSp(),
                    height = osSize.height.toSp(),
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) { Text(text = "OS", style = osStyle, maxLines = 1, softWrap = false) }
        )
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = inline
    )
}

private const val OS_INLINE_ID = "zenmode_os"

/** Mark + "ZenMode" + gradient "OS", in Clash Display. */
@Composable
fun ZenModeOsWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 21.sp,
    markSize: Dp? = null,
    markGap: Dp? = null,
    zenModeColor: Color = ZenTheme.colors.textPrimary,
    letterSpacing: TextUnit = (-0.3).sp
) {
    val gradient = rememberBrandOsGradient()
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = PRODUCT_NAME },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (markSize != null) {
            Image(
                painter = painterResource(R.drawable.ic_zen_mark_gradient),
                contentDescription = null,
                modifier = Modifier.size(markSize)
            )
            Spacer(Modifier.width(markGap ?: (markSize / 3)))
        }
        val wordStyle = FullLineBox.copy(
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize,
            letterSpacing = letterSpacing
        )
        Text(text = "ZenMode ", style = wordStyle, color = zenModeColor, maxLines = 1)
        // Its own Text so the gradient spans exactly "OS".
        Text(text = "OS", style = wordStyle.copy(brush = gradient), maxLines = 1)
    }
}

/**
 * "ZenMode OS Settings" — the Settings title and every "back to Settings" label. "OS" keeps
 * the brand gradient; [color] tints the rest.
 */
@Composable
fun ZenModeOsSettingsTitle(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    color: Color = ZenTheme.colors.textPrimary,
    letterSpacing: TextUnit = (-0.44).sp
) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$PRODUCT_NAME Settings" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        ZenModeOsWordmark(fontSize = fontSize, zenModeColor = color, letterSpacing = letterSpacing)
        Text(
            text = " Settings",
            style = FullLineBox.copy(
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = fontSize,
                letterSpacing = letterSpacing
            ),
            color = color,
            maxLines = 1
        )
    }
}
