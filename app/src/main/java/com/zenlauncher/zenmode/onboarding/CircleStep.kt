package com.zenlauncher.zenmode.onboarding

import com.zenlauncher.zenmode.ui.theme.ZenTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import com.zenlauncher.zenmode.ui.screens.BuddyAddResult
import com.zenlauncher.zenmode.ui.screens.ZenCircleConnectOptions
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * 05 · My Zen Circle — the same three ways in as the home screen (share a link, trade
 * codes, find a buddy). Entirely optional: Continue works whether or not they connect.
 */
@Composable
internal fun CircleStep(
    progress: StepProgress,
    userCode: String?,
    connectedBuddyName: String?,
    onBack: () -> Unit,
    onShareLink: () -> Unit,
    onCopyCode: () -> Unit,
    onAddBuddy: suspend (String) -> BuddyAddResult,
    onRandomConnect: () -> Unit,
    onContinue: () -> Unit
) {
    OnboardingPage(
        topBar = {
            progress.TopBar(onBack = onBack, trailing = { OnboardingChip(text = "Halfway there") })
        },
        bottomBar = {
            OnboardingButton(
                text = if (connectedBuddyName != null) "Continue with $connectedBuddyName" else "Continue",
                onClick = onContinue,
                style = if (connectedBuddyName != null) OnboardingButtonStyle.Brand else OnboardingButtonStyle.Ink
            )
            if (connectedBuddyName == null) {
                Text(
                    text = "No rush. You can build your circle anytime from home.",
                    fontFamily = Geist,
                    fontSize = 12.rsp,
                    color = ZenTheme.colors.textTertiary
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(horizontal = OnboardingMargin)) {
                Spacer(Modifier.height(20.rdp))
                OnboardingEyebrow("My Zen Circle", modifier = Modifier.staggeredEntrance(0))
                Spacer(Modifier.height(10.rdp))
                OnboardingHeadline("Better with your people.", modifier = Modifier.staggeredEntrance(1))
                Spacer(Modifier.height(12.rdp))
                OnboardingBody(
                    "Pick one Zen Bro to keep you honest, or share your link with the whole gang.",
                    modifier = Modifier.staggeredEntrance(2)
                )
            }
            Spacer(Modifier.height(22.rdp))
            ZenCircleConnectOptions(
                userCode = userCode,
                onShareLink = onShareLink,
                onCopyCode = onCopyCode,
                onAddBuddy = onAddBuddy,
                onRandomConnect = onRandomConnect,
                modifier = Modifier
                    .padding(horizontal = 18.rdp)
                    .staggeredEntrance(3)
            )
            Spacer(Modifier.height(16.rdp))
        }
    }
}
