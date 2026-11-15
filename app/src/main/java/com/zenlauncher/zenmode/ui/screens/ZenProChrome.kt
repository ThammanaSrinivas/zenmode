package com.zenlauncher.zenmode.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.GlyphKind
import com.zenlauncher.zenmode.ui.components.ZenGlyph
import com.zenlauncher.zenmode.ui.components.rememberReduceMotion
import com.zenlauncher.zenmode.ui.theme.Spacing
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp

// The chrome every Pro surface shares: the back bar and the OS gradient rule.
// They live in their own file so ZenProScreen.kt stays about the plan itself.

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
                .clickable(onClickLabel = stringResource(R.string.common_back), role = Role.Button, onClick = onBackClick),
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
