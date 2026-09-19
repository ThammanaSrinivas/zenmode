package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.performTextInput
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.AppInfo
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.testing.TestData
import com.zenlauncher.zenmode.ui.screens.HomeScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
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
        onZenGoldClick: () -> Unit = {},
        onZenScoreClick: () -> Unit = {},
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
                    zenScore = 93, // 9.3 of 10, see ZenScore
                    goldInvested = AppConstants.PLACEHOLDER_GOLD_INVESTED,
                    goldChangePercent = AppConstants.PLACEHOLDER_GOLD_CHANGE_PERCENT,
                    onShowSearchChange = onShowSearchChange,
                    onZenGoldClick = onZenGoldClick,
                    onZenScoreClick = onZenScoreClick,
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
        composeTestRule.onNodeWithText("Zen Score").assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsAppGrid() {
        // Icons only on screen; each is named for screen readers.
        setContent(apps = TestData.createAppList(3))
        composeTestRule.onNodeWithContentDescription("App 1").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("App 3").assertIsDisplayed()
    }

    // v3 home has no dock: swipe left for Zen Gold, right for Zen Score, long-press to lock.

    @Test
    fun homeScreen_swipeLeft_opensZenGoldOnce() {
        var opened = 0
        setContent(onZenGoldClick = { opened++ })
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        assertEquals("one swipe opens the page once", 1, opened)
    }

    @Test
    fun homeScreen_swipeRight_opensZenScoreOnce() {
        var opened = 0
        setContent(onZenScoreClick = { opened++ })
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        assertEquals("one swipe opens the page once", 1, opened)
    }

    @Test
    fun homeScreen_longPress_locks() {
        var locked = false
        setContent(onLockClick = { locked = true })
        // The left margin is bare wash, clear of the cards and app icons.
        composeTestRule.onRoot().performTouchInput { longClick(Offset(4f, centerY)) }
        assertTrue(locked)
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
        // Home pill + overlay field both carry the prompt; the field has focus.
        composeTestRule.onNode(hasSetTextAction() and isFocused()).assertIsDisplayed()
    }

    @Test
    fun homeScreen_searchOverlay_showsAppsAndGoogleOnceTyped() {
        setContent(showSearch = true, apps = TestData.createAppList(5))
        composeTestRule.onNode(hasSetTextAction()).performTextInput("App")

        composeTestRule.onNodeWithText("APPS").assertIsDisplayed()
        // Five matches collapse to three plus an expander.
        composeTestRule.onNodeWithText("+2 more apps").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("App 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search Google for “App”").assertIsDisplayed()
    }

    @Test
    fun homeScreen_searchOverlay_googleRowHandsOffQuery() {
        var searched: String? = null
        setContent(showSearch = true, onGoogleSearch = { searched = it })
        composeTestRule.onNode(hasSetTextAction()).performTextInput("zen")
        composeTestRule.onNodeWithText("Search Google for “zen”").performClick()
        assertTrue(searched == "zen")
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
        // The header and the stats card both show the streak.
        composeTestRule.onAllNodesWithText("7").onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText("days").assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsScreenTime() {
        // 2 hours usage should show "02" somewhere
        setContent(usage = TestData.twoHoursUsage)
        composeTestRule.onNodeWithText("02", substring = true).assertIsDisplayed()
    }
}
