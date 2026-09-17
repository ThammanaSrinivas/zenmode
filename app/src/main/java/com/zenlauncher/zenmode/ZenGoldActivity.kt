package com.zenlauncher.zenmode

import android.content.Intent
import android.os.Bundle
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
                    onInvestGoldClick = {
                        startActivity(Intent(this, KiteBasketActivity::class.java))
                    }
                )
            }
        }
    }
}
