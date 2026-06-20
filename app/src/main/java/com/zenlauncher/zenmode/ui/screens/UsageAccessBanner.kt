package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.ui.theme.ZenTheme

/**
 * Non-intrusive banner shown on the home screen only when Usage Access has been revoked
 * (common on MIUI/HyperOS, which auto-revokes it). Without it, screen time silently shows a
 * stale/zero value with no explanation. Healthy installs with the grant never see this.
 */
@Composable
fun UsageAccessBanner(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Screen time is paused",
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Usage access was turned off, so your screen time can't update. Grant it again to keep tracking.",
            color = colors.textSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.actionPrimary,
                    contentColor = colors.actionPrimaryText
                )
            ) {
                Text("Grant access")
            }
        }
    }
}
