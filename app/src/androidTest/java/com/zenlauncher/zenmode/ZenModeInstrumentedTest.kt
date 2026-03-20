package com.zenlauncher.zenmode

import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZenModeInstrumentedTest {

    @Before
    fun setup() {
        // Set onboarding as complete so MainActivity doesn't redirect
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("zen_mode_stats", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_onboarding_complete", true).commit()
        
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun mainActivity_launches_and_shows_core_elements() {
        val scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
        
        // Check if RecyclerView for apps is displayed
        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()))

        // Check if Widget Slots are displayed (placeholders for Stats)
        onView(withId(R.id.widget_slot_1)).check(matches(isDisplayed()))
        onView(withId(R.id.widget_slot_2)).check(matches(isDisplayed()))

        // Check Bottom Dock Icons
        onView(withId(R.id.iv_settings)).check(matches(isDisplayed()))
        onView(withId(R.id.iv_google)).check(matches(isDisplayed()))
        onView(withId(R.id.iv_phone)).check(matches(isDisplayed()))
        
        scenario.close()
    }

    @Test
    fun settings_icon_click_navigates_to_settings_activity() {
        val scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
        
        onView(withId(R.id.iv_settings)).perform(click())
        intended(hasComponent(SettingsActivity::class.java.name))
        
        scenario.close()
    }

    @Test
    fun phone_icon_click_launches_dialer() {
        val scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
        
        // Stub the intent to prevent actual dialer from opening (optional but good practice)
        intending(hasAction(Intent.ACTION_DIAL)).respondWith(Instrumentation.ActivityResult(0, null))

        onView(withId(R.id.iv_phone)).perform(click())
        intended(hasAction(Intent.ACTION_DIAL))
        
        scenario.close()
    }
}
