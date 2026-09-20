package com.zenlauncher.zenmode.onboarding

import com.zenlauncher.zenmode.ui.components.rememberZenFeedback
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.isInk
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

@Immutable
data class HomeAppOption(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
    /** Package + activity: one package can have two launcher entries (see LauncherActivities). */
    val key: String = packageName
)

/**
 * 07 · Pick the 8 apps on the ZenHome, pre-filled with calm essentials, then the final
 * ask: make ZenMode the default home screen.
 */
@Composable
internal fun HomeAppsStep(
    progress: StepProgress,
    apps: List<HomeAppOption>?,
    selected: List<String>,
    isDefaultLauncher: Boolean,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onSetDefault: () -> Unit,
    onMaybeLater: () -> Unit
) {
    val limit = HomeAppSuggestions.HOME_APP_LIMIT
    val byPackage = apps.orEmpty().associateBy { it.packageName }

    OnboardingPage(
        topBar = {
            progress.TopBar(
                onBack = onBack,
                trailing = { OnboardingChip(text = "Last step") }
            )
        },
        bottomBar = {
            OnboardingButton(
                text = if (isDefaultLauncher) "Enter ZenMode" else "Set ZenMode as default",
                onClick = onSetDefault,
                enabled = selected.isNotEmpty(),
                style = OnboardingButtonStyle.Brand
            )
            if (!isDefaultLauncher) OnboardingTextButton(text = "Maybe later", onClick = onMaybeLater)
        }
    ) {
        if (apps == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ZenTheme.colors.textBrand, strokeWidth = 2.dp)
            }
            return@OnboardingPage
        }

        // Heading, preview and label stay put; only the app list scrolls under them.
        Column(modifier = Modifier.padding(horizontal = OnboardingMargin)) {
            Spacer(Modifier.height(20.rdp))
            OnboardingEyebrow("Your ZenHome")
            Spacer(Modifier.height(10.rdp))
            OnboardingHeadline("Pick your $limit.")
            Spacer(Modifier.height(12.rdp))
            OnboardingBody("These live on your home screen. Everything else is one search away.")
            Spacer(Modifier.height(20.rdp))
            DockPreview(selected = selected.mapNotNull { byPackage[it] }, limit = limit)
            Spacer(Modifier.height(22.rdp))
            OnboardingEyebrow("All apps", color = ZenTheme.colors.textTertiary)
            Spacer(Modifier.height(10.rdp))
        }
        HorizontalDivider(color = ZenTheme.colors.borderSubtle, thickness = 1.dp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = OnboardingMargin, end = OnboardingMargin, top = 14.rdp, bottom = 16.rdp),
            horizontalArrangement = Arrangement.spacedBy(8.rdp),
            verticalArrangement = Arrangement.spacedBy(14.rdp)
        ) {
            items(apps, key = { it.key }) { app ->
                AppTile(
                    app = app,
                    isSelected = app.packageName in selected,
                    isFull = selected.size >= limit,
                    onToggle = { onToggle(app.packageName) }
                )
            }
        }
    }
}

/** A mini home screen: the picks in order, empty seats dashed, a live counter. */
@Composable
private fun DockPreview(selected: List<HomeAppOption>, limit: Int) {
    val seat = Color.White.copy(alpha = 0.22f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.rdp))
            .background(colorResource(if (ZenTheme.colors.isInk) R.color.zen_950 else R.color.zen_900))
            .then(if (ZenTheme.colors.isInk) Modifier.border(1.dp, ZenTheme.colors.borderSubtle, RoundedCornerShape(24.rdp)) else Modifier)
            .padding(16.rdp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "ZenHome preview",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.rsp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${selected.size}/$limit",
                fontFamily = DepartureMono,
                fontSize = 16.rsp,
                color = colorResource(if (selected.size == limit) R.color.zen_300 else R.color.amber_500),
                modifier = Modifier.semantics { contentDescription = "${selected.size} of $limit apps picked" }
            )
        }
        Spacer(Modifier.height(14.rdp))
        for (row in 0 until limit / 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.rdp)) {
                for (col in 0 until 4) {
                    val app = selected.getOrNull(row * 4 + col)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                modifier = Modifier.fillMaxSize(0.82f)
                            )
                        } else {
                            Canvas(Modifier.fillMaxSize(0.82f)) {
                                drawRoundRect(
                                    color = seat,
                                    cornerRadius = CornerRadius(size.minDimension * 0.3f),
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f))
                                    )
                                )
                            }
                        }
                    }
                }
            }
            if (row < limit / 4 - 1) Spacer(Modifier.height(10.rdp))
        }
    }
}

@Composable
private fun AppTile(app: HomeAppOption, isSelected: Boolean, isFull: Boolean, onToggle: () -> Unit) {
    val enabled = isSelected || !isFull
    val ring by animateColorAsState(
        if (isSelected) ZenTheme.colors.textBrand else Color.Transparent,
        label = "ring"
    )
    val scale by animateFloatAsState(if (isSelected) 1f else 0.94f, spring(dampingRatio = 0.55f), label = "tileScale")
    val feedback = rememberZenFeedback()

    Column(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.38f)
            .pressScale(
                onClick = {
                    feedback.toggle(!isSelected)
                    onToggle()
                },
                enabled = enabled,
                pressedScale = 0.9f,
                sound = false
            )
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(60.rdp)
                    .clip(RoundedCornerShape(18.rdp))
                    .border(2.dp, ring, RoundedCornerShape(18.rdp))
                    .background(if (isSelected) ZenTheme.colors.surfaceTint else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.rdp)
                        .scale(scale)
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.rdp, y = (-4).rdp)
                        .size(20.rdp)
                        .clip(CircleShape)
                        .background(ZenTheme.colors.textBrand)
                        .border(2.dp, ZenTheme.colors.bgPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = ZenTheme.colors.textOnBrand, modifier = Modifier.size(12.rdp))
                }
            }
        }
        Spacer(Modifier.height(6.rdp))
        Text(
            text = app.label,
            fontFamily = Geist,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 12.rsp,
            color = ZenTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.rdp)
        )
    }
}
