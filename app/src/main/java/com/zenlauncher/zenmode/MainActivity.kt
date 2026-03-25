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
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.BuddyAddResult
import com.zenlauncher.zenmode.ui.screens.HomeScreen
import com.zenlauncher.zenmode.ui.screens.ZenBuddyConnectBottomSheet
import com.zenlauncher.zenmode.ui.theme.ZenTheme

class MainActivity : AppCompatActivity() {
    private var isReceiverRegistered = false
    private lateinit var viewModel: MainViewModel
    private lateinit var repository: UsageRepository

    private var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
    private var showSearch by mutableStateOf(false)
    private var showBuddyConnect by mutableStateOf(false)

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
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.onResumeCheck()
            viewModel.refreshBuddyStatsFromCache()
        }
        checkAndStartDoomMonitor()
        loadInstalledApps()

        // Analytics
        val tracker = ServiceLocator.analyticsTracker

        // Check Launcher Default
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, 0)
        val currentLauncher = resolveInfo?.activityInfo?.packageName
        val isDefault = currentLauncher == packageName
        tracker.trackLauncherSetAsDefault(isDefault)

        // Check Buddies
        if (::viewModel.isInitialized && viewModel.hasBuddies.value == true) {
            tracker.trackBuddyConnectionActive(1)
        }

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
        installedApps = activities.map { resolveInfo ->
            AppInfo(
                label = resolveInfo.loadLabel(packageManager),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(packageManager)
            )
        }.sortedBy { it.label.toString() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel
        val analyticsManager = ServiceLocator.analyticsManager
        repository = UsageRepository(this, analyticsManager)
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

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

                HomeScreen(
                    usage = usage,
                    streaks = 0, // TODO: wire up streak tracking
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
                        showBuddyConnect = true
                    },
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
                    apps = installedApps
                )

                // Zen Buddy Connect bottom sheet
                if (showBuddyConnect) {
                    ZenBuddyConnectBottomSheet(
                        userCode = userCode,
                        onCopyCode = {
                            userCode?.let { code ->
                                val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                                val clip = android.content.ClipData.newPlainText("ZenMode Code", code)
                                clipboard.setPrimaryClip(clip)
                                ServiceLocator.analyticsTracker.trackBuddyCodeGenerated("copy_link")
                                android.widget.Toast.makeText(this, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAddBuddy = { targetUid -> addBuddy(targetUid) },
                        onRandomConnect = {
                            android.widget.Toast.makeText(this, "Coming soon!", android.widget.Toast.LENGTH_SHORT).show()
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
            android.widget.Toast.makeText(this, "Please enable ZenMode accessibility service to lock screen", android.widget.Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private suspend fun addBuddy(targetUid: String): BuddyAddResult {
        val currentUserId = ServiceLocator.authProvider.getCurrentUserId()

        if (targetUid == currentUserId) return BuddyAddResult.SelfAdd

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
            ServiceLocator.analyticsTracker.trackBuddyLinkAccepted("buddy")

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

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            unregisterReceiver(screenReceiver)
        }
    }
}
