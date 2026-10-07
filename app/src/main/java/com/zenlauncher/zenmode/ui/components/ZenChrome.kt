package com.zenlauncher.zenmode.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.semantics.Role
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

/** Page-position dots shared by the three home pages. Each page owns its swipe; tapping a dot
 * for a page other than [current] jumps straight there via [onPageClick], same destination a
 * swipe would reach. */
@Composable
fun HomePageDots(
    current: HomePage,
    modifier: Modifier = Modifier,
    onPageClick: ((HomePage) -> Unit)? = null
) {
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
                // A 24dp slot around the 7dp dot — a real tap target without the row growing
                // wide enough to read as spaced-out rather than a tight page indicator.
                modifier = Modifier
                    .size(24.rdp)
                    .then(
                        if (onPageClick != null && page != current) {
                            Modifier.clip(CircleShape).clickable(
                                onClickLabel = "Go to ${page.name.lowercase().replace('_', ' ')}",
                                role = Role.Button
                            ) { onPageClick(page) }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
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
}

// ── Page footer metrics ───────────────────────────────────────────
// SOURCE OF TRUTH for where a home page's main action sits. Home, Zen Score and Zen Gold are
// one left-right swipe apart, so their primary actions have to land on the same line —
// otherwise the button slides up and down as you page between them. The footer fixes that by
// owning the slot heights rather than letting each page stack whatever it has: the primary
// action is always [ZenPrimaryCtaHeight] tall and always the top row, and the quieter action
// below it always occupies [ZenSecondaryCtaSlot] whether or not the page has one.

/** Every page's main action is this tall, so they stack to the same line. */
val ZenPrimaryCtaHeight: Dp @Composable get() = 52.rdp

/** Reserved under the primary action, even on a page with no secondary — that reservation
 *  is what keeps the primary action from sliding down on the pages that don't have one. */
val ZenSecondaryCtaSlot: Dp @Composable get() = 42.rdp

private val CtaGap: Dp @Composable get() = 4.rdp

/**
 * The sticky foot of a home page: the page's [primary] action, a quieter [secondary] under it,
 * and the page dots, held at the bottom over a fade into [fadeTo] so they stay on screen
 * however far the page scrolls. Place it last in a Box, aligned to the bottom, and leave
 * [onHeightChanged]'s height free at the end of the scrolling content so nothing hides under
 * it for good.
 *
 * [primary] is measured at exactly [ZenPrimaryCtaHeight] and [secondary] inside a
 * [ZenSecondaryCtaSlot]-tall box, so every page's footer is the same height to the pixel and
 * nothing moves as you swipe between them.
 */
@Composable
fun PinnedPageFooter(
    current: HomePage,
    /** The page colour the footer fades out of, so scrolling content disappears under it
     *  rather than running into it. Null on a page that doesn't scroll (Home), where the
     *  fade would just be a visible band across the backdrop. */
    fadeTo: Color?,
    onHeightChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier,
    onPageClick: ((HomePage) -> Unit)? = null,
    horizontalMargin: Dp? = null,
    primary: (@Composable () -> Unit)? = null,
    secondary: (@Composable () -> Unit)? = null
) {
    val density = LocalDensity.current
    val margin = horizontalMargin ?: 30.rdp
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChanged(with(density) { it.height.toDp() }) }
            .then(
                if (fadeTo == null) Modifier
                else Modifier.background(
                    Brush.verticalGradient(
                        0f to fadeTo.copy(alpha = 0f),
                        0.14f to fadeTo.copy(alpha = 0.85f),
                        0.28f to fadeTo,
                        1f to fadeTo
                    )
                )
            )
            // Swallow taps between the buttons so they don't fall through to the page.
            .pointerInput(Unit) { detectTapGestures() }
            .padding(top = 22.rdp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (primary != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = margin)
                    .height(ZenPrimaryCtaHeight),
                contentAlignment = Alignment.Center
            ) { primary() }
        }
        // Reserved whether or not there is a secondary — see [ZenSecondaryCtaSlot].
        Spacer(Modifier.height(CtaGap))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = margin)
                .height(ZenSecondaryCtaSlot),
            contentAlignment = Alignment.Center
        ) { secondary?.invoke() }
        HomePageDots(current = current, onPageClick = onPageClick, modifier = Modifier.padding(bottom = 6.rdp))
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
 * Vertical companion to [pageSwipe], for the Home "swipe up for search" gesture. Fires once per
 * gesture as soon as the finger has travelled far enough upward, and ignores downward drags
 * (the notification shade's pull-down is the system's, not ours).
 *
 * Kept separate from [pageSwipe] rather than folded into one all-direction detector: a combined
 * [androidx.compose.foundation.gestures.detectDragGestures] claims vertical drags too, which
 * would eat the scroll on the other pages that use [pageSwipe]. As two axis-specific detectors
 * they don't collide — a drag only reaches this one by crossing Compose's *vertical* touch slop,
 * and once [pageSwipe] consumes a change at its own threshold this detector cancels. Apply it
 * only where nothing underneath scrolls vertically.
 *
 * A null [onSwipeUp] leaves the drag alone entirely.
 */
fun Modifier.swipeUp(onSwipeUp: (() -> Unit)?): Modifier =
    if (onSwipeUp == null) this else pointerInput(Unit) {
        val threshold = 72.dp.toPx()
        var travelled = 0f
        var fired = false
        detectVerticalDragGestures(
            onDragStart = {
                travelled = 0f
                fired = false
            }
        ) { change, dragAmount ->
            if (fired) return@detectVerticalDragGestures
            travelled += dragAmount
            if (travelled > -threshold) return@detectVerticalDragGestures
            fired = true
            change.consume()
            onSwipeUp()
        }
    }

/** Which side of a page a margin tap landed on. */
enum class PageMargin { LEFT, RIGHT }

/**
 * Home's tap gestures, in one detector so they can't fight over the same tap: a long press, an
 * optional double tap, and an optional tap in either margin.
 *
 * [marginWidth] is the strip down each edge that counts as a margin — the page's own content
 * margin, so a margin tap is by definition a tap beside the content rather than on it. Taps on
 * anything with its own click handler (an app icon, a card) are consumed there and never arrive
 * here.
 *
 * [onDoubleTap] is null when the gesture is off, which matters beyond doing nothing: with a
 * double-tap handler registered, every single tap has to wait out the double-tap timeout before
 * [onMarginTap] can fire, so leaving it null keeps margin taps instant.
 */
fun Modifier.pageTapGestures(
    marginWidth: Dp,
    onLongPress: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    onMarginTap: ((PageMargin) -> Unit)? = null
    // Keyed on which handlers exist, never on their identity: these lambdas are rebuilt on
    // every recomposition, and keying on them would restart the detector mid-gesture — a long
    // press on Home would be cancelled by the clock ticking. Same reasoning as [pageSwipe].
): Modifier = pointerInput(onLongPress != null, onDoubleTap != null, onMarginTap != null) {
    val marginPx = marginWidth.toPx()
    detectTapGestures(
        onLongPress = onLongPress?.let { { _ -> it() } },
        onDoubleTap = onDoubleTap?.let { { _ -> it() } },
        onTap = onMarginTap?.let { tap ->
            { offset ->
                when {
                    offset.x <= marginPx -> tap(PageMargin.LEFT)
                    offset.x >= size.width - marginPx -> tap(PageMargin.RIGHT)
                    else -> Unit
                }
            }
        }
    )
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




