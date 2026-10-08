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
import com.zenlauncher.zenmode.HomeGesture
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.testing.TestData
import com.zenlauncher.zenmode.ui.screens.HomeScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
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
        gestures: Set<HomeGesture> = emptySet(),
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
                    gestures = gestures,
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

    // ── Gestures (GesturePreferences / HomeGesture) ────────────────
    // Every one is off by default, so the tests above describe Home as it ships. These
    // describe Home once a gesture is switched on in Settings. The margin that the tap
    // gestures use is the same bare wash the long-press test aims at.

    @Test
    fun homeScreen_swipeUp_opensSearch_whenGestureOn() {
        var opened: Boolean? = null
        setContent(
            gestures = setOf(HomeGesture.SWIPE_UP_SEARCH),
            onShowSearchChange = { opened = it }
        )
        composeTestRule.onRoot().performTouchInput { swipeUp() }
        assertEquals(true, opened)
    }

    @Test
    fun homeScreen_swipeUp_doesNothing_whenGestureOff() {
        var opened: Boolean? = null
        setContent(onShowSearchChange = { opened = it })
        composeTestRule.onRoot().performTouchInput { swipeUp() }
        assertEquals(null, opened)
    }

    @Test
    fun homeScreen_swipeUpGesture_replacesSearchBar() {
        // The gesture is the way in, so the pill it replaces comes off the screen.
        setContent(gestures = setOf(HomeGesture.SWIPE_UP_SEARCH))
        composeTestRule.onNodeWithText("Search apps, files & everything", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun homeScreen_horizontalSwipe_stillTurnsPages_withSwipeUpOn() {
        // The two detectors share Home; a page swipe must not be eaten by the vertical one.
        var gold = 0
        setContent(gestures = setOf(HomeGesture.SWIPE_UP_SEARCH), onZenGoldClick = { gold++ })
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        assertEquals(1, gold)
    }

    @Test
    fun homeScreen_marginTaps_openThePageThatWay() {
        var score = 0
        var gold = 0
        setContent(
            gestures = setOf(HomeGesture.MARGIN_TAP_PAGES),
            onZenScoreClick = { score++ },
            onZenGoldClick = { gold++ }
        )
        composeTestRule.onRoot().performTouchInput { click(Offset(4f, centerY)) }
        composeTestRule.onRoot().performTouchInput { click(Offset(width - 4f, centerY)) }
        assertEquals("left margin opens the page on the left", 1, score)
        assertEquals("right margin opens the page on the right", 1, gold)
    }

    @Test
    fun homeScreen_marginTaps_ignoreTheMiddle() {
        var turned = 0
        setContent(
            gestures = setOf(HomeGesture.MARGIN_TAP_PAGES),
            onZenScoreClick = { turned++ },
            onZenGoldClick = { turned++ }
        )
        // Between the app rows: inside the content, well clear of either margin.
        composeTestRule.onRoot().performTouchInput { click(Offset(centerX, centerY)) }
        assertEquals(0, turned)
    }

    @Test
    fun homeScreen_marginTap_doesNothing_whenGestureOff() {
        var turned = 0
        setContent(onZenScoreClick = { turned++ }, onZenGoldClick = { turned++ })
        composeTestRule.onRoot().performTouchInput { click(Offset(4f, centerY)) }
        assertEquals(0, turned)
    }

    @Test
    fun homeScreen_doubleTap_locks_whenGestureOn() {
        var locked = 0
        setContent(gestures = setOf(HomeGesture.DOUBLE_TAP_LOCK), onLockClick = { locked++ })
        composeTestRule.onRoot().performTouchInput { doubleClick(Offset(4f, centerY)) }
        assertEquals(1, locked)
    }

    @Test
    fun homeScreen_doubleTap_doesNothing_whenGestureOff() {
        var locked = 0
        setContent(onLockClick = { locked++ })
        composeTestRule.onRoot().performTouchInput { doubleClick(Offset(4f, centerY)) }
        assertEquals(0, locked)
    }

    @Test
    fun homeScreen_longPress_stillLocks_withDoubleTapOn() {
        // Double-tap is a second way in, not a replacement.
        var locked = false
        setContent(gestures = setOf(HomeGesture.DOUBLE_TAP_LOCK), onLockClick = { locked = true })
        composeTestRule.onRoot().performTouchInput { longClick(Offset(4f, centerY)) }
        assertTrue(locked)
    }
}
