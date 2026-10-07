package com.zenlauncher.zenmode.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints

/**
 * Utility composable for creating proportional consistent vertical gaps.
 * This can be used to distribute space consistently across onboarding pages.
 */
@Composable
fun ColumnScope.WeightSpacer(weight: Float) {
    if (weight > 0f) {
        Spacer(modifier = Modifier.weight(weight))
    }
}

/**
 * Utility composable for creating proportional consistent horizontal gaps.
 */
@Composable
fun RowScope.WeightSpacer(weight: Float) {
    if (weight > 0f) {
        Spacer(modifier = Modifier.weight(weight))
    }
}

/**
 * Lays [content] out at its natural width, then scales the whole block down uniformly if it
 * would be wider than the space it is given. For fixed-ratio blocks drawn from a Figma frame
 * — a big mono readout, a label beside a meter — where wrapping or clipping would break the
 * composition but shrinking keeps it intact at any font scale or screen width.
 */
@Composable
fun FitToWidth(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)) }
        val naturalWidth = placeables.maxOfOrNull { it.width } ?: 0
        val naturalHeight = placeables.maxOfOrNull { it.height } ?: 0
        val scale = if (naturalWidth > constraints.maxWidth && naturalWidth > 0) {
            constraints.maxWidth.toFloat() / naturalWidth
        } else 1f
        val width = (naturalWidth * scale).toInt().coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (naturalHeight * scale).toInt().coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            placeables.forEach { placeable ->
                placeable.placeWithLayer(0, 0) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        }
    }
}
