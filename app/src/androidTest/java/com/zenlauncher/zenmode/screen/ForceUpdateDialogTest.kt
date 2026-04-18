package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zenlauncher.zenmode.ui.screens.ForceUpdateDialog
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ForceUpdateDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        onUpdateClick: () -> Unit = {},
        onTomorrowClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                ForceUpdateDialog(
                    onUpdateClick = onUpdateClick,
                    onTomorrowClick = onTomorrowClick
                )
            }
        }
    }

    @Test
    fun forceUpdateDialog_showsTitle() {
        setContent()
        composeTestRule.onNodeWithText("Update Required").assertIsDisplayed()
    }

    @Test
    fun forceUpdateDialog_showsDescription() {
        setContent()
        composeTestRule.onNodeWithText("A new version of ZenMode is required", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun forceUpdateDialog_showsUpdateButton() {
        setContent()
        composeTestRule.onNodeWithText("Update Now").assertIsDisplayed()
    }

    @Test
    fun forceUpdateDialog_showsTomorrowButton() {
        setContent()
        composeTestRule.onNodeWithText("Tomorrow").assertIsDisplayed()
    }

    @Test
    fun forceUpdateDialog_updateClick_callsCallback() {
        var clicked = false
        setContent(onUpdateClick = { clicked = true })
        composeTestRule.onNodeWithText("Update Now").performClick()
        assertTrue(clicked)
    }

    @Test
    fun forceUpdateDialog_tomorrowClick_callsCallback() {
        var clicked = false
        setContent(onTomorrowClick = { clicked = true })
        composeTestRule.onNodeWithText("Tomorrow").performClick()
        assertTrue(clicked)
    }
}
