package com.zenlauncher.zenmode

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.recap.ProUpsellSheet
import com.zenlauncher.zenmode.recap.RecapReport
import com.zenlauncher.zenmode.recap.RecapStore
import com.zenlauncher.zenmode.ui.screens.ZenScoreScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The left-hand home page (Figma node 2026:2035): reached by swiping right on Home or tapping
 * Home's Zen Score. A standalone Activity, same reasoning as [ZenGoldActivity]: MainActivity's
 * own composition/state stays untouched by it.
 */
class ZenScoreActivity : AppCompatActivity() {

    private var showProSheet by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HomePageSide.LEFT.applyOnCreate(this)

        val scores = ZenScoreStore(this, UsageRepository(applicationContext, ServiceLocator.analyticsManager))
        val score = scores.refresh()
        val yesterday = scores.yesterday()
        val auth = ServiceLocator.authProvider

        setContent {
            ZenTheme(darkTheme = ThemePreferences.isDarkMode(this@ZenScoreActivity)) {
                val isPro = ProAccess.isProState(this@ZenScoreActivity)
                ZenScoreScreen(
                    score = score,
                    yesterdayScore = yesterday,
                    userName = auth.getDisplayName(),
                    photoUrl = auth.getPhotoUrl(),
                    isPro = isPro,
                    onBackClick = { finish() },
                    onUpgradeProClick = { openProSheet("zen_score_header") },
                    onDownloadReportClick = {
                        if (isPro) downloadLatestReport() else openProSheet("zen_score_report")
                    },
                    onShareScoreClick = { shareScore(score) }
                )
                if (showProSheet) {
                    ProUpsellSheet(
                        onDismiss = { showProSheet = false },
                        onRequestAccess = {
                            showProSheet = false
                            ProAccess.requestAccess(this@ZenScoreActivity)
                        },
                        onEnableForTesting = if (ProAccess.canUseDebugOverride) {
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

    private fun openProSheet(source: String) {
        ServiceLocator.analyticsTracker.trackProUpsellViewed(source)
        showProSheet = true
    }

    private fun shareScore(score: Int) {
        val text = "My Zen Score today is ${ZenScore.format(score)}/${ZenScore.MAX_DISPLAY} on ZenMode OS — " +
            "less scrolling, more living. zenmodeos.com"
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, "Share Zen Score"))
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
            openPdf(uri)
        }
    }

    private fun openPdf(uri: Uri) {
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(view)
        } catch (_: android.content.ActivityNotFoundException) {
            // No PDF viewer installed; the file is still in Downloads.
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
