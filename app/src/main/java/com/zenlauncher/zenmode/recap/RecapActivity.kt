package com.zenlauncher.zenmode.recap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.zenlauncher.zenmode.InvestGoldActivity
import com.zenlauncher.zenmode.MyPromiseActivity
import com.zenlauncher.zenmode.ProAccess
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Plays one week's recap. Opened from the notification, the home screen or Settings. */
class RecapActivity : ComponentActivity() {

    companion object {
        const val SOURCE_NOTIFICATION = "notification"
        const val SOURCE_HOME = "home"
        const val SOURCE_SETTINGS = "settings"

        private const val EXTRA_WEEK_START = "week_start"
        private const val EXTRA_SOURCE = "source"

        fun intent(context: Context, weekStart: LocalDate, source: String): Intent =
            Intent(context, RecapActivity::class.java)
                .putExtra(EXTRA_WEEK_START, weekStart.toString())
                .putExtra(EXTRA_SOURCE, source)
    }

    private var recap by mutableStateOf<WeeklyRecap?>(null)
    private var completedTracked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val weekStart = intent.getStringExtra(EXTRA_WEEK_START)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (weekStart == null) {
            finish()
            return
        }
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: SOURCE_HOME

        lifecycleScope.launch {
            val store = RecapStore(applicationContext)
            val loaded = withContext(Dispatchers.IO) {
                store.recap(weekStart)?.also { store.markSeen(weekStart) }
            }
            if (loaded == null) {
                finish()
                return@launch
            }
            RecapNotifier.cancel(applicationContext)
            ServiceLocator.analyticsTracker.trackRecapOpened(weekStart.toString(), loaded.outcome.analyticsKey, source)
            ServiceLocator.analyticsTracker.trackWeeklyScreentimeViewed(source)
            
            try {
                val installTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime
                val daysSinceInstall = ((System.currentTimeMillis() - installTime) / (1000 * 60 * 60 * 24)).toInt()
                if (daysSinceInstall in 6..8) {
                    ServiceLocator.analyticsTracker.trackDay7MilestoneShown()
                } else if (daysSinceInstall in 28..32) {
                    ServiceLocator.analyticsTracker.trackDay30MilestoneShown()
                }
            } catch (e: Exception) {}
            
            recap = loaded
        }

        setContent {
            ZenTheme(darkTheme = false) {
                val current = recap ?: return@ZenTheme
                val week = current.weekStart.toString()
                val outcome = current.outcome.analyticsKey
                RecapScreen(
                    recap = current,
                    onCardViewed = { card, position ->
                        ServiceLocator.analyticsTracker.trackRecapCardViewed(week, outcome, card.key, position)
                        if (position == RecapStory.cards(current).lastIndex && !completedTracked) {
                            completedTracked = true
                            ServiceLocator.analyticsTracker.trackRecapCompleted(week, outcome)
                        }
                    },
                    onInvest = {
                        ServiceLocator.analyticsTracker.trackRecapCtaClicked(week, outcome, "invest")
                        trackMilestoneEngaged()
                        startActivity(Intent(this, InvestGoldActivity::class.java))
                        finish()
                    },
                    onRecommit = {
                        ServiceLocator.analyticsTracker.trackRecapCtaClicked(week, outcome, "recommit")
                        trackMilestoneEngaged()
                        val suggested = (RecapStory.cards(current).last() as? RecapCard.Recommit)?.suggestedPromiseHours
                        startActivity(MyPromiseActivity.intent(this, suggested))
                        finish()
                    },
                    onShare = {
                        lifecycleScope.launch {
                            runCatching {
                                RecapReport.share(this@RecapActivity, current, attachPdf = ProAccess.isPro(this@RecapActivity))
                            }.onFailure {
                                Toast.makeText(this@RecapActivity, "Couldn't share the report. Please try again.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onClose = ::finish
                )
            }
        }
    }

    private fun trackMilestoneEngaged() {
        try {
            val installTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime
            val daysSinceInstall = ((System.currentTimeMillis() - installTime) / (1000 * 60 * 60 * 24)).toInt()
            if (daysSinceInstall in 6..8) {
                ServiceLocator.analyticsTracker.trackDay7MilestoneEngaged()
            } else if (daysSinceInstall in 28..32) {
                ServiceLocator.analyticsTracker.trackDay30MilestoneEngaged(0L) // Passing 0 as we don't have cumulative time handy here
            }
        } catch (e: Exception) {}
    }
}
