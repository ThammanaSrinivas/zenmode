package com.zenlauncher.zenmode.ui.screens

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.coreapi.services.BillingPeriod
import com.zenlauncher.zenmode.coreapi.services.Entitlement
import com.zenlauncher.zenmode.coreapi.services.PlanOffer
import com.zenlauncher.zenmode.coreapi.services.ProStatus
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test

/**
 * Golden coverage for the v3 settings screen and the Pro page, light build only.
 * Baselines: `recordPaparazzi`, review under app/src/test/snapshots/, commit.
 */
class SettingsScreenScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 5600))

    private val offers = listOf(
        PlanOffer(BillingPeriod.ANNUAL, "₹699", "₹699", "₹58", freeTrialDays = 30),
        PlanOffer(BillingPeriod.MONTHLY, "₹75", "₹899", "₹75", freeTrialDays = 0)
    )
    private val week = listOf(3.2f, 2.6f, 4.1f, 2.2f, 1.8f, 2.9f, 1.9f)

    @Test
    fun `settings - free`() {
        paparazzi.snapshot {
            ZenTheme(darkTheme = false) {
                SettingsScreen(
                    weeklyHours = week,
                    displayName = "Kamal",
                    isProAvailable = true,
                    offers = offers,
                    distractingAppCount = 6,
                    isContentBlockingOn = true,
                    onBackClick = {},
                    onChangeDistractingAppsClick = {},
                    onAccountabilityPartnerClick = {},
                    onContributeClick = {},
                    onRateClick = {},
                    onShareClick = {}
                )
            }
        }
    }

    @Test
    fun `settings - pro trial`() {
        paparazzi.snapshot {
            ZenTheme(darkTheme = false) {
                SettingsScreen(
                    weeklyHours = week,
                    displayName = "Kamal",
                    isProAvailable = true,
                    entitlement = Entitlement(
                        status = ProStatus.TRIAL,
                        period = BillingPeriod.ANNUAL,
                        since = 1_789_603_200_000L,
                        trialEndsOn = 1_792_195_200_000L,
                        renewsOn = 1_792_195_200_000L
                    ),
                    offers = offers,
                    distractingAppCount = 6,
                    onBackClick = {},
                    onChangeDistractingAppsClick = {},
                    onAccountabilityPartnerClick = {},
                    onContributeClick = {},
                    onRateClick = {},
                    onShareClick = {}
                )
            }
        }
    }

    @Test
    fun `settings - pro unavailable`() {
        paparazzi.snapshot {
            ZenTheme(darkTheme = false) {
                SettingsScreen(
                    weeklyHours = week,
                    onBackClick = {},
                    onChangeDistractingAppsClick = {},
                    onAccountabilityPartnerClick = {},
                    onContributeClick = {},
                    onRateClick = {},
                    onShareClick = {}
                )
            }
        }
    }

    @Test
    fun `pro page - free`() {
        paparazzi.snapshot {
            ZenTheme(darkTheme = false) {
                ZenProScreen(
                    entitlement = Entitlement.Free,
                    offers = offers,
                    isWorking = false,
                    errorMessage = null,
                    onBackClick = {},
                    onPurchase = {},
                    onCancel = {},
                    onResume = {}
                )
            }
        }
    }
}
