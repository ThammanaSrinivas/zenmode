package com.zenlauncher.zenmode.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.ContentBlockPrefs
import com.zenlauncher.zenmode.ZenAccessibilityService
import com.zenlauncher.zenmode.accessibility.ContentBlockRules
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * Per-surface toggles for in-app content blocking. Reads the surface list from
 * [ContentBlockRules.default] so new apps/surfaces show up automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentBlockingBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = ZenTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rules = remember { ContentBlockRules.default() }

    val accessibilityOn = remember { ZenAccessibilityService.isEnabledInSettings(context) }

    // Toggle state, seeded from prefs and written through on change.
    val blocked = remember {
        mutableStateMapOf<String, Boolean>().apply {
            rules.apps.values.forEach { app ->
                app.surfaces.forEach { surface ->
                    put(
                        "${app.packageName}/${surface.id}",
                        ContentBlockPrefs.isSurfaceBlocked(context, app.packageName, surface.id)
                    )
                }
            }
        }
    }
    var debugDump by remember { mutableStateOf(ContentBlockPrefs.isDebugDumpEnabled(context)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgSecondary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.rdp, bottom = 8.rdp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textSecondary)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.rdp)
                .padding(bottom = 32.rdp)
        ) {
            Text(
                text = "Block in-app content",
                color = colors.textPrimary,
                style = TextStyle(
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.rsp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Block the endless-scroll parts of an app while keeping the rest usable.",
                color = colors.textSecondary,
                style = TextStyle(
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.rsp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!accessibilityOn) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.bgPrimary)
                        .clickable {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Accessibility permission needed",
                        color = colors.textPrimary,
                        fontFamily = CabinetGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.rsp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to open Accessibility settings and turn on ZenMode. Blocking stays off until you do.",
                        color = colors.textSecondary,
                        fontFamily = CabinetGrotesque,
                        fontSize = 13.rsp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            rules.apps.values.forEach { app ->
                val appLabel = appLabelFor(app.packageName)
                app.surfaces.forEach { surface ->
                    val stateKey = "${app.packageName}/${surface.id}"
                    ToggleRow(
                        text = "$appLabel — ${surface.label}",
                        checked = blocked[stateKey] == true,
                        enabled = true,
                        trackColor = colors.textBrand,
                        offColor = colors.textSecondary,
                        onCheckedChange = { on ->
                            blocked[stateKey] = on
                            ContentBlockPrefs.setSurfaceBlocked(context, app.packageName, surface.id, on)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ToggleRow(
                text = "Log view IDs (debugging)",
                checked = debugDump,
                enabled = true,
                trackColor = colors.textBrand,
                offColor = colors.textSecondary,
                onCheckedChange = { on ->
                    debugDump = on
                    ContentBlockPrefs.setDebugDumpEnabled(context, on)
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Writes the on-screen view IDs to logcat (tag ZenA11y) so blocking rules can be tuned to your app version.",
                color = colors.textSecondary,
                fontFamily = CabinetGrotesque,
                fontSize = 12.rsp
            )

            val lastCrash = remember { ZenAccessibilityService.lastCrash(context) }
            if (lastCrash != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Blocker last crashed: $lastCrash\nTap to clear.",
                    color = colors.textSecondary,
                    fontFamily = CabinetGrotesque,
                    fontSize = 12.rsp,
                    modifier = Modifier.clickable { ZenAccessibilityService.clearLastCrash(context) }
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    text: String,
    checked: Boolean,
    enabled: Boolean,
    trackColor: Color,
    offColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = ZenTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.bgPrimary)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontFamily = CabinetGrotesque,
            fontWeight = FontWeight.Medium,
            fontSize = 16.rsp,
            color = if (enabled) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = trackColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = offColor
            )
        )
    }
}

private fun appLabelFor(packageName: String): String = when (packageName) {
    "com.google.android.youtube" -> "YouTube"
    "com.instagram.android" -> "Instagram"
    "com.snapchat.android" -> "Snapchat"
    else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
}
