package com.zenlauncher.zenmode

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.DistractingAppsBottomSheet
import com.zenlauncher.zenmode.ui.screens.SettingsScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private var notificationBadgesEnabled by mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        notificationBadgesEnabled = ZenNotificationListenerService.isEnabledInSettings(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = UsageRepository(applicationContext, ServiceLocator.analyticsManager)
        val weeklyHours = repository.getWeeklyScreenTimeHours()
        val profilePhotoUrl = ServiceLocator.authProvider.getPhotoUrl()
        notificationBadgesEnabled = ZenNotificationListenerService.isEnabledInSettings(this)

        setContent {
            ZenTheme(darkTheme = ThemePreferences.isDarkMode(this@SettingsActivity)) {
                var showDistractingSheet by remember { mutableStateOf(false) }
                SettingsScreen(
                    weeklyHours = weeklyHours,
                    profilePhotoUrl = profilePhotoUrl,
                    isNotificationBadgesEnabled = notificationBadgesEnabled,
                    onNotificationBadgesClick = {
                        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    },
                    onBackClick = { finish() },
                    onChangeDistractingAppsClick = { showDistractingSheet = true },
                    onAccountabilityPartnerClick = {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("SHOW_BUDDY_BATTLE", true)
                        }
                        startActivity(intent)
                    },
                    onContributeClick = { openGitHub() },
                    onRateClick = { openPlayStore() },
                    onShareClick = { shareZenMode() },
                    onLogoutClick = { performLogout(repository) },
                    onDeleteAccountClick = { performDeleteAccount(repository) }
                )
                if (showDistractingSheet) {
                    DistractingAppsBottomSheet(onDismiss = { showDistractingSheet = false })
                }
            }
        }
    }

    private fun openGitHub() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.GITHUB_URL)))
    }

    private fun openPlayStore() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                )
            )
        } catch (e: android.content.ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    private fun shareZenMode() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ZenMode Launcher")
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out ZenMode - a minimalist open-source android launcher! ${AppConstants.GITHUB_URL}"
            )
        }
        startActivity(Intent.createChooser(intent, "Share ZenMode"))
    }

    private fun performLogout(repository: UsageRepository) {
        lifecycleScope.launch {
            ServiceLocator.authProvider.signOut()
            CredentialManager.create(this@SettingsActivity)
                .clearCredentialState(ClearCredentialStateRequest())
            repository.clearUserData()
            navigateToOnboarding()
        }
    }

    private fun performDeleteAccount(repository: UsageRepository) {
        val uid = ServiceLocator.authProvider.getCurrentUserId()
        lifecycleScope.launch {
            try {
                if (uid != null) {
                    ServiceLocator.firestoreDataSource.deleteUser(uid)
                }
                ServiceLocator.authProvider.deleteAccount()
            } catch (_: Exception) {
                // Proceed with local cleanup even if remote deletion fails
            }
            CredentialManager.create(this@SettingsActivity)
                .clearCredentialState(ClearCredentialStateRequest())
            repository.clearAllData()
            ThemePreferences.clear(this@SettingsActivity)
            navigateToOnboarding()
        }
    }

    private fun navigateToOnboarding() {
        val intent = Intent(this, OnboardingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
