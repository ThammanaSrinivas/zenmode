package com.zenlauncher.zenmode.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.SettingsActivity
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import androidx.compose.foundation.systemGestureExclusion as foundationSystemGestureExclusion


// ── Shared v3 chrome ──────────────────────────────────────────────
// Pieces every v3 page draws the same way: Figma's tapered top/bottom stroke, the ☰ that
// always leads to Settings, and the three home-page dots.

/**
 * Figma's `border-t` / `border-b` on a rounded box: the outer rounded rect minus the same box
 * inset by [top] and [bottom], so each rule is full width along the edge and tapers to nothing
 * as it runs round the corners. Radii are clamped the way CSS clamps them, so a short box with a
 * big radius becomes a pill whose stroke fades out at its vertical middle.
 */
fun Modifier.taperedBorder(
    color: Color,
    cornerRadius: Dp,
    top: Dp = 0.dp,
    bottom: Dp = 0.dp
): Modifier = drawWithCache {
    val r = cornerRadius.toPx().coerceAtMost(size.minDimension / 2f)
    val t = top.toPx()
    val b = bottom.toPx()
    val outer = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }
    val innerTop = CornerRadius(r, (r - t).coerceAtLeast(0f))
    val innerBottom = CornerRadius(r, (r - b).coerceAtLeast(0f))
    val inner = Path().apply {
        addRoundRect(
            RoundRect(
                left = 0f, top = t, right = size.width, bottom = size.height - b,
                topLeftCornerRadius = innerTop,
                topRightCornerRadius = innerTop,
                bottomRightCornerRadius = innerBottom,
                bottomLeftCornerRadius = innerBottom
            )
        )
    }
    val border = Path.combine(PathOperation.Difference, outer, inner)
    onDrawBehind { drawPath(border, color) }
}

/** Opens Settings — where every ☰ in the app leads (Zen Circle's own menu is the exception). */
fun openSettings(context: Context) {
    context.startActivity(Intent(context, SettingsActivity::class.java))
}

/**
 * The universal ☰. Always opens Settings, so pages never wire it themselves. The glyph keeps
 * its Figma size in the layout while the tap target is a full 48dp around it.
 */
@Composable
fun SettingsMenuButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier.size(width = 29.rdp, height = 17.5.rdp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .requiredSize(48.dp)
                .clip(CircleShape)
                .pressScale(onClick = { openSettings(context) }, onClickLabel = "Open settings")
                .semantics { contentDescription = "Settings" },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_hamburger_menu),
                contentDescription = null,
                modifier = Modifier.size(width = 29.rdp, height = 17.5.rdp)
            )
        }
    }
}

/** The three home pages, left to right. Swipe right from Home for Zen Score, left for Zen Gold. */
enum class HomePage { ZEN_SCORE, HOME, ZEN_GOLD }

/** Page-position dots shared by the three home pages. Purely visual — each page owns its swipe. */
@Composable
fun HomePageDots(current: HomePage, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.rdp)
            .semantics { contentDescription = "Page ${current.ordinal + 1} of ${HomePage.entries.size}" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomePage.entries.forEach { page ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.rdp)
                    .size(7.rdp)
                    .clip(CircleShape)
                    .background(
                        if (page == current) colors.textBrand
                        else colors.textPrimary.copy(alpha = 0.35f)
                    )
            )
        }
    }
}

/**
 * The sticky foot of a scrolling home page: the page's main actions ([content]) and the
 * page dots, held at the bottom over a fade into [fadeTo] so they stay on screen however far
 * the page scrolls. Place it last in a Box, aligned to the bottom, and leave [onHeightChanged]'s
 * height free at the end of the scrolling content so nothing hides under it for good.
 */
@Composable
fun PinnedPageFooter(
    current: HomePage,
    fadeTo: Color,
    onHeightChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val density = LocalDensity.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChanged(with(density) { it.height.toDp() }) }
            .background(Brush.verticalGradient(0f to fadeTo.copy(alpha = 0f), 0.14f to fadeTo.copy(alpha = 0.85f), 0.28f to fadeTo, 1f to fadeTo))
            // Swallow taps between the buttons so they don't fall through to the page.
            .pointerInput(Unit) { detectTapGestures() }
            .padding(top = 22.rdp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
        HomePageDots(current = current, modifier = Modifier.padding(bottom = 6.rdp))
    }
}

/**
 * Horizontal page swipe for the home pages. Fires once per gesture, as soon as the finger has
 * travelled far enough, so a long or fast swipe can't open a page twice. A null direction is
 * ignored (the drag is left for anything underneath).
 */
fun Modifier.pageSwipe(onSwipeLeft: (() -> Unit)? = null, onSwipeRight: (() -> Unit)? = null): Modifier =
    pointerInput(onSwipeLeft != null, onSwipeRight != null) {
        val threshold = 56.dp.toPx()
        var travelled = 0f
        var fired = false
        detectHorizontalDragGestures(
            onDragStart = {
                travelled = 0f
                fired = false
            }
        ) { change, dragAmount ->
            if (fired) return@detectHorizontalDragGestures
            travelled += dragAmount
            val action = when {
                travelled <= -threshold -> onSwipeLeft
                travelled >= threshold -> onSwipeRight
                else -> null
            } ?: return@detectHorizontalDragGestures
            fired = true
            change.consume()
            action()
        }
    }

/**
 * Excludes the composable's layout bounds from the system gesture (back-swipe) zone.
 * Required on OEM ROMs (e.g. MIUI) that widen the edge zone so aggressively that
 * [pageSwipe] gestures starting near a screen edge are silently swallowed before
 * Compose sees them. Delegates to Compose Foundation's [foundationSystemGestureExclusion] which
 * maps to [android.view.View.setSystemGestureExclusionRects] (API 29+; no-op below).
 */
fun Modifier.systemGestureExclusion(): Modifier =
    foundationSystemGestureExclusion { coords ->
        androidx.compose.ui.geometry.Rect(
            left = 0f,
            top = 0f,
            right = coords.size.width.toFloat(),
            bottom = coords.size.height.toFloat()
        )
    }




