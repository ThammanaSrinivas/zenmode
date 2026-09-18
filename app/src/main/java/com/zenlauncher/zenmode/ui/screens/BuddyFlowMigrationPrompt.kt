package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * One-time prompt for a user who already had a Zen Buddy relationship before Zen Circle
 * shipped. Shown at most once per install - the choice is cached and this never reappears.
 * "Keep Zen Buddy" is not a dead end: the classic summary screen always carries its own
 * "Switch to Zen Circle" action for later.
 */
@Composable
fun BuddyFlowMigrationPrompt(
    onTryZenCircle: () -> Unit,
    onKeepZenBuddy: () -> Unit
) {
    val colors = ZenTheme.colors
    AlertDialog(
        onDismissRequest = { /* forces an explicit choice */ },
        containerColor = colors.bgSecondary,
        title = {
            Text(
                text = "We rebuilt Zen Buddy into Zen Circle",
                fontFamily = Geist,
                fontWeight = FontWeight.Bold,
                fontSize = 20.rsp,
                color = colors.textPrimary
            )
        },
        text = {
            Text(
                text = "Same buddy, a fresh dashboard. You can always switch later from your " +
                    "Zen Buddy summary.",
                fontFamily = Geist,
                fontWeight = FontWeight.Normal,
                fontSize = 14.rsp,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            Text(
                text = "Try Zen Circle",
                fontFamily = Geist,
                fontWeight = FontWeight.Bold,
                fontSize = 16.rsp,
                color = colors.textBrand,
                modifier = Modifier
                    .clickable { onTryZenCircle() }
                    .padding(8.rdp)
            )
        },
        dismissButton = {
            Text(
                text = "Keep Zen Buddy",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 16.rsp,
                color = colors.textPrimary,
                modifier = Modifier
                    .clickable { onKeepZenBuddy() }
                    .padding(8.rdp)
            )
        }
    )
}
