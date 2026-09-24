package com.zenlauncher.zenmode.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test

/**
 * Snapshot coverage for Invest Gold step 1 (Figma node 2026:1250): a quick pick
 * selected, and the weekly cap reached (plus disabled, no quick pick selected);
 * and step 2, the order read back before Kite.
 * Inspection mode is forced on so entrance motion renders its settled frame.
 * Baselines: `./gradlew recordPaparazzi`, review under app/src/test/snapshots/.
 */
class InvestGoldScreenScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `invest gold - quick pick selected`() {
        snapshotInvestGold(units = 3)
    }

    @Test
    fun `invest gold - weekly cap reached`() {
        snapshotInvestGold(units = AppConstants.INVEST_GOLD_MAX_UNITS_PER_WEEK)
    }

    @Test
    fun `invest gold - review`() {
        paparazzi.golden {
            Settled { InvestGoldReviewScreen(units = 8, onBackClick = {}, onOpenKiteClick = {}, onChangeQuantityClick = {}) }
        }
    }

    private fun snapshotInvestGold(units: Int) {
        paparazzi.golden { Settled { InvestGoldScreen(units = units, onUnitsChange = {}, onBackClick = {}, onReviewInKiteClick = {}) } }
    }

    @Composable
    private fun Settled(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalInspectionMode provides true) {
            ZenTheme(darkTheme = false) { content() }
        }
    }
}
