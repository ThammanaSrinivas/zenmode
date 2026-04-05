package com.zenlauncher.zenmode.screen.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zenlauncher.zenmode.ui.screens.AccessibilityServiceScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class AccessibilityServiceScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        onGrantAccessClick: () -> Unit = {},
        onSkipClick: () -> Unit = {},
        onBackClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                AccessibilityServiceScreen(
                    onGrantAccessClick = onGrantAccessClick,
                    onSkipClick = onSkipClick,
                    onBackClick = onBackClick
                )
            }
        }
    }

    @Test
    fun accessibility_rendersWithoutCrash() {
        setContent()
        composeTestRule.onNodeWithText("83%").assertIsDisplayed()
    }

    @Test
    fun accessibility_showsGrantButton() {
        setContent()
        composeTestRule.onNodeWithText("Grant access").assertIsDisplayed()
    }

    @Test
    fun accessibility_grantButton_callsCallback() {
        var clicked = false
        setContent(onGrantAccessClick = { clicked = true })
        composeTestRule.onNodeWithText("Grant access").performClick()
        assertTrue(clicked)
    }

    @Test
    fun accessibility_backButton_callsCallback() {
        var clicked = false
        setContent(onBackClick = { clicked = true })
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(clicked)
    }

    @Test
    fun accessibility_showsSkipOption() {
        setContent()
        composeTestRule.onNodeWithText("Skip for now").assertIsDisplayed()
    }
}
