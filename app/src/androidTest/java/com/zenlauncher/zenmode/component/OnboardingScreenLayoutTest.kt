package com.zenlauncher.zenmode.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zenlauncher.zenmode.ui.components.OnboardingScreenLayout
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class OnboardingScreenLayoutTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        progress: Float = 0.5f,
        progressText: String = "50%",
        buttonText: String = "Continue",
        onButtonClick: () -> Unit = {},
        onBackClick: (() -> Unit)? = null
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                OnboardingScreenLayout(
                    progress = progress,
                    progressText = progressText,
                    buttonText = buttonText,
                    onButtonClick = onButtonClick,
                    onBackClick = onBackClick
                ) {
                    Text("Test Content")
                }
            }
        }
    }

    @Test
    fun layout_showsProgressText() {
        setContent(progressText = "75%")
        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
    }

    @Test
    fun layout_showsButtonText() {
        setContent(buttonText = "Grant access")
        composeTestRule.onNodeWithText("Grant access").assertIsDisplayed()
    }

    @Test
    fun layout_buttonClick_callsCallback() {
        var clicked = false
        setContent(
            buttonText = "Next",
            onButtonClick = { clicked = true }
        )
        composeTestRule.onNodeWithText("Next").performClick()
        assertTrue(clicked)
    }

    @Test
    fun layout_showsBackButton_whenProvided() {
        setContent(onBackClick = {})
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun layout_backClick_callsCallback() {
        var clicked = false
        setContent(onBackClick = { clicked = true })
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(clicked)
    }

    @Test
    fun layout_hidesBackButton_whenNull() {
        setContent(onBackClick = null)
        composeTestRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }
}
