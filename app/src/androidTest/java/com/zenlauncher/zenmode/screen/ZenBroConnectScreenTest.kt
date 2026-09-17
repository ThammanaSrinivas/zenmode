package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenlauncher.zenmode.testing.TestActivity
import com.zenlauncher.zenmode.ui.screens.BuddyAddResult
import com.zenlauncher.zenmode.ui.screens.ZenBroConnectScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZenBroConnectScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        userCode: String? = "USER1234567",
        onBackClick: () -> Unit = {},
        onShareLink: () -> Unit = {},
        onCopyCode: () -> Unit = {},
        onAddBuddy: suspend (String) -> BuddyAddResult = { BuddyAddResult.Error("test") },
        onRandomConnect: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                ZenBroConnectScreen(
                    userCode = userCode,
                    onBackClick = onBackClick,
                    onShareLink = onShareLink,
                    onCopyCode = onCopyCode,
                    onAddBuddy = onAddBuddy,
                    onRandomConnect = onRandomConnect
                )
            }
        }
    }

    @Test
    fun zenBroConnect_showsHeaderAndAllThreeOptions() {
        setContent()
        composeTestRule.onNodeWithText("My Zen Circle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connect with your Zen Bro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Share a link").assertIsDisplayed()
        composeTestRule.onNodeWithText("Use a code").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Random connect").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun zenBroConnect_connectPassesTrimmedCodeAndShowsResult() {
        var received: String? = null
        setContent(onAddBuddy = { code ->
            received = code
            BuddyAddResult.Success("Asha")
        })

        composeTestRule.onNodeWithText("ZEN-000").performScrollTo().performTextInput("  BUDDY42 ")
        composeTestRule.onNodeWithText("Connect").performClick()

        composeTestRule.onNodeWithText("Connected with Asha!").performScrollTo().assertIsDisplayed()
        assertEquals("BUDDY42", received)
    }

    @Test
    fun zenBroConnect_connectDisabledUntilCodeEntered() {
        setContent()
        composeTestRule.onNodeWithText("Connect").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun zenBroConnect_actionsInvokeCallbacks() {
        var shared = false
        var copied = false
        var random = false
        setContent(
            onShareLink = { shared = true },
            onCopyCode = { copied = true },
            onRandomConnect = { random = true }
        )

        composeTestRule.onNodeWithText("Share link").performClick()
        composeTestRule.onNodeWithText("Copy my code").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Find a buddy").performScrollTo().performClick()

        assertTrue(shared)
        assertTrue(copied)
        assertTrue(random)
    }

    @Test
    fun zenBroConnect_signedOut_disablesShareAndCopy() {
        setContent(userCode = null)
        composeTestRule.onNodeWithText("Share link").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Sign in first").performScrollTo().assertIsDisplayed()
    }
}
