package com.zenlauncher.zenmode.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.BuildConfig
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ZenAccessibilityService
import com.zenlauncher.zenmode.coreapi.UsageAccess
import com.zenlauncher.zenmode.ui.components.RowTrailing
import com.zenlauncher.zenmode.ui.components.ZenRowDivider
import com.zenlauncher.zenmode.ui.components.ZenSettingsGroup
import com.zenlauncher.zenmode.ui.components.ZenSettingsRow
import com.zenlauncher.zenmode.ui.components.rememberBrandOsGradient
import com.zenlauncher.zenmode.ui.components.rememberReduceMotion
import com.zenlauncher.zenmode.ui.components.rememberResumeCount
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.ZenTypography
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlin.math.PI
import kotlin.math.sin

// ── Settings → Phone ──────────────────────────────────────────────
// The Android settings ZenMode depends on, each with its live state (re-read every time
// Settings comes back into view, since they're all changed outside the app), plus a way
// into the rest of the phone's settings.

private data class PhoneState(
    val isDefaultHome: Boolean,
    val usageAccess: Boolean,
    val accessibility: Boolean,
    val batteryUnrestricted: Boolean
)

/** Each read is guarded: a missing or misbehaving system service reads as "not set", never a crash. */
private fun readPhoneState(context: Context): PhoneState {
    val home = runCatching {
        context.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName
    }.getOrNull()
    return PhoneState(
        isDefaultHome = home == context.packageName,
        usageAccess = runCatching { UsageAccess.isGranted(context) }.getOrDefault(false),
        accessibility = runCatching { ZenAccessibilityService.isEnabledInSettings(context) }.getOrDefault(false),
        batteryUnrestricted = runCatching {
            context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true
        }.getOrDefault(false)
    )
}

/** Opens a system settings screen, falling back to the main Settings app if this phone lacks it. */
private fun openSystemSetting(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

@Composable
internal fun PhoneSettingsGroup() {
    val context = LocalContext.current
    val resumes = rememberResumeCount()
    val state = remember(resumes) { readPhoneState(context) }
    val pkgUri = Uri.parse("package:${context.packageName}")

    ZenSettingsGroup(label = "Phone") {
        ZenSettingsRow(
            title = "Default home app",
            subtitle = "ZenMode only works as your home screen.",
            value = if (state.isDefaultHome) "ZenMode" else "Not set",
            trailing = RowTrailing.External,
            onClick = { openSystemSetting(context, Intent(Settings.ACTION_HOME_SETTINGS)) }
        )
        ZenRowDivider()
        ZenSettingsRow(
            title = "Usage access",
            subtitle = "Lets ZenMode read your screen time. It never leaves the phone.",
            value = if (state.usageAccess) "Allowed" else "Needed",
            trailing = RowTrailing.External,
            onClick = { openSystemSetting(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        )
        ZenRowDivider()
        ZenSettingsRow(
            title = "Accessibility",
            subtitle = "Powers the Distraction Blocker.",
            value = if (state.accessibility) "On" else "Off",
            trailing = RowTrailing.External,
            onClick = { openSystemSetting(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        )
        ZenRowDivider()
        ZenSettingsRow(
            title = "Battery",
            subtitle = "Unrestricted keeps streaks and reports counting in the background.",
            value = if (state.batteryUnrestricted) "Unrestricted" else "Optimised",
            trailing = RowTrailing.External,
            onClick = {
                openSystemSetting(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri))
            }
        )
        ZenRowDivider()
        ZenSettingsRow(
            title = "Do Not Disturb",
            subtitle = "Silence the phone itself while you focus.",
            trailing = RowTrailing.External,
            onClick = { openSystemSetting(context, Intent("android.settings.ZEN_MODE_SETTINGS")) }
        )
        ZenRowDivider()
        ZenSettingsRow(
            title = "ZenMode notifications",
            subtitle = "Weekly reports and your Zen Bro's nudges.",
            trailing = RowTrailing.External,
            onClick = {
                openSystemSetting(
                    context,
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        )
        ZenRowDivider()
        ZenSettingsRow(
            title = "All phone settings",
            trailing = RowTrailing.External,
            onClick = { openSystemSetting(context, Intent(Settings.ACTION_SETTINGS)) }
        )
    }
}

// ── Footer ────────────────────────────────────────────────────────
// "Quiet the noise, Together." over a line that starts as jittery noise on the left and
// settles into one calm wave, where two circles — you and your Zen Bro — sit side by side.

@Composable
internal fun SettingsFooter() {
    val colors = ZenTheme.colors
    val gradient = rememberBrandOsGradient()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.rdp, bottom = 8.rdp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.rdp)
    ) {
        QuietTogetherGraphic(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.rdp)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.semantics(mergeDescendants = true) {}
        ) {
            Text(
                text = "Quiet the noise,",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 26.rsp,
                letterSpacing = (-0.78).sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Together.",
                style = TextStyle(
                    brush = gradient,
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 26.rsp,
                    letterSpacing = (-0.78).sp,
                    textAlign = TextAlign.Center
                )
            )
        }
        Text(
            text = "You could be anywhere, and you chose a calmer phone. Thanks for being here.",
            fontFamily = Geist,
            fontSize = 13.rsp,
            lineHeight = 18.rsp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.rdp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.rdp)) {
            Text(
                text = "#ZenMode #IMZ #InMyZone",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.rsp,
                color = colors.textBrand
            )
        }
        Text(
            text = "ZENMODE OS ${BuildConfig.VERSION_NAME} · © 2026 · OPEN SOURCE",
            style = ZenTypography.monoLabel,
            color = colors.textMuted
        )
    }
}

@Composable
private fun QuietTogetherGraphic(modifier: Modifier = Modifier) {
    val noise = colorResource(R.color.stone_300)
    val calm = colorResource(R.color.zen_700)
    val you = colorResource(R.color.zen_700)
    val bro = colorResource(R.color.score_orange)
    val still = rememberReduceMotion() || LocalInspectionMode.current
    val flow by rememberInfiniteTransition(label = "quiet").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "quiet-flow"
    )
    val phase = if (still) 0f else flow
    // Fixed pseudo-random jitter so the "noise" end looks busy but doesn't flicker.
    val jitter = remember { FloatArray(64) { i -> sin(i * 12.9898f).let { (it * 43758.547f) % 1f } } }

    Canvas(modifier = modifier.clearAndSetSemantics { contentDescription = "Noise settling into a calm line, two friends together" }) {
        val w = size.width
        val mid = size.height * 0.62f
        val amp = size.height * 0.32f
        val path = Path()
        val steps = 160
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val x = w * t
            // Noise dies away over the first 60%; a slow wave carries on and flattens out.
            val calmness = (t / 0.6f).coerceIn(0f, 1f)
            val jag = jitter[(i + (phase * 3).toInt()) % jitter.size] * (1f - calmness)
            val wave = sin(t * 10f - phase) * (1f - calmness * 0.85f) * 0.35f
            val y = mid - amp * (jag + wave) * (1f - t * 0.6f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(0f to noise, 0.55f to calm, 1f to calm),
            style = Stroke(width = 2.2f * density, cap = StrokeCap.Round)
        )
        // You and your Zen Bro, resting on the calm end of the line.
        val r = size.height * 0.16f
        val cy = mid - r - 2f * density
        drawCircle(color = you, radius = r, center = Offset(w * 0.82f, cy))
        drawCircle(color = bro.copy(alpha = 0.9f), radius = r, center = Offset(w * 0.82f + r * 1.45f, cy))
        drawCircle(color = Color.White.copy(alpha = 0.35f), radius = r * 0.35f, center = Offset(w * 0.82f - r * 0.3f, cy - r * 0.3f))
    }
}
