package com.zenlauncher.zenmode.screen.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zenlauncher.zenmode.ui.screens.DefaultLauncherScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class DefaultLauncherScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        onSetDefaultLauncherClick: () -> Unit = {},
        onShareClick: () -> Unit = {},
        onBackClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                DefaultLauncherScreen(
                    onSetDefaultLauncherClick = onSetDefaultLauncherClick,
                    onShareClick = onShareClick,
                    onBackClick = onBackClick
                )
            }
        }
    }

    @Test
    fun defaultLauncher_rendersWithoutCrash() {
        setContent()
        composeTestRule.onNodeWithText("99%").assertIsDisplayed()
    }

    @Test
    fun defaultLauncher_showsSetDefaultButton() {
        setContent()
        composeTestRule.onNodeWithText("Set as default Launcher").assertIsDisplayed()
    }

    @Test
    fun defaultLauncher_setDefaultButton_callsCallback() {
        var clicked = false
        setContent(onSetDefaultLauncherClick = { clicked = true })
        composeTestRule.onNodeWithText("Set as default Launcher").performClick()
        assertTrue(clicked)
    }

    @Test
    fun defaultLauncher_backButton_callsCallback() {
        var clicked = false
        setContent(onBackClick = { clicked = true })
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(clicked)
    }
}
