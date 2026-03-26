package com.zenlauncher.zenmode

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

        // Discover and run the private module initializer via ServiceLoader
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
        }
    }
}
