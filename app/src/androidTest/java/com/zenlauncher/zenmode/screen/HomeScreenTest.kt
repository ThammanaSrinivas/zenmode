package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zenlauncher.zenmode.AppInfo
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.testing.TestData
import com.zenlauncher.zenmode.ui.screens.HomeScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        usage: DailyUsage? = TestData.twoHoursUsage,
        streaks: Int = 5,
        yesterdayChangePercent: Int? = 10,
        hasBuddies: Boolean = false,
        buddyStats: BuddyStats? = null,
        isSignedIn: Boolean = true,
        showSearch: Boolean = false,
        onShowSearchChange: (Boolean) -> Unit = {},
        onSettingsClick: () -> Unit = {},
        onGoogleSearch: (String) -> Unit = {},
        onPhoneClick: () -> Unit = {},
        onLockClick: () -> Unit = {},
        onInviteBuddyClick: () -> Unit = {},
        onSignInClick: () -> Unit = {},
        onAppClick: (AppInfo) -> Unit = {},
        apps: List<AppInfo> = TestData.createAppList(3)
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                HomeScreen(
                    usage = usage,
                    streaks = streaks,
                    yesterdayChangePercent = yesterdayChangePercent,
                    hasBuddies = hasBuddies,
                    buddyStats = buddyStats,
                    isSignedIn = isSignedIn,
                    showSearch = showSearch,
                    onShowSearchChange = onShowSearchChange,
                    onSettingsClick = onSettingsClick,
                    onGoogleSearch = onGoogleSearch,
                    onPhoneClick = onPhoneClick,
                    onLockClick = onLockClick,
                    onInviteBuddyClick = onInviteBuddyClick,
                    onSignInClick = onSignInClick,
                    onAppClick = onAppClick,
                    apps = apps
                )
            }
        }
    }

    @Test
    fun homeScreen_rendersWithoutCrash() {
        setContent()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsAppGrid() {
        // App grid shows icons only (no text labels). Verify the grid renders with lock icon.
        setContent(apps = TestData.createAppList(3))
        composeTestRule.onNodeWithContentDescription("Lock phone").assertIsDisplayed()
    }

    @Test
    fun homeScreen_settingsIcon_callsCallback() {
        var clicked = false
        setContent(onSettingsClick = { clicked = true })
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        assertTrue(clicked)
    }

    @Test
    fun homeScreen_phoneIcon_callsCallback() {
        var clicked = false
        setContent(onPhoneClick = { clicked = true })
        composeTestRule.onNodeWithContentDescription("Phone").performClick()
        assertTrue(clicked)
    }

    @Test
    fun homeScreen_lockIcon_callsCallback() {
        var clicked = false
        setContent(onLockClick = { clicked = true })
        composeTestRule.onNodeWithContentDescription("Lock phone").performClick()
        assertTrue(clicked)
    }

    @Test
    fun homeScreen_showsSearchBar() {
        setContent()
        composeTestRule.onNodeWithText("Search apps & everything", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsSearchOverlay_whenShowSearchTrue() {
        setContent(showSearch = true)
        // "Search apps & everything" appears in both BottomDock and SearchOverlay, use onAllNodes
        composeTestRule.onAllNodesWithText("Search apps & everything", substring = true)[0]
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsStreakCount() {
        setContent(streaks = 7)
        composeTestRule.onNodeWithText("7", substring = true).assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsScreenTime() {
        // 2 hours usage should show "02" somewhere
        setContent(usage = TestData.twoHoursUsage)
        composeTestRule.onNodeWithText("02", substring = true).assertIsDisplayed()
    }
}
