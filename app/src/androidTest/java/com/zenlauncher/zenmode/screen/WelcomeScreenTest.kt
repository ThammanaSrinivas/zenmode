package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zenlauncher.zenmode.ui.screens.WelcomeScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        onGoogleSignInClick: () -> Unit = {},
        onEmailSignInClick: (String, String) -> Unit = { _, _ -> }
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                WelcomeScreen(
                    onGoogleSignInClick = onGoogleSignInClick,
                    onEmailSignInClick = onEmailSignInClick
                )
            }
        }
    }

    @Test
    fun welcomeScreen_rendersWithoutCrash() {
        setContent()
        // Wait for animations to reach the final state
        composeTestRule.mainClock.advanceTimeBy(5000)
        composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
    }

    @Test
    fun welcomeScreen_showsGoogleSignInButton() {
        setContent()
        composeTestRule.mainClock.advanceTimeBy(5000)
        composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
    }

    @Test
    fun welcomeScreen_googleSignIn_callsCallback() {
        var clicked = false
        setContent(onGoogleSignInClick = { clicked = true })
        composeTestRule.mainClock.advanceTimeBy(5000)
        composeTestRule.onNodeWithText("Sign in with Google").performClick()
        assertTrue(clicked)
    }

    @Test
    fun welcomeScreen_showsReviewerSignInToggle() {
        setContent()
        composeTestRule.mainClock.advanceTimeBy(5000)
        composeTestRule.onNodeWithText("Reviewer? Sign in here").assertIsDisplayed()
    }

    @Test
    fun welcomeScreen_showsZenModeBranding() {
        setContent()
        composeTestRule.mainClock.advanceTimeBy(5000)
        composeTestRule.onNodeWithText("ZENMODE").assertIsDisplayed()
    }
}
