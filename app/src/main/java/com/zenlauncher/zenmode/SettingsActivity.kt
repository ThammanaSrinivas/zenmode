package com.zenlauncher.zenmode

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.SettingsScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = UsageRepository(this, ServiceLocator.analyticsManager)
        val weeklyHours = repository.getWeeklyScreenTimeHours()
        val profilePhotoUrl = ServiceLocator.authProvider.getPhotoUrl()

        setContent {
            ZenTheme(darkTheme = ThemePreferences.isDarkMode(this@SettingsActivity)) {
                SettingsScreen(
                    weeklyHours = weeklyHours,
                    profilePhotoUrl = profilePhotoUrl,
                    onBackClick = { finish() },
                    onChangeDistractingAppsClick = {
                        // TODO: navigate to distracting apps picker
                    },
                    onAccountabilityPartnerClick = {
                        // TODO: navigate to accountability partner settings
                    },
                    onContributeClick = { openGitHub() },
                    onRateClick = { openPlayStore() },
                    onShareClick = { shareZenMode() },
                    onLogoutClick = { performLogout(repository) },
                    onDeleteAccountClick = { performDeleteAccount(repository) }
                )
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
        ServiceLocator.authProvider.signOut()
        repository.clearUserData()
        navigateToOnboarding()
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
