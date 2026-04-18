package com.zenlauncher.zenmode.screen.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zenlauncher.zenmode.ui.screens.UsageAccessPermissionScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class UsageAccessPermissionScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        onGrantAccessClick: () -> Unit = {},
        onBackClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                UsageAccessPermissionScreen(
                    onGrantAccessClick = onGrantAccessClick,
                    onBackClick = onBackClick
                )
            }
        }
    }

    @Test
    fun usageAccess_rendersWithoutCrash() {
        setContent()
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
    }

    @Test
    fun usageAccess_showsGrantButton() {
        setContent()
        composeTestRule.onNodeWithText("Grant access").assertIsDisplayed()
    }

    @Test
    fun usageAccess_grantButton_callsCallback() {
        var clicked = false
        setContent(onGrantAccessClick = { clicked = true })
        composeTestRule.onNodeWithText("Grant access").performClick()
        assertTrue(clicked)
    }

    @Test
    fun usageAccess_backButton_callsCallback() {
        var clicked = false
        setContent(onBackClick = { clicked = true })
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(clicked)
    }
}
