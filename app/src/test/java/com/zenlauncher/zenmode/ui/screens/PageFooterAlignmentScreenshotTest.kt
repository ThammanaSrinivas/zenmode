package com.zenlauncher.zenmode.ui.screens

import android.graphics.drawable.ColorDrawable
import android.graphics.Color as AndroidColor
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.AppInfo
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test

/**
 * Home, Zen Score and Zen Gold are one left-right swipe apart, so their main actions have to
 * land on the same line — otherwise the button jumps as you page between them.
 *
 * All three go through [com.zenlauncher.zenmode.ui.components.PinnedPageFooter], which owns the
 * slot heights, so the guarantee is structural. These goldens are what catches it if someone
 * puts a page's action back in the page body, or hands the footer content that outgrows its
 * slot: the search bar, "Share Zen Score" and "Invest Gold" must sit at the same y in all
 * three images, with the page dots underneath them at the same y too.
 */
class PageFooterAlignmentScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `footer alignment - home`() {
        paparazzi.golden {
            ZenTheme(darkTheme = false) {
                HomeScreen(
                    usage = DailyUsage(screenTimeInMillis = 95 * 60 * 1000L),
                    streaks = 4,
                    yesterdayChangePercent = -8,
                    hasBuddies = false,
                    buddyStats = BuddyStats(screenTimeMins = 0L),
                    isSignedIn = true,
                    showSearch = false,
                    zenScore = 76,
                    goldInvested = "0",
                    goldChangePercent = 3,
                    onShowSearchChange = {},
                    onGoogleSearch = {},
                    onLensClick = {},
                    onPhoneClick = {},
                    onLockClick = {},
                    onInviteBuddyClick = {},
                    onSignInClick = {},
                    onAppClick = {},
                    apps = listOf(
                        AppInfo(
                            label = "Sample App",
                            packageName = "com.example.sample",
                            icon = ColorDrawable(AndroidColor.DKGRAY)
                        )
                    )
                )
            }
        }
    }

    @Test
    fun `footer alignment - zen score`() {
        paparazzi.golden {
            ZenTheme(darkTheme = false) {
                ZenScoreScreen(score = 76, userName = "Kamal", onBackClick = {})
            }
        }
    }

    @Test
    fun `footer alignment - zen gold`() {
        paparazzi.golden {
            ZenTheme(darkTheme = false) {
                ZenGoldScreen(onBackClick = {})
            }
        }
    }
}
