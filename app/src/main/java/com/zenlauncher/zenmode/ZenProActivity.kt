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
            track("pro_page_viewed", mapOf("entry" to entry, "tier" to provider.entitlement.value.status.name.lowercase()))
        }

        setContent {
            ZenTheme {
                val entitlement by provider.entitlement.collectAsState()
                LaunchedEffect(Unit) { offers = provider.offers() }

                if (celebrating) {
                    BackHandler { finish() }
                    ProWelcome(onContinue = { finish() })
                    return@ZenTheme
                }
                ZenProScreen(
                    entitlement = entitlement,
                    isPro = ProAccess.isProState(this@ZenProActivity),
                    offers = offers,
                    isWorking = isWorking,
                    errorMessage = errorMessage,
                    onBackClick = { finish() },
                    onPurchase = { period -> purchase(period) },
                    onCancel = { runAction("pro_cancelled") { provider.cancel(this@ZenProActivity) } },
                    onResume = { runAction("pro_resumed") { provider.resume(this@ZenProActivity) } }
                )
            }
        }
    }

    private fun purchase(period: BillingPeriod) {
        val provider = ServiceLocator.entitlementProvider
        val trial = offers.firstOrNull { it.period == period }?.freeTrialDays?.let { it > 0 } == true
        val props = mapOf("period" to period.name.lowercase(), "trial" to trial)
        track("pro_purchase_started", props)
        isWorking = true
        errorMessage = null
        lifecycleScope.launch {
            when (val result = provider.purchase(this@ZenProActivity, period)) {
                PurchaseResult.Success -> {
                    track("pro_purchase_completed", props)
                    // One thank-you screen, then straight back to where they came from.
                    celebrating = true
                }
                PurchaseResult.Cancelled -> Unit
                is PurchaseResult.Failed -> errorMessage = result.message
            }
            isWorking = false
        }
    }

    private fun runAction(event: String, action: suspend () -> Boolean) {
        isWorking = true
        errorMessage = null
        lifecycleScope.launch {
            val ok = action()
            if (ok) {
                val e = ServiceLocator.entitlementProvider.entitlement.value
                track(event, mapOf("period" to (e.period?.name?.lowercase() ?: "none")))
            } else {
                errorMessage = "That didn't go through. Nothing was changed. Try again in a moment."
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

        fun intent(context: Context, entry: ProEntry): Intent =
            Intent(context, ZenProActivity::class.java).putExtra(EXTRA_ENTRY, entry.analyticsName)
    }
}
