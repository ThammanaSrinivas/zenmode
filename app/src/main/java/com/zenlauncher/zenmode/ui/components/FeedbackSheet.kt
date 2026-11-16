package com.zenlauncher.zenmode.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.Feedback
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * Settings → "Report a bug" and "Suggest a feature". Says where it goes and opens the mail.
 *
 * The sheet owns no policy: [Feedback] decides what the mail carries and where it goes, so
 * one sent from here reads the same as one sent from anywhere else we add the entry point.
 */
@Composable
fun FeedbackSheet(kind: Feedback.Kind, onDismiss: () -> Unit) {
    val colors = ZenTheme.colors
    val context = LocalContext.current

    ZenSheet(onDismiss = onDismiss) {
        when (kind) {
            Feedback.Kind.BUG -> {
                ZenSheetTitle("Report a bug")
                ZenSheetBody(
                    "Email us at ${AppConstants.SUPPORT_EMAIL}. What happened, and the step just before " +
                        "it, is usually the useful part. Attach a screenshot in your mail app if you can."
                )
            }
            Feedback.Kind.IDEA -> {
                ZenSheetTitle("Suggest a feature")
                ZenSheetBody(
                    "Email us at ${AppConstants.SUPPORT_EMAIL}. Tell us what you'd like, and the moment " +
                        "you wished you had it. We read every one."
                )
            }
        }

        Text(
            text = "Your phone model, Android version and app build are filled in. " +
                "You can read every line before you send.",
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
                    Feedback.send(context, kind)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
