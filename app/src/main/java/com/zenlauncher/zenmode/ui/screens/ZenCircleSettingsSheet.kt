package com.zenlauncher.zenmode.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.launch

// ── Zen Circle settings sheet ─────────────────────────────────────
// Behind the ☰ on "My Zen Circle" (Figma node 2026:2939; the sheet is a flattened image there,
// so sizes are measured off it). Rows: invite, edit, manage members, remove buddy, leave.
//
// Motion: rows rise in one after another as the sheet lands. Destructive rows don't act on the
// first tap — the row opens an inline confirm (expands in place, the icon turns red) and the
// confirm button pulses while the request is in flight. Rows that aren't built yet wiggle a
// "Soon" tag and spin their icon instead of doing nothing.

/** Most people a circle can hold, you included ("up to 7 people you trust"). */
internal const val ZenCircleCapacity = com.zenlauncher.zenmode.coreapi.ZEN_CIRCLE_MAX_MEMBERS

/** What the settings sheet needs from its page. */
data class ZenCircleSettings(
    val buddyName: String,
    val memberCount: Int,
    /** True while a remove/leave request is in flight. */
    val removing: Boolean,
    val onRemoveBuddy: () -> Unit,
    val onLeaveCircle: () -> Unit
)

private enum class PendingAction { Remove, Leave }

@Composable
internal fun ZenCircleSettingsContent(
    settings: ZenCircleSettings,
    onInvitePeople: () -> Unit
) {
    var pending by rememberSaveable { mutableStateOf<PendingAction?>(null) }
    val seatsLeft = (ZenCircleCapacity - settings.memberCount).coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Drag handle (the sheet is draggable from anywhere; this is the affordance).
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 2.5.rdp)
                .width(48.rdp)
                .height(4.rdp)
                .clip(CircleShape)
                .background(colorResource(R.color.paper_hairline))
        )

        Spacer(Modifier.height(19.5.rdp))
        Text(
            text = "ZEN CIRCLE SETTINGS",
            fontFamily = DepartureMono,
            fontSize = 11.5.rsp,
            letterSpacing = 1.2.sp,
            color = colorResource(R.color.zen_500),
            modifier = Modifier.padding(start = 13.7.rdp)
        )
        Spacer(Modifier.height(14.rdp))
        Text(
            text = "Circle",
            fontFamily = ClashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 24.rsp,
            color = colorResource(R.color.ink_base),
            modifier = Modifier
                .padding(start = 13.7.rdp)
                .semantics { heading() }
        )

        Spacer(Modifier.height(29.rdp))
        SettingsRow(
            index = 0,
            icon = Icons.Outlined.AddCircleOutline,
            title = "Invite people",
            subtitle = if (seatsLeft == 1) "1 seat left" else "$seatsLeft seats left",
            enabled = seatsLeft > 0,
            onClick = onInvitePeople
        )
        SettingsRow(
            index = 1,
            icon = Icons.Outlined.LightMode,
            title = "Edit Circle",
            subtitle = "Name and icon",
            comingSoon = true
        )
        SettingsRow(
            index = 2,
            icon = Icons.Outlined.AccessibilityNew,
            title = "Manage members",
            subtitle = "Remove, or hand over ownership",
            comingSoon = true
        )
        SettingsRow(
            index = 3,
            icon = Icons.Outlined.PersonRemove,
            title = "Remove buddy",
            subtitle = "End your Zen Bro connection with ${settings.buddyName}",
            destructive = true,
            confirming = pending == PendingAction.Remove,
            onClick = { pending = if (pending == PendingAction.Remove) null else PendingAction.Remove }
        ) {
            ConfirmStrip(
                message = "${settings.buddyName} won’t see your stats anymore, and you won’t see theirs.",
                confirmLabel = "Remove",
                busyLabel = "Removing…",
                busy = settings.removing,
                onCancel = { pending = null },
                onConfirm = settings.onRemoveBuddy
            )
        }
        SettingsRow(
            index = 4,
            icon = Icons.Outlined.Close,
            title = "Leave Circle",
            subtitle = "Your own history stays yours",
            destructive = true,
            confirming = pending == PendingAction.Leave,
            onClick = { pending = if (pending == PendingAction.Leave) null else PendingAction.Leave }
        ) {
            ConfirmStrip(
                message = "You’ll leave this circle. Your screen time history stays with you.",
                confirmLabel = "Leave",
                busyLabel = "Leaving…",
                busy = settings.removing,
                onCancel = { pending = null },
                onConfirm = settings.onLeaveCircle
            )
        }

        // Figma: ~30dp from the last subtitle to the rule; the row's own 20dp padding covers most of it.
        Spacer(Modifier.height(9.7.rdp))
        Box(
            modifier = Modifier
                .padding(horizontal = 13.7.rdp)
                .fillMaxWidth()
                .height(1.dp)
                .background(colorResource(R.color.paper_hairline))
        )
        Spacer(Modifier.height(16.4.rdp))
        Text(
            text = "You started it, so you can hand it over.",
            fontFamily = Geist,
            fontSize = 14.5.rsp,
            letterSpacing = (-0.1).sp,
            color = colorResource(R.color.zen_circle_sheet_muted),
            modifier = Modifier.padding(start = 13.7.rdp, end = 13.7.rdp)
        )
        Spacer(Modifier.height(15.rdp))
    }
}

@Composable
private fun SettingsRow(
    index: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    comingSoon: Boolean = false,
    destructive: Boolean = false,
    confirming: Boolean = false,
    onClick: () -> Unit = {},
    confirmContent: (@Composable () -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val spin = remember { Animatable(0f) }
    val wiggle = remember { Animatable(0f) }
    val green = colorResource(R.color.zen_700)
    val danger = colorResource(R.color.ember_700)
    val iconTint = if (destructive && confirming) danger else green

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .riseIn(delayMillis = 90 + index * 55, rise = 22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.rdp))
                .clickable(enabled = enabled, onClickLabel = title) {
                    if (comingSoon) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch { spin.animateTo(spin.value + 90f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
                        scope.launch { for (a in listOf(-12f, 10f, -6f, 3f, 0f)) wiggle.animateTo(a, tween(55)) }
                    } else {
                        if (destructive) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                }
                .semantics { if (comingSoon) stateDescription = "Coming soon" }
                .padding(top = 20.rdp, bottom = 20.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon centred 42dp in, titles at 68.7dp (measured off the Figma raster).
            Box(modifier = Modifier.width(68.7.rdp), contentAlignment = Alignment.CenterStart) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(start = 31.rdp)
                        .size(22.rdp)
                        .graphicsLayer { rotationZ = spin.value }
                )
            }
            Column(modifier = Modifier.weight(1f).padding(end = 13.7.rdp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontFamily = Geist,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.rsp,
                        letterSpacing = (-0.2).sp,
                        color = if (destructive && confirming) danger else colorResource(R.color.ink_base)
                    )
                    if (comingSoon) {
                        Spacer(Modifier.width(7.rdp))
                        Text(
                            text = "SOON",
                            fontFamily = DepartureMono,
                            fontSize = 9.rsp,
                            letterSpacing = 0.6.sp,
                            color = green,
                            modifier = Modifier
                                .graphicsLayer { rotationZ = wiggle.value }
                                .clip(CircleShape)
                                .border(0.8.dp, green.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 6.rdp, vertical = 1.rdp)
                        )
                    }
                }
                Spacer(Modifier.height(4.rdp))
                Text(
                    text = subtitle,
                    fontFamily = Geist,
                    fontSize = 15.rsp,
                    letterSpacing = (-0.1).sp,
                    color = colorResource(R.color.zen_circle_wheel_name)
                )
            }
        }

        if (confirmContent != null) {
            AnimatedVisibility(
                visible = confirming,
                enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(180, delayMillis = 60)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(120))
            ) {
                confirmContent()
            }
        }
    }
}

@Composable
private fun ConfirmStrip(
    message: String,
    confirmLabel: String,
    busyLabel: String,
    busy: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val danger = colorResource(R.color.ember_700)
    val pulse by rememberInfiniteTransition(label = "confirmBusy").animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "confirmBusyAlpha"
    )

    Column(
        modifier = Modifier
            .padding(start = 68.7.rdp, end = 13.7.rdp, bottom = 14.rdp)
            .fillMaxWidth()
    ) {
        Text(
            text = message,
            fontFamily = Geist,
            fontSize = 13.5.rsp,
            lineHeight = 18.rsp,
            color = colorResource(R.color.zen_circle_sheet_muted)
        )
        Spacer(Modifier.height(10.rdp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.rdp)) {
            Box(
                modifier = Modifier
                    .height(38.rdp)
                    .clip(CircleShape)
                    .border(1.dp, colorResource(R.color.paper_hairline), CircleShape)
                    .clickable(enabled = !busy, onClick = onCancel)
                    .padding(horizontal = 18.rdp),
                contentAlignment = Alignment.Center
            ) {
                Text("Keep", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 15.rsp, color = colorResource(R.color.ink_base))
            }
            Box(
                modifier = Modifier
                    .height(38.rdp)
                    .graphicsLayer { alpha = if (busy) pulse else 1f }
                    .clip(CircleShape)
                    .background(danger)
                    .clickable(enabled = !busy, onClick = onConfirm)
                    .padding(horizontal = 18.rdp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = busy,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                    label = "confirmLabel"
                ) { isBusy ->
                    Text(
                        text = if (isBusy) busyLabel else confirmLabel,
                        fontFamily = Geist,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.rsp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
