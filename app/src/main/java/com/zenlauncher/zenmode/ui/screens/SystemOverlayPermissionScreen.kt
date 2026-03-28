package com.zenlauncher.zenmode.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.components.OnboardingScreenLayout
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.ZenTheme

@Composable
fun SystemOverlayPermissionScreen(
    onGrantAccessClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val colors = ZenTheme.colors
    OnboardingScreenLayout(
        progress = 0.66f,
        progressText = "66%",
        buttonText = "Grant access",
        onButtonClick = onGrantAccessClick,
        showLogo = true,
        onBackClick = onBackClick,
        bottomFooter = null
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Image(
            painter = painterResource(id = R.drawable.onboarding_system_overlay_permission),
            contentDescription = null,
            modifier = Modifier.size(65.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(9.dp))

        Text(
            text = "System Overlay Permission",
            color = colors.textPrimary,
            style = TextStyle(
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // System overlay mockup card mimicking Android overlay settings panel
        Box(
            modifier = Modifier
                .width(286.dp)
                .background(colors.bgSecondary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "System overlay Permission",
                    color = colors.textSecondary,
                    style = TextStyle(
                        fontFamily = CabinetGrotesque,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Zenmode row with divider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Green dot icon (Zenmode app icon indicator)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colors.textBrand.copy(alpha = 0.2f))
                                .border(2.dp, colors.textBrand, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colors.textBrand)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Zenmode",
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(text = ">", color = colors.textSecondary, fontSize = 12.sp)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.textSecondary.copy(alpha = 0.5f))
                    )

                    // Toggle row (showing "Allow display over other apps" off state)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Off-state toggle
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(22.dp)
                                .background(colors.textSecondary.copy(alpha = 0.6f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 2.dp)
                                    .size(18.dp)
                                    .background(colors.textPrimary, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Gray placeholder line
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(8.dp)
                                .background(colors.textSecondary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(text = ">", color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Let us be the strict friend who won't let you doom scroll. You'll thank us later (probably).",
            color = colors.textPrimary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

class SystemOverlayPermissionFragment : Fragment() {

    private var hasOpenedSettings = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ServiceLocator.analyticsTracker.trackPermissionScreenViewed("overlay")
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setContent {
                ZenTheme {
                    SystemOverlayPermissionScreen(
                        onGrantAccessClick = { handleGrantAccess() },
                        onBackClick = { navigateTo(-1) }
                    )
                }
            }
        }
    }

    private fun navigateTo(delta: Int) {
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager) ?: return
        viewPager.currentItem = (viewPager.currentItem + delta).coerceAtLeast(0)
    }

    private fun handleGrantAccess() {
        if (Settings.canDrawOverlays(requireContext())) {
            trackPermissionAndNavigate()
        } else {
            hasOpenedSettings = true
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireContext().packageName}")
            )
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasOpenedSettings && Settings.canDrawOverlays(requireContext())) {
            hasOpenedSettings = false
            trackPermissionAndNavigate()
        }
    }

    private fun trackPermissionAndNavigate() {
        val tracker = ServiceLocator.analyticsTracker
        tracker.trackPermissionGranted("overlay")

        // Record in repository for setup_completed count
        val repository = com.zenlauncher.zenmode.coreapi.UsageRepository(
            requireContext(),
            ServiceLocator.analyticsManager
        )
        repository.recordPermissionGranted("overlay")

        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
        if (viewPager != null) {
            viewPager.post { viewPager.currentItem = viewPager.currentItem + 1 }
        }
    }
}
