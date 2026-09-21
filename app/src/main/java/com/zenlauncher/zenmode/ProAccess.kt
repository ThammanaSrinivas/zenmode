package com.zenlauncher.zenmode

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.ProEntry
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The app's single answer to "is this user PRO?". Pro comes from either a server grant
 * ([ServiceLocator.proEntitlementProvider], invite-only early access) or a subscription
 * ([ServiceLocator.entitlementProvider]); every screen asks here so the two never disagree.
 * Debug builds can additionally switch PRO on locally to test PRO surfaces.
 */
object ProAccess {
    private const val PREFS_NAME = "zenmode_prefs"
    private const val KEY_DEBUG_OVERRIDE = "pro_debug_override"

    @Volatile
    private var debugOverride: MutableStateFlow<Boolean>? = null

    fun isPro(context: Context): Boolean = combine(
        granted = ServiceLocator.proEntitlementProvider.isPro.value,
        subscribed = ServiceLocator.entitlementProvider.entitlement.value.isPro,
        overridden = debugOverride(context).value
    )

    /** Recomposes when either entitlement changes (e.g. after sign-in or a purchase). */
    @Composable
    fun isProState(context: Context): Boolean {
        val granted by ServiceLocator.proEntitlementProvider.isPro.collectAsState()
        val subscription by ServiceLocator.entitlementProvider.entitlement.collectAsState()
        val overridden by debugOverride(context).collectAsState()
        return combine(granted, subscription.isPro, overridden)
    }

    /** The one place the three signals are combined, so [isPro] and [isProState] can't drift. */
    private fun combine(granted: Boolean, subscribed: Boolean, overridden: Boolean): Boolean =
        granted || subscribed || overridden

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

    /**
     * Where every "Upgrade" in the app goes: the Pro plan page when Pro can be bought, otherwise
     * an early-access request. [source] names the surface for analytics only.
     */
    fun openUpgrade(context: Context, source: String) {
        if (ServiceLocator.entitlementProvider.isAvailable) {
            context.startActivity(ZenProActivity.intent(context, ProEntry.GATE, source))
        } else {
            requestAccess(context)
        }
    }

    /** True when "Upgrade" leads to the plan page rather than an early-access request. */
    val canPurchase: Boolean get() = ServiceLocator.entitlementProvider.isAvailable

    /**
     * Builds that can't sell Pro ask the team for early access by email, falling back to the
     * Telegram group when there's no mail app.
     */
    private fun requestAccess(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${AppConstants.SUPPORT_EMAIL}"))
            .putExtra(Intent.EXTRA_SUBJECT, "ZenMode PRO early access")
            .putExtra(Intent.EXTRA_TEXT, "Hi ZenMode team, I'd love early access to ZenMode PRO.")
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.TELEGRAM_URL)))
        }
    }
}
