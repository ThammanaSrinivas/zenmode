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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.AccessibilityDisclosureScreen
import com.zenlauncher.zenmode.ui.screens.AccountabilityScreen
import com.zenlauncher.zenmode.ui.screens.BuddyAddResult
import com.zenlauncher.zenmode.ui.screens.ForceUpdateDialog
import com.zenlauncher.zenmode.ui.screens.HomeScreen
import com.zenlauncher.zenmode.ui.screens.ZenBuddyConnectBottomSheet
import androidx.compose.runtime.collectAsState
import com.zenlauncher.zenmode.ui.theme.ZenTheme

class MainActivity : AppCompatActivity() {
    private var isReceiverRegistered = false
    private lateinit var viewModel: MainViewModel
    private lateinit var repository: UsageRepository

    private var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
    private var showSearch by mutableStateOf(false)
    private var showBuddyConnect by mutableStateOf(false)
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

        // Stats Sync Check
        if (::repository.isInitialized) {
            val lastProcessed = repository.getLastStatsProcessedTime()
            val now = System.currentTimeMillis()
            val interval = AppConstants.STATS_SYNC_INTERVAL_MINUTES * 60 * 1000L

            if (now - lastProcessed > interval) {
                val workerClass = Class.forName("com.zenlauncher.zenmode.internal.StatSyncWorker") as Class<out androidx.work.ListenableWorker>
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

    private fun checkAndStartDoomMonitor() {
        if (DoomScrollingMonitorService.isRunning) return

        val hasUsageStats = hasUsageStatsPermission()
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

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun loadInstalledApps() {
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

        installedApps = pinned + unpinned
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel
        val analyticsManager = ServiceLocator.analyticsManager
        repository = UsageRepository(this, analyticsManager)
        val factory = MainViewModelFactory(repository)
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
                val yesterdayChangePercent by viewModel.yesterdayChangePercent.observeAsState()
                val hasBuddies by viewModel.hasBuddies.observeAsState(initial = false)
                val buddyStats by viewModel.buddyStats.observeAsState()
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

                // Handle disconnect result
                androidx.compose.runtime.LaunchedEffect(accountabilityUiState.disconnectResult) {
                    when (val result = accountabilityUiState.disconnectResult) {
                        is DisconnectResult.Success -> {
                            showBuddyBattle = false
                            showBuddyConnect = true
                            viewModel.refreshBuddyStatsFromCache()
                            accountabilityViewModel.resetDisconnectResult()
                        }
                        is DisconnectResult.Error -> {
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
                    weeklyScreenTimeMillis = weeklyMillis,
                    yesterdayChangePercent = yesterdayChangePercent,
                    hasBuddies = hasBuddies,
                    buddyStats = buddyStats,
                    isSignedIn = isSignedIn,
                    showSearch = showSearch,
                    onShowSearchChange = { showSearch = it },
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
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
                        { showBuddyBattle = true }
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
                    apps = installedApps
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
                    AccountabilityScreen(
                        uiState = accountabilityUiState,
                        onBackClick = { showBuddyBattle = false },
                        onCopyCode = { code ->
                            val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ZenMode Code", code))
                            android.widget.Toast.makeText(this@MainActivity, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onBackToHomeClick = { showBuddyBattle = false },
                        onChangeBuddyConfirmed = { accountabilityViewModel.disconnectBuddy() }
                    )
                }

                // Zen Buddy Connect bottom sheet
                if (showBuddyConnect) {
                    ZenBuddyConnectBottomSheet(
                        userCode = userCode,
                        onCopyCode = {
                            userCode?.let { code ->
                                val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                                val clip = android.content.ClipData.newPlainText("ZenMode Code", code)
                                clipboard.setPrimaryClip(clip)
                                ServiceLocator.analyticsTracker.trackBuddyCodeCopied("manual")
                                android.widget.Toast.makeText(this, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAddBuddy = { targetUid -> addBuddy(targetUid) },
                        onRandomConnect = {
                            lifecycleScope.launch { randomConnect() }
                        },
                        onWatchVideo = {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.YT_BUDDY_INVITE_URL)))
                        },
                        onDismiss = { showBuddyConnect = false }
                    )
                }
            }
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

    private suspend fun randomConnect() {
        val currentUserId = ServiceLocator.authProvider.getCurrentUserId()
        if (currentUserId == null) {
            android.widget.Toast.makeText(this, "Not signed in.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val connectivityManager = getSystemService(android.net.ConnectivityManager::class.java)
        val isConnected = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!isConnected) {
            android.widget.Toast.makeText(this, "No internet connection.", android.widget.Toast.LENGTH_SHORT).show()
            return
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
            return
        }

        try {
            val buddyUid = ServiceLocator.firestoreDataSource.findRandomBuddy(currentUserId)
            if (buddyUid == null) {
                repository.saveLastRandomConnectAttemptTime(System.currentTimeMillis())
                android.widget.Toast.makeText(
                    this,
                    "No buddies available right now. Try again in 30 seconds!",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else {
                val buddy = ServiceLocator.firestoreDataSource.getUser(buddyUid)
                repository.clearCachedBuddy()
                repository.saveHasBuddy(true)
                viewModel.fetchBuddyData()
                ServiceLocator.analyticsTracker.trackBuddyConnected("random")
                android.widget.Toast.makeText(
                    this,
                    "Connected with ${buddy?.displayName ?: "a Zen buddy"}!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            android.widget.Toast.makeText(this, "Connection timed out. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Something went wrong. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            unregisterReceiver(screenReceiver)
        }
    }
}
