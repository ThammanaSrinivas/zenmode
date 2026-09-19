package com.zenlauncher.zenmode

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.zenlauncher.zenmode.ui.screens.ZenGoldScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

/**
 * The home screen's right-swipe page (Figma node 2026:1648). Mirrors
 * [SettingsActivity]'s role on the left swipe — a standalone Activity so
 * MainActivity's own composition/state stays untouched by it.
 */
class ZenGoldActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenTheme(darkTheme = ThemePreferences.isDarkMode(this@ZenGoldActivity)) {
                ZenGoldScreen(
                    onBackClick = { finish() },
                    // TEMP: force-unlocked so the Kite basket-redirect spike is reachable
                    // for testing — revert to the real PLACEHOLDER_INVEST_GOLD_UNLOCKED
                    // gate once a real weekly-promise backend drives it.
                    investGoldUnlocked = true,
                    // TEMP: direct native-app redirect (no prefill) while a Zerodha partner
                    // approval for prefilled native deep links is pending — see the email in
                    // zenmode_docs/docs/features/gold-streak.md. Swap back to KiteBasketActivity
                    // (still in the codebase, WebView + prefilled basket order) once approved.
                    onInvestGoldClick = { openKite() }
                )
            }
        }
    }

    private fun openKite() {
        val intent = packageManager.getLaunchIntentForPackage(KITE_PACKAGE_NAME)
            ?: Intent(Intent.ACTION_VIEW, Uri.parse(KITE_WEB_URL))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Couldn't open Kite", Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val KITE_PACKAGE_NAME = "com.zerodha.kite3"
        const val KITE_WEB_URL = "https://kite.zerodha.com/"
    }
}
