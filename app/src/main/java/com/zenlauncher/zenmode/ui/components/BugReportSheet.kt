package com.zenlauncher.zenmode.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.BugReport
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * Settings → "Report a bug". One question, one optional screenshot, one button.
 *
 * The sheet owns no policy: [BugReport] decides what the report says and where it goes, so a
 * report sent from here reads the same as one sent from anywhere else we add the entry point.
 * [onSent] closes the sheet; [onNoMailApp] is what the host does when the phone can't send mail.
 */
@Composable
fun BugReportSheet(
    onDismiss: () -> Unit,
    onSent: () -> Unit,
    onNoMailApp: () -> Unit
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    var screenshot by remember { mutableStateOf<android.net.Uri?>(null) }
    var screenshotName by remember { mutableStateOf<String?>(null) }

    // Photo Picker where the OS has one (API 33+, and the Play-services backport below it):
    // it needs no storage permission and only ever hands back the one image the user chose.
    val pickScreenshot = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        screenshot = uri
        screenshotName = uri?.lastPathSegment
    }

    ZenSheet(onDismiss = onDismiss) {
        ZenSheetTitle("Report a bug")
        ZenSheetBody(
            "What happened? The step before it went wrong is usually the useful part."
        )

        BasicTextField(
            value = description,
            onValueChange = { description = it },
            textStyle = TextStyle(
                fontFamily = Geist,
                fontSize = 15.rsp,
                lineHeight = 22.rsp,
                color = colors.textPrimary
            ),
            cursorBrush = SolidColor(colors.textBrand),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.rdp)
                .clip(RoundedCornerShape(12.rdp))
                .background(colors.bgPrimary)
                .border(1.dp, colors.borderOutline, RoundedCornerShape(12.rdp))
                .padding(14.rdp)
                .imePadding(),
            decorationBox = { inner ->
                if (description.isEmpty()) {
                    Text(
                        text = "What happened?",
                        fontFamily = Geist,
                        fontSize = 15.rsp,
                        lineHeight = 22.rsp,
                        color = colors.textMuted
                    )
                }
                inner()
            }
        )

        // ── Attachment ──
        if (screenshot == null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.rdp))
                    .clickable(onClickLabel = "Attach a screenshot", role = Role.Button) {
                        pickScreenshot.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                    .padding(vertical = 6.rdp, horizontal = 2.rdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.AttachFile,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.rdp)
                )
                Spacer(Modifier.width(8.rdp))
                Text(
                    text = "Add a screenshot",
                    fontFamily = Geist,
                    fontSize = 14.rsp,
                    color = colors.textSecondary
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.rdp))
                    .background(colors.bgPrimary)
                    .padding(start = 12.rdp, top = 8.rdp, bottom = 8.rdp, end = 4.rdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.AttachFile,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.rdp)
                )
                Spacer(Modifier.width(8.rdp))
                Text(
                    text = screenshotName ?: "Screenshot attached",
                    fontFamily = Geist,
                    fontSize = 14.rsp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(40.rdp)
                        .clip(RoundedCornerShape(8.rdp))
                        .clickable(onClickLabel = "Remove screenshot", role = Role.Button) {
                            screenshot = null
                            screenshotName = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remove screenshot",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.rdp)
                    )
                }
            }
        }

        Text(
            text = "Your phone model, Android version and app build go with it so we can " +
                "reproduce it. Nothing is uploaded — the report opens in your mail app and " +
                "you can read every line before you send it.",
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
                text = "Send report",
                // An empty report wastes the user's trip to the mail app and ours reading it.
                enabled = description.isNotBlank(),
                onClick = {
                    if (BugReport.send(context, description, screenshot)) onSent() else onNoMailApp()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
