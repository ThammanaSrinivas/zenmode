package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenlauncher.zenmode.testing.TestActivity
import com.zenlauncher.zenmode.ui.screens.ZenCircleMember
import com.zenlauncher.zenmode.ui.screens.ZenCircleScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZenCircleScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private val you = ZenCircleMember("You", isYou = true, screenTimeMinutes = 59, zenScore = 93, streaks = 13)
    private val asha = ZenCircleMember("Asha", isYou = false, screenTimeMinutes = 300, zenScore = 88, streaks = 5)

    private fun setContent(
        onBackToHome: () -> Unit = {},
        onSendLove: (ZenCircleMember) -> Unit = {},
        onWeeklyClick: () -> Unit = {},
        onRemoveBuddy: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                ZenCircleScreen(
                    members = listOf(you, asha),
                    userCode = "USER1234567",
                    onBackClick = {},
                    onShareInviteLink = {},
                    onCopyInviteCode = {},
                    onBackToHome = onBackToHome,
                    onSendLove = onSendLove,
                    onSendMelt = {},
                    onWeeklyClick = onWeeklyClick,
                    removingBuddy = false,
                    onRemoveBuddy = onRemoveBuddy,
                    onLeaveCircle = {}
                )
            }
        }
    }

    /**
     * The screen opens one member back and spins the first one into place after a short
     * real-time delay, which the test clock doesn't cover. Wait for the leader to land.
     */
    private fun awaitEntrance() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("#01").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun circle_showsTagRankingAndDailyToggle() {
        setContent()
        awaitEntrance()
        composeTestRule.onNodeWithText("ZENCIR-02").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily").assertIsDisplayed()
        composeTestRule.onNodeWithText("#01").assertExists()
    }

    @Test
    fun reactions_disabledOnYourself_enabledOnYourBuddy() {
        var loved: ZenCircleMember? = null
        setContent(onSendLove = { loved = it })
        awaitEntrance()

        composeTestRule.onNodeWithContentDescription("Send love to You", useUnmergedTree = true).assertIsNotEnabled()

        composeTestRule.onNodeWithContentDescription("Next member").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("#02").assertExists()

        composeTestRule.onNodeWithContentDescription("Send love to Asha", useUnmergedTree = true).performClick()
        assertEquals(asha, loved)
    }

    @Test
    fun weeklyAndBackToHome_invokeCallbacks() {
        var weekly = false
        var home = false
        setContent(onBackToHome = { home = true }, onWeeklyClick = { weekly = true })

        composeTestRule.onNodeWithText("Weekly").performClick()
        composeTestRule.onNodeWithText("Back to the Home").performScrollTo().performClick()

        assertTrue(weekly)
        assertTrue(home)
    }

    @Test
    fun shareAndInvite_opensShareCardPreview() {
        setContent()
        composeTestRule.onNodeWithText("Share & Invite to Zen Circle").performScrollTo().performClick()
        // The leaderboard card (Figma "Sharable") with its share/save actions.
        composeTestRule.onNodeWithText("Zencircle daily leaderboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Share my Zen Circle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save as image").assertIsDisplayed()
    }

    @Test
    fun menu_opensSettings_andRemoveBuddyNeedsConfirmation() {
        var removed = false
        setContent(onRemoveBuddy = { removed = true })

        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("ZEN CIRCLE SETTINGS").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 seats left").assertIsDisplayed()

        composeTestRule.onNodeWithText("Remove buddy").performClick()
        assertTrue("first tap only asks", !removed)
        // The confirm row sits in the (non-scrolling) sheet itself.
        composeTestRule.onNodeWithText("Remove").performClick()
        assertTrue(removed)
    }

    @Test
    fun settings_invitePeople_swapsToInviteSheet() {
        setContent()
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Invite people").performClick()
        composeTestRule.onNodeWithText("Invite your people!").assertIsDisplayed()
    }
}
