package com.zenlauncher.zenmode.onboarding

import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.isInk
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/** 03 · Google sign-in, or explore first. Account-only features (Zen Circle) wait until later. */
@Composable
internal fun SignInStep(
    progress: StepProgress,
    isLoading: Boolean,
    onBack: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onEmailSignIn: (email: String, password: String) -> Unit,
    onExplore: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    OnboardingPage(
        topBar = { progress.TopBar(onBack = onBack) },
        bottomBar = {
            OnboardingButton(
                text = if (isLoading) "Signing you in…" else "Continue with Google",
                onClick = onGoogleSignIn,
                enabled = !isLoading,
                style = OnboardingButtonStyle.Light,
                modifier = Modifier.border(1.dp, ZenTheme.colors.borderSubtle, RoundedCornerShape(percent = 50)),
                leading = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.rdp),
                            strokeWidth = 2.dp,
                            color = ZenTheme.colors.textBrand
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_google),
                            contentDescription = null,
                            modifier = Modifier.size(20.rdp)
                        )
                    }
                }
            )
            OnboardingTextButton(text = "Explore first, sign in later", onClick = onExplore)
            Text(
                text = buildAnnotatedString {
                    append("By continuing you agree to our ")
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append("privacy policy") }
                    append(".")
                },
                fontFamily = Geist,
                fontSize = 12.rsp,
                color = ZenTheme.colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.pressScale(
                    onClick = { uriHandler.openUri(AppConstants.PRIVACY_POLICY_URL) },
                    onClickLabel = "Open privacy policy",
                    pressedScale = 0.98f
                )
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
            OnboardingEyebrow("Your account", modifier = Modifier.staggeredEntrance(0))
            Spacer(Modifier.height(10.rdp))
            OnboardingHeadline("Keep your Zen safe.", modifier = Modifier.staggeredEntrance(1))
            Spacer(Modifier.height(12.rdp))
            OnboardingBody(
                "Sign in to save your Zen Score, streaks and gold, and to find your Zen Bro.",
                modifier = Modifier.staggeredEntrance(2)
            )

            Spacer(Modifier.height(28.rdp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(3)
                    .clip(RoundedCornerShape(22.rdp))
                    .background(ZenTheme.colors.surfaceElevated)
                    .border(1.dp, ZenTheme.colors.borderSubtle, RoundedCornerShape(22.rdp))
                    .padding(vertical = 6.rdp)
            ) {
                PromiseRow("Your messages, photos and browsing never leave your phone.")
                PromiseRow("Only your daily screen-time total syncs, in minutes.")
                PromiseRow("The app is open source on GitHub.")
                PromiseRow("Delete your account anytime from Settings.", last = true)
            }

            Spacer(Modifier.height(20.rdp))
            ReviewerSignIn(isLoading = isLoading, onSignIn = onEmailSignIn)
            Spacer(Modifier.height(20.rdp))
        }
    }
}

/**
 * Email/password sign-in for Play Store reviewers, who get a test account rather than a
 * Google login. Kept low-key behind a small link.
 */
@Composable
private fun ReviewerSignIn(isLoading: Boolean, onSignIn: (email: String, password: String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!expanded) {
            OnboardingTextButton(
                text = "Reviewer? Sign in here",
                onClick = { expanded = true },
                color = ZenTheme.colors.textTertiary
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.rdp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OnboardingTextButton(
                    text = "Sign in",
                    onClick = {
                        if (!isLoading && email.isNotBlank() && password.isNotBlank()) onSignIn(email.trim(), password)
                    },
                    color = ZenTheme.colors.textBrand,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun PromiseRow(text: String, last: Boolean = false) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.rdp, vertical = 13.rdp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.rdp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.rdp)
                    .clip(CircleShape)
                    .background(ZenTheme.colors.surfaceTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = ZenTheme.colors.textBrand,
                    modifier = Modifier.size(15.rdp)
                )
            }
            Text(
                text = text,
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 15.rsp,
                color = ZenTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }
        if (!last) {
            Box(
                Modifier
                    .padding(start = 52.rdp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ZenTheme.colors.surfaceSunk)
            )
        }
    }
}
