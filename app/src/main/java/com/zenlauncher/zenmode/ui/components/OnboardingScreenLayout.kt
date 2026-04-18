package com.zenlauncher.zenmode.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp

@Composable
fun OnboardingScreenLayout(
    progress: Float,
    progressText: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLogo: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    bottomFooter: (@Composable () -> Unit)? = null,
    bgShurikenOffsetX: Dp = 0.dp,
    bgShurikenOffsetY: Dp = 0.dp,
    bgShurikenBlur: Dp = 128.6.dp,
    bgShurikenScale: Float = 1.2f,
    showBgShuriken: Boolean = false,
    secondaryButtonText: String? = null,
    onSecondaryButtonClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ZenTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
    ) {
        // Decorative corner elements

        // Top-left: Crystal
        Image(
            painter = painterResource(id = R.drawable.onboarding_crystal),
            contentDescription = null,
            modifier = Modifier
                .size(160.rdp)
                .offset(x = (-62).rdp, y = (-50).rdp)
                .align(Alignment.TopStart),
            contentScale = ContentScale.Fit
        )

        // Top-right: Shuriken
        Image(
            painter = painterResource(id = R.drawable.onboarding_shuriken),
            contentDescription = null,
            modifier = Modifier
                .size(120.rdp)
                .offset(x = 60.rdp, y = (-60).rdp)
                .blur(15.9.dp)
                .align(Alignment.TopEnd),
            contentScale = ContentScale.Fit
        )

        // Background shuriken pattern
        if (showBgShuriken) {
            Image(
                painter = painterResource(id = R.drawable.onboarding_bg_shuriken),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = bgShurikenScale; scaleY = bgShurikenScale }
                    .offset(x = bgShurikenOffsetX, y = bgShurikenOffsetY)
                    .align(Alignment.TopCenter)
                    .blur(bgShurikenBlur),
                contentScale = ContentScale.FillWidth
            )
        }

        // Top-center: App logo
        if (showLogo) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "ZenMode logo",
                modifier = Modifier
                    .size(60.rdp)
                    .offset(y = 16.rdp)
                    .align(Alignment.TopCenter),
                contentScale = ContentScale.Fit
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (showLogo) 100.rdp else 40.rdp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main dynamic content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }

            // Bottom section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.rdp, start = 20.rdp, end = 20.rdp)
            ) {
                // Progress bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.rdp)
                ) {
                    if (onBackClick != null) {
                        Image(
                            painter = painterResource(id = R.drawable.left_arrow),
                            contentDescription = "Back",
                            modifier = Modifier
                                .size(24.rdp)
                                .clickable { onBackClick() },
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(colors.bgSecondary, RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(colors.textPrimary, RoundedCornerShape(2.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = progressText,
                        color = colors.textBrand,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.height(20.rdp))

                // Primary Action Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.rdp)
                        .background(colors.textBrand, RoundedCornerShape(28.dp))
                        .clickable { onButtonClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonText,
                        color = colors.bgPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Optional secondary action (e.g. "Skip for now")
                if (secondaryButtonText != null && onSecondaryButtonClick != null) {
                    Spacer(modifier = Modifier.height(12.rdp))
                    Text(
                        text = secondaryButtonText,
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { onSecondaryButtonClick() }
                    )
                }

                // Optional bottom footer (like terms & config links)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.rdp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (bottomFooter != null) {
                        bottomFooter()
                    }
                }
            }
        }
    }
}
