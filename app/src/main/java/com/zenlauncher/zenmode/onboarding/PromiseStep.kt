package com.zenlauncher.zenmode.onboarding

import com.zenlauncher.zenmode.ui.theme.ZenTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import com.zenlauncher.zenmode.ui.screens.PromiseCard
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * 04 · How Zen Gold works, then the promise itself — the same stepper card as
 * Zen Gold → "Edit my promise", pre-filled so Continue is always one tap away.
 */
@Composable
internal fun PromiseStep(
    progress: StepProgress,
    dailyHours: Int,
    onDailyHoursChange: (Int) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    OnboardingPage(
        topBar = { progress.TopBar(onBack = onBack) },
        bottomBar = {
            OnboardingButton(
                text = "Lock in $dailyHours ${if (dailyHours == 1) "hour" else "hours"} a day",
                onClick = onContinue,
                style = OnboardingButtonStyle.Brand
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OnboardingMargin)
        ) {
            Spacer(Modifier.height(20.rdp))
            OnboardingEyebrow("Zen Gold", color = ZenTheme.colors.accentReward, modifier = Modifier.staggeredEntrance(0))
            Spacer(Modifier.height(10.rdp))
            OnboardingHeadline("Make a promise to yourself.", modifier = Modifier.staggeredEntrance(1))
            Spacer(Modifier.height(12.rdp))
            OnboardingBody(
                "Pick a daily screen-time limit you can actually keep. Start gentle. You can tighten it later.",
                modifier = Modifier.staggeredEntrance(2)
            )

            Spacer(Modifier.height(22.rdp))
            HowGoldWorks(modifier = Modifier.staggeredEntrance(3))

            Spacer(Modifier.height(18.rdp))
            Box(modifier = Modifier.staggeredEntrance(4)) {
                PromiseCard(dailyHours = dailyHours, onDailyHoursChange = onDailyHoursChange)
            }
            Spacer(Modifier.height(16.rdp))
        }
    }
}

/** Three beats: promise → keep it → gold unlocks. */
@Composable
private fun HowGoldWorks(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.rdp)
    ) {
        GoldBeat(number = "1", title = "Promise", body = "Set your daily limit", modifier = Modifier.weight(1f))
        GoldBeat(
            number = "2",
            title = "Keep it",
            body = "${AppConstants.PROMISE_DAYS_TO_UNLOCK} of ${AppConstants.PROMISE_DAYS_PER_WEEK} days",
            modifier = Modifier.weight(1f)
        )
        GoldBeat(number = "3", title = "Earn gold", body = "Invest unlocks", highlight = true, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GoldBeat(
    number: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    val container = if (highlight) ZenTheme.colors.rewardSurface else ZenTheme.colors.surfaceElevated
    val accent = if (highlight) ZenTheme.colors.accentReward else ZenTheme.colors.textBrand
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.rdp))
            .background(container)
            .padding(12.rdp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(22.rdp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number, fontFamily = DepartureMono, fontSize = 12.rsp, color = ZenTheme.colors.actionPrimaryText, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(10.rdp))
        Text(
            text = title,
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.rsp,
            color = ZenTheme.colors.textPrimary,
            maxLines = 1
        )
        Text(
            text = body,
            fontFamily = Geist,
            fontSize = 12.rsp,
            lineHeight = 16.rsp,
            color = ZenTheme.colors.textSecondary
        )
    }
}
