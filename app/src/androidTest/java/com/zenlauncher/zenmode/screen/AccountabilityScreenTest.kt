package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zenlauncher.zenmode.testing.TestActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zenlauncher.zenmode.AccountabilityUiState
import com.zenlauncher.zenmode.testing.TestData
import com.zenlauncher.zenmode.ui.screens.AccountabilityScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class AccountabilityScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun setContent(
        uiState: AccountabilityUiState = TestData.createAccountabilityUiState(),
        onBackClick: () -> Unit = {},
        onCopyCode: (String) -> Unit = {},
        onBackToHomeClick: () -> Unit = {},
        onChangeBuddyConfirmed: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                AccountabilityScreen(
                    uiState = uiState,
                    onBackClick = onBackClick,
                    onCopyCode = onCopyCode,
                    onBackToHomeClick = onBackToHomeClick,
                    onChangeBuddyConfirmed = onChangeBuddyConfirmed
                )
            }
        }
    }

    @Test
    fun accountabilityScreen_rendersWithoutCrash() {
        setContent()
        composeTestRule.onNodeWithText("Zen Buddy Summary").assertIsDisplayed()
    }

    @Test
    fun accountabilityScreen_showsUserCode() {
        setContent(uiState = TestData.createAccountabilityUiState(userCode = "XYZ789"))
        composeTestRule.onNodeWithText("XYZ789", substring = true).assertIsDisplayed()
    }

    @Test
    fun accountabilityScreen_showsWeeklyBattleHeading() {
        setContent()
        composeTestRule.onNodeWithText("Zenmode Weekly Battle", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun accountabilityScreen_backToHome_callsCallback() {
        var clicked = false
        setContent(onBackToHomeClick = { clicked = true })
        composeTestRule.onNodeWithContentDescription("Back to home").performClick()
        assertTrue(clicked)
    }

    @Test
    fun accountabilityScreen_changeBuddy_showsConfirmDialog() {
        setContent()
        composeTestRule.onNodeWithText("Change my buddy").performClick()
        composeTestRule.onNodeWithText("Change buddy?").assertIsDisplayed()
    }

    @Test
    fun accountabilityScreen_confirmDisconnect_callsCallback() {
        var confirmed = false
        setContent(onChangeBuddyConfirmed = { confirmed = true })
        composeTestRule.onNodeWithText("Change my buddy").performClick()
        composeTestRule.onNodeWithText("Yes, disconnect").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun accountabilityScreen_cancelDisconnect_dismissesDialog() {
        setContent()
        composeTestRule.onNodeWithText("Change my buddy").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Change buddy?").assertDoesNotExist()
    }

    @Test
    fun accountabilityScreen_copyCode_callsCallback() {
        var copiedCode: String? = null
        setContent(
            uiState = TestData.createAccountabilityUiState(userCode = "ABC123"),
            onCopyCode = { copiedCode = it }
        )
        composeTestRule.onNodeWithContentDescription("Copy code").performClick()
        assertTrue(copiedCode != null)
    }
}
