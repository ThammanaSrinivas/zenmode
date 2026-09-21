package com.zenlauncher.zenmode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.zenlauncher.zenmode.coreapi.services.BillingPeriod
import com.zenlauncher.zenmode.coreapi.services.PlanOffer
import com.zenlauncher.zenmode.coreapi.services.PurchaseResult
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.ProEntry
import com.zenlauncher.zenmode.ui.screens.ProWelcome
import androidx.activity.compose.BackHandler
import com.zenlauncher.zenmode.ui.screens.ZenProScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rememberDarkTheme
import kotlinx.coroutines.launch

/**
 * Hosts ZenMode Pro: the plan page for Free users, Manage for Pro users. Reachable from the
 * settings plan card and from any PRO gate. Never opened automatically.
 */
class ZenProActivity : AppCompatActivity() {

    private var offers by mutableStateOf<List<PlanOffer>>(emptyList())
    private var isWorking by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var celebrating by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val provider = ServiceLocator.entitlementProvider
        if (!provider.isAvailable) {
            finish()
            return
        }

        val entry = intent.getStringExtra(EXTRA_ENTRY) ?: ProEntry.PLAN_CARD.analyticsName
        if (savedInstanceState == null) {
            val props = mutableMapOf<String, Any>("entry" to entry, "tier" to provider.entitlement.value.status.name.lowercase())
            intent.getStringExtra(EXTRA_SOURCE)?.let { props["source"] = it }
            track("pro_page_viewed", props)
        }

        setContent {
            // The welcome is a dawn forest: the app dims into ink for it whatever the user's
            // Appearance is, which ZenTheme crossfades, and which also hands the scene the
            // right system bars and the ink tokens its text needs.
            // Read unconditionally: `||` would short-circuit a composable call away and
            // leave the slot table a different shape on the frame the welcome opens.
            val ink = rememberDarkTheme()
            ZenTheme(darkTheme = celebrating || ink) {
                val entitlement by provider.entitlement.collectAsState()
                LaunchedEffect(Unit) {
                    offers = runCatching { provider.offers() }.getOrDefault(emptyList())
                    if (offers.isEmpty()) errorMessage = "Couldn't load plans. Check your connection and try again."
                }

                if (celebrating) {
                    BackHandler { finish() }
                    ProWelcome(onContinue = { finish() }, isSimulated = provider.isSimulated)
                    return@ZenTheme
                }
                ZenProScreen(
                    entitlement = entitlement,
                    isPro = ProAccess.isProState(this@ZenProActivity),
                    offers = offers,
                    isWorking = isWorking,
                    isSimulated = provider.isSimulated,
                    errorMessage = errorMessage,
                    onBackClick = { finish() },
                    onPurchase = { period -> purchase(period) },
                    onCancel = { runAction("pro_cancelled") { provider.cancel(this@ZenProActivity) } },
                    onResume = { runAction("pro_resumed") { provider.resume(this@ZenProActivity) } }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Renewals, trial conversions and lapses happen while the page is closed.
        lifecycleScope.launch { ServiceLocator.entitlementProvider.refresh() }
    }

    private fun purchase(period: BillingPeriod) {
        if (isWorking) return
        val provider = ServiceLocator.entitlementProvider
        val trial = offers.firstOrNull { it.period == period }?.freeTrialDays?.let { it > 0 } == true
        val props = mapOf("period" to period.name.lowercase(), "trial" to trial, "simulated" to provider.isSimulated)
        track("pro_purchase_started", props)
        isWorking = true
        errorMessage = null
        lifecycleScope.launch {
            val result = runCatching { provider.purchase(this@ZenProActivity, period) }
                .getOrElse { PurchaseResult.Failed(GENERIC_ERROR) }
            when (result) {
                PurchaseResult.Success -> {
                    track("pro_purchase_completed", props)
                    // One thank-you screen, then straight back to where they came from.
                    celebrating = true
                }
                PurchaseResult.Cancelled -> track("pro_purchase_cancelled", props)
                is PurchaseResult.Failed -> {
                    track("pro_purchase_failed", props)
                    errorMessage = result.message
                }
            }
            isWorking = false
        }
    }

    private fun runAction(event: String, action: suspend () -> Boolean) {
        if (isWorking) return
        isWorking = true
        errorMessage = null
        lifecycleScope.launch {
            val ok = runCatching { action() }.getOrDefault(false)
            if (ok) {
                val e = ServiceLocator.entitlementProvider.entitlement.value
                track(event, mapOf("period" to (e.period?.name?.lowercase() ?: "none")))
            } else {
                errorMessage = GENERIC_ERROR
            }
            isWorking = false
        }
    }

    // Deliberately never includes the Zen Score: it is not an input to the sales path.
    private fun track(event: String, props: Map<String, Any>) {
        if (ServiceLocator.isInitialized) ServiceLocator.analyticsManager.trackEvent(event, props)
    }

    companion object {
        private const val EXTRA_ENTRY = "entry"
        private const val EXTRA_SOURCE = "source"
        private const val GENERIC_ERROR = "That didn't go through. Nothing was changed. Try again in a moment."

        /** [source] names the surface behind a gate (e.g. "zen_score_report"), for analytics only. */
        fun intent(context: Context, entry: ProEntry, source: String? = null): Intent =
            Intent(context, ZenProActivity::class.java)
                .putExtra(EXTRA_ENTRY, entry.analyticsName)
                .putExtra(EXTRA_SOURCE, source)
    }
}
