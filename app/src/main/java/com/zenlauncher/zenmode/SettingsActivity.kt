package com.zenlauncher.zenmode

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.PlanOffer
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.ContentBlockingBottomSheet
import com.zenlauncher.zenmode.ui.screens.DistractingAppsBottomSheet
import com.zenlauncher.zenmode.ui.components.zenOverlayBlur
import com.zenlauncher.zenmode.ui.screens.HomeAppsPickerOverlay
import com.zenlauncher.zenmode.ui.screens.SettingsScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private var notificationBadgesEnabled by mutableStateOf(false)
    private var distractingAppCount by mutableStateOf(0)
    private var contentBlockingOn by mutableStateOf(false)
    private var offers by mutableStateOf<List<PlanOffer>>(emptyList())
    private var homeAppsChosenCount by mutableStateOf(0)
    private var showHomeAppsPicker by mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        // All three are changed outside this screen (system settings or a sheet), so re-read them.
        notificationBadgesEnabled = ZenNotificationListenerService.isEnabledInSettings(this)
        distractingAppCount = DistractingAppsRepository.getUserSelected(this).size
        contentBlockingOn = ContentBlockPrefs.isAnyBlockEnabled(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = UsageRepository(applicationContext, ServiceLocator.analyticsManager)
        homeAppsChosenCount = repository.getPinnedApps().size
        val weeklyHours = repository.getWeeklyScreenTimeHours()
        val profilePhotoUrl = ServiceLocator.authProvider.getPhotoUrl()
        val displayName = ServiceLocator.authProvider.getDisplayName()
        val entitlements = ServiceLocator.entitlementProvider
        notificationBadgesEnabled = ZenNotificationListenerService.isEnabledInSettings(this)
        distractingAppCount = DistractingAppsRepository.getUserSelected(this).size
        contentBlockingOn = ContentBlockPrefs.isAnyBlockEnabled(this)
        if (savedInstanceState == null) {
            ServiceLocator.analyticsManager.trackEvent(
                "settings_viewed",
                mapOf("tier" to entitlements.entitlement.value.status.name.lowercase())
            )
        }

        setContent {
            ZenTheme(darkTheme = ThemePreferences.isDarkMode(this@SettingsActivity)) {
                var showDistractingSheet by remember { mutableStateOf(false) }
                var showContentBlockSheet by remember { mutableStateOf(false) }
                val entitlement by entitlements.entitlement.collectAsState()
                LaunchedEffect(entitlements.isAvailable) {
                    if (entitlements.isAvailable) offers = entitlements.offers()
                }
                SettingsScreen(
                    weeklyHours = weeklyHours,
                    profilePhotoUrl = profilePhotoUrl,
                    displayName = displayName,
                    isProAvailable = entitlements.isAvailable,
                    entitlement = entitlement,
                    offers = offers,
                    distractingAppCount = distractingAppCount,
                    isContentBlockingOn = contentBlockingOn,
                    homeAppsChosenCount = homeAppsChosenCount,
                    onChooseHomeAppsClick = { showHomeAppsPicker = true },
                    loadMonthlyHours = {
                        withContext(Dispatchers.IO) {
                            repository.getDailyScreenTimeHours(UsageRepository.CACHE_RETENTION_DAYS)
                        }
                    },
                    isNotificationBadgesEnabled = notificationBadgesEnabled,
                    onNotificationBadgesClick = {
                        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    },
                    onBackClick = { finish() },
                    onChangeDistractingAppsClick = { showDistractingSheet = true },
                    onBlockInAppContentClick = { showContentBlockSheet = true },
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
                    onOpenPro = { entry -> startActivity(ZenProActivity.intent(this, entry)) },
                    onProGateShown = { feature ->
                        ServiceLocator.analyticsManager.trackEvent(
                            "pro_gate_opened",
                            mapOf("feature" to feature.name.lowercase())
                        )
                    },
                    onLogoutClick = { performLogout(repository) },
                    onDeleteAccountClick = { performDeleteAccount(repository) },
                    modifier = Modifier.zenOverlayBlur(showHomeAppsPicker)
                )
                if (showDistractingSheet) {
                    DistractingAppsBottomSheet(onDismiss = {
                        showDistractingSheet = false
                        distractingAppCount = DistractingAppsRepository.getUserSelected(this).size
                    })
                }
                HomeAppsPickerOverlay(
                    visible = showHomeAppsPicker,
                    limit = AppGridPreferences.getAppCount(this),
                    // Home apps reuse the pinned-apps list, so earlier pins carry over as the first spots.
                    initialSelection = repository.getPinnedApps(),
                    onSelectionChange = { packages ->
                        repository.savePinnedApps(packages)
                        homeAppsChosenCount = packages.size
                    },
                    onDismiss = { showHomeAppsPicker = false }
                )
                if (showContentBlockSheet) {
                    ContentBlockingBottomSheet(onDismiss = {
                        showContentBlockSheet = false
                        contentBlockingOn = ContentBlockPrefs.isAnyBlockEnabled(this)
                    })
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
