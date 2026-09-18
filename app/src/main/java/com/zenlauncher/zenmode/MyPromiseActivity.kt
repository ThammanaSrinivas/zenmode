package com.zenlauncher.zenmode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.zenlauncher.zenmode.ui.screens.MyPromiseScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

/**
 * Zen Gold's "Edit my promise" destination (Figma node 2026:1793). The stepper
 * edits a draft; it's committed only when the user leaves (arrow, CTA or system
 * back), so tapping through values doesn't count as repeated promise changes.
 */
class MyPromiseActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_SUGGESTED_HOURS = "suggested_hours"

        /** [suggestedHours] pre-fills the stepper (e.g. from a missed week's recap). */
        fun intent(context: Context, suggestedHours: Int? = null): Intent =
            Intent(context, MyPromiseActivity::class.java).apply {
                if (suggestedHours != null) putExtra(EXTRA_SUGGESTED_HOURS, suggestedHours)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storedHours = intent.getIntExtra(EXTRA_SUGGESTED_HOURS, 0)
            .takeIf { it in AppConstants.PROMISE_MIN_DAILY_HOURS..AppConstants.PROMISE_MAX_DAILY_HOURS }
            ?: PromisePreferences.getDailyHours(this)

        setContent {
            var dailyHours by rememberSaveable { mutableIntStateOf(storedHours) }

            ZenTheme(darkTheme = ThemePreferences.isDarkMode(this@MyPromiseActivity)) {
                MyPromiseScreen(
                    dailyHours = dailyHours,
                    onDailyHoursChange = { dailyHours = it },
                    onBackClick = {
                        PromisePreferences.setDailyHours(this@MyPromiseActivity, dailyHours)
                        finish()
                    },
                    // No holdings screen exists yet — same as Zen Gold's unwired "View all".
                    onSeeAllHoldingsClick = {}
                )
            }
        }
    }
}
