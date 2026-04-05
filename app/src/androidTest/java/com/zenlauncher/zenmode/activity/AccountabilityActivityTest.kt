package com.zenlauncher.zenmode.activity

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zenlauncher.zenmode.AccountabilityActivity
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountabilityActivityTest {

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("zen_mode_stats", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_onboarding_complete", true).commit()
    }

    @Test
    fun accountabilityActivity_launches_successfully() {
        val scenario = ActivityScenario.launch(AccountabilityActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }
}
