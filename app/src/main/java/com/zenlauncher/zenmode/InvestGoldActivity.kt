package com.zenlauncher.zenmode

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.zenlauncher.zenmode.ui.screens.InvestGoldScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

/**
 * Zen Gold's "Invest Gold" destination (Figma node 2026:1250) — step 1, choosing a
 * quantity. ZenMode never places or pre-fills the order: "Review in Kite" only opens
 * Kite (the app if installed, the web otherwise) where the user buys it themselves.
 * The quantity starts at the minimum, so the screen never suggests an amount.
 */
class InvestGoldActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var units by rememberSaveable { mutableIntStateOf(AppConstants.INVEST_GOLD_MIN_UNITS) }

            ZenTheme(darkTheme = ThemePreferences.isDarkMode(this@InvestGoldActivity)) {
                InvestGoldScreen(
                    units = units,
                    onUnitsChange = { units = GoldOrder.clampUnits(it) },
                    onBackClick = { finish() },
                    onReviewInKiteClick = { openKite() }
                    // Menu and T&C stay unwired, same as Zen Gold's own menu / "View T&C".
                )
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
