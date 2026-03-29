package com.zenlauncher.zenmode.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.zenlauncher.zenmode.ThemePreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.MainActivity
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.SettingsActivity
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.components.OnboardingScreenLayout
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.components.ZenSettingToggleItem
import com.zenlauncher.zenmode.ui.components.StatsCardsRow
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque

@Composable
fun DefaultLauncherScreen(
    onSetDefaultLauncherClick: () -> Unit,
    onShareClick: () -> Unit,
    onChangeDistractingAppsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var isDarkMode by remember { mutableStateOf(ThemePreferences.isDarkMode(context)) }
    val uriHandler = LocalUriHandler.current

    ZenTheme(darkTheme = isDarkMode) {
        val colors = ZenTheme.colors
        OnboardingScreenLayout(
        progress = 0.99f,
        progressText = "99%",
        buttonText = "Set as default Launcher",
        onButtonClick = onSetDefaultLauncherClick,
        showLogo = false,
        onBackClick = onBackClick,
        bottomFooter = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShareClick() }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Share zenmode",
                    color = colors.textBrand,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(7.dp))
                Image(
                    painter = painterResource(id = R.drawable.share_zenmode),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ZenMode logo
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "ZenMode logo",
                modifier = Modifier.size(60.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(13.dp))

            // Title
            Text(
                text = "One Last step to Experience Zen",
                color = colors.textPrimary,
                style = TextStyle(
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Let's make it pretty personal, Ready?",
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dark mode toggle
            ZenSettingToggleItem(
                text = "Dark mode",
                checked = isDarkMode,
                onCheckedChange = { enabled ->
                    isDarkMode = enabled
                    ThemePreferences.setDarkMode(context, enabled)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Change distracting app list card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSecondary, RoundedCornerShape(26.dp))
                    .border(1.dp, colors.bgSecondary, RoundedCornerShape(26.dp))
                    .clickable { onChangeDistractingAppsClick() }
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Change distracting app list",
                    color = colors.textBrand,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats preview
            StatsCardsRow(
                usage = DailyUsage(screenTimeInMillis = 2220000L), // 37 mins
                yesterdayChangePercent = -12,
                hasBuddies = true,
                buddyStats = BuddyStats(screenTimeMins = 15),
                isSignedIn = true,
                onInviteBuddyClick = {},
                onSignInClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // "How to invite your zen buddy Watch video(↗)"
            val buddyInviteText = buildAnnotatedString {
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = colors.textPrimary)) {
                    append("How to invite your zen buddy ")
                }
                pushStringAnnotation(tag = "URL", annotation = AppConstants.YT_BUDDY_INVITE_URL)
                withStyle(
                    MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = colors.textBrand)
                ) {
                    append("Watch video(↗)")
                }
                pop()
            }
            androidx.compose.foundation.text.ClickableText(
                text = buddyInviteText,
                modifier = Modifier.fillMaxWidth(),
                onClick = { offset ->
                    buddyInviteText.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { uriHandler.openUri(it.item) }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Numbered invite steps
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "1. Share", color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
                Text(text = "2. ", color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
                Text(text = "3. ", color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // "(Still confused? Watch video on how add your friend as zen buddy(↗))"
            val confusedText = buildAnnotatedString {
                withStyle(MaterialTheme.typography.bodyMedium.toSpanStyle().copy(color = colors.textPrimary)) {
                    append("(Still confused? ")
                }
                pushStringAnnotation(tag = "URL", annotation = AppConstants.YT_BUDDY_CONFUSED_URL)
                withStyle(
                    MaterialTheme.typography.bodyMedium.toSpanStyle().copy(color = colors.textBrand)
                ) {
                    append("Watch video on how add your friend as zen buddy(↗)")
                }
                pop()
                withStyle(MaterialTheme.typography.bodyMedium.toSpanStyle().copy(color = colors.textPrimary)) {
                    append(")")
                }
            }
            androidx.compose.foundation.text.ClickableText(
                text = confusedText,
                modifier = Modifier.fillMaxWidth(),
                onClick = { offset ->
                    confusedText.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { uriHandler.openUri(it.item) }
                }
            )
        }
    }
    }
}

class DefaultLauncherFragment : Fragment() {

    private var hasOpenedSettings = false

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (isDefaultLauncher()) completeOnboarding()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ServiceLocator.analyticsTracker.trackPermissionScreenViewed("launcher_set_default")
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setContent {
                ZenTheme {
                    DefaultLauncherScreen(
                        onSetDefaultLauncherClick = { handleSetDefaultLauncher() },
                        onShareClick = { shareZenMode() },
                        onChangeDistractingAppsClick = {
                            startActivity(Intent(requireContext(), SettingsActivity::class.java))
                        },
                        onBackClick = { navigateTo(-1) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasOpenedSettings && isDefaultLauncher()) {
            hasOpenedSettings = false
            completeOnboarding()
        }
    }

    private fun handleSetDefaultLauncher() {
        if (isDefaultLauncher()) {
            completeOnboarding()
        } else {
            hasOpenedSettings = true
            settingsLauncher.launch(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val info = requireContext().packageManager.resolveActivity(
            intent, PackageManager.MATCH_DEFAULT_ONLY
        )
        return info?.activityInfo?.packageName == requireContext().packageName
    }

    private fun navigateTo(delta: Int) {
        val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager) ?: return
        viewPager.currentItem = (viewPager.currentItem + delta).coerceAtLeast(0)
    }

    private fun shareZenMode() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out ZenMode - a minimalist open-source android launcher! ${AppConstants.GITHUB_URL}"
            )
        }
        startActivity(Intent.createChooser(intent, "Share ZenMode"))
    }

    private fun completeOnboarding() {
        val analyticsManager = ServiceLocator.analyticsManager
        val repository = UsageRepository(requireContext(), analyticsManager)
        repository.setOnboardingComplete(true)
        repository.clearOnboardingCurrentPage()
        
        // Finalize metrics
        val startTime = repository.getOnboardingStartTime()
        val timeTakenSec = if (startTime > 0) ((System.currentTimeMillis() - startTime) / 1000).toInt() else 0
        
        // Launcher is also a permission in this context
        repository.recordPermissionGranted("launcher_set_default")
        ServiceLocator.analyticsTracker.trackPermissionGranted("launcher_set_default")
        val grantedCount = repository.getGrantedPermissionsCount()

        ServiceLocator.analyticsTracker.trackSetupCompleted(timeTakenSec, grantedCount)
        
        // Optional: clear metrics after successful track
        repository.clearOnboardingMetrics()

        val mainIntent = Intent(requireContext(), MainActivity::class.java)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(mainIntent)
        requireActivity().finish()
    }
}
