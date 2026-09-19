package com.zenlauncher.zenmode.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenlauncher.zenmode.testing.TestActivity
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInStepTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(onEmailSignIn: (String, String) -> Unit = { _, _ -> }) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                SignInStep(
                    progress = StepProgress(segments = 5, currentIndex = 1),
                    isLoading = false,
                    onBack = {},
                    onGoogleSignIn = {},
                    onEmailSignIn = onEmailSignIn,
                    onExplore = {}
                )
            }
        }
    }

    @Test
    fun signInStep_showsReviewerSignInToggle() {
        setContent()
        composeTestRule.onNodeWithText("Reviewer? Sign in here").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun signInStep_reviewerSignIn_passesTrimmedCredentials() {
        var credentials: Pair<String, String>? = null
        setContent(onEmailSignIn = { email, password -> credentials = email to password })

        composeTestRule.onNodeWithText("Reviewer? Sign in here").performScrollTo().performClick()
        composeTestRule.onNode(hasSetTextAction() and hasText("Email")).performTextInput(" reviewer@zenmode.app ")
        composeTestRule.onNode(hasSetTextAction() and hasText("Password")).performTextInput("secret")
        composeTestRule.onNodeWithText("Sign in").performScrollTo().performClick()

        assertEquals("reviewer@zenmode.app" to "secret", credentials)
    }
}
