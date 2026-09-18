/*
 * ZenMode - A local screen-time blocker and digital wellness app.
 * Copyright (C) 2026 Thammana Srinivas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zenlauncher.zenmode

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.zenlauncher.zenmode.coreapi.UsageAccess
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.AccessibilityDisclosureScreen
import com.zenlauncher.zenmode.ui.screens.AccountabilityScreen
import com.zenlauncher.zenmode.ui.screens.BuddyAddResult
import com.zenlauncher.zenmode.ui.screens.ForceUpdateDialog
import com.zenlauncher.zenmode.ui.components.zenOverlayBlur
import com.zenlauncher.zenmode.ui.screens.HomeAppActionsOverlay
import com.zenlauncher.zenmode.ui.screens.HomeAppsPickerOverlay
import com.zenlauncher.zenmode.ui.screens.HomeScreen
import com.zenlauncher.zenmode.ui.screens.ZenBroConnectScreen
import com.zenlauncher.zenmode.ui.screens.ZenBroConnectedScreen
import com.zenlauncher.zenmode.ui.screens.ZenCircleMember
import com.zenlauncher.zenmode.ui.screens.ZenCircleScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import com.zenlauncher.zenmode.onboarding.EnteringZenModeScreen
import com.zenlauncher.zenmode.recap.RecapActivity
import com.zenlauncher.zenmode.recap.RecapStore
import com.zenlauncher.zenmode.ui.theme.ZenTheme

/** Which page of the Zen Bro connect flow is showing. */
private sealed interface ZenBroStage {
    data object Connect : ZenBroStage
    data class Connected(val buddyName: String) : ZenBroStage
    data class Circle(val buddyName: String) : ZenBroStage
}

class MainActivity : AppCompatActivity() {
    private var isReceiverRegistered = false
    private lateinit var viewModel: MainViewModel
    private lateinit var repository: UsageRepository

    private var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
    // Same reasoning as installedApps: a plain `remember` here only reads the pref
    // once at first composition, so changing "apps on home screen" in Settings and
    // pressing back wouldn't take effect until the process restarted — MainActivity
    // is singleTask, so returning from Settings resumes it rather than recreating it.
    private var homeAppCount by mutableStateOf(AppGridPreferences.DEFAULT_APP_COUNT)
    private var showSearch by mutableStateOf(false)
    private var longPressedApp by mutableStateOf<AppInfo?>(null)
    private var showHomeAppsPicker by mutableStateOf(false)
    // The app whose actions opened the picker, so back returns to them.
    private var pickerReturnApp: AppInfo? = null
    private var showBuddyConnect by mutableStateOf(false)
    private var showEnteringZenMode by mutableStateOf(false)
    // Set when a connect succeeds; swaps My Zen Circle for the "You're Zen Bros now" screen.
    private var connectedBuddyName by mutableStateOf<String?>(null)
    // "Maybe later" on the connected screen, or the buddy card on home, opens the circle dashboard.
    private var showZenCircle by mutableStateOf(false)
    // Buddy's display name when the dashboard is opened from home (no connect flow to carry it).
    private var circleBuddyName by mutableStateOf<String?>(null)
    // True while "Remove buddy" / "Leave Circle" is waiting on the server.
    private var removingBuddy by mutableStateOf(false)
    private var showBuddyBattle by mutableStateOf(false)
    private var showAccessibilityDisclosure by mutableStateOf(false)
    private lateinit var accountabilityViewModel: AccountabilityViewModel
    private val buddyConnector by lazy {
        BuddyConnector(this, repository) { viewModel.fetchBuddyData() }
    }

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_USER_PRESENT -> {
                    viewModel.onScreenUnlocked()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    viewModel.onScreenLocked()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home button pressed while already on launcher — dismiss search and home-app overlays
        showSearch = false
        longPressedApp = null
        showHomeAppsPicker = false
        if (intent.getBooleanExtra("SHOW_BUDDY_CONNECT", false)) {
            showBuddyConnect = true
        }
        if (intent.getBooleanExtra("SHOW_BUDDY_BATTLE", false)) {
            showBuddyBattle = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.onResumeCheck()
            viewModel.refreshBuddyStatsFromCache()
        }
        checkAndStartDoomMonitor()
        loadInstalledApps()
        homeAppCount = AppGridPreferences.getAppCount(this)
        openUnseenRecap()

        // Stats Sync Check
        if (::repository.isInitialized) {
            val lastProcessed = repository.getLastStatsProcessedTime()
            val now = System.currentTimeMillis()
            val interval = AppConstants.STATS_SYNC_INTERVAL_MINUTES * 60 * 1000L

            // StatSyncWorker ships in zenmode_core_private; core-mock builds don't have it.
            val workerClass = statSyncWorkerClass()
            if (workerClass != null && now - lastProcessed > interval) {
                val syncRequest = androidx.work.OneTimeWorkRequest.Builder(workerClass)
                    .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                    .build()
                androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
                    "ManualStatSync",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
                repository.updateLastStatsProcessedTime(now)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun statSyncWorkerClass(): Class<out androidx.work.ListenableWorker>? = try {
        Class.forName("com.zenlauncher.zenmode.internal.StatSyncWorker") as Class<out androidx.work.ListenableWorker>
    } catch (e: ClassNotFoundException) {
        null
    }

    private fun checkAndStartDoomMonitor() {
        if (DoomScrollingMonitorService.isRunning) return

        val hasUsageStats = UsageAccess.isGranted(this)
        val hasOverlayPermission = Settings.canDrawOverlays(this)

        if (hasUsageStats && hasOverlayPermission) {
            try {
                val intent = Intent(this, DoomScrollingMonitorService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN, null)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                val activities = packageManager.queryIntentActivities(intent, 0)
                val pinnedPackages = repository.getPinnedApps()
                val pinnedSet = pinnedPackages.toSet()

                val allApps = activities.map { resolveInfo ->
                    val pkg = resolveInfo.activityInfo.packageName
                    AppInfo(
                        label = resolveInfo.loadLabel(packageManager),
                        packageName = pkg,
                        icon = resolveInfo.loadIcon(packageManager),
                        isPinned = pkg in pinnedSet
                    )
                }.distinctBy { it.packageName.toString() }

                val appsByPackage = allApps.associateBy { it.packageName.toString() }
                val pinned = pinnedPackages.mapNotNull { appsByPackage[it] }
                val unpinned = allApps.filter { it.packageName.toString() !in pinnedSet }
                    .sortedBy { it.label.toString() }

                pinned + unpinned
            }
            installedApps = result
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)


        // Initialize ViewModel
        val analyticsManager = ServiceLocator.analyticsManager
        repository = UsageRepository(applicationContext, analyticsManager)
        val factory = MainViewModelFactory(repository) {
            ResistancePreferences.isEnabled(applicationContext)
        }
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        accountabilityViewModel = ViewModelProvider(
            this, AccountabilityViewModelFactory(repository)
        )[AccountabilityViewModel::class.java]

        // New users, and v2 users who haven't seen the ZenMode OS revamp yet.
        if (!repository.isOnboardingComplete() || !repository.isOsOnboardingComplete()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        showEnteringZenMode = repository.isEnteringCelebrationPending()
        // Don't stack the system prompt over the celebration; it asks once that ends.
        if (!showEnteringZenMode) requestPostNotificationsIfNeeded()

        // Disable back button since this is a launcher home screen
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing — home screen is the root
            }
        })

        // Register Receiver
        val filter = android.content.IntentFilter()
        filter.addAction(Intent.ACTION_USER_PRESENT)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        androidx.core.content.ContextCompat.registerReceiver(
            this, screenReceiver, filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )
        isReceiverRegistered = true

        // Observe delayed unlock navigation (non-Compose, stays as LiveData observer)
        viewModel.navigateToDelayedUnlock.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                val delayedIntent = Intent(this, DelayedUnlockActivity::class.java)
                delayedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(delayedIntent)
                viewModel.onDelayedUnlockNavigated()
            }
        }

        // Fetch buddy data
        viewModel.fetchBuddyData()

        // Load apps initially
        loadInstalledApps()
        homeAppCount = AppGridPreferences.getAppCount(this)

        // Handle cold-start intents
        if (intent.getBooleanExtra("SHOW_BUDDY_CONNECT", false)) {
            showBuddyConnect = true
        }
        if (intent.getBooleanExtra("SHOW_BUDDY_BATTLE", false)) {
            showBuddyBattle = true
        }

        setContent {
            ZenTheme(darkTheme = ThemePreferences.isDarkMode(this@MainActivity)) {
                val usage by viewModel.stats.observeAsState()
                val usagePermissionMissing by viewModel.usagePermissionMissing.observeAsState(initial = false)
                val yesterdayChangePercent by viewModel.yesterdayChangePercent.observeAsState()
                val hasBuddies by viewModel.hasBuddies.observeAsState(initial = false)
                val buddyStats by viewModel.buddyStats.observeAsState()
                val myLikes by viewModel.myLikes.observeAsState(initial = 0L)
                val buddyLikes by viewModel.buddyLikes.observeAsState(initial = 0L)
                val likeToast by viewModel.likeToast.observeAsState()
                val isSignedIn = remember { ServiceLocator.authProvider.isSignedIn() }
                val userCode = remember {
                    repository.getUserUid()
                        ?: ServiceLocator.authProvider.getCurrentUserId()
                }
                val accountabilityUiState by accountabilityViewModel.uiState.observeAsState(AccountabilityUiState())
                val showForceUpdate by viewModel.showForceUpdateDialog.collectAsState(initial = false)

                if (showForceUpdate) {
                    ForceUpdateDialog(
                        onUpdateClick = {
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                            } catch (e: android.content.ActivityNotFoundException) {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                            }
                        },
                        onTomorrowClick = { viewModel.snoozeForceUpdateUntilTomorrow() }
                    )
                }

                // Reload accountability data whenever the overlay is opened
                androidx.compose.runtime.LaunchedEffect(showBuddyBattle) {
                    if (showBuddyBattle) accountabilityViewModel.reload()
                }

                // Show like toast messages
                androidx.compose.runtime.LaunchedEffect(likeToast) {
                    likeToast?.let { msg ->
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            msg,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        viewModel.clearLikeToast()
                    }
                }

                // Handle disconnect result
                androidx.compose.runtime.LaunchedEffect(accountabilityUiState.disconnectResult) {
                    when (val result = accountabilityUiState.disconnectResult) {
                        is DisconnectResult.Success -> {
                            removingBuddy = false
                            showBuddyBattle = false
                            // Back to the start of the connect flow, ready to find a new Zen Bro.
                            closeBuddyConnect()
                            showBuddyConnect = true
                            viewModel.refreshBuddyStatsFromCache()
                            accountabilityViewModel.resetDisconnectResult()
                        }
                        is DisconnectResult.Error -> {
                            removingBuddy = false
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                "Failed to disconnect: ${result.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            accountabilityViewModel.resetDisconnectResult()
                        }
                        null -> Unit
                    }
                }

                val weeklyMillis = remember { repository.getWeeklyScreenTimeMillis() }
                val streakCount = remember { AppLogic.getStreakCount(weeklyMillis) }

                HomeScreen(
                    usage = usage,
                    streaks = streakCount,
                    yesterdayChangePercent = yesterdayChangePercent,
                    hasBuddies = hasBuddies,
                    buddyStats = buddyStats,
                    isSignedIn = isSignedIn,
                    showSearch = showSearch,
                    zenScore = AppConstants.PLACEHOLDER_ZEN_SCORE,
                    goldInvested = AppConstants.PLACEHOLDER_GOLD_INVESTED,
                    goldChangePercent = AppConstants.PLACEHOLDER_GOLD_CHANGE_PERCENT,
                    appCount = homeAppCount,
                    myLikes = myLikes,
                    buddyLikes = buddyLikes,
                    onLikeClick = { viewModel.sendLike() },
                    onShowSearchChange = { showSearch = it },
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onZenGoldClick = {
                        startActivity(Intent(this, ZenGoldActivity::class.java))
                    },
                    onZenScoreClick = {
                        startActivity(Intent(this, ZenScoreActivity::class.java))
                    },
                    onGoogleSearch = { query ->
                        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                            putExtra(android.app.SearchManager.QUERY, query)
                        }
                        if (searchIntent.resolveActivity(packageManager) != null) {
                            startActivity(searchIntent)
                        } else {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")))
                        }
                    },
                    onPhoneClick = {
                        startActivity(Intent(Intent.ACTION_DIAL))
                    },
                    onLockClick = {
                        lockScreen()
                    },
                    onInviteBuddyClick = {
                        ServiceLocator.analyticsTracker.trackBuddyShareStarted("manual")
                        showBuddyConnect = true
                    },
                    onBuddyCardClick = if (hasBuddies) {
                        { openZenCircleFromHome() }
                    } else null,
                    onSignInClick = {
                        val intent = OnboardingActivity.signInIntent(this).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    },
                    onAppClick = { appInfo ->
                        val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName.toString())
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        }
                    },
                    onAppLongClick = { longPressedApp = it },
                    modifier = Modifier.zenOverlayBlur(longPressedApp != null || showHomeAppsPicker),
                    apps = run {
                        val notifCounts = ZenNotificationListenerService.notificationCounts
                        installedApps.map { app ->
                            val count = notifCounts[app.packageName.toString()] ?: 0
                            if (count != app.notificationCount) app.copy(notificationCount = count) else app
                        }
                    }
                )

                // Re-grant banner: only when usage access was revoked (e.g. MIUI auto-revoke).
                if (usagePermissionMissing) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        com.zenlauncher.zenmode.ui.screens.UsageAccessBanner(
                            onGrantClick = {
                                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(16.dp)
                        )
                    }
                }

                // Long-press on a home app: frosted actions, then the frosted home-apps picker.
                HomeAppActionsOverlay(
                    app = longPressedApp,
                    position = installedApps.take(homeAppCount)
                        .indexOfFirst { it.packageName == longPressedApp?.packageName } + 1,
                    appCount = homeAppCount,
                    onChangeHomeApps = {
                        pickerReturnApp = longPressedApp
                        longPressedApp = null
                        showHomeAppsPicker = true
                    },
                    onAppInfo = { appInfo ->
                        longPressedApp = null
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:${appInfo.packageName}"))
                        )
                    },
                    onDismiss = { longPressedApp = null }
                )
                HomeAppsPickerOverlay(
                    visible = showHomeAppsPicker,
                    limit = homeAppCount,
                    initialSelection = repository.getPinnedApps(),
                    onSelectionChange = { packages ->
                        // Home apps reuse the pinned-apps list; home reorders live behind the blur.
                        repository.savePinnedApps(packages)
                        loadInstalledApps()
                    },
                    onDismiss = {
                        showHomeAppsPicker = false
                        pickerReturnApp = null
                    },
                    onBack = {
                        showHomeAppsPicker = false
                        longPressedApp = pickerReturnApp
                        pickerReturnApp = null
                    }
                )

                // Accessibility disclosure full-screen
                if (showAccessibilityDisclosure) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AccessibilityDisclosureScreen(
                            onAccept = {
                                showAccessibilityDisclosure = false
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onDecline = {
                                showAccessibilityDisclosure = false
                            }
                        )
                    }
                }

                // Zen Buddy Battle summary overlay
                if (showBuddyBattle) {
                    val accMyLikes by accountabilityViewModel.myLikes.observeAsState(initial = 0L)
                    val accBuddyLikes by accountabilityViewModel.buddyLikes.observeAsState(initial = 0L)
                    val accLikeToast by accountabilityViewModel.likeToast.observeAsState()
                    androidx.compose.runtime.LaunchedEffect(accLikeToast) {
                        accLikeToast?.let { msg ->
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                msg,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            accountabilityViewModel.clearLikeToast()
                        }
                    }
                    AccountabilityScreen(
                        uiState = accountabilityUiState,
                        onBackClick = { showBuddyBattle = false },
                        onCopyCode = { code ->
                            val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ZenMode Code", code))
                            android.widget.Toast.makeText(this@MainActivity, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onBackToHomeClick = { showBuddyBattle = false },
                        onChangeBuddyConfirmed = { accountabilityViewModel.disconnectBuddy() },
                        myLikes = accMyLikes,
                        buddyLikes = accBuddyLikes,
                        onLikeClick = { accountabilityViewModel.sendLike() }
                    )
                }

                // Zen Bro connect flow: My Zen Circle options → "You're Zen Bros now" → circle dashboard
                if (showBuddyConnect) {
                    val stage = when {
                        showZenCircle -> ZenBroStage.Circle(connectedBuddyName ?: circleBuddyName ?: "Zen Bro")
                        connectedBuddyName != null -> ZenBroStage.Connected(connectedBuddyName!!)
                        else -> ZenBroStage.Connect
                    }
                    AnimatedContent(
                        targetState = stage,
                        contentKey = { it::class },
                        transitionSpec = {
                            (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.96f)) togetherWith fadeOut(tween(200))
                        },
                        label = "zenBroConnectFlow"
                    ) { current ->
                        when (current) {
                            is ZenBroStage.Circle -> ZenCircleScreen(
                                members = listOf(
                                    ZenCircleMember(
                                        name = "You",
                                        isYou = true,
                                        screenTimeMinutes = (usage?.screenTimeInMillis ?: 0L) / 60_000,
                                        zenScore = AppConstants.PLACEHOLDER_ZEN_SCORE,
                                        streaks = streakCount,
                                        changePercent = yesterdayChangePercent
                                    ),
                                    ZenCircleMember(
                                        name = current.buddyName,
                                        isYou = false,
                                        screenTimeMinutes = buddyStats?.screenTimeMins ?: 0L,
                                        zenScore = AppConstants.PLACEHOLDER_BUDDY_ZEN_SCORE,
                                        streaks = AppConstants.PLACEHOLDER_BUDDY_STREAK
                                    )
                                ),
                                userCode = userCode,
                                // Opened from home there's no connected screen to return to.
                                onBackClick = { if (connectedBuddyName != null) showZenCircle = false else closeBuddyConnect() },
                                onShareInviteLink = { userCode?.let { buddyConnector.shareBuddyInvite(it) } },
                                onCopyInviteCode = { userCode?.let { buddyConnector.copyUserCode(it, showToast = false) } },
                                onBackToHome = { closeBuddyConnect() },
                                onSendLove = { viewModel.sendLike() },
                                onSendMelt = {
                                    Toast.makeText(this@MainActivity, "Melt reactions are coming soon", Toast.LENGTH_SHORT).show()
                                },
                                onWeeklyClick = {
                                    Toast.makeText(this@MainActivity, "Weekly rankings are part of PRO", Toast.LENGTH_SHORT).show()
                                },
                                removingBuddy = removingBuddy,
                                onRemoveBuddy = { removeBuddy() },
                                onLeaveCircle = { removeBuddy() }
                            )
                            is ZenBroStage.Connected -> ZenBroConnectedScreen(
                                buddyName = current.buddyName,
                                usage = usage,
                                streaks = streakCount,
                                zenScore = AppConstants.PLACEHOLDER_ZEN_SCORE,
                                buddyStats = buddyStats,
                                userCode = userCode,
                                onBackClick = { closeBuddyConnect() },
                                onShareInviteLink = { userCode?.let { buddyConnector.shareBuddyInvite(it) } },
                                // The sheet confirms inline, so no toast here.
                                onCopyInviteCode = { userCode?.let { buddyConnector.copyUserCode(it, showToast = false) } },
                                onMaybeLater = { showZenCircle = true }
                            )
                            ZenBroStage.Connect -> ZenBroConnectScreen(
                                userCode = userCode,
                                onBackClick = { showBuddyConnect = false },
                                onShareLink = { userCode?.let { buddyConnector.shareBuddyInvite(it) } },
                                onCopyCode = { userCode?.let { buddyConnector.copyUserCode(it, showToast = true) } },
                                onAddBuddy = { targetUid ->
                                    buddyConnector.addBuddy(targetUid).also { result ->
                                        if (result is BuddyAddResult.Success) {
                                            // Let "Connected with …" register before the celebration takes over.
                                            lifecycleScope.launch {
                                                kotlinx.coroutines.delay(700)
                                                connectedBuddyName = result.buddyName
                                            }
                                        }
                                    }
                                },
                                onRandomConnect = {
                                    lifecycleScope.launch {
                                        buddyConnector.randomConnect()?.let { connectedBuddyName = it }
                                    }
                                }
                            )
                        }
                    }
                }

                // "Entering ZenMode", once, right after onboarding — shown here because
                // granting the home role relaunches this activity on top of onboarding.
                if (showEnteringZenMode) {
                    EnteringZenModeScreen(onFinished = {
                        repository.setEnteringCelebrationPending(false)
                        showEnteringZenMode = false
                        requestPostNotificationsIfNeeded()
                    })
                }
            }
        }
    }

    /**
     * The first home visit after a week's recap is announced plays it — the notification
     * may have been missed or blocked. Once opened it's marked seen and won't reappear.
     */
    private fun openUnseenRecap() {
        if (showEnteringZenMode || !::repository.isInitialized) return
        lifecycleScope.launch {
            val week = withContext(Dispatchers.IO) { RecapStore(applicationContext).unseenWeek() } ?: return@launch
            if (showEnteringZenMode) return@launch
            startActivity(RecapActivity.intent(this@MainActivity, week.weekStart, RecapActivity.SOURCE_HOME))
        }
    }

    private fun closeBuddyConnect() {
        showBuddyConnect = false
        connectedBuddyName = null
        showZenCircle = false
        circleBuddyName = null
    }

    /** Home's buddy card: straight to the circle dashboard, fetching the buddy's name alongside. */
    private fun openZenCircleFromHome() {
        connectedBuddyName = null
        showZenCircle = true
        showBuddyConnect = true
        val buddyUid = repository.getBuddyUid() ?: return
        lifecycleScope.launch {
            circleBuddyName = runCatching { ServiceLocator.firestoreDataSource.getUser(buddyUid)?.displayName }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        }
    }

    /**
     * "Remove buddy" and "Leave Circle" — with one buddy per circle today, both end the
     * relationship. The result lands in the DisconnectResult effect above.
     */
    private fun removeBuddy() {
        if (removingBuddy) return
        // Same preconditions disconnectBuddy() checks; without them it returns silently and
        // the confirm button would sit on "Removing…" forever.
        val signedIn = (repository.getUserUid() ?: ServiceLocator.authProvider.getCurrentUserId()) != null
        if (!signedIn || repository.getBuddyUid() == null) {
            Toast.makeText(this, "You don\u2019t have a Zen Bro to remove.", Toast.LENGTH_SHORT).show()
            return
        }
        removingBuddy = true
        accountabilityViewModel.disconnectBuddy()
    }

    private fun requestPostNotificationsIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }

    private fun lockScreen() {
        if (ZenAccessibilityService.isRunning()) {
            ZenAccessibilityService.lockScreen()
        } else if (ZenAccessibilityService.isEnabledInSettings(this)) {
            android.widget.Toast.makeText(this, "Accessibility service is reconnecting, please try again", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            showAccessibilityDisclosure = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            unregisterReceiver(screenReceiver)
        }
    }
}
