package com.zenlauncher.zenmode

import android.app.Application
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.AppInitializer
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import java.util.ServiceLoader

class ZenModeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Discover and run the private module initializer via ServiceLoader
        val initializers = ServiceLoader.load(AppInitializer::class.java)
        for (initializer in initializers) {
            initializer.initialize(this)
        }

        // Track App Open & Install (only if ServiceLocator was populated)
        if (ServiceLocator.isInitialized) {
            val analyticsTracker = ServiceLocator.analyticsTracker
            analyticsTracker.trackAppOpened()

            val analyticsManager = ServiceLocator.analyticsManager
            val repository = UsageRepository(this, analyticsManager)

            if (repository.isFirstRun()) {
                analyticsTracker.trackAppInstalled()
                repository.setFirstRunComplete()
            }
        }
    }
}
