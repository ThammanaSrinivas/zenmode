package com.zenlauncher.zenmode.coreapi.services

import android.app.Application

/**
 * Initializes backend services (Firebase, PostHog, etc.) at app startup.
 * Discovered at runtime via ServiceLoader.
 */
interface AppInitializer {
    fun initialize(application: Application)
}
