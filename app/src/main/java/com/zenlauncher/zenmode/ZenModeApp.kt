package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.recap.WeeklyRecapWorker
import android.app.Application
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.AppInitializer
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import java.util.ServiceLoader

class ZenModeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Restore persisted dark/light theme before any activity renders
        ThemePreferences.applyStoredTheme(this)
        ZenSound.init(this)

        // [WHAT] Discovers and runs the backend AppInitializer via the ServiceLoader SPI.
        // [WHY] The one place core-mock vs core-private gets selected - no other file branches
        // on which backend is present.
        // [HOW] Reads META-INF/services/...AppInitializer; the discovered impl populates
        // ServiceLocator's lateinit properties.
        // [WHERE] Runs once, before any Activity/ViewModel touches ServiceLocator.
        val initializers = ServiceLoader.load(AppInitializer::class.java)
        for (initializer in initializers) {
            initializer.initialize(this)
        }

        // Track App First Open (only if ServiceLocator was populated)
        if (ServiceLocator.isInitialized) {
            val analyticsTracker = ServiceLocator.analyticsTracker
            val analyticsManager = ServiceLocator.analyticsManager
            val repository = UsageRepository(this, analyticsManager)

            if (repository.isFirstRun()) {
                analyticsTracker.trackAppFirstOpen(
                    source = "direct", // Placeholder source
                    device = android.os.Build.MODEL
                )
                repository.setFirstRunComplete()
            }

            // Weekly recap: record finished days before Android forgets them, announce Mondays.
            WeeklyRecapWorker.schedule(this)

            // Re-identify existing signed-in users (one-time backfill)
            if (ServiceLocator.authProvider.isSignedIn() && !repository.isPostHogIdentified()) {
                val authProvider = ServiceLocator.authProvider
                val userId = authProvider.getCurrentUserId()
                if (userId != null) {
                    analyticsManager.identifyUser(userId, mapOf(
                        "name" to (authProvider.getDisplayName() ?: ""),
                        "email" to (authProvider.getEmail() ?: "")
                    ))
                    repository.setPostHogIdentified(true)
                }
            }
        }
    }
}
