package com.zenlauncher.zenmode

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

class DelayedUnlockActivity : AppCompatActivity() {

    private lateinit var progressCountdown: ProgressBar
    private lateinit var txtCountdown: TextView
    private var timer: CountDownTimer? = null
    private lateinit var repository: UsageRepository
    
    // Timer settings
    private val totalTime = 7000L // 7 seconds
    private val interval = 1000L // 1 second

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delayed_unlock)

        val analyticsManager = ServiceLocator.analyticsManager
        repository = UsageRepository(this, analyticsManager)
        
        val tracker = ServiceLocator.analyticsTracker
        tracker.trackScreenUnlockStarted("user_action") // Default trigger for now as intent source isn't explicit

        // Initialize Views
        progressCountdown = findViewById(R.id.progress_countdown)
        txtCountdown = findViewById(R.id.txt_countdown)

        // Bind Stats
        bindStats()

        // Setup Action Buttons
        findViewById<android.view.View>(R.id.btn_settings).setOnClickListener {
            ServiceLocator.analyticsTracker.trackMindfulUnlockSkipped(
                getCurrentWaitedSec(), "settings"
            )
            startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
            finishUnlock()
        }

        findViewById<android.view.View>(R.id.btn_google).setOnClickListener {
            ServiceLocator.analyticsTracker.trackMindfulUnlockSkipped(
                getCurrentWaitedSec(), "google"
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH)
            startActivity(intent)
            finishUnlock()
        }

        findViewById<android.view.View>(R.id.btn_phone).setOnClickListener {
            ServiceLocator.analyticsTracker.trackMindfulUnlockSkipped(
                getCurrentWaitedSec(), "phone"
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
            startActivity(intent)
            finishUnlock()
        }

        findViewById<android.view.View>(R.id.btn_skip).setOnClickListener {
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

        // Back button handler (replaces deprecated onBackPressed)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                ServiceLocator.analyticsTracker.trackMindfulUnlockDismissed(getCurrentWaitedSec())
                finish()
            }
        })

        // Start Countdown
        startCountdown()
    }

    private fun bindStats() {
        // Use Repository
        val usage = repository.getTodayUsage()
        val unlocks = usage.unlocks
        val totalMillis = usage.screenTimeInMillis

        // Screen Time (Minutes only as requested in other task, keeping pattern)
        val minutes = (totalMillis / 1000) / 60
        
        // Update Screen Time Stat
        findViewById<TextView>(R.id.txt_screen_time_stats).text = "${minutes}/${AppConstants.THRESHOLD_NEUTRAL_MINUTES}"
        findViewById<ProgressBar>(R.id.progress_screen_time).progress = ((minutes.toFloat() / AppConstants.THRESHOLD_NEUTRAL_MINUTES.toFloat()) * 100).toInt()

        // Update Unlocks Stat
        findViewById<TextView>(R.id.txt_unlocks_stats).text = "${unlocks}/${AppConstants.GOAL_UNLOCKS_COUNT}"
        findViewById<ProgressBar>(R.id.progress_unlocks).progress = ((unlocks.toFloat() / AppConstants.GOAL_UNLOCKS_COUNT.toFloat()) * 100).toInt()
        
        // Mindfulness Logic
        val mindfulnessProgress = AppLogic.getMindfulnessPercentage(minutes)
        val mindfulnessColorRes = AppLogic.getMindfulnessColor(minutes)
        val mindfulnessColor = androidx.core.content.ContextCompat.getColor(this, mindfulnessColorRes)
        
        // Bind Custom Segmented Bar
        val segmentedBar = findViewById<SegmentedProgressBar>(R.id.progress_mindfulness)
        segmentedBar.setProgress(mindfulnessProgress)
        segmentedBar.setFilledColor(mindfulnessColor)

        // Dynamic UI Logic
        val rootLayout = findViewById<android.widget.LinearLayout>(R.id.root_layout)
        val imgFace = findViewById<android.widget.ImageView>(R.id.img_face_large)
        val txtAdvice = findViewById<TextView>(R.id.txt_advice)


        val colorRes: Int
        val faceRes: Int
        val message: String

        when (AppLogic.getMoodState(minutes)) {
            MoodState.HAPPY -> {
                // Happy State
                colorRes = androidx.core.content.ContextCompat.getColor(this, R.color.zen_green)
                faceRes = R.drawable.face_happy
                message = "You're being so mindful Today!💚"
            }
            MoodState.NEUTRAL -> {
                // Neutral State
                colorRes = androidx.core.content.ContextCompat.getColor(this, R.color.zen_yellow)
                faceRes = R.drawable.face_neutral
                message = "Free advice: Don't lose your peace"
            }
            MoodState.ANNOYED -> {
                // Annoyed State
                colorRes = androidx.core.content.ContextCompat.getColor(this, R.color.zen_red)
                faceRes = R.drawable.face_annoyed
                message = "Put the phone down. Live.❤️"
            }
        }

        rootLayout.setBackgroundColor(colorRes)
        imgFace.setImageResource(faceRes)
        txtAdvice.text = message
    }

    private fun startCountdown() {
        // Reset color of countdown if needed or keep it standard
        progressCountdown.max = 7
        
        timer = object : CountDownTimer(totalTime, interval) {
            override fun onTick(millisUntilFinished: Long) {
                // Determine seconds passed (1 to 7)
                // millisUntilFinished counts down from 7000 to 0
                // We want to show 1, 2, 3, 4, 5, 6, 7
                
                val secondsRemaining = millisUntilFinished / 1000
                val check = 7 - secondsRemaining
                
                // Adjustment for UI feeling natural
                val displayValue = if (check < 1) 1 else check.toInt()

                txtCountdown.text = displayValue.toString()
                progressCountdown.progress = displayValue
            }

            override fun onFinish() {
                txtCountdown.text = "7"
                progressCountdown.progress = 7
                finishUnlock()
            }
        }.start()
    }
    
    private fun getCurrentWaitedSec(): Long {
        // totalTime is 7000L
        val progress = progressCountdown.progress // 1 to 7
        return progress.toLong()
    }
    

    
    private fun finishUnlock() {
        val tracker = ServiceLocator.analyticsTracker
        if (progressCountdown.progress >= 7) {
            tracker.trackMindfulUnlockCompleted(7)
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
        // Hides the "Home" and "Recents" buttons using WindowInsetsControllerCompat (API 35 compatible)
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
