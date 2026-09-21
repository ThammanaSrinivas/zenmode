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
import com.zenlauncher.zenmode.coreapi.PromiseEditLock
import com.zenlauncher.zenmode.coreapi.PromisePreferences
import com.zenlauncher.zenmode.ui.screens.MyPromiseScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

/**
 * Zen Gold's "Edit my promise" destination (Figma node 2026:1793). The stepper
 * edits a draft; it's committed only when the user leaves (arrow, CTA or system
 * back), so tapping through values doesn't count as repeated promise changes.
 * The draft only moves when [PromisePreferences.editLock] allows it — free edits
 * once a week on Sundays, Pro edits twice a week on any day.
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

        val persistedHours = PromisePreferences.getDailyHours(this)
        val initialHours = intent.getIntExtra(EXTRA_SUGGESTED_HOURS, 0)
            .takeIf { it in AppConstants.PROMISE_MIN_DAILY_HOURS..AppConstants.PROMISE_MAX_DAILY_HOURS }
            ?: persistedHours

        setContent {
            var dailyHours by rememberSaveable { mutableIntStateOf(initialHours) }
            val isPro = ProAccess.isProState(this@MyPromiseActivity)
            val editLock = PromisePreferences.editLock(this@MyPromiseActivity, isPro)

            ZenTheme() {
                MyPromiseScreen(
                    dailyHours = dailyHours,
                    editLock = editLock,
                    isPro = isPro,
                    onDailyHoursChange = { dailyHours = it },
                    onBackClick = {
                        val stillAllowed = PromisePreferences.editLock(
                            this@MyPromiseActivity,
                            ProAccess.isPro(this@MyPromiseActivity)
                        ) == PromiseEditLock.NONE
                        if (dailyHours != persistedHours && stillAllowed) {
                            PromisePreferences.setDailyHours(this@MyPromiseActivity, dailyHours)
                            PromisePreferences.recordPromiseEdit(this@MyPromiseActivity)
                        }
                        finish()
                    },
                    // No holdings screen exists yet — same as Zen Gold's unwired "View all".
                    onSeeAllHoldingsClick = {}
                )
            }
        }
    }
}
