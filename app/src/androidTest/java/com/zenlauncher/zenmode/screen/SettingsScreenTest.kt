package com.zenlauncher.zenmode.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenlauncher.zenmode.coreapi.services.BillingPeriod
import com.zenlauncher.zenmode.coreapi.services.Entitlement
import com.zenlauncher.zenmode.coreapi.services.PlanOffer
import com.zenlauncher.zenmode.coreapi.services.ProFeature
import com.zenlauncher.zenmode.coreapi.services.ProStatus
import com.zenlauncher.zenmode.testing.TestActivity
import com.zenlauncher.zenmode.ui.screens.ProEntry
import com.zenlauncher.zenmode.ui.screens.SettingsScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private val offers = listOf(
        PlanOffer(BillingPeriod.ANNUAL, "₹699", "₹699", "₹58", freeTrialDays = 30),
        PlanOffer(BillingPeriod.MONTHLY, "₹75", "₹899", "₹75", freeTrialDays = 0)
    )

    private fun setContent(
        isProAvailable: Boolean = false,
        entitlement: Entitlement = Entitlement.Free,
        onBackClick: () -> Unit = {},
        onAccountabilityPartnerClick: () -> Unit = {},
        onContributeClick: () -> Unit = {},
        onRateClick: () -> Unit = {},
        onShareClick: () -> Unit = {},
        onOpenPro: (ProEntry) -> Unit = {},
        onProGateShown: (ProFeature) -> Unit = {},
        onLogoutClick: () -> Unit = {},
        onDeleteAccountClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ZenTheme(darkTheme = false) {
                SettingsScreen(
                    isProAvailable = isProAvailable,
                    entitlement = entitlement,
                    offers = offers,
                    onBackClick = onBackClick,
                    onAccountabilityPartnerClick = onAccountabilityPartnerClick,
                    onContributeClick = onContributeClick,
                    onRateClick = onRateClick,
                    onShareClick = onShareClick,
                    onOpenPro = onOpenPro,
                    onProGateShown = onProGateShown,
                    onLogoutClick = onLogoutClick,
                    onDeleteAccountClick = onDeleteAccountClick
                )
            }
        }
    }

    // ── Existing flows ─────────────────────────────────────────────

    @Test
    fun rendersTitleAndScreenTime() {
        setContent()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("THIS WEEK").assertIsDisplayed()
    }

    @Test
    fun keepsEveryExistingSetting() {
        setContent()
        listOf(
            "Resistance screen",
            "Block in-app content",
            "Apps on home screen",
            "Notification badges",
            "Dark mode (beta)",
            "Accountability partner",
            "Rate on Play Store",
            "Share ZenMode",
            "Contribute on GitHub"
        ).forEach { composeTestRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
    }

    @Test
    fun backButton_callsCallback() {
        var clicked = false
        setContent(onBackClick = { clicked = true })
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(clicked)
    }

    @Test
    fun accountabilityPartner_callsCallback() {
        var clicked = false
        setContent(onAccountabilityPartnerClick = { clicked = true })
        composeTestRule.onNodeWithText("Accountability partner").performScrollTo().performClick()
        assertTrue(clicked)
    }

    @Test
    fun contribute_callsCallback() {
        var clicked = false
        setContent(onContributeClick = { clicked = true })
        composeTestRule.onNodeWithText("Contribute on GitHub").performScrollTo().performClick()
        assertTrue(clicked)
    }

    @Test
    fun rate_callsCallback() {
        var clicked = false
        setContent(onRateClick = { clicked = true })
        composeTestRule.onNodeWithText("Rate on Play Store").performScrollTo().performClick()
        assertTrue(clicked)
    }

    @Test
    fun deleteAccount_needsConfirmation() {
        var deleted = false
        setContent(onDeleteAccountClick = { deleted = true })
        composeTestRule.onNodeWithContentDescription("Account").performClick()
        composeTestRule.onNodeWithText("Delete account").performClick()
        assertTrue("Delete must not fire before confirming", !deleted)
        composeTestRule.onNodeWithText("Delete my account").performClick()
        assertTrue(deleted)
    }

    @Test
    fun logout_callsCallback() {
        var loggedOut = false
        setContent(onLogoutClick = { loggedOut = true })
        composeTestRule.onNodeWithContentDescription("Account").performClick()
        composeTestRule.onNodeWithText("Log out").performClick()
        assertTrue(loggedOut)
    }

    // ── Pro vs free ────────────────────────────────────────────────

    @Test
    fun proUnavailable_showsNoProSurfaces() {
        setContent(isProAvailable = false)
        composeTestRule.onAllNodesWithText("ZenMode Pro").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("PRO").assertCountEquals(0)
    }

    @Test
    fun free_planCardShowsPriceInTheRow() {
        setContent(isProAvailable = true)
        composeTestRule.onNodeWithText("ZenMode Pro").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹699/year, or ₹75/month").assertIsDisplayed()
    }

    @Test
    fun free_planCard_opensProPage() {
        var entry: ProEntry? = null
        setContent(isProAvailable = true, onOpenPro = { entry = it })
        composeTestRule.onNodeWithText("See what it adds").performClick()
        assertEquals(ProEntry.PLAN_CARD, entry)
    }

    @Test
    fun free_lockedRow_opensGateInsteadOfFeature() {
        var gated: ProFeature? = null
        var entry: ProEntry? = null
        setContent(isProAvailable = true, onProGateShown = { gated = it }, onOpenPro = { entry = it })
        composeTestRule.onNodeWithText("Export data").performScrollTo().performClick()
        assertEquals(ProFeature.DATA_EXPORT, gated)
        composeTestRule.onNodeWithText("See what Pro adds").performClick()
        assertEquals(ProEntry.GATE, entry)
    }

    @Test
    fun free_gate_notNow_dismissesWithoutOpeningPro() {
        var entry: ProEntry? = null
        setContent(isProAvailable = true, onOpenPro = { entry = it })
        composeTestRule.onNodeWithText("Home-screen themes").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Not now").performClick()
        composeTestRule.onAllNodesWithText("See what Pro adds").assertCountEquals(0)
        assertEquals(null, entry)
    }

    @Test
    fun pro_planCard_opensManage() {
        var entry: ProEntry? = null
        setContent(
            isProAvailable = true,
            entitlement = Entitlement(status = ProStatus.ACTIVE, period = BillingPeriod.ANNUAL),
            onOpenPro = { entry = it }
        )
        composeTestRule.onNodeWithText("Manage").performClick()
        assertEquals(ProEntry.MANAGE, entry)
    }

    @Test
    fun pro_proRow_neverOpensGate() {
        var gated: ProFeature? = null
        setContent(
            isProAvailable = true,
            entitlement = Entitlement(status = ProStatus.ACTIVE, period = BillingPeriod.MONTHLY),
            onProGateShown = { gated = it }
        )
        composeTestRule.onNodeWithText("Export data").performScrollTo().performClick()
        assertEquals(null, gated)
        composeTestRule.onNodeWithText("Included in your Pro".uppercase()).assertIsDisplayed()
    }
}
