package com.zenlauncher.zenmode

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.zenlauncher.zenmode.ui.screens.InvestGoldReviewScreen
import com.zenlauncher.zenmode.ui.screens.InvestGoldScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

/**
 * Zen Gold's "Invest Gold" destination: step 1 picks a quantity (Figma node 2026:1250),
 * step 2 reads the order back. ZenMode never places or pre-fills the order: "Open Kite to
 * authorise" only opens Kite (the app if installed, the web otherwise) where the user buys
 * it themselves. The quantity starts at the minimum, so the screen never suggests an amount.
 */
class InvestGoldActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var units by rememberSaveable { mutableIntStateOf(AppConstants.INVEST_GOLD_MIN_UNITS) }
            var reviewing by rememberSaveable { mutableStateOf(false) }

            ZenTheme() {
                AnimatedContent(
                    targetState = reviewing,
                    transitionSpec = {
                        // Forward slides in from the right, back from the left.
                        val direction = if (targetState) 1 else -1
                        (slideInHorizontally(tween(320)) { it * direction / 4 } + fadeIn(tween(320)))
                            .togetherWith(slideOutHorizontally(tween(240)) { -it * direction / 4 } + fadeOut(tween(200)))
                    },
                    label = "investGoldStep"
                ) { onReview ->
                    // Menu and T&C stay unwired, same as Zen Gold's own menu / "View T&C".
                    if (onReview) {
                        InvestGoldReviewScreen(
                            units = units,
                            onBackClick = { reviewing = false },
                            onOpenKiteClick = { openKite() },
                            onChangeQuantityClick = { reviewing = false }
                        )
                    } else {
                        InvestGoldScreen(
                            units = units,
                            onUnitsChange = { units = GoldOrder.clampUnits(it) },
                            onBackClick = { finish() },
                            onReviewInKiteClick = { reviewing = true }
                        )
                    }
                }
            }
        }
    }

    private fun openKite() {
        val intent = packageManager.getLaunchIntentForPackage(AppConstants.KITE_PACKAGE_NAME)
            ?: Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.KITE_WEB_URL))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.invest_gold_kite_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}
