package com.zenlauncher.zenmode.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ZenAccessibilityService
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.components.OnboardingScreenLayout
import com.zenlauncher.zenmode.ui.theme.Black
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.Grey400
import com.zenlauncher.zenmode.ui.theme.White
import com.zenlauncher.zenmode.ui.theme.ZenBase
import com.zenlauncher.zenmode.ui.theme.ZenDark
import com.zenlauncher.zenmode.ui.theme.ZenTheme

@Composable
fun AccessibilityDisclosureScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Accessibility Service Disclosure",
                color = White,
                style = TextStyle(
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Why
            Text(
                text = "Why ZenMode needs Accessibility Service",
                color = ZenBase,
                style = TextStyle(
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ZenMode uses Android's Accessibility Service API solely to lock your screen " +
                        "when you tap the lock button on the home screen. Android does not provide any " +
                        "other way for a launcher app to lock the screen, so this permission is required " +
                        "for the lock-screen feature to work.",
                color = White,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section: What it does
            Text(
                text = "What this service does",
                color = ZenBase,
                style = TextStyle(
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Uses the system lock-screen action (GLOBAL_ACTION_LOCK_SCREEN) to lock your device\n" +
                        "• This action is triggered only when you manually tap the lock button\n" +
                        "• The service does not perform any other actions",
                color = White,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section: What it does NOT do
            Text(
                text = "What this service does NOT do",
                color = ZenBase,
                style = TextStyle(
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Does NOT read, collect, or access any screen content\n" +
                        "• Does NOT monitor or process any accessibility events\n" +
                        "• Does NOT perform gestures or interact with other apps\n" +
                        "• Does NOT collect, store, transmit, or share any personal data\n" +
                        "• Does NOT run in the background to observe your activity",
                color = White,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Privacy
            Text(
                text = "Privacy",
                color = ZenBase,
                style = TextStyle(
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your privacy is important to us. This service exists only to enable the lock button. " +
                        "No data of any kind is accessed through this service.",
                color = White,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Read our Privacy Policy",
                color = ZenDark,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.clickable {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.PRIVACY_POLICY_URL))
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Buttons at the bottom
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ZenBase),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "I Understand and Agree",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onDecline,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Decline", color = Grey400)
        }
    }
}

@Composable
fun AccessibilityServiceScreen(
    onGrantAccessClick: () -> Unit,
    onSkipClick: () -> Unit,
    onBackClick: () -> Unit
) {
    OnboardingScreenLayout(
        progress = 0.83f,
        progressText = "83%",
        buttonText = "Grant access",
        onButtonClick = onGrantAccessClick,
        showLogo = true,
        onBackClick = onBackClick,
        bottomFooter = {
            Text(
                text = "Skip for now",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onSkipClick() }
            )
        }
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Image(
            painter = painterResource(id = R.drawable.onboarding_accessibility_service),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(9.dp))

        Text(
            text = "Accessibility Service",
            color = White,
            style = TextStyle(
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Placeholder card for accessibility permission settings animation
        Box(
            modifier = Modifier
                .width(286.dp)
                .height(187.dp)
                .background(Color(0x29494949), RoundedCornerShape(10.dp))
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "ZenMode needs the Accessibility Service permission to lock your screen when you tap the lock button. This is the only thing it does \u2014 it does not read your screen or collect any data.",
            color = White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

class AccessibilityServiceFragment : Fragment() {

    private var hasOpenedSettings = false
    private var showDisclosureDialog = mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ServiceLocator.analyticsTracker.trackPermissionScreenViewed("acc")
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setContent {
                ZenTheme {
                    if (showDisclosureDialog.value) {
                        AccessibilityDisclosureScreen(
                            onAccept = {
                                showDisclosureDialog.value = false
                                hasOpenedSettings = true
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onDecline = {
                                showDisclosureDialog.value = false
                                navigateTo(+1)
                            }
                        )
                    } else {
                        AccessibilityServiceScreen(
                            onGrantAccessClick = { handleGrantAccess() },
                            onSkipClick = { navigateTo(+1) },
                            onBackClick = { navigateTo(-1) }
                        )
                    }
                }
            }
        }
    }

    private fun navigateTo(delta: Int) {
        val viewPager = activity?.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager) ?: return
        viewPager.currentItem = (viewPager.currentItem + delta).coerceAtLeast(0)
    }

    private fun handleGrantAccess() {
        if (isAccessibilityServiceEnabled(requireContext())) {
            trackPermissionAndNavigate()
        } else {
            showDisclosureDialog.value = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasOpenedSettings && isAccessibilityServiceEnabled(requireContext())) {
            hasOpenedSettings = false
            trackPermissionAndNavigate()
        }
    }

    private fun trackPermissionAndNavigate() {
        val tracker = ServiceLocator.analyticsTracker
        tracker.trackPermissionGranted("acc")

        // Record in repository
        val repository = UsageRepository(requireContext(), ServiceLocator.analyticsManager)
        repository.recordPermissionGranted("acc")

        navigateTo(+1)
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return ZenAccessibilityService.isEnabledInSettings(context)
    }
}
