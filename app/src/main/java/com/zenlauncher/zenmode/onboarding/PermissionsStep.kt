package com.zenlauncher.zenmode.onboarding

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.zenlauncher.zenmode.ui.components.rememberZenFeedback
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * 06 · Device permissions, as one checklist instead of four screens. Rows refresh when
 * the user comes back from Settings; Continue unlocks once the required two are on.
 */
@Composable
internal fun PermissionsStep(
    progress: StepProgress,
    permissions: List<ZenPermission>,
    granted: Set<ZenPermission>,
    onBack: () -> Unit,
    onAllow: (ZenPermission) -> Unit,
    onContinue: () -> Unit
) {
    val requiredDone = ZenPermission.hasAllRequired(granted)
    val requiredCount = permissions.count { it.required }
    val requiredGranted = permissions.count { it.required && it in granted }

    OnboardingPage(
        topBar = { progress.TopBar(onBack = onBack) },
        bottomBar = {
            OnboardingButton(
                text = if (requiredDone) "Continue" else "Allow ${requiredCount - requiredGranted} more to continue",
                onClick = onContinue,
                enabled = requiredDone,
                style = if (requiredDone) OnboardingButtonStyle.Brand else OnboardingButtonStyle.Ink
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OnboardingMargin)
        ) {
            Spacer(Modifier.height(20.rdp))
            OnboardingEyebrow("Two steps to Zone Zen", modifier = Modifier.staggeredEntrance(0))
            Spacer(Modifier.height(10.rdp))
            OnboardingHeadline("Allow what Zen needs.", modifier = Modifier.staggeredEntrance(1))
            Spacer(Modifier.height(12.rdp))
            OnboardingBody(
                "Just what powers your features, all processed on this phone. Change them anytime in Settings.",
                modifier = Modifier.staggeredEntrance(2)
            )

            Spacer(Modifier.height(20.rdp))
            Row(
                modifier = Modifier.staggeredEntrance(3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$requiredGranted/$requiredCount",
                    fontFamily = DepartureMono,
                    fontSize = 20.rsp,
                    color = (if (requiredDone) ZenTheme.colors.textBrand else ZenTheme.colors.textPrimary)
                )
                Spacer(Modifier.width(8.rdp))
                Text(
                    text = if (requiredDone) "required allowed, you're set" else "required allowed",
                    fontFamily = Geist,
                    fontSize = 14.rsp,
                    color = ZenTheme.colors.textSecondary
                )
            }

            Spacer(Modifier.height(12.rdp))
            Column(
                modifier = Modifier.staggeredEntrance(4),
                verticalArrangement = Arrangement.spacedBy(10.rdp)
            ) {
                permissions.forEach { permission ->
                    PermissionRow(
                        permission = permission,
                        granted = permission in granted,
                        onAllow = { onAllow(permission) }
                    )
                }
            }
            Spacer(Modifier.height(16.rdp))
        }
    }
}

@Composable
private fun PermissionRow(permission: ZenPermission, granted: Boolean, onAllow: () -> Unit) {
    val border = if (granted) ZenTheme.colors.surfaceTintLine else ZenTheme.colors.borderSubtle
    val feedback = rememberZenFeedback()
    // Grants happen in system Settings; the chime plays when the user comes back and it's done.
    var wasGranted by remember { mutableStateOf(granted) }
    LaunchedEffect(granted) {
        if (granted && !wasGranted) feedback.success()
        wasGranted = granted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.rdp))
            .background(if (granted) ZenTheme.colors.surfaceTint else ZenTheme.colors.surfaceElevated)
            .border(1.dp, border, RoundedCornerShape(20.rdp))
            .pressScale(onClick = onAllow, enabled = !granted, onClickLabel = "Allow ${permission.title}", pressedScale = 0.98f)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "${permission.title}, ${if (permission.required) "required" else "optional"}, " +
                    if (granted) "allowed" else "not allowed"
            }
            .padding(14.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.rdp)
                .clip(RoundedCornerShape(12.rdp))
                .background(if (granted) ZenTheme.colors.surfaceElevated else ZenTheme.colors.bgPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = permission.icon,
                contentDescription = null,
                tint = (if (granted) ZenTheme.colors.textBrand else ZenTheme.colors.textPrimary),
                modifier = Modifier.size(22.rdp)
            )
        }
        Spacer(Modifier.width(12.rdp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (permission.required) "Required" else "Optional",
                fontFamily = DepartureMono,
                fontSize = 10.rsp,
                letterSpacing = 0.8.sp,
                color = (if (permission.required) ZenTheme.colors.accentDeduct else ZenTheme.colors.textTertiary)
            )
            Text(
                text = permission.title,
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.rsp,
                color = ZenTheme.colors.textPrimary
            )
            Spacer(Modifier.height(2.rdp))
            Text(
                text = permission.reason,
                fontFamily = Geist,
                fontSize = 13.rsp,
                lineHeight = 18.rsp,
                color = ZenTheme.colors.textSecondary
            )
        }
        Spacer(Modifier.width(10.rdp))
        AnimatedContent(
            targetState = granted,
            transitionSpec = { (fadeIn() + scaleIn(spring(dampingRatio = 0.5f))).togetherWith(fadeOut()) },
            label = "permissionState"
        ) { isGranted ->
            if (isGranted) {
                Box(
                    modifier = Modifier
                        .size(30.rdp)
                        .clip(CircleShape)
                        .background(ZenTheme.colors.textBrand),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = ZenTheme.colors.textOnBrand, modifier = Modifier.size(18.rdp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(ZenTheme.colors.actionPrimary)
                        .padding(horizontal = 14.rdp, vertical = 8.rdp)
                ) {
                    Text(
                        text = "Allow",
                        fontFamily = Geist,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.rsp,
                        color = ZenTheme.colors.actionPrimaryText
                    )
                }
            }
        }
    }
}

private val ZenPermission.icon: ImageVector
    get() = when (this) {
        ZenPermission.USAGE_ACCESS -> Icons.Rounded.Insights
        ZenPermission.ACCESSIBILITY -> Icons.Rounded.VisibilityOff
        ZenPermission.NOTIFICATIONS -> Icons.Rounded.NotificationsActive
    }
