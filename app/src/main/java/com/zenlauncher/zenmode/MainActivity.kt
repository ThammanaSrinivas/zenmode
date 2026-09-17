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
import android.provider.Settings
import android.widget.Toast
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.zenlauncher.zenmode.coreapi.UsageAccess
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.AccessibilityDisclosureScreen
import com.zenlauncher.zenmode.ui.screens.AccountabilityScreen
import com.zenlauncher.zenmode.ui.screens.BuddyAddResult
import com.zenlauncher.zenmode.ui.screens.ForceUpdateDialog
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
    private var showBuddyConnect by mutableStateOf(false)
    // Set when a connect succeeds; swaps My Zen Circle for the "You're Zen Bros now" screen.
    private var connectedBuddyName by mutableStateOf<String?>(null)
    // "Maybe later" on the connected screen, or the buddy card on home, opens the circle dashboard.
    private var showZenCircle by mutableStateOf(false)
    // Buddy's display name when the dashboard is opened from home (no connect flow to carry it).
    private var circleBuddyName by mutableStateOf<String?>(null)
    // Pending "Connected with …" → celebration handoff; cancelled if the user backs out early
    // so it can't set connectedBuddyName after the flow has already been closed.
    private var connectSuccessJob: Job? = null
    // True while "Remove buddy" / "Leave Circle" is waiting on the server.
    private var removingBuddy by mutableStateOf(false)
    private var showBuddyBattle by mutableStateOf(false)
    private var showAccessibilityDisclosure by mutableStateOf(false)
    private lateinit var accountabilityViewModel: AccountabilityViewModel

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
        // Home button pressed while already on launcher — dismiss search overlay
        showSearch = false
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

        requestPostNotificationsIfNeeded()

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

        // for testing onboarding just remove !
        if (!repository.isOnboardingComplete()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

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
                        val intent = Intent(this, OnboardingActivity::class.java).apply {
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
                    onAppLongClick = { appInfo ->
                        val pinned = repository.getPinnedApps()
                        val isCurrentlyPinned = pinned.contains(appInfo.packageName.toString())
                        val toggled = repository.togglePinnedApp(appInfo.packageName.toString())
                        if (!isCurrentlyPinned && !toggled) {
                            Toast.makeText(
                                this,
                                "Only ${UsageRepository.MAX_PINNED_APPS} apps can be pinned",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        loadInstalledApps()
                    },
                    onAppInfoClick = { appInfo ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${appInfo.packageName}")
                        }
                        startActivity(intent)
                    },
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
                                onShareInviteLink = { userCode?.let { shareBuddyInvite(it) } },
                                onCopyInviteCode = { userCode?.let { copyUserCode(it, showToast = false) } },
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
                                onShareInviteLink = { userCode?.let { shareBuddyInvite(it) } },
                                // The sheet confirms inline, so no toast here.
                                onCopyInviteCode = { userCode?.let { copyUserCode(it, showToast = false) } },
                                onMaybeLater = { showZenCircle = true }
                            )
                            ZenBroStage.Connect -> ZenBroConnectScreen(
                                userCode = userCode,
                                onBackClick = { closeBuddyConnect() },
                                onShareLink = { userCode?.let { shareBuddyInvite(it) } },
                                onCopyCode = { userCode?.let { copyUserCode(it, showToast = true) } },
                                onAddBuddy = { targetUid ->
                                    addBuddy(targetUid).also { result ->
                                        if (result is BuddyAddResult.Success) {
                                            // Let "Connected with …" register before the celebration takes over.
                                            connectSuccessJob = lifecycleScope.launch {
                                                kotlinx.coroutines.delay(700)
                                                connectedBuddyName = result.buddyName
                                            }
                                        }
                                    }
                                },
                                onRandomConnect = {
                                    lifecycleScope.launch {
                                        randomConnect()?.let { connectedBuddyName = it }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun copyUserCode(code: String, showToast: Boolean) {
        val clipboard = getSystemService(android.content.ClipboardManager::class.java)
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ZenMode Code", code))
        ServiceLocator.analyticsTracker.trackBuddyCodeCopied("manual")
        if (showToast) {
            android.widget.Toast.makeText(this, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeBuddyConnect() {
        connectSuccessJob?.cancel()
        connectSuccessJob = null
        showBuddyConnect = false
        connectedBuddyName = null
        showZenCircle = false
        circleBuddyName = null
    }

    /** Home's buddy card: straight to the circle dashboard, fetching the buddy's name alongside. */
    private fun openZenCircleFromHome() {
        connectSuccessJob?.cancel()
        connectSuccessJob = null
        connectedBuddyName = null
        circleBuddyName = null
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

    /** "Share a link": the Play Store link plus this user's code, via the system share sheet. */
    private fun shareBuddyInvite(code: String) {
        ServiceLocator.analyticsTracker.trackBuddyShareStarted("link")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Be my Zen Bro on ZenMode")
            putExtra(
                Intent.EXTRA_TEXT,
                "Be my Zen Bro on ZenMode! Get the app: " +
                    "https://play.google.com/store/apps/details?id=$packageName\n" +
                    "Then paste my Zen code in My Zen Circle: $code"
            )
        }
        startActivity(Intent.createChooser(intent, "Share invite"))
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

    private suspend fun addBuddy(targetUid: String): BuddyAddResult {
        val currentUserId = ServiceLocator.authProvider.getCurrentUserId()

        if (targetUid == currentUserId) return BuddyAddResult.SelfAdd

        ServiceLocator.analyticsTracker.trackBuddyCodePasted("manual")
        
        // Check network connectivity
        val connectivityManager = getSystemService(android.net.ConnectivityManager::class.java)
        val activeNetwork = connectivityManager?.activeNetwork
        val networkCapabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        val isConnected = networkCapabilities?.hasCapability(
            android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) == true

        if (!isConnected) {
            return BuddyAddResult.Error("No internet connection. Please check your network and try again.")
        }

        val firestoreDataSource = ServiceLocator.firestoreDataSource
        return try {
            val user = firestoreDataSource.getUser(targetUid)
                ?: return BuddyAddResult.Error("User ID not found. Please check the ID and try again.")

            val myUid = currentUserId
                ?: return BuddyAddResult.Error("Not signed in.")

            if (firestoreDataSource.checkRelationshipExists(myUid, targetUid)) {
                return BuddyAddResult.AlreadyBuddies(user.displayName)
            }

            firestoreDataSource.sendBuddyInvite(myUid, targetUid)
            repository.clearCachedBuddy()
            viewModel.fetchBuddyData()
            ServiceLocator.analyticsTracker.trackBuddyConnected("manual")

            BuddyAddResult.Success(user.displayName)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            BuddyAddResult.Error("Connection timed out. Please check your network and try again.")
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("offline", ignoreCase = true) == true ->
                    "Unable to connect. Please check your internet and try again."
                else -> "Failed: ${e.message}"
            }
            BuddyAddResult.Error(errorMessage)
        }
    }

    /** Returns the new buddy's display name on success, null otherwise (with a toast). */
    private suspend fun randomConnect(): String? {
        val currentUserId = ServiceLocator.authProvider.getCurrentUserId()
        if (currentUserId == null) {
            android.widget.Toast.makeText(this, "Not signed in.", android.widget.Toast.LENGTH_SHORT).show()
            return null
        }

        val connectivityManager = getSystemService(android.net.ConnectivityManager::class.java)
        val isConnected = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!isConnected) {
            android.widget.Toast.makeText(this, "No internet connection.", android.widget.Toast.LENGTH_SHORT).show()
            return null
        }

        // Cooldown: only allow retrying after cooldown if last attempt found no buddy
        val lastTried = repository.getLastRandomConnectAttemptTime()
        val remaining = AppConstants.RANDOM_CONNECT_COOLDOWN_MS - (System.currentTimeMillis() - lastTried)
        if (remaining > 0) {
            val secs = (remaining / 1000).coerceAtLeast(1)
            android.widget.Toast.makeText(
                this,
                "No buddies were available last time. Try again in ${secs}s.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return null
        }

        return try {
            val buddyUid = ServiceLocator.firestoreDataSource.findRandomBuddy(currentUserId)
            if (buddyUid == null) {
                repository.saveLastRandomConnectAttemptTime(System.currentTimeMillis())
                android.widget.Toast.makeText(
                    this,
                    "No buddies available right now. Try again in 30 seconds!",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                null
            } else {
                val buddy = ServiceLocator.firestoreDataSource.getUser(buddyUid)
                repository.clearCachedBuddy()
                repository.saveHasBuddy(true)
                viewModel.fetchBuddyData()
                ServiceLocator.analyticsTracker.trackBuddyConnected("random")
                // No toast on success: the "You're Zen Bros now" screen says it.
                buddy?.displayName?.takeIf { it.isNotBlank() } ?: "your Zen Bro"
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            android.widget.Toast.makeText(this, "Connection timed out. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
            null
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Something went wrong. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            unregisterReceiver(screenReceiver)
        }
    }
}
