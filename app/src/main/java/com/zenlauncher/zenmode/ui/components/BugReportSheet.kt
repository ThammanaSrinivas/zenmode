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
import androidx.compose.ui.res.stringResource
import com.zenlauncher.zenmode.R

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
        ZenSheetTitle(stringResource(R.string.settings_report_bug_title))
        ZenSheetBody(
            stringResource(R.string.bug_report_email_body, AppConstants.SUPPORT_EMAIL)
        )

        Text(
            text = stringResource(R.string.bug_report_device_note),
            fontFamily = Geist,
            fontSize = 12.rsp,
            lineHeight = 17.rsp,
            color = colors.textMuted
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.rdp)) {
            ZenButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                style = ZenButtonStyle.Outline,
                modifier = Modifier.weight(1f)
            )
            ZenButton(
                text = stringResource(R.string.bug_report_email_us),
                onClick = {
                    BugReport.send(context)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
