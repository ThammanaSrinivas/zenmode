package com.zenlauncher.zenmode.share

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.ZenMotion
import com.zenlauncher.zenmode.ui.components.rememberReduceMotion
import com.zenlauncher.zenmode.ui.components.rememberZenFeedback
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ── Share sheet ───────────────────────────────────────────────────
// The bottom sheet every post-shaped card sits in (Figma node 252:3646, "streaks OVerlay"): Home
// or the page behind stays visible, blurred by the caller (zenOverlayBlur), under a translucent
// paper panel with an eyebrow and close, the card itself as the hero, one line of what's next,
// then Save / Share. The card is fitted to whatever height the phone has, at its 4:5 shape;
// what's saved or shared is always the full 1080×1350 card (ShareExport), not this preview.

/** Full-screen scrim over the blurred backdrop — same tone the frosted overlays scrim with. */
internal val ShareSheetScrim: Color @Composable get() = ZenTheme.colors.bgPrimary.copy(alpha = 0.28f)

/** The panel itself: paper at Figma's ~71% so the blur underneath still reads through. */
internal val ShareSheetPanelBg: Color @Composable get() = ZenTheme.colors.bgPrimary.copy(alpha = 0.71f)

private val SheetMargin: Dp @Composable get() = 30.rdp

/**
 * @param art builds the card for a kit; called once for the preview and again, with a kit of
 *   its own, for each export. [key] is what the card shows — a new one rebuilds the preview.
 * @param note one line under the card: what's next, how far.
 */
@Composable
fun ShareSheet(
    eyebrow: String,
    key: Any,
    art: (ShareKit) -> ShareArt,
    note: String?,
    shareLabel: String,
    fileBaseName: String,
    shareText: String,
    chooserTitle: String,
    analyticsKey: String,
    onDismiss: () -> Unit
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val feedback = rememberZenFeedback()
    val kit = rememberShareKit()
    val preview = remember(kit, key) { art(kit) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var busy by remember { mutableStateOf(false) }

    // The panel rises into place on the app's arrival curve.
    val still = rememberReduceMotion() || LocalInspectionMode.current
    val rise = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!still) {
            feedback.sheetOpen()
            rise.animateTo(1f, ZenMotion.arrive(ZenMotion.SLOW))
        }
    }
    val risePx = with(LocalDensity.current) { 48.dp.toPx() }

    BackHandler(enabled = true) { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // The drag is in pixels, and so is this — the sheet follows the finger exactly.
            .offset { IntOffset(0, offsetY.coerceAtLeast(0f).roundToInt()) }
            .background(ShareSheetScrim)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) onDismiss()
                        offsetY = 0f
                    },
                    onVerticalDrag = { _, dragAmount -> offsetY += dragAmount }
                )
            }
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            // Everything in the panel but the card, so the card can have the rest.
            val chrome = 286.rdp
            val cardWidth = minOf(maxWidth - SheetMargin * 2, (maxHeight - chrome) * 0.8f).coerceAtLeast(160.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .graphicsLayer {
                        translationY = (1f - rise.value) * risePx
                        alpha = rise.value
                    }
                    .clip(RoundedCornerShape(topStart = 24.rdp, topEnd = 24.rdp))
                    .background(ShareSheetPanelBg)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { /* consume */ }
                    .padding(horizontal = SheetMargin)
                    .padding(top = 17.rdp, bottom = 28.rdp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = eyebrow,
                        fontFamily = Geist,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.rsp,
                        letterSpacing = (-0.42).sp,
                        color = colors.textPrimary,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_milestone_close),
                        contentDescription = "Close",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(24.rdp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                role = Role.Button
                            ) { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(16.rdp))

                ShareArtView(
                    art = preview,
                    modifier = Modifier
                        .width(cardWidth)
                        .aspectRatio(ShareFormat.POST.width / ShareFormat.POST.height.toFloat())
                        .clip(RoundedCornerShape(20.rdp))
                )

                if (note != null) {
                    Spacer(modifier = Modifier.height(14.rdp))
                    Text(
                        text = note,
                        fontFamily = Geist,
                        fontSize = 14.rsp,
                        letterSpacing = (-0.14).sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.rdp))

                Column(verticalArrangement = Arrangement.spacedBy(12.rdp)) {
                    ShareOutlineButton(text = "Save as image", enabled = !busy) {
                        feedback.saved()
                        trackShare("save_image", analyticsKey)
                        busy = true
                        scope.launch {
                            try {
                                ShareExport.saveImage(context, art, fileBaseName)
                            } finally {
                                busy = false
                            }
                        }
                    }
                    ShareSolidButton(text = shareLabel, enabled = !busy) {
                        feedback.shared()
                        trackShare("share_image", analyticsKey)
                        busy = true
                        scope.launch {
                            try {
                                ShareExport.shareImage(context, art, fileBaseName, shareText, chooserTitle)
                            } finally {
                                busy = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ShareOutlineButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(47.rdp)
            .alpha(if (enabled) 1f else 0.6f)
            .clip(RoundedCornerShape(50))
            .background(colorResource(R.color.milestone_outline_bg))
            .border(1.rdp, colorResource(R.color.zen_700).copy(alpha = 0.26f), RoundedCornerShape(50))
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Image(painter = painterResource(R.drawable.ic_download), contentDescription = null, modifier = Modifier.size(16.rdp))
        Spacer(modifier = Modifier.width(8.rdp))
        Text(
            text = text,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 17.rsp,
            letterSpacing = (-0.35).sp,
            color = colorResource(R.color.gold_delta_text)
        )
    }
}

@Composable
internal fun ShareSolidButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(47.rdp)
            .alpha(if (enabled) 1f else 0.6f)
            .clip(RoundedCornerShape(50))
            .background(colorResource(R.color.zen_700))
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 17.rsp,
            letterSpacing = (-0.35).sp,
            color = Color.White
        )
    }
}
