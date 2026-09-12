package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zenlauncher.zenmode.AppConstants
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
                    zenScore = AppConstants.PLACEHOLDER_ZEN_SCORE,
                    goldInvested = AppConstants.PLACEHOLDER_GOLD_INVESTED,
                    goldChangePercent = AppConstants.PLACEHOLDER_GOLD_CHANGE_PERCENT,
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
        composeTestRule.onNodeWithText("Search apps, files & everything", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsSearchOverlay_whenShowSearchTrue() {
        setContent(showSearch = true)
        // The overlay labels its three parts; Apps is always the first.
        composeTestRule.onNodeWithText("APPS").assertIsDisplayed()
    }

    @Test
    fun homeScreen_searchOverlay_labelsAllThreeSections() {
        setContent(showSearch = true)
        composeTestRule.onNodeWithText("APPS").assertIsDisplayed()
        composeTestRule.onNodeWithText("GOOGLE").assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsZenScore() {
        setContent()
        composeTestRule.onNodeWithText("Zen Score").assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsGoldInvested() {
        setContent()
        composeTestRule.onNodeWithText("GOLD INVESTED").assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsStreakCount() {
        // The count and "days" are separate Text nodes (the count alone carries the
        // gradient style) — Compose doesn't merge sibling text into one semantics
        // string, so this must assert each independently, not the concatenation.
        setContent(streaks = 7)
        composeTestRule.onNodeWithText("7").assertIsDisplayed()
        composeTestRule.onNodeWithText("days").assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsScreenTime() {
        // 2 hours usage should show "02" somewhere
        setContent(usage = TestData.twoHoursUsage)
        composeTestRule.onNodeWithText("02", substring = true).assertIsDisplayed()
    }
}
