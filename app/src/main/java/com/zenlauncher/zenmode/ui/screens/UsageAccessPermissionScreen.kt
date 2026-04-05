package com.zenlauncher.zenmode.ui.screens

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import com.zenlauncher.zenmode.ui.components.WeightSpacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.zenlauncher.zenmode.ui.theme.rdp
import androidx.viewpager2.widget.ViewPager2
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.components.OnboardingScreenLayout
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.Grey600
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rsp

@Composable
fun UsageAccessPermissionScreen(
    onGrantAccessClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val colors = ZenTheme.colors
    OnboardingScreenLayout(
        progress = 0.5f,
        progressText = "50%",
        buttonText = "Grant access",
        onButtonClick = onGrantAccessClick,
        showLogo = true,
        onBackClick = onBackClick,
        bottomFooter = null,
        showBgShuriken = true,
        bgShurikenOffsetX = 80.rdp,
        bgShurikenOffsetY = 200.rdp,
    ) {
        UsageBarChartIcon(
            modifier = Modifier.size(60.dp)
        )
        
        WeightSpacer(1f)

        // Title
        Text(
            text = "Usage Access Permission",
            color = colors.textPrimary,
            style = TextStyle(
                fontFamily = CabinetGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 24.rsp
            )
        )

        WeightSpacer(1f)

        // Center visual component natively built mimicking the Figma spec "usage_access_permission_overlay"
        Box(
            modifier = Modifier
                .width(286.dp)
                .background(colors.bgSecondary.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                .border(1.dp, colors.bgSecondary, RoundedCornerShape(10.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Usage Access Permission",
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Inner permission mock
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bgPrimary, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.textSecondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    // Row 1: App row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Green icon
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
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(colors.textBrand)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = "Zenmode",
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Fake chevron
                        Text(text = ">", color = colors.textSecondary)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.textSecondary.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Row 2: Toggle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Toggle knob mock
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(colors.textPrimary, RoundedCornerShape(6.dp))
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Line mock
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                                .background(colors.textPrimary.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = ">", color = Grey600)
                    }
                }
            }
        }
        
        WeightSpacer(2f)

        // Text instructions
        Text(
            text = "We want to see which apps are eating your time. Not to judge just to help you reclaim it.",
            color = colors.textPrimary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        WeightSpacer(2f)
    }
}

@Composable
fun UsageBarChartIcon(modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    val axisColor = colors.textBrand
    val barColor = colors.textBrand

    Canvas(modifier = modifier) {
        val strokeWidth = 1.5.dp.toPx()

        // Draw L axis
        drawLine(
            color = axisColor,
            start = Offset(size.width * 0.15f, size.height * 0.15f),
            end = Offset(size.width * 0.15f, size.height * 0.85f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = axisColor,
            start = Offset(size.width * 0.15f, size.height * 0.85f),
            end = Offset(size.width * 0.85f, size.height * 0.85f),
            strokeWidth = strokeWidth
        )

        val barWidth = size.width * 0.14f
        val gap = size.width * 0.08f

        // Bar 1 (Short)
        var startX = size.width * 0.25f
        drawRect(
            color = barColor,
            topLeft = Offset(startX, size.height * 0.6f),
            size = androidx.compose.ui.geometry.Size(barWidth, size.height * 0.25f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )

        // Bar 2 (Medium)
        startX += barWidth + gap
        drawRect(
            color = barColor,
            topLeft = Offset(startX, size.height * 0.4f),
            size = androidx.compose.ui.geometry.Size(barWidth, size.height * 0.45f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        
        // Let's add diagonal lines to the second bar to match Figma loosely
        val maxH2 = size.height * 0.4f
        var yPos2 = size.height * 0.85f
        while (yPos2 > maxH2) {
            drawLine(
                color = barColor,
                start = Offset(startX, yPos2),
                end = Offset(startX + barWidth, yPos2 - barWidth),
                strokeWidth = strokeWidth / 2f
            )
            yPos2 -= barWidth * 0.6f
        }

        // Bar 3 (Tall)
        startX += barWidth + gap
        drawRect(
            color = barColor,
            topLeft = Offset(startX, size.height * 0.25f),
            size = androidx.compose.ui.geometry.Size(barWidth, size.height * 0.6f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )

        // Add diagonal lines to the last bar
        val maxH3 = size.height * 0.25f
        var yPos3 = size.height * 0.85f
        while (yPos3 > maxH3) {
            drawLine(
                color = barColor,
                start = Offset(startX, yPos3),
                end = Offset(startX + barWidth, yPos3 - barWidth),
                strokeWidth = strokeWidth / 2f
            )
            yPos3 -= barWidth * 0.6f
        }
    }
}

class UsageAccessPermissionFragment : Fragment() {

    private var hasOpenedSettings = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ServiceLocator.analyticsTracker.trackPermissionScreenViewed("usage")
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setContent {
                ZenTheme {
                    UsageAccessPermissionScreen(
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
        if (hasUsageStatsPermission()) {
            trackPermissionAndNavigate()
        } else {
            hasOpenedSettings = true
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasOpenedSettings && hasUsageStatsPermission()) {
            hasOpenedSettings = false
            trackPermissionAndNavigate()
        }
    }

    private fun trackPermissionAndNavigate() {
        val tracker = ServiceLocator.analyticsTracker
        tracker.trackPermissionGranted("usage")

        // Record in repository for setup_completed count
        val repository = UsageRepository(requireContext(), ServiceLocator.analyticsManager)
        repository.recordPermissionGranted("usage")

        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
        if (viewPager != null && viewPager.adapter != null &&
            viewPager.currentItem < (viewPager.adapter!!.itemCount - 1)) {
            viewPager.currentItem = viewPager.currentItem + 1
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = requireContext().getSystemService(android.content.Context.APP_OPS_SERVICE)
            as android.app.AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                requireContext().packageName
            )
        } else {
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                requireContext().packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
}
