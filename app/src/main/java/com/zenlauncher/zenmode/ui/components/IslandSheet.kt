package com.zenlauncher.zenmode.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.ZenAccessibilityService
import com.zenlauncher.zenmode.ZenNotificationListenerService
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.island.IslandDevice
import com.zenlauncher.zenmode.island.IslandGeometry
import com.zenlauncher.zenmode.island.IslandHost
import com.zenlauncher.zenmode.island.IslandPrefs
import com.zenlauncher.zenmode.island.IslandSamples
import com.zenlauncher.zenmode.island.Placement
import com.zenlauncher.zenmode.island.ui.CardActions
import com.zenlauncher.zenmode.island.ui.IslandSurface
import com.zenlauncher.zenmode.ui.theme.rdp

/**
 * Settings → Phone → "Zen Island". The switch, what the island may carry, and the one
 * permission it needs to draw over other apps when the Distraction Blocker's access is off.
 */
@Composable
fun IslandSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resumes = rememberResumeCount()
    var on by remember { mutableStateOf(IslandPrefs.isOn(context)) }
    var notes by remember { mutableStateOf(IslandPrefs.notesOn(context)) }
    var live by remember { mutableStateOf(IslandPrefs.liveOn(context)) }
    val access = remember(resumes) { Access.read(context) }
    val tier = remember { IslandDevice.tier(context) }

    fun apply() = IslandHost.update(context)

    ZenSheet(onDismiss = onDismiss) {
        ZenSheetTitle("Zen Island")
        ZenSheetBody(
            "Live moments round your camera, over every app: a Quiet session counting down, music, " +
                "a call, a timer, and the days that count for your week."
        )
        IslandPreviewStrip()

        Column(Modifier.fillMaxWidth().zenCard()) {
            ZenSettingToggleItem(
                text = "Zen Island",
                checked = on,
                subtitle = when {
                    tier == IslandDevice.Tier.LITE && !IslandPrefs.isChosen(context) ->
                        "Starts off on this phone to save its memory. Turn it on any time."
                    tier == IslandDevice.Tier.LITE -> "Runs lighter on this phone: no glow, still bars."
                    else -> "On top of every app, out of the way when you watch or play."
                },
                onCheckedChange = {
                    on = it
                    IslandPrefs.setOn(context, it)
                    apply()
                    runCatching {
                        ServiceLocator.analyticsManager.trackEvent("island_toggled", mapOf("on" to it, "tier" to tier.name.lowercase()))
                    }
                }
            )
            if (on) {
                ZenRowDivider()
                ZenSettingsRow(
                    title = "Show over other apps",
                    subtitle = when {
                        access.accessibility -> "Round the camera, through the Distraction Blocker's access."
                        access.overlay -> "Just under the status bar. Turn on the Distraction Blocker to sit it round the camera."
                        else -> "Allow $PRODUCT_NAME to display over other apps."
                    },
                    value = if (access.canDraw) "On" else "Needed",
                    trailing = RowTrailing.External,
                    onClick = { openOverlaySettings(context) }
                )
                ZenRowDivider()
                ZenSettingToggleItem(
                    text = "Notifications",
                    checked = notes,
                    subtitle = "Messages pass through for a moment. Kept out during a Quiet session.",
                    onCheckedChange = {
                        notes = it
                        IslandPrefs.setNotesOn(context, it)
                    }
                )
                ZenRowDivider()
                ZenSettingToggleItem(
                    text = "Music, calls and timers",
                    checked = live,
                    subtitle = "Stay in the island while they run.",
                    onCheckedChange = {
                        live = it
                        IslandPrefs.setLiveOn(context, it)
                        apply()
                    }
                )
                if ((notes || live) && !access.listener) {
                    ZenRowDivider()
                    ZenSettingsRow(
                        title = "Notification access",
                        subtitle = "Lets the island see them. Nothing it shows leaves your phone.",
                        value = "Needed",
                        trailing = RowTrailing.External,
                        onClick = { open(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    )
                }
            }
        }

        if (on && access.canDraw) {
            ZenButton(text = "Show me", onClick = {
                apply()
                IslandHost.preview()
                onDismiss()
            })
        }
    }
}

/** What the island may use on this phone, re-read each time Settings comes back. */
private data class Access(val accessibility: Boolean, val overlay: Boolean, val listener: Boolean) {
    val canDraw: Boolean get() = accessibility || overlay

    companion object {
        fun read(context: Context) = Access(
            accessibility = runCatching { ZenAccessibilityService.isEnabledInSettings(context) }.getOrDefault(false),
            overlay = runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false),
            listener = runCatching { ZenNotificationListenerService.isEnabledInSettings(context) }.getOrDefault(false)
        )
    }
}

/** The island, drawn here as it will look: Zeno and the week, in a strip of the screen's top. */
@Composable
private fun IslandPreviewStrip() {
    val density = LocalDensity.current
    val width = with(density) { 320.dp.toPx() }
    val geometry = remember(density) {
        IslandGeometry.of(width, with(density) { 30.dp.toPx() }, emptyList(), density.density, Placement.CAMERA)
    }
    Box(Modifier.fillMaxWidth().height(44.rdp).zenCard()) {
        IslandSurface(
            scene = IslandSamples.preview,
            geometry = geometry,
            lite = true,
            actions = NoActions,
            onTap = {},
            onSwipeUp = {},
            onWindow = { _, _, _ -> },
            nowOverride = 0L
        )
    }
}

private object NoActions : CardActions {
    override fun open(item: Any) = Unit
    override fun send(action: com.zenlauncher.zenmode.island.IslandAction) = Unit
    override fun endQuiet() = Unit
    override fun pet() = Unit
    override fun mediaToggle() = Unit
    override fun mediaNext() = Unit
    override fun mediaPrevious() = Unit
    override fun touched() = Unit
}

private fun openOverlaySettings(context: Context) =
    open(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", context.packageName, null)))

private fun open(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
