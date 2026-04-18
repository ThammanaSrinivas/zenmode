package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.zenlauncher.zenmode.ui.screens.SettingsScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        onBackClick: () -> Unit = {},
        onChangeDistractingAppsClick: () -> Unit = {},
        onAccountabilityPartnerClick: () -> Unit = {},
        onContributeClick: () -> Unit = {},
        onRateClick: () -> Unit = {},
        onShareClick: () -> Unit = {},
        onLogoutClick: () -> Unit = {},
        onDeleteAccountClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                SettingsScreen(
                    onBackClick = onBackClick,
                    onChangeDistractingAppsClick = onChangeDistractingAppsClick,
                    onAccountabilityPartnerClick = onAccountabilityPartnerClick,
                    onContributeClick = onContributeClick,
                    onRateClick = onRateClick,
                    onShareClick = onShareClick,
                    onLogoutClick = onLogoutClick,
                    onDeleteAccountClick = onDeleteAccountClick
                )
            }
        }
    }

    @Test
    fun settingsScreen_rendersWithoutCrash() {
        setContent()
        composeTestRule.onNodeWithText("Personalise, Your Way!").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_showsWeeklyStatsSection() {
        setContent()
        composeTestRule.onNodeWithText("My Weekly stats").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_showsDarkModeToggle() {
        setContent()
        composeTestRule.onNodeWithText("Dark mode").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_showsNotificationBadgesToggle() {
        setContent()
        composeTestRule.onNodeWithText("Notification badges").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_backButton_callsCallback() {
        var clicked = false
        setContent(onBackClick = { clicked = true })
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(clicked)
    }

    @Test
    fun settingsScreen_changeDistractingApps_callsCallback() {
        var clicked = false
        setContent(onChangeDistractingAppsClick = { clicked = true })
        composeTestRule.onNodeWithText("Change distracting app list").performClick()
        assertTrue(clicked)
    }

    @Test
    fun settingsScreen_accountabilityPartner_callsCallback() {
        var clicked = false
        setContent(onAccountabilityPartnerClick = { clicked = true })
        composeTestRule.onNodeWithText("Accountability partner settings").performClick()
        assertTrue(clicked)
    }

    @Test
    fun settingsScreen_contribute_callsCallback() {
        var clicked = false
        setContent(onContributeClick = { clicked = true })
        composeTestRule.onNodeWithText("Contribute zenmode (via GitHub)").performClick()
        assertTrue(clicked)
    }

    @Test
    fun settingsScreen_showsHeroSection() {
        setContent()
        composeTestRule.onNodeWithText("You're the Hero!").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_showsRateButton() {
        setContent()
        composeTestRule.onNodeWithText("Rate us on play store").performScrollTo().assertIsDisplayed()
    }
}
