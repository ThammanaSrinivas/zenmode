package com.zenlauncher.zenmode.ui.screens

import com.zenlauncher.zenmode.ui.theme.ZenTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// ── Zen Circle join ────────────────────────────────────────────────
// Paste-code fallback + deep-link landing. Also hosts the Buddy<->Circle switch
// interstitial and the "circle is full" state. No Figma reference -- functional
// layout using the existing design tokens, not pixel-matched.

/**
 * @param prefillCircleId Set when opened from a /c/{circleId} deep link -- skips straight
 * to the confirm state instead of asking the user to paste a code.
 * @param pendingBuddySwitchCircleId Non-null when the join attempt hit AlreadyInCircle and
 * the caller has a classic Buddy -- shows the switch-confirmation popup instead of erroring.
 * @param hasOpenSlots Only meaningful during the switch popup -- gates the "share this with
 * your buddy too" nudge, see the plan doc's switch-flow section.
 */
@Composable
fun CircleJoinScreen(
    prefillCircleId: String?,
    joining: Boolean,
    errorMessage: String?,
    pendingBuddySwitchCircleId: String?,
    hasOpenSlots: Boolean,
    onBackClick: () -> Unit,
    onJoin: (circleId: String) -> Unit,
    onConfirmSwitchFromBuddy: (circleId: String) -> Unit,
    onDismissSwitchPrompt: () -> Unit,
    onShareWithBuddy: () -> Unit,
    onClearError: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenTheme.colors.bgPrimary)
            .systemBarsPadding()
            .padding(horizontal = 20.rdp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(24.rdp))
            Text(
                text = "JOIN CIRCLE",
                fontFamily = DepartureMono,
                fontSize = 11.5.rsp,
                letterSpacing = 1.2.sp,
                color = colorResource(R.color.zen_500)
            )
            Spacer(Modifier.height(12.rdp))
            Text(
                text = "Join a Zen Circle",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 28.rsp,
                color = ZenTheme.colors.textPrimary
            )
            Spacer(Modifier.height(32.rdp))

            var code by rememberSaveable(prefillCircleId) { mutableStateOf(prefillCircleId ?: "") }
            val canJoin = code.isNotBlank() && !joining

            Text(
                text = "Circle code",
                fontFamily = Geist,
                fontSize = 14.rsp,
                color = colorResource(R.color.zen_circle_wheel_name)
            )
            Spacer(Modifier.height(8.rdp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.rdp))
                    .border(1.dp, ZenTheme.colors.borderSubtle, RoundedCornerShape(14.rdp))
                    .padding(horizontal = 16.rdp, vertical = 14.rdp)
            ) {
                BasicTextField(
                    value = code,
                    onValueChange = { code = it.trim() },
                    textStyle = TextStyle(fontFamily = DepartureMono, fontSize = 16.rsp, color = ZenTheme.colors.textPrimary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (canJoin) onJoin(code) })
                )
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(12.rdp))
                Text(
                    text = errorMessage,
                    fontFamily = Geist,
                    fontSize = 14.rsp,
                    color = ZenTheme.colors.accentDeduct
                )
            }

            Spacer(Modifier.height(24.rdp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.rdp)
                    .clip(CircleShape)
                    .background(if (canJoin) ZenTheme.colors.textBrand else ZenTheme.colors.borderSubtle)
                    .clickable(enabled = canJoin) { onClearError(); onJoin(code) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (joining) "Joining…" else "Join circle",
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.rsp,
                    color = if (canJoin) ZenTheme.colors.textOnBrand else colorResource(R.color.zen_circle_wheel_name)
                )
            }
        }
    }

    if (pendingBuddySwitchCircleId != null) {
        BuddySwitchConfirmDialog(
            circleId = pendingBuddySwitchCircleId,
            hasOpenSlots = hasOpenSlots,
            onConfirm = { onConfirmSwitchFromBuddy(pendingBuddySwitchCircleId) },
            onDismiss = onDismissSwitchPrompt,
            onShareWithBuddy = onShareWithBuddy
        )
    }
}

@Composable
private fun BuddySwitchConfirmDialog(
    circleId: String,
    hasOpenSlots: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onShareWithBuddy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 28.rdp)
                .clip(RoundedCornerShape(20.rdp))
                .background(ZenTheme.colors.bgPrimary)
                // Enabled (not disabled) clickable with no indication -- the standard, reliable
                // Compose idiom for "swallow this tap" so it doesn't fall through to the scrim's
                // dismiss handler behind it. A disabled clickable's tap-consumption behavior has
                // shifted across Compose versions and isn't something to rely on here.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(20.rdp)
        ) {
            Column {
                Text(
                    text = "Switch to Zen Circle?",
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.rsp,
                    color = ZenTheme.colors.textPrimary
                )
                Spacer(Modifier.height(10.rdp))
                Text(
                    text = "You have a Zen Buddy -- joining this circle will disconnect you from them.",
                    fontFamily = Geist,
                    fontSize = 14.5.rsp,
                    lineHeight = 20.rsp,
                    color = colorResource(R.color.zen_circle_sheet_muted)
                )
                if (hasOpenSlots) {
                    Spacer(Modifier.height(10.rdp))
                    Text(
                        text = "There's room in this circle -- want to share the link with your buddy too, so you both join?",
                        fontFamily = Geist,
                        fontSize = 14.5.rsp,
                        lineHeight = 20.rsp,
                        color = ZenTheme.colors.textBrand
                    )
                    Spacer(Modifier.height(8.rdp))
                    Text(
                        text = "Share with buddy",
                        fontFamily = Geist,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.rsp,
                        color = ZenTheme.colors.textBrand,
                        modifier = Modifier.clickable(onClick = onShareWithBuddy)
                    )
                }
                Spacer(Modifier.height(20.rdp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.rdp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.rdp)
                            .clip(CircleShape)
                            .border(1.dp, ZenTheme.colors.borderSubtle, CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 15.rsp, color = ZenTheme.colors.textPrimary)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.rdp)
                            .clip(CircleShape)
                            .background(ZenTheme.colors.textBrand)
                            .clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        // "(Recommended)" per the plan doc -- this is a steering moment during
                        // Buddy's ~1-month deprecation runway, not a neutral either-way choice.
                        Text("Join Circle (Recommended)", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.rsp, color = ZenTheme.colors.textOnBrand)
                    }
                }
            }
        }
    }
}
