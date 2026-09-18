package com.zenlauncher.zenmode

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The app's single answer to "is this user PRO?". The entitlement itself comes from
 * [ServiceLocator.proEntitlementProvider]; debug builds can additionally switch PRO on
 * locally so the team can test PRO surfaces without a server grant.
 */
object ProAccess {
    private const val PREFS_NAME = "zenmode_prefs"
    private const val KEY_DEBUG_OVERRIDE = "pro_debug_override"

    @Volatile
    private var debugOverride: MutableStateFlow<Boolean>? = null

    fun isPro(context: Context): Boolean =
        ServiceLocator.proEntitlementProvider.isPro.value || debugOverride(context).value

    /** Recomposes when the entitlement changes (e.g. after sign-in). */
    @Composable
    fun isProState(context: Context): Boolean {
        val entitled by ServiceLocator.proEntitlementProvider.isPro.collectAsState()
        val overridden by debugOverride(context).collectAsState()
        return entitled || overridden
    }

    val canUseDebugOverride: Boolean get() = BuildConfig.DEBUG

    fun setDebugOverride(context: Context, enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DEBUG_OVERRIDE, enabled).apply()
        debugOverride(context).value = enabled
    }

    private fun debugOverride(context: Context): MutableStateFlow<Boolean> =
        debugOverride ?: synchronized(this) {
            debugOverride ?: MutableStateFlow(
                BuildConfig.DEBUG && context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(KEY_DEBUG_OVERRIDE, false)
            ).also { debugOverride = it }
        }
}
