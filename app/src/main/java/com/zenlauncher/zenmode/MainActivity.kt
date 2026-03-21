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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.HomeScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

class MainActivity : AppCompatActivity() {
    private var isReceiverRegistered = false
    private lateinit var viewModel: MainViewModel
    private lateinit var repository: UsageRepository

    private var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
    private var showSearch by mutableStateOf(false)

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
            ZenTheme(darkTheme = true) {
                val usage by viewModel.stats.observeAsState()

                HomeScreen(
                    usage = usage,
                    streaks = 0, // TODO: wire up streak tracking
                    yesterdayChangePercent = null, // TODO: wire up yesterday comparison
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
                        showBuddyInviteDialog()
                    },
                    onAppClick = { appInfo ->
                        val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName.toString())
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        }
                    },
                    apps = installedApps
                )
            }
        }
    }

    private fun lockScreen() {
        if (ZenAccessibilityService.isRunning()) {
            ZenAccessibilityService.lockScreen()
        } else {
            android.widget.Toast.makeText(this, "Please enable ZenMode accessibility service to lock screen", android.widget.Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun showBuddyInviteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_buddy, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val analyticsManager = ServiceLocator.analyticsManager
        val currentUserId = ServiceLocator.authProvider.getCurrentUserId()
        val uid = repository.getUserUid() ?: currentUserId

        val tvMyCode = dialogView.findViewById<android.widget.TextView>(R.id.tv_my_code_value)
        if (uid != null && uid.length > 7) {
            tvMyCode.text = "${uid.take(7)}..."
        } else {
            tvMyCode.text = uid ?: "Not Signed In"
        }

        dialogView.findViewById<android.view.View>(R.id.iv_copy_code)?.setOnClickListener {
            uid?.let { code ->
                val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("ZenMode Code", code)
                clipboard.setPrimaryClip(clip)
                ServiceLocator.analyticsTracker.trackBuddyCodeGenerated("copy_link")
                android.widget.Toast.makeText(this, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        val etBuddyCode = dialogView.findViewById<android.widget.EditText>(R.id.et_buddy_code)
        val btnAddBuddy = dialogView.findViewById<android.view.View>(R.id.btn_add_buddy)

        btnAddBuddy.isEnabled = false
        btnAddBuddy.alpha = 0.5f

        etBuddyCode.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString().trim()
                btnAddBuddy.isEnabled = input.isNotEmpty()
                btnAddBuddy.alpha = if (input.isNotEmpty()) 1.0f else 0.5f
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnAddBuddy.setOnClickListener {
            val targetUid = etBuddyCode.text.toString().trim()
            if (targetUid.isEmpty()) return@setOnClickListener
            if (targetUid == currentUserId) {
                android.widget.Toast.makeText(this, "You cannot add yourself as a buddy.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val firestoreDataSource = ServiceLocator.firestoreDataSource
            btnAddBuddy.isEnabled = false
            btnAddBuddy.alpha = 0.5f

            lifecycleScope.launch {
                try {
                    val user = firestoreDataSource.getUser(targetUid)
                    if (user != null) {
                        val buddyName = user.displayName
                        val myUid = currentUserId
                        val isAlreadyBuddy = myUid?.let { firestoreDataSource.checkRelationshipExists(it, targetUid) } == true
                        if (isAlreadyBuddy) {
                            android.widget.Toast.makeText(this@MainActivity, "You are already buddies with $buddyName!", android.widget.Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (myUid != null) {
                            firestoreDataSource.sendBuddyInvite(myUid, targetUid)
                            repository.clearCachedBuddy()
                            viewModel.fetchBuddyData()
                            ServiceLocator.analyticsTracker.trackBuddyLinkAccepted("buddy")
                            android.widget.Toast.makeText(this@MainActivity, "Successfully added $buddyName!", android.widget.Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    } else {
                        android.widget.Toast.makeText(this@MainActivity, "User ID not found.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this@MainActivity, "Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                } finally {
                    btnAddBuddy.isEnabled = true
                    btnAddBuddy.alpha = 1.0f
                }
            }
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            unregisterReceiver(screenReceiver)
        }
    }
}
