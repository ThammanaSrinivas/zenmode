package com.zenlauncher.zenmode

import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import com.zenlauncher.zenmode.testing.TestActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class MinimalComposeTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun simpleText_isDisplayed() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent {
                Text("Hello Test")
            }
        }
        composeTestRule.onNodeWithText("Hello Test").assertIsDisplayed()
        scenario.close()
    }
}
