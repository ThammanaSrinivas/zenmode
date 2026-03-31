package com.zenlauncher.zenmode.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.ui.theme.rsp
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import kotlinx.coroutines.delay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation

enum class WelcomeState {
    PINS_6, PINS_12, PINS_24, LOGO_GLOW, BOTTOM_TEXT, FULL_PAGE
}

@Composable
fun WelcomeScreen(
    onGoogleSignInClick: () -> Unit,
    onEmailSignInClick: (email: String, password: String) -> Unit = { _, _ -> }
) {
    val colors = ZenTheme.colors
    var state by remember { mutableStateOf(WelcomeState.PINS_6) }

    LaunchedEffect(Unit) {
        delay(600)
        state = WelcomeState.PINS_12
        delay(600)
        state = WelcomeState.PINS_24
        delay(600)
        state = WelcomeState.LOGO_GLOW
        delay(1000)
        state = WelcomeState.BOTTOM_TEXT
        delay(1000)
        state = WelcomeState.FULL_PAGE
    }

    // Animation removed as per request

    // Use the common OnboardingScreenLayout
    com.zenlauncher.zenmode.ui.components.OnboardingScreenLayout(
        progress = 0.17f,
        progressText = "17%",
        buttonText = "Sign in with Google",
        onButtonClick = onGoogleSignInClick,
        bottomFooter = {
            var showReviewerFields by remember { mutableStateOf(false) }
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val footerText = buildAnnotatedString {
                    withStyle(MaterialTheme.typography.bodyMedium.toSpanStyle().copy(color = colors.textSecondary)) {
                        append("By continuing, you agree to the ")
                    }
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = AppConstants.PRIVACY_POLICY_URL
                    )
                    withStyle(MaterialTheme.typography.bodyMedium.toSpanStyle().copy(
                        color = colors.textBrand,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )) {
                        append("Zenmode's policy and terms & conditions")
                    }
                    pop()
                }
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                androidx.compose.foundation.text.ClickableText(
                    text = footerText,
                    modifier = Modifier.padding(horizontal = 32.rdp),
                    style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    onClick = { offset ->
                        footerText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                    }
                )

                Spacer(modifier = Modifier.height(8.rdp))

                if (!showReviewerFields) {
                    Text(
                        text = "Reviewer? Sign in here",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.rsp),
                        modifier = Modifier.clickable { showReviewerFields = true }
                    )
                }

                AnimatedVisibility(visible = showReviewerFields) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.rdp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(52.rdp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = colors.textPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.textBrand,
                                unfocusedBorderColor = colors.textSecondary,
                                focusedLabelColor = colors.textBrand,
                                unfocusedLabelColor = colors.textSecondary,
                                cursorColor = colors.textBrand
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().height(52.rdp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = colors.textPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.textBrand,
                                unfocusedBorderColor = colors.textSecondary,
                                focusedLabelColor = colors.textBrand,
                                unfocusedLabelColor = colors.textSecondary,
                                cursorColor = colors.textBrand
                            )
                        )
                        Spacer(modifier = Modifier.height(8.rdp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.rdp)
                                .background(colors.textBrand, RoundedCornerShape(20.dp))
                                .clickable {
                                    if (email.isNotBlank() && password.isNotBlank()) {
                                        onEmailSignInClick(email.trim(), password)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign in",
                                color = colors.bgPrimary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    ) {
        // ── Main content for the center area ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Background decorative blurred pins
            Image(
                painter = painterResource(id = R.drawable.logo_only_pins),
                contentDescription = null,
                modifier = Modifier
                    .size(303.rdp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 210.rdp)
                    .blur(9.7.dp),
                contentScale = ContentScale.Fit
            )
            
            val numPins = when (state) {
                WelcomeState.PINS_6 -> 6
                WelcomeState.PINS_12 -> 12
                else -> 24
            }

            val pinPath = remember {
                Path().apply {
                    moveTo(166.09f, 57.7f)
                    lineTo(171.64f, 77.69f)
                    lineTo(166.09f, 81.42f)
                    lineTo(160.46f, 77.69f)
                    close()
                }
            }

            // Single Unified Canvas for faint outer glow and custom arc pins (only before final state)
            if (state < WelcomeState.LOGO_GLOW) {
                val outerGlowColors = listOf(
                    colors.textBrand.copy(alpha = 0.3f), // faint glow behind pins
                    Color.Transparent
                )
                Canvas(
                    modifier = Modifier.size(250.rdp)
                ) {
                    val centerPos = Offset(size.width / 2, size.height / 2)
                    val gradientRadius = size.width / 2

                    // 1. Draw outer glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = outerGlowColors,
                            center = centerPos,
                            radius = gradientRadius
                        ),
                        radius = gradientRadius,
                        center = centerPos
                    )

                    // 2. Draw Arc Pins
                    val viewScale = size.width / 333f
                    val pivot333 = Offset(166.5f, 166.5f)
                    val startAngleOffset = 0f
                    
                    scale(viewScale, pivot = Offset.Zero) {
                        for (i in 0 until numPins) {
                            val angle = startAngleOffset + i * (360f / 24f)
                            rotate(degrees = angle, pivot = pivot333) {
                                drawPath(pinPath, color = Color(0xFF2AB828))
                            }
                        }
                    }
                }
            }

            // Show the official logo drawable seamlessly when central glow appears
            androidx.compose.animation.AnimatedVisibility(
                visible = state >= WelcomeState.LOGO_GLOW,
                enter = fadeIn(animationSpec = tween(600)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "Zenmode Logo",
                    modifier = Modifier.size(250.rdp),
                    contentScale = ContentScale.Fit
                )
            }

            // "ZENMODE" text below the logo
            androidx.compose.animation.AnimatedVisibility(
                visible = state >= WelcomeState.LOGO_GLOW,
                enter = fadeIn(animationSpec = tween(800)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 140.rdp)
            ) {
                Text(
                    text = "ZENMODE",
                    color = colors.textBrand,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 18.rsp)
                )
            }
        }

        // ── Middle Text (fades in at FULL_PAGE) ──
        androidx.compose.animation.AnimatedVisibility(
            visible = state == WelcomeState.FULL_PAGE,
            enter = fadeIn(animationSpec = tween(800)),
            modifier = Modifier.padding(top = 40.rdp, bottom = 30.rdp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val firstStepText = buildAnnotatedString {
                    withStyle(MaterialTheme.typography.titleMedium.toSpanStyle().copy(
                        color = colors.textPrimary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )) {
                        append("Your first step ")
                    }
                    withStyle(MaterialTheme.typography.bodyMedium.toSpanStyle().copy(color = colors.textSecondary)) {
                        append("towards")
                    }
                }
                Text(text = firstStepText)

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Mindful Digital time",
                    color = colors.textBrand,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.rsp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = colors.textBrand.copy(alpha = 0.5f),
                            blurRadius = 24.2f,
                            offset = Offset(0f, 16f)
                        )
                    )
                )
            }
        }

        // ── Bottom Content (fades in at BOTTOM_TEXT) ──
        androidx.compose.animation.AnimatedVisibility(
            visible = state >= WelcomeState.BOTTOM_TEXT,
            enter = fadeIn(animationSpec = tween(800)),
            modifier = Modifier.padding(bottom = 40.rdp, start = 20.rdp, end = 20.rdp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // "Bro, Hear me close," + heart
                // Offset by half of (spacer 8dp + heart 34dp) so only text is center-aligned
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset(x = 21.rdp)
                ) {
                    Text(
                        text = "Bro, Hear me close,",
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.rsp)
                    )
                    Spacer(modifier = Modifier.width(8.rdp))
                    Image(
                        painter = painterResource(id = R.drawable.heart_byhand),
                        contentDescription = "Heart",
                        modifier = Modifier.size(34.rdp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // "Life is *too* short!"
                val fontSize14 = 14.rsp
                val lifeText = buildAnnotatedString {
                    withStyle(MaterialTheme.typography.titleMedium.toSpanStyle().copy(color = colors.textPrimary, fontSize = fontSize14)) {
                        append("Life is ")
                    }
                    withStyle(MaterialTheme.typography.labelLarge.toSpanStyle().copy(color = colors.textBrand, fontSize = fontSize14)) {
                        append("too")
                    }
                    withStyle(MaterialTheme.typography.titleMedium.toSpanStyle().copy(color = colors.textPrimary, fontSize = fontSize14)) {
                        append(" short!")
                    }
                }
                Text(
                    text = lifeText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.rdp))

                // 'Just "4000 weeks", Let's make em count!'
                val fontSize16 = 16.rsp
                val weeksText = buildAnnotatedString {
                    withStyle(MaterialTheme.typography.titleMedium.toSpanStyle().copy(color = colors.textPrimary, fontSize = fontSize16)) {
                        append("Just \"4000 weeks\", ")
                    }
                    withStyle(MaterialTheme.typography.labelLarge.toSpanStyle().copy(color = colors.textPrimary, fontSize = fontSize16)) {
                        append("Let's make em count!")
                    }
                }
                Text(
                    text = weeksText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
