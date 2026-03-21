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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import com.zenlauncher.zenmode.ui.components.OnboardingScreenLayout
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.White
import com.zenlauncher.zenmode.ui.theme.ZenTheme

@Composable
fun AccessibilityServiceScreen(
    onGrantAccessClick: () -> Unit,
    onBackClick: () -> Unit
) {
    OnboardingScreenLayout(
        progress = 0.83f,
        progressText = "83%",
        buttonText = "Grant access",
        onButtonClick = onGrantAccessClick,
        showLogo = true,
        onBackClick = onBackClick,
        bottomFooter = null
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setContent {
                ZenTheme {
                    AccessibilityServiceScreen(
                        onGrantAccessClick = { handleGrantAccess() },
                        onBackClick = { navigateTo(-1) }
                    )
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
            com.zenlauncher.zenmode.coreapi.services.ServiceLocator.analyticsTracker
                .trackPermissionsGranted("accessibility_service")
            navigateTo(+1)
        } else {
            hasOpenedSettings = true
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasOpenedSettings && isAccessibilityServiceEnabled(requireContext())) {
            hasOpenedSettings = false
            com.zenlauncher.zenmode.coreapi.services.ServiceLocator.analyticsTracker
                .trackPermissionsGranted("accessibility_service")
            navigateTo(+1)
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expectedComponent = "${context.packageName}/${context.packageName}.ZenAccessibilityService"
        return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
    }
}
