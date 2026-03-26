package com.zenlauncher.zenmode.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ZenAccessibilityService
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.components.OnboardingScreenLayout
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.White
import com.zenlauncher.zenmode.ui.theme.ZenBase
import com.zenlauncher.zenmode.ui.theme.ZenTheme

@Composable
fun AccessibilityDisclosureDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Don't allow dismiss by tapping outside */ },
        title = { Text("How ZenMode uses Accessibility") },
        text = {
            Text(
                "ZenMode needs Accessibility permissions to lock the screen.\n\n" +
                "We do not collect, store, or share any personal data or screen content. " +
                "This is only used to enable the lock screen feature."
            )
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = ZenBase)
            ) { Text("I Agree") }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text("No Thanks") }
        }
    )
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
            text = "This is where the magic happens. Fair warning: it requires a lot of trust from you. We won't let you down.",
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
                    AccessibilityServiceScreen(
                        onGrantAccessClick = { handleGrantAccess() },
                        onSkipClick = { navigateTo(+1) },
                        onBackClick = { navigateTo(-1) }
                    )

                    if (showDisclosureDialog.value) {
                        AccessibilityDisclosureDialog(
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
