package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.testing.TestActivity
import com.zenlauncher.zenmode.ui.screens.ZenBroConnectedScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZenBroConnectedScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        buddyStats: BuddyStats? = BuddyStats(screenTimeMins = 120),
        onShareInviteLink: () -> Unit = {},
        onCopyInviteCode: () -> Unit = {},
        onMaybeLater: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                ZenBroConnectedScreen(
                    buddyName = "Asha",
                    usage = null,
                    streaks = 2,
                    zenScore = 7,
                    buddyStats = buddyStats,
                    userCode = "USER1234567",
                    onBackClick = {},
                    onShareInviteLink = onShareInviteLink,
                    onCopyInviteCode = onCopyInviteCode,
                    onMaybeLater = onMaybeLater
                )
            }
        }
    }

    @Test
    fun connected_greetsBuddyByName() {
        setContent()
        composeTestRule.onNodeWithText("You’re Zen Bros now").assertIsDisplayed()
        composeTestRule.onNodeWithText("You and Asha", substring = true).assertIsDisplayed()
    }

    @Test
    fun connected_showsBothStatCards_evenBeforeBuddyStatsLoad() {
        setContent(buddyStats = null)
        composeTestRule.onNodeWithText("My Screen Time").assertExists()
        composeTestRule.onNodeWithText("My Buddy's Stats").assertExists()
    }

    @Test
    fun connected_maybeLaterInvokesCallback() {
        var later = false
        setContent(onMaybeLater = { later = true })
        composeTestRule.onNodeWithText("Maybe later").performScrollTo().performClick()
        assertTrue(later)
    }

    @Test
    fun invitePeople_opensSheet_whoseOptionsInvokeCallbacks() {
        var shared = false
        var copied = false
        setContent(onShareInviteLink = { shared = true }, onCopyInviteCode = { copied = true })

        composeTestRule.onNodeWithText("Invite people").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Invite your people!").assertIsDisplayed()

        composeTestRule.onNodeWithText("Share invite link").performClick()
        composeTestRule.onNodeWithText("Copy Invite code").performClick()
        composeTestRule.onNodeWithText("Copied", substring = true).assertIsDisplayed()

        assertTrue(shared)
        assertTrue(copied)
    }

    @Test
    fun inviteSheet_closeButtonDismisses() {
        setContent()
        composeTestRule.onNodeWithText("Invite people").performScrollTo().performClick()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithText("Invite your people!").assertDoesNotExist()
    }
}
