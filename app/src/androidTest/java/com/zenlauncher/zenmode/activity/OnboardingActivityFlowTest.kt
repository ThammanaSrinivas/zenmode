package com.zenlauncher.zenmode.activity

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.zenlauncher.zenmode.OnboardingActivity
import com.zenlauncher.zenmode.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingActivityFlowTest {

    @Test
    fun onboarding_launches_successfully() {
        val scenario = ActivityScenario.launch(OnboardingActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun onboarding_startsOnFirstPage() {
        val scenario = ActivityScenario.launch(OnboardingActivity::class.java)
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            assertNotNull(viewPager)
            assertEquals(0, viewPager.currentItem)
        }
        scenario.close()
    }

    @Test
    fun onboarding_hasCorrectPageCount() {
        val scenario = ActivityScenario.launch(OnboardingActivity::class.java)
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            assertNotNull(viewPager)
            assertNotNull(viewPager.adapter)
            assertEquals(6, viewPager.adapter!!.itemCount)
        }
        scenario.close()
    }
}
