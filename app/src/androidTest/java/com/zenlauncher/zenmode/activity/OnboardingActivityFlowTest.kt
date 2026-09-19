package com.zenlauncher.zenmode.activity

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenlauncher.zenmode.OnboardingActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The onboarding is a Compose flow (onboarding/OnboardingFlow.kt), not a ViewPager.
 * Step routing is unit-tested in OnboardingFlowTest; this checks the activity boots
 * into it and puts the product on screen.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingActivityFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<OnboardingActivity>()

    @Test
    fun onboarding_launches_successfully() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun onboarding_showsZenModeOnFirstScreen() {
        composeTestRule.waitForIdle()
        val nodes = composeTestRule
            .onAllNodes(hasText("ZenMode", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Expected the first onboarding screen to name ZenMode", nodes.isNotEmpty())
    }
}
