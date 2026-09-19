package com.zenlauncher.zenmode

import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.zenlauncher.zenmode.recap.ProUpsellSheet
import com.zenlauncher.zenmode.recap.RecapActivity
import com.zenlauncher.zenmode.recap.RecapReport
import com.zenlauncher.zenmode.recap.RecapStore
import com.zenlauncher.zenmode.recap.WeeklyRecap
import com.zenlauncher.zenmode.recap.WeeklyReportsSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
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
import com.zenlauncher.zenmode.ui.components.zenOverlayBlur
import com.zenlauncher.zenmode.ui.screens.HomeAppsPickerOverlay
import com.zenlauncher.zenmode.ui.screens.SettingsScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private var notificationBadgesEnabled by mutableStateOf(false)
    private var weeklyReports by mutableStateOf<List<WeeklyRecap>>(emptyList())
    private var downloadingWeek by mutableStateOf<LocalDate?>(null)
    private var showProSheet by mutableStateOf(false)

    /** Android 9 has no MediaStore Downloads; the user picks where the PDF goes. */
    private var pendingPickerWeek: LocalDate? = null
    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val week = pendingPickerWeek ?: return@registerForActivityResult
        pendingPickerWeek = null
        if (uri != null) writeReport(week) { recap -> contentResolver.openOutputStream(uri)?.use { RecapReport.write(this, recap, it) }; uri }
    }
    private var contentBlockingOn by mutableStateOf(false)
    private var offers by mutableStateOf<List<PlanOffer>>(emptyList())
    private var homeAppsChosenCount by mutableStateOf(0)
    private var showHomeAppsPicker by mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        // These are changed outside this screen (system settings or a sheet), so re-read them.
        notificationBadgesEnabled = ZenNotificationListenerService.isEnabledInSettings(this)
        lifecycleScope.launch {
            weeklyReports = withContext(Dispatchers.IO) { RecapStore(applicationContext).completedWeeks() }
            ServiceLocator.proEntitlementProvider.refresh()
        }
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
        contentBlockingOn = ContentBlockPrefs.isAnyBlockEnabled(this)
        if (savedInstanceState == null) {
            ServiceLocator.analyticsManager.trackEvent(
                "settings_viewed",
                mapOf("tier" to entitlements.entitlement.value.status.name.lowercase())
            )
        }

        setContent {
            ZenTheme(darkTheme = ThemePreferences.isDarkMode(this@SettingsActivity)) {
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
                    weeklyReports = {
                        WeeklyReportsSection(
                            reports = weeklyReports,
                            isPro = ProAccess.isProState(this@SettingsActivity),
                            downloadingWeek = downloadingWeek,
                            onOpen = { week ->
                                startActivity(RecapActivity.intent(this@SettingsActivity, week, RecapActivity.SOURCE_SETTINGS))
                            },
                            onDownload = ::downloadReport,
                            onUnlockPro = {
                                ServiceLocator.analyticsTracker.trackProUpsellViewed("settings_reports")
                                showProSheet = true
                            }
                        )
                    },
                    modifier = Modifier.zenOverlayBlur(showHomeAppsPicker)
                )
                if (showProSheet) {
                    ProUpsellSheet(
                        onDismiss = { showProSheet = false },
                        onRequestAccess = {
                            showProSheet = false
                            requestProAccess()
                        },
                        onEnableForTesting = if (ProAccess.canUseDebugOverride) {
                            {
                                ProAccess.setDebugOverride(this@SettingsActivity, true)
                                showProSheet = false
                            }
                        } else null
                    )
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

    private fun downloadReport(week: LocalDate) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val recap = weeklyReports.firstOrNull { it.weekStart == week } ?: return
            pendingPickerWeek = week
            createDocument.launch(RecapReport.fileName(recap))
            return
        }
        writeReport(week) { recap -> RecapReport.saveToDownloads(this, recap) }
    }

    /** Runs [save] off the main thread, then offers to open the saved PDF. */
    private fun writeReport(week: LocalDate, save: (WeeklyRecap) -> Uri?) {
        val recap = weeklyReports.firstOrNull { it.weekStart == week } ?: return
        downloadingWeek = week
        lifecycleScope.launch {
            val uri = withContext(Dispatchers.IO) { runCatching { save(recap) }.getOrNull() }
            downloadingWeek = null
            if (uri == null) {
                Toast.makeText(this@SettingsActivity, "Couldn't save the report. Please try again.", Toast.LENGTH_LONG).show()
                return@launch
            }
            ServiceLocator.analyticsTracker.trackReportDownloaded(week.toString())
            val where = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) " to Downloads" else ""
            Toast.makeText(this@SettingsActivity, "Report saved$where", Toast.LENGTH_SHORT).show()
            openPdf(uri)
        }
    }

    private fun openPdf(uri: Uri) {
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(view)
        } catch (_: android.content.ActivityNotFoundException) {
            // No PDF viewer installed; the file is still in Downloads.
        }
    }

    private fun requestProAccess() {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${AppConstants.SUPPORT_EMAIL}"))
            .putExtra(Intent.EXTRA_SUBJECT, "ZenMode PRO early access")
            .putExtra(Intent.EXTRA_TEXT, "Hi ZenMode team, I'd love early access to ZenMode PRO.")
        try {
            startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.TELEGRAM_URL)))
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
