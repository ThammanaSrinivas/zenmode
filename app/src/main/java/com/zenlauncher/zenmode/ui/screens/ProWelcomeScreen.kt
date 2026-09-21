package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.ui.components.ForestGlass
import com.zenlauncher.zenmode.ui.components.ForestGlassLine
import com.zenlauncher.zenmode.ui.components.ForestScene
import com.zenlauncher.zenmode.ui.components.ZenButton
import com.zenlauncher.zenmode.ui.components.ZenModeOsWordmark
import com.zenlauncher.zenmode.ui.components.ZenProTag
import com.zenlauncher.zenmode.ui.components.rememberZenFeedback
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.Spacing
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.delay

// ── "You're Pro" ──────────────────────────────────────────────────
//
// The one flourish in the sales path, and it comes *after* the money. Dawn breaks over a
// forest (`ForestScene`), the Pro chord lands on the first frame and the wood wakes up a
// beat later, then the words rise out of the scene in order. Two taps' worth of attention,
// then back to where they came from — nothing here asks for anything.
//
// The host forces the dark theme while this is up, so every token on this page is already
// the ink one and reads light against the trees.

/** What Pro actually opens today. Features still arriving belong in the changelog, not here. */
private val ProOpens = listOf(
    "All-time history, downloadable",
    "Weekly Zen Score & report history",
    "50 random connects a week, first priority",
    "Data export + Telegram community"
)

@Composable
fun ProWelcome(
    onContinue: () -> Unit,
    /** Early access with nothing charged: the thanks can't claim they're paying for servers. */
    isSimulated: Boolean = false
) {
    val colors = ZenTheme.colors
    val feedback = rememberZenFeedback()
    val inspection = LocalInspectionMode.current

    // The chord is the confirmation of the purchase; the forest is the place it lands in.
    // A beat between them so they read as two events, not one thick noise.
    LaunchedEffect(Unit) {
        if (inspection) return@LaunchedEffect
        feedback.proUnlocked()
        delay(280)
        feedback.forest()
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.bgPrimary)) {
        ForestScene(Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screenMargin),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.7f))
            ZenModeOsWordmark(
                fontSize = 15.rsp,
                zenModeColor = colors.textPrimary.copy(alpha = 0.7f),
                modifier = Modifier.staggeredEntrance(6, stepMillis = 130)
            )
            Spacer(Modifier.height(14.rdp))
            ZenProTag(unlocked = true, modifier = Modifier.staggeredEntrance(7, stepMillis = 130))
            Spacer(Modifier.height(16.rdp))
            Text(
                text = "You're Pro.\nThank you.",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 36.rsp,
                lineHeight = 40.rsp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .staggeredEntrance(8, stepMillis = 130)
                    .semantics { heading() }
            )
            Spacer(Modifier.height(12.rdp))
            Text(
                text = if (isSimulated) "Every Pro feature is yours during early access. Nothing was charged."
                else "You're keeping the servers on and the app ad-free. We're grateful.",
                fontFamily = Geist,
                fontSize = 15.rsp,
                lineHeight = 22.rsp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.staggeredEntrance(9, stepMillis = 130)
            )
            Spacer(Modifier.height(20.rdp))
            Box(
                modifier = Modifier
                    .width(96.rdp)
                    .staggeredEntrance(10, stepMillis = 130)
            ) { OsRule() }
            Spacer(Modifier.weight(1f))
            ProOpensCard()
            Spacer(Modifier.height(20.rdp))
            ZenButton(
                text = "Back to ZenMode",
                onClick = onContinue,
                modifier = Modifier
                    .padding(bottom = 16.rdp)
                    .staggeredEntrance(16, stepMillis = 100)
            )
        }
    }
}

/**
 * Glass, not paper: a card in `bgSecondary` here would punch a flat hole in the forest, so
 * this one is smoked glass with a hairline, and the dawn behind it still shows through.
 */
@Composable
private fun ProOpensCard() {
    val colors = ZenTheme.colors
    val shape = RoundedCornerShape(20.rdp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEntrance(11, stepMillis = 100)
            .clip(shape)
            .background(ForestGlass.copy(alpha = 0.55f))
            .border(1.dp, ForestGlassLine.copy(alpha = 0.18f), shape)
            .padding(18.rdp),
        verticalArrangement = Arrangement.spacedBy(12.rdp)
    ) {
        ProOpens.forEachIndexed { i, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.staggeredEntrance(12 + i, stepMillis = 100)
            ) {
                Box(
                    Modifier.size(18.rdp).clip(CircleShape).background(colors.surfaceTint),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", fontSize = 10.rsp, color = colors.textOnTint)
                }
                Spacer(Modifier.width(10.rdp))
                Text(item, fontFamily = Geist, fontSize = 14.rsp, color = colors.textPrimary)
            }
        }
    }
}
