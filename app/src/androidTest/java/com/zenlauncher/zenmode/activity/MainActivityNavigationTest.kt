package com.zenlauncher.zenmode.activity

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zenlauncher.zenmode.MainActivity
import com.zenlauncher.zenmode.OnboardingActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityNavigationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("zen_mode_stats", Context.MODE_PRIVATE)
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
    fun mainActivity_redirectsToOnboarding_whenNotComplete() {
        val prefs = context.getSharedPreferences("zen_mode_stats", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_onboarding_complete", false).commit()

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        // Activity finishes quickly and gets destroyed, so check state
        Thread.sleep(2000)
        assertTrue(scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED)
        scenario.close()

        // Restore for other tests
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

    @Test
    fun mainActivity_launchesWithBuddyConnectExtra() {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("SHOW_BUDDY_CONNECT", true)
        }
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun mainActivity_launchesWithBuddyBattleExtra() {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("SHOW_BUDDY_BATTLE", true)
        }
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }
}
