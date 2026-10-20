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
import com.zenlauncher.zenmode.R
import androidx.compose.ui.res.stringResource

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
                text = stringResource(R.string.buddy_flow_title),
                fontFamily = Geist,
                fontWeight = FontWeight.Bold,
                fontSize = 20.rsp,
                color = colors.textPrimary
            )
        },
        text = {
            Text(
                text = stringResource(R.string.buddy_flow_message_part_one) +
                    stringResource(R.string.buddy_flow_message_part_two),
                fontFamily = Geist,
                fontWeight = FontWeight.Normal,
                fontSize = 14.rsp,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            Text(
                text = stringResource(R.string.buddy_flow_try_circle_button),
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
                text = stringResource(R.string.buddy_flow_keep_buddy_button),
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
