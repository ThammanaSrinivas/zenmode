package com.zenlauncher.zenmode.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.ZEN_CIRCLE_MAX_MEMBERS
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// ── Zen Circle setup ──────────────────────────────────────────────
// Create a new circle. No Figma reference for this screen -- functional layout using
// the existing design tokens (fonts, colors, spacing units), not pixel-matched.

/**
 * @param created Non-null once creation succeeds -- (circleId, circleName) for the
 * "share this" confirmation state. circleId doubles as the shareable code (no
 * separate invite_code field, see the plan doc).
 */
@Composable
fun CircleSetupScreen(
    creating: Boolean,
    created: Pair<String, String>?,
    onBackClick: () -> Unit,
    onCreate: (name: String) -> Unit,
    onShareInviteLink: (circleId: String) -> Unit,
    onCopyCode: (circleId: String) -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.paper_base))
            .systemBarsPadding()
            .padding(horizontal = 20.rdp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(24.rdp))
            Text(
                text = "NEW CIRCLE",
                fontFamily = DepartureMono,
                fontSize = 11.5.rsp,
                letterSpacing = 1.2.sp,
                color = colorResource(R.color.zen_500)
            )
            Spacer(Modifier.height(12.rdp))
            Text(
                text = if (created == null) "Start a Zen Circle" else "Circle created",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 28.rsp,
                color = colorResource(R.color.ink_base)
            )
            Spacer(Modifier.height(32.rdp))

            if (created == null) {
                CreateCircleForm(creating = creating, onCreate = onCreate)
            } else {
                CircleCreatedConfirmation(
                    circleId = created.first,
                    circleName = created.second,
                    onShareInviteLink = onShareInviteLink,
                    onCopyCode = onCopyCode,
                    onDone = onDone
                )
            }
        }
    }
}

@Composable
private fun CreateCircleForm(creating: Boolean, onCreate: (name: String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val canCreate = name.isNotBlank() && !creating

    Text(
        text = "Circle name",
        fontFamily = Geist,
        fontSize = 14.rsp,
        color = colorResource(R.color.zen_circle_wheel_name)
    )
    Spacer(Modifier.height(8.rdp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.rdp))
            .border(1.dp, colorResource(R.color.paper_hairline), RoundedCornerShape(14.rdp))
            .padding(horizontal = 16.rdp, vertical = 14.rdp)
    ) {
        BasicTextField(
            value = name,
            onValueChange = { if (it.length <= 40) name = it },
            textStyle = TextStyle(fontFamily = Geist, fontSize = 16.rsp, color = colorResource(R.color.ink_base)),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (canCreate) onCreate(name.trim()) })
        )
    }
    Spacer(Modifier.height(24.rdp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.rdp)
            .clip(CircleShape)
            .background(if (canCreate) colorResource(R.color.zen_700) else colorResource(R.color.paper_hairline))
            .clickable(enabled = canCreate) { onCreate(name.trim()) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (creating) "Creating…" else "Create circle",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 16.rsp,
            color = if (canCreate) Color.White else colorResource(R.color.zen_circle_wheel_name)
        )
    }
}

@Composable
private fun CircleCreatedConfirmation(
    circleId: String,
    circleName: String,
    onShareInviteLink: (circleId: String) -> Unit,
    onCopyCode: (circleId: String) -> Unit,
    onDone: () -> Unit
) {
    Text(
        text = "\"$circleName\" is ready. Share this code so people can join -- up to $ZEN_CIRCLE_MAX_MEMBERS people total.",
        fontFamily = Geist,
        fontSize = 15.rsp,
        lineHeight = 20.rsp,
        color = colorResource(R.color.zen_circle_sheet_muted)
    )
    Spacer(Modifier.height(20.rdp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.rdp))
            .border(1.dp, colorResource(R.color.paper_hairline), RoundedCornerShape(14.rdp))
            .padding(16.rdp)
    ) {
        Text(text = circleId, fontFamily = DepartureMono, fontSize = 15.rsp, color = colorResource(R.color.ink_base))
    }
    Spacer(Modifier.height(16.rdp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.rdp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.rdp)
                .clip(CircleShape)
                .background(colorResource(R.color.zen_700))
                .clickable { onShareInviteLink(circleId) },
            contentAlignment = Alignment.Center
        ) {
            Text("Share link", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 15.rsp, color = Color.White)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.rdp)
                .clip(CircleShape)
                .border(1.dp, colorResource(R.color.paper_hairline), CircleShape)
                .clickable { onCopyCode(circleId) },
            contentAlignment = Alignment.Center
        ) {
            Text("Copy code", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 15.rsp, color = colorResource(R.color.ink_base))
        }
    }
    Spacer(Modifier.height(28.rdp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.rdp)
            .clip(CircleShape)
            .background(colorResource(R.color.zen_700))
            .clickable(onClick = onDone),
        contentAlignment = Alignment.Center
    ) {
        Text("Done", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 16.rsp, color = Color.White)
    }
}
