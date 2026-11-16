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
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
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
        onExportData: () -> Unit = {},
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
                    onExportData = onExportData,
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
        // The title is the "ZenMode OS Settings" wordmark, read out as one phrase.
        composeTestRule.onNodeWithContentDescription("$PRODUCT_NAME Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("THIS WEEK").assertIsDisplayed()
    }

    @Test
    fun keepsEveryExistingSetting() {
        setContent()
        // Every row a free user sees, group by group. Rows that only appear with Pro on sale
        // (themes, export) or behind a toggle (check-in time) are covered by their own tests.
        listOf(
            // Focus
            "Resistance screen",
            "Distraction Blocker",
            // Daily check-in
            "Evening check-in",
            // Home screen
            "Choose home apps",
            "Notification badges",
            // Gestures
            "Swipe up to search",
            "Tap the margins",
            "Double-tap to lock",
            // Look & sound
            "Appearance",
            "Interface sounds",
            // Accountability
            "Accountability partner",
            "Zen Circle members",
            // Phone
            "Default home app",
            "Zen Island",
            "Widgets",
            "Usage access",
            "Accessibility",
            "Battery",
            "Do Not Disturb",
            "$PRODUCT_NAME notifications",
            "All phone settings",
            // Help us build it
            "Report a bug",
            "Suggest a feature",
            // ZenMode OS
            "Rate on Play Store",
            "Share $PRODUCT_NAME",
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
        composeTestRule.onAllNodesWithText(PRO_PLAN_NAME).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("PRO").assertCountEquals(0)
    }

    @Test
    fun free_planCardShowsPriceInTheRow() {
        setContent(isProAvailable = true)
        composeTestRule.onNodeWithText(PRO_PLAN_NAME).assertIsDisplayed()
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
        var exported = false
        setContent(
            isProAvailable = true,
            entitlement = Entitlement(status = ProStatus.ACTIVE, period = BillingPeriod.MONTHLY),
            onProGateShown = { gated = it },
            onExportData = { exported = true }
        )
        // Export has shipped: on Pro the row runs it straight away, no gate and no "arriving" note.
        composeTestRule.onNodeWithText("Export data").performScrollTo().performClick()
        assertEquals(null, gated)
        assertTrue("Pro's Export data row should run the export", exported)
        composeTestRule.onAllNodesWithText("See what Pro adds").assertCountEquals(0)
    }

    private companion object {
        /** The plan card's title. */
        const val PRO_PLAN_NAME = "$PRODUCT_NAME Pro"
    }
}
