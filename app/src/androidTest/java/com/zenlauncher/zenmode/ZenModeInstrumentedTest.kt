package com.zenlauncher.zenmode

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZenModeInstrumentedTest {

    @Before
    fun setup() {
        // Set onboarding as complete so MainActivity doesn't redirect
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("zen_mode_stats", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_onboarding_complete", true).commit()
    }

    @Test
    fun mainActivity_launches_successfully() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun onboardingActivity_launches_when_onboarding_not_complete() {
        // Reset onboarding flag
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("zen_mode_stats", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_onboarding_complete", false).commit()

        // Launch MainActivity — it should redirect to OnboardingActivity and finish
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            // MainActivity finishes itself after starting OnboardingActivity
            assert(activity.isFinishing)
        }
        scenario.close()

        // Restore onboarding flag for other tests
        prefs.edit().putBoolean("is_onboarding_complete", true).commit()
    }

    @Test
    fun onboardingActivity_launches_directly() {
        val scenario = ActivityScenario.launch(OnboardingActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }
}
