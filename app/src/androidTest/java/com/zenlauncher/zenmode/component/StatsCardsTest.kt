package com.zenlauncher.zenmode.component

import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.testing.TestData
import com.zenlauncher.zenmode.ui.components.StatsCardsRow
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class StatsCardsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun statsCards_showsScreenTime_whenUsageProvided() {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                StatsCardsRow(
                    usage = TestData.twoHoursUsage,
                    yesterdayChangePercent = 10,
                    hasBuddies = false,
                    buddyStats = null,
                    isSignedIn = true,
                    onInviteBuddyClick = {},
                    onSignInClick = {}
                )
            }
        }
        // 2 hours = "02" hours displayed
        composeTestRule.onNodeWithText("02", substring = true).assertIsDisplayed()
    }

    @Test
    fun statsCards_showsBuddyInviteCard_whenNoBuddiesAndSignedIn() {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                StatsCardsRow(
                    usage = TestData.twoHoursUsage,
                    yesterdayChangePercent = null,
                    hasBuddies = false,
                    buddyStats = null,
                    isSignedIn = true,
                    onInviteBuddyClick = {},
                    onSignInClick = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Invite buddy").assertIsDisplayed()
    }

    @Test
    fun statsCards_showsBuddyStatsCard_whenHasBuddies() {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                StatsCardsRow(
                    usage = TestData.twoHoursUsage,
                    yesterdayChangePercent = null,
                    hasBuddies = true,
                    buddyStats = TestData.defaultBuddyStats,
                    isSignedIn = true,
                    onInviteBuddyClick = {},
                    onSignInClick = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Buddy mood face").assertIsDisplayed()
    }

    @Test
    fun statsCards_showsSignInCard_whenNotSignedIn() {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                StatsCardsRow(
                    usage = TestData.twoHoursUsage,
                    yesterdayChangePercent = null,
                    hasBuddies = false,
                    buddyStats = null,
                    isSignedIn = false,
                    onInviteBuddyClick = {},
                    onSignInClick = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Sign in with Google").assertIsDisplayed()
    }

    @Test
    fun statsCards_inviteClick_callsCallback() {
        var clicked = false
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                StatsCardsRow(
                    usage = TestData.twoHoursUsage,
                    yesterdayChangePercent = null,
                    hasBuddies = false,
                    buddyStats = null,
                    isSignedIn = true,
                    onInviteBuddyClick = { clicked = true },
                    onSignInClick = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Invite buddy").performClick()
        assertTrue(clicked)
    }
}
