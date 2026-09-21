package com.zenlauncher.zenmode

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import com.zenlauncher.zenmode.coreapi.PromisePreferences
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.ZenGoldScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

/**
 * The home screen's left-swipe page (Figma node 2026:1648) — the right-hand of the three
 * home pages, opposite [ZenScoreActivity]. A standalone Activity so MainActivity's own
 * composition/state stays untouched by it.
 */
class ZenGoldActivity : AppCompatActivity() {

    private val promiseHours = mutableIntStateOf(AppConstants.PLACEHOLDER_PROMISE_HOURS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HomePageSide.RIGHT.applyOnCreate(this)
        
        ServiceLocator.analyticsTracker.trackGoldTabViewed("home_swipe_or_click")

        setContent {
            ZenTheme() {
                ZenGoldScreen(
                    promiseHours = promiseHours.intValue,
                    onBackClick = { finish() },
                    // TEMP: force-unlocked so the Kite basket-redirect spike is reachable
                    // for testing — revert to the real PLACEHOLDER_INVEST_GOLD_UNLOCKED
                    // gate once a real weekly-promise backend drives it.
                    investGoldUnlocked = true,
                    // TEMP: Invest Gold opens InvestGoldActivity, whose own "Review in Kite"
                    // does a direct native-app redirect (no prefill) while a Zerodha partner
                    // approval for prefilled native deep links is pending — see the email in
                    // zenmode_docs/docs/features/gold-streak.md. KiteBasketActivity (WebView +
                    // prefilled basket order) stays in the codebase to swap back in once approved.
                    onInvestGoldClick = {
                        startActivity(Intent(this@ZenGoldActivity, InvestGoldActivity::class.java))
                    },
                    onEditPromiseClick = {
                        startActivity(Intent(this@ZenGoldActivity, MyPromiseActivity::class.java))
                    }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        HomePageSide.RIGHT.applyOnFinish(this)
    }

    // Re-read on every resume so an edit made in MyPromiseActivity shows on return.
    override fun onResume() {
        super.onResume()
        promiseHours.intValue = PromisePreferences.getDailyHours(this)
    }
}
