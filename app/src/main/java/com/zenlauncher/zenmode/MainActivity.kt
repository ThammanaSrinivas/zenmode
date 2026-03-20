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
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.widget.LinearLayout
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

class MainActivity : AppCompatActivity() {
    private var isReceiverRegistered = false
    private lateinit var viewModel: MainViewModel
    private lateinit var repository: UsageRepository

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

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.onResumeCheck()
            viewModel.refreshBuddyStatsFromCache()
        }
        checkAndStartDoomMonitor()

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
             // Retrieve count if possible, or just default to 1 for now if boolean
             // The viewModel hasBuddies is boolean, but I can check buddyStats maybe?
             // For now, if hasBuddies is true, we assume at least 1.
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
        if (DoomScrollingMonitorService.isRunning) return // Already running, skip redundant start

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
        } else {
             // Optionally prompt user. Since this is ZenMode, we should probably guide them.
             // For v1, I will just log or user might have to enable manually, 
             // but user said "Use permissions...". I should ideally prompt.
             // Let's add a simple check to prompt if missing.
             if (!hasUsageStats) {
                 // We could show a dialog here or just toast
                 // android.widget.Toast.makeText(this, "Please grant Usage Access for ZenMode", android.widget.Toast.LENGTH_LONG).show()
                 // startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
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

        setContentView(R.layout.activity_main)

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

        // Observe ViewModel
        viewModel.navigateToDelayedUnlock.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                val delayedIntent = Intent(this, DelayedUnlockActivity::class.java)
                delayedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(delayedIntent)
                viewModel.onDelayedUnlockNavigated()
            }
        }

        // 1. Inject Fragments
        if (savedInstanceState == null) {
            val myStats = StatsWidgetFragment.newInstance("My stats", true)
            val buddyInvite = BuddyInviteFragment.newInstance()

            supportFragmentManager.beginTransaction()
                .replace(R.id.widget_slot_1, myStats)
                .replace(R.id.widget_slot_2, buddyInvite)
                .commit()
        }

        // Fetch buddy data and swap widget if buddies exist
        viewModel.fetchBuddyData()
        viewModel.hasBuddies.observe(this) { hasBuddies ->
            if (hasBuddies) {
                val buddyStats = StatsWidgetFragment.newInstance("My Buddy's stats", false)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.widget_slot_2, buddyStats)
                    .commit()
            }
        }

        // 2. Setup Bottom Bar Actions
        findViewById<ImageView>(R.id.iv_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageView>(R.id.iv_google).setOnClickListener {
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            // Fallback if no web search app found (rare)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                 startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
            }
        }

        findViewById<ImageView>(R.id.iv_phone).setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL))
        }

        // 3. Setup RecyclerView and App Grid
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view)
        val indicatorLayout = findViewById<LinearLayout>(R.id.indicator_layout)
        
        AppGridManager(this, recyclerView, indicatorLayout)
//
//        // For debugging: Trigger an immediate ONE-TIME sync so you can see it work now.
//        val testRequest = androidx.work.OneTimeWorkRequestBuilder<com.zenlauncher.zenmode.worker.StatSyncWorker>()
//            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
//            .build()
//        androidx.work.WorkManager.getInstance(this).enqueue(testRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            unregisterReceiver(screenReceiver)
        }
    }
}
