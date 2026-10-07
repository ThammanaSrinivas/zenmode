package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.testing.TestData
import com.zenlauncher.zenmode.ui.screens.ResistenceScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ResistenceScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        usage: DailyUsage? = TestData.twoHoursUsage,
        streaks: Int = 3,
        yesterdayChangePercent: Int? = 10,
        skipsLeft: Int = 2,
        countdownSeconds: Int = 5,
        countdownFinished: Boolean = false,
        onSettingsClick: () -> Unit = {},
        onPhoneClick: () -> Unit = {},
        onSkipClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                ResistenceScreen(
                    usage = usage,
                    streaks = streaks,
                    yesterdayChangePercent = yesterdayChangePercent,
                    skipsLeft = skipsLeft,
                    countdownSeconds = countdownSeconds,
                    countdownFinished = countdownFinished,
                    onSettingsClick = onSettingsClick,
                    onPhoneClick = onPhoneClick,
                    onSkipClick = onSkipClick
                )
            }
        }
    }

    @Test
    fun resistenceScreen_rendersWithoutCrash() {
        setContent()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun resistenceScreen_showsSkipText_whenSkipsAvailable() {
        setContent(skipsLeft = 3, countdownFinished = true)
        composeTestRule.onNodeWithText("skip & open", substring = true).assertIsDisplayed()
    }

    @Test
    fun resistenceScreen_showsNoSkipsLeft_whenZeroSkips() {
        setContent(skipsLeft = 0, countdownFinished = true)
        composeTestRule.onNodeWithText("No skips left", substring = true).assertIsDisplayed()
    }

    @Test
    fun resistenceScreen_skipClick_callsCallback() {
        var clicked = false
        setContent(skipsLeft = 2, countdownFinished = true, onSkipClick = { clicked = true })
        composeTestRule.onNodeWithText("skip", substring = true).performClick()
        assertTrue(clicked)
    }

    @Test
    fun resistenceScreen_settingsClick_callsCallback() {
        var clicked = false
        setContent(onSettingsClick = { clicked = true })
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        assertTrue(clicked)
    }

    @Test
    fun resistenceScreen_phoneClick_callsCallback() {
        var clicked = false
        setContent(onPhoneClick = { clicked = true })
        composeTestRule.onNodeWithContentDescription("Phone").performClick()
        assertTrue(clicked)
    }

    @Test
    fun resistenceScreen_showsScreenTime() {
        // 2 hours = "02" hours
        setContent(usage = TestData.twoHoursUsage)
        composeTestRule.onNodeWithText("02", substring = true).assertIsDisplayed()
    }

    @Test
    fun resistenceScreen_showsV3StreakBlock() {
        // The v2 pill ("streaks: 3") is gone; the header now draws the same
        // StreakStat block Home does — label over count, with the unit beside it.
        setContent(streaks = 3)
        composeTestRule.onNodeWithText("Streaks").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("days").assertIsDisplayed()
    }

    @Test
    fun resistenceScreen_streakBlockUsesSingularDay_whenStreakIsOne() {
        setContent(streaks = 1)
        composeTestRule.onNodeWithText("day").assertIsDisplayed()
    }

    @Test
    fun resistenceScreen_showsMood() {
        setContent()
        composeTestRule.onNodeWithContentDescription("Mood").assertIsDisplayed()
    }
}
