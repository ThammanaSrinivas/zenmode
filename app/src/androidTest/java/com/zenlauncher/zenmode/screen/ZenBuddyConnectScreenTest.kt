package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.zenlauncher.zenmode.ui.screens.BuddyAddResult
import com.zenlauncher.zenmode.ui.screens.ZenBuddyConnectContent
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ZenBuddyConnectScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        userCode: String? = "USER123",
        onCopyCode: () -> Unit = {},
        onAddBuddy: suspend (String) -> BuddyAddResult = { BuddyAddResult.Error("test") },
        onRandomConnect: () -> Unit = {},
        onWatchVideo: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                ZenBuddyConnectContent(
                    userCode = userCode,
                    onCopyCode = onCopyCode,
                    onAddBuddy = onAddBuddy,
                    onRandomConnect = onRandomConnect,
                    onWatchVideo = onWatchVideo
                )
            }
        }
    }

    @Test
    fun buddyConnect_showsTitle() {
        setContent()
        composeTestRule.onNodeWithText("Zen Buddy Connect").assertIsDisplayed()
    }

    @Test
    fun buddyConnect_showsUserCode() {
        // User code appears in title row and as input placeholder, so use onAllNodes
        setContent(userCode = "ABC456")
        composeTestRule.onAllNodesWithText("ABC456", substring = true)[0].assertIsDisplayed()
    }

    @Test
    fun buddyConnect_showsBuddyCodeSection() {
        setContent()
        composeTestRule.onNodeWithText("ENTER YOUR BUDDY'S CODE:").assertIsDisplayed()
    }

    @Test
    fun buddyConnect_showsRandomConnectSection() {
        setContent()
        composeTestRule.onNodeWithText("Single Forever? No Worries", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun buddyConnect_showsVideoLink() {
        setContent()
        composeTestRule.onNodeWithText("Watch video", substring = true).assertIsDisplayed()
    }
}
