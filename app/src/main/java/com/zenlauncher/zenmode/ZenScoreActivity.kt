package com.zenlauncher.zenmode

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.zenlauncher.zenmode.ui.screens.ZenScoreScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

/**
 * The home screen's "Zen Score" tap target (Figma node 2026:2035) — the widget
 * in HomeHeader, not a swipe gesture. A standalone Activity, same reasoning as
 * [ZenGoldActivity]: MainActivity's own composition/state stays untouched by it.
 */
class ZenScoreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenTheme(darkTheme = ThemePreferences.isDarkMode(this@ZenScoreActivity)) {
                ZenScoreScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}
