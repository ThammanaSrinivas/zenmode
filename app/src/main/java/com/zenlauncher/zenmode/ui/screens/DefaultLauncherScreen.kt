package com.zenlauncher.zenmode.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
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
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.components.OnboardingScreenLayout
import com.zenlauncher.zenmode.ui.theme.Black
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.Grey400
import com.zenlauncher.zenmode.ui.theme.Grey600
import com.zenlauncher.zenmode.ui.theme.White
import com.zenlauncher.zenmode.ui.theme.ZenBase
import com.zenlauncher.zenmode.ui.theme.ZenGlow
import com.zenlauncher.zenmode.ui.theme.ZenTheme

private const val PREFS_NAME = "zenmode_prefs"
private const val KEY_DARK_MODE = "dark_mode_enabled"

@Composable
fun DefaultLauncherScreen(
    onSetDefaultLauncherClick: () -> Unit,
    onShareClick: () -> Unit,
    onChangeDistractingAppsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var isDarkMode by remember { mutableStateOf(prefs.getBoolean(KEY_DARK_MODE, true)) }
    val uriHandler = LocalUriHandler.current

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
                    color = ZenGlow,
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
                color = White,
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
                color = Grey400,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dark mode toggle card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(26.dp))
                    .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(26.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dark  mode",
                    color = White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { enabled ->
                        isDarkMode = enabled
                        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
                        AppCompatDelegate.setDefaultNightMode(
                            if (enabled) AppCompatDelegate.MODE_NIGHT_YES
                            else AppCompatDelegate.MODE_NIGHT_NO
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = ZenGlow,
                        uncheckedThumbColor = White,
                        uncheckedTrackColor = Grey600
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Change distracting app list card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(26.dp))
                    .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(26.dp))
                    .clickable { onChangeDistractingAppsClick() }
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Change distracting app list",
                    color = ZenBase,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats preview image
            Image(
                painter = painterResource(id = R.drawable.last_step_stats),
                contentDescription = "Stats preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.FillWidth
            )

            Spacer(modifier = Modifier.height(16.dp))

            // "How to invite your zen buddy Watch video(↗)"
            val buddyInviteText = buildAnnotatedString {
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = White)) {
                    append("How to invite your zen buddy ")
                }
                pushStringAnnotation(tag = "URL", annotation = AppConstants.YT_BUDDY_INVITE_URL)
                withStyle(
                    MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = ZenGlow)
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
                Text(text = "1. Share", color = White, style = MaterialTheme.typography.bodyLarge)
                Text(text = "2. ", color = White, style = MaterialTheme.typography.bodyLarge)
                Text(text = "3. ", color = White, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // "(Still confused? Watch video on how add your friend as zen buddy(↗))"
            val confusedText = buildAnnotatedString {
                withStyle(MaterialTheme.typography.bodyMedium.toSpanStyle().copy(color = White)) {
                    append("(Still confused? ")
                }
                pushStringAnnotation(tag = "URL", annotation = AppConstants.YT_BUDDY_CONFUSED_URL)
                withStyle(
                    MaterialTheme.typography.bodyMedium.toSpanStyle().copy(color = ZenGlow)
                ) {
                    append("Watch video on how add your friend as zen buddy(↗)")
                }
                pop()
                withStyle(MaterialTheme.typography.bodyMedium.toSpanStyle().copy(color = White)) {
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
        ServiceLocator.analyticsTracker.trackOnboardingCompleted()

        val mainIntent = Intent(requireContext(), MainActivity::class.java)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(mainIntent)
        requireActivity().finish()
    }
}
