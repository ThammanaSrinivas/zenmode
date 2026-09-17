package com.zenlauncher.zenmode

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableIntStateOf
import com.zenlauncher.zenmode.ui.screens.ZenGoldScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

/**
 * The home screen's right-swipe page (Figma node 2026:1648). Mirrors
 * [SettingsActivity]'s role on the left swipe — a standalone Activity so
 * MainActivity's own composition/state stays untouched by it.
 */
class ZenGoldActivity : AppCompatActivity() {

    private val promiseHours = mutableIntStateOf(AppConstants.PLACEHOLDER_PROMISE_HOURS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenTheme(darkTheme = ThemePreferences.isDarkMode(this@ZenGoldActivity)) {
                ZenGoldScreen(
                    promiseHours = promiseHours.intValue,
                    onBackClick = { finish() },
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

    // Re-read on every resume so an edit made in MyPromiseActivity shows on return.
    override fun onResume() {
        super.onResume()
        promiseHours.intValue = PromisePreferences.getDailyHours(this)
    }
}
