package com.zenlauncher.zenmode

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.zenlauncher.zenmode.coreapi.PromisePreferences
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.recap.RecapCollector
import com.zenlauncher.zenmode.recap.RecapStore
import com.zenlauncher.zenmode.recap.weekStartOf
import com.zenlauncher.zenmode.ui.screens.ZenGoldScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The home screen's left-swipe page (Figma node 2026:1648) — the right-hand of the three
 * home pages, opposite [ZenScoreActivity]. A standalone Activity so MainActivity's own
 * composition/state stays untouched by it.
 */
class ZenGoldActivity : AppCompatActivity() {

    private lateinit var recapStore: RecapStore
    private lateinit var usageRepository: UsageRepository

    private var weekly by mutableStateOf(ZenGoldPromiseState())
    private var monthly by mutableStateOf(ZenGoldPromiseState(period = GoldPromisePeriod.MONTHLY))

    // The Weekly tab's "look back" (Pro). 0 is the live week; -1 is last week, and so on.
    // [displayedWeek] is recomputed for the chosen offset, while [weekly] stays on the live
    // week so gold pay's gate never moves with the view.
    private var weekOffset by mutableIntStateOf(0)
    private var displayedWeek by mutableStateOf(ZenGoldPromiseState())
    private var earliestWeekOffset by mutableIntStateOf(0)
    private var goldInvested by mutableStateOf("0")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HomePageSide.RIGHT.applyOnCreate(this)

        ServiceLocator.analyticsTracker.trackGoldTabViewed("home_swipe_or_click")

        recapStore = RecapStore(this)
        usageRepository = UsageRepository(this, ServiceLocator.analyticsManager)

        setContent {
            ZenTheme() {
                val isPro = ProAccess.isProState(this@ZenGoldActivity)
                ZenGoldScreen(
                    weekly = weekly,
                    monthly = monthly,
                    isPro = isPro,
                    displayedWeek = displayedWeek,
                    weekOffset = weekOffset,
                    earliestWeekOffset = earliestWeekOffset,
                    onWeekOffsetChange = { offset -> showWeek(offset) },
                    goldInvested = goldInvested,
                    onBackClick = { finish() },
                    onZenScoreClick = {
                        finish()
                        startActivity(Intent(this@ZenGoldActivity, ZenScoreActivity::class.java))
                    },
                    // Invest Gold opens InvestGoldActivity, whose own "Review in Kite" does a
                    // direct native-app redirect (no prefill) while a Zerodha partner approval
                    // for prefilled native deep links is pending — see the email in
                    // zenmode_docs/docs/features/gold-streak.md. KiteBasketActivity (WebView +
                    // prefilled basket order) stays in the codebase to swap back in once approved.
                    onInvestGoldClick = {
                        startActivity(Intent(this@ZenGoldActivity, InvestGoldActivity::class.java))
                    },
                    onEditPromiseClick = {
                        startActivity(Intent(this@ZenGoldActivity, MyPromiseActivity::class.java))
                    },
                    onViewTermsClick = {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.TERMS_OF_SERVICE_URL)))
                        } catch (_: ActivityNotFoundException) {
                        }
                    }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        HomePageSide.RIGHT.applyOnFinish(this)
    }

    // Re-read on every resume (including the first, right after onCreate), so an edit made in
    // MyPromiseActivity — or usage that accrued while this screen was backgrounded — shows on return.
    override fun onResume() {
        super.onResume()
        goldInvested = GoldLedger.balanceLabel(this)
        refreshPromiseState()
    }

    /**
     * Pages the Weekly card to [offset] (0 = live week, negative = back). Clamped to the
     * history actually on record, so the arrows can never walk off the end of RecapStore's
     * retention window into a week of empty bars.
     */
    private fun showWeek(offset: Int) {
        val target = offset.coerceIn(earliestWeekOffset, 0)
        if (target == weekOffset) return
        weekOffset = target
        lifecycleScope.launch {
            displayedWeek = withContext(Dispatchers.IO) { computeWeek(target) }
        }
    }

    /** One week's state at [offset]; the live week needs today's usage, a past week doesn't. */
    private fun computeWeek(offset: Int): ZenGoldPromiseState {
        val promiseHours = PromisePreferences.getDailyHours(applicationContext)
        val todayMinutes = usageRepository.getTodayUsage().screenTimeInMillis / 60_000L
        return ZenGoldPromise.weekly(
            days = recapStore.days(),
            todayMinutes = todayMinutes,
            promiseHours = promiseHours,
            weekOffset = offset
        )
    }

    private fun refreshPromiseState() {
        lifecycleScope.launch {
            val (newWeekly, newMonthly) = withContext(Dispatchers.IO) {
                // WeeklyRecapWorker only records finished days every few hours, so yesterday can
                // still be missing just after midnight; record it now rather than show it undecided.
                RecapCollector(applicationContext, usageRepository, recapStore).backfill()
                val promiseHours = PromisePreferences.getDailyHours(applicationContext)
                val days = recapStore.days()
                val todayMinutes = usageRepository.getTodayUsage().screenTimeInMillis / 60_000L
                ZenGoldPromise.weekly(days, todayMinutes, promiseHours) to
                    ZenGoldPromise.monthly(days, todayMinutes, promiseHours, recapStore::recap)
            }
            weekly = newWeekly
            monthly = newMonthly

            // Only weeks with a full seven days on record can be shown; anything shorter
            // would draw undecided bars and read as a week the user never had.
            val (earliest, displayed) = withContext(Dispatchers.IO) {
                val oldest = recapStore.completedWeeks().minByOrNull { it.weekStart }?.weekStart
                val earliest = oldest?.let {
                    -(ChronoUnit.WEEKS.between(it, weekStartOf(LocalDate.now())).toInt())
                } ?: 0
                val clamped = weekOffset.coerceIn(earliest, 0)
                earliest to if (clamped == 0) newWeekly else computeWeek(clamped)
            }
            earliestWeekOffset = earliest
            weekOffset = weekOffset.coerceIn(earliest, 0)
            displayedWeek = displayed
        }
    }
}
