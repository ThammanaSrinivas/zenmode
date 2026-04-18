package com.zenlauncher.zenmode.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zenlauncher.zenmode.ui.components.ZenSettingToggleItem
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ZenSettingToggleItemTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun toggle_displaysLabel() {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                ZenSettingToggleItem(
                    text = "Dark Mode",
                    checked = false,
                    onCheckedChange = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Dark Mode").assertIsDisplayed()
    }

    @Test
    fun toggle_reflectsCheckedState_on() {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                ZenSettingToggleItem(
                    text = "Dark Mode",
                    checked = true,
                    onCheckedChange = {}
                )
            }
        }
        composeTestRule.onNode(
            androidx.compose.ui.test.isToggleable()
        ).assertIsOn()
    }

    @Test
    fun toggle_reflectsCheckedState_off() {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                ZenSettingToggleItem(
                    text = "Dark Mode",
                    checked = false,
                    onCheckedChange = {}
                )
            }
        }
        composeTestRule.onNode(
            androidx.compose.ui.test.isToggleable()
        ).assertIsOff()
    }

    @Test
    fun toggle_callsOnCheckedChange_whenClicked() {
        var newValue: Boolean? = null
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                ZenSettingToggleItem(
                    text = "Dark Mode",
                    checked = false,
                    onCheckedChange = { newValue = it }
                )
            }
        }
        composeTestRule.onNode(
            androidx.compose.ui.test.isToggleable()
        ).performClick()
        assertTrue(newValue == true)
    }
}
