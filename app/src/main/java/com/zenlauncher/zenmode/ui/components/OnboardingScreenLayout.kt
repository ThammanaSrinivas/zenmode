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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.ZenTheme

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
                .size(160.dp)
                .offset(x = (-62).dp, y = (-50).dp)
                .align(Alignment.TopStart),
            contentScale = ContentScale.Fit
        )

        // Top-right: Shuriken
        Image(
            painter = painterResource(id = R.drawable.onboarding_shuriken),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .offset(x = 60.dp, y = (-60).dp)
                .blur(15.9.dp)
                .align(Alignment.TopEnd),
            contentScale = ContentScale.Fit
        )

        // Top-center: App logo (optional)
        if (showLogo) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "ZenMode logo",
                modifier = Modifier
                    .size(60.dp)
                    .offset(y = 16.dp)
                    .align(Alignment.TopCenter),
                contentScale = ContentScale.Fit
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (showLogo) 100.dp else 40.dp),
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
                    .padding(bottom = 40.dp, start = 20.dp, end = 20.dp)
            ) {
                // Progress bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    if (onBackClick != null) {
                        Image(
                            painter = painterResource(id = R.drawable.left_arrow),
                            contentDescription = "Back",
                            modifier = Modifier
                                .size(24.dp)
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

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
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

                // Optional bottom footer (like terms & config links)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
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
