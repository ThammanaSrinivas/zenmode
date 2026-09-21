package com.zenlauncher.zenmode

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.zenlauncher.zenmode.accessibility.ContentBlockRules
import com.zenlauncher.zenmode.coreapi.UsageAccess
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.recap.DayAggregator
import com.zenlauncher.zenmode.recap.ProUpsellSheet
import com.zenlauncher.zenmode.ui.screens.BlockerApp
import com.zenlauncher.zenmode.ui.screens.BlockerState
import com.zenlauncher.zenmode.ui.screens.DistractionBlockerScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Settings → Distraction Blocker (Figma nodes 2026:949 / 2026:992). Quiet reels & shorts
 * across the short-form apps, quiet whole apps, and (PRO) pause everything for 30 minutes.
 * Enforcement lives in [ZenAccessibilityService]; this screen only edits [ContentBlockPrefs].
 */
class DistractionBlockerActivity : AppCompatActivity() {

    private var state by mutableStateOf(BlockerState())
    private var showProSheet by mutableStateOf(false)
    /** The surface that opened the upsell sheet, for analytics on the plan page. */
    private var proSheetSource = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HomePageSide.RIGHT.applyOnCreate(this)
        refreshPrefs()
        loadApps()

        setContent {
            ZenTheme() {
                val isPro = ProAccess.isProState(this@DistractionBlockerActivity)
                // Ticks the "Paused 24m" countdown and flips back to "Pause" when it runs out.
                LaunchedEffect(state.pausedUntil) {
                    while (state.pausedUntil > System.currentTimeMillis()) {
                        delay(15_000)
                        refreshPrefs()
                    }
                    if (state.pausedUntil != 0L) refreshPrefs()
                }
                DistractionBlockerScreen(
                    state = state.copy(isPro = isPro),
                    onBackClick = { finish() },
                    onPauseClick = {
                        when {
                            state.isPaused -> ContentBlockPrefs.resume(this)
                            isPro -> ContentBlockPrefs.pause(this)
                            else -> {
                                proSheetSource = "distraction_blocker_pause"
                                ServiceLocator.analyticsTracker.trackProUpsellViewed(proSheetSource)
                                showProSheet = true
                            }
                        }
                        refreshPrefs()
                    },
                    onReelsToggle = { on ->
                        ContentBlockPrefs.setReelsQuieted(this, on)
                        refreshPrefs()
                    },
                    onAppToggle = { pkg, on ->
                        ContentBlockPrefs.setAppQuieted(this, pkg, on)
                        ServiceLocator.analyticsManager.trackEvent(
                            if (on) "app_quieted" else "app_unquieted",
                            mapOf("app" to pkg)
                        )
                        refreshPrefs()
                    },
                    onSurfaceToggle = { pkg, surfaceId, on ->
                        ContentBlockPrefs.setSurfaceBlocked(this, pkg, surfaceId, on)
                        refreshPrefs()
                    },
                    onEnableAccessibility = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onDebugDumpToggle = { on ->
                        ContentBlockPrefs.setDebugDumpEnabled(this, on)
                        refreshPrefs()
                    },
                    onClearLastCrash = {
                        ZenAccessibilityService.clearLastCrash(this)
                        refreshPrefs()
                    }
                )
                if (showProSheet) {
                    ProUpsellSheet(
                        onDismiss = { showProSheet = false },
                        canPurchase = ProAccess.canPurchase,
                        onUpgrade = {
                            showProSheet = false
                            ProAccess.openUpgrade(this@DistractionBlockerActivity, proSheetSource)
                        },
                        // Only where Pro can't be bought: otherwise test the real flow.
                        onEnableForTesting = if (ProAccess.canUseDebugOverride && !ProAccess.canPurchase) {
                            {
                                ProAccess.setDebugOverride(this@DistractionBlockerActivity, true)
                                showProSheet = false
                            }
                        } else null
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Accessibility is switched on outside this screen, and stats grow while it's closed.
        refreshPrefs()
    }

    override fun finish() {
        super.finish()
        HomePageSide.RIGHT.applyOnFinish(this)
    }

    private fun refreshPrefs() {
        val rules = ContentBlockRules.default()
        state = state.copy(
            accessibilityOn = ZenAccessibilityService.isEnabledInSettings(this),
            reelsQuieted = ContentBlockPrefs.isReelsQuieted(this),
            quieted = ContentBlockPrefs.quietedApps(this),
            blockedSurfaces = rules.apps.values.flatMap { app ->
                app.surfaces.filter { ContentBlockPrefs.isSurfaceBlocked(this, app.packageName, it.id) }
                    .map { "${app.packageName}/${it.id}" }
            }.toSet(),
            pausedUntil = ContentBlockPrefs.pausedUntil(this).takeIf { it > System.currentTimeMillis() } ?: 0L,
            stops = ContentBlockPrefs.stopsCount(this),
            minutesSaved = ContentBlockPrefs.minutesSaved(this),
            debugDump = ContentBlockPrefs.isDebugDumpEnabled(this),
            lastCrash = ZenAccessibilityService.lastCrash(this)
        )
    }

    /** Every launchable app except ZenMode itself, most-used today first. */
    private fun loadApps() {
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = packageManager
                val launchable = LauncherActivities.query(pm)
                    .map { it.activityInfo.packageName }.distinct().filter { it != packageName }
                val minutes = todayMinutesByApp()
                val surfaces = ContentBlockRules.default().apps
                launchable.map { pkg ->
                    BlockerApp(
                        packageName = pkg,
                        label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg),
                        minutesToday = minutes[pkg] ?: 0L,
                        surfaces = surfaces[pkg]?.surfaces.orEmpty().map { it.id to it.label }
                    )
                }.sortedWith(compareByDescending<BlockerApp> { it.minutesToday }.thenBy { it.label.lowercase() })
            }
            state = state.copy(apps = apps, appsLoaded = true)
        }
    }

    private fun todayMinutesByApp(): Map<String, Long> {
        if (!UsageAccess.isGranted(this)) return emptyMap()
        val repository = UsageRepository(applicationContext, ServiceLocator.analyticsManager)
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = System.currentTimeMillis()
        return runCatching {
            DayAggregator.aggregate(repository.getForegroundSessions(start, end), start, end, setOf(packageName))
                .appMillis.associate { (pkg, millis) -> pkg to TimeUnit.MILLISECONDS.toMinutes(millis) }
        }.getOrDefault(emptyMap())
    }
}
