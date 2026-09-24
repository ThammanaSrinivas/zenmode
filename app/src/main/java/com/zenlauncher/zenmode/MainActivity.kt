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
import android.os.SystemClock
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.zenlauncher.zenmode.accessibility.A11yPermissionMonitor
import com.zenlauncher.zenmode.ui.components.HomeRevealCue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import com.zenlauncher.zenmode.ui.screens.CircleCodeResult
import com.zenlauncher.zenmode.ui.screens.ZenBroConnectScreen
import com.zenlauncher.zenmode.ui.screens.ZenBroConnectedScreen
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

/** A recognized zenmodeos.com App Link tap -- Buddy (/b/{code}) and Circle (/c/{circleId})
 * are separate invite mechanisms with separate landings, see MainActivity.parseDeepLink. */
private sealed interface DeepLink {
    data object None : DeepLink
    data class Buddy(val code: String) : DeepLink
    data class Circle(val circleId: String) : DeepLink
}

/** What to auto-fire once a circle-mode Share/Use-a-code tap's createCircle() completes. */
internal enum class PendingCircleAction { SHARE, COPY }

class MainActivity : AppCompatActivity() {
    private var isReceiverRegistered = false
    private lateinit var viewModel: MainViewModel
    internal lateinit var repository: UsageRepository

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
    internal var showBuddyConnect by mutableStateOf(false)
    private var showEnteringZenMode by mutableStateOf(false)
    // Set from a zenmodeos.com/b/{code} App Link tap; consumed once the Connect screen
    // is actually on-screen (see the LaunchedEffect next to ZenBroStage.Connect below).
    internal var pendingInviteCode by mutableStateOf<String?>(null)
    // Share/Use a code in circle mode create or join a real circle directly, then navigate
    // to the dashboard -- no separate setup/join screens. Set right before createCircle();
    // consumed by the justEnteredCircle LaunchedEffect below to fire the actual share/copy
    // once the circle exists.
    internal var pendingCircleAutoAction by mutableStateOf<PendingCircleAction?>(null)
    // Set when a connect succeeds; swaps My Zen Circle for the "You're Zen Bros now" screen.
    internal var connectedBuddyName by mutableStateOf<String?>(null)
    // "Maybe later" on the connected screen, or the buddy card on home, opens the circle dashboard.
    internal var showZenCircle by mutableStateOf(false)
    // Buddy's display name when the dashboard is opened from home (no connect flow to carry it).
    internal var circleBuddyName by mutableStateOf<String?>(null)
    // Pending "Connected with …" → celebration handoff; cancelled if the user backs out early
    // so it can't set connectedBuddyName after the flow has already been closed.
    internal var connectSuccessJob: Job? = null
    // True while "Remove buddy" / "Leave Circle" is waiting on the server.
    internal var removingBuddy by mutableStateOf(false)
    internal var showBuddyBattle by mutableStateOf(false)
    // First-time Zen Buddy -> Zen Circle choice, shown at most once (see BuddyFlowPreferences).
    internal var showBuddyFlowMigrationPrompt by mutableStateOf(false)
    private var showAccessibilityDisclosure by mutableStateOf(false)
    internal lateinit var accountabilityViewModel: AccountabilityViewModel
    internal val buddyConnector by lazy {
        BuddyConnector(this, repository) { viewModel.fetchBuddyData() }
    }
    internal lateinit var circleViewModel: CircleViewModel

    // Home's entrance plays once per unlock, and only when the unlock lands on Home: hidden
    // while the screen is off, played on unlock, settled if the unlock went somewhere else.
    private var homeRevealCue by mutableStateOf(HomeRevealCue())
    private var screenOffAt = 0L
    private var unlockedAt = 0L
    private var settleRevealJob: Job? = null
    private var revealAfterDelayedUnlock = false

    companion object {
        // Shared to avoid re-running the animation if the home screen restarts while
        // the device is unlocked, since ACTION_USER_PRESENT is only broadcast once.
        var lastScreenOffTime = 0L
        var lastSessionStartTrackTime = 0L
    }

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_USER_PRESENT -> {
                    viewModel.onScreenUnlocked()
                    unlockedAt = SystemClock.elapsedRealtime()
                    // The Delayed Unlock pause opens over Home; save the entrance for after it.
                    revealAfterDelayedUnlock = viewModel.navigateToDelayedUnlock.value == true
                    if (!revealAfterDelayedUnlock && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        playUnlockReveal()
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    viewModel.onScreenLocked()
                    hideHomeForUnlock()
                }
            }
        }
    }

    private fun hideHomeForUnlock() {
        screenOffAt = SystemClock.elapsedRealtime()
        revealAfterDelayedUnlock = false
        settleRevealJob?.cancel()
        homeRevealCue = homeRevealCue.hidden()
    }

    private fun playUnlockReveal() {
        if (homeRevealCue.phase != HomeRevealCue.Phase.Hidden) return
        settleRevealJob?.cancel()
        homeRevealCue = homeRevealCue.play()
    }

    /** Called on resume: decides what a hidden Home does now that it's on screen. */
    private fun resolveUnlockReveal() {
        if (homeRevealCue.phase != HomeRevealCue.Phase.Hidden) return
        val now = SystemClock.elapsedRealtime()
        when {
            // Back from the Delayed Unlock pause: let its exit transition finish first.
            revealAfterDelayedUnlock -> {
                revealAfterDelayedUnlock = false
                settleRevealJob?.cancel()
                settleRevealJob = lifecycleScope.launch {
                    delay(DELAYED_UNLOCK_EXIT_MS)
                    homeRevealCue = homeRevealCue.play()
                }
            }
            unlockedAt > screenOffAt && now - unlockedAt <= UNLOCK_REVEAL_WINDOW_MS -> playUnlockReveal()
            // Unlocked into another app a while ago; coming back to Home isn't an unlock.
            unlockedAt > screenOffAt -> homeRevealCue = homeRevealCue.settled()
            // Resumed just ahead of USER_PRESENT; wait for it, but never leave Home blank.
            else -> {
                settleRevealJob?.cancel()
                settleRevealJob = lifecycleScope.launch {
                    delay(UNLOCK_REVEAL_WINDOW_MS)
                    if (homeRevealCue.phase == HomeRevealCue.Phase.Hidden) homeRevealCue = homeRevealCue.settled()
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
        handleIntentDeepLink(intent)
    }

    /**
     * Parses a zenmodeos.com/{b,c}/{code} App Link tap. /b/ is the classic 1:1 Buddy invite,
     * /c/ the real multi-member Zen Circle invite -- distinct paths, distinct landings.
     */
    private fun parseDeepLink(intent: Intent): DeepLink {
        val data = intent.data ?: return DeepLink.None
        if (data.host != "zenmodeos.com") return DeepLink.None
        val code = data.lastPathSegment?.takeIf { it.isNotBlank() } ?: return DeepLink.None
        return when {
            data.path?.startsWith("/b/") == true -> DeepLink.Buddy(code)
            data.path?.startsWith("/c/") == true -> DeepLink.Circle(code)
            else -> DeepLink.None
        }
    }

    /**
     * Routes a cold-start or onNewIntent [intent] to the right flow: a Buddy link opens the
     * classic connect flow ([openBuddyFlow]). A Circle link auto-joins directly and heads
     * straight to the dashboard -- no separate confirm screen (joining a group isn't gated
     * by [BuddyFlowPreferences] the way "the buddy area" is). If the tapper already has a
     * classic Buddy, the join comes back AlreadyInCircle and a LaunchedEffect on
     * circleUiState.pendingBuddySwitchCircleId below tells them to disconnect it first --
     * simpler than the dedicated switch-confirmation dialog this used to show, see the
     * plan doc for the tradeoff. Falls back to the legacy boolean extras otherwise.
     */
    private fun handleIntentDeepLink(intent: Intent) {
        when (val link = parseDeepLink(intent)) {
            is DeepLink.Buddy -> {
                pendingInviteCode = link.code
                openBuddyFlow()
            }
            is DeepLink.Circle -> {
                closeBuddyConnect()
                circleViewModel.joinCircle(link.circleId, via = "invite_link")
                showZenCircle = true
                showBuddyConnect = true
            }
            DeepLink.None -> {
                // Both extras open "the buddy area" - which screen that resolves to is
                // openBuddyFlow()'s call, not the sender's (Settings, an old push notification).
                if (intent.getBooleanExtra("SHOW_BUDDY_CONNECT", false) ||
                    intent.getBooleanExtra("SHOW_BUDDY_BATTLE", false)
                ) {
                    openBuddyFlow()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Paused by the screen going off: hide now, while Home can still draw a frame, so the
        // wake-up shows a blank stage rather than a flash of the old Home before the reveal.
        if (!getSystemService(android.os.PowerManager::class.java).isInteractive) hideHomeForUnlock()
    }

    override fun onResume() {
        super.onResume()
        resolveUnlockReveal()
        A11yPermissionMonitor.check(this)
        if (::viewModel.isInitialized) {
            viewModel.onResumeCheck()
            viewModel.refreshBuddyStatsFromCache()
        }
        // Circle users have no periodic self-heal the way StatSyncWorker's onResume hook below
        // gives classic buddies (it skips circle users entirely -- see its own comment) and no
        // realtime listener either -- loadCircle() otherwise only runs once at ViewModel init
        // and on a circle_react push, so a member joining/leaving never reaches an already-open
        // app until this fires. Cheap: a single circle doc read, not the full StatSyncWorker.
        if (::circleViewModel.isInitialized) {
            circleViewModel.loadCircle()
        }
        loadInstalledApps()
        homeAppCount = AppGridPreferences.getAppCount(this)
        openUnseenRecap()

        // Stats Sync Check
        if (::repository.isInitialized) {
            val now = System.currentTimeMillis()
            if (now - lastSessionStartTrackTime > 30 * 60_000L) {
                lastSessionStartTrackTime = now
                val sessionNum = repository.incrementSessionNumber()
                ServiceLocator.analyticsTracker.trackSessionStart(sessionNum)
            }
            if (!repository.isDay1CheckinTracked()) {
                try {
                    val installTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime
                    val daysSinceInstall = ((now - installTime) / (1000 * 60 * 60 * 24)).toInt()
                    if (daysSinceInstall >= 1) {
                        ServiceLocator.analyticsTracker.trackDay1CheckinCompleted("home_opened")
                        repository.setDay1CheckinTracked(true)
                    }
                } catch (e: Exception) {}
            }
            val lastProcessed = repository.getLastStatsProcessedTime()
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

    private fun loadInstalledApps() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val activities = LauncherActivities.query(packageManager)
                val homeKeys = repository.getPinnedApps()
                val homeRank = homeKeys.withIndex().associate { (i, k) -> k to i }

                // ZenMode declares CATEGORY_LAUNCHER (so it's selectable as default home)
                // alongside CATEGORY_HOME, so it shows up in its own launcher query. Left in,
                // it lists itself in the drawer and in search; tapping it re-delivers an
                // intent to this already-running singleTask Activity via onNewIntent, which
                // resets showSearch/etc — the search overlay just vanishes, looking like the
                // app crashed. Same exclusion OnboardingViewModel.queryLaunchableApps applies.
                val allApps = activities
                    .filter { it.activityInfo.packageName != packageName }
                    .map { resolveInfo ->
                        AppInfo(
                            label = resolveInfo.loadLabel(packageManager),
                            packageName = resolveInfo.activityInfo.packageName,
                            icon = resolveInfo.loadIcon(packageManager),
                            activityClassName = resolveInfo.activityInfo.name,
                            key = LauncherActivities.selectionKey(resolveInfo, activities)
                        )
                    }
                    .sortedBy { it.label.toString() }

                // The picked home apps lead, in the order chosen; everything else stays A–Z.
                val (home, rest) = allApps.partition { it.key in homeRank }
                home.sortedBy { homeRank.getValue(it.key) } + rest
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
        val factory = MainViewModelFactory(repository, ZenScoreStore(applicationContext, repository)) {
            ResistancePreferences.isEnabled(applicationContext)
        }
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        accountabilityViewModel = ViewModelProvider(
            this, AccountabilityViewModelFactory(repository)
        )[AccountabilityViewModel::class.java]
        circleViewModel = ViewModelProvider(
            this, CircleViewModelFactory(repository)
        )[CircleViewModel::class.java]

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
        handleIntentDeepLink(intent)

        setContent {
            ZenTheme {
                val usage by viewModel.stats.observeAsState()
                val usagePermissionMissing by viewModel.usagePermissionMissing.observeAsState(initial = false)
                val yesterdayChangePercent by viewModel.yesterdayChangePercent.observeAsState()
                val zenScore by viewModel.zenScore.observeAsState(initial = 0)
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
                val circleUiState by circleViewModel.uiState.observeAsState(CircleUiState())
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
                            // Nothing classic left to preserve once the relationship is gone -
                            // any new buddy from here on goes through Zen Circle.
                            BuddyFlowPreferences.setDecision(this@MainActivity, BuddyFlow.ZEN_CIRCLE)
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

                // Same reasoning as the disconnect handler above -- without this, leaving a
                // real circle (with no classic buddy underneath) falls back to rendering a
                // placeholder "buddyName" member ("Zen Bro") that isn't a real person, instead
                // of navigating away.
                androidx.compose.runtime.LaunchedEffect(circleUiState.justLeftCircle) {
                    if (circleUiState.justLeftCircle) {
                        closeBuddyConnect()
                        showBuddyConnect = true
                        circleViewModel.consumeLeftCircleEvent()
                    }
                }

                val recapStore = remember { RecapStore(applicationContext) }
                val todayIsMindful = remember(zenScore) {
                    zenScore >= AppConstants.MINDFUL_DAY_ZEN_SCORE_THRESHOLD * 10
                }
                val streakCount = remember(zenScore) { AppLogic.getStreakCount(recapStore, todayIsMindful) }
                val homeBuddyCard = rememberHomeBuddyCardState(circleUiState.circle, userCode, hasBuddies, buddyStats)

                HomeScreen(
                    usage = usage,
                    streaks = streakCount,
                    yesterdayChangePercent = yesterdayChangePercent,
                    hasBuddies = homeBuddyCard.show,
                    buddyStats = homeBuddyCard.stats,
                    buddyZenScoreOverride = homeBuddyCard.zenScoreOverride,
                    buddyStreaksOverride = homeBuddyCard.streaksOverride,
                    circleStackMembers = homeBuddyCard.stackMembers,
                    isSignedIn = isSignedIn,
                    showSearch = showSearch,
                    zenScore = zenScore,
                    goldInvested = AppConstants.PLACEHOLDER_GOLD_INVESTED,
                    goldChangePercent = GoldOrder.changePercentFor(AppConstants.PLACEHOLDER_GOLD_INVESTED),
                    appCount = homeAppCount,
                    myLikes = myLikes,
                    buddyLikes = buddyLikes,
                    onLikeClick = { viewModel.sendLike() },
                    onShowSearchChange = { showSearch = it },
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
                        openBuddyFlow()
                    },
                    inviteButtonLabel = if (BuddyFlowPreferences.decision(this) == BuddyFlow.ZEN_CIRCLE) "Add Bro" else "Add Buddy",
                    onBuddyCardClick = if (homeBuddyCard.show) {
                        { openBuddyFlow() }
                    } else null,
                    onSignInClick = {
                        val intent = OnboardingActivity.signInIntent(this).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    },
                    onAppClick = onAppClick@{ appInfo ->
                        // Quieted in the Distraction Blocker: don't even start it.
                        if (ContentBlockPrefs.shouldQuietApp(this, appInfo.packageName.toString())) {
                            Toast.makeText(this, "${appInfo.label} is quieted. Let it back in Settings → Distraction Blocker.", Toast.LENGTH_SHORT).show()
                            return@onAppClick
                        }
                        // Launch the exact activity the icon represents rather than
                        // packageManager.getLaunchIntentForPackage(), which resolves a
                        // single "default" activity per package and can't distinguish
                        // Phone from Contacts when an OEM ships both from the same
                        // package (e.g. MIUI's com.android.contacts).
                        val launchIntent = if (appInfo.activityClassName.isNotEmpty()) {
                            Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_LAUNCHER)
                                component = android.content.ComponentName(
                                    appInfo.packageName.toString(),
                                    appInfo.activityClassName
                                )
                            }
                        } else {
                            packageManager.getLaunchIntentForPackage(appInfo.packageName.toString())
                        }
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        }
                    },
                    onAppLongClick = { longPressedApp = it },
                    modifier = Modifier.zenOverlayBlur(longPressedApp != null || showHomeAppsPicker),
                    reveal = homeRevealCue,
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
                        .indexOfFirst { it.key == longPressedApp?.key } + 1,
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
                        onLikeClick = { accountabilityViewModel.sendLike() },
                        onSwitchToZenCircle = {
                            BuddyFlowPreferences.setDecision(this@MainActivity, BuddyFlow.ZEN_CIRCLE)
                            showBuddyBattle = false
                            openBuddyFlow()
                        }
                    )
                }

                // One-time Zen Buddy -> Zen Circle migration choice.
                if (showBuddyFlowMigrationPrompt) {
                    com.zenlauncher.zenmode.ui.screens.BuddyFlowMigrationPrompt(
                        onTryZenCircle = {
                            BuddyFlowPreferences.setDecision(this@MainActivity, BuddyFlow.ZEN_CIRCLE)
                            showBuddyFlowMigrationPrompt = false
                            openBuddyFlow()
                        },
                        onKeepZenBuddy = {
                            BuddyFlowPreferences.setDecision(this@MainActivity, BuddyFlow.ZEN_BUDDY_CLASSIC)
                            showBuddyFlowMigrationPrompt = false
                            openBuddyFlow()
                        }
                    )
                }

                // Zen Bro connect flow: My Zen Circle options → "You're Zen Bros now" → circle dashboard
                if (showBuddyConnect) {
                    val stage = when {
                        showZenCircle -> ZenBroStage.Circle(connectedBuddyName ?: circleBuddyName ?: "Zen Bro")
                        connectedBuddyName != null -> ZenBroStage.Connected(connectedBuddyName!!)
                        else -> ZenBroStage.Connect
                    }
                    // An invite-link tap only auto-connects once the Connect screen is actually
                    // showing — if openBuddyFlow() routed to the circle/migration-prompt instead
                    // (user already has a buddy), the code is left for closeBuddyConnect() to drop.
                    androidx.compose.runtime.LaunchedEffect(pendingInviteCode, stage) {
                        val code = pendingInviteCode
                        if (code != null && stage == ZenBroStage.Connect) {
                            pendingInviteCode = null
                            connectWithInviteCode(code)
                        }
                    }
                    // Fires the actual share/copy action once a circle-mode Share-link/Use-a-code
                    // tap's createCircle() finishes -- see onCreateCircle wiring below. Also the
                    // generic "just landed on a real circle" consume point (deep-link auto-join
                    // included), mirroring justLeftCircle's LaunchedEffect above.
                    androidx.compose.runtime.LaunchedEffect(circleUiState.justEnteredCircle) {
                        if (circleUiState.justEnteredCircle) {
                            val circle = circleUiState.circle
                            if (circle != null) {
                                when (pendingCircleAutoAction) {
                                    PendingCircleAction.SHARE -> buddyConnector.shareCircleInvite(circle.id)
                                    PendingCircleAction.COPY -> buddyConnector.copyUserCode(circle.id, showToast = true)
                                    null -> Unit
                                }
                            }
                            pendingCircleAutoAction = null
                            circleViewModel.consumeEnteredCircleEvent()
                        }
                    }
                    // A join attempt (deep link or pasted code) hit AlreadyInCircle because the
                    // tapper still has a classic Buddy. No dedicated switch-confirmation dialog
                    // anymore (see handleIntentDeepLink) -- point them at Settings instead.
                    androidx.compose.runtime.LaunchedEffect(circleUiState.pendingBuddySwitchCircleId) {
                        if (circleUiState.pendingBuddySwitchCircleId != null) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have a Zen Bro -- disconnect them in Settings first, then try the invite again.",
                                Toast.LENGTH_LONG
                            ).show()
                            circleViewModel.dismissBuddySwitchPrompt()
                        }
                    }
                    // Generic surface for circleUiState.errorMessage -- covers paths that
                    // navigate optimistically before knowing the outcome (e.g. random circle
                    // connect), which would otherwise fail silently with no screen to show it on.
                    androidx.compose.runtime.LaunchedEffect(circleUiState.errorMessage) {
                        circleUiState.errorMessage?.let { msg ->
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                            circleViewModel.clearError()
                        }
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
                            is ZenBroStage.Circle -> {
                                // Real circle takes over once the user has actually created/joined
                                // one (circleUiState.circle != null) -- until then, a circle-mode
                                // user sees just "You" (waiting on Share/Use-a-code to finish
                                // creating one) and a classic-buddy user sees the reskin, exactly
                                // as before. Melt reactions and real leaveCircle() only make sense
                                // once a real circle exists; there's nowhere in Firestore for them
                                // to go otherwise.
                                CircleStageScreen(
                                    circle = circleUiState.circle,
                                    isCircleMode = BuddyFlowPreferences.decision(this@MainActivity) == BuddyFlow.ZEN_CIRCLE,
                                    buddyNameFallback = current.buddyName,
                                    userCode = userCode,
                                    usage = usage,
                                    zenScore = zenScore,
                                    streakCount = streakCount,
                                    yesterdayChangePercent = yesterdayChangePercent,
                                    buddyStats = buddyStats,
                                    reactions = circleUiState.reactions,
                                    connectedToBuddyScreen = connectedBuddyName != null,
                                    removingBuddy = removingBuddy,
                                    circleRemoving = circleUiState.removing,
                                    buddyConnector = buddyConnector,
                                    circleViewModel = circleViewModel,
                                    viewModel = viewModel,
                                    context = this@MainActivity,
                                    onBackToZenCircleFalse = { showZenCircle = false },
                                    onCloseBuddyConnect = { closeBuddyConnect() },
                                    onRemoveBuddy = { removeBuddy() }
                                )
                            }
                            is ZenBroStage.Connected -> ZenBroConnectedScreen(
                                buddyName = current.buddyName,
                                usage = usage,
                                streaks = streakCount,
                                zenScore = zenScore,
                                buddyStats = buddyStats,
                                userCode = userCode,
                                onBackClick = { closeBuddyConnect() },
                                onShareInviteLink = { userCode?.let { buddyConnector.shareBuddyInvite(it) } },
                                // The sheet confirms inline, so no toast here.
                                onCopyInviteCode = { userCode?.let { buddyConnector.copyUserCode(it, showToast = false) } },
                                onMaybeLater = { showZenCircle = true }
                            )
                            ZenBroStage.Connect -> {
                                val isCircleMode = BuddyFlowPreferences.decision(this@MainActivity) == BuddyFlow.ZEN_CIRCLE
                                ZenBroConnectScreen(
                                    userCode = userCode,
                                    circleMode = isCircleMode,
                                    onBackClick = { closeBuddyConnect() },
                                    onShareLink = {
                                        startOrShareCircle(isCircleMode, PendingCircleAction.SHARE) {
                                            userCode?.let { buddyConnector.shareBuddyInvite(it) }
                                        }
                                    },
                                    onCopyCode = {
                                        startOrShareCircle(isCircleMode, PendingCircleAction.COPY) {
                                            userCode?.let { buddyConnector.copyUserCode(it, showToast = true) }
                                        }
                                    },
                                    onAddBuddy = { targetUid ->
                                        buddyConnector.addBuddy(targetUid).also { result ->
                                            if (result is BuddyAddResult.Success) {
                                                // Let "Connected with …" register before the celebration takes over.
                                                connectSuccessJob = lifecycleScope.launch {
                                                    kotlinx.coroutines.delay(700)
                                                    connectedBuddyName = result.buddyName
                                                }
                                            }
                                        }
                                    },
                                    onJoinCircleCode = { code ->
                                        when (circleViewModel.joinCircleAwait(code, via = "code")) {
                                            is com.zenlauncher.zenmode.coreapi.CircleJoinResult.Success -> {
                                                showZenCircle = true
                                                CircleCodeResult.Success
                                            }
                                            is com.zenlauncher.zenmode.coreapi.CircleJoinResult.CircleFull -> CircleCodeResult.CircleFull
                                            is com.zenlauncher.zenmode.coreapi.CircleJoinResult.AlreadyInCircle -> CircleCodeResult.AlreadyInCircle
                                            null -> CircleCodeResult.Error("Not signed in.")
                                        }
                                    },
                                    onRandomConnect = {
                                        if (isCircleMode) {
                                            circleViewModel.findRandomCircle(ProAccess.isPro(this@MainActivity))
                                            showZenCircle = true
                                        } else {
                                            lifecycleScope.launch {
                                                buddyConnector.randomConnect()?.let { connectedBuddyName = it }
                                            }
                                        }
                                    }
                                )
                            }
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

    // removeBuddy and connectWithInviteCode live in MainActivityBuddyFlow.kt as extension
    // functions -- split out to keep this file under the 1000-line ceiling.

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

/** How soon after unlocking Home must appear for the entrance to count as "the unlock". */
private const val UNLOCK_REVEAL_WINDOW_MS = 1_500L

/** Roughly how long the Delayed Unlock screen takes to close over Home. */
private const val DELAYED_UNLOCK_EXIT_MS = 300L
