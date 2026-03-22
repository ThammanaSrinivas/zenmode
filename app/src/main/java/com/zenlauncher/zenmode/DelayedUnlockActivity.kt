package com.zenlauncher.zenmode

import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.ResistenceScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

class DelayedUnlockActivity : AppCompatActivity() {

    private var timer: CountDownTimer? = null
    private lateinit var repository: UsageRepository

    // Timer settings
    private val totalTimeMs = AppConstants.COUNTDOWN_SECONDS * 1000L
    private val intervalMs = 1000L

    // Compose state
    private var usage by mutableStateOf<DailyUsage?>(null)
    private var countdownSeconds by mutableIntStateOf(1)
    private var countdownFinished by mutableStateOf(false)
    private var currentProgress by mutableIntStateOf(1)
    private var skipsLeft by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val analyticsManager = ServiceLocator.analyticsManager
        repository = UsageRepository(this, analyticsManager)

        val tracker = ServiceLocator.analyticsTracker
        tracker.trackScreenUnlockStarted("user_action")

        // Load stats
        usage = repository.getTodayUsage()
        skipsLeft = (AppConstants.MAX_DAILY_SKIPS - repository.getTodaySkipCount()).coerceAtLeast(0)

        // Back button handler
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                ServiceLocator.analyticsTracker.trackMindfulUnlockDismissed(getCurrentWaitedSec())
                finish()
            }
        })

        // Start Countdown
        startCountdown()

        setContent {
            ZenTheme(darkTheme = true) {
                ResistenceScreen(
                    usage = usage,
                    streaks = 0, // TODO: wire up streak tracking
                    yesterdayChangePercent = null, // TODO: wire up yesterday comparison
                    skipsLeft = skipsLeft,
                    countdownSeconds = countdownSeconds,
                    countdownFinished = countdownFinished,
                    onSettingsClick = {
                        ServiceLocator.analyticsTracker.trackMindfulUnlockSkipped(
                            getCurrentWaitedSec(), "settings"
                        )
                        startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
                        finishUnlock()
                    },
                    onPhoneClick = {
                        ServiceLocator.analyticsTracker.trackMindfulUnlockSkipped(
                            getCurrentWaitedSec(), "phone"
                        )
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                        startActivity(intent)
                        finishUnlock()
                    },
                    onSkipClick = {
                        if (skipsLeft > 0) {
                            repository.incrementSkipCount()
                            skipsLeft = (AppConstants.MAX_DAILY_SKIPS - repository.getTodaySkipCount()).coerceAtLeast(0)
                            ServiceLocator.analyticsTracker.trackMindfulUnlockSkipped(
                                getCurrentWaitedSec(), "skip_button"
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                addCategory(android.content.Intent.CATEGORY_HOME)
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                            finishUnlock()
                        }
                    }
                )
            }
        }
    }

    private fun startCountdown() {
        timer = object : CountDownTimer(totalTimeMs, intervalMs) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                val check = AppConstants.COUNTDOWN_SECONDS - secondsRemaining
                val displayValue = if (check < 1) 1 else check.toInt()

                countdownSeconds = displayValue
                currentProgress = displayValue
            }

            override fun onFinish() {
                countdownSeconds = AppConstants.COUNTDOWN_SECONDS
                currentProgress = AppConstants.COUNTDOWN_SECONDS
                countdownFinished = true
                finishUnlock()
            }
        }.start()
    }

    private fun getCurrentWaitedSec(): Long {
        return currentProgress.toLong()
    }

    private fun finishUnlock() {
        val tracker = ServiceLocator.analyticsTracker
        if (currentProgress >= AppConstants.COUNTDOWN_SECONDS) {
            tracker.trackMindfulUnlockCompleted(AppConstants.COUNTDOWN_SECONDS)
        }

        repository.setZenUnlockFlag(true)

        timer?.cancel()
        finish()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
