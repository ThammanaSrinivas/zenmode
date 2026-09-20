package com.zenlauncher.zenmode.ui.screens

import com.zenlauncher.zenmode.Sfx
import com.zenlauncher.zenmode.ZenSound
import androidx.compose.ui.text.TextStyle
import com.zenlauncher.zenmode.ui.components.BrandedText
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Zen Circle sheets ─────────────────────────────────────────────
// Bottom sheets raised over Zen Circle pages: the invite sheet (Figma node 2026:2420) and the
// circle settings sheet behind the ☰ menu (node 2026:2939, content in ZenCircleSettingsSheet.kt).
// Both sit on a frosted scrim over the blurred page with a back arrow in the corner.
//
// Motion: scrim fades in while the sheet springs up from the bottom; drag the sheet down (or
// tap outside, the arrow, or system back) to dismiss. Invite: the close glyph turns from "+"
// into "×" as it lands and the two options rise in one after the other.

/** Page blur behind the open sheet (Figma `backdrop-blur-[14.2px]`). Needs API 31+. */
internal val InviteSheetBlur: Dp = 14.2.dp

/** Real blur exists from Android 12; older devices get a denser scrim and an opaque sheet instead. */
internal val SupportsBlur: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private val InviteSheetHeight: Dp @Composable get() = 400.1.rdp
internal val ZenSheetRadius: Dp @Composable get() = 24.26.rdp

/** The sheets a Zen Circle page can raise over itself. */
enum class ZenCircleSheet { Invite, Settings }

/**
 * A Zen Circle page that can raise its sheets over itself. [content] gets the modifier that
 * blurs the page while a sheet is up (apply it to the page's content, not its background)
 * and a lambda that opens a sheet. [settings] is only needed by pages that open
 * [ZenCircleSheet.Settings]. Opening one sheet from another swaps them in place.
 */
@Composable
internal fun ZenCircleSheetHost(
    userCode: String?,
    onShareInviteLink: () -> Unit,
    onCopyInviteCode: () -> Unit,
    settings: ZenCircleSettings? = null,
    initialSheet: ZenCircleSheet? = null,
    content: @Composable (pageModifier: Modifier, openSheet: (ZenCircleSheet) -> Unit) -> Unit
) {
    var sheet by rememberSaveable { mutableStateOf(initialSheet) }
    val pageBlur by animateDpAsState(
        targetValue = if (sheet != null && SupportsBlur) InviteSheetBlur else 0.dp,
        animationSpec = tween(260),
        label = "zenSheetPageBlur"
    )
    content(Modifier.blur(pageBlur)) { sheet = it }

    ZenSheetScrim(visible = sheet != null, onDismiss = { sheet = null })
    ZenSheetSurface(
        visible = sheet == ZenCircleSheet.Invite,
        paneTitle = "Invite your people",
        onDismiss = { sheet = null }
    ) {
        Box(modifier = Modifier.height(InviteSheetHeight)) {
            SheetContent(
                userCode = userCode,
                onShareInviteLink = onShareInviteLink,
                onCopyInviteCode = onCopyInviteCode
            )
            CloseButton(onDismiss = { sheet = null })
        }
    }
    if (settings != null) {
        ZenSheetSurface(
            visible = sheet == ZenCircleSheet.Settings,
            paneTitle = "Zen Circle settings",
            onDismiss = { sheet = null }
        ) {
            ZenCircleSettingsContent(
                settings = settings,
                onInvitePeople = { sheet = ZenCircleSheet.Invite }
            )
        }
    }
}

/** Frosted scrim over the page plus the back arrow that sits on it (node 2026:2421). */
@Composable
private fun ZenSheetScrim(visible: Boolean, onDismiss: () -> Unit) {
    BackHandler(enabled = visible, onBack = onDismiss)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(240)),
        exit = fadeOut(tween(200))
    ) {
        val scrim = colorResource(R.color.zen_circle_scrim)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (SupportsBlur) scrim else scrim.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Close",
                    onClick = onDismiss
                )
        ) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    // Arrow is 29.6dp wide at x=16, centred 16dp below the status bar.
                    .offset(x = 16.rdp + 14.8.rdp - 24.dp, y = 16.2.rdp - 24.dp)
                    .requiredSize(48.dp)
                    .clip(CircleShape)
                    .clickable(onClickLabel = "Close", onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_zen_circle_back),
                    contentDescription = "Back",
                    modifier = Modifier
                        .width(29.6.rdp)
                        .height(20.3.rdp)
                )
            }
        }
    }
}

/**
 * Translucent bottom sheet that springs up from the bottom edge and can be dragged down to
 * dismiss. Fills the screen so it stacks over the scrim.
 */
@Composable
private fun ZenSheetSurface(
    visible: Boolean,
    paneTitle: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
            ) { it },
            exit = slideOutVertically(tween(220)) { it }
        ) {
            val scope = rememberCoroutineScope()
            val density = LocalDensity.current
            val dismissThresholdPx = with(density) { 110.dp.toPx() }
            val dragOffset = remember { Animatable(0f) }
            val sheetColor = colorResource(R.color.zen_circle_sheet_bg)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationY = dragOffset.value }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            // Downward travel only.
                            scope.launch { dragOffset.snapTo((dragOffset.value + delta).coerceAtLeast(0f)) }
                        },
                        onDragStopped = { velocity ->
                            if (dragOffset.value > dismissThresholdPx || velocity > 1_800f) {
                                onDismiss()
                            } else {
                                dragOffset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium))
                            }
                        }
                    )
                    .clip(RoundedCornerShape(topStart = ZenSheetRadius, topEnd = ZenSheetRadius))
                    .background(if (SupportsBlur) sheetColor else sheetColor.copy(alpha = 0.97f))
                    // Swallow taps so they don't fall through to the scrim and dismiss.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding()
                    .semantics { this.paneTitle = paneTitle }
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SheetContent(
    userCode: String?,
    onShareInviteLink: () -> Unit,
    onCopyInviteCode: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1_800)
            copied = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(39.75.rdp))
        Text(
            text = "ZEN CIRCLE SETTINGS",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 14.15.rsp,
            lineHeight = 17.rsp,
            letterSpacing = (-0.42).sp,
            color = ZenTheme.colors.textPrimary,
            modifier = Modifier.padding(start = 33.36.rdp)
        )

        Spacer(Modifier.height(9.rdp))
        Column(modifier = Modifier.padding(start = 32.35.rdp, end = 32.35.rdp)) {
            Text(
                text = "Invite your people!",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 20.22.rsp,
                lineHeight = 24.3.rsp,
                color = ZenTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(12.13.rdp))
            Text(
                text = "A Wholesome Zen Journey awaits!",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.18.rsp,
                lineHeight = 19.4.rsp,
                letterSpacing = (-0.16).sp,
                color = colorResource(R.color.zen_700)
            )
        }

        Spacer(Modifier.height(34.rdp))
        Column(modifier = Modifier.padding(start = 31.11.rdp, end = 40.rdp)) {
            InviteOption(
                iconRes = R.drawable.ic_zen_circle_share,
                iconSize = 26.42.rdp,
                title = "Share invite link",
                subtitle = "Works even without having ZenMode OS installed",
                enabled = userCode != null,
                entranceDelay = 140,
                onClick = onShareInviteLink
            )
            Spacer(Modifier.height(34.57.rdp))
            InviteOption(
                iconRes = R.drawable.ic_zen_circle_copy,
                iconSize = 26.79.rdp,
                title = "Copy Invite code",
                subtitle = if (copied) "Copied. Paste it to your people" else userCode ?: "Sign in to get your code",
                subtitleHighlighted = copied,
                enabled = userCode != null,
                entranceDelay = 210,
                onClick = {
                    onCopyInviteCode()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    ZenSound.play(Sfx.TOGGLE_ON)
                    copied = true
                }
            )
        }

        Spacer(Modifier.height(28.rdp))
        Box(
            modifier = Modifier
                .padding(horizontal = 38.5.rdp)
                .fillMaxWidth()
                .height(0.86.dp)
                .background(colorResource(R.color.zen_circle_divider))
        )

        Spacer(Modifier.height(16.4.rdp))
        Text(
            text = "No spam, no auto-follows. They'll just see your invite and decide.",
            fontFamily = Geist,
            fontSize = 13.83.rsp,
            lineHeight = 19.36.rsp,
            letterSpacing = (-0.14).sp,
            color = colorResource(R.color.zen_circle_sheet_muted),
            modifier = Modifier
                .padding(start = 47.15.rdp)
                .width(287.76.rdp)
        )
    }
}

@Composable
private fun InviteOption(
    iconRes: Int,
    iconSize: Dp,
    title: String,
    subtitle: String,
    enabled: Boolean,
    entranceDelay: Int,
    onClick: () -> Unit,
    subtitleHighlighted: Boolean = false
) {
    val inspection = LocalInspectionMode.current
    val entrance = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(entranceDelay.toLong())
        entrance.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
    }
    val risePx = with(LocalDensity.current) { 14.dp.toPx() }
    val muted = colorResource(R.color.zen_circle_sheet_muted)
    val highlight = colorResource(R.color.zen_700)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entrance.value.coerceIn(0f, 1f)
                translationY = (1f - entrance.value) * risePx
            }
            .clip(RoundedCornerShape(12.rdp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Both icons share one 26.8dp slot so the two titles start on the same line.
        Box(modifier = Modifier.size(26.79.rdp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
        }
        Spacer(Modifier.width(10.37.rdp))
        Column {
            Text(
                text = title,
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 16.18.rsp,
                lineHeight = 19.4.rsp,
                letterSpacing = (-0.16).sp,
                color = colorResource(R.color.zen_circle_sheet_option)
            )
            Spacer(Modifier.height(5.19.rdp))
            AnimatedContent(
                targetState = subtitle to subtitleHighlighted,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                label = "inviteOptionSubtitle"
            ) { (text, highlighted) ->
                BrandedText(
                    text = text,
                    style = TextStyle(
                        fontFamily = Geist,
                        fontSize = 12.1.rsp,
                        lineHeight = 14.5.rsp,
                        letterSpacing = (-0.12).sp,
                        color = if (highlighted) highlight else muted
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BoxScope.CloseButton(onDismiss: () -> Unit) {
    val inspection = LocalInspectionMode.current
    var landed by remember { mutableStateOf(inspection) }
    LaunchedEffect(Unit) {
        delay(120)
        landed = true
    }
    // "+" turns into "×" as the sheet settles (Figma shows it at 45°).
    val rotation by animateFloatAsState(
        targetValue = if (landed) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "inviteSheetClose"
    )

    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            // Figma: 28.9dp box at x=361.9, y=31.1 in a 416.5dp sheet; 48dp hit area around it.
            .offset(x = -(25.6.rdp - (48.dp - 28.94.rdp) / 2), y = 31.11.rdp - (48.dp - 28.94.rdp) / 2)
            .requiredSize(48.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = "Close", onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_zen_circle_close),
            contentDescription = "Close",
            modifier = Modifier
                .size(20.46.rdp)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}
