package com.zenlauncher.zenmode

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.zenlauncher.zenmode.coreapi.AppCategory
import com.zenlauncher.zenmode.coreapi.PhoneSession
import com.zenlauncher.zenmode.coreapi.SessionLogRepository
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.ZenScore
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.recap.ProUpsellSheet
import com.zenlauncher.zenmode.recap.RecapReport
import com.zenlauncher.zenmode.recap.RecapStore
import com.zenlauncher.zenmode.recap.formatMinutes
import com.zenlauncher.zenmode.ui.screens.ZenScoreCategory
import com.zenlauncher.zenmode.ui.screens.ZenScoreScreen
import com.zenlauncher.zenmode.ui.screens.ZenSessionLogEntry
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * The left-hand home page (Figma node 2026:2035): reached by swiping right on Home or tapping
 * Home's Zen Score. A standalone Activity, same reasoning as [ZenGoldActivity]: MainActivity's
 * own composition/state stays untouched by it.
 */
class ZenScoreActivity : AppCompatActivity() {

    private var showProSheet by mutableStateOf(false)
    /** The surface that opened the upsell sheet, for analytics on the plan page. */
    private var proSheetSource = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HomePageSide.LEFT.applyOnCreate(this)

        val repository = UsageRepository(this, ServiceLocator.analyticsManager)
        val sessionLogRepository = SessionLogRepository(this, repository)
        val scores = ZenScoreStore(this, repository, sessionLogRepository)

        val score = scores.refresh()
        val yesterday = scores.yesterday()
        val auth = ServiceLocator.authProvider

        val sessions = sessionLogRepository.getTodaySessions()
        val categories = categoryBreakdown(sessionLogRepository)
        // Newest first, so the latest session is visible without scrolling
        // (getTodaySessions() itself returns oldest-first).
        val sessionLog = sessions.asReversed().map(::toLogEntry)
        // Home's number, not the rows' sum: the rows can't itemise a day the OEM only reports
        // in aggregate, and the total must read the same on every screen.
        val totalMins = TimeUnit.MILLISECONDS.toMinutes(repository.getTodayUsage().screenTimeInMillis)
        val sessionTotalLabel =
            "TODAY · ${formatMinutes(totalMins)} · ${sessions.size} SESSION${if (sessions.size == 1) "" else "S"}"
        val reclaimedMinutes = reclaimedMinutesToday(repository)

        ServiceLocator.analyticsTracker.trackDailyScreentimeViewed(totalMins, 0)

        setContent {
            ZenTheme {
                val isPro = ProAccess.isProState(this@ZenScoreActivity)
                ZenScoreScreen(
                    score = score,
                    yesterdayScore = yesterday,
                    categories = categories,
                    reclaimedMinutes = reclaimedMinutes,
                    sessionTotalLabel = sessionTotalLabel,
                    sessionLog = sessionLog,
                    userName = auth.getDisplayName(),
                    photoUrl = auth.getPhotoUrl(),
                    isPro = isPro,
                    onBackClick = { finish() },
                    onZenGoldClick = {
                        finish()
                        startActivity(Intent(this@ZenScoreActivity, ZenGoldActivity::class.java))
                    },
                    onUpgradeProClick = { openProSheet("zen_score_header") },
                    onDownloadReportClick = {
                        if (isPro) downloadLatestReport() else openProSheet("zen_score_report")
                    }
                )
                if (showProSheet) {
                    ProUpsellSheet(
                        onDismiss = { showProSheet = false },
                        canPurchase = ProAccess.canPurchase,
                        onUpgrade = {
                            showProSheet = false
                            ProAccess.openUpgrade(this@ZenScoreActivity, proSheetSource)
                        },
                        // Only where Pro can't be bought: otherwise test the real flow.
                        onEnableForTesting = if (ProAccess.canUseDebugOverride && !ProAccess.canPurchase) {
                            {
                                ProAccess.setDebugOverride(this@ZenScoreActivity, true)
                                showProSheet = false
                            }
                        } else null
                    )
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        HomePageSide.LEFT.applyOnFinish(this)
    }

    /** The four category legend rows, in the app's existing label/colour convention. */
    private fun categoryBreakdown(sessionLogRepository: SessionLogRepository): List<ZenScoreCategory> =
        sessionLogRepository.getCategoryBreakdownPercent().map { (category, percent) ->
            val (label, colorRes) = when (category) {
                AppCategory.FOCUS -> "Productivity" to R.color.zen_900
                AppCategory.ENTERTAINMENT -> "Entertainment" to R.color.score_status_red
                AppCategory.COMMUNICATION -> "Messaging" to R.color.score_category_messaging
                AppCategory.OTHER -> "Everything else" to R.color.zen_300
            }
            ZenScoreCategory(label, percent, colorRes)
        }

    private fun toLogEntry(session: PhoneSession) = ZenSessionLogEntry(
        time = DateFormat.getTimeFormat(this).format(Date(session.startMillis)),
        appName = session.dominantLabel,
        otherApps = (session.appCount - 1).coerceAtLeast(0),
        duration = formatSessionDuration(session.durationMillis),
        type = session.eventType
    )

    /** "4m", "1h 05m" — plain minutes like the rest of the screen; under a minute reads "<1m". */
    private fun formatSessionDuration(millis: Long): String =
        if (millis < 60_000L) "<1m" else formatMinutes(TimeUnit.MILLISECONDS.toMinutes(millis))

    /** Minutes won back today vs. yesterday's screen time at this same point, floored at 0. */
    private fun reclaimedMinutesToday(repository: UsageRepository): Int {
        val yesterdayMillis = repository.getYesterdayScreenTimeMillis()
        val todayMillis = repository.getTodayUsage().screenTimeInMillis
        return TimeUnit.MILLISECONDS.toMinutes((yesterdayMillis - todayMillis).coerceAtLeast(0)).toInt()
    }

    private fun openProSheet(source: String) {
        ServiceLocator.analyticsTracker.trackProUpsellViewed(source)
        proSheetSource = source
        showProSheet = true
    }

    /**
     * The report PDF is weekly: saves the most recent finished week to Downloads and opens it.
     * Android 9 has no direct Downloads write, so there the full list in Settings handles it.
     */
    private fun downloadLatestReport() {
        lifecycleScope.launch {
            val recap = withContext(Dispatchers.IO) { RecapStore(applicationContext).completedWeeks().firstOrNull() }
            if (recap == null) {
                toast("Your first weekly report arrives after your first full week.")
                return@launch
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                startActivity(Intent(this@ZenScoreActivity, SettingsActivity::class.java))
                return@launch
            }
            val uri = withContext(Dispatchers.IO) {
                runCatching { RecapReport.saveToDownloads(applicationContext, recap) }.getOrNull()
            }
            if (uri == null) {
                toast("Couldn't save the report. Please try again.")
                return@launch
            }
            ServiceLocator.analyticsTracker.trackReportDownloaded(recap.weekStart.toString())
            toast("Report saved to Downloads")
            RecapReport.open(this@ZenScoreActivity, uri)
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
