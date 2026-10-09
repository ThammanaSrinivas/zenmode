package com.zenlauncher.zenmode.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.BugReport
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * Settings → "Report a bug". Says where reports go and opens the mail to send it.
 *
 * The sheet owns no policy: [BugReport] decides what the report carries and where it goes, so a
 * report sent from here reads the same as one sent from anywhere else we add the entry point.
 */
@Composable
fun BugReportSheet(onDismiss: () -> Unit) {
    val colors = ZenTheme.colors
    val context = LocalContext.current

    ZenSheet(onDismiss = onDismiss) {
        ZenSheetTitle("Report a bug")
        ZenSheetBody(
            "Email us at ${AppConstants.SUPPORT_EMAIL}. What happened, and the step just before " +
                "it, is usually the useful part. Attach a screenshot in your mail app if you can."
        )

        Text(
            text = "Your phone model, Android version and app build are filled in so we can " +
                "reproduce it. You can read every line before you send.",
            fontFamily = Geist,
            fontSize = 12.rsp,
            lineHeight = 17.rsp,
            color = colors.textMuted
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.rdp)) {
            ZenButton(
                text = "Cancel",
                onClick = onDismiss,
                style = ZenButtonStyle.Outline,
                modifier = Modifier.weight(1f)
            )
            ZenButton(
                text = "Email us",
                onClick = {
                    BugReport.send(context)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
